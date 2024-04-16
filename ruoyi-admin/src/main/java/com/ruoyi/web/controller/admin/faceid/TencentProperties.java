package com.ruoyi.web.controller.admin.faceid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class TencentProperties {

	@Value("${tencent.faceid.appid:1}")
	private String appId;

	@Value("${tencent.faceid.access-key-id:1}")
	private String secretId;

	@Value("${tencent.faceid.access-key-secret:1}")
	private String secretKey;

	@Value("${tencent.faceid.endpoint:1}")
	private String endpoint;

	@Value("${tencent.faceid.iai-endpoint:1}")
	private String iaiEndpoint;

	@Value("${tencent.faceid.region:1}")
	private String region;

	public TencentProperties() {
		super();
	}

	public TencentProperties(String appId, String secretId, String secretKey, String endpoint, String iaiEndpoint, String region) {
		super();
		this.appId = appId;
		this.secretId = secretId;
		this.secretKey = secretKey;
		this.endpoint = endpoint;
		this.iaiEndpoint = iaiEndpoint;
		this.region = region;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getSecretId() {
		return secretId;
	}

	public void setSecretId(String secretId) {
		this.secretId = secretId;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getIaiEndpoint() {
		return iaiEndpoint;
	}

	public void setIaiEndpoint(String iaiEndpoint) {
		this.iaiEndpoint = iaiEndpoint;
	}

}
