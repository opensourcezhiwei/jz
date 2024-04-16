package com.ruoyi.web.controller.admin.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.BankCard;
import com.ruoyi.system.zny.entity.SysConfig;
import com.ruoyi.system.zny.entity.User;
import com.ruoyi.system.zny.entity.Withdraw;
import com.ruoyi.system.zny.service.*;
import com.ruoyi.system.zny.utils.DateUtil;
import com.ruoyi.system.zny.vo.ProductActiveCount;
import com.ruoyi.system.zny.vo.WithdrawVo;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
import com.ruoyi.web.controller.admin.constants.AppConstants;
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
import java.util.ArrayList;
import java.util.List;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.*;

@RestController
@RequestMapping(value = "/withdraw")
public class WithdrawController extends BaseController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserService userService;

    @Autowired
    private BankCardService bankCardService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private WithdrawService withdrawService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductActiveService productActiveService;

    @Operation(summary = "申请提现")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "amount", value = "提现金额", required = true, dataType = "decimal", dataTypeClass = BigDecimal.class), //
            @ApiImplicitParam(name = "bankCardId", value = "银行卡id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "type", value = "1提现 0签到提现", required = true, dataType = "int", dataTypeClass = Integer.class), //
            @ApiImplicitParam(name = "tradePassword", value = "交易密码", required = false, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/apply")
    @UserSyncLock(key = "#userId")
    public R applyWithdraw(Long userId, Long bankCardId, BigDecimal amount, String tradePassword, HttpServletRequest request) {
        try {
            if (userId == null || bankCardId == null || amount == null || amount.compareTo(new BigDecimal(0)) <= 0) {
                return R.fail(ERROR, "无效参数");
            }
            String sessionId = this.getSessionIdInHeader(request);
            if (StringUtils.isNull(sessionId)) {
                return R.fail(ERROR, "请重新登录");
            }
            logger.info("/withdraw/apply userId = {}, bankCardId = {}, amount = {}, tradePassword = {}, sessionId = {}, ip = {}", userId, bankCardId, amount, tradePassword, sessionId, this.getIpAddr(request));
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail(ERROR, "用户不存在");
            }
            if (user.getStatus() == Dictionary.STATUS.DISABLE) {
                return R.fail(ERROR, "禁止提现");
            }
            String sessionIdInMap = AppConstants.sessionIdMap.get(user.getTel());
            sessionIdInMap = sessionIdInMap == null ? "" : sessionIdInMap;
            if (!sessionId.equals(sessionIdInMap)) {
                logger.error("session expired. /withdraw/apply tel = {}, sessionId = {}, sessionIdInMap = {}", user.getTel(), sessionId, sessionIdInMap);
                return R.fail(ERROR, "session不匹配请重新登录");
            }
            if (StrUtil.isNotEmpty(user.getTradePassword())) {
                if (StrUtil.isEmpty(tradePassword)) {
                    return R.fail(ERROR, "交易密码未设置");
                }
                if (!user.getTradePassword().equals(userService.passwdGenerate(tradePassword, user.getTel()))) {
                    return R.fail(ERROR, "交易密码错误");
                }
            }
            BankCard bankCard = this.bankCardService.selectById(bankCardId);
            if (bankCard == null) {
                return R.fail(ERROR, "银行卡不存在");
            }
            if (user.getProductMoney().compareTo(amount) < 0) {
                return R.fail(ERROR, "提现金额不够");
            }
            if (!bankCard.getTel().equals(user.getTel())) {
                logger.error("提现的银行卡不是本人 card_tel = {}, user_tel = {}", bankCard.getTel(), user.getTel());
                return R.fail(ERROR, "银行卡错误");
            }
            // 过滤提现内容
            List<SysConfig> configList = this.sysConfigService.selectByType(4);
            SysConfig config = configList.get(0);
            if (config.getSort() != null && amount.compareTo(new BigDecimal(config.getSort())) < 0) {
                return R.fail(ERROR, "不满足最低提现金额" + config.getSort());
            }
            if (!StringUtils.isNull(config.getUrl())) {
                if (user.getProductMoney().multiply(new BigDecimal(config.getUrl())).compareTo(amount) < 0) {
                    return R.fail(ERROR, "超过金额的百分之" + config.getUrl());
                }
            }
            if (config.getVisitCount() != null) {
                List<Byte> statusList = new ArrayList<>();
                statusList.add(Dictionary.PayStatusEnum.SUCCESS.getKey());
                statusList.add(Dictionary.PayStatusEnum.PAYING.getKey());
                WithdrawVo vo = new WithdrawVo();
                vo.setUserId(userId);
                Integer withdrawCount = this.withdrawService.selectCountByStatusAndDate(statusList, vo, DateUtil.getDayFirstSecond(0), null);
                if (withdrawCount != null && withdrawCount >= config.getVisitCount()) {
                    return R.fail(ERROR, "超过今天最大提现次数");
                }
            }
            if (!StringUtils.isNull(config.getDesc())) {
                if (amount.compareTo(new BigDecimal(config.getDesc())) > 0) {
                    return R.fail(ERROR, "提现最大金额位" + user.getProductMoney().multiply(new BigDecimal(config.getDesc())));
                }
            }
            List<SysConfig> drawConfigExtendList = this.sysConfigService.selectByType(SysConfigService.SysConfigType.DrawConfigExtend.getType());
            if (drawConfigExtendList.size() != 0 && drawConfigExtendList.get(0).getStatus() == 1 && user.getCharge().compareTo(new BigDecimal(drawConfigExtendList.get(0).getName())) < 0) {
                return R.fail(ERROR, drawConfigExtendList.get(0).getDesc());
            }
            Withdraw withdraw = new Withdraw();
            withdraw.setAmount(amount);
            withdraw.setBankCardId(bankCardId);
            withdraw.setBankCardInfo(bankCard.getBankAccount() + "|" + bankCard.getBankName() + "|" + bankCard.getBankNo());
            withdraw.setCharge(user.getCharge());
            List<User> list = this.userService.selectByParentId(userId);
            withdraw.setChildrenCharge(new BigDecimal(0));
            if (list != null && list.size() > 0) {
                for (User user2 : list) {
                    withdraw.setChildrenCharge(withdraw.getChildrenCharge().add(user2.getCharge()));
                }
            }
            withdraw.setChildren(list == null ? 0 : list.size());

            withdraw.setRate(new BigDecimal(config.getName()));
            withdraw.setRealAmount(amount.multiply(new BigDecimal(1).subtract(withdraw.getRate())));
            withdraw.setStatus(Dictionary.PayStatusEnum.PAYING.getKey());
            withdraw.setType((byte) 1);
            withdraw.setUserId(userId);
            withdraw.setUsername(user.getRealname());
            this.withdrawService.save(withdraw);
            this.userService.updateProductMoney(user.getId(), amount, OUT, Dictionary.MoneyTypeEnum.WITHDRAW.getKey(), "余额提现");
            UserController.filterTel(user);
            return R.ok(user);
        } catch (Exception e) {
            logger.error("查询个人资料", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }

    @Operation(summary = "假钱提现")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "amount", value = "提现金额", required = true, dataType = "decimal", dataTypeClass = BigDecimal.class), //
            @ApiImplicitParam(name = "bankCardId", value = "银行卡id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "type", value = "1提现 0签到提现", required = true, dataType = "int", dataTypeClass = Integer.class), //
            @ApiImplicitParam(name = "tradePassword", value = "交易密码", required = false, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/applyMoney")
    @UserSyncLock(key = "#userId")
    public R applyWithdraw2(Long userId, Long bankCardId, BigDecimal amount, String tradePassword, HttpServletRequest request) {
        try {
            if (userId == null || bankCardId == null || amount == null || amount.compareTo(new BigDecimal(0)) <= 0) {
                return R.fail(ERROR, "无效参数");
            }
            String sessionId = this.getSessionIdInHeader(request);
            if (StringUtils.isNull(sessionId)) {
//				return result(ERROR, "请重新登录");
            }
            logger.info("分红提现 userId = {}, bankCardId = {}, amount = {}, tradePassword = {}, sessionId = {}, ip = {}", userId, bankCardId, amount, tradePassword, sessionId, this.getIpAddr(request));
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail(ERROR, "用户不存在");
            }
            if (user.getStatus() == Dictionary.STATUS.DISABLE) {
                return R.fail(ERROR, "禁止提现");
            }
            String sessionIdInMap = AppConstants.sessionIdMap.get(user.getTel());
            sessionIdInMap = sessionIdInMap == null ? "" : sessionIdInMap;
            if (!sessionId.equals(sessionIdInMap)) {
                logger.error("session expired. /withdraw/apply tel = {}, sessionId = {}, sessionIdInMap = {}", user.getTel(), sessionId, sessionIdInMap);
                return R.fail(ERROR, "session不匹配请重新登录");
            }
            if (StrUtil.isNotEmpty(user.getTradePassword())) {
                if (StrUtil.isEmpty(tradePassword)) {
                    return R.fail(ERROR, "交易密码未设置");
                }
                if (!user.getTradePassword().equals(userService.passwdGenerate(tradePassword, user.getTel()))) {
                    return R.fail(ERROR, "交易密码错误");
                }
            }
            BankCard bankCard = this.bankCardService.selectById(bankCardId);
            if (bankCard == null) {
                return R.fail(ERROR, "银行卡不存在");
            }
            if (user.getMoney().compareTo(amount) < 0) {
                return R.fail(ERROR, "提现金额不够");
            }
            if (!bankCard.getTel().equals(user.getTel())) {
                logger.error("提现的银行卡不是本人 card_tel = {}, user_tel = {}", bankCard.getTel(), user.getTel());
                return R.fail(ERROR, "银行卡错误");
            }
            // 过滤提现内容
            List<SysConfig> configList = this.sysConfigService.selectByType(4);
            SysConfig config = configList.get(0);
            if (config.getSort() != null && amount.compareTo(new BigDecimal(config.getSort())) < 0) {
                return R.fail(ERROR, "不满足最低提现金额" + config.getSort());
            }
            if (!StringUtils.isNull(config.getUrl())) {
                if (user.getMoney().multiply(new BigDecimal(config.getUrl())).compareTo(amount) < 0) {
                    return R.fail(ERROR, "超过金额的百分之" + config.getUrl());
                }
            }
            if (config.getVisitCount() != null) {
                List<Byte> statusList = new ArrayList<>();
                statusList.add(Dictionary.PayStatusEnum.SUCCESS.getKey());
                statusList.add(Dictionary.PayStatusEnum.PAYING.getKey());
                WithdrawVo vo = new WithdrawVo();
                vo.setUserId(userId);
                Integer withdrawCount = this.withdrawService.selectCountByStatusAndDate(statusList, vo, DateUtil.getDayFirstSecond(0), null);
                if (withdrawCount != null && withdrawCount >= config.getVisitCount()) {
                    return R.fail(ERROR, "超过今天最大提现次数");
                }
            }
            if (!StringUtils.isNull(config.getDesc())) {
                if (amount.compareTo(new BigDecimal(config.getDesc())) > 0) {
                    return R.fail(ERROR, "提现最大金额位" + user.getMoney().multiply(new BigDecimal(config.getDesc())));
                }
            }
            List<SysConfig> drawConfigExtendList = this.sysConfigService.selectByType(SysConfigService.SysConfigType.DrawConfigExtend.getType());
            if (drawConfigExtendList.size() != 0 && drawConfigExtendList.get(0).getStatus() == 1 && user.getCharge().compareTo(new BigDecimal(drawConfigExtendList.get(0).getName())) < 0) {
                return R.fail(ERROR, drawConfigExtendList.get(0).getDesc());
            }
            Withdraw withdraw = new Withdraw();
            withdraw.setAmount(amount);
            withdraw.setBankCardId(bankCardId);
            withdraw.setBankCardInfo(bankCard.getBankAccount() + "|" + bankCard.getBankName() + "|" + bankCard.getBankNo());
            withdraw.setCharge(user.getCharge());
            List<User> list = this.userService.selectByParentId(userId);
            withdraw.setChildrenCharge(new BigDecimal(0));
            if (CollectionUtil.isNotEmpty(list)) {
                for (User user2 : list) {
                    withdraw.setChildrenCharge(withdraw.getChildrenCharge().add(user2.getCharge()));
                }
            }
            withdraw.setChildren(list == null ? 0 : list.size());

            withdraw.setRate(new BigDecimal(config.getName()));
            withdraw.setRealAmount(amount.multiply(new BigDecimal(1).subtract(withdraw.getRate())));
            withdraw.setStatus(Dictionary.PayStatusEnum.PAYING.getKey());
            withdraw.setType((byte) 2);
            withdraw.setUserId(userId);
            withdraw.setUsername(user.getRealname());
            this.withdrawService.save(withdraw);
            this.userService.updateMoney(user.getId(), amount, OUT, Dictionary.MoneyTypeEnum.WITHDRAW.getKey(), "Money提现");
            UserController.filterTel(user);
            return R.ok(user);
        } catch (Exception e) {
            logger.error("查询个人资料", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }

    @Operation(summary = "promiseMoney提现")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "amount", value = "提现金额", required = true, dataType = "decimal", dataTypeClass = BigDecimal.class), //
            @ApiImplicitParam(name = "bankCardId", value = "银行卡id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "type", value = "1提现 0签到提现", required = true, dataType = "int", dataTypeClass = Integer.class), //
            @ApiImplicitParam(name = "tradePassword", value = "交易密码", required = false, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/applyPromiseMoney")
//	@UserSyncLock(key = "#userId")
    public R applyWithdraw3(Long userId, Long bankCardId, BigDecimal amount, String tradePassword, HttpServletRequest request) {
        try {
            if (userId == null || bankCardId == null || amount == null || amount.compareTo(new BigDecimal(0)) <= 0) {
                return R.fail(ERROR, "无效参数");
            }
            String sessionId = this.getSessionIdInHeader(request);
            if (StrUtil.isEmpty(sessionId)) {
                return R.fail(ERROR, "请重新登录");
            }
            logger.info("Promise提现 userId = {}, bankCardId = {}, amount = {}, tradePassword = {}, sessionId = {}, ip = {}", userId, bankCardId, amount, tradePassword, sessionId, this.getIpAddr(request));
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail(ERROR, "用户不存在");
            }
            if (user.getStatus() == Dictionary.STATUS.DISABLE) {
                return R.fail(ERROR, "禁止提现");
            }
            String sessionIdInMap = AppConstants.sessionIdMap.get(user.getTel());
            sessionIdInMap = sessionIdInMap == null ? "" : sessionIdInMap;
            if (!sessionId.equals(sessionIdInMap)) {
                logger.error("session expired. /withdraw/apply tel = {}, sessionId = {}, sessionIdInMap = {}", user.getTel(), sessionId, sessionIdInMap);
                return R.fail(ERROR, "session不匹配请重新登录");
            }
            if (StrUtil.isNotEmpty(user.getTradePassword())) {
                if (StrUtil.isEmpty(tradePassword)) {
                    return R.fail(ERROR, "交易密码未设置");
                }
                if (!user.getTradePassword().equals(userService.passwdGenerate(tradePassword, user.getTel()))) {
                    return R.fail(ERROR, "交易密码错误");
                }
            }
            List<ProductActiveCount> activeCountList = this.productActiveService.selectBuyPersonByParentId(userId, null, null);
            if (CollectionUtil.isEmpty(activeCountList) || activeCountList.get(0).getCount() < 5) {
                return R.fail(ERROR, "当前团队激活人数未满5人");
            }
            BankCard bankCard = this.bankCardService.selectById(bankCardId);
            if (bankCard == null) {
                return R.fail(ERROR, "银行卡不存在");
            }
            if (user.getPromiseMoney().compareTo(amount) < 0) {
                return R.fail(ERROR, "提现金额不够");
            }
            if (!ObjectUtil.equals(user.getTel(), bankCard.getTel())) {
                logger.error("提现的银行卡不是本人 card_tel = {}, user_tel = {}", bankCard.getTel(), user.getTel());
                return R.fail(ERROR, "银行卡错误");
            }
            SysConfig config = this.sysConfigService.selectOneByType(4);
            if (config != null) {
                if (config.getSort() != null && amount.compareTo(new BigDecimal(config.getSort())) < 0) {
                    return R.fail(ERROR, "不满足最低提现金额" + config.getSort());
                }
                if (StrUtil.isNotEmpty(config.getUrl())) {
                    if (user.getPromiseMoney().multiply(new BigDecimal(config.getUrl())).compareTo(amount) < 0) {
                        return R.fail(ERROR, "超过金额的百分之" + config.getUrl());
                    }
                }
            }
            List<Byte> statusList = new ArrayList<>();
            statusList.add(Dictionary.PayStatusEnum.SUCCESS.getKey());
            statusList.add(Dictionary.PayStatusEnum.PAYING.getKey());
            if (config.getVisitCount() != null) {
                WithdrawVo vo = new WithdrawVo();
                vo.setUserId(userId);
                Integer withdrawCount = this.withdrawService.selectCountByStatusAndDate(statusList, vo, DateUtil.getDayFirstSecond(0), null);
                if (withdrawCount != null && withdrawCount >= config.getVisitCount()) {
                    return R.fail(ERROR, "超过今天最大提现次数");
                }
            }
            if (!StringUtils.isNull(config.getDesc())) {
                if (amount.compareTo(new BigDecimal(config.getDesc())) > 0) {
                    return R.fail(ERROR, "提现最大金额位" + user.getPromiseMoney().multiply(new BigDecimal(config.getDesc())));
                }
            }
            WithdrawVo param = new WithdrawVo();
            param.setUserId(userId);
            param.setType((byte) 3);
            Integer withdrawCount = this.withdrawService.selectCountByStatusAndDate(statusList, param, null, null);
            List<ProductActiveCount> countList = this.productActiveService.selectBuyPersonByParentId(userId, null, null);
            if (CollectionUtil.isEmpty(countList)) {
                return R.fail(ERROR, "邀请5人激活可立即提现");
            }
            ProductActiveCount productActiveCount = countList.get(0);
            if (new BigDecimal(productActiveCount.getCount()).compareTo(new BigDecimal(withdrawCount + 1).multiply(new BigDecimal(5))) < 0) {
                return R.fail(ERROR, "再邀请5人激活可再次提现");
            }
            Withdraw withdraw = new Withdraw();
            withdraw.setAmount(amount);
            withdraw.setBankCardId(bankCardId);
            withdraw.setBankCardInfo(bankCard.getBankAccount() + "|" + bankCard.getBankName() + "|" + bankCard.getBankNo());
            withdraw.setCharge(user.getCharge());
            List<User> list = this.userService.selectByParentId(userId);
            withdraw.setChildrenCharge(new BigDecimal(0));
            if (CollectionUtil.isNotEmpty(list)) {
                for (User user2 : list) {
                    withdraw.setChildrenCharge(withdraw.getChildrenCharge().add(user2.getCharge()));
                }
            }
            withdraw.setChildren(list == null ? 0 : list.size());
/*			List<SysConfig> drawConfigExtendList = this.sysConfigService.selectByType(SysConfigService.SysConfigType.DrawConfigExtend.getType());
			if (drawConfigExtendList.size()!=0 && drawConfigExtendList.get(0).getStatus() == 1 && user.getCharge().compareTo(new BigDecimal(drawConfigExtendList.get(0).getName())) < 0) {
				return result(ERROR, drawConfigExtendList.get(0).getDesc());
			}*/
            withdraw.setRate(new BigDecimal(config.getName()));
            withdraw.setRealAmount(amount.multiply(new BigDecimal(1).subtract(withdraw.getRate())));
            withdraw.setStatus(Dictionary.PayStatusEnum.PAYING.getKey());
            withdraw.setType((byte) 3);
            withdraw.setUserId(userId);
            withdraw.setUsername(user.getRealname());
            this.withdrawService.save(withdraw);
            this.userService.updatePromiseMoney(user.getId(), amount, OUT, Dictionary.MoneyTypeEnum.WITHDRAW.getKey(), "提现");
            UserController.filterTel(user);
            return R.ok(user);
        } catch (Exception e) {
            logger.error("查询个人资料", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }


    @Operation(summary = "POINT提现")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "amount", value = "提现金额", required = true, dataType = "decimal", dataTypeClass = Integer.class), //
            @ApiImplicitParam(name = "bankCardId", value = "银行卡id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "type", value = "1提现 0签到提现", required = true, dataType = "int", dataTypeClass = Integer.class), //
            @ApiImplicitParam(name = "tradePassword", value = "交易密码", required = false, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/applyPoint")
    @UserSyncLock(key = "#userId")
    public R applyWithdraw4(Long userId, Long bankCardId, Integer amount, String tradePassword, HttpServletRequest request) {
        try {
            if (userId == null || bankCardId == null || amount == null || amount <= 0) {
                return R.fail(ERROR, "无效参数");
            }
            String sessionId = this.getSessionIdInHeader(request);
            if (StringUtils.isNull(sessionId)) {
                return R.fail(ERROR, "请重新登录");
            }
            logger.info("POINT提现 userId = {}, bankCardId = {}, amount = {}, tradePassword = {}, sessionId = {}, ip = {}", userId, bankCardId, amount, tradePassword, sessionId, this.getIpAddr(request));
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail(ERROR, "用户不存在");
            }
            if (user.getStatus() == Dictionary.STATUS.DISABLE) {
                return R.fail(ERROR, "禁止提现");
            }
            String sessionIdInMap = AppConstants.sessionIdMap.get(user.getTel());
            sessionIdInMap = sessionIdInMap == null ? "" : sessionIdInMap;
            if (!sessionId.equals(sessionIdInMap)) {
                logger.error("session expired. /withdraw/apply tel = {}, sessionId = {}, sessionIdInMap = {}", user.getTel(), sessionId, sessionIdInMap);
                return R.fail(ERROR, "session不匹配请重新登录");
            }
            if (!StringUtils.isNull(tradePassword)) {
                if (!user.getTradePassword().equals(userService.passwdGenerate(tradePassword, user.getTel()))) {
                    return R.fail(ERROR, "交易密码错误");
                }
            }
            BankCard bankCard = this.bankCardService.selectById(bankCardId);
            if (bankCard == null) {
                return R.fail(ERROR, "银行卡不存在");
            }
            if (user.getPoint() < amount) {
                return R.fail(ERROR, "提现金额不够");
            }
            if (!bankCard.getTel().equals(user.getTel())) {
                logger.error("提现的银行卡不是本人 card_tel = {}, user_tel = {}", bankCard.getTel(), user.getTel());
                return R.fail(ERROR, "银行卡错误");
            }
            List<SysConfig> configList = this.sysConfigService.selectByType(4);
            SysConfig config = configList.get(0);
            if (config.getSort() != null && amount < new BigDecimal(config.getSort()).intValue()) {
                return R.fail(ERROR, "不满足最低提现金额" + config.getSort());
            }
            if (config.getVisitCount() != null) {
                List<Byte> statusList = new ArrayList<>();
                statusList.add(Dictionary.PayStatusEnum.SUCCESS.getKey());
                statusList.add(Dictionary.PayStatusEnum.PAYING.getKey());
                WithdrawVo vo = new WithdrawVo();
                vo.setUserId(userId);
                Integer withdrawCount = this.withdrawService.selectCountByStatusAndDate(statusList, vo, DateUtil.getDayFirstSecond(0), null);
                if (withdrawCount != null && withdrawCount >= config.getVisitCount()) {
                    return R.fail(ERROR, "超过今天最大提现次数");
                }
            }
            Withdraw withdraw = new Withdraw();
            withdraw.setAmount(new BigDecimal(amount));
            withdraw.setBankCardId(bankCardId);
            withdraw.setBankCardInfo(bankCard.getBankAccount() + "|" + bankCard.getBankName() + "|" + bankCard.getBankNo());
            withdraw.setCharge(user.getCharge());
            List<User> list = this.userService.selectByParentId(userId);
            withdraw.setChildrenCharge(new BigDecimal(0));
            if (CollectionUtil.isNotEmpty(list)) {
                for (User user2 : list) {
                    withdraw.setChildrenCharge(withdraw.getChildrenCharge().add(user2.getCharge()));
                }
            }
            withdraw.setChildren(list == null ? 0 : list.size());
/*			List<SysConfig> drawConfigExtendList = this.sysConfigService.selectByType(SysConfigService.SysConfigType.DrawConfigExtend.getType());
			if (drawConfigExtendList.size()!=0 && drawConfigExtendList.get(0).getStatus() == 1 && user.getCharge().compareTo(new BigDecimal(drawConfigExtendList.get(0).getName())) < 0) {
				return result(ERROR, drawConfigExtendList.get(0).getDesc());
			}*/
            withdraw.setRate(new BigDecimal(config.getName()));
            withdraw.setRealAmount(new BigDecimal(amount).multiply(new BigDecimal(1).subtract(withdraw.getRate())));
            withdraw.setStatus(Dictionary.PayStatusEnum.PAYING.getKey());
            withdraw.setType((byte) 4);
            withdraw.setUserId(userId);
            withdraw.setUsername(user.getRealname());
            this.withdrawService.save(withdraw);
            this.userService.updatePoint(user.getId(), amount, new BigDecimal(amount), OUT, "提现");
            UserController.filterTel(user);
            return R.ok(user);
        } catch (Exception e) {
            logger.error("查询个人资料", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }

    @PostMapping(value = "/backhongbao")
    @UserSyncLock(key = "'backhongbao'")
    public R backMoney() {
        try {
            List<WithdrawVo> withdrawVoList = withdrawService.selectByCondition(null, new WithdrawVo());
            for (WithdrawVo withdrawVo : withdrawVoList) {
                if (withdrawVo.getStatus().intValue() == 3 && withdrawVo.getType().intValue() == 3) {
                    this.userService.updateMoney(withdrawVo.getUserId(), withdrawVo.getAmount(), IN, "红包审核退回", "红包审核退回");
                    logger.info("ID [{}],退回红包 {}", withdrawVo.getId(), withdrawVo.getAmount().doubleValue());
                }
            }
            return R.ok(true);
        } catch (Exception e) {
            logger.error("查询个人资料", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }

}
