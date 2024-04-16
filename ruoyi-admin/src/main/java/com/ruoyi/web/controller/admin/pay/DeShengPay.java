package com.ruoyi.web.controller.admin.pay;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DeShengPay implements StatusCode {

	private static final Logger logger = LoggerFactory.getLogger(DeShengPay.class);

	public static String pay(BigDecimal amount, String orderNum, String type) {
		Integer method = null;
		if ("wechat".equals(type)) {
			method = 815;
		}
		if (method == null) {
			return ERROR;
		}
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("sign_type", "MD5");
		paramMap.put("format", "JSON");
		paramMap.put("charset", "utf-8");
		paramMap.put("mch_code", "180246");
		paramMap.put("out_trade_no", orderNum);
		paramMap.put("method", method);
		paramMap.put("amount", amount.intValue());
		paramMap.put("notify_url", "http://45.204.2.49:8093/zny/notify/desheng");
		paramMap.put("sign", SignUtil.buildSign(paramMap, "de690076d40d8eff6a82686fef8023f25b52bd86e232bc7884fb7d466e24157f").toLowerCase());
		String result = HttpClientUtil.sendPostByJson("http://16.162.250.42:9098/gateway/pay", JSONObject.toJSONString(paramMap));
		// {"charset":"utf-8","amount":100.00,"out_trade_no":"1648843625172","plat_sign":"873e7e7c50cbe4462900ec53cc6e02d1","plat_date_time":"220402040707341","fee":26.00,"format":"JSON","plat_order_no":"DS80220402040706489251","plat_resp_code":"SUCCESS","plat_resp_msg":"交易成功","plat_sign_type":"MD5","pay_url":"http://18.162.32.214:10009/pay/alipay/pay1.html?id=802204020407064981407"}
		JSONObject parseObject = JSONObject.parseObject(result);
		if ("SUCCESS".equals(parseObject.getString("plat_resp_code"))) {
			return parseObject.getString("pay_url");
		}
		logger.error("desheng pay 错误, result = {}", result);
		return ERROR;
	}

	public static Map<String, Object> result(String code, Object msg, Object data) {
		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put(STATUS, code);
		resultMap.put(MESSAGE, msg);
		resultMap.put(DATA, data);
		return resultMap;
	}

	public static Map<String, Object> result(String code, Object msg) {
		return result(code, msg, null);
	}

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("sign_type", "MD5");
		paramMap.put("format", "JSON");
		paramMap.put("charset", "utf-8");
		paramMap.put("mch_code", detail.getMerchant());
		paramMap.put("out_trade_no", orderNum);
		paramMap.put("method", type);
		paramMap.put("amount", amount.intValue());
		paramMap.put("notify_url", detail.getNotifyUrl());
		paramMap.put("sign", SignUtil.buildSign(paramMap, detail.getKey()).toLowerCase());
		String result = HttpClientUtil.sendPostByJson(detail.getUrl(), JSONObject.toJSONString(paramMap));
		// {"charset":"utf-8","amount":100.00,"out_trade_no":"1648843625172","plat_sign":"873e7e7c50cbe4462900ec53cc6e02d1","plat_date_time":"220402040707341","fee":26.00,"format":"JSON","plat_order_no":"DS80220402040706489251","plat_resp_code":"SUCCESS","plat_resp_msg":"交易成功","plat_sign_type":"MD5","pay_url":"http://18.162.32.214:10009/pay/alipay/pay1.html?id=802204020407064981407"}
		JSONObject parseObject = JSONObject.parseObject(result);
		if ("SUCCESS".equals(parseObject.getString("plat_resp_code"))) {
			return result(SUCCESS, OK, parseObject.getString("pay_url"));
		}
		logger.error("desheng pay 错误, result = {}", result);
		return result(ERROR, parseObject.getString("plat_resp_msg"));
	}

}
