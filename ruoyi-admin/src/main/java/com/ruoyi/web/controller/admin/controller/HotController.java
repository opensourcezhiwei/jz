package com.ruoyi.web.controller.admin.controller;

import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.entity.Hot;
import com.ruoyi.system.zny.service.HotService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ruoyi.common.core.domain.R.SUCCESS;
import static com.ruoyi.system.zny.constants.StatusCode.MAYBE;
import static com.ruoyi.system.zny.constants.StatusCode.OK;

/**
 * 热门精选
 */
@RestController
@RequestMapping(value = "/hot")
public class HotController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private HotService hotService;


	@Operation(summary = "保存热门精选")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "有id修改，无id新增", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "name", value = "名称", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "intro", value = "简介", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "imgs", value = "图片", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "type", value = "类型自定义", required = true, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "count", value = "计数器或库存等概念", required = false, dataType = "int", dataTypeClass = Integer.class), //
			@ApiImplicitParam(name = "remark", value = "备注", required = false, dataType = "string", dataTypeClass = String.class), //
			@ApiImplicitParam(name = "memo", value = "备注2", required = false, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/save")
	public R save(Hot hot) {
		try {
			this.hotService.save(hot);
			return R.ok(hot);
		} catch (Exception e) {
			logger.error("查询优惠券", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "查询热门")
	@PostMapping(value = "/selectByCondition")
	public R selectByCondition(String type, Integer pageSize, Integer pageNum) {
		try {
			Hot hot = new Hot();
			hot.setType(type);
			PageInfo<Hot> page = this.hotService.selectPageByCondition(hot, pageNum, pageSize);
			return R.ok(page);
		} catch (Exception e) {
			logger.error("查询优惠券", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "删除热门")
	@DeleteMapping(value = "/deleteById")
	public R deleteByIds(Integer id) {
		try {
			this.hotService.deleteById(id);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("查询优惠券", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

}
