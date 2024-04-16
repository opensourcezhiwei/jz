package com.ruoyi.web.controller.admin.pay;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import com.ruoyi.system.zny.utils.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class PubPay implements StatusCode {

	private static final Logger logger = LoggerFactory.getLogger(PubPay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("merid", detail.getMerchant());
		paramMap.put("reqsn", orderNum);
		String r8 = RandomUtil.generateMixString(8);
		paramMap.put("randstr", r8);
		paramMap.put("paytype", type);
		paramMap.put("ormon", amount.multiply(new BigDecimal(100)).intValue());
		paramMap.put("mreurl", detail.getNotifyUrl());
		String sign = detail.getMerchant() + "&" + detail.getNotifyUrl() + "&" + paramMap.get("ormon") + "&" + type + //
				"&" + r8 + "&" + orderNum;
//		String sign = "X907Pjq5&http://45.204.2.49&9900&8&" + r8 + "&" + orderNum;
		paramMap.put("sign", SignUtil.hmacSHA256(sign, detail.getKey()));
		String result = HttpClientUtil.sendPost(detail.getUrl() + "getpay", paramMap);
//		{"status":200,"msg":"TokenisOk","data":"TP0406195126624D7EBE15C8497"}
		JSONObject parseObject = JSONObject.parseObject(result);
		if ("200".equals(parseObject.getString("status"))) {
			return DeShengPay.result(SUCCESS, OK, postHtmlFrom(detail.getUrl() + "topay", parseObject.getString("data"), "POST"));
		}
		logger.error("pubpay result = {}", result);
		return DeShengPay.result(ERROR, parseObject.getString("msg"));
	}

	public static String postHtmlFrom(String Url, String token, String method) {
		String FormString = "<form  id='actform' name='actform' method='" + method + "' action='" + Url + "'>";
		FormString += "<input name='token' type='text' value='" + token + "'>";
		FormString += "</form>";
		return FormString;
	}
	
}
