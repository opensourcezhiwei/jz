package com.ruoyi.web.controller.admin.controller;

import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.entity.*;
import com.ruoyi.system.zny.service.CouponService;
import com.ruoyi.system.zny.service.UserService;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
import com.ruoyi.web.controller.admin.constants.AppConstants;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.common.core.domain.R.SUCCESS;
import static com.ruoyi.system.zny.constants.StatusCode.*;

@RestController
@RequestMapping(value = "/coupon")
public class CouponController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private CouponService couponService;

	@Autowired
	private UserService userService;

	@Operation(summary = "查询可用优惠券")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/queryByUserId")
	public R queryByUserId(Long userId) {
		try {
			if (userId == null) {
				return R.fail(ERROR, "用户id为空");
			}
			List<Map<String,Object>> list = this.couponService.selectUserConponList(userId);
			return R.fail(list);
		} catch (Exception e) {
			logger.error("查询优惠券", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询所有优惠券")
	@PostMapping(value = "/query")
	public R query() {
		try {
			List<CouponCode> list = this.couponService.selectByCondition(new CouponCode());
			return R.fail(list);
		} catch (Exception e) {
			logger.error("查询优惠券", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "优惠兑换")
	@PostMapping(value = "/redeem/change")
	@UserSyncLock(key = "#userId")
	public R bgy(Long userId, String code) {
		try {
			logger.info("bgy userId = {}, code = {}", userId, code);
			if (userId == null || StringUtils.isEmpty(code)) {
				return R.fail(ERROR, "参数错误");
			}
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			RedeemCode param = new RedeemCode();
			param.setCode(code);
			param.setStatus((byte)1);
			List<RedeemCode> list = couponService.selectByCondition(param);
			if (list.size() == 0) {
				return R.fail(ERROR, "兑换码不存在或已使用");
			}
			RedeemCode redeemCode = list.get(0);
			couponService.insertUserCoupon(userId,redeemCode.getCouponId());
			redeemCode.setStatus((byte)0);
			couponService.save(redeemCode);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}
	@Operation(summary = "增加优惠码")
	@PostMapping(value = "/redeem/add")
	@UserSyncLock(key = "#code")
	public R addRedeem(String code,Long couponId, HttpServletRequest request) {
		try {
			if (couponId == null || StringUtils.isEmpty(code)) {
				return R.fail(ERROR, "参数错误");
			}
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无查看权限 {}", adminUser);
				return R.fail(ERROR, "无查看权限，请重新登录");
			}
			CouponCode couponCode = this.couponService.selectById(couponId);
			if (couponCode == null) {
				return R.fail(ERROR, "参数错误");
			}
			RedeemCode param = new RedeemCode();
			param.setCode(code);
			param.setStatus((byte)1);
			param.setCouponId(couponId);
			this.couponService.save(param);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}
	@Operation(summary = "兑换码查询")
	@PostMapping(value = "/redeem/query")
	public R queryRedeem( Integer pageNum, Integer pageSize,HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
/*			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无查看权限 {}", adminUser);
				return result(ERROR, "无查看权限，请重新登录");
			}*/
			RedeemCode query = new RedeemCode();
			query.setStatus((byte)1);
			PageInfo<RedeemCodeExtend> list = this.couponService.selectByCondition(query, pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(list);
		} catch (Exception e) {
			logger.error("/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

}
