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

public class ZonghePay implements StatusCode {

	private static final Logger logger = LoggerFactory.getLogger(ZonghePay.class);

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
		paramMap.put("pay_productname", "商品购买");
		String result = HttpClientUtil.sendPost(detail.getUrl(), paramMap);
		logger.info("zonghe result = {}", result);
		JSONObject parseObject = JSONObject.parseObject(result);
		if (parseObject.getInteger("code") != null && parseObject.getIntValue("code") == 200) {
			return DeShengPay.result(SUCCESS, OK, parseObject.getString("data"));
		}
		return DeShengPay.result(ERROR, parseObject.getString("msg"));
//		{"status":"error","msg":"签名验证失败","data":{"pay_amount":"500","pay_applydate":"2022-06-09 03:03:16","pay_bankcode":"932","pay_callbackurl":"http://www.baidu.com","pay_md5sign":"19592438E329C6CDFC39F2C7888DF61F","pay_memberid":"220683851","pay_notifyurl":"http://www.baidu.com","pay_orderid":"1654714996145","pay_productname":"lifestyle"}}
//		{"code":200,"msg":"\u6210\u529f","data":"http:\/\/www.p-8015.com\/alipay?orderno=2022060902584309797382858784"}

	}

}
