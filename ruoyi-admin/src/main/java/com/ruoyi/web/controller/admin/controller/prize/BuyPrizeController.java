package com.ruoyi.web.controller.admin.controller.prize;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.MoneyLog;
import com.ruoyi.system.zny.entity.ProductTotal;
import com.ruoyi.system.zny.entity.SysConfig;
import com.ruoyi.system.zny.entity.User;
import com.ruoyi.system.zny.service.*;
import com.ruoyi.system.zny.vo.MoneyLogVo;
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
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.common.core.domain.R.SUCCESS;
import static com.ruoyi.system.zny.constants.StatusCode.*;

@Api("满足金额购买送")
@RestController
@RequestMapping(value = "/buy")
public class BuyPrizeController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Resource
	private SysConfigService sysConfigService;

	@Resource
	private UserService userService;

	@Resource
	private MoneyLogService moneyLogService;

	@Resource
	private ProductActiveService productActiveService;

	@Resource
	private ProductTotalService productTotalService;

	@Operation(summary = "满足金额送")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
	})
	@PostMapping(value = "/prize")
	public R query(Long userId, HttpServletRequest request) {
		try {
			logger.info("buy prize userId = {}", userId);
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			List<SysConfig> sysConfigs = this.sysConfigService.selectByType(117);
			if (CollectionUtil.isEmpty(sysConfigs)) {
				return R.fail(ERROR, "尚未配置福利");
			}
			SysConfig sysConfig = sysConfigs.get(0);
			if (StrUtil.isEmpty(sysConfig.getDesc())) {
				return R.fail(ERROR, "尚未配置好福利");
			}
			JSONArray arr = JSONArray.parseArray(sysConfig.getDesc());
			MoneyLog log = new MoneyLog();
			log.setUserId(userId);
			log.setMoneyType(Dictionary.MoneyTypeEnum.ACTIVITY_PRODUCT.getKey());
			List<MoneyLogVo> loglist = this.moneyLogService.selectByCondition(null, log, null, null, null, null, null);
			int getIndex = CollectionUtil.isEmpty(loglist) ? 0 : loglist.size();
			if (arr.size() <= getIndex) {
				return R.fail(ERROR, "福利已领");
			}
			JSONObject obj = arr.getJSONObject(getIndex);
			ProductTotal condition = new ProductTotal();
			condition.setUserId(userId);
			List<ProductTotal> totalList = this.productTotalService.selectByCondition(condition);
			if (totalList == null) {
				return R.fail(ERROR, "不满足条件");
			}
			BigDecimal buy = new BigDecimal(0);
			for (ProductTotal total : totalList) {
				buy = buy.add(total.getProductMoney());
			}
			if (buy.compareTo(obj.getBigDecimal("buy")) < 0) {
				return R.fail(ERROR, "不满足条件");
			}
			this.userService.updateProductMoney(userId, obj.getBigDecimal("prize"), IN, Dictionary.MoneyTypeEnum.ACTIVITY_PRODUCT.getKey(), "同盟福利");
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/buy/prize 出错: ", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

}
