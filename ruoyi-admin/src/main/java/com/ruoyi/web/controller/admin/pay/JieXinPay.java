package com.ruoyi.web.controller.admin.pay;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.constants.StatusCode;
import com.ruoyi.system.zny.entity.PayConfigDetail;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

/**
 * mm支付  jiexin 支付
 */
public class JieXinPay implements StatusCode {

	private final static Logger logger = LoggerFactory.getLogger(JieXinPay.class);

	public static Map<String, Object> pay(BigDecimal amount, String orderNum, String type, PayConfigDetail detail) {
		Map<String, Object> paramMap = new TreeMap<>();
		paramMap.put("mchId", Long.valueOf(detail.getMerchant()));
		paramMap.put("productId", Integer.valueOf(type));
		paramMap.put("mchOrderNo", orderNum);
		paramMap.put("amount", amount.multiply(new BigDecimal(100)).intValue());
		paramMap.put("notifyUrl", detail.getNotifyUrl());
		if (!StringUtils.isNull(detail.getReturnUrl())) {
			paramMap.put("returnUrl", detail.getReturnUrl());
		}
		String sign = SignUtil.buildSign(paramMap, detail.getKey());
		paramMap.put("sign", sign);
		String result = HttpClientUtil.sendPost(detail.getUrl(), paramMap);
		logger.info("jiexin reslt = {}", result);
		JSONObject parseObject = JSONObject.parseObject(result);
		if ("SUCCESS".equals(parseObject.getString("retCode"))) {
//			{"payOrderId":"JX202205172007287310760","sign":"DADA4E467AAA4E3AEBC72FB9C3722500","payParams":{"payMethod":"codeImg","payUrl":"http://18.166.158.23:7480/pay/?orderid=20220517200729165278924924399"},"retCode":"SUCCESS"}
			return DeShengPay.result(SUCCESS, OK, parseObject.getJSONObject("payParams").getString("payUrl"));
		}
		return DeShengPay.result(ERROR, parseObject.getString("errDes"));
	}

}
