package com.ruoyi.web.controller.admin.controller;


import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSSClient;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.entity.UploadFiles;
import com.ruoyi.system.zny.service.UploadFileService;
import com.ruoyi.system.zny.utils.DateUtil;
import com.ruoyi.web.controller.admin.config.MultipartFileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Date;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.*;

@Api("上传")
@RestController
@RequestMapping(value = "/file")
public class UploadController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private OSSClient ossClient;

	@Autowired
	private UploadFileService uploadFileService;

	@Value("${nginx.web.url:http://127.0.0.1}")
	private String fileUrl;

	@Value("${nginx.web.addr:/usr/share/nginx/html}")
	private String fileAddr;

	@Value("${aliyun.oss.bucketName}")
	private String bucketName;

	/**
	 * 采用spring提供的上传文件的方法
	 */
	@Operation(summary = "上传文件")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/upload", consumes = "multipart/*", headers = "content-type=multipart/form-data")
	public R upload(@RequestParam("username") String username, //
			@RequestParam("app") Byte app, //
			@RequestPart("file") MultipartFile file) {
		try {
			if (StringUtils.isNull(username)) {
				return R.fail(ERROR, "username不能为空");
			}
			logger.info("upload app = {}, username = {}", app, username);
			UploadFiles upload = new UploadFiles();
			if (file.isEmpty()) {
				return R.fail(ERROR, "file is empty!");
			}
			if (!file.getOriginalFilename().contains(".")) {
				return R.fail(ERROR, "file format error, must contains . suffix ");
			}
			if (!file.getContentType().contains("image")) {
				return R.fail(ERROR, "文件类型错误");
			}
			String subffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
			String fileName = username + System.currentTimeMillis() + subffix;
			File filePath = new File(fileAddr + File.separator + username, fileName);
			if (!filePath.getParentFile().exists()) {
				filePath.getParentFile().mkdirs();
			}
			file.transferTo(new File(fileAddr + File.separator + username + File.separator + fileName));
			logger.info("username = {}, type = {}, filePath = {}, 已更新", username, username, fileUrl + File.separator + username + File.separator + fileName);
			upload.setApp(app);
			upload.setCreateTime(new Date());
			upload.setExtension(subffix);
			upload.setFileName(fileName);
			upload.setTel(username);
			upload.setUrl(fileUrl + File.separator + username + File.separator + fileName);
			this.uploadFileService.save(upload);
			return R.ok(upload);
		} catch (Exception e) {
			logger.error("上传出错 : ", e);
			return R.fail(MAYBE, NETWORK_IS_ERROR);
		}
	}

	/**
	 * 采用spring提供的上传文件的方法
	 */
	@Operation(summary = "上传文件")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/ossUploadByBase64")
	public R ossUpload(@RequestParam("username") String username, //
			@RequestParam("app") Byte app, //
			@RequestParam("base64") String base64) {
		MultipartFile file = MultipartFileUtil.base64ToMultipart(base64);
		return R.ok(this.ossUpload(username, app, file));
	}

	/**
	 * 采用spring提供的上传文件的方法
	 */
	@Operation(summary = "上传文件")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/ossUpload", consumes = "multipart/*", headers = "content-type=multipart/form-data")
	public R ossUpload(@RequestParam("username") String username, //
			@RequestParam("app") Byte app, //
			@RequestPart("file") MultipartFile file) {
		try {
			if (StringUtils.isNull(username)) {
				return R.fail(ERROR, "username不能为空");
			}
			logger.info("upload app = {}, username = {}", app, username);
			UploadFiles upload = new UploadFiles();
			if (file.isEmpty()) {
				return R.fail(ERROR, "file is empty!");
			}
			if (!file.getOriginalFilename().contains(".")) {
				return R.fail(ERROR, "file format error, must contains . suffix ");
			}
			if (!file.getContentType().contains("image")) {
				return R.fail(ERROR, "文件类型错误");
			}
			String subffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
			String fileName = username + System.currentTimeMillis() + subffix;
//			File filePath = new File(fileAddr + File.separator + username, fileName);
//			if (!filePath.getParentFile().exists()) {
//				filePath.getParentFile().mkdirs();
//			}
//			file.transferTo(new File(fileAddr + File.separator + username + File.separator + fileName));
			String fileKey = "zny/" + username + "/" + fileName;
//			final String downloadFile = System.currentTimeMillis() + ".png";

			this.ossClient.putObject(bucketName, fileKey, file.getInputStream());

//			this.ossClient.getObject(bucketName, fileKey).getObjectContent();
//			StreamUtils.copy(inputStream, new FileOutputStream(downloadFile));
			String filePath = this.ossClient.generatePresignedUrl(bucketName, fileKey, DateUtil.dateDiffer(new Date(), 500), HttpMethod.GET).toString();
			filePath = filePath.replace("http:", "https:");
//			this.ossClient.getObject(bucketName, fileKey);
			logger.info("username = {}, type = {}, filePath = {}, size = {}, 已更新", username, username, filePath, file.getSize());
			upload.setApp(app);
			upload.setCreateTime(new Date());
			upload.setExtension(subffix);
			upload.setFileName(fileName);
			upload.setTel(username);
			upload.setUrl(filePath);
			this.uploadFileService.save(upload);
			return R.ok(upload);
		} catch (Exception e) {
			logger.error("上传出错 : ", e);
			return R.fail(MAYBE, NETWORK_IS_ERROR);
		}
	}

	/**
	 * 采用spring提供的上传文件的方法
	 */
