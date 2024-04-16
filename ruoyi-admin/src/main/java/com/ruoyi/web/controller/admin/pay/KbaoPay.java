package com.ruoyi.web.controller.admin.pay;

import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class KbaoPay implements StatusCode {

	private static final Logger logger = LoggerFactory.getLogger(KbaoPay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("pay_memberid", detail.getMerchant());
		paramMap.put("pay_orderid", orderNum);
		paramMap.put("pay_applydate", DateUtil.getCurTime());
		paramMap.put("pay_amount", amount);
		paramMap.put("pay_bankcode", type);
		paramMap.put("pay_callbackurl", detail.getReturnUrl());
		paramMap.put("pay_notifyurl", detail.getNotifyUrl());
		String sign = SignUtil.buildSign(paramMap, detail.getNotifyKey());
		paramMap.put("pay_md5sign", sign.toUpperCase());
		paramMap.put("pay_productname", "lifestyle");
//		String result = HttpClientUtil.sendPost("http://154.213.28.77:8686/Pay_Index.html", paramMap);
		String result = EdPay.postHtmlFrom("http://154.213.28.77:8686/Pay_Index.html", paramMap);
		return DeShengPay.result(SUCCESS, OK, result);
	}

}
