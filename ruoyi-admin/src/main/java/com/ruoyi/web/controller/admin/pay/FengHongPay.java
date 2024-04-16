package com.ruoyi.web.controller.admin.pay;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.DateUtil;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import com.ruoyi.system.zny.utils.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class FengHongPay implements StatusCode {

	private final static Logger logger = LoggerFactory.getLogger(FengHongPay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail, String ip) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("mch_id", detail.getMerchant());
		paramMap.put("pass_code", type);
		paramMap.put("subject", "小商品");
		paramMap.put("out_trade_no", orderNum);
		paramMap.put("amount", amount);
		paramMap.put("client_ip", ip);
		paramMap.put("notify_url", detail.getNotifyUrl());
		paramMap.put("return_url", detail.getReturnUrl());
		paramMap.put("timestamp", DateUtil.getCurTime());
		String sign = SignUtil.buildSign(paramMap, detail.getKey(), true);
		paramMap.put("sign", sign);
		String result = HttpClientUtil.sendPostByJson(detail.getUrl(), JSONUtil.map2Json(paramMap));
		logger.info("fenghong result = {}", result);
//		{"code":0,"msg":"ok","data":{"mch_id":"1024","trade_no":"E24637891881922068444","out_trade_no":"1653562591391","original_trade_no":"E86637891881994385571","money":"300","pay_url":"http://116.255.226.125:8088/api/payPage.html?id=173751","qrcode_url":null,"qrcode_content":null,"sdk_content":null,"expired_time":"2022-05-26 18:59:33"}}
		JSONObject parseObject = JSONObject.parseObject(result);
		if (parseObject.getIntValue("code") == 0) {
			return DeShengPay.result(SUCCESS, OK, parseObject.getJSONObject("data").getString("pay_url"));
		}
		logger.error("丰宏支付错误: {}", parseObject.getString("msg"));
		return DeShengPay.result(ERROR, parseObject.getString("msg"));
	}
	
	
	public static String postHtmlFrom(String Url, String method) {
		String FormString = "<form  id='actform' name='actform' method='" + method + "' action='" + Url + "'>";
		FormString += "</form>";
		return FormString;
	}

}
