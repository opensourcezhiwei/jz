package com.ruoyi.web.controller.admin.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.AdminUser;
import com.ruoyi.system.zny.entity.BankCard;
import com.ruoyi.system.zny.entity.User;
import com.ruoyi.system.zny.service.BankCardService;
import com.ruoyi.system.zny.service.UserService;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
import com.ruoyi.web.controller.admin.constants.AppConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api("银行卡")
@RestController
@RequestMapping(value = "/bank")
public class BankCardController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private BankCardService bankCardService;

	@Autowired
	private UserService userService;

	@Operation(summary = "查询个人银行卡")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/query")
	public R query(Long userId, HttpServletRequest request) {
		try {
			logger.info("bank query = {}", userId);
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail("用户不存在");
			}
			BankCard param = new BankCard();
			param.setTel(user.getTel());
			param.setStatus(Dictionary.STATUS.ENABLE);
			List<BankCard> list = this.bankCardService.selectByCondition(param);
			for (BankCard vo : list) {
				if (!StringUtils.isNull(vo.getTel()) && vo.getTel().length() >= 11) {
					vo.setTel(vo.getTel().substring(0, 3) + "****" + vo.getTel().substring(7));
				}
			}
			return R.ok(list);
		} catch (Exception e) {
			logger.error("/bank/query 出错: ", e);
			return R.fail("服务器出错");
		}
	}

	@Operation(summary = "修改银行卡")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/update")
	@UserSyncLock(key = "#param.tel")
	public R update(BankCard param, HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无权限 修改银行卡 {}", adminUser);
				return R.fail("无操作权限，请重新登录");
			}
			this.bankCardService.save(param);
			return R.ok(param);
		}catch(Exception e) {
			return R.fail("服务器出错: ");
		}
	}

	@Operation(summary = "添加银行卡")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "bankAccount", value = "银行账户", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "bankName", value = "银行名称", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "bankNo", value = "银行卡号", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/add")
	@UserSyncLock(key = "#userId")
	public R add(Long userId, BankCard param, HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			if (StringUtils.isNull(sessionId)) {
				return R.fail("请重新登录");
			}
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail( "用户不存在");
			}
			param.setTel(user.getTel());
			logger.info("addBank param = {}, sessionId = {}", param.toString(), sessionId);
			if (userId == null) {
				return R.fail("用户参数为空");
			}
			String sessionIdInMap = AppConstants.sessionIdMap.get(user.getTel());
			sessionIdInMap = sessionIdInMap == null ? "" : sessionIdInMap;
			if (!sessionId.equals(sessionIdInMap)) {
				logger.error("tel = {}, sessionId = {}, sessionIdInMap = {}", userId, sessionId, sessionIdInMap);
				return R.fail("session不匹配请重新登录");
			}
//			if (StringUtil.isNull(param.getBankAccount())) {
//				return result(ERROR, "银行卡姓名为空");
//			}
			param.setBankAccount(user.getRealname());
			if (StringUtils.isNull(param.getBankName())) {
				return R.fail("银行名称不能为空");
			}
			if (param.getBankNo() == null) {
				return R.fail("银行卡号不能为空");
			}
			List<BankCard> list = this.bankCardService.selectByCondition(param);
			if (list != null && list.size() > 0) {
				return R.fail("银行卡存在");
			}
			param.setStatus(Dictionary.STATUS.ENABLE);
			this.bankCardService.save(param);
			return R.ok(true);
		} catch (Exception e) {
			logger.error("/bank/add 出错:", e);
			return R.fail("服务器出错");
		}
	}

	@Operation(summary = "删除银行卡")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "银行id", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/del")
	@UserSyncLock(key = "#param.tel")
	public R del(BankCard param, HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			if (StringUtils.isNull(sessionId)) {
				return R.fail("请重新登录");
			}
			logger.info("addBank param = {}, sessionId = {}", param.toString(), sessionId);
			if (param.getId() == null) {
				return R.fail( "银行卡id不能为空");
			}
			BankCard bankCard = this.bankCardService.selectById(param.getId());
			String sessionIdInMap = AppConstants.sessionIdMap.get(bankCard.getTel());
			if (!sessionId.equals(sessionIdInMap)) {
				logger.error("tel = {}, sessionId = {}, sessionIdMap = {}", bankCard.getTel(), sessionId, sessionIdInMap);
				return R.fail("session不匹配请重新登录");
			}
			this.bankCardService.delById(bankCard.getId());
//			if (bankCard != null) {
//				bankCard.setStatus(Dictionary.STATUS.DISABLE);
//				this.bankCardService.save(bankCard);
//			}
			return R.ok(true);
		} catch (Exception e) {
			logger.error("/bank/del 出错: ", e);
			return R.fail("服务器出错");
		}
	}

}
