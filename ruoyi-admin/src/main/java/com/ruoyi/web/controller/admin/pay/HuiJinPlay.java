package com.ruoyi.web.controller.admin.pay;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.DateUtil;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class HuiJinPlay implements StatusCode {

	private static final Logger logger = LoggerFactory.getLogger(HuiJinPlay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("mchId", detail.getMerchant());
		paramMap.put("productId", type);
		paramMap.put("mchOrderNo", orderNum);
		paramMap.put("amount", amount.multiply(new BigDecimal(100)).intValue());
		paramMap.put("currency", "cny");
		paramMap.put("device", "app");
		paramMap.put("notifyUrl", detail.getNotifyUrl());
		paramMap.put("subject", "购买商品");
		paramMap.put("body", "购买商品");
		paramMap.put("reqTime", DateUtil.format(DateUtil.YYMMDDHHMMSS, new Date()));
		paramMap.put("version", "1.0");
		String sign = SignUtil.buildSign(paramMap, detail.getKey()).toUpperCase();
		paramMap.put("sign", sign);
		String result = HttpClientUtil.sendPost(detail.getUrl(), paramMap);
		logger.info("huijinplay result = {}", result);
		JSONObject parseObject = JSONObject.parseObject(result);
		if ("0".equals(parseObject.getString("retCode"))) {
			return DeShengPay.result(SUCCESS, OK, parseObject.getString("payJumpUrl"));
		}
		logger.error("huijinplay支付错误 = {}", result);
		return DeShengPay.result(ERROR, result);
		// {"retCode":"0","sign":"B60E13DEA1137841CAA978932951FBB2","payOrderId":"P01202204062002096472813","payMethod":"formJump","payUrl":"<script>window.location.href
		// =
		// 'http://sm100.cdlmdjt.com/h5/page/pay/PayYDPC.html?businessId=202204062002098086GLL';</script>","payJumpUrl":"https://api.yoooapay.com/api/pay_P01202204062002096820844.htm"}

	}
}
