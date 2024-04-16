package com.ruoyi.web.controller.admin.controller;

import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.PayOrder;
import com.ruoyi.system.zny.entity.User;
import com.ruoyi.system.zny.service.*;
import com.ruoyi.system.zny.vo.PayOrderVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.*;

@Api("抢单")
@RestController
@RequestMapping(value = "/grab")
public class GrabController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private PayChannelService payChannelService;

	@Autowired
	private PayConfigDetailService payConfigDetailService;

	@Autowired
	private UserService userService;

	@Autowired
	private PayOrderService payOrderService;

	@Autowired
	private ProductService productService;

	@Operation(summary = "设置自动抢单")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/setauto")
	public R setauto(Long userId, HttpServletRequest request) {
		try {
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			BigDecimal amount = this.productService.selectMinPrice();
			if (user.getProductMoney().compareTo(amount) < 0) {
				return R.fail(ERROR, "余额不足");
			}
			List<Byte> statusList = new ArrayList<>();
			statusList.add(Dictionary.PayStatusEnum.LOCKED.getKey());
			statusList.add(Dictionary.PayStatusEnum.TO_GRAB.getKey());
			return R.ok(this.productService.grabTask(statusList, user));
		} catch (Exception e) {
			logger.error("/grab/setauto 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "抢单记录")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "status", value = "2成功确认, 3失败且退款, 5冻结, 6抢单成功,待支付确认, 7支付确认,待成功", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "type", value = "支付配置id, 0:余额支付, 1:微信  2:支付宝, 3:积分购买, 4:银联支付, 5:抢单余额支付", required = false, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/queryGrab")
	public R queryGrab(Long userId, Byte type, Byte status, //
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime, //
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime, //
			Integer pageNum, Integer pageSize, //
			HttpServletRequest request) {
		try {
			String ip = this.getIpAddr(request);
			logger.info("queryGrab userId = {}, type = {}, status = {}", userId, type, status);
			PayOrder order = new PayOrder();
			order.setType(type);
			order.setStatus(status);
			order.setUserid(userId);
			PageInfo<PayOrderVo> page = this.payOrderService.selectPageByCondition(null, order, null, startTime, endTime, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.fail(page);
		} catch (Exception e) {
			logger.error("/grab/queryGrab 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "取消")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "orderNum", value = "订单号", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/cancel")
	public R cancel(Long userId, String orderNum) {
		try {
			logger.info("cancel userId = {}, orderNum = {}", userId, orderNum);
			PayOrderVo order = this.payOrderService.selectByOrderNum(orderNum);
			if (order == null) {
				return R.fail(ERROR, "订单不存在");
			}
			if (order.getUserid().longValue() != userId.longValue()) {
				return R.fail(ERROR, "用户不匹配");
			}
			if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.TO_GRAB.getKey().byteValue()) {
				return R.fail(ERROR, "订单状态不对");
			}
			order.setStatus(Dictionary.PayStatusEnum.FAILED.getKey());
			order.setTypeDesc("用户取消");
			this.payOrderService.save(order);
			this.userService.updateProductMoney(userId, order.getPayAmount(), IN, Dictionary.MoneyTypeEnum.AUDIT_FAILED.getKey());
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/grab/cancel 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "抢单成功,点击确认支付")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "orderNum", value = "订单号", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/confimPay")
	public R confimPay(Long userId, String orderNum) {
		try {
			logger.info("confimPay userId = {}, orderNum = {}", userId, orderNum);
			PayOrderVo order = this.payOrderService.selectByOrderNum(orderNum);
			if (order == null) {
				return R.fail(ERROR, "订单不存在");
			}
			if (order.getUserid().longValue() != userId.longValue()) {
				return R.fail(ERROR, "用户不匹配");
			}
			if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.TO_GRAB.getKey().byteValue()) {
				return R.fail(ERROR, "订单状态不对");
			}
			order.setStatus(Dictionary.PayStatusEnum.GRABED.getKey());
			this.payOrderService.save(order);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/grab/confimPay 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

}
