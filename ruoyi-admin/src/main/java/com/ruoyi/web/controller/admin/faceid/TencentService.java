package com.ruoyi.web.controller.admin.faceid;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.GetGroupInfoRequest;
import com.tencentcloudapi.iai.v20200303.models.GetGroupInfoResponse;

@Service
public class TencentService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Resource
	private TencentProperties tencentProperties;
	
	/**
	 * 获取人员库信息
	 * 
	 * @param groupId 人员库
	 */
	public void getFaceIdDB(String groupId) {
		try {
			// 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
			// 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
			Credential cred = new Credential(tencentProperties.getSecretId(), tencentProperties.getSecretKey());
			// 实例化一个http选项，可选的，没有特殊需求可以跳过
			HttpProfile httpProfile = new HttpProfile();
			httpProfile.setEndpoint(tencentProperties.getEndpoint());
			// 实例化一个client选项，可选的，没有特殊需求可以跳过
			ClientProfile clientProfile = new ClientProfile();
			clientProfile.setHttpProfile(httpProfile);
			// 实例化要请求产品的client对象,clientProfile是可选的
			IaiClient client = new IaiClient(cred, "", clientProfile);
			// 实例化一个请求对象,每个接口都会对应一个request对象
			GetGroupInfoRequest req = new GetGroupInfoRequest();
			req.setGroupId(groupId);
			// 返回的resp是一个GetGroupInfoResponse的实例，与请求对象对应
			GetGroupInfoResponse resp = client.GetGroupInfo(req);
			// 输出json格式的字符串回包
			System.out.println(GetGroupInfoResponse.toJsonString(resp));
		} catch (TencentCloudSDKException e) {
			logger.error("腾讯云出错 : ", e.toString());
		}
	}

}
