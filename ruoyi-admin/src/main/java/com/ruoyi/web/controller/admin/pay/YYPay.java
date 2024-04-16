package com.ruoyi.web.controller.admin.pay;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class YYPay implements StatusCode {

	private static final Logger logger = LoggerFactory.getLogger(YYPay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("method", "order");
		paramMap.put("app_id", detail.getMerchant());
		paramMap.put("order_sn", orderNum);
		paramMap.put("order_amount", amount);
		paramMap.put("bank_code", type);
		paramMap.put("return_url", detail.getReturnUrl());
		paramMap.put("notify_url", detail.getNotifyUrl());
		String sign = SignUtil.buildSign(paramMap, detail.getKey());
		paramMap.put("signature", sign.toUpperCase());
//	String result = HttpClientUtil.sendPost("https://api.canaskme.com/Pay_Index.html", paramMap);
//		String result = EdPay.postHtmlFrom(detail.getUrl(), paramMap, "post");
		String result = HttpClientUtil.sendPost(detail.getUrl() + "/order", paramMap);
		logger.info("YYPay result = {}", result);
		JSONObject parseObject = JSONObject.parseObject(result);
		if ("0".equals(parseObject.getString("code"))) {
			return DeShengPay.result(SUCCESS, OK, parseObject.getJSONObject("data").getString("url"));
		}
		return DeShengPay.result(SUCCESS, OK, parseObject.getString("msg"));
	}

}
