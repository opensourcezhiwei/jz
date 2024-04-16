package com.ruoyi.web.controller.admin.faceid.api;

import com.ruoyi.system.zny.utils.FileUtil;
import com.ruoyi.web.controller.admin.faceid.TencentProperties;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.CompareFaceRequest;
import com.tencentcloudapi.iai.v20200303.models.CompareFaceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;

@Component
public class IaiApi {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Resource
	private TencentProperties tencentProperties;

	private Credential cred = null;

	private IaiClient client = null;

	@PostConstruct
	public void init() {
		cred = new Credential(tencentProperties.getSecretId(), tencentProperties.getSecretKey());
		HttpProfile httpProfile = new HttpProfile();
		httpProfile.setEndpoint(tencentProperties.getIaiEndpoint());
		// 实例化一个client选项，可选的，没有特殊需求可以跳过
		ClientProfile clientProfile = new ClientProfile();
		clientProfile.setHttpProfile(httpProfile);
		// 实例化要请求产品的client对象,clientProfile是可选的
		client = new IaiClient(cred, tencentProperties.getRegion(), clientProfile);
	}

	/**
	 * 请求人脸核身接口
	 *
	 * @return
	 */
	public boolean faceCompare(String urla, byte[] imgb) {
		try {
			CompareFaceRequest request = new CompareFaceRequest();
			request.setUrlA(urla);
			request.setImageB(FileUtil.encryptToBase64(imgb));
			request.setFaceModelVersion("3.0");
			CompareFaceResponse resp = client.CompareFace(request);
			logger.info("人脸比对resp = {}", CompareFaceResponse.toJsonString(resp));
			//{"Score":100.0,"FaceModelVersion":"3.0","RequestId":"676036d2-d7fd-4a0d-9021-d65943e6d7b1"}
			//{"Score":2.4567425,"FaceModelVersion":"3.0","RequestId":"f6fae3a1-bf1a-4c86-920b-eb859fa076e3"}
			return new BigDecimal(resp.getScore()).compareTo(new BigDecimal(70)) >= 0;
		} catch (TencentCloudSDKException e) {
			logger.error("人脸核身出错: ", e);
			return false;
		}
	}

	/**
	 * 请求人脸核身接口
	 *
	 * @return
	 */
	public boolean faceCompare(String urla, String urlb) {
		try {
			CompareFaceRequest request = new CompareFaceRequest();
			request.setUrlA(urla);
			request.setUrlB(urlb);
			request.setFaceModelVersion("3.0");
			CompareFaceResponse resp = client.CompareFace(request);
			System.out.println(CompareFaceResponse.toJsonString(resp));
			return false;
		} catch (TencentCloudSDKException e) {
			logger.error("人脸核身出错: ", e.toString());
			return false;
		}
	}

}