//	@Operation(summary = "异步上传文件")
//	@ApiImplicitParams({ //
//	})
//	@PostMapping(value = "/asyncUpload")
//	public Map<String, Object> asyncUpload(@RequestParam("username") String username, //
//			@RequestParam("app") Byte app, //
//			@RequestParam("file") MultipartFile file) {
//		try {
//			logger.info("upload app = {}, username = {}", app, username);
//			UploadFiles upload = new UploadFiles();
//			if (file.isEmpty()) {
//				return result(ERROR, "file is empty!");
//			}
//			if (!file.getOriginalFilename().contains(".")) {
//				return result(ERROR, "file format error, must contains . suffix ");
//			}
//			if (!file.getContentType().contains("image")) {
//				return result(ERROR, "文件类型错误");
//			}
//			String subffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
//			String fileName = username + System.currentTimeMillis() + subffix;
//			File filePath = new File(fileAddr + File.separator + username, fileName);
//			if (!filePath.getParentFile().exists()) {
//				filePath.getParentFile().mkdirs();
//			}
//			file.transferTo(new File(fileAddr + File.separator + username + File.separator + fileName));
//			logger.info("username = {}, type = {}, filePath = {}, 已更新", username, username, fileUrl + File.separator + username + File.separator + fileName);
//			upload.setApp(app);
//			upload.setCreateTime(new Date());
//			upload.setExtension(subffix);
//			upload.setFileName(fileName);
//			upload.setTel(username);
//			upload.setUrl(fileUrl + File.separator + username + File.separator + fileName);
//			this.uploadFileService.save(upload);
//			return result(SUCCESS, OK, upload);
//		} catch (Exception e) {
//			logger.error("上传出错 : ", e);
//			return result(MAYBE, NETWORK_IS_ERROR);
//		}
//	}

	/**
	 * 查询上传列表
	 * 
	 * @return
	 */
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "tel", value = "手机号码/后台账号", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "pageNum", value = "页码", required = false, dataType = "integer", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "pageSize", value = "每页显示的条数", required = false, dataType = "integer", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/queryFileList")
	public R queryFileList(UploadFiles file, Integer pageNum, Integer pageSize, HttpServletRequest request) {
		try {
			PageInfo<UploadFiles> page = this.uploadFileService.selectByCondition(file, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("queryUploadList 出错 : ", e);
			return R.fail(MAYBE, NETWORK_IS_ERROR);
		}
	}

	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "xx", required = true, dataType = "integer", dataTypeClass = Integer.class) })
	@PostMapping(value = "/delFileById")
	public R delFileById(Integer id) {
		try {
			UploadFiles file = this.uploadFileService.selectById(id);
			if (file == null) {
				return R.fail(ERROR, "文件不存在");
			}
			File filePath = new File(fileAddr + File.separator + file.getTel(), file.getFileName());
			if (filePath.exists()) {
				filePath.delete();
			}
			this.uploadFileService.delById(id);
			return R.ok(true);
		} catch (Exception e) {
			logger.error("queryUploadList 出错 : ", e);
			return R.fail(MAYBE, NETWORK_IS_ERROR);
		}
	}
}
