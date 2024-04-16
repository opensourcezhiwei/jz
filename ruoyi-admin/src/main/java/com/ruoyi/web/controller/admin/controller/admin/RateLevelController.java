package com.ruoyi.web.controller.admin.controller.admin;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.entity.RateLevel;
import com.ruoyi.system.zny.service.RateLevelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static com.ruoyi.common.core.domain.R.SUCCESS;
import static com.ruoyi.system.zny.constants.StatusCode.MAYBE;
import static com.ruoyi.system.zny.constants.StatusCode.OK;

@Api("返佣层级")
@RestController
@RequestMapping(value = "/rate")
public class RateLevelController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Resource
	private RateLevelService rateLevelService;

	@Operation(summary = "返佣记录查询")
	@PostMapping(value = "/query")
	public Object query() {
		try {
			List<RateLevel> list = this.rateLevelService.selectByCondition(null, null);
			return R.ok(list);
		} catch (Exception e) {
			logger.error("/rate/query 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "返佣记录修改")
	@PostMapping(value = "/edit")
	public R edit(RateLevel level) {
		try {
			this.rateLevelService.edit(level);
			return R.ok(level);
		} catch (Exception e) {
			logger.error("/rate/edit 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "返佣记录删除")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "id", value = "返佣记录的id", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/delete")
	public R delete(Integer id) {
		try {
			this.rateLevelService.deleteById(id);
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/rate/delete 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

}
