package com.ruoyi.web.controller.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.service.MoneyLogService;
import com.ruoyi.system.zny.service.UserService;
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

import java.math.BigDecimal;
import java.util.List;

import static com.ruoyi.system.zny.constants.StatusCode.MAYBE;

@Api("银行卡")
@RestController
@RequestMapping(value = "/money")
public class MoneyController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private MoneyLogService moneyLogService;

	@Autowired
	private UserService userService;

	@Operation(summary = "统计moneyLog根据类型")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "type", value = "moneyType, json数组", required = false, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/query")
	public R query(Long userId, String type) {
		try {
			List<String> typeList = null;
			if (StrUtil.isNotEmpty(type)) {
				typeList = JSONArray.parseArray(type, String.class);
			}
			BigDecimal bigDecimal = this.moneyLogService.totalByType(null, userId, null, typeList, null, null);
			return R.ok(bigDecimal);
		} catch (Exception e) {
			logger.error("/bank/query 出错: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

}
