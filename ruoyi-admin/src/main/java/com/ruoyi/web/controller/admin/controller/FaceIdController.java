package com.ruoyi.web.controller.admin.controller;

import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.entity.FaceId;
import com.ruoyi.system.zny.service.FaceService;
import com.ruoyi.web.controller.admin.config.MultipartFileUtil;
import com.ruoyi.web.controller.admin.faceid.api.ActionSequenceResponse;
import com.ruoyi.web.controller.admin.faceid.api.FaceApi;
import com.ruoyi.web.controller.admin.faceid.api.IaiApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.MAYBE;

@RestController
@RequestMapping(value = "/face")
@Api("人脸识别")
public class FaceIdController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * 动作顺序
	 */
	private String validateData = "1,2";

	@Autowired
	private FaceApi faceApi;

	@Autowired
	private IaiApi iaiApi;

	@Autowired
	private FaceService faceIdService;

	/**
	 * 人脸上传
	 * 
	 * @return
	 */
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "faceUpload")
	public R faceUpload(String username, String faceUrl) {
		try {
			if (StringUtils.isNull(username)) {
				return R.fail(ERROR, "参数不能为空");
			}
			FaceId face = this.faceIdService.selectByUsername(username);
			if (face == null) {
				face = new FaceId();
				face.setUsername(username);
			}
			if (StringUtils.isNull(face.getFaceUrl())) {
				face.setFaceUrl("[]");
			}
			JSONArray parseArray = JSONArray.parseArray(face.getFaceUrl());
			if (parseArray.size() > 3) {
				return R.fail(ERROR, "人脸过多");
			}
			parseArray.add(faceUrl);
			face.setFaceUrl(parseArray.toJSONString());
			this.faceIdService.save(face);
			return R.ok(face);
		} catch (Exception e) {
			logger.error("人脸核身出错 : ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@ApiImplicitParams({ //
	})
	@PostMapping(value = "faceCompareByBase64")
	public R faceCompareByBase64(String tel, String base64) {
		try {
			logger.info("base64 = {}", base64);
			MultipartFile file = MultipartFileUtil.base64ToMultipart(base64);
			return this.faceCompare(tel, file);
		} catch (Exception e) {
			logger.error("人脸base64: ", e);
			return R.fail(MAYBE, "服务器出错 : ");
		}
	}

	@ApiImplicitParams({ //
	})
	@PostMapping(value = "faceCompare", consumes = "multipart/*", headers = "content-type=multipart/form-data")
	public R faceCompare(String tel, @RequestPart("file") MultipartFile file) {
		try {
			if (StringUtils.isNull(tel)) {
				return R.fail(ERROR, "tel不能为空");
			}
			if (file.isEmpty()) {
				return R.fail(ERROR, "file is empty!");
			}
			if (!file.getOriginalFilename().contains(".")) {
				return R.fail(ERROR, "file format error, must contains . suffix ");
			}
			if (!file.getContentType().contains("image")) {
				return R.fail(ERROR, "文件类型错误");
			}
			FaceId faceId = this.faceIdService.selectByUsername(tel);
			if (faceId == null) {
				return R.fail(ERROR, "人脸尚未收集");
			}
			if (StringUtils.isNull(faceId.getFaceUrl())) {
				return R.fail(ERROR, "人脸尚未收集");
			}
			JSONArray parseArray = JSONArray.parseArray(faceId.getFaceUrl());
			if (parseArray.size() <= 0) {
				return R.fail(ERROR, "人脸尚未收集");
			}
			boolean isCompare = this.iaiApi.faceCompare(parseArray.getString(0), file.getBytes());
			return R.ok(isCompare);
		} catch (Exception e) {
			logger.error("人脸比对", e);
			return R.fail(MAYBE, "服务器出错:");
		}
	}

	/**
	 * 人脸核身
	 * 
	 * @param
	 * @return
	 */
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "faceInspect", consumes = "multipart/*", headers = "content-type=multipart/form-data")
	public R faceInspect(String tel, @RequestPart("video") MultipartFile video) {
		try {
			if (video.isEmpty()) {
				return R.fail(ERROR, "参数不能为空");
			}
			logger.info("img-video: {}-{}", video.getContentType(), video.getSize());
			FaceId faceId = this.faceIdService.selectByUsername(tel);
			if (faceId == null) {
				return R.fail(ERROR, "人脸尚未收集");
			}
			if (StringUtils.isNull(faceId.getFaceUrl())) {
				return R.fail(ERROR, "人脸尚未收集");
			}
			JSONArray parseArray = JSONArray.parseArray(faceId.getFaceUrl());
			if (parseArray.size() <= 0) {
				return R.fail(ERROR, "人脸尚未收集");
			}
			boolean isSuccess = this.faceApi.faceInspect(parseArray.getString(0), video.getBytes());
			return R.ok(isSuccess);
		} catch (Exception e) {
			logger.error("人脸核身出错 : ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	/**
	 * 获取动作顺序
	 * 
	 * @return
	 */
	@RequestMapping("getActionSequence")
	public String getActionSequence() {
		// res.FaceInspectResponse.ActionSequence==null ?
		// alert(res.FaceInspectResponse.Error.Message) :
		// alert(res.FaceInspectResponse.ActionSequence)
		String actionSequence = faceApi.getActionSequence();

		// 赋值使用
		if (!StringUtils.isNull(actionSequence)) {
			ObjectMapper mapper = new ObjectMapper();
			ActionSequenceResponse actionSequenceResponse = null;
			try {
				actionSequenceResponse = mapper.readValue(actionSequence.toLowerCase(), ActionSequenceResponse.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (actionSequenceResponse != null) {
				validateData = actionSequenceResponse.getActionSequence();
			}
		}
		return actionSequence;
	}

	public static void main(String[] args) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ActionSequenceResponse actionSequenceResponse = mapper.readValue("{\"ActionSequence\":\"2,1\",\"RequestId\":\"2bd07c95-5367-4d4c-8f8a-7f48eb4dc6e7\"}", ActionSequenceResponse.class);
		System.out.println("actionSequenceResponse = " + actionSequenceResponse);
	}

}
