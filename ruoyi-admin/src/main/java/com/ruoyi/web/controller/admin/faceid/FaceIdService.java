package com.ruoyi.web.controller.admin.faceid;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.faceid.v20180301.FaceidClient;
import com.tencentcloudapi.faceid.v20180301.models.LivenessCompareRequest;
import com.tencentcloudapi.faceid.v20180301.models.LivenessCompareResponse;

@Service
public class FaceIdService {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private TencentProperties tencentProperties;

	private Credential cred;

	private FaceidClient client;

	@PostConstruct
	public void init() {
		cred = new Credential(tencentProperties.getSecretId(), tencentProperties.getSecretKey());
		HttpProfile httpProfile = new HttpProfile();
		httpProfile.setEndpoint(tencentProperties.getEndpoint());
		// 实例化一个client选项，可选的，没有特殊需求可以跳过
		ClientProfile clientProfile = new ClientProfile();
		clientProfile.setHttpProfile(httpProfile);
		// 实例化要请求产品的client对象,clientProfile是可选的
		client = new FaceidClient(cred, tencentProperties.getRegion(), clientProfile);
	}

	/**
	 * 活体人脸比对 图片->视频 进行比对
	 * 
	 * @param livenessType LIP/ACTION/SILENT
	 * @param imageUrl     图片地址
	 * @param videoBase64  base64的视频加密字符
	 */
	public void livenessCompare(String livenessType, String imageUrl, String videoBase64) {
		try {
			// 实例化一个请求对象,每个接口都会对应一个request对象
			LivenessCompareRequest req = new LivenessCompareRequest();
			req.setImageUrl(imageUrl);
			req.setLivenessType(imageUrl);
			req.setOptional("{\"BestFrameNum\":10}");
			req.setVideoBase64(videoBase64);
			// 返回的resp是一个LivenessCompareResponse的实例，与请求对象对应
			LivenessCompareResponse resp = client.LivenessCompare(req);
			// 输出json格式的字符串回包
			log.info("活体人脸比对 result = {}", LivenessCompareResponse.toJsonString(resp));
		} catch (TencentCloudSDKException e) {
			log.error("活体人脸比对出错 : ", e);
		} catch (Exception e) {
			log.error("活体人脸比对出错2 : ", e);
		}
	}
}
