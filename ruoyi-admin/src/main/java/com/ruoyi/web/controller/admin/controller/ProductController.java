package com.ruoyi.web.controller.admin.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.*;
import com.ruoyi.system.zny.env.ZnyEnvironment;
import com.ruoyi.system.zny.service.*;
import com.ruoyi.system.zny.utils.DateUtil;
import com.ruoyi.system.zny.utils.EncUtil;
import com.ruoyi.system.zny.utils.MD5Util;
import com.ruoyi.system.zny.vo.MoneyLogVo;
import com.ruoyi.system.zny.vo.ProductActiveCount;
import com.ruoyi.system.zny.vo.ProductActiveVo;
import com.ruoyi.system.zny.vo.TotalCountVo;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
import com.ruoyi.web.controller.admin.constants.AppConstants;
import com.ruoyi.web.controller.admin.pay.*;
import com.ruoyi.web.controller.admin.vo.ProductVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.hibernate.validator.constraints.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.ruoyi.system.zny.constants.StatusCode.*;

@Api("产品配置")
@RestController
@Validated
@RequestMapping(value = "/product")
public class ProductController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private PayOrderService payOrderService;

	@Autowired
	private ProductActiveService productActiveService;

	@Autowired
	private PayConfigService payConfigService;

	@Autowired
	private PayConfigDetailService payConfigDetailService;

	@Autowired
	private ProductTotalService productTotalService;

	@Autowired
	private ProductChangeService productChangeService;

	@Autowired
	private ProductReleaseService productReleaseService;

	@Autowired
	private ReleaseMoneyLogService releaseMoneyLogService;

	@Autowired
	private ZnyEnvironment environmentUtils;


	@Autowired
	private CouponService couponService;

	@Operation(summary = "查询产品")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "status", value = "0回购(一星期释放), 1投资, 2理财", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/query")
	public R query(Integer id, Byte status, Byte type) {
		try {
			Product param = new Product();
			param.setId(id);
			param.setStatus(status);
			param.setType(type);
			List<Product> list = this.productService.selectByCondition(param);
			List<Product> result = new ArrayList<>();
			for (Product product : list) {
				if (product.getStatus() >= 0) {
					result.add(product);
				}
			}
			return R.ok(result);
		} catch (Exception e) {
			logger.error("/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "分页查询产品")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "status", value = "0禁止 1开启", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/queryPage")
	public R queryPage(Integer id, Byte status, Byte type, Byte type2, Byte type3, Byte type4, String name, Integer pageNum, Integer pageSize) {
		try {
			logger.info("id = {}, status = {}, type = {}, type2 = {}, type3 = {}, type4 = {}, name = {}, num = {}, size = {}.",
					id, status, type, type2, type3, type4, name, pageNum, pageSize);
			Product param = new Product();
			param.setId(id);
			param.setStatus(status);
			param.setType(type);
			param.setType2(type2);
			param.setType3(type3);
			param.setType4(type4);

			PageInfo<Product> page = this.productService.selectPageByCondition(param, name, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "删除商品")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/delById")
	public R delById(Integer id) {
		try {
			this.productService.deleteById(id);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "回购商品")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/back")
	public R backProduct(Integer id,HttpServletRequest request) {
		try {
			String sessionId = this.getSessionIdInHeader(request);
			AdminUser adminUser = AppConstants.adminSessionCache.getIfPresent(sessionId);
			if (adminUser == null || adminUser.getAgentId() != null) {
				logger.error("无权限 修改银行卡 {}", adminUser);
				return R.fail(ERROR, "无操作权限，请重新登录");
			}
			ProductActive productActive = productActiveService.selectById(id);
			if (productActive == null) {
				return R.fail(ERROR, "产品不存在");
			}
			productActiveService.backProduct(productActive);
			return R.fail(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}


	Cache<String,String> ip_limit_cache = CacheBuilder.newBuilder()
			.expireAfterAccess(5, TimeUnit.SECONDS)
			.build();


	@Operation(summary = "购买产品")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "产品id", required = true, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "type", value = "支付配置id, 0:余额支付, 1:微信  2:支付宝, 3:积分购买, 4:银联支付, 5:抢单余额支付, 6:下级返利赠送, 7:自定义金额购买", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "money", value = "购买金额", required = false, dataType = "decimal", dataTypeClass = BigDecimal.class), //
			})
	@PostMapping(value = "/buy")
	@UserSyncLock(key = "#userId")
	public R buy(Integer id, Long userId, Integer type, HttpServletRequest request,/*FZ购买参数 */BigDecimal applyMoney,@Length(max=64) String memo, BigDecimal money) {
		try {
			String ip = this.getIpAddr(request);
			logger.info("id = {}, userId = {}, type = {},applyMoney = {}, ip = {}", id, userId, type,applyMoney, ip);
			if (ip_limit_cache.getIfPresent(ip) != null) {
				return R.fail(ERROR, "请稍后重试下单");
			}
			ip_limit_cache.put(ip,System.currentTimeMillis()+"");
			String sessionId = this.getSessionIdInHeader(request);
			if (StringUtils.isNull(sessionId)) {
				return R.fail(ERROR, "请重新登录");
			}
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			String sessionIdInMap = AppConstants.sessionIdMap.get(user.getTel());
			sessionIdInMap = sessionIdInMap == null ? "" : sessionIdInMap;
			if (!sessionId.equals(sessionIdInMap)) {
				logger.error("tel = {}, sessionId = {}, sessionIdInMap = {}", userId, sessionId, sessionIdInMap);
				return R.fail(ERROR, "session不匹配请重新登录");
			}
			Product product = this.productService.selectById(id);
			if (product == null) {
				return R.fail(ERROR, "产品不存在");
			}
			if(product.getPrice().doubleValue() < 0) {
				return R.fail(ERROR, "产品无法购买");
			}
			/*if ( applyMoney != null && (applyMoney.doubleValue() < 0 || applyMoney.doubleValue() > Integer.MAX_VALUE)) {
				return result(ERROR, "申请金额有误");
			}*/

			if (user.getStatus() != null) {
				if (Dictionary.STATUS.DISABLE == user.getStatus()) {
					return R.fail(ERROR, "账号被禁用, 请联系客服!");
				}
			}
			if (product.getStatus() < Dictionary.STATUS.ENABLE) {
				return R.fail(ERROR, "该产品暂停购买");
			}
			//会员购买
			if(product.getType() == 6) {
				if (user.getVip() != 1) {
					return R.fail(ERROR, "仅限会员购买");
				}
			}
			if(environmentUtils.is("fz")) {
				int count = productTotalService.selectProductCount(id, user.getId());
				if (count > 0) {
					return R.fail(ERROR, "仅限购买一次");
				}
			}
			// 生成订单, 拉起支付
			PayOrder order = new PayOrder();
			order.setOrderNum(MD5Util.string2MD5(System.currentTimeMillis() + "" + user.getTel()));
			order.setPayAmount(product.getPrice());
			order.setProductid(id);
			order.setUserid(userId);
			order.setType(type.byteValue());
			if(product.getType() == 5) { //仅限基金兑换
				if (user.getSignMoney().compareTo(product.getPrice()) < 0) {
					return R.fail(ERROR, "兑换券不足");
				}
				order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
				order.setTypeDesc("兑换购买");
				this.payOrderService.save(order);
				user = this.userService.updateSignMoney(userId, product.getPrice(), OUT, Dictionary.MoneyTypeEnum.BUY.getKey(),"兑换购买");
				this.productActiveService.active(product.getId(), user, order.getType(),order);
				return R.ok(SUCCESS, OK);
			}
			if (type == 3 ) {
				if (user.getProductMoney().compareTo(product.getPrice()) < 0) {
					return R.fail(ERROR, "用户余额不足");
				}
				ProductActive activeCondition = new ProductActive();
				activeCondition.setProductId(product.getId());
				activeCondition.setUserId(user.getId());
				List<ProductActiveVo> voList = this.productActiveService.selectByCondition(activeCondition, null, null, null);
				if (CollectionUtil.isNotEmpty(voList)) {
					return R.fail(ERROR, "用户已经领取过");
				}
				TotalCountVo totalCountVo = this.userService.totalByParentIdAndVerify(user.getId(), null, null);
				if (totalCountVo.getCount() < product.getFouthValue().intValue()) {
					return R.fail(ERROR, "不满足条件");
				}
				order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
				order.setTypeDesc("推荐奖励");
				this.payOrderService.save(order);
				user = this.userService.updateProductMoney(userId, product.getPrice(), OUT, Dictionary.MoneyTypeEnum.BUY.getKey(), null, product.getId());
				this.productActiveService.active(product.getId(), user, order.getType(), order);
				return R.ok(SUCCESS, OK);
			}

			if (type == 0) {
				if (user.getProductMoney().compareTo(product.getPrice()) < 0) {
					return R.fail(ERROR, "用户余额不足");
				}
				// 过滤余额为0只能一次
				if (product.getPrice().compareTo(new BigDecimal(0)) == 0) {
					ProductActive activeCondition = new ProductActive();
					activeCondition.setUserId(userId);
					activeCondition.setProductId(product.getId());
					List<ProductActiveVo> voList = this.productActiveService.selectByCondition(activeCondition, null, null, null);
					if (CollectionUtil.isNotEmpty(voList)) {
						return R.fail(ERROR, "已经领取过");
					}
					TotalCountVo totalCountVo = this.userService.totalByParentIdAndVerify(user.getId(), null, null);
					if (totalCountVo.getCount() < product.getFouthValue().intValue()) {
						return R.fail(ERROR, "不满足条件");
					}
					order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
					order.setTypeDesc("推荐奖励");
					this.payOrderService.save(order);
					user = this.userService.updateProductMoney(userId, product.getPrice(), OUT, Dictionary.MoneyTypeEnum.BUY.getKey(), null, product.getId());
					this.productActiveService.active(product.getId(), user, order.getType(), order);
					return R.ok(SUCCESS, OK);
				}
				order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
				order.setTypeDesc("余额购买");
				this.payOrderService.save(order);
				user = this.userService.updateProductMoney(userId, product.getPrice(), OUT, Dictionary.MoneyTypeEnum.BUY.getKey(), null, product.getId());
				this.productActiveService.active(product.getId(), user, order.getType(), order);
				return R.ok(SUCCESS, OK);
			}
			if (type == 7) {
				if (user.getProductMoney().compareTo(money) < 0) {
					return R.fail(ERROR, "用户余额不足");
				}
				order.setPayAmount(money);
				order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
				order.setTypeDesc("投资金购买");
				this.payOrderService.save(order);
				user = this.userService.updateProductMoney(userId, money, OUT, Dictionary.MoneyTypeEnum.BUY.getKey(), product.getName(), product.getId());
				this.productActiveService.active(product.getId(), user, order.getType(),order);
				return R.fail(SUCCESS, OK);
			}
			order.setStatus(Dictionary.PayStatusEnum.PAYING.getKey());
			PayConfig config = this.payConfigService.selectOneAvaliable(id, type.byteValue(), Dictionary.STATUS.ENABLE);
			if (config == null) {
				return R.fail(ERROR, "支付配置不存在");
			}
			config.setUpdateTime(new Date());
			this.payConfigService.save(config);
			PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
			if (detail == null) {
				return R.fail(ERROR, "支付详情配置不存在");
			}
			Map<String, Object> result = new HashMap<>();
			if ("desheng".equals(detail.getType())) {
				result = DeShengPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("pubPay".equals(detail.getType())) {
				result = PubPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("huijinplay".equals(detail.getType())) {
				result = HuiJinPlay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("jinniu".equals(detail.getType())) {
				result = JinNiuPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail, ip);
			}
			if ("duola".equals(detail.getType())) {
				result = DuoLaPay.pay(order.getPayAmount(), order.getOrderNum(), config, detail, ip);
			}
			if ("haoyuan".equals(detail.getType()) || "mm".equals(detail.getType())) {
				result = JieXinPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("chuanhu".equals(detail.getType())) {
				result = ChuanHuPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("fenghong".equals(detail.getType())) {
				result = FengHongPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail, ip);
			}
			if ("ed".equals(detail.getType())) {
				result = EdPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("zonghe".equals(detail.getType())) {
				result = ZonghePay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("xuebi".equals(detail.getType())) {
				result = XuebiPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("kbao".equals(detail.getType())) {
				result = KbaoPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("wangzi".equals(detail.getType())) {
				result = WangZiPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("jiulong".equals(detail.getType())) {
				result = JiuLongPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("quansheng".equals(detail.getType())) {
				result = QuanSheng.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail, ip);
			}
			if ("qinchen".equals(detail.getType())) {
				result = QinChenPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail, ip);
			}
			if ("jintian".equals(detail.getType())) {
				result = JinTianPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail, ip);
			}
			if ("baishi".equals(detail.getType())) {
				result = BaiShiPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail, ip);
			}
			if ("qitian".equals(detail.getType())) {
				result = QiTianPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail, ip);
			}
			if ("hy".equals(detail.getType())) {
				result = HongYaPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("youyi".equals(detail.getType())) {
				result = YouYiPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("youqian".equals(detail.getType())) {
				result = YouQianPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			if ("quanshengkeji".equals(detail.getType())) {
				result = QuanShengPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail,ip);
			}
			if ("yypay".equals(detail.getType())) {
				result = YYPay.pay(order.getPayAmount(), order.getOrderNum(), config.getType(), detail);
			}
			order.setPayConfigId(config.getId());
			order.setTypeDesc(config.getName() + ":" + config.getType() + ":" + ip);
			if (ERROR.equals(result.get(STATUS) + "")) {
				return R.ok(result);
			}
			this.payOrderService.save(order);
			return R.ok(result);
		} catch (Exception e) {
			logger.error("/product/buy 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询支付配置")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "productId", value = "产品id", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/pay/query")
	public R queryPay(Integer productId) {
		try {
			PayConfig config = new PayConfig();
			config.setStatus(Dictionary.STATUS.ENABLE);
			config.setProductId(productId);
			List<PayConfig> list = this.payConfigService.selectByCondition(config);
			return R.ok(list);
		} catch (Exception e) {
			logger.error("/product/pay/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "统计投资记录")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "productId", value = "产品id", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/active/total")
	public R activeTotal(Long userId, Integer productId) {
		try {
			BigDecimal total = this.productActiveService.activeTotalByUserIdOrProductId(userId, productId);
			return R.ok(total);
		} catch (Exception e) {
			logger.error("/active/total 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "投资记录")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "tel", value = "手机号", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "productId", value = "产品id", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "productIds", value = "产品ids", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "pageNum", value = "页码", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "pageSize", value = "页条", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/active/query")
	public R queryActive(Integer id, Long userId,String tel, Integer productId, String productIds, //
							  Byte status,
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime, //
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime, //
			Integer pageNum, Integer pageSize) {
		try {
			ProductActive condition = new ProductActive();
			condition.setId(id);
			condition.setUserId(userId);
			condition.setProductId(productId);
			condition.setStatus(status);
			if(StringUtils.isNotEmpty(tel)) {
				condition.setTel(tel);
			}

			List<Integer> productIdList = null;
			if (StrUtil.isNotEmpty(productIds)) {
				productIdList = JSONArray.parseArray(productIds, Integer.class);
			}
			PageInfo<ProductActiveVo> page = this.productActiveService.selectPageByCondition(condition, startTime, endTime, productIdList, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/active/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		} finally {
		}
	}

	@Operation(summary = "投资记录")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/active/query2")
	public R queryActive2(Long userId) {
		try {
			ProductActive condition = new ProductActive();
			condition.setUserId(userId);
//			condition.setProductId(productId);
//			List<Integer> productIdList = null;
//			if (!StringUtil.isNull(productIds)) {
//				productIdList = JSONArray.parseArray(productIds, Integer.class);
//			}
			List<Integer> productIdList = Arrays.asList(1, 2, 3, 7);
			Date startTime = DateUtil.parse("yyyy-MM-dd HH:mm:ss", "2022-06-19 00:00:00");
			Date endTime = DateUtil.parse("yyyy-MM-dd HH:mm:ss", "2022-06-19 23:59:59");
			List<ProductActiveVo> list = this.productActiveService.selectByCondition(condition, productIdList, startTime, endTime);
			BigDecimal amount = new BigDecimal(0);
			if (CollectionUtil.isNotEmpty(list)) {
				for (ProductActiveVo vo : list) {
					amount = amount.add(vo.getProductMoney());
				}
			}
			return R.ok(amount);
		} catch (Exception e) {
			logger.error("/active/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		} finally {
		}
	}

	@Operation(summary = "第一次购买记录")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "userId", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "productIds", value = "productIds", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/active/first")
	public R first(Long userId, String productIds) {
		try {
			List<Integer> productIdList = JSONArray.parseArray(productIds, Integer.class);
			List<JSONObject> resultList = new ArrayList<>();
			for (Integer productId : productIdList) {
				ProductActive active = this.productActiveService.selectFirstByUserIdAndProductId(userId, productId);
				JSONObject result = new JSONObject();
				if (active != null && active.getId() != null) {
					result.put("certNum", EncUtil.toMD5(productId + "", 16));
					result.put("createTime", active.getCreateTime());
					result.put("productId", productId);
					resultList.add(result);
				}
			}
			return R.ok(resultList);
		} catch (Exception e) {
			logger.error("/active/first 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "最新购买记录")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "userId", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/active/lastone")
	public R queryPay(Long userId) {
		try {
			Product param = new Product();
			List<Product> list = this.productService.selectByCondition(param);
			Map<Integer, Long> productTimeMap = new HashMap<>();
			for (Product product : list) {
				ProductActive active = this.productActiveService.selectLastOneByUserIdAndProductId(userId, product.getId());
				if (active != null) {
					productTimeMap.put(active.getProductId(), active.getCreateTime().getTime());
				}
			}
			return R.ok(productTimeMap);
		} catch (Exception e) {
			logger.error("/active/lastone 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "购买统计")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "parent2Id", value = "上2级id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "parentId", value = "上级id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "userId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "productId", value = "产品id", required = false, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/total/query")
	public R totalQuery(ProductTotal condition, Integer pageNum, Integer pageSize) {
		try {
			PageInfo<ProductTotal> page = this.productTotalService.selectPageByCondition(condition, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/total/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "购买人数数量")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "parentId", value = "上级id", required = false, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/total/personByParentId")
	public R personByParentId(Long parentId) {
		try {
			List<ProductActiveCount> count = this.productActiveService.selectBuyPersonByParentId(parentId, null, null);
			return R.ok((count == null || count.size() <= 0) ? 0 : count.get(0).getCount());
		} catch (Exception e) {
			logger.error("/total/personByParentId 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "购买数量")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "parent2Id", value = "上2级id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "parentId", value = "上级id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "userId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "productId", value = "产品id", required = false, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/total/count")
	public R totalCount(ProductTotal condition, Integer pageNum, Integer pageSize) {
		try {
			logger.info("userId = {}, product = {}, parentId = {}, parent2Id = {}.", condition.getUserId(), condition.getProductId(), condition.getParentId(), condition.getParent2Id());
			List<Product> list = this.productService.selectByCondition(null);
			List<ProductVo> voList = new ArrayList<>();
			ProductTotal total = null;
			for (Product product : list) {
				ProductVo vo = new ProductVo();
				BeanUtils.copyProperties(product, vo);
				total = new ProductTotal();
				total.setUserId(condition.getUserId());
				total.setProductId(vo.getId().longValue());
				List<ProductTotal> totalList = this.productTotalService.selectByCondition(total);
				if (CollectionUtil.isEmpty(totalList)) {
					vo.setNum(0);
				} else {
					total = totalList.get(0);
					vo.setNum(total.getNum());
				}
				voList.add(vo);
			}
			return R.ok(voList);
		} catch (Exception e) {
			logger.error("/total/count 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "兑换")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "num", value = "兑换的数量", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/change2")
	@UserSyncLock(key = "#userId")
	public R change2(Long userId, Integer num, HttpServletRequest request) {
		try {
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			if (user.getProductCount() == null || user.getProductCount().intValue() < num.intValue()) {
				return R.fail(ERROR, "数量不足");
			}
			this.userService.updateProductCount(user.getId(), num, OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey());
			this.userService.updateProductMoney(user.getId(), new BigDecimal(num).multiply(new BigDecimal(50000)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey());
//			this.userService.updatePoint(user.getId(), num * 50, null, IN, Dictionary.MoneyTypeEnum.CHANGE.getKey());
			UserController.filterTel(user);
			return R.ok(user);
		} catch (Exception e) {
			logger.error("/change2 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "兑换")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "productId", value = "产品id", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "num", value = "兑换的数量", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/change")
	@UserSyncLock(key = "#userId")
	public R change(Long userId, Integer productId, Integer num, HttpServletRequest request) {
		try {
			logger.info("change userId = {}, productId = {}, num = {}", userId, productId, num);
			if (userId == null) {
				return R.fail(ERROR, "还未开启兑换");
			}
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			String sessionId = this.getSessionIdInHeader(request);
			if (StringUtils.isNull(sessionId)) {
				return R.fail(ERROR, "请重新登录");
			}
			String sessionIdInMap = AppConstants.sessionIdMap.get(user.getTel());
			sessionIdInMap = sessionIdInMap == null ? "" : sessionIdInMap;
			if (!sessionId.equals(sessionIdInMap)) {
				logger.error("tel = {}, sessionId = {}, sessionIdInMap = {}", userId, sessionId, sessionIdInMap);
				return R.fail(ERROR, "session不匹配请重新登录");
			}
			Product product = this.productService.selectById(productId);
			ProductTotal total = new ProductTotal();
			total.setUserId(userId);
			total.setProductId(productId.longValue());
			List<ProductTotal> list = this.productTotalService.selectByCondition(total);
			if (list == null || list.size() <= 0) {
				return R.fail(ERROR, "不存在产品购买记录");
			}
			total = list.get(0);
			if (total.getNum() < num) {
				return R.fail(ERROR, "兑换的数量不够");

			}
			ProductChange change = new ProductChange();
			if (total.getNum() < 1) {
				return R.fail(ERROR, "产品数量不足");
			}
			total.setNum(total.getNum() - num);
			total.setProductMoney(total.getProductMoney().subtract(product.getFouthRealValue().multiply(new BigDecimal(num))));
//			total.setProductCount(total.getProductCount() - (product.getProductCount());
//			total.setPoint(total.getPoint() - Integer.valueOf(product.getMemo1()));
			this.productTotalService.save(total);
			change.setName(num + "");
			change.setPrice(product.getFouthRealValue().multiply(new BigDecimal(num)));
			change.setProductId(productId);
			change.setProductName(product.getName());
			change.setTel(user.getTel());
			change.setUserId(userId);
			this.productChangeService.save(change);
			user.setProductMoney(user.getProductMoney().add(product.getFouthRealValue().multiply(new BigDecimal(num))));
			this.userService.update(user);
			return R.ok(change);
		} catch (Exception e) {
			logger.error("/change 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "兑换查询")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "productId", value = "产品id", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/change/query")
	@UserSyncLock(key = "#userId")
	public R changeQuery(Long userId, Integer productId, Integer pageNum, Integer pageSize) {
		try {
			ProductChange condition = new ProductChange();
			condition.setUserId(userId);
			condition.setProductId(productId);
			PageInfo<ProductChange> page = this.productChangeService.selectPageByCondition(condition, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/change/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "动态红利统计")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/active/realTotal")
	public R realTotal(Long userId) {
		try {
			BigDecimal amount = this.productActiveService.totalUserProductMoneyByUserId(userId);
			return R.ok(amount);
		} catch (Exception e) {
			logger.error("/active/realTotal 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "动态红利记录")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/active/realRecord")
	public R realRecord(Long userId, Integer pageNum, Integer pageSize) {
		try {
			ProductActive condition = new ProductActive();
			condition.setUserId(userId);
			PageInfo<ProductActiveVo> page = this.productActiveService.selectPageByCondition(condition, null, null, null, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/change/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "手动释放1")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/active/release1")
	public R activeRelease1(Long userId, Integer pageNum, Integer pageSize) {
		try {
			Long DAY = 24 * 60 * 60 * 1000l;
			// 每天返利
			List<ProductActiveProcess> processList = this.productActiveService.selectProductActiveProcess();
			List<ProductActive> list = this.productActiveService.selectRateList(null);
			for (ProductActive productActive : list) {
				if ((System.currentTimeMillis() - productActive.getCreateTime().getTime()) >= DAY) {
					logger.info("id = {}, time = {}", productActive.getId(), System.currentTimeMillis() - productActive.getCreateTime().getTime());
					this.productActiveService.periodRateByDay(productActive, processList,new BigDecimal(0));
				}
			}
			// 每日实名奖励
/*			List<User> userList = this.userService.selectByIdCardNotNull();
			for (User user : userList) {
				this.userService.updateProductMoney(user.getId(), new BigDecimal(38), IN, Dictionary.MoneyTypeEnum.RELEASE_MONEY2.getKey());
			}*/
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/active/release 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "手动释放2")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/active/release2")
	public R activeRelease2(Long userId, Integer pageNum, Integer pageSize) {
		try {
			List<ProductActiveVo> list = this.productActiveService.selectByCondition(null, null, null, null);
			ProductRelease condition = null;
			List<ProductRelease> releaseList = this.productReleaseService.selectByCondition(null);
			Map<String, ProductRelease> releaseMap = new HashMap<>();
			for (ProductRelease vo : releaseList) {
				releaseMap.put(vo.getProductId() + "-" + vo.getDay(), vo);
			}
			// 静态 & 动态红利
			BigDecimal dongtaiRate = null;
			User user = null;
			for (ProductActiveVo vo : list) {
				if (vo.getMoney() != null && vo.getMoney().compareTo(new BigDecimal(0)) > 0) {
					this.userService.updateMoney(vo.getUserId(), vo.getMoney(), IN, Dictionary.MoneyTypeEnum.RELEASE_MONEY.getKey(), "静态红利");
				}
				// 动态
				condition = releaseMap.get(vo.getProductId() + "-" + (vo.getCount() + 1));
				if (condition == null) {
					logger.error("productId = {}, day = {}, 找不到配置", vo.getProductId(), vo.getCount() + 1);
					continue;
				}
				user = this.userService.selectById(vo.getUserId());
				if (user == null) {
					logger.error("用户不存在 userId = {}", vo.getUserId());
					continue;
				}
				dongtaiRate = user.getMoney().multiply(condition.getRate().multiply(new BigDecimal(0.01)));
//				vo.setUserProductMoney((vo.getUserProductMoney() == null ? new BigDecimal(0) : vo.getUserProductMoney()).add(dongtaiRate));
//				vo.setCount(vo.getCount() + 1);
				logger.info("userId = {}, dongtai = {}", vo.getUserId(), dongtaiRate);
				this.userService.updateDongTaiReleaseProductMoney(vo, dongtaiRate, IN, Dictionary.MoneyTypeEnum.RELEASE_PROMISE_MONEY.getKey(), vo.getId() + "");
			}
			// 静态
//			this.productActiveService.periodRateByDay(vo);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/active/release2 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "动态红利记录")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/active/dongtai")
	public R activeDongtai(Long userId, //
								@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime, //
								@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime, //
								Integer pageNum, Integer pageSize) {
		try {
			ReleaseMoneyLog param = new ReleaseMoneyLog();
			param.setUserId(userId);
			PageInfo<MoneyLogVo> page = this.releaseMoneyLogService.selectPageByCondition(null, param, null, null, null, startTime, endTime, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/active/dongtai 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "动态红利转录")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/active/transfer")
	public R activeDongtai(Long userId, Integer id) {
		try {
			ProductActive active = this.productActiveService.selectById(id);
			if (active == null) {
				return R.fail(ERROR, "此产品不存在");
			}
			if (active.getUserProductMoney().compareTo(new BigDecimal(0)) <= 0) {
				return R.fail(ERROR, "金额不符合");
			}
			User user = this.userService.selectById(active.getUserId());
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			if (userId.longValue() != user.getId().longValue()) {
				return R.fail(ERROR, "用户不匹配");
			}
			BigDecimal amount = active.getUserProductMoney();
			BigDecimal money = user.getMoney();
			logger.info("转账 userId = {}, productId = {}, userProductMoney = {}, money = {}", userId, id, amount, money);
			this.userService.updateDongTaiReleaseProductMoney(active, amount, OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "转账到余额");
			this.userService.updateProductMoney(userId, amount, IN, Dictionary.MoneyTypeEnum.CHANGE.getKey());
			this.userService.updatePromiseMoney(userId, money, IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "已经提现过的静态收益");
			this.userService.updateMoney(userId, money, OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "重新累计");
			return R.ok(active.getUserProductMoney());
		} catch (Exception e) {
			logger.error("/active/transfer 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}


	@Operation(summary = "云家园查询房产证")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class)
	})
	@PostMapping(value = "/ysjy/property")
	public R zt(Long userId) {
		try {
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			ProductTotal total = new ProductTotal();
			total.setUserId(userId);
			List<ProductTotal> totalList = this.productTotalService.selectByCondition(total);
			if (totalList.size() == 0) {
				return R.fail(OK,"无激活产品");
			}
			JSONArray array = new JSONArray();
			for (ProductTotal productTotal : totalList) {
				if((productTotal.getProductId() == 1 || productTotal.getProductId() == 2)) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("name", productTotal.getProductName());
					jsonObject.put("productId", productTotal.getProductId());
					jsonObject.put("money", productTotal.getProductMoney().divide(new BigDecimal(productTotal.getNum())).intValue());
					jsonObject.put("count", productTotal.getNum());
					jsonObject.put("title", "产权证");
					jsonObject.put("createtime", DateUtil.format(DateUtil.YY_MM_DD_HH_MM_SS,productTotal.getCreateTime()));
				}
			}
			return R.ok(array);
		} catch (Exception e) {
			logger.error("/product/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}
}
