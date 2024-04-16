package com.ruoyi.web.controller.admin.faceid;

public class FaceIdUserVo {

	private String webankAppId;

	/**
	 * 订单号，由合作方上送，每次唯一，不能超过32位
	 */
	private String orderNo;

	/**
	 * 姓名
	 */
	private String name;

	/**
	 * 证件号码
	 */
	private String idNo;

	/**
	 * 用户 ID ，用户的唯一标识（不能带有特殊字符）
	 */
	private String userId;

	/**
	 * 比对源照片，注意：原始图片不能超过500KB，且必须为 JPG 或 PNG 格式
	 * 参数有值：使用合作伙伴提供的比对源照片进行比对，必须注照片是正脸可信照片，照片质量由合作方保证 参数为空 ：根据身份证号+姓名使用权威数据源比对
	 */
	private String sourcePhotoStr;

	/**
	 * 比对源照片类型，注意： 如合作方上送比对源则必传，使用权威数据源可不传 参数值为1：水纹正脸照 参数值为2：高清正脸照
	 */
	private String sourcePhotoType;

	/**
	 * 默认参数值为：1.0.0
	 */
	private String version = "1.0.0";

	/**
	 * 签名
	 */
	private String sign;

	public FaceIdUserVo() {
		super();
	}

	public FaceIdUserVo(String webankAppId, String orderNo, String name, String idNo, String userId, String sourcePhotoStr, String sourcePhotoType, String version, String sign) {
		super();
		this.webankAppId = webankAppId;
		this.orderNo = orderNo;
		this.name = name;
		this.idNo = idNo;
		this.userId = userId;
		this.sourcePhotoStr = sourcePhotoStr;
		this.sourcePhotoType = sourcePhotoType;
		this.version = version;
		this.sign = sign;
	}

	public String getWebankAppId() {
		return webankAppId;
	}

	public void setWebankAppId(String webankAppId) {
		this.webankAppId = webankAppId;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIdNo() {
		return idNo;
	}

	public void setIdNo(String idNo) {
		this.idNo = idNo;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getSourcePhotoStr() {
		return sourcePhotoStr;
	}

	public void setSourcePhotoStr(String sourcePhotoStr) {
		this.sourcePhotoStr = sourcePhotoStr;
	}

	public String getSourcePhotoType() {
		return sourcePhotoType;
	}

	public void setSourcePhotoType(String sourcePhotoType) {
		this.sourcePhotoType = sourcePhotoType;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

}
