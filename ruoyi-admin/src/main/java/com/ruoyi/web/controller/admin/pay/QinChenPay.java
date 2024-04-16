package com.ruoyi.web.controller.admin.pay;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class QinChenPay implements StatusCode {

	private final static Logger logger = LoggerFactory.getLogger(QinChenPay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail, String ip) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("pay_memberid", detail.getMerchant());
		paramMap.put("pay_bankcode", type);
		paramMap.put("pay_orderid", orderNum);
		paramMap.put("pay_amount", amount);
		paramMap.put("pay_notifyurl", detail.getNotifyUrl());
		if (StrUtil.isNotEmpty(detail.getReturnUrl())) {
			paramMap.put("pay_callbackurl", detail.getReturnUrl());
		}
		paramMap.put("pay_md5sign", SignUtil.buildSign(paramMap, detail.getKey()).toUpperCase());
		String result = HttpClientUtil.sendPost(detail.getUrl(),paramMap);
		logger.info("qinchen pay result = {}", result);
		JSONObject parseObject = JSONObject.parseObject(result);
		if ("200".equals(parseObject.getString("status"))) {
			return DeShengPay.result(SUCCESS, OK, parseObject.getString("data"));
		}
		return DeShengPay.result(ERROR, parseObject.getString("msg"));
	}
	public static void main(String[] args) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("pay_orderid", "123213213");
		paramMap.put("pay_amount", new BigDecimal(200));
		paramMap.put("pay_notifyurl", "https://www.baidu.com");
		paramMap.put("pay_callbackurl", "https://www.baidu.com");
		paramMap.put("pay_memberid", "240331231");
		paramMap.put("pay_bankcode", "666");
		String sign = SignUtil.buildSign(paramMap, "kjN6McuPd6dRrskNQw0FnF4C8vBogewC").toUpperCase();
		paramMap.put("pay_md5sign", sign);
		String result = HttpClientUtil.sendPost("https://qc1.top/Pay", paramMap);
		System.out.println(result);
	}

}
