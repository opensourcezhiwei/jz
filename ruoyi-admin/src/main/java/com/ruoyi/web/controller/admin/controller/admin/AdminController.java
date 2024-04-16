package com.ruoyi.web.controller.admin.controller.admin;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.*;
import com.ruoyi.system.zny.service.*;
import com.ruoyi.system.zny.utils.DateUtil;
import com.ruoyi.system.zny.utils.MD5Util;
import com.ruoyi.system.zny.vo.*;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
import com.ruoyi.web.controller.admin.constants.AppConstants;
import com.ruoyi.web.controller.admin.controller.UserController;
import com.ruoyi.web.controller.admin.vo.TotalVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.*;

@Api(value = "后台用户相关")
@RestController
@RequestMapping(value = "/admin")
@Validated
public class AdminController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AdminUserService adminUserService;

	@Autowired
	private MoneyLogService moneyLogService;

	@Autowired
	private CountLogService countLogService;

	@Autowired
	private UserService userService;

	@Autowired
	private BankCardService bankCardService;

	@Autowired
	private ProductService productService;

	@Autowired
	private RateLevelService rateLevelService;


	@Autowired
	private ProductActiveService productActiveService;

	@Autowired
	private PromoterProductActivityService promoterProductActivityService;

	@Autowired
	private PromoterCountActivityService promoterCountActivityService;

	@Autowired
	private ProductRecordService productRecordService;

	@Autowired
	private CountRecordService countRecordService;

	@Autowired
	private SiteMsgService siteMsgService;

	@Autowired
	private PayOrderService payOrderService;

	@Autowired
	private PayConfigService payConfigService;

	@Autowired
	private PayConfigDetailService payConfigDetailService;

	@Autowired
	private WithdrawService withdrawService;

	@Autowired
	private PrizeConfigService prizeConfigService;

	@Autowired
	private PointRecordService pointRecordService;

	@Autowired
	private SysConfigService sysConfigService;

	@Autowired
	private ChargeWithdrawInfoService chargeWithdrawInfoService;

	@Autowired
	private PayChannelService payChannelService;

	@Autowired
	private PromiseLogService promiseLogService;

	@Autowired
	private ProductReleaseService productReleaseService;

	@Autowired
	private BuyPrizeService buyPrizeService;
	
	@Autowired
	private UsdtChargeService usdtChargeService;

	@Autowired
	private QueueChargeService queueChargeService;

	@Operation(summary = "查看客户资料")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "topId", value = "代理id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "tel", value = "手机号码", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "idCard", value = "身份证", required = false, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/user/query")
	public R userQuery(User user, Integer pageNum, Integer pageSize, HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null) {
				logger.error("无查看权限 {}", adminUser);
				return R.fail(ERROR, "无查看权限，请重新登录");
			}
			if (adminUser.getAgentId() != null) { //代理
				if(!adminUser.getAgentId().equals(user.getTopId())) {
					logger.error("无查看权限 {}", adminUser);
					return R.fail(ERROR, "无查看权限，请重新登录");
				}
			}
			PageInfo<UserVo> page = this.userService.selectPageVoByCondition(user, pageNum == null ? 0 : pageNum, pageSize == null ? 0 : pageSize);
