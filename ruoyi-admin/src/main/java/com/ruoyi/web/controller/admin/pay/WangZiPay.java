package com.ruoyi.web.controller.admin.pay;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.DateUtil;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import com.ruoyi.system.zny.utils.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class WangZiPay implements StatusCode {

	private static final Logger logger = LoggerFactory.getLogger(WangZiPay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("pay_memberid", detail.getMerchant());
		paramMap.put("pay_orderid", orderNum);
		paramMap.put("pay_applydate", DateUtil.getCurTime());
		paramMap.put("pay_amount", amount);
		paramMap.put("pay_bankcode", type);
		paramMap.put("pay_callbackurl", detail.getReturnUrl());
		paramMap.put("pay_notifyurl", detail.getNotifyUrl());
		String key = MD5Util.string2MD5(paramMap.get("pay_orderid") + "UV" + detail.getKey());
		String sign = SignUtil.buildSign(paramMap, key);
		paramMap.put("pay_md5sign", sign.toUpperCase());
		paramMap.put("pay_productname", "lifestyle");
		String result = HttpClientUtil.sendPost(detail.getUrl(), paramMap);
		logger.info("wangzi result = {}", result);
		JSONObject parseObject = JSONObject.parseObject(result);
		if (parseObject.getIntValue("code") == 200) {
			return DeShengPay.result(SUCCESS, OK, parseObject.getString("url"));
		}
		return DeShengPay.result(ERROR, parseObject.getString("msg"));
	}

}
