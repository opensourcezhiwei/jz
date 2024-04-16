package com.ruoyi.web.controller.admin.controller;

import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayChannel;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.entity.PayOrder;
import com.ruoyi.system.zny.entity.User;
import com.ruoyi.system.zny.service.PayChannelService;
import com.ruoyi.system.zny.service.PayConfigDetailService;
import com.ruoyi.system.zny.service.PayOrderService;
import com.ruoyi.system.zny.service.UserService;
import com.ruoyi.system.zny.utils.MD5Util;
import com.ruoyi.system.zny.vo.PayOrderVo;
import com.ruoyi.system.zny.vo.TotalCountVo;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
import com.ruoyi.web.controller.admin.pay.*;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.Dictionary.PayStatusEnum.MAYBE;
import static com.ruoyi.system.zny.constants.StatusCode.DEFAULT_PAGE_SIZE;
import static com.ruoyi.system.zny.constants.StatusCode.STATUS;

@Api("支付")
@RestController
@RequestMapping(value = "/pay")
public class PayController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private PayChannelService payChannelService;

	@Autowired
	private PayConfigDetailService payConfigDetailService;

	@Autowired
	private UserService userService;

	@Autowired
	private PayOrderService payOrderService;

	@Operation(summary = "自定义金额购买产品")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "type", value = "支付配置id, 0:余额支付, 1:微信  2:支付宝, 4:银行卡", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "amount", value = "支付金额", required = true, dataType = "decimal", dataTypeClass = BigDecimal.class), //
	})
	@PostMapping(value = "/pay")
	@UserSyncLock(key = "#userId")
	public R pay(Long userId, Integer type, BigDecimal amount, HttpServletRequest request) {
		try {
			String ip = this.getIpAddr(request);
			logger.info("amount = {}, userId = {}, type = {}, ip = {}", amount, userId, type, ip);
			PayChannel channel = this.payChannelService.selectOneByType(type, amount);
			if (channel == null) {
				return R.fail(ERROR, "没有合适的通道");
			}
			PayConfigDetail detail = this.payConfigDetailService.selectById(channel.getPayConfigDetailId());
			if (detail == null) {
				return R.fail(ERROR, "支付通道配置错误");
			}
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			if (Dictionary.STATUS.DISABLE == user.getStatus()) {
				return R.fail(ERROR, "用户被禁用");
			}
			// 生成订单, 拉起支付
			PayOrder order = new PayOrder();
			order.setOrderNum(MD5Util.string2MD5(System.currentTimeMillis() + "" + user.getTel()));
			order.setPayAmount(amount);
			order.setUserid(userId);
			order.setType(type.byteValue());
			order.setStatus(Dictionary.PayStatusEnum.PAYING.getKey());

			channel.setUpdateTime(new Date());
			this.payChannelService.save(channel);
			Map<String, Object> result = new HashMap<>();
			if ("desheng".equals(detail.getType())) {
				result = DeShengPay.pay(amount, order.getOrderNum(), channel.getType(), detail);
			}
			if ("pubPay".equals(detail.getType())) {
				result = PubPay.pay(amount, order.getOrderNum(), channel.getType(), detail);
			}
			if ("huijinplay".equals(detail.getType())) {
				result = HuiJinPlay.pay(amount, order.getOrderNum(), channel.getType(), detail);
			}
//			if ("juyun".equals(detail.getType())) {
//				result = JuYunPay.pay(amount, order.getOrderNum(), config, detail);
//			}
			if ("jinniu".equals(detail.getType())) {
				result = JinNiuPay.pay(amount, order.getOrderNum(), channel.getType(), detail, ip);
			}
			if ("jiexin".equals(detail.getType()) || "haoyuan".equals(detail.getType())) { // 捷信  灏源
				result = JieXinPay.pay(amount, order.getOrderNum(), channel.getType(), detail);
			}
			if ("chuanhu".equals(detail.getType())) {
				result = ChuanHuPay.pay(amount, order.getOrderNum(), channel.getType(), detail);
			}
			if ("fenghong".equals(detail.getType())) {
				result = FengHongPay.pay(amount, order.getOrderNum(), channel.getType(), detail, ip);
			}
			if ("jiulong".equals(detail.getType())) {
				result = JiuLongPay.pay(amount, order.getOrderNum(), channel.getType(), detail);
			}
			order.setPayConfigId(channel.getId());
			order.setTypeDesc(channel.getName() + ":" + channel.getType() + ":" + ip);
			if (StatusCode.ERROR.equals(result.get(STATUS) + "")) {
				return R.ok(result);
			}
			this.payOrderService.save(order);
			return R.ok(result);
		} catch (Exception e) {
			logger.error("/product/buy 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "抢单记录")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "status", value = "1,2,3,5,7", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "type", value = "支付配置id, 0:余额支付, 1:微信  2:支付宝, 3:积分购买, 4:银联支付, 5:抢单余额支付", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/queryGrab")
	public R queryGrab(Long userId, Byte type, Byte status, //
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime, //
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime, //
			Integer pageNum, Integer pageSize, //
			HttpServletRequest request) {
		try {
			String ip = this.getIpAddr(request);
			logger.info("queryGrab amount = {}, userId = {}, type = {}, ip = {}", userId, type, ip);
			PayOrder order = new PayOrder();
			order.setType(type);
			order.setStatus(status);
			PageInfo<PayOrderVo> page = this.payOrderService.selectPageByCondition(userId, order, ip, startTime, endTime, //
					pageNum == null ? DEFAULT_PAGE_SIZE : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/pay/queryGrab 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "统计佣金和单数")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "status", value = "1,2,3,5,7", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "type", value = "支付配置id, 0:余额支付, 1:微信  2:支付宝, 3:积分购买, 4:银联支付, 5:抢单余额支付", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/totalRate")
	public R totalRate(Long userId, Byte type, Byte status, //
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime, //
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
		try {
			logger.info("totalRate userId = {}, type = {}, status = {}", userId, type, status);
			PayOrder order = new PayOrder();
			order.setUserid(userId);
			order.setType(type);
			order.setStatus(status);
			TotalCountVo count = this.payOrderService.totalRateByCondition(order, startTime, endTime);
			return R.ok(count);
		} catch (Exception e) {
			logger.error("/pay/queryGrab 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}
}