//			PageInfo<User> page = this.userService.selectPageByCondition(user, pageNum == null ? 0 : pageNum, pageSize == null ? 0 : pageSize);
//			filterTel(page.getList());
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/user/query 出错: ", e);
			return R.fail(MAYBE, "服务器异常");
		}
	}


	@Operation(summary = "查看下级客户资料")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "topId", value = "代理id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "tel", value = "手机号码", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "idCard", value = "身份证", required = false, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/user/queryDown")
	public R userDown(User user,Integer level, Integer pageNum, Integer pageSize,HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null) {
				logger.error("无查看权限 {}", adminUser);
				return R.fail(ERROR, "无查看权限，请重新登录");
			}
			if (adminUser.getAgentId() != null) { //代理
				if(!adminUser.getAgentId().equals(user.getTopId())) {
					logger.error("无查看权限 {}", adminUser);
					return R.fail(ERROR, "无查看权限，请重新登录");
				}
			}
			List<User> list = userService.selectByCondition(user);
			if (list.size() == 0) {
				return R.fail(ERROR, "用户不存在");
			}
			if (level < 1 || level > 3) { //ROOT用户
				return R.fail(ERROR, "请选择下级层级");
			}
			PageInfo<UserVo> page = userService.selectChild(list.get(0).getId(), pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize, level);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/user/query 出错: ", e);
			return R.fail(MAYBE, "服务器异常");
		}
	}
	@Operation(summary = "下级SUM")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "shareId", value = "邀请码", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/team/sum")
	public R userQuery(Long id, Integer level) {
		try {

			User cu = userService.selectById(id);
			if (cu == null) {
				return R.fail(ERROR, "用户不存在");
			}
			if (cu.getParentId() == null && level == 0) { //ROOT用户
				return R.fail(ERROR, "请选择下级层级");
			}
			ChildSum childSum = userService.selectChild(cu.getId(), level);
			return R.ok(childSum);
		} catch (Exception e) {
			logger.error("/admin/team/sum 出错: ", e);
			return R.fail(MAYBE, "服务器异常");
		}
	}


	@Operation(summary = "客户状态修改")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "status", value = "0:禁用, 1:启用", required = true, dataType = "byte", dataTypeClass = Byte.class), //
	})
	@PostMapping(value = "/user/status")
	public R userStatus(Long id, Byte status, Integer pageNum, Integer pageSize,HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无权限/user/status {}", adminUser);
				return R.fail(ERROR, "无操作权限，请重新登录");
			}
			logger.info("/user/back/status id = {}, status = {}", id, status);
			User user = this.userService.selectById(id);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			user.setStatus(status);
			this.userService.update(user);
			filterTel(user);
			return R.ok(user);
		} catch (Exception e) {
			logger.error("/admin/user/status 出错 : ", e);
			return R.fail(MAYBE, "服务器异常");
		}
	}

	@Operation(summary = "客户删除")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/user/del")
	public R userDel(Long id,HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无客户删除权限 {}", adminUser);
				return R.fail(ERROR, "无操作权限，请重新登录");
			}
			logger.info("/user/back/del id = {} {}", id, this.getIpAddr(request));
			this.userService.delById(id);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/admin/user/del 出错", e);
			return R.fail(MAYBE, "服务器异常");
		}
	}

	@Operation(summary = "客户资金修改")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "money", value = "用户余额", required = false, dataType = "java.math.BigDecimal", dataTypeClass = BigDecimal.class), //
			@ApiImplicitParam(name = "productMoney", value = "可提现余额", required = false, dataType = "java.math.BigDecimal", dataTypeClass = BigDecimal.class), //
			@ApiImplicitParam(name = "productCount", value = "购买的数量", required = false, dataType = "integer", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "point", value = "积分", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/user/balance")
	@UserSyncLock(key = "#id")
	public R userBalance(Long id, BigDecimal money, BigDecimal productMoney, Integer productCount, Integer point,HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无客户资金修改权限 {}", adminUser);
				return R.fail(ERROR, "无操作权限，请重新登录");
			}
			logger.info("/user/back/balance id = {}, money = {}, productMoney = {}", id, money, productMoney);
//			String sessionId = this.getSessionIdInHeader(request);
//			if (StringUtil.isNull(sessionId)) {
//				return result(ERROR, "请重新登录");
//			}
			User user = this.userService.selectById(id);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
//			String sessionIdInMap = AppConstants.sessionIdMap.get(user.getTel());
//			sessionIdInMap = sessionIdInMap == null ? "" : sessionIdInMap;
//			if (!sessionId.equals(sessionIdInMap)) {
//				logger.error("tel = {}, sessionId = {}, sessionIdInMap = {}", id, sessionId, sessionIdInMap);
//				return result(ERROR, "session不匹配请重新登录");
//			}
			if (money != null) {
//				BigDecimal changeMoney = money.subtract(user.getMoney());
				user.setMoney(money);
//				if (changeMoney.compareTo(new BigDecimal(0)) != 0) {
//					this.userService.updateMoney(id, changeMoney.abs(), changeMoney.compareTo(new BigDecimal(0)) > 0 ? IN : OUT, Dictionary.MoneyTypeEnum.WITHDRAW.getKey(), Dictionary.MoneyTypeEnum.BACK.getValue());
//				}
			}
			if (productMoney != null) {
				user.setProductMoney(productMoney);
//				BigDecimal changeProductMoney = productMoney.subtract(user.getProductMoney());
//				if (changeProductMoney.compareTo(new BigDecimal(0)) != 0) {
//					this.userService.updateProductMoney(id, changeProductMoney.abs(), changeProductMoney.compareTo(new BigDecimal(0)) > 0 ? IN : OUT, Dictionary.MoneyTypeEnum.BACK.getKey());
//				}
			}
			if (productCount != null) {
				user.setProductCount(productCount);
//				Integer changeProductCount = productCount - user.getProductCount();
//				if (changeProductCount != 0) {
//					this.userService.updateProductCount(id, Math.abs(changeProductCount), changeProductCount > 0 ? IN : OUT, Dictionary.MoneyTypeEnum.BACK.getKey());
//				}
			}
			if (point != null) {
				user.setPoint(point);
			}
			this.userService.update(user);
			filterTel(user);
			return R.ok(user);
		} catch (Exception e) {
			logger.error("/admin/user/balance 出错:", e);
			return R.fail(MAYBE, "服务器异常");
		}
	}

	@Operation(summary = "上级修改")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "parentId", value = "上级id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/updateParentId")
	@UserSyncLock(key = "#userId")
	public R updateParentId(Long userId, Long parentId) {
		try {
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			User parent = this.userService.selectById(parentId);
			if (parent == null) {
				return R.fail(ERROR, "上级不存在");
			}
			user.setParentId(parent.getId());
			user.setParent2Id(parent.getParentId());
			user.setParent3Id(parent.getParent2Id());
			user.setTopId(parent.getTopId());
			this.userService.update(user);
			UserController.filterTel(user);
			return R.ok(user);
		} catch (Exception e) {
			logger.error("查询个人资料", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询下级")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "level", value = "1:下一级(默认)  2:下二级", required = false, dataType = "byte", dataTypeClass = Byte.class), //
			@ApiImplicitParam(name = "show", value = "1过滤 0不过滤 默认1", required = false, dataType = "byte", dataTypeClass = Byte.class), //
	})
	@PostMapping(value = "/user/queryByParentId")
	public R queryByParentId(Long userId, Integer level, Byte show, Integer pageNum, Integer pageSize,HttpServletRequest request) {
		try {
			User condition = new User();
			if (level == null || level.intValue() == 1) {
				condition.setParentId(userId);
			} else {
				condition.setParent2Id(userId);
			}
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null) {
				logger.error("无查看权限 {}", adminUser);
				return R.fail(ERROR, "无查看权限，请重新登录");
			}
			PageInfo<User> page = this.userService.selectPageByCondition(condition, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			if (show == null || show.byteValue() == Dictionary.STATUS.ENABLE) {
				filterTel(page.getList());
			}
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/user/balance 出错:", e);
			return R.fail(MAYBE, "服务器异常");
		}
	}

	@Operation(summary = "客户密码修改")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "用户id", required = true, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "loginPassword", value = "登录新密码", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/user/updateLoginPassword")
	public R updateLoginPassword(Long id, String loginPassword, HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无权限 /balance/update {}", adminUser);
				return R.fail(ERROR, "无操作权限，请重新登录");
			}
			User user = this.userService.selectById(id);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			logger.info("客户密码修改 [{}] [{}] [{}]" ,user.getTel() ,loginPassword,this.getIpAddr(request));
			User _user = new User();
			_user.setId(user.getId());
			_user.setLoginPassword(userService.passwdGenerate(loginPassword,user.getTel()));
			this.userService.update(_user);
			filterTel(user);
			return R.ok(user);
		} catch (Exception e) {
			logger.error("/admin/user/updateLoginPassword 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "客户交易密码修改")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "tradePassword", value = "交易密码", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/user/updateTradePassword")
	public R updateTradePassword(Long id, String loginPassword, String tradePassword, HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无权限 /balance/update {}", adminUser);
				return R.fail(ERROR, "无操作权限，请重新登录");
			}
			User user = this.userService.selectById(id);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			logger.info("客户交易密码修改 [{}] [{}] [{}]" ,user.getTel() ,tradePassword,this.getIpAddr(request));
			User _user = new User();
			_user.setId(user.getId());
			_user.setTradePassword(userService.passwdGenerate(tradePassword,user.getTel()));
			this.userService.update(_user);
			filterTel(user);
			return R.ok(user);
		} catch (Exception e) {
			logger.error("/admin/user/updateTradePassword 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "后台资料编辑")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "客户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "headIcon", value = "头像", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "realname", value = "真实姓名", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "idCard", value = "身份证号码", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "idCardZheng", value = "身份证正面", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "idCardFan", value = "身份证反面", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "age", value = "年龄", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "status", value = "0:禁用,1:启用(后台传)", required = false, dataType = "byte", dataTypeClass = Byte.class), //
			@ApiImplicitParam(name = "riskLevel", value = "0:免风控,1:非免风控(后台传)", required = false, dataType = "byte", dataTypeClass = Byte.class), //
			@ApiImplicitParam(name = "sex", value = "1:男,2:女,0:保密(后台传)", required = true, dataType = "byte", dataTypeClass = Byte.class), //
	})
	@PostMapping(value = "/user/updateByTel")
	public R updateByTel(User param, HttpServletRequest request) {
		try {
			if (param.getId() == null) {
				return R.fail(ERROR, "id不能为空");
			}
			User user = this.userService.selectById(param.getId());
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			User _user = new User();
			_user.setId(user.getId());
			if (!StringUtils.isNull(param.getHeadIcon())) {
				_user.setHeadIcon(param.getHeadIcon());
			}
			if (!StringUtils.isNull(param.getRealname())) {
				_user.setRealname(param.getRealname());
			}
			if (!StringUtils.isNull(param.getIdCard())) {
				if (!param.getIdCard().endsWith("****")) {
					_user.setIdCard(param.getIdCard());
				}
			}
			if (!StringUtils.isNull(param.getIdCardZheng())) {
				_user.setIdCardZheng(param.getIdCardZheng());
			}
			if (!StringUtils.isNull(param.getIdCardFan())) {
				_user.setIdCardFan(param.getIdCardFan());
			}
			if (param.getAge() != null) {
				_user.setAge(param.getAge());
			}
			if (param.getStatus() != null) {
				_user.setStatus(param.getStatus());
			}
			if (param.getRiskLevel() != null) {
				_user.setRiskLevel(param.getRiskLevel());
			}
			if (param.getSex() != null) {
				_user.setSex(param.getSex());
			}
			this.userService.update(_user);
			User result = this.userService.selectById(param.getId());
			filterTel(result);
			return R.ok(result);
		} catch (Exception e) {
			logger.error("/admin/user/updateByTel 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询后台用户")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "username", value = "用户名", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "pageNum", value = "页码", required = true, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "pageSize", value = "页条数", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/admin_user/query")
	public R adminUserQuery(String username, Integer pageNum, Integer pageSize, HttpServletRequest request) {
		try {
			logger.info("/admin/query username = {}", username);
			AdminUser user = new AdminUser();
			user.setUsername(username);
			PageInfo<AdminUser> page = this.adminUserService.selectPageByCondition(user, pageNum == null ? DEFAULT_PAGE_NUM : pageNum, pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/admin_user/query 出错: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "添加后台用户")
	@ApiImplicitParams({ //
//			@ApiImplicitParam(name = "id", value = "有id修改, 无id新增, 下面都要有", required = true, dataType = "string", dataTypeClass = String.class), //
//			@ApiImplicitParam(name = "bankAccount", value = "银行账户", required = true, dataType = "string", dataTypeClass = String.class), //
//			@ApiImplicitParam(name = "bankName", value = "银行名称", required = true, dataType = "string", dataTypeClass = String.class), //
//			@ApiImplicitParam(name = "bankNo", value = "银行卡号", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/admin_user/add")
	public R adminUserAdd(AdminUser param, HttpServletRequest request) {
		try {
			param.setPassword(MD5Util.string2MD5(param.getPassword()));
			this.adminUserService.save(param);
			return R.ok(param);
		} catch (Exception e) {
			logger.error("/admin/admin_user/add 出错 : ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}
	@Operation(summary = "后台用户修改密码")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "username", value = "用户名", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "oldPassword", value = "旧密码", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/admin_user/update")
	public R adminUserUpdate(String username,String oldPassword, String password, HttpServletRequest request) {
		try {
			logger.info("/admin_user/update username = {}, oldPassword={}, password = {}, ip = {}", username,oldPassword, password, getIpAddr(request));
			if (oldPassword == null || username == null) {
				return R.fail(ERROR, "请输入旧账号密码");
			}
			if (password.length() < 8) {
				return R.fail(ERROR, "密码不得少于8位");
			}
			AdminUser user = this.adminUserService.login(username, oldPassword);
			if (user == null) {
				return R.fail(ERROR, "用户或密码错误");
			}
			AppConstants.adminSessionCache.invalidate(user.getPassword());
			user.setPassword(MD5Util.string2MD5(password));
			this.adminUserService.save(user);
			return R.ok(SUCCESS, "修改成功，请重新登录");
		} catch (Exception e) {
			logger.error("/admin/user/login 出错 : ", e);
			return R.fail(MAYBE, "服务器异常");
		}
	}
	@Operation(summary = "后台用户登出")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "username", value = "用户名", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/admin_user/logout")
	public R adminUserLogout(String username, HttpServletRequest request) {
		try {
			logger.info("adminUserLogout username = {}, ip = {}",username, getIpAddr(request));
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser != null && adminUser.getUsername().equals(username)) {
				AppConstants.adminSessionCache.invalidate(sessionId);
				logger.info("adminUser session invalidate {}", username);
			}
			return R.ok(SUCCESS, "登出成功");
		} catch (Exception e) {
			logger.error("/admin/user/login 出错 : ", e);
			return R.fail(MAYBE, "服务器异常");
		}
	}


	@Operation(summary = "删除后台用户")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "用户id", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/admin_user/del")
	public R adminUserDel(Integer id) {
		try {
			logger.info("/admin/del id = {}", id);
			this.adminUserService.delById(id);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/admin/admin_user/del 出错 : ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "后台用户登录")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "username", value = "用户名", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/user/login")
	public R login(String username, String password, HttpServletRequest request) {
		try {
			logger.info("/user/login username = {}, password = {}, session timeout = {}s", username, password, request.getSession().getMaxInactiveInterval());
			AdminUser user = this.adminUserService.login(username, password);
			if (user == null) {
				return R.fail(ERROR, "用户或密码错误");
			}
			request.getSession().setAttribute("user", user);
			AppConstants.adminSessionCache.put(user.getPassword(), user);
			return R.ok(user);
		} catch (Exception e) {
			logger.error("/admin/user/login 出错 : ", e);
			return R.fail(MAYBE, "服务器异常");
		}
	}

	@Operation(summary = "后台查询银行卡")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "tel", value = "手机号码", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "pageNum", value = "页码", required = true, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "pageSize", value = "页条数", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/bank_card/query")
	public R backQuery(BankCard param, Integer pageNum, Integer pageSize) {
		try {
			logger.info("/bank_card/query tel = {}", param.toString());
			PageInfo<BankCard> page = this.bankCardService.selectPageByCondition(param, pageNum == null ? 0 : pageNum, pageSize == null ? 0 : pageSize);
			List<BankCard> list = page.getList();
			for (BankCard vo : list) {
				if (!StringUtils.isNull(vo.getTel()) && vo.getTel().length() >= 11) {
					vo.setTel(vo.getTel().substring(0, 3) + "****" + vo.getTel().substring(7));
				}
			}
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/bank_card/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "后台删除银行卡")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "银行卡id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/bank_card/del")
	public R backdel(Long id, HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无后台删除银行卡权限 {}", adminUser);
				return R.fail(ERROR, "无操作权限，请重新登录");
			}
			logger.info("/bank_card/del id = {} {}", id, this.getIpAddr(request));
			this.bankCardService.delById(id);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/admin/bank_card/del 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "保存推广产品活动")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "活动id, 有就修改, 没有新增, 活动条件不能修改", required = false, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/activity/product/save")
	public R saveActivityProduct(PromoterProductActivity promoterProductActivity) {
		try {
			logger.info("/admin/activity/product/save condition = {}", promoterProductActivity.getCondition());
			if (StringUtils.isNull(promoterProductActivity.getCondition())) {
				return R.fail(ERROR, "请携带条件");
			}
			if (promoterProductActivity.getStartTime() == null || promoterProductActivity.getEndTime() == null) {
				return R.fail(ERROR, "时间不能为空");
			}
			promoterProductActivity.setCondition(URLDecoder.decode(promoterProductActivity.getCondition(), "UTF-8"));
			JSONArray.parseArray(promoterProductActivity.getCondition());
			this.promoterProductActivityService.save(promoterProductActivity);
			return R.ok(promoterProductActivity);
		} catch (Exception e) {
			logger.error("/admin/activity/product/save 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询推广产品活动")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/activity/product/query")
	public R queryActivityProduct(Integer id, Date startTime, Date endTime, Integer pageNum, Integer pageSize) {
		try {
			logger.info("/activity/product/query");
			PromoterProductActivity activity = new PromoterProductActivity();
			activity.setId(id);
			PageInfo<PromoterProductActivity> page = this.promoterProductActivityService.selectPageByCondition(activity, startTime, endTime, pageNum == null ? DEFAULT_PAGE_NUM : pageNum, pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/activity/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "保存推广人数活动")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "活动id, 有就修改, 没有新增, 活动条件不能修改", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "condition", value = "[{count:xx, money}]", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/activity/person/save")
	public R saveActivityPerson(PromoterCountActivity promoterCountActivity) {
		try {
			logger.info("/activity/person/save condition = {}", promoterCountActivity.getCondition());
			if (StringUtils.isNull(promoterCountActivity.getCondition())) {
				return R.fail(ERROR, "请携带条件");
			}
			if (promoterCountActivity.getStartTime() == null || promoterCountActivity.getEndTime() == null) {
				return R.fail(ERROR, "时间不能为空");
			}
			promoterCountActivity.setCondition(URLDecoder.decode(promoterCountActivity.getCondition(), "UTF-8"));
			JSONArray.parseArray(promoterCountActivity.getCondition());
			this.promoterCountActivityService.save(promoterCountActivity);
			return R.ok(promoterCountActivity);
		} catch (Exception e) {
			logger.error("/admin/activity/person/save 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询推广人数活动")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/activity/person/query")
	public R queryActivityPerson(Integer id, Date startTime, Date endTime, Integer pageNum, Integer pageSize) {
		try {
			logger.info("/activity/person/query");
			PromoterCountActivity activity = new PromoterCountActivity();
			activity.setId(id);
			PageInfo<PromoterCountActivity> page = this.promoterCountActivityService.selectPageByCondition(activity, startTime, endTime, pageNum == null ? DEFAULT_PAGE_NUM : pageNum, pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/activity/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询产品")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/product/query")
	public R queryProduct(Integer pageNum, Integer pageSize) {
		try {
			PageInfo<Product> page = this.productService.selectPageByCondition(pageNum == null ? DEFAULT_PAGE_NUM : pageNum, pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/activity/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询产品根据id")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/product/queryById")
	public R queryProductById(Integer id) {
		try {
			Product p = this.productService.selectById(id);
			return R.ok(p);
		} catch (Exception e) {
			logger.error("/admin/activity/product/queryById 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "保存产品")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "产品id, 必传", required = true, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "status", value = "状态1开, 0关", required = true, dataType = "byte", dataTypeClass = Byte.class), //
			@ApiImplicitParam(name = "type", value = "产品id, 必传", required = true, dataType = "byte", dataTypeClass = Byte.class), //
	})
	@PostMapping(value = "/product/save")
	public R saveProduct(@RequestBody  Product product) {
		try {
			logger.info("first_reavl_value = {}", product.getFirstRealValue());
			this.productService.save(product);
			return R.ok(product);
		} catch (Exception e) {
			logger.error("/admin/activity/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "推广产品明细")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/productActivity/detail")
	public R productActivityDetail(Long userId, Integer pageNum, Integer pageSize) {
		try {
			ProductRecord record = new ProductRecord();
			record.setUserId(userId);
			PageInfo<ProductRecord> page = this.productRecordService.selectPageByCondition(record, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/productActivity/detail 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "推广产品统计")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/productActivity/total")
	public R productActivityTotal() {
		try {
			PromoterActivityTotal total = this.productRecordService.totalUser();
			return R.ok(total);
		} catch (Exception e) {
			logger.error("/admin/productActivity/detail 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "推广人数明细")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/countActivity/detail")
	public R countActivityDetail(Long userId, Integer pageNum, Integer pageSize) {
		try {
			CountRecord record = new CountRecord();
			record.setUserId(userId);
			PageInfo<CountRecord> page = this.countRecordService.selectPageByCondition(record, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/countActivity/detail 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "推广人数统计")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/countActivity/total")
	public R countActivityTotal() {
		try {
			PromoterActivityTotal totalUser = this.countRecordService.totalUser();
			return R.ok(totalUser);
		} catch (Exception e) {
			logger.error("/admin/countActivity/total 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "资金记录")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/moneylog/query")
	public R queryMoneyLog(Long topId, Long userId, String moneyType, String type, String tel, String excludeMoneyType, String moneyTypes, //
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime, @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime, Integer pageNum, Integer pageSize,HttpServletRequest request) {
		try {
			boolean isAdmin = false;
			String sessionId = this.getSessionIdInHeader(request);
			if (sessionId != null) {
				AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
				if (adminUser != null && adminUser.getAgentId() == null) {
					isAdmin = true;
				}
			}
			MoneyLog log = new MoneyLog();
			log.setMoneyType(moneyType);
			log.setUserId(userId);
			log.setType(type);
			List<String> typeList = JSONArray.parseArray(moneyTypes, String.class);
			PageInfo<MoneyLogVo> page = this.moneyLogService.selectPageByCondition(topId, log, tel, excludeMoneyType, typeList, //
					startTime, endTime, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			if(!isAdmin) {
				List<MoneyLogVo> list = page.getList();
				for (MoneyLogVo vo : list) {
					if (!StringUtils.isNull(vo.getTel()) && vo.getTel().length() >= 11) {
						vo.setTel(vo.getTel().substring(0, 3) + "****" + vo.getTel().substring(7));
					}
				}
			}
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/moneylog/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "产品记录")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/countlog/query")
	public R queryCountLog(Long topId, Long userId, String moneyType, String moneyTypes, String tel, Integer pageNum, Integer pageSize) {
		try {
			CountLog log = new CountLog();
			log.setMoneyType(moneyType);
			log.setUserId(userId);
			List<String> typeList = JSONArray.parseArray(moneyTypes, String.class);
			PageInfo<CountLogVo> page = this.countLogService.selectPageByCondition(topId, log, tel, typeList, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			List<CountLogVo> list = page.getList();
			for (CountLogVo vo : list) {
				if (!StringUtils.isNull(vo.getTel()) && vo.getTel().length() >= 11) {
					vo.setTel(vo.getTel().substring(0, 3) + "****" + vo.getTel().substring(7));
				}
			}
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/countlog/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "借贷返利记录")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/promiselog/query")
	public R queryPromiseLog(Long topId, Long userId, String moneyType, String tel, Integer pageNum, Integer pageSize) {
		try {
			PromiseLog log = new PromiseLog();
			log.setMoneyType(moneyType);
			log.setUserId(userId);
			PageInfo<PromiseLogVo> page = this.promiseLogService.selectPageByCondition(topId, log, tel, null, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			List<PromiseLogVo> list = page.getList();
			for (PromiseLogVo vo : list) {
				if (!StringUtils.isNull(vo.getTel()) && vo.getTel().length() >= 11) {
					vo.setTel(vo.getTel().substring(0, 3) + "****" + vo.getTel().substring(7));
				}
			}
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/countlog/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "支付详情查询")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/pay/detail")
	public R queryPay() {
		try {
			List<PayConfigDetail> list = this.payConfigDetailService.selectByCondition(null);
			return R.ok(list);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "支付记录查询")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "topId", value = "代理id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "userId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "orderNum", value = "单号", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "tel", value = "手机号", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "status", value = "状态", required = false, dataType = "byte", dataTypeClass = Byte.class), //
	})
	@PostMapping(value = "/pay/query")
	public R queryPay(Long topId, Long userId, String orderNum, String tel, Byte status, Integer pageNum, Integer pageSize) {
		try {
			PayOrder order = new PayOrder();
			order.setUserid(userId);
			order.setOrderNum(orderNum);
			order.setStatus(status);
			PageInfo<PayOrderVo> page = this.payOrderService.selectPageByCondition(topId, order, tel, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			List<PayOrderVo> list = page.getList();
			for (PayOrderVo vo : list) {
				if (!StringUtils.isNull(vo.getTel()) && vo.getTel().length() >= 11) {
					vo.setTel(vo.getTel().substring(0, 3) + "****" + vo.getTel().substring(7));
				}
			}
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "手动回调")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "orderNum", value = "单号", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/pay/notify")
	public R payNotify(String orderNum) {
		try {
			PayOrder order = new PayOrder();
			order.setOrderNum(orderNum);
			order = this.payOrderService.selectByOrderNum(orderNum);
			if (order == null) {
				return R.fail(ERROR, "订单不存在");
			}
			if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
				return R.fail(ERROR, "订单状态不对");
			}
			Product product = this.productService.selectById(order.getProductid());
			if (product == null) {
				return R.fail(ERROR, "产品不存在");
			}
			User user = this.userService.selectById(order.getUserid());
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
			order.setTypeDesc("手动回调");
			this.payOrderService.save(order);
			this.productActiveService.active(order.getProductid(), user, order.getType(),order);
			return R.ok(order);
		} catch (Exception e) {
			logger.error("/admin/pay/notify 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "手动回调")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "orderNum", value = "单号", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/pay/notify2")
	public R payNotify2(String orderNum) {
		try {
			PayOrder order = new PayOrder();
			order.setOrderNum(orderNum);
			order = this.payOrderService.selectByOrderNum(orderNum);
			if (order == null) {
				return R.fail(ERROR, "订单不存在");
			}
			if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
				return R.fail(ERROR, "订单状态不对");
			}
			order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
			order.setTypeDesc("手动回调");
			this.payOrderService.save(order);
			this.userService.updateProductMoney(order.getUserid(), order.getPayAmount(), IN, Dictionary.MoneyTypeEnum.CHARGE.getKey());
			return R.ok(order);
		} catch (Exception e) {
			logger.error("/admin/pay/notify 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "支付配置查询")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "productId", value = "产品id", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/payConfig/query")
	public R queryPayConfig(Integer productId) {
		try {
			List<PayConfig> list = this.payConfigService.selectByProductId(productId);
			return R.ok(list);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "支付配置修改")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/payConfig/save")
	public R savePayConfig(PayConfig config) {
		try {
			config.setUpdateTime(new Date());
			this.payConfigService.save(config);
			return R.ok(config);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "支付配置修改")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "param", value = "[{id:记录id, name:名称, status:1启用 0禁用, type:通道编码}, {}]", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/payConfig/saveList")
	public R savePayConfigList(String param) {
		try {
			List<PayConfig> configList = JSONArray.parseArray(param, PayConfig.class);
			for (PayConfig payConfig : configList) {
				PayConfig old = this.payConfigService.selectById(payConfig.getId());
				old.setName(payConfig.getName());
				old.setStatus(payConfig.getStatus());
				old.setType(payConfig.getType());
				this.payConfigService.save(old);
			}
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "删除支付配置")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "通道id", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/payConfig/delById")
	public R delById(Integer id) {
		try {
			this.payConfigService.delById(id);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "余额支付配置查询")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "通道id", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/payChannel/query")
	public R queryPayChannel(PayChannel channel) {
		try {
			List<PayChannel> list = this.payChannelService.selectByCondition(channel);
			return R.ok(list);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "余额支付配置修改")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/payChannel/save")
	public R savePayChannel(PayChannel channel) {
		try {
			this.payChannelService.save(channel);
			return R.ok(channel);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}


	@Operation(summary = "后台上下分")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class),
			@ApiImplicitParam(name = "type", value = "操作类型", required = true, dataType = "string", dataTypeClass = String.class,example = "manual_charge=上分 ; manual_withdraw=下分 "),
			@ApiImplicitParam(name = "balance", value = "变动余额", required = false, dataType = "java.math.BigDecimal", dataTypeClass = BigDecimal.class), //
			@ApiImplicitParam(name = "giftBalance", value = "赠送金额", required = false, dataType = "java.math.BigDecimal", dataTypeClass = BigDecimal.class)

	})
	@PostMapping(value = "/balance/update")
	public R backMoneyUpdate(Long userId, String type, BigDecimal balance, BigDecimal  giftBalance,HttpServletRequest request) {
		try {

			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无权限 /balance/update {}", adminUser);
				return R.fail(ERROR, "无操作权限，请重新登录");
			}

			logger.info("userId={},type={},balance={},giftBalance={},ip={}",userId,type,balance,giftBalance,this.getIpAddr(request));
			if (balance == null && giftBalance == null) {
				return R.fail(PARAM_FORMAT_ERROR, "请输入金额");
			}
			if(balance != null && balance.doubleValue() < 0) {
				return R.fail(PARAM_FORMAT_ERROR, "参数异常");
			}
			if(giftBalance != null && giftBalance.doubleValue() <= 0) {
				return R.fail(PARAM_FORMAT_ERROR, "参数异常");
			}
			if (giftBalance != null) {
				List<SysConfig> sysConfigs = this.sysConfigService.selectByType(5);
				if (CollectionUtil.isNotEmpty(sysConfigs)) {
					SysConfig sysConfig = sysConfigs.get(0);
					if(sysConfig.getName()!=null) {
						BigDecimal maxValue = new BigDecimal(sysConfig.getName());
						if(giftBalance.compareTo(maxValue) > 0) {
							return R.fail(ERROR, "超过赠送分上限: " + maxValue.doubleValue());
						}
					}
				}
			}
			User user = userService.selectById(userId);
			if (user == null) {
				logger.error("用户不存在 id = {}", userId);
				return R.fail(ERROR, "用户不存在");
			}
			Dictionary.MoneyTypeEnum typeEnum = Dictionary.MoneyTypeEnum.valueOf(type.toUpperCase(Locale.ROOT));
            if(typeEnum == Dictionary.MoneyTypeEnum.MANUAL_CHARGE) {
				if(balance != null && balance.doubleValue() > 0) {
					this.userService.addCharge(user.getId(), 0, balance);
					this.userService.updateProductMoney(user.getId(), balance, IN, Dictionary.MoneyTypeEnum.MANUAL_CHARGE.getKey());
					PayOrder order = new PayOrder();
					order.setOrderNum(MD5Util.string2MD5(System.currentTimeMillis() + "" + user.getTel()));
					order.setPayAmount(balance);
					//order.setProductid(id);
					order.setUserid(userId);
					order.setType(Dictionary.PayType.MANUALLY.getKey().byteValue());
					order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
					order.setTypeDesc(Dictionary.PayType.MANUALLY.getValue());
					this.payOrderService.save(order);
					List<RateLevel> rateList = this.rateLevelService.selectByCondition(null, new BigDecimal(0));
					for (RateLevel rateLevel : rateList) {
						if (rateLevel.getLevel() != null && rateLevel.getRate().compareTo(new BigDecimal(0)) > 0 ) {
							Long parentId = this.userService.getUserIdByLevel(user, rateLevel.getLevel());
							if (parentId != null) {
								User parent = this.userService.selectById(parentId);
								parent.setTotalChildCharge(parent.getTotalChildCharge().add(balance));
								this.userService.update(parent);
							}
						}
					}
				}

				if(giftBalance != null ) {
					this.userService.updateProductMoney(user.getId(), giftBalance, IN, Dictionary.MoneyTypeEnum.GIFT_MONEY.getKey());
				}

			}else if (typeEnum == Dictionary.MoneyTypeEnum.MANUAL_WITHDRAW) {
				if(balance == null || balance.doubleValue() < 0) {
					return R.fail(PARAM_FORMAT_ERROR, "参数异常");
				}
				if (user.getProductMoney().compareTo(balance) < 0) {
					return R.fail(ERROR, "提现金额不够");
				}

				Withdraw withdraw = new Withdraw();
				withdraw.setAmount(balance);
				//withdraw.setBankCardId(bankCardId);
				withdraw.setCharge(user.getCharge());
				List<User> list = this.userService.selectByParentId(userId);
				withdraw.setChildrenCharge(new BigDecimal(0));
				if (CollectionUtil.isNotEmpty(list)) {
					for (User user2 : list) {
						withdraw.setChildrenCharge(withdraw.getChildrenCharge().add(user2.getCharge()));
					}
				}
				List<SysConfig> configList = this.sysConfigService.selectByType(4);
				SysConfig config = configList.get(0);

				withdraw.setChildren(list == null ? 0 : list.size());
				withdraw.setRate(new BigDecimal(config.getName()));
				withdraw.setRealAmount(balance.multiply(new BigDecimal(1).subtract(withdraw.getRate())));
				withdraw.setStatus(Dictionary.PayStatusEnum.MANUALLY.getKey());
				withdraw.setType((byte) 1);
				withdraw.setUserId(userId);
				withdraw.setUsername(user.getRealname());
				this.userService.addWithdraw(user.getId(), withdraw.getAmount());
				this.withdrawService.save(withdraw);
				this.userService.updateProductMoney(user.getId(), balance, OUT, Dictionary.MoneyTypeEnum.MANUAL_WITHDRAW.getKey());
			}
			return R.ok(SUCCESS, "操作成功");

		} catch (Exception e) {
			logger.error("/balance/update:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}



	@Operation(summary = "删除余额支付配置")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "通道id", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/payChannel/delById")
	public R delChannelById(Integer id) {
		try {
			this.payChannelService.delById(id);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询提现")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "topId", value = "代理id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "userId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "status", value = "状态", required = false, dataType = "byte", dataTypeClass = Byte.class), //
	})
	@PostMapping(value = "/withdraw/query")
	public R queryWithdraw(Long topId, Long userId, String username, String tel, Byte status, Integer pageNum, Integer pageSize,HttpServletRequest request) {
		try {
			boolean isAdmin = false;
			String sessionId = this.getSessionIdInHeader(request);
			if (sessionId != null) {
				AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
				if (adminUser != null && adminUser.getAgentId() == null) {
					isAdmin = true;
				}
			}
			if (!isAdmin && userId == null) {
				return R.fail(ERROR, "session不匹配请重新登录");
			}
			WithdrawVo w = new WithdrawVo();
			w.setUserId(userId);
			w.setUsername(username);
			w.setTel(tel);
			w.setStatus(status);
			PageInfo<WithdrawVo> page = this.withdrawService.selectPageByCondition(topId, w, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			if(!isAdmin) {
				filterTelWithdraw(page.getList());
			}
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "审核提现")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "id记录", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "status", value = "1支付中  2支付成功  3支付失败", required = true, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "remark", value = "备注", required = false, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/withdraw/audit")
	@UserSyncLock(key="'withdraw:'"+"+#id")
	public R auditWithdraw(Long id, Integer status, String remark,HttpServletRequest request) {
		try {
			logger.info("id = {}, status = {}, remark = {},ip = {}", id, status, remark,this.getIpAddr(request));
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无审核提现权限 {}", adminUser);
				return R.fail(ERROR, "无操作权限，请重新登录");
			}
			logger.info("id = {}, status = {}, remark = {}", id, status, remark);
			Withdraw withdraw = this.withdrawService.selectById(id);
			if (withdraw == null) {
				return R.fail(ERROR, "记录不存在");
			}
			if (withdraw.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
				return R.fail(ERROR, "单子状态不对");
			}
			User user = this.userService.selectById(withdraw.getUserId());
			if (user == null) {
				return R.fail(ERROR, "提现用户不存在");
			}
			SiteMsg msg = new SiteMsg();
			if (status.byteValue() == Dictionary.PayStatusEnum.FAILED.getKey().byteValue()) {
				byte type = withdraw.getType();
				if (type == 1) {
					user = this.userService.updateProductMoney(withdraw.getUserId(), withdraw.getAmount(), IN, Dictionary.MoneyTypeEnum.WITHDRAW_FAILED.getKey());
				}else if (type == 2) {
					user = this.userService.updateMoney(withdraw.getUserId(), withdraw.getAmount(), IN, Dictionary.MoneyTypeEnum.WITHDRAW_FAILED.getKey(),"审核失败退款");
				}else if (type == 3) {
					user = this.userService.updatePromiseMoney(withdraw.getUserId(), withdraw.getAmount(), IN, Dictionary.MoneyTypeEnum.WITHDRAW_FAILED.getKey(),"审核失败退款");
					this.userService.updateMoney(user.getId(), withdraw.getAmount(), IN, "红包审核退回", "红包审核退回");
				}else {
					return R.fail(ERROR, "该提现类型无法审核失败");
				}
				msg.setContent("提现失败" + (StringUtils.isNull(remark) ? "" : (":" + remark)));
			} else {
				msg.setContent("提现成功" + (StringUtils.isNull(remark) ? "" : (":" + remark)));
				this.userService.addWithdraw(user.getId(), withdraw.getAmount());
			}
			withdraw.setStatus(status.byteValue());
			withdraw.setRemark(remark);
			msg.setUserId(withdraw.getUserId());
			this.siteMsgService.save(msg);
			this.withdrawService.save(withdraw);
			filterTelWithdraw(withdraw);
			return R.ok(withdraw);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "批量审核提现")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "param", value = "[{id:xx, status:(1支付中  2支付成功  3支付失败), remark:(备注)}]", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/withdraw/auditBatch")
	public synchronized R auditBatchWithdraw(String param,HttpServletRequest request) {
		try {
			logger.info("/withdraw/auditBatch param = {},ip = {}", param, this.getIpAddr(request));
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无审核提现权限 {}", adminUser);
				return R.fail(ERROR, "无操作权限，请重新登录");
			}
			logger.info("/withdraw/auditBatch param = {}", param);
			JSONArray parseArray = JSONArray.parseArray(param);
			for (int i = 0; i < parseArray.size(); i++) {
				JSONObject jsonObject = parseArray.getJSONObject(i);
				Withdraw withdraw = this.withdrawService.selectById(jsonObject.getLong("id"));
				if (withdraw == null) {
//					return result(ERROR, "记录不存在");
					continue;
				}
				if (withdraw.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
//					return result(ERROR, "单子状态不对");
					continue;
				}
				SiteMsg msg = new SiteMsg();
				if (jsonObject.getByteValue("status") == Dictionary.PayStatusEnum.FAILED.getKey().byteValue()) {
					byte type = withdraw.getType();
					if (type == 1) {
						this.userService.updateProductMoney(withdraw.getUserId(), withdraw.getAmount(), IN, Dictionary.MoneyTypeEnum.WITHDRAW_FAILED.getKey());
					}else if (type == 2) {
						this.userService.updateMoney(withdraw.getUserId(), withdraw.getAmount(), IN, Dictionary.MoneyTypeEnum.WITHDRAW_FAILED.getKey(),"审核失败退款");
					}else if (type == 3) {
						this.userService.updatePromiseMoney(withdraw.getUserId(), withdraw.getAmount(), IN, Dictionary.MoneyTypeEnum.WITHDRAW_FAILED.getKey(),"审核失败退款");
						this.userService.updateMoney(withdraw.getUserId(), withdraw.getAmount(), IN, "红包审核退回", "红包审核退回");					} else {
						continue;
					}
					msg.setContent("提现失败:" + jsonObject.getString("remark"));
				} else if(jsonObject.getByteValue("status") == Dictionary.PayStatusEnum.SUCCESS.getKey().byteValue()) {
					msg.setContent("提现成功:" + jsonObject.getString("remark"));
					this.userService.addWithdraw(withdraw.getUserId(), withdraw.getAmount());
				}
				withdraw.setStatus(jsonObject.getByte("status"));
				withdraw.setRemark(jsonObject.getString("remark"));
				msg.setUserId(withdraw.getUserId());
				this.siteMsgService.save(msg);
				this.withdrawService.save(withdraw);
			}

			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/admin/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询转盘配置")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "id为空新增, 有则修改", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/prize/query")
	public R queryPrize(Integer id, Integer pageNum, Integer pageSize) {
		try {
			PrizeConfig param = new PrizeConfig();
			param.setId(id);
			PageInfo<PrizeConfig> page = this.prizeConfigService.selectPageByCondition(param, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/prize/query 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "转盘配置修改")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "id为空新增, 有则修改", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/prize/save")
	public R savePrize(PrizeConfig config) {
		try {
			this.prizeConfigService.save(config);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/prize/save 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "首页统计")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "topId", value = "代理id", required = false, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/total")
	public R total(Long topId,HttpServletRequest request) {
		try {
			PayOrder condition = new PayOrder();
			condition.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
			TotalCountVo all = this.userService.totalByDate(topId, null, null);
			TotalCountVo today = this.payOrderService.totalByCondition(topId, condition, null, DateUtil.getDayFirstSecond(0), null);
//			TotalCountVo today = this.productActiveService.totalMoneyCount(topId, DateUtil.getDayFirstSecond(0), null);
			TotalCountVo todayRegister = this.userService.totalByDate(topId, DateUtil.getDayFirstSecond(0), null);
			TotalCountVo yesterday = this.payOrderService.totalByCondition(topId, condition, null, DateUtil.getDayFirstSecond(-1), DateUtil.getDayLastSecond(-1));
//			TotalCountVo yesterday = this.productActiveService.totalMoneyCount(topId, DateUtil.getDayFirstSecond(-1), DateUtil.getDayLastSecond(-1));
			TotalCountVo yesterdayRegister = this.userService.totalByDate(topId, DateUtil.getDayFirstSecond(-1), DateUtil.getDayLastSecond(-1));
//			TotalCountVo month = this.productActiveService.totalMoneyCount(topId, DateUtil.getMonthStart(new Date()), null);
			TotalCountVo month = this.payOrderService.totalByCondition(topId, condition, null, DateUtil.getMonthStart(new Date()), DateUtil.getMonthEnd(new Date()));
			TotalCountVo monthRegister = this.userService.totalByDate(topId, DateUtil.getMonthStart(new Date()), null);
			BigDecimal productMoney = this.userService.totalProductMoney(topId);
			TotalCountVo payingWithdraw = this.withdrawService.totalByDate(topId, Dictionary.PayStatusEnum.PAYING.getKey(), null, null);
			TotalCountVo successWithdraw = this.withdrawService.totalByDate(topId, Dictionary.PayStatusEnum.SUCCESS.getKey(), null, null);
			TotalCountVo todayWithdraw = this.withdrawService.totalByDate(topId, Dictionary.PayStatusEnum.SUCCESS.getKey(), DateUtil.getDayFirstSecond(0), null);
			TotalCountVo yesterdayWithdraw = this.withdrawService.totalByDate(topId, Dictionary.PayStatusEnum.SUCCESS.getKey(), DateUtil.getDayFirstSecond(-1), DateUtil.getDayLastSecond(-1));
			BigDecimal money = this.moneyLogService.totalByType(topId, null, Dictionary.MoneyTypeEnum.BUY.getKey(), null, null, null);
			Map<Integer, BigDecimal> todayCharge = this.payOrderService.totalMapByStatusAndDate(topId, Dictionary.PayStatusEnum.SUCCESS.getKey(), DateUtil.getDayFirstSecond(0), null);
			Map<Integer, BigDecimal> yesterdayCharge = this.payOrderService.totalMapByStatusAndDate(topId, Dictionary.PayStatusEnum.SUCCESS.getKey(), DateUtil.getDayFirstSecond(-1), DateUtil.getDayLastSecond(-1));
			Map<Integer, BigDecimal> monthCharge = this.payOrderService.totalMapByStatusAndDate(topId, Dictionary.PayStatusEnum.SUCCESS.getKey(), DateUtil.getMonthStart(new Date()), null);
			TotalVo vo = new TotalVo(all, today, yesterday, month, todayRegister, yesterdayRegister, monthRegister, productMoney, payingWithdraw, successWithdraw, todayWithdraw, yesterdayWithdraw, money, todayCharge, yesterdayCharge, monthCharge);
			return R.ok(vo);
		} catch (Exception e) {
			logger.error("/admin/total 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "/point/record/query")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "topId", value = "代理id", required = false, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/point/record/query")
	public R queryPointRecord(Long topId, PointRecord condition, Integer pageNum, Integer pageSize) {
		try {
			PageInfo<PointRecord> page = this.pointRecordService.selectPageByCondition(topId, condition, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			List<PointRecord> list = page.getList();
			for (PointRecord vo : list) {
				if (!StringUtils.isNull(vo.getTel()) && vo.getTel().length() >= 11) {
					vo.setTel(vo.getTel().substring(0, 3) + "****" + vo.getTel().substring(7));
				}
			}
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/point/record/query 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "/point/record/save")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "status", value = "1审核中 2成功 3失败", required = false, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/point/record/save")
	public R savePointRecord(Long id, Byte status) {
		try {
			if (id == null) {
				return R.fail(ERROR, "必须传id");
			}
			PointRecord pointRecord = this.pointRecordService.selectById(id);
			pointRecord.setStatus(status);
			this.pointRecordService.save(pointRecord);
			return R.ok(pointRecord);
		} catch (Exception e) {
			logger.error("/point/record/query 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "/chargeWithdraw/query")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "topId", value = "代理id", required = false, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/chargeWithdraw/query")
	public R queryPointRecord(Long topId, Long userId, String tel, Integer pageNum, Integer pageSize) {
		try {
			ChargeWithdrawInfo condition = new ChargeWithdrawInfo();
			condition.setUserId(userId);
			condition.setTel(tel);
			PageInfo<ChargeWithdrawInfo> page = this.chargeWithdrawInfoService.selectPageByCondition(topId, condition, pageNum, pageSize);
			List<ChargeWithdrawInfo> list = page.getList();
			for (ChargeWithdrawInfo vo : list) {
				if (!StringUtils.isNull(vo.getTel()) && vo.getTel().length() >= 11) {
					vo.setTel(vo.getTel().substring(0, 3) + "****" + vo.getTel().substring(7));
				}
			}
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/point/record/query 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "手动激活产品")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/product/activeManaual")
	@UserSyncLock(key="'Active:'"+"+#tel")
	public R activeManaualProduct(Integer productId, String tel, HttpServletRequest request,BigDecimal dynamicAmount) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无权限/product/activeManaual {}", adminUser);
				return R.fail(ERROR, "无操作权限，请重新登录");
			}
			User user = this.userService.selectByTel(tel);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			Product product = this.productService.selectById(productId);
			if (product == null) {
				return R.fail(ERROR, "产品不存在");
			}
			if(user.getProductMoney().compareTo(product.getPrice()) < 0) {
				return R.fail(ERROR, "用户余额不足，余: " + user.getProductMoney());
			}
			if ("动态金额".equals(product.getMemo1())) {
				if (dynamicAmount == null || dynamicAmount.compareTo(product.getPrice()) < 0) {
					return R.fail(ERROR, "输入金额有误");
				}
				BigDecimal[] result = dynamicAmount.divideAndRemainder(product.getPrice());
				if (result[1].compareTo(new BigDecimal(0)) != 0) {
					return R.fail(ERROR, "输入整数倍");
				}
				if (user.getProductMoney().compareTo(dynamicAmount) < 0) {
					return R.fail(ERROR, "用户余额不足，余: " + user.getProductMoney());
				}

				PayOrder order = new PayOrder();
				order.setOrderNum(System.currentTimeMillis() + "" + tel);
				order.setPayAmount(dynamicAmount);
				order.setProductid(productId);
				order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
				order.setType((byte) 0);
				order.setTypeDesc("手动激活: " + this.getIpAddr(request));
				order.setUserid(user.getId());
				this.payOrderService.save(order);
				user = this.userService.updateProductMoney(user.getId(), dynamicAmount, OUT, Dictionary.MoneyTypeEnum.BUY.getKey());
				this.productActiveService.activeDynamic(product.getId(), user, order.getType(),order);
				return R.ok(order);

			}else {
				PayOrder order = new PayOrder();
				order.setOrderNum(System.currentTimeMillis() + "" + tel);
				order.setPayAmount(product.getPrice());
				order.setProductid(productId);
				order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
				order.setType((byte) 0);
				order.setTypeDesc("手动激活: " + this.getIpAddr(request));
				order.setUserid(user.getId());
				this.payOrderService.save(order);
				user = this.userService.updateProductMoney(user.getId(), product.getPrice(), OUT, Dictionary.MoneyTypeEnum.BUY.getKey());
				this.productActiveService.active(product.getId(), user, order.getType(),order);
				return R.ok(order);
			}
		} catch (Exception e) {
			logger.error("/point/record/query 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询动态释放")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/product/release/query")
	public R queryProductRelease(ProductRelease param, Integer pageSize, Integer pageNum) {
		try {
			PageInfo<ProductRelease> page = this.productReleaseService.selectPageByCondition(param, pageSize == null ? DEFAULT_PAGE_SIZE : pageSize, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/point/record/query 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "批量修改动态释放")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "param", value = "json传(返回的排除时间字段)", required = false, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/product/release/update")
	public R updateProductRelease(String param) {
		try {
			List<ProductRelease> list = JSONArray.parseArray(param, ProductRelease.class);
			this.productReleaseService.saveList(list);
			return R.ok(list);
		} catch (Exception e) {
			logger.error("/point/record/query 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "购买送查询")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "productId", value = "产品id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "name", value = "名称", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "productMoney", value = "真钱", required = false, dataType = "decimal", dataTypeClass = BigDecimal.class), //
			@ApiImplicitParam(name = "money", value = "假钱", required = false, dataType = "decimal", dataTypeClass = BigDecimal.class), //
			@ApiImplicitParam(name = "point", value = "积分", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "productCount", value = "股权", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "promiseMoney", value = "承诺金", required = false, dataType = "decimal", dataTypeClass = BigDecimal.class), //
	})
	@PostMapping(value = "/product/prize/query")
	public R queryProductPrize(BuyPrize prize, Integer pageNum, Integer pageSize) {
		try {
			PageInfo<BuyPrize> page = this.buyPrizeService.selectPageByCondition(prize, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/point/record/query 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "购买送")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "不存在就增加, 存在修改", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "productId", value = "产品id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "name", value = "名称", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "productMoney", value = "真钱", required = false, dataType = "decimal", dataTypeClass = BigDecimal.class), //
			@ApiImplicitParam(name = "money", value = "假钱", required = false, dataType = "decimal", dataTypeClass = BigDecimal.class), //
			@ApiImplicitParam(name = "point", value = "积分", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "productCount", value = "股权", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "promiseMoney", value = "承诺金", required = false, dataType = "decimal", dataTypeClass = BigDecimal.class), //
	})
	@PostMapping(value = "/product/prize")
	public R productPrize(BuyPrize prize) {
		try {
			this.buyPrizeService.save(prize);
			return R.ok(prize);
		} catch (Exception e) {
			logger.error("/point/record/query 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}
	
	@Operation(summary = "通道配置")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/payDetail/save")
	public R payDetailSave(PayConfigDetail detail) {
		try {
			this.payConfigDetailService.save(detail);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/point/record/query 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}
	
	public void filterTel(List<User> list) {
		if (list != null && list.size() > 0) {
			for (User user : list) {
				if (!StringUtils.isNull(user.getTel()) && user.getTel().length() >= 11) {
					user.setTel(user.getTel().substring(0, 3) + "****" + user.getTel().substring(7));
				}
				if (!StringUtils.isNull(user.getUsername()) && user.getUsername().length() >= 11) {
					user.setUsername(user.getUsername().substring(0, 3) + "****" + user.getUsername().substring(7));
				}
				if (!StringUtils.isNull(user.getIdCard()) && user.getIdCard().length() >= 18) {
					user.setIdCard(user.getIdCard().substring(0, 10) + "****" + user.getIdCard().substring(14));
				}
			}
		}
	}

	public void filterTel(User user) {
		if (!StringUtils.isNull(user.getTel()) && user.getTel().length() >= 11) {
			user.setTel(user.getTel().substring(0, 3) + "****" + user.getTel().substring(7));
		}
		if (!StringUtils.isNull(user.getUsername()) && user.getUsername().length() >= 11) {
			user.setUsername(user.getUsername().substring(0, 3) + "****" + user.getUsername().substring(7));
		}
	}

	public void filterTelWithdraw(List<WithdrawVo> list) {
		if (list != null && list.size() > 0) {
			for (WithdrawVo user : list) {
				if (!StringUtils.isNull(user.getTel()) && user.getTel().length() >= 11) {
					user.setTel(user.getTel().substring(0, 3) + "****" + user.getTel().substring(7));
				}
				if (!StringUtils.isNull(user.getUsername()) && user.getUsername().length() >= 11) {
					user.setUsername(user.getUsername().substring(0, 3) + "****" + user.getUsername().substring(7));
				}
			}
		}
	}

	public void filterTelWithdrawVo(WithdrawVo user) {
		if (!StringUtils.isNull(user.getTel()) && user.getTel().length() >= 11) {
			user.setTel(user.getTel().substring(0, 3) + "****" + user.getTel().substring(7));
		}
		if (!StringUtils.isNull(user.getUsername()) && user.getUsername().length() >= 11) {
			user.setUsername(user.getUsername().substring(0, 3) + "****" + user.getUsername().substring(7));
		}
	}

	public void filterTelWithdraw(Withdraw user) {
		if (!StringUtils.isNull(user.getUsername()) && user.getUsername().length() >= 11) {
			user.setUsername(user.getUsername().substring(0, 3) + "****" + user.getUsername().substring(7));
		}
	}
}
