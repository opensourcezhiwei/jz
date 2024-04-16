package com.ruoyi.web.controller.admin.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.PayType;
import com.ruoyi.system.zny.service.PayTypeService;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static com.ruoyi.system.zny.constants.StatusCode.*;

@Api("支付")
@RestController
@RequestMapping(value = "/payType")
public class PayTypeController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private PayTypeService payTypeService;


	@Operation(summary = "添加支付类型")
	@PostMapping(value = "/add")
	@UserSyncLock(key = "#payType.name")
	public R pay(@Validated PayType payType) {
		try {
			logger.info("添加支付类型 name = {}, status = {}", payType.getName(), payType.getStatus());
			payType.setType(Dictionary.PayType.OTHER_PAY.getKey().byteValue()); //所有配置都是其它，因为需要微信阿里需要默认配置
			payTypeService.save(payType);
			payType.setSort(payType.getId().intValue());
			payTypeService.save(payType);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/product/buy 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}
	@Operation(summary = "删除支付类型")
	@PostMapping(value = "/del")
	@UserSyncLock(key ="'PAYTYPE'+"+ "#id")
	public R del(@RequestParam Long id, HttpServletRequest request) {
		try {
/*			AdminUser adminUser = (AdminUser)request.getSession().getAttribute("user");
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("删除支付类型权限 {}", adminUser);
				return result(ERROR, "无操作权限，请重新登录");
			}*/
			if(id <= 3) {
				return R.fail(ERROR, "无法删除");
			}
			payTypeService.delById(id);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("删除支付类型 出错: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "支付状态修改")
	@PostMapping(value = "/status")
	@UserSyncLock(key ="'PAYTYPE'+"+ "#id")
	public R updateStatus(@RequestParam Long id, @RequestParam  Byte status, HttpServletRequest request) {
		try {
/*			AdminUser adminUser = (AdminUser)request.getSession().getAttribute("user");
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("支付状态修改无权限 {}", adminUser);
				return result(ERROR, "无操作权限，请重新登录");
			}*/
			PayType payType = payTypeService.selectById(id);
			if (payType == null) {
				return R.fail(ERROR, "ID不存在");
			}
			payType.setStatus(status);
			payTypeService.update(payType);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("删除支付类型 出错: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}
	@Operation(summary = "支付上移")
	@PostMapping(value = "/moveUp")
	@UserSyncLock(key ="'PAYTYPE'+"+ "#id")
	public R moveUp(@RequestParam Long id, HttpServletRequest request) {
		try {
/*			AdminUser adminUser = (AdminUser)request.getSession().getAttribute("user");
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("支付上移无权限 {}", adminUser);
				return result(ERROR, "无操作权限，请重新登录");
			}*/
			PayType payType = payTypeService.selectById(id);
			if (payType == null) {
				return R.fail(ERROR, "ID不存在");
			}
			payTypeService.moveUp(payType);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("支付上移 出错: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}
	@Operation(summary = "支付下移")
	@PostMapping(value = "/moveDown")
	@UserSyncLock(key ="'PAYTYPE'+"+ "#id")
	public R moveDown(@RequestParam Long id, HttpServletRequest request) {
		try {
/*			AdminUser adminUser = (AdminUser)request.getSession().getAttribute("user");
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("支付下移无权限 {}", adminUser);
				return result(ERROR, "无操作权限，请重新登录");
			}*/
			PayType payType = payTypeService.selectById(id);
			if (payType == null) {
				return R.fail(ERROR, "ID不存在");
			}
			payTypeService.moveDown(payType);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("支付下移 出错: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询支付列表")
	@PostMapping(value = "/query")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "status", value = "状态0不可用 1可用"), //
	})
	public R query(Byte status) {
		try {
			PayType payType = new PayType();
			payType.setStatus(status);
			return R.ok(payTypeService.selectByCondition(payType));
		} catch (Exception e) {
			logger.error("查询支付列表出错: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}
}
