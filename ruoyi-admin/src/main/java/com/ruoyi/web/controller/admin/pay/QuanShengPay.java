package com.ruoyi.web.controller.admin.pay;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import com.ruoyi.system.zny.utils.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class QuanShengPay implements StatusCode {

	private final static Logger logger = LoggerFactory.getLogger(QuanShengPay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail, String ip) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("mchId", detail.getMerchant());
		paramMap.put("wayCode", type);
		paramMap.put("subject", "小商品");
		paramMap.put("outTradeNo", orderNum);
		paramMap.put("amount", amount.multiply(new BigDecimal(100)).intValue());
		paramMap.put("clientIp", ip);
		paramMap.put("notifyUrl", detail.getNotifyUrl());
		paramMap.put("returnUrl", detail.getReturnUrl());
		paramMap.put("reqTime", System.currentTimeMillis());
		String sign = SignUtil.buildSign(paramMap, detail.getKey()).toUpperCase();
		paramMap.put("sign", sign);
		String result = HttpClientUtil.sendPostByJson(detail.getUrl(), JSONUtil.map2Json(paramMap));
		logger.info("quansheng result = {}", result);
//		{"code":0,"msg":"ok","data":{"mch_id":"1024","trade_no":"E24637891881922068444","out_trade_no":"1653562591391","original_trade_no":"E86637891881994385571","money":"300","pay_url":"http://116.255.226.125:8088/api/payPage.html?id=173751","qrcode_url":null,"qrcode_content":null,"sdk_content":null,"expired_time":"2022-05-26 18:59:33"}}
		JSONObject parseObject = JSONObject.parseObject(result);
		if (parseObject.getIntValue("code") == 0) {
			return DeShengPay.result(SUCCESS, OK, parseObject.getJSONObject("data").getString("payUrl"));
		}
		logger.error("quansheng支付错误: {}", parseObject.getString("msg"));
		return DeShengPay.result(ERROR, parseObject.getString("msg"));
	}

}
