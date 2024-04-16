package com.ruoyi.web.controller.admin.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.Loan;
import com.ruoyi.system.zny.entity.SysConfig;
import com.ruoyi.system.zny.entity.User;
import com.ruoyi.system.zny.service.LoanService;
import com.ruoyi.system.zny.service.SysConfigService;
import com.ruoyi.system.zny.service.UserService;
import com.ruoyi.web.controller.admin.constants.AppConstants;
import com.ruoyi.web.controller.admin.sms.SmsService;
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
import java.math.BigDecimal;
import java.util.List;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.common.core.domain.R.SUCCESS;
import static com.ruoyi.system.zny.constants.StatusCode.*;

@RestController
@RequestMapping(value = "/loan")
public class LoanController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private UserService userService;

	@Autowired
	private LoanService loanService;

	@Autowired
	private SysConfigService sysConfigService;

	@Autowired
	private SmsService smsService;

	@Operation(summary = "借款查询")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "status", value = "1. 申请, 2.审核成功, 3.审核失败", required = true, dataType = "byte", dataTypeClass = Byte.class), //
	})
	@PostMapping(value = "/query")
	public R query(Long id, Long userId, Byte status) {
		try {
			Loan condition = new Loan();
			condition.setId(id);
			condition.setUserId(userId);
			condition.setStatus(status);
			List<Loan> list = this.loanService.selectByCondition(condition);
			return R.fail(list);
		} catch (Exception e) {
			logger.error("借款查询: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "借款")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "status", value = "1. 申请, 2.审核成功, 3.审核失败", required = false, dataType = "byte", dataTypeClass = Byte.class), //
	})
	@PostMapping(value = "/save")
	public R save(Loan loan, HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			if (StringUtils.isNull(sessionId)) {
				return R.fail(ERROR, "请重新登录");
			}
			logger.info("借款 userId = {}, status = {}, periodPre = {}", loan.getUserId(), loan.getStatus(), loan.getPeriodPre());
			Loan condition = new Loan();
			condition.setUserId(loan.getUserId());
			condition.setStatus((byte) 1);
			User user = this.userService.selectById(loan.getUserId());
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			String sessionIdInMap = AppConstants.sessionIdMap.get(user.getTel());
			sessionIdInMap = sessionIdInMap == null ? "" : sessionIdInMap;
			if (!sessionId.equals(sessionIdInMap)) {
				logger.error("tel = {}, sessionId = {}, sessionIdInMap = {}", user.getTel(), sessionId, sessionIdInMap);
				return R.fail(ERROR, "session不匹配请重新登录");
			}
			SysConfig config = new SysConfig();
			config.setType(201);
			config.setStatus(Dictionary.STATUS.ENABLE);
			config.setName(loan.getPeriodPre() + "");
			List<SysConfig> configList = this.sysConfigService.selectByCondition(config);
			if (CollectionUtil.isEmpty(configList)) {
				return R.fail(ERROR, "配置不存在");
			}
			config = configList.get(0);
			if (loan.getMoneyPre().compareTo(new BigDecimal(config.getSort())) < 0) {
				return R.fail(ERROR, "小于最低借款金额" + config.getSort());
			}
			loan.setRate(loan.getMoneyPre().multiply(new BigDecimal(config.getMemo())));
			loan.setMoneyBack(loan.getMoneyPre().add(loan.getRate()));
			loan.setStatus((byte) 1);
			loan.setTel(user.getTel());
			loan.setRealname(user.getRealname());
			this.loanService.save(loan);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("借款查询: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "修改")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "借款单子的id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "status", value = "1. 申请, 2.审核成功, 3.审核失败", required = true, dataType = "byte", dataTypeClass = Byte.class), //
	})
	@PostMapping(value = "/update")
	public R update(Long id, Byte status) {
		try {
			logger.info("loan update id = {}, status = {}", id, status);
			Loan loan = this.loanService.selectById(id);
			if (loan.getStatus().byteValue() != 1) {
				return R.fail(ERROR, "申请中的才能审批");
			}
			if (status.byteValue() == 2) { // 审核成功
				User user = this.userService.selectById(loan.getUserId());
				if (user != null) {
					if (StringUtils.isNull(user.getWork()) || loan.getMoneyPre().compareTo(new BigDecimal(user.getWork())) > 0) {
						return R.fail(ERROR, "借款额度不足");
					}
					if (user.getParentId() != null) {
						User parent = this.userService.selectById(user.getParentId());
						if (parent != null) {
							this.userService.updatePromiseMoney(parent.getId(), loan.getMoneyPre().multiply(new BigDecimal(0.05)), IN, Dictionary.MoneyTypeEnum.RELEASE_MONEY_BY_CHILD.getKey(), user.getRealname());
						}
					}
					user.setWork(new BigDecimal(user.getWork()).subtract(loan.getMoneyPre()).toString());
					this.userService.update(user);
					List<SysConfig> list = this.sysConfigService.selectByType(6);
					if (list != null && list.size() > 0) {
						SysConfig sysConfig = list.get(0);
						this.smsService.send(user.getTel(), sysConfig.getMemo(), true);
					}
				}
			}
			loan.setStatus(status);
			this.loanService.save(loan);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("借款查询: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "缴纳保证金")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "amount", value = "金额", required = true, dataType = "decimal", dataTypeClass = BigDecimal.class), //
			@ApiImplicitParam(name = "type", value = "IN缴纳保证金  OUT解冻保证金", required = true, dataType = "stirng", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/promiseMoney")
	public R promiseMoney(Long userId, BigDecimal amount, String type) {
		try {
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail(ERROR, "保证金不存在");
			}
			if (amount.compareTo(new BigDecimal(0)) <= 0) {
				return R.fail(ERROR, "请填写正确金额");
			}
			if (IN.equals(type)) {
				if (user.getProductMoney().compareTo(amount) < 0) {
					return R.fail(ERROR, "额度不足");
				}
				user.setProductMoney(user.getProductMoney().subtract(amount));
				user.setMoney(user.getMoney().add(amount));
				this.userService.update(user);
				UserController.filterTel(user);
				return R.ok(user);
			} else {
				if (user.getMoney().compareTo(amount) < 0) {
					return R.fail(ERROR, "保证金不足");
				}
				Loan condition = new Loan();
				condition.setUserId(userId);
				List<Loan> list = this.loanService.selectByCondition(condition);
				if (list != null && list.size() > 0) {
					return R.fail(ERROR, "保证金被冻结");
				}
				user.setMoney(user.getMoney().subtract(amount));
				user.setProductMoney(user.getProductMoney().add(amount));
				UserController.filterTel(user);
				return R.ok(user);
			}
		} catch (Exception e) {
			logger.error("promiseMoney 出错: ", e);
			return R.fail(ERROR, "服务器出错");
		}
	}

	@Operation(summary = "统计下级借款人数")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "parentId", value = "父级用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "status", value = "1. 申请, 2.审核成功, 3.审核失败", required = true, dataType = "byte", dataTypeClass = Byte.class), //
	})
	@PostMapping(value = "/totalCountByParentId")
	public R totalCountByParentId(Long parentId, Byte status) {
		try {
			Loan loan = new Loan();
			loan.setStatus(status);
			Integer count = this.loanService.totalCountByParentId(loan, parentId);
			return R.ok(count);
		} catch (Exception e) {
			logger.error("totalCountByParentId 出错: ", e);
			return R.fail(MAYBE, "服务器出错 : ");
		}
	}

}
