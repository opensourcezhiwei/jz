package com.ruoyi.web.controller.admin.pay;

import cn.hutool.core.util.RandomUtil;
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

public class MiaomiaoPay implements StatusCode {

	private final static Logger logger = LoggerFactory.getLogger(MiaomiaoPay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type,String ip, PayConfigDetail detail) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("mchKey", Long.valueOf(detail.getMerchant()));
		paramMap.put("product", type);
		paramMap.put("mchOrderNo", orderNum);
		paramMap.put("mchUserId", "product");
		paramMap.put("userIp", ip);
		paramMap.put("amount", amount.multiply(new BigDecimal(100)).intValue());
		paramMap.put("nonce", RandomUtil.randomString(8));
		paramMap.put("timestamp", System.currentTimeMillis()+"");
		paramMap.put("notifyUrl", detail.getNotifyUrl());
		if (!StringUtils.isNull(detail.getReturnUrl())) {
			paramMap.put("returnUrl", detail.getReturnUrl());
		}
		String sign = SignUtil.buildSign(paramMap, detail.getKey(),true);
		paramMap.put("sign", sign);
		String result = HttpClientUtil.sendPost(detail.getUrl(), paramMap);
		logger.info("MiaomiaoPay reslt = {}", result);
		JSONObject parseObject = JSONObject.parseObject(result);
		if ("200".equals(parseObject.getString("code"))) {
//			{"payOrderId":"JX202205172007287310760","sign":"DADA4E467AAA4E3AEBC72FB9C3722500","payParams":{"payMethod":"codeImg","payUrl":"http://18.166.158.23:7480/pay/?orderid=20220517200729165278924924399"},"retCode":"SUCCESS"}
			return DeShengPay.result(SUCCESS, OK, parseObject.getJSONObject("data").getString("url"));
		}
		return DeShengPay.result(ERROR, parseObject.getString("msg"));
	}

}
