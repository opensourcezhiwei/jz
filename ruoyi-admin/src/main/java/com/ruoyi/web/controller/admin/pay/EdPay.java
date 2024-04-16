package com.ruoyi.web.controller.admin.pay;

import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.DateUtil;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class EdPay implements StatusCode {

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("pay_memberid", detail.getMerchant());
		paramMap.put("pay_orderid", orderNum);
		paramMap.put("pay_applydate", DateUtil.getCurTime());
		paramMap.put("pay_amount", amount);
		paramMap.put("pay_bankcode", type);
		paramMap.put("pay_callbackurl", detail.getReturnUrl());
		paramMap.put("pay_notifyurl", detail.getNotifyUrl());
		String sign = SignUtil.buildSign(paramMap, detail.getKey());
		paramMap.put("pay_md5sign", sign.toUpperCase());
		paramMap.put("pay_productname", "商品购买");
		String result = postHtmlFrom(detail.getUrl(), paramMap);
		return DeShengPay.result(SUCCESS, OK, result);
	}

	public static String postHtmlFrom(String Url, Map paramMap) {
		return postHtmlFrom(Url, paramMap, "post");
	}

	public static String postHtmlFrom(String Url, Map paramMap, String method) {
		if (paramMap.isEmpty()) {
			return "参数不能为空！";
		}
		String FormString = "<form  id='actform' name='actform' method='" + method + "' action='" + Url + "'>";
		for (Object key : paramMap.keySet()) {
			FormString += "<input name='" + key + "' type='hidden' value='" + paramMap.get(key) + "'>";
		}

//		FormString += "</form><script>document.forms[0].submit();</script>";
		FormString += "</form>";
		return FormString;
	}

}
