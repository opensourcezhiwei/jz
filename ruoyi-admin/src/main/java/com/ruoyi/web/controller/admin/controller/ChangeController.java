package com.ruoyi.web.controller.admin.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.*;
import com.ruoyi.system.zny.service.*;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
import com.ruoyi.web.controller.admin.constants.AppConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.*;

@Api("兑换")
@RestController
@RequestMapping(value = "/change")
public class ChangeController extends BaseController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private UserService userService;

    @Resource
    private ProductTotalService productTotalService;

    @Resource
    private ProductService productService;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private BankCardService bankCardService;

    @Resource
    private WithdrawService withdrawService;

    @Resource
    private HotService hotService;

    @Resource
    private PointRecordService pointRecordService;


    @Operation(summary = "gyqq兑换")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "num", value = "兑换的数量", required = true, dataType = "int", dataTypeClass = Integer.class), //
            @ApiImplicitParam(name = "type", value = "1: gpw(money), 2: hpw(productCount)", required = true, dataType = "int", dataTypeClass = Integer.class), //
    })
    @PostMapping(value = "/gyqq")
    @UserSyncLock(key = "#userId")
    public R gyqq(Long userId, Integer num, Integer type) {
        try {
            if (num == null || num <= 0 || type == null) {
                return R.fail("兑换数量不足");
            }
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail("用户不存在");
            }
            if (1 == type.intValue()) {
                if (user.getMoney() == null || user.getMoney().compareTo(new BigDecimal(num)) < 0) {
                    return R.fail("兑换数量不足");
                }
                this.userService.updateMoney(userId, new BigDecimal(num), OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "GPW币兑换");
                this.userService.updateProductMoney(userId, new BigDecimal(5000).multiply(new BigDecimal(num)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "GPW币兑换");
            }
            if (2 == type.intValue()) {
                if (user.getProductCount() == null || user.getProductCount().intValue() < num.intValue()) {
                    return R.fail("兑换数量不足");
                }
                this.userService.updateProductCount(userId, num, OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "HPW币兑换");
                this.userService.updateProductMoney(userId, new BigDecimal(10000).multiply(new BigDecimal(num)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "HPW币兑换");
            }
            return R.ok(true);
        } catch (Exception e) {
            logger.error("/product/query 出错:", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "yjr兑换")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "num", value = "兑换的数量", required = true, dataType = "int", dataTypeClass = Integer.class), //
            @ApiImplicitParam(name = "type", value = "1: wxb(signMoney), 2: hjb(promiseMoney黄金币), 3: productCount现金币", required = true, dataType = "int", dataTypeClass = Integer.class), //
    })
    @PostMapping(value = "/yjr")
    @UserSyncLock(key = "#userId")
    public R yjr(Long userId, Integer num, Integer type) {
        try {
            if (userId != null) {
                return R.fail("集团上市后可出售股权");
            }
            logger.info("userId = {}, num = {}, type = {}.", userId, num, type);
            if (num == null || num <= 0 || type == null) {
                return R.fail("兑换数量不足");
            }
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail("用户不存在");
            }
            if (1 == type) {
                if (user.getSignMoney() == null || user.getSignMoney().compareTo(new BigDecimal(num)) < 0) {
                    return R.fail("兑换数量不足");
                }
                user.setSignMoney(user.getSignMoney().subtract(new BigDecimal(num)));
                this.userService.update(user);
                this.userService.updateProductMoney(user.getId(), new BigDecimal(num).multiply(new BigDecimal(10000)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey());
//				this.userService.updateMoney(userId, new BigDecimal(num), OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "GPW币兑换");
//				this.userService.updateProductMoney(userId, new BigDecimal(5000).multiply(new BigDecimal(num)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "GPW币兑换");
            }
            if (2 == type) {
                if (user.getPromiseMoney() == null || user.getPromiseMoney().intValue() < num) {
                    return R.fail("兑换数量不足");
                }
                this.userService.updatePromiseMoney(userId, new BigDecimal(num), OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "黄金币兑换");
                this.userService.updateProductMoney(userId, new BigDecimal(10000).multiply(new BigDecimal(num)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "黄金币兑换");
            }
            if (3 == type) {
                if (user.getProductCount() == null || user.getProductCount() < num) {
                    return R.fail("兑换数量不足");
                }
                this.userService.updateProductCount(userId, num, OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey());
                List<SysConfig> sysConfigs = this.sysConfigService.selectByType(7);
                if (CollectionUtil.isEmpty(sysConfigs)) {
                    this.userService.updateProductMoney(userId, new BigDecimal(num).multiply(new BigDecimal(10000)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey());
                } else {
                    SysConfig sysConfig = sysConfigs.get(0);
                    this.userService.updateProductMoney(userId, new BigDecimal(num).multiply(new BigDecimal(sysConfig.getName()), MathContext.DECIMAL32), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey());
                }
            }
            return R.ok(true);
        } catch (Exception e) {
            logger.error("/product/query 出错:", e);
            return R.fail( "服务器出错");
        }
    }

    @Operation(summary = "myl兑换")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "num", value = "兑换的数量", required = true, dataType = "int", dataTypeClass = Integer.class), //
            @ApiImplicitParam(name = "type", value = "11:(promiseMoney社保金)换现金，12换基金券, 21: 债券(point)换现金 22换基金券", required = true, dataType = "int", dataTypeClass = Integer.class), //
    })
    @PostMapping(value = "/myl")
    @UserSyncLock(key = "#userId")
    public R myl(Long userId, Integer num, Integer type) {
        try {
            logger.info("myl兑换 userId={},num={},type={}", userId, num, type);
            if (num == null || num <= 0 || type == null) {
                return R.fail( "兑换数量不足");
            }
            if (num % 100 != 0) {
                return R.fail("请输入一个整数");
            }
            User user = this.userService.selectById(userId);

            if (user == null) {
                return R.fail( "用户不存在");
            }
            if (11 == type) {
                if (user.getPromiseMoney() == null || user.getPromiseMoney().intValue() < num.intValue()) {
                    return R.fail( "兑换数量不足");
                }
                this.userService.updatePromiseMoney(userId, new BigDecimal(num), OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "社保金兑换债券");
                this.userService.updateProductMoney(userId, new BigDecimal(num).divide(new BigDecimal(100)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "社保金兑基金券");
            }
            if (12 == type) {
                if (user.getPromiseMoney() == null || user.getPromiseMoney().intValue() < num.intValue()) {
                    return R.fail("兑换数量不足");
                }
                this.userService.updatePromiseMoney(userId, new BigDecimal(num), OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "社保金兑换现金");
                this.userService.updateSignMoney(userId, new BigDecimal(num).divide(new BigDecimal(100)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "社保金兑换现金");
            }
            if (21 == type) {
                if (user.getPoint() == null || user.getPoint().intValue() < num.intValue()) {
                    return R.fail( "兑换数量不足");
                }
                this.userService.updatePoint(user.getId(), num, new BigDecimal(0), OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey());
                this.userService.updateProductMoney(userId, new BigDecimal(num).divide(new BigDecimal(100)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "债券兑换基金券");
            }
            if (22 == type) {
                if (user.getPoint() == null || user.getPoint().intValue() < num.intValue()) {
                    return R.fail( "兑换数量不足");
                }
                this.userService.updatePoint(user.getId(), num, new BigDecimal(0), OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey());
                this.userService.updateSignMoney(userId, new BigDecimal(num).divide(new BigDecimal(100)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "债券兑换现金");
            }
            return R.ok(true);
        } catch (Exception e) {
            logger.error("myl兑换出错:", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "point兑换")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "point", value = "积分", required = true, dataType = "int", dataTypeClass = Integer.class), //
    })
    @PostMapping(value = "/point")
    @UserSyncLock(key = "#userId")
    public R zt(Long userId, Integer point) {
        try {
            logger.info("pointChange userId = {}, point = {}", userId, point);
            if (point == null || point <= 0) {
                return R.fail("请填写正确兑换数量");
            }
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail("用户不存在");
            }
            if (user.getPoint() == null || user.getPoint().intValue() < point) {
                return R.fail( "超出可兑换额度");
            }
/*			List<SysConfig> list = this.sysConfigService.selectByType(SysConfigService.SysConfigType.SysConfigALl.getType());
			if(list.size() != 0) {
				SysConfig config = list.get(0);
				String desc = config.getDesc();
				desc.split("&").;*/
            this.userService.updatePoint(userId, point, null, OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey());
            this.userService.updateMoney(userId, new BigDecimal(point).multiply(new BigDecimal(430)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "兑换");
            //}
            UserController.filterTel(user);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("/product/query 出错:", e);
            return R.fail("服务器出错");
        }
    }


    @Operation(summary = "YJJB兑换")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "productId", value = "产品id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "num", value = "兑换数量", required = true, dataType = "int", dataTypeClass = Integer.class), //

    })
    @PostMapping(value = "/yjjb")
    @UserSyncLock(key = "#userId")
    public R zt(Long userId, Long productId, Integer num) {
        try {
            logger.info("yjjb userId = {}, productId = {}, num = {}", userId, productId, num);
            if (num == null || num <= 0) {
                return R.fail(ERROR, "请填写正确兑换数量");
            }
            if (productId == null || productId <= 0) {
                return R.fail(ERROR, "产品不存在");
            }
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail(ERROR, "用户不存在");
            }
            ProductTotal total = new ProductTotal();
            total.setUserId(userId);
            total.setProductId(productId);
            List<ProductTotal> totalList = this.productTotalService.selectByCondition(total);
            if (totalList.size() == 0) {
                return R.fail(ERROR, "金币数量不足");
            }
            ProductTotal entity = totalList.get(0);
            if (entity.getNum() < num) {
                return R.fail(ERROR, "金币数量不足");
            }
            Product product = this.productService.selectById(productId.intValue());
            this.userService.updateProductMoney(userId, product.getThirdValue().multiply(new BigDecimal(num)), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "兑换" + product.getMemo1() + ": " + num + "枚");
            entity.setNum(entity.getNum() - num);
            productTotalService.save(entity);
            return R.fail(user);
        } catch (Exception e) {
            logger.error("/product/query 出错:", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }


    @Operation(summary = "bgy兑换")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "productId", value = "产品id", required = true, dataType = "long", dataTypeClass = Long.class), //

    })
    @PostMapping(value = "/bgy")
    @UserSyncLock(key = "#userId")
    public R bgy(Long userId, Integer productId) {
        try {
            logger.info("bgy userId = {}, productId = {}", userId, productId);
            if (userId == null || productId == null || productId < 0) {
                return R.fail(ERROR, "参数错误");
            }
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail(ERROR, "用户不存在");
            }
            List<SysConfig> sysConfigs = this.sysConfigService.selectByType(SysConfigService.SysConfigType.PointShop.getType());
            if (sysConfigs.size() == 0) {
                return R.fail(ERROR, "商城关闭");
            }
            SysConfig sysConfig = sysConfigs.get(0);
            if (sysConfig.getStatus() == 0 || StringUtils.isEmpty(sysConfig.getUrl())) {
                return R.fail(ERROR, "商城关闭");
            }
            BankCard param = new BankCard();
            param.setTel(user.getTel());
            List<BankCard> bankCards = bankCardService.selectByCondition(param);
            if (bankCards.size() == 0) {
                return R.fail(ERROR, "未绑定银行卡");
            }
            BankCard bankCard = bankCards.get(0);
            JSONArray array = JSONArray.parseArray(sysConfig.getUrl());
            if (productId > array.size()) {
                return R.fail(ERROR, "参数错误");
            }
            JSONObject jsonObject = (JSONObject) array.get(productId - 1);
            int point = jsonObject.getIntValue("point");
            int amount = jsonObject.getIntValue("product");
            if (user.getPromiseMoney().compareTo(new BigDecimal(point)) < 0) {
                return R.fail(ERROR, "积分不足");
            }
            if (!jsonObject.containsKey("type")) {
                if (user.getMoney().compareTo(new BigDecimal(amount)) < 0) {
                    return R.fail(ERROR, "红包不足");
                }
                this.userService.updateMoney(user.getId(), new BigDecimal(amount), OUT, Dictionary.MoneyTypeEnum.WITHDRAW.getKey(), "红包提现");
                this.userService.updatePromiseMoney(user.getId(), new BigDecimal(point), OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "红包提现");
                this.userService.updateProductMoney(user.getId(), new BigDecimal(amount), IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "红包兑换");

				/*Withdraw withdraw = new Withdraw();
				withdraw.setAmount(new BigDecimal(amount));
				withdraw.setBankCardId(bankCard.getId());
				withdraw.setBankCardInfo(bankCard.getBankAccount()+"|"+bankCard.getBankName()+"|"+bankCard.getBankNo());
				withdraw.setCharge(user.getCharge());
				List<User> list = this.userService.selectByParentId(userId);
				withdraw.setChildrenCharge(new BigDecimal(0));
				if (list != null && list.size() > 0) {
					for (User user2 : list) {
						withdraw.setChildrenCharge(withdraw.getChildrenCharge().add(user2.getCharge()));
					}
				}
				withdraw.setChildren(list == null ? 0 : list.size());

				withdraw.setRate(new BigDecimal(1));
				withdraw.setRealAmount(new BigDecimal(amount));
				withdraw.setStatus(Dictionary.PayStatusEnum.PAYING.getKey());
				withdraw.setType((byte) 3);
				withdraw.setUserId(userId);
				withdraw.setUsername(user.getRealname());
				this.withdrawService.save(withdraw);*/
                return R.ok(user);
            } else {
                if (StringUtils.isEmpty(user.getHome())) {
                    return R.fail(ERROR, "未填写地址");
                }
                if (user.getPoint().compareTo(amount) < 0) {
                    return R.fail(ERROR, "大米不足");
                }
                this.userService.updatePoint(user.getId(), amount, new BigDecimal(amount), OUT, "提现" + amount + "斤大米");
                this.userService.updatePromiseMoney(user.getId(), new BigDecimal(point), OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "大米提取");
                Withdraw withdraw = new Withdraw();
                withdraw.setAmount(new BigDecimal(amount));
                withdraw.setBankCardId(bankCard.getId());
                withdraw.setBankCardInfo(user.getHome());
                withdraw.setCharge(user.getCharge());
                List<User> list = this.userService.selectByParentId(userId);
                withdraw.setChildrenCharge(new BigDecimal(0));
                if (list != null && list.size() > 0) {
                    for (User user2 : list) {
                        withdraw.setChildrenCharge(withdraw.getChildrenCharge().add(user2.getCharge()));
                    }
                }
                withdraw.setChildren(list == null ? 0 : list.size());

                withdraw.setRate(new BigDecimal(1));
                withdraw.setRealAmount(new BigDecimal(amount));
                withdraw.setStatus(Dictionary.PayStatusEnum.PAYING.getKey());
                withdraw.setType((byte) 4);
                withdraw.setUserId(userId);
                withdraw.setUsername(user.getRealname());
                this.withdrawService.save(withdraw);
                return R.ok(user);
            }

        } catch (Exception e) {
            logger.error("/product/query 出错:", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }

    @Operation(summary = "whh兑换")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "hotId", value = "热门产品id", required = true, dataType = "int", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "type", value = "1:积分兑换 2:积分换钱", required = true, dataType = "int", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/whh")
    @UserSyncLock(key = "#userId")
    public R whh(Long userId, Integer hotId, Integer type, HttpServletRequest request) {
        try {
            logger.info("兑换 whh uerId = {}, hotId = {}.", userId, hotId);
            String sessionId = this.getSessionIdInHeader(request);
            if (com.ruoyi.common.utils.StringUtils.isNull(sessionId)) {
                return R.fail(ERROR, "请重新登录");
            }
            Hot hot = this.hotService.selectById(hotId);
            if (hot == null) {
                return R.fail(ERROR, "产品不存在");
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
            if (StrUtil.isEmpty(user.getHome())) {
                return R.fail(ERROR, "请填写收货地址");
            }
            if (user.getPoint() < Integer.parseInt(hot.getPrice())) {
                return R.fail(ERROR, "积分不够");
            }
            BigDecimal amount = new BigDecimal(0);
            byte status = 1;  // 审核中
            if (type == 2) {
                // 增加记录
                List<SysConfig> configList = this.sysConfigService.selectByType(7);
                if (CollectionUtil.isEmpty(configList)) {
                    logger.error("尚未配置换钱比例");
                    return R.fail(ERROR, "尚未配置换钱比例");
                }
                logger.info("price = {}, desc = {}.", hot.getPrice(), configList.get(0).getDesc());
                amount = new BigDecimal(hot.getPrice()).multiply(new BigDecimal(configList.get(0).getDesc()));
                status = 2; // 审核成功
            }
            // 扣减积分
            this.userService.updatePoint(user.getId(), Integer.valueOf(hot.getPrice()), amount, OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), hot.getName(), user.getHome(), status);
            this.userService.updateProductMoney(user.getId(), amount, IN, Dictionary.MoneyTypeEnum.CHANGE.getKey());
            return R.ok(SUCCESS, OK);
        } catch (Exception e) {
            logger.error("/change/whh 出错:", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }

}
