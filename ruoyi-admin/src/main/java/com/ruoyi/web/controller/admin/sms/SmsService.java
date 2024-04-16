package com.ruoyi.web.controller.admin.sms;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.entity.SysConfig;
import com.ruoyi.system.zny.service.CommonService;
import com.ruoyi.system.zny.service.SysConfigService;
import com.ruoyi.system.zny.utils.DateUtil;
import com.ruoyi.system.zny.utils.HttpClientUtil;
import com.ruoyi.system.zny.utils.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SmsService extends CommonService {

	private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

	@Autowired
	private SysConfigService sysConfigService;

	private Map<String, Object> smsMap = new HashMap<>();

	public Map<String, Object> send(String tel, String content) {
		return this.send(tel, content, false);
	}

	public Map<String, Object> send(String tel, String content, boolean isAll) {
		try {
			List<SysConfig> configList = this.sysConfigService.selectByType(6);
			if (configList == null || configList.size() <= 0) {
				return result(ERROR, "短信发送失败");
			}
			SysConfig config = configList.get(0);
			String username = config.getName();
			String password = config.getDesc();
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("userid", config.getSort());
//		paramMap.put("userid", 329);
			String timestamp = DateUtil.format(DateUtil.YYMMDDHHMMSS, new Date());
			paramMap.put("timestamp", timestamp);
			paramMap.put("mobile", tel);
			if (isAll) {
				paramMap.put("content", content);
			}else {
				paramMap.put("content", "【" + config.getMemo() + "】您的验证码为:" + content + ", 请勿泄露!");
			}
			paramMap.put("action", "send");
			paramMap.put("rt", "json");
			paramMap.put("sign", MD5Util.string2MD5(username + password + timestamp).toLowerCase());
//		String result = HttpClientUtil.sendPost("http://118.31.174.89:8088/v2sms.aspx?action=send", paramMap);
			String result = HttpClientUtil.sendPost(config.getUrl(), paramMap);
			// {"ReturnStatus":"Success","Message":"ok","RemainPoint":9,"TaskID":1485036,"SuccessCounts":1}
			JSONObject parseObject = JSONObject.parseObject(result);
			if ("Success".equals(parseObject.getString("ReturnStatus"))) {
				this.smsMap.put(tel, content);
				return result(SUCCESS, OK);
			}
			logger.error("sms result = {}", result);
			return result(ERROR, result);
		} catch (Exception e) {
			logger.error("sms 出错", e);
			return result(MAYBE, "服务器异常");
		}
	}

	public boolean validCode(String tel, String code) {
		if (StringUtils.isNull(tel) || StringUtils.isNull(code)) {
			return false;
		}
		String content = this.smsMap.get(tel) + "";
		if (content.equals(code)) {
			return true;
		}
		return false;
	}

}
