package com.ruoyi.web.controller.admin.pay;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class JinNiuPay implements StatusCode {

	private final static Logger logger = LoggerFactory.getLogger(JinNiuPay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail, String ip) {
//		String merchant = "54";
//		String key = "13a7ba0ff00848538ed7b5cb82a2476b";
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("pay_memberid", detail.getMerchant());
		paramMap.put("pay_bankcode", type);
		paramMap.put("pay_orderid", orderNum);
		paramMap.put("pay_amount", amount);
		paramMap.put("pay_notifyurl", detail.getNotifyUrl());
		if (!StringUtils.isNull(detail.getReturnUrl())) {
			paramMap.put("pay_callbackurl", detail.getReturnUrl());
		}
		paramMap.put("pay_md5sign", SignUtil.buildSign(paramMap, detail.getKey()).toUpperCase());
		String result = HttpClientUtil.sendPost(detail.getUrl(), paramMap);
		// {"code":0,"msg":"ok","data":{"mch_id":"54","trade_no":"X17692828771287813","out_trade_no":"1649657985509","psg_trade_no":"0","money":"500","pay_url":"http://pay.baoxuepay.top/pay/payer?orderid=20220411141947164965798735555","expired_time":"2022-04-11
		// 14:24:47","state":1}}
		logger.info("jinniu pay result = {}", result);
		JSONObject parseObject = JSONObject.parseObject(result);
		if ("200".equals(parseObject.getString("status"))) {
			return DeShengPay.result(SUCCESS, OK, parseObject.getString("data"));
		}
		return DeShengPay.result(ERROR, parseObject.getString("msg"));
	}

	public static void main(String[] args) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("pay_orderid", "1312321");
		paramMap.put("pay_amount", new BigDecimal(200));
		paramMap.put("pay_notifyurl", "https://www.baidu.com");
		paramMap.put("pay_callbackurl", "https://www.baidu.com");
		paramMap.put("pay_memberid", "240385754");
		paramMap.put("pay_bankcode", "202");
		String sign = SignUtil.buildSign(paramMap, "B6xDSEoDUVmCPh6zPURtbxXNll1TCy3U").toUpperCase();
		paramMap.put("pay_md5sign", sign);
		String result = HttpClientUtil.sendPost("https://jinniu1.top/Pay", paramMap);
		System.out.println(result);
	}

}
