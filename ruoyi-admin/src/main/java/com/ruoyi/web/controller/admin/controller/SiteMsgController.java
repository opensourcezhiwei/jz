package com.ruoyi.web.controller.admin.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.SiteMsg;
import com.ruoyi.system.zny.service.SiteMsgService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.MAYBE;

@RestController
@RequestMapping(value = "/siteMsg")
public class SiteMsgController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private SiteMsgService siteMsgService;

	@Operation(summary = "查询站内信列表")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/queryByUserId")
	public R queryByUserId(Long userId) {
		try {
			if (userId == null) {
				return R.fail(ERROR, "用户id为空");
			}
			SiteMsg msg = new SiteMsg();
			msg.setUserId(userId);
			List<SiteMsg> list = this.siteMsgService.selectByCondition(msg);
			return R.ok(list);
		} catch (Exception e) {
			logger.error("查询个人资料", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询站内信列表")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/queryById")
	public R queryById(Long id) {
		try {
			if (id == null) {
				return R.fail(ERROR, "id为空");
			}
			SiteMsg msg = this.siteMsgService.selectById(id);
			if (msg == null) {
				return R.ok(true);
			}
			msg.setStatus(Dictionary.STATUS.ENABLE);
			this.siteMsgService.save(msg);
			return R.ok(msg);
		} catch (Exception e) {
			logger.error("查询个人资料", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

}
