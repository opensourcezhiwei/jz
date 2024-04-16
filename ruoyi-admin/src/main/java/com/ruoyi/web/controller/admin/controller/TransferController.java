package com.ruoyi.web.controller.admin.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.ProductActive;
import com.ruoyi.system.zny.entity.User;
import com.ruoyi.system.zny.service.ProductActiveService;
import com.ruoyi.system.zny.service.UserService;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
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

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.*;

@Api("转账")
@RestController
@RequestMapping(value = "/transfer")
public class TransferController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private UserService userService;
	
	@Autowired
	private ProductActiveService productActiveService;

	@Operation(summary = "充值")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "productActiveId", value = "产品激活的id", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/cyyz")
	@UserSyncLock(key = "#userId")
	public R cyyz(Long userId, Integer productActiveId) {
		try {
			ProductActive active = this.productActiveService.selectById(productActiveId);
			if (active == null) {
				return R.fail(ERROR, "此产品不存在");
			}
			if (active.getUserProductMoney() == null || active.getUserProductMoney().compareTo(new BigDecimal(0)) <= 0) {
				return R.fail(ERROR, "金额不符合");
			}
			User user = this.userService.selectById(active.getUserId());
			if (user == null) {
				return R.fail(ERROR, "用户不存在");
			}
			if (userId.longValue() != user.getId().longValue()) {
				return R.fail(ERROR, "用户不匹配");
			}
			BigDecimal amount = active.getUserProductMoney();
			logger.info("转账 userId = {}, productId = {}, userProductMoney = {}", userId, productActiveId, amount);
			this.userService.updateTransferReleaseProductMoney(active, amount, OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "转账到余额");
			this.userService.updateProductMoney(userId, amount, IN, Dictionary.MoneyTypeEnum.CHANGE.getKey());
//			this.userService.updatePromiseMoney(userId, money, IN, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "已经提现过的静态收益");
//			this.userService.updateMoney(userId, money, OUT, Dictionary.MoneyTypeEnum.CHANGE.getKey(), "重新累计");
			return R.fail(active.getUserProductMoney());
		} catch (Exception e) {
			logger.error("/active/transfer 出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

}
