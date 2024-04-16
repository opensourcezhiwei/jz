package com.ruoyi.web.controller.admin.pay;

import com.ruoyi.system.zny.utils.EncUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SignUtil {

	private static final Logger logger = LoggerFactory.getLogger(SignUtil.class);

	public static String buildSign(Map<String, Object> paramMap) {
		Set<Entry<String, Object>> entrySet = paramMap.entrySet();
		StringBuffer buffer = new StringBuffer();
		for (Entry<String, Object> entry : entrySet) {
			buffer.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
		}
		return EncUtil.toMD5(buffer.substring(0, buffer.length() - 1).toString());
	}

	public static String buildSign(Map<String, Object> paramMap, String encrypt) {
		return buildSign(paramMap, encrypt, false);
	}

	public static String buildSign(Map<String, Object> paramMap, String encrypt, boolean filterKey) {
		Set<Entry<String, Object>> entrySet = paramMap.entrySet();
		StringBuilder buffer = new StringBuilder();
		for (Entry<String, Object> entry : entrySet) {
			buffer.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
		}
		String bufferStr = null;
		if (filterKey) {
			bufferStr = buffer.substring(0, buffer.length() - 1) + encrypt;
		} else {
			bufferStr = buffer.append("key=").append(encrypt).toString();
		}
		logger.info("原串 = {}", bufferStr);

		return EncUtil.toMD5(bufferStr);
	}

	/**
	 * xiaoyu
	 *
	 * @param data 待签名数据
	 * @param key
	 * @return SHA256编码在Base64编码，最后再使用UrlEncode
	 */
	public static String hmacSHA256(String data, String key) {
		String str = "";
		try {
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
			sha256_HMAC.init(secret_key);
			byte[] array = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
			String ars = Base64.getEncoder().encodeToString(array);
			str = URLEncoder.encode(ars, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return str;
	}
}
