package com.ruoyi.web.controller.admin.pay;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;

public class ChuanHuPay implements StatusCode {

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("pid", detail.getMerchant());
		paramMap.put("type", type);
		paramMap.put("out_trade_no", orderNum);
		paramMap.put("notify_url", detail.getNotifyUrl());
		paramMap.put("return_url", detail.getReturnUrl());
		paramMap.put("name", "vip会员");
		paramMap.put("money", amount);
		String sign = SignUtil.buildSign(paramMap, detail.getKey(), true);
		paramMap.put("sign", sign);
		paramMap.put("sign_type", "MD5");
		Set<Entry<String, Object>> entrySet = paramMap.entrySet();
		String result = detail.getUrl() + "?";
		for (Entry<String, Object> entry : entrySet) {
			result += entry.getKey() + "=" + entry.getValue() + "&";
		}
		return DeShengPay.result(SUCCESS, OK, result.substring(0, result.length() - 1));
	}

}
