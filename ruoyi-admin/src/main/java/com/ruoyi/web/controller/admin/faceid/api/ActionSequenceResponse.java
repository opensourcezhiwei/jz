package com.ruoyi.web.controller.admin.faceid.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionSequenceResponse {

	/**
	 * 动作顺序(2,1 or 1,2) 。1代表张嘴，2代表闭眼。
	 */
	@JsonProperty("ActionSequence")
	private String actionSequence;

	/**
	 * 唯一请求 ID，每次请求都会返回。定位问题时需要提供该次请求的 RequestId。
	 */
	@JsonProperty("RequestId")
	private String requestId;

	@JsonProperty("Error")
	private Error error;

	public String getActionSequence() {
		return actionSequence;
	}

	public void setActionSequence(String actionSequence) {
		this.actionSequence = actionSequence;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public Error getError() {
		return error;
	}

	public void setError(Error error) {
		this.error = error;
	}

}