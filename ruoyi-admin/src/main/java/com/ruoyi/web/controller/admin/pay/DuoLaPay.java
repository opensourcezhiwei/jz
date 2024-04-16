package com.ruoyi.web.controller.admin.pay;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfig;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

 public class DuoLaPay implements StatusCode {

	private static final Logger logger = LoggerFactory.getLogger(DuoLaPay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, PayConfig config, PayConfigDetail detail, String ip) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("orderId", orderNum);
		paramMap.put("orderAmount", amount);
		paramMap.put("notifyUrl", detail.getNotifyUrl());
		paramMap.put("returnUrl", detail.getReturnUrl());
		paramMap.put("merchantId", detail.getMerchant());
		paramMap.put("channelType", config.getType());
		paramMap.put("payer_ip", ip);
		String sign = SignUtil.buildSign(paramMap, detail.getKey()).toLowerCase();
		paramMap.put("sign", sign);
		String result = HttpClientUtil.sendPost(detail.getUrl(), paramMap);
		JSONObject parseObject = JSONObject.parseObject(result);
		if ("200".equals(parseObject.getString("code"))) {
			return DeShengPay.result(SUCCESS, OK, parseObject.getJSONObject("data").getString("payUrl"));
		}
		logger.error("duola result = {}", result);
		return DeShengPay.result(ERROR, parseObject.getString("msg"));
	}

	 public static void main(String[] args) {
		 Map<String, Object> paramMap = new TreeMap<>();
		 paramMap.put("orderId", "1312321");
		 paramMap.put("orderAmount", new BigDecimal(200));
		 paramMap.put("notifyUrl", "https://www.baidu.com");
		 paramMap.put("returnUrl", "https://www.baidu.com");
		 paramMap.put("merchantId", "10014");
		 paramMap.put("channelType", "103");
		 paramMap.put("payer_ip", "127.0.0.1");
		 String sign = SignUtil.buildSign(paramMap, "c5e048664aa8bfc4ac0bed87f39348d2").toLowerCase();
		 paramMap.put("sign", sign);
		 String result = HttpClientUtil.sendPost("http://duola.just168.vip/api/newOrder", paramMap);
		 System.out.println(result);
	 }

}
