package com.ruoyi.web.controller.admin.faceid.api;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 人脸核身请求参数实体
 */
public class FaceInspectRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 公共参数，本接口取值：LivenessRecognition
	 */
	@JsonProperty("Action")
	private String action = "LivenessRecognition";

	/**
	 * 公共参数，本接口取值：2018-03-01
	 */
	@JsonProperty("Version")
	private String version = "2018-03-01";

	/**
	 * 公共参数，详见产品支持的地域列表
	 */
	@JsonProperty("Region")
	private String region = "ap-beijing";

	/**
	 * 身份证号
	 */
	@JsonProperty("IdCard")
	private String idCard;

	/**
	 * 姓名。中文请使用UTF-8编码。
	 */
	@JsonProperty("Name")
	private String name;

	/**
	 * 用于活体检测的视频，视频的BASE64值； BASE64编码后的大小不超过5M，支持mp4、avi、flv格式。
	 */
	@JsonProperty("VideoBase64")
	private String videoBase64;

	/**
	 * 活体检测类型，取值：LIP/ACTION/SILENT。 LIP为数字模式，ACTION为动作模式，SILENT为静默模式，三种模式选择一种传入。
	 */
	@JsonProperty("LivenessType")
	private String livenessType;

	/**
	 * 数字模式传参：数字验证码(1234)，需先调用接口获取数字验证码； 动作模式传参：传动作顺序(2,1 or 1,2)，需先调用接口获取动作顺序；
	 * 静默模式传参：空。
	 */
	@JsonProperty("ValidateData")
	private String validateData;

	public FaceInspectRequest() {
	}

	public FaceInspectRequest(String idCard, String name, String videoBase64, String livenessType, String validateData) {
		this.idCard = idCard;
		this.name = name;
		this.videoBase64 = videoBase64;
		this.livenessType = livenessType;
		this.validateData = validateData;
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getIdCard() {
		return idCard;
	}

	public void setIdCard(String idCard) {
		this.idCard = idCard;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVideoBase64() {
		return videoBase64;
	}

	public void setVideoBase64(String videoBase64) {
		this.videoBase64 = videoBase64;
	}

	public String getLivenessType() {
		return livenessType;
	}

	public void setLivenessType(String livenessType) {
		this.livenessType = livenessType;
	}

	public String getValidateData() {
		return validateData;
	}

	public void setValidateData(String validateData) {
		this.validateData = validateData;
	}

	@Override
	public String toString() {
		return "FaceInspectRequest{" + "action='" + action + '\'' + ", version='" + version + '\'' + ", region='" + region + '\'' + ", idCard='" + idCard + '\'' + ", name='" + name + '\'' + ", videoBase64='" + videoBase64 + '\'' + ", livenessType='" + livenessType + '\'' + ", validateData='" + validateData + '\'' + '}';
	}
}