package com.ruoyi.web.controller.admin.controller.notify;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.PayConfig;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.entity.PayOrder;
import com.ruoyi.system.zny.entity.User;
import com.ruoyi.system.zny.service.*;
import com.ruoyi.system.zny.utils.MD5Util;
import com.ruoyi.system.zny.vo.PayOrderVo;
import com.ruoyi.web.controller.admin.controller.notify.vo.JinniuVo;
import com.ruoyi.web.controller.admin.pay.SignUtil;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

import static com.ruoyi.common.constant.HttpStatus.ERROR;

@Hidden
@ApiIgnore
@RestController
@RequestMapping(value = "/notify")
public class NotifyController extends BaseController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private PayConfigService payConfigService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductActiveService productActiveService;

    @Autowired
    private PayConfigDetailService payConfigDetailService;

    /**
     * 商户编号 mch_code 必填 Varchar(10) 商户编号18开头6位数字 <br>
     * 商户唯一订单号 out_trade_no 必填 Varchar(50) 商户系统唯一订单号<br>
     * 平台订单号 plat_order_no 必填 Varchar(32) 平台流水号<br>
     * 支付状态 status 必填 Varchar(10) SUCCESS:成功<br>
     * 订单金额 amount 必填 Decimal(9,2) 单位：元<br>
     * 签名类型 plat_sign_type 必填 Varchar (14) MD5<br>
     * 费率 fee 必填 Decimal(9,2) 手续费用，单位为RMB-元。<br>
     * 数据格式 format 必填 Varchar(12) 固定值：JSON<br>
     * 编码格式 charset 必填 Varchar(12) 固定值：utf-8<br>
     * 应答描述 msg 是 Varchar (128) <br>
     * 数据签名 plat_sign 是 Varchar(255) MD5加密，详见数据签名机制<br>
     */
    @PostMapping(value = "/desheng")
    public R desheng(String mch_code, String out_trade_no, String plat_order_no, String status, String amount, String plat_sign_type, //
                     String fee, String format, String charset, String msg, String plat_sign) {
        try {
            logger.info("mch_code = {}, out_trade_no = {}, plat_order_no = {}, status = {}, amount = {}, plat_sign_type = {}, fee = {}, format = {}, charset = {}, msg = {}, plat_sign = {}", //
                    mch_code, out_trade_no, plat_order_no, status, amount, plat_sign_type, fee, format, //
                    charset, msg, plat_sign);
            // 验证签名
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("mch_code", mch_code);
            paramMap.put("out_trade_no", out_trade_no);
            paramMap.put("plat_order_no", plat_order_no);
            paramMap.put("status", status);
            paramMap.put("amount", amount);
            paramMap.put("plat_sign_type", plat_sign_type);
            paramMap.put("fee", fee);
            paramMap.put("format", format);
            paramMap.put("charset", charset);
            paramMap.put("msg", msg);
            PayOrder order = this.payOrderService.selectByOrderNum(out_trade_no);
            if (order == null) {
                logger.error("订单不存在 orderNum = {}", out_trade_no);
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                logger.error("配置不存在id = {}", order.getPayConfigId());
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey());
            if (!valid.equals(plat_sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", plat_sign, valid);
                return R.fail("密钥签名验证错误, sign = +" + plat_sign + "+, valid = +" + valid + "+");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                R.fail("用户不存在");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum());
            }
            // 更新订单
            order.setRate(new BigDecimal(fee));
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
//			if (order.getType().intValue() == 2) {
//				this.userService.updateProductMoney(order.getUserid(), order.getPayAmount(), IN, Dictionary.MoneyTypeEnum.BUY.getKey());
//			}
            return R.ok(true);
        } catch (Exception e) {
            logger.error("/notify/desheng", e);
            return R.fail(ERROR);
        }
    }

    @PostMapping(value = "/pubPay", produces = MediaType.APPLICATION_JSON_VALUE)
    public R pubPay(@RequestBody String param) {
        try {
            logger.info("param = {}", param);
            JSONObject parseObject = JSONObject.parseObject(param);
            String valid = parseObject.getString("merid") + "&" + parseObject.getString("mon") + "&" + //
                    parseObject.getString("orderid") + "&" + parseObject.getString("paytype") + "&" + parseObject.getString("reqsn");
            PayOrder order = this.payOrderService.selectByOrderNum(parseObject.getString("reqsn"));
            if (order == null) {
                logger.error("订单不存在 orderNum = {}", parseObject.getString("reqsn"));
                return R.fail("订单不存在 orderNum = " + parseObject.getString("reqsn"));
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                logger.error("支付配置不存在 id = {}", order.getPayConfigId());
                return R.fail("支付配置不存在 id = " + order.getPayConfigId());
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            valid = SignUtil.hmacSHA256(valid, detail.getNotifyKey());
            if (!valid.equals(parseObject.getString("sign"))) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", parseObject.getString("sign"), valid);
                return R.fail("密钥签名验证错误 , sign = " + parseObject.getString("sign") + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
//			if (order.getType().intValue() == 2) {
//				this.userService.updateProductMoney(order.getUserid(), order.getPayAmount(), IN, Dictionary.MoneyTypeEnum.BUY.getKey());
//			}
            return R.ok(true);
        } catch (Exception e) {
            logger.error("/notify/pubPay:", e);
            return R.fail(ERROR);
        }
    }

    /**
     * @param payOrderId  P01202204072121355187016
     * @param mchId       20000016
     * @param productId   8022
     * @param mchOrderNo  164933769549113561234567
     * @param amount      999900
     * @param status      2
     * @param paySuccTime 1649337738000
     * @param backType    2
     * @param reqTime     20220407212218
     * @param sign        C179698AA56C0933B5EC4803D655C88C
     * @return
     */
    @PostMapping(value = "/huijinplay")
    public R huijinplay(String payOrderId, String mchId, String appId, String productId, String mchOrderNo, Integer amount, Integer income, Integer status, //
                        String channelOrderNo, String param1, String param2, Long paySuccTime, Integer backType, String reqTime, String sign) {
        try {
            logger.info("/notify/huijinplay payOrderId = {}, mchId = {}, appId = {}, productId = {}, mchOrderNo = {}, amount = {},income = {}, status = {}, channelOrderNo = {}, param1 = {}, param2 = {}, paySuccTime = {}, backType = {}, reqTime = {}, sign = {}", //
                    payOrderId, mchId, appId, productId, mchOrderNo, amount, income, status, //
                    channelOrderNo, param1, param2, paySuccTime, backType, reqTime, sign);
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("payOrderId", payOrderId);
            paramMap.put("mchId", mchId);
            if (!StringUtils.isNull(appId)) {
                paramMap.put("appId", appId);
            }
            paramMap.put("productId", productId);
            paramMap.put("mchOrderNo", mchOrderNo);
            paramMap.put("amount", amount);
            paramMap.put("status", status);
            if (!StringUtils.isNull(channelOrderNo)) {
                paramMap.put("channelOrderNo", channelOrderNo);
            }
            if (!StringUtils.isNull(param1)) {
                paramMap.put("param1", param1);
            }
            if (!StringUtils.isNull(param2)) {
                paramMap.put("param2", param2);
            }
            paramMap.put("paySuccTime", paySuccTime);
            paramMap.put("backType", backType);
            paramMap.put("reqTime", reqTime);
            PayOrder order = this.payOrderService.selectByOrderNum(mchOrderNo);
            if (order == null) {
                logger.error("订单不存在 orderNum = {}", mchOrderNo);
                return R.fail("订单不存在 orderNum = " + mchOrderNo);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                logger.error("支付配置id错误, 请重新生成订单");
                return R.fail("支付配置id错误, 请重新生成订单");
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误 , sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(payOrderId);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
//			if (order.getType().intValue() == 2) {
//				this.userService.updateProductMoney(order.getUserid(), order.getPayAmount(), IN, Dictionary.MoneyTypeEnum.BUY.getKey());
//			}
            return R.ok(true);
        } catch (Exception e) {
            logger.error("/notify/pubPay", e);
            return R.fail(ERROR);
        }
    }

    /**
     * 请求方式  post application/x-www-urlencoded
     *
     * @param merchantId 商户号
     * @param orderId    商户订单号
     * @param amount     订单金额
     * @param status     订单状态 订单已支付才会回调,此参数值固定为 ok
     * @param sign       回调签名
     * @return 到我方回调通知后, 请返回小写英文字符串 ok或success  否则我方将按一定频率重复通知若干次
     */
    @PostMapping(value = "/duola")
    public R duola(String merchantId, String orderId, String amount, String status, String sign) {
        try {
            logger.info("merchantId = {}, orderId = {}, amount = {}, status = {}, sign = {}", //
                    merchantId, orderId, amount, status, sign);
            PayOrderVo order = this.payOrderService.selectByOrderNum(orderId);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("merchantId", merchantId);
            paramMap.put("orderId", orderId);
            paramMap.put("amount", amount);
            paramMap.put("status", status);
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toLowerCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail(ERROR);
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(orderId);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
//			if (order.getType().intValue() == 2) {
//				this.userService.updateProductMoney(order.getUserid(), order.getPayAmount(), IN, Dictionary.MoneyTypeEnum.BUY.getKey());
//			}
            return R.ok(true);
        } catch (Exception e) {
            logger.error("duola 出错: ", e);
            return R.fail(ERROR);
        }
    }

    @PostMapping(value = "/jinniu")
    public R jinniu(JinniuVo vo) {
        try {
            logger.info("jinniu notify param = {}", vo);
            PayOrderVo order = this.payOrderService.selectByOrderNum(vo.getOrderid());
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("memberid", vo.getMemberid());
            paramMap.put("orderid", vo.getOrderid());
            paramMap.put("amount", vo.getAmount());
            paramMap.put("transaction_id", vo.getTransaction_id());
            paramMap.put("datetime", vo.getDatetime());
            paramMap.put("returncode", vo.getReturncode());
            if (!"00".equals(vo.getReturncode())) {
                logger.error("jinniu 支付错误 order_num = {}", order.getOrderNum());
                return R.fail("jinniu 支付错误 order_num = " + order.getOrderNum() + "");
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            String sign = vo.getSign();
            if (!valid.equals(sign)) {
                logger.error("金牛密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail(ERROR);
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对2 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(vo.getTransaction_id());
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("jinniu 出错: ", e);
            return R.fail(ERROR);
        }
    }

    @PostMapping(value = "/haoyuan")
    public R haoyuan(String payOrderId, String mchId, Integer productId, String mchOrderNo, BigDecimal amount, Integer status, Long paySuccTime, String sign) {
        try {
            logger.info("haoyuan notify payOrderId = {}, mchId = {}, productId = {}, mchOrderNo = {}, amount = {}, status = {}, paySuccTime = {}, sign = {}", //
                    payOrderId, mchId, productId, mchOrderNo, amount, status, paySuccTime, sign);
            PayOrderVo order = this.payOrderService.selectByOrderNum(mchOrderNo);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("payOrderId", payOrderId);
            paramMap.put("mchId", mchId);
            paramMap.put("productId", productId);
            paramMap.put("mchOrderNo", mchOrderNo);
            paramMap.put("amount", amount);
            paramMap.put("status", status);
            paramMap.put("paySuccTime", paySuccTime);
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对2 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(payOrderId);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("捷信支付出错", e);
            return R.fail(ERROR);
        }
    }

    @PostMapping(value = "/mm")
    public R mm(String payOrderId, String mchId, Integer productId, String mchOrderNo, BigDecimal amount, Integer status, Long paySuccTime, String sign) {
        try {
            logger.info("mm notify payOrderId = {}, mchId = {}, productId = {}, mchOrderNo = {}, amount = {}, status = {}, paySuccTime = {}, sign = {}", //
                    payOrderId, mchId, productId, mchOrderNo, amount, status, paySuccTime, sign);
            PayOrderVo order = this.payOrderService.selectByOrderNum(mchOrderNo);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("payOrderId", payOrderId);
            paramMap.put("mchId", mchId);
            paramMap.put("productId", productId);
            paramMap.put("mchOrderNo", mchOrderNo);
            paramMap.put("amount", amount);
            paramMap.put("status", status);
            paramMap.put("paySuccTime", paySuccTime);
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对2 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(payOrderId);
            this.payOrderService.save(order);
            // 激活产品
            if (ObjectUtil.isNotEmpty(order.getProductid())) {
                this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            } else { // 更新用户充值
                user.setCharge((user.getCharge() == null ? new BigDecimal(0) : user.getCharge()).add(order.getPayAmount()));
                this.userService.update(user);
            }
            return R.ok(true);
        } catch (Exception e) {
            logger.error("捷信支付出错", e);
            return R.fail("捷信支付出错");
        }
    }

    @RequestMapping(value = "/chuanhu")
    public R chuanhu(int pid, String trade_no, String out_trade_no, String type, String name, BigDecimal money, String trade_status, String sign, String sign_type) {
        try {
            logger.info("chuanhu notify pid = {}, trade_no = {}, out_trade_no = {}, type = {}, name = {}, money = {}, trade_status = {}, sign = {}, sign_type = {}", //
                    pid, trade_no, out_trade_no, type, name, money, trade_status, sign, sign_type);
            if (!"TRADE_SUCCESS".equals(trade_status)) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(out_trade_no);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("pid", pid);
            paramMap.put("trade_no", trade_no);
            paramMap.put("out_trade_no", out_trade_no);
            paramMap.put("type", type);
            paramMap.put("name", name);
            paramMap.put("money", money);
            paramMap.put("trade_status", trade_status);
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey(), true);
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(trade_no);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("捷信支付出错", e);
            return R.fail("捷信支付出错");
        }
    }

    @RequestMapping(value = "/fenghong", produces = MediaType.APPLICATION_JSON_VALUE)
    public R fenghong(@RequestBody String param) {
        logger.info("fenghong param = {}", param);
        try {
//			{"mch_id":"1024","trade_no":"E24637891921099581397","out_trade_no":"b9139405052b40503d57dc15e0329b6b","money":"150",
//			"notify_time":"2022-05-26 20:06:00","status":"2","original_trade_no":"20220526005843","subject":"小商品","body":"",
//			"sign":"B5E6D6E1BABA31A9ED8B0E3E3845C7D2"}
            JSONObject parseObject = JSONObject.parseObject(param);
            if (!"2".equals(parseObject.getString("status"))) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(parseObject.getString("out_trade_no"));
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("mch_id", parseObject.getString("mch_id"));
            paramMap.put("trade_no", parseObject.getString("trade_no"));
            paramMap.put("out_trade_no", parseObject.getString("out_trade_no"));
            paramMap.put("money", parseObject.getString("money"));
            paramMap.put("notify_time", parseObject.getString("notify_time"));
            paramMap.put("subject", parseObject.getString("subject"));
            paramMap.put("status", parseObject.getString("status"));
            paramMap.put("original_trade_no", parseObject.getString("original_trade_no"));
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey(), true).toUpperCase();
            String sign = parseObject.getString("sign");
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(parseObject.getString("trade_no"));
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("丰宏支付出错: ", e);
            return R.fail("丰宏支付出错");
        }
    }

    @RequestMapping(value = "/qitian", produces = MediaType.APPLICATION_JSON_VALUE)
    public R qitian(@RequestBody String param) {
        logger.info("qitian param = {}", param);
        try {
//			{"mch_id":"1024","trade_no":"E24637891921099581397","out_trade_no":"b9139405052b40503d57dc15e0329b6b","money":"150",
//			"notify_time":"2022-05-26 20:06:00","status":"2","original_trade_no":"20220526005843","subject":"小商品","body":"",
//			"sign":"B5E6D6E1BABA31A9ED8B0E3E3845C7D2"}
            JSONObject parseObject = JSONObject.parseObject(param);
            if (!"2".equals(parseObject.getString("status"))) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(parseObject.getString("out_trade_no"));
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("mch_id", parseObject.getString("mch_id"));
            paramMap.put("trade_no", parseObject.getString("trade_no"));
            paramMap.put("out_trade_no", parseObject.getString("out_trade_no"));
            paramMap.put("money", parseObject.getString("money"));
            paramMap.put("notify_time", parseObject.getString("notify_time"));
            paramMap.put("subject", parseObject.getString("subject"));
            paramMap.put("status", parseObject.getString("status"));
            paramMap.put("original_trade_no", parseObject.getString("original_trade_no"));
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey(), true).toUpperCase();
            String sign = parseObject.getString("sign");
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(parseObject.getString("trade_no"));
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("qitian回调出错: ", e);
            return R.fail(ERROR);
        }
    }

    @RequestMapping(value = "/quansheng", produces = MediaType.APPLICATION_JSON_VALUE)
    public R quansheng(@RequestBody String param) {
        logger.info("quansheng param = {}", param);
        try {
//			{"mch_id":"1024","trade_no":"E24637891921099581397","out_trade_no":"b9139405052b40503d57dc15e0329b6b","money":"150",
//			"notify_time":"2022-05-26 20:06:00","status":"2","original_trade_no":"20220526005843","subject":"小商品","body":"",
//			"sign":"B5E6D6E1BABA31A9ED8B0E3E3845C7D2"}
            JSONObject parseObject = JSONObject.parseObject(param);
            if (!"2".equals(parseObject.getString("status"))) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(parseObject.getString("out_trade_no"));
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("mch_id", parseObject.getString("mch_id"));
            paramMap.put("trade_no", parseObject.getString("trade_no"));
            paramMap.put("out_trade_no", parseObject.getString("out_trade_no"));
            paramMap.put("money", parseObject.getString("money"));
            paramMap.put("notify_time", parseObject.getString("notify_time"));
            paramMap.put("subject", parseObject.getString("subject"));
            paramMap.put("status", parseObject.getString("status"));
            paramMap.put("original_trade_no", parseObject.getString("original_trade_no"));
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey(), true).toUpperCase();
            String sign = parseObject.getString("sign");
            if (!valid.equalsIgnoreCase(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(parseObject.getString("trade_no"));
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("丰宏支付出错: ", e);
            return R.fail("丰宏支付出错");
        }
    }

    @RequestMapping(value = "/jintian", produces = MediaType.APPLICATION_JSON_VALUE)
    public R jintian(@RequestBody String param) {
        logger.info("jintian param = {}", param);
        try {
//			{"mch_id":"1024","trade_no":"E24637891921099581397","out_trade_no":"b9139405052b40503d57dc15e0329b6b","money":"150",
//			"notify_time":"2022-05-26 20:06:00","status":"2","original_trade_no":"20220526005843","subject":"小商品","body":"",
//			"sign":"B5E6D6E1BABA31A9ED8B0E3E3845C7D2"}
            JSONObject parseObject = JSONObject.parseObject(param);
            if (!"2".equals(parseObject.getString("status"))) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(parseObject.getString("out_trade_no"));
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("mch_id", parseObject.getString("mch_id"));
            paramMap.put("trade_no", parseObject.getString("trade_no"));
            paramMap.put("out_trade_no", parseObject.getString("out_trade_no"));
            paramMap.put("money", parseObject.getString("money"));
            paramMap.put("notify_time", parseObject.getString("notify_time"));
            paramMap.put("subject", parseObject.getString("subject"));
            paramMap.put("status", parseObject.getString("status"));
            paramMap.put("original_trade_no", parseObject.getString("original_trade_no"));
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey(), true).toUpperCase();
            String sign = parseObject.getString("sign");
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(parseObject.getString("trade_no"));
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("丰宏支付出错: ", e);
            return R.fail("丰宏支付出错");
        }
    }

    @PostMapping(value = "/ed")
    public R ed(String memberid, String orderid, BigDecimal amount, String transaction_id, String datetime, String returncode, String sign) {
        try {
            logger.info("ed notify param : memberid = {}, orderid = {}, amount = {}, transaction_id = {}, datetime = {}, returncode = {}, sign = {}", //
                    memberid, orderid, amount, transaction_id, datetime, returncode, sign);
            if (!"00".equals(returncode)) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(orderid);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("memberid", memberid);
            paramMap.put("orderid", orderid);
            paramMap.put("amount", amount);
            paramMap.put("transaction_id", transaction_id);
            paramMap.put("datetime", datetime);
            paramMap.put("returncode", returncode);
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(memberid);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("ed回调异常 : ", e);
        }
        return null;
    }

    @PostMapping(value = "/zonghe")
    public R zonghe(String memberid, String orderid, BigDecimal amount, String transaction_id, String datetime, String returncode, String sign) {
        try {
            logger.info("zonghe notify param : memberid = {}, orderid = {}, amount = {}, transaction_id = {}, datetime = {}, returncode = {}, sign = {}", //
                    memberid, orderid, amount, transaction_id, datetime, returncode, sign);
            if (!"00".equals(returncode)) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(orderid);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("memberid", memberid);
            paramMap.put("orderid", orderid);
            paramMap.put("amount", amount);
            paramMap.put("transaction_id", transaction_id);
            paramMap.put("datetime", datetime);
            paramMap.put("returncode", returncode);
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(memberid);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("ed回调异常 : ", e);
        }
        return null;
    }

    @PostMapping(value = "/xuebi")
    public R xuebi(String memberid, String orderid, BigDecimal amount, String transaction_id, String datetime, String returncode, String sign) {
        try {
            logger.info("xuebi notify param : memberid = {}, orderid = {}, amount = {}, transaction_id = {}, datetime = {}, returncode = {}, sign = {}", //
                    memberid, orderid, amount, transaction_id, datetime, returncode, sign);
            if (!"00".equals(returncode)) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(orderid);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("memberid", memberid);
            paramMap.put("orderid", orderid);
            paramMap.put("amount", amount);
            paramMap.put("transaction_id", transaction_id);
            paramMap.put("datetime", datetime);
            paramMap.put("returncode", returncode);
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(memberid);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("ed回调异常 : ", e);
            R.fail("ed回调异常");
        }
        return null;
    }

    @PostMapping(value = "/kbao")
    public R kbao(String memberid, String orderid, BigDecimal amount, String transaction_id, String datetime, String returncode, String sign) {
        try {
            logger.info("kbao notify param : memberid = {}, orderid = {}, amount = {}, transaction_id = {}, datetime = {}, returncode = {}, sign = {}", //
                    memberid, orderid, amount, transaction_id, datetime, returncode, sign);
            if (!"00".equals(returncode)) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(orderid);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("memberid", memberid);
            paramMap.put("orderid", orderid);
            paramMap.put("amount", amount);
            paramMap.put("transaction_id", transaction_id);
            paramMap.put("datetime", datetime);
            paramMap.put("returncode", returncode);
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(memberid);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("ed回调异常 : ", e);
        }
        return null;
    }

    @PostMapping(value = "/wangzi")
    public R wangzi(String memberid, String orderid, BigDecimal amount, String transaction_id, String datetime, String returncode, String sign) {
        try {
            logger.info("wangzi notify param : memberid = {}, orderid = {}, amount = {}, transaction_id = {}, datetime = {}, returncode = {}, sign = {}", //
                    memberid, orderid, amount, transaction_id, datetime, returncode, sign);
            if (!"00".equals(returncode)) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(orderid);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("memberid", memberid);
            paramMap.put("orderid", orderid);
            paramMap.put("amount", amount);
            paramMap.put("transaction_id", transaction_id);
            paramMap.put("datetime", datetime);
            paramMap.put("returncode", returncode);
            String key = MD5Util.string2MD5(orderid + "UV" + detail.getNotifyKey());
            String valid = SignUtil.buildSign(paramMap, key).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(memberid);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("ed回调异常 : ", e);
        }
        return null;
    }

    @PostMapping(value = "/jiulong")
    public R wangzi(String memberid, String orderid, BigDecimal amount, BigDecimal true_amount, String transaction_id, String datetime, String returncode, String sign) {
        logger.info("jiulong notify param : memberid = {}, orderid = {}, amount = {}, true_amount = {}, transaction_id = {}, datetime = {}, returncode = {}, sign = {}", //
                memberid, orderid, amount, true_amount, transaction_id, datetime, returncode, sign);
        try {
            if (!"00".equals(returncode)) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(orderid);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("memberid", memberid);
            paramMap.put("orderid", orderid);
            paramMap.put("amount", amount);
            paramMap.put("true_amount", true_amount);
            paramMap.put("transaction_id", transaction_id);
            paramMap.put("datetime", datetime);
            paramMap.put("returncode", returncode);
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(memberid);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("丰宏支付出错: ", e);
            return R.fail("丰宏支付出错");
        }
    }

    @PostMapping(value = "/qinchen")
    public R qinchen(JinniuVo vo) {
        try {
            logger.info("qinchen notify param = {}", vo);
            PayOrderVo order = this.payOrderService.selectByOrderNum(vo.getOrderid());
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("memberid", vo.getMemberid());
            paramMap.put("orderid", vo.getOrderid());
            paramMap.put("amount", vo.getAmount());
            paramMap.put("transaction_id", vo.getTransaction_id());
            paramMap.put("datetime", vo.getDatetime());
            paramMap.put("returncode", vo.getReturncode());
            if (!"00".equals(vo.getReturncode())) {
                logger.error("qinchen 支付错误 order_num = {}, returncode = {}.", order.getOrderNum(), vo.getReturncode());
                return R.fail("qinchen 支付错误 order_num = " + order.getOrderNum() + ", returncode = " + vo.getReturncode() + "");
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            String sign = vo.getSign();
            if (!valid.equals(sign)) {
                logger.error("清晨密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(vo.getTransaction_id());
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("qinchen 出错: ", e);
            return R.fail("qinchen 出错");
        }
    }

    @RequestMapping(value = "/baishi", produces = MediaType.APPLICATION_JSON_VALUE)
    public R baishi(@RequestBody String param) {
        logger.info("baishi param = {}", param);
        try {
//			{"mch_id":"1024","trade_no":"E24637891921099581397","out_trade_no":"b9139405052b40503d57dc15e0329b6b","money":"150",
//			"notify_time":"2022-05-26 20:06:00","status":"2","original_trade_no":"20220526005843","subject":"小商品","body":"",
//			"sign":"B5E6D6E1BABA31A9ED8B0E3E3845C7D2"}
            JSONObject parseObject = JSONObject.parseObject(param);
            if (!"2".equals(parseObject.getString("status"))) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(parseObject.getString("out_trade_no"));
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("mch_id", parseObject.getString("mch_id"));
            paramMap.put("trade_no", parseObject.getString("trade_no"));
            paramMap.put("out_trade_no", parseObject.getString("out_trade_no"));
            paramMap.put("money", parseObject.getString("money"));
            paramMap.put("notify_time", parseObject.getString("notify_time"));
            paramMap.put("subject", parseObject.getString("subject"));
            paramMap.put("status", parseObject.getString("status"));
            paramMap.put("original_trade_no", parseObject.getString("original_trade_no"));
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey(), true).toUpperCase();
            String sign = parseObject.getString("sign");
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(parseObject.getString("trade_no"));
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("baishi支付出错: ", e);
            return R.fail("baishi支付出错");
        }
    }

    @PostMapping(value = "/hy")
    public R hy(String memberid, String orderid, BigDecimal amount, String transaction_id, String datetime, String returncode, String sign) {
        logger.info("hy notify param : memberid = {}, orderid = {}, amount = {}, transaction_id = {}, datetime = {}, returncode = {}, sign = {}", //
                memberid, orderid, amount, transaction_id, datetime, returncode, sign);
        try {
            if (!"00".equals(returncode)) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(orderid);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("memberid", memberid);
            paramMap.put("orderid", orderid);
            paramMap.put("amount", amount);
            paramMap.put("transaction_id", transaction_id);
            paramMap.put("datetime", datetime);
            paramMap.put("returncode", returncode);
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(memberid);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(ERROR);
        } catch (Exception e) {
            logger.error("丰宏支付出错: ", e);
            return R.ok("丰宏支付出错");
        }
    }

    @PostMapping(value = "/youyi")
    public R youyi(String memberid, String orderid, BigDecimal amount, String transaction_id, String datetime, String returncode, String sign) {
        logger.info("youyi notify param : memberid = {}, orderid = {}, amount = {}, transaction_id = {}, datetime = {}, returncode = {}, sign = {}", //
                memberid, orderid, amount, transaction_id, datetime, returncode, sign);
        try {
            if (!"00".equals(returncode)) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(orderid);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("memberid", memberid);
            paramMap.put("orderid", orderid);
            paramMap.put("amount", amount);
            paramMap.put("transaction_id", transaction_id);
            paramMap.put("datetime", datetime);
            paramMap.put("returncode", returncode);
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(memberid);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("丰宏支付出错: ", e);
            return R.fail("丰宏支付出错");
        }
    }

    @PostMapping(value = "/youqian")
    public R youqian(String memberid, String orderid, BigDecimal amount, String transaction_id, String datetime, String returncode, String sign) {
        logger.info("youqian notify param : memberid = {}, orderid = {}, amount = {}, transaction_id = {}, datetime = {}, returncode = {}, sign = {}", //
                memberid, orderid, amount, transaction_id, datetime, returncode, sign);
        try {
            if (!"00".equals(returncode)) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(orderid);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("memberid", memberid);
            paramMap.put("orderid", orderid);
            paramMap.put("amount", amount);
            paramMap.put("transaction_id", transaction_id);
            paramMap.put("datetime", datetime);
            paramMap.put("returncode", returncode);
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equals(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(memberid);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("youqian error: ", e);
            return R.fail(ERROR);
        }
    }

    @RequestMapping(value = "/quanshengkeji", produces = MediaType.APPLICATION_JSON_VALUE)
    public R quanshengkeji(@RequestBody String param) {
        logger.info("quanshengkeji param = {}", param);
        try {
//			{"mch_id":"1024","trade_no":"E24637891921099581397","out_trade_no":"b9139405052b40503d57dc15e0329b6b","money":"150",
//			"notify_time":"2022-05-26 20:06:00","status":"2","original_trade_no":"20220526005843","subject":"小商品","body":"",
//			"sign":"B5E6D6E1BABA31A9ED8B0E3E3845C7D2"}
            JSONObject parseObject = JSONObject.parseObject(param);
            if (!"1".equals(parseObject.getString("state"))) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(parseObject.getString("outTradeNo"));
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("mchId", parseObject.getString("mchId"));
            paramMap.put("tradeNo", parseObject.getString("tradeNo"));
            paramMap.put("outTradeNo", parseObject.getString("outTradeNo"));
            paramMap.put("amount", parseObject.getInteger("amount"));
            paramMap.put("notifyTime", parseObject.getString("notifyTime"));
            paramMap.put("subject", parseObject.getString("subject"));
            paramMap.put("state", parseObject.getString("state"));
            paramMap.put("originTradeNo", parseObject.getString("originTradeNo"));
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            String sign = parseObject.getString("sign");
            if (!valid.equalsIgnoreCase(sign)) {
                logger.error("密钥签名验证错误, sign = {}, valid = {}", sign, valid);
                return R.fail("密钥签名验证错误, sign = " + sign + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(parseObject.getString("trade_no"));
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("全盛科技支付出错: ", e);
            return R.fail("全盛科技支付出错");
        }
    }

    @GetMapping(value = "/yypay")
    public R yypay(String app_id, String order_sn, BigDecimal order_amount, String yy_order_sn, String bank_code, String pay_status, String signature) {
        try {
            logger.info("yypay notify param : app_id = {}, order_sn = {}, order_amount = {}, yy_order_sn = {}, bank_code = {}, pay_status = {}, signature = {}", //
                    app_id, order_sn, order_amount, yy_order_sn, bank_code, pay_status, signature);
            if (!"1".equals(pay_status)) {
                return R.fail(ERROR);
            }
            PayOrderVo order = this.payOrderService.selectByOrderNum(order_sn);
            if (order == null) {
                return R.fail(ERROR);
            }
            PayConfig config = this.payConfigService.selectById(order.getPayConfigId());
            if (config == null) {
                return R.fail(ERROR);
            }
            PayConfigDetail detail = this.payConfigDetailService.selectById(config.getPayConfigDetailId());
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("app_id", app_id);
            paramMap.put("order_sn", order_sn);
            paramMap.put("order_amount", order_amount);
            paramMap.put("yy_order_sn", yy_order_sn);
            paramMap.put("bank_code", bank_code);
            paramMap.put("pay_status", pay_status);
            String valid = SignUtil.buildSign(paramMap, detail.getNotifyKey()).toUpperCase();
            if (!valid.equalsIgnoreCase(signature)) {
                logger.error("密钥签名验证错误, signature = {}, valid = {}", signature, valid);
                return R.fail("密钥签名验证错误, signature = " + signature + ", valid = " + valid + "");
            }
            User user = this.userService.selectById(order.getUserid());
            if (user == null) {
                logger.error("用户不存在 id = {}", order.getUserid());
                return R.fail("用户不存在 id = " + order.getUserid() + "");
            }
            if (order.getStatus().byteValue() != Dictionary.PayStatusEnum.PAYING.getKey().byteValue()) {
                logger.error("订单状态不对2 order_num = {}", order.getOrderNum());
                return R.fail("订单状态不对 order_num = " + order.getOrderNum() + "");
            }
            // 更新订单
            order.setStatus(Dictionary.PayStatusEnum.SUCCESS.getKey());
            order.setClientOrderNum(yy_order_sn);
            this.payOrderService.save(order);
            // 激活产品
            this.productActiveService.active(order.getProductid(), user, order.getType(), order);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("ed回调异常 : ", e);
            R.fail("ed回调异常");
        }
        return null;
    }
}
