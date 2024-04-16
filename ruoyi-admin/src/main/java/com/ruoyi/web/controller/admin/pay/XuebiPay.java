package com.ruoyi.web.controller.admin.pay;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.DateUtil;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class XuebiPay implements StatusCode {

	private static final Logger logger = LoggerFactory.getLogger(XuebiPay.class);

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
		paramMap.put("pay_productname", "lifestyle");
		String result = HttpClientUtil.sendPost(detail.getUrl(), paramMap);
		JSONObject parseObject = JSONObject.parseObject(result);
		if ("0".equals(parseObject.getString("code"))) {
			return DeShengPay.result(SUCCESS, OK, parseObject.getString("payUrl"));
		}
//		{
//		    "code": 0,
//		    "transaction_id": "20220610124046101515",
//		    "payUrl": "http://106.52.162.154:80/api/payPage.html?id=347627"
//		}
		return DeShengPay.result(ERROR, result);
	}

}
