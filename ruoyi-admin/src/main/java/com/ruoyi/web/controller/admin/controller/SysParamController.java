package com.ruoyi.web.controller.admin.controller;

import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.entity.SysConfig;
import com.ruoyi.system.zny.entity.WebsiteParam;
import com.ruoyi.system.zny.service.SysConfigService;
import com.ruoyi.system.zny.service.SysParamService;
import io.swagger.annotations.Api;
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

import static com.ruoyi.system.zny.constants.StatusCode.MAYBE;

@Api("系统参数")
@RestController
@RequestMapping(value = "/sysparam")
public class SysParamController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private SysParamService sysParamService;

	@Autowired
	private SysConfigService sysConfigService;

	@Operation(summary = "查询网站参数")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/queryWebsite")
	public R queryWebsite() {
		try {
			WebsiteParam obj = this.sysParamService.selectWebsiteParam();
			return R.ok(obj);
		} catch (Exception e) {
			logger.error("/sysparam/queryWebsite 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "编辑网站参数")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/updateWebsite")
	public R updateWebsite(WebsiteParam param) {
		try {
			logger.info("updateWebsite = {}", param.toString());
			param.setId(1L);
			this.sysParamService.save(param);
			return R.ok(param);
		} catch (Exception e) {
			logger.error("/sysparam/updateWebsite 出错: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "后台分页系统配置查询")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "type", value = "类型", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "pageNum", value = "页码", required = true, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "pageSize", value = "页条数", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/queryPageSysConfig")
	public R queryPageSysConfig(Integer type, Integer pageNum, Integer pageSize) {
		try {
			SysConfig config = new SysConfig();
			config.setType(type);
			PageInfo<SysConfig> page = this.sysConfigService.selectPageByCondition(config, pageNum, pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("/sysparam/querySysConfig 出错: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "系统配置查询")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "记录id", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "type", value = "类型", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/querySysConfig")
	public R querySysConfig(Integer id, Integer type) {
		try {
			SysConfig config = new SysConfig();
			config.setId(id);
			config.setType(type);
			List<SysConfig> list = this.sysConfigService.selectByCondition(config);
			return R.ok(list);
		} catch (Exception e) {
			logger.error("/sysparam/querySysConfig 出错: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "编辑系统配置")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "记录id, 不传新增", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "name", value = "记录名称", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "desc", value = "记录描述", required = true, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "status", value = "状态0:禁用, 1:启用", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/updateSysConfig")
	public R updateSysConfig(SysConfig param) {
		try {
			this.sysConfigService.save(param);
			return R.ok(param);
		} catch (Exception e) {
			logger.error("/sysparam/updateSysConfig 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "删除系统配置")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "记录id", required = false, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/delSysConfig")
	public Object delSysConfig(Integer id) {
		try {
			this.sysConfigService.delById(id);
			return R.ok(true);
		} catch (Exception e) {
			logger.error("/sysparam/updateSysConfig 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

}
