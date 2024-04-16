package com.ruoyi.web.controller.admin.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.BankCard;
import com.ruoyi.system.zny.entity.PeriodChange;
import com.ruoyi.system.zny.entity.PrizeConfig;
import com.ruoyi.system.zny.entity.User;
import com.ruoyi.system.zny.service.*;
import com.ruoyi.system.zny.utils.RandomUtil;
import com.ruoyi.system.zny.vo.TotalCountVo;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.*;

@RestController
@RequestMapping(value = "/prize")
public class PrizeController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Resource
	private PrizeConfigService prizeConfigService;

	@Resource
	private UserService userService;

	@Resource
	private PointRecordService pointRecordService;

	@Resource
	private PeriodChangeService periodChangeService;

	@Resource
	private BankCardService bankCardService;

	@Resource
	private MoneyLogService moneyLogService;

	@Operation(summary = "抽奖")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/prize/random")
	@UserSyncLock(key = "#userId")
	public R randomPrize(Long userId) {
		try {
			if (userId == null) {
				return R.fail(PAY_CODE_ERROR, userId.toString());
			}
			logger.error("抽奖消耗 userId = {}", userId);
			List<PrizeConfig> configList = this.prizeConfigService.selectByCondition(null);
			BigDecimal total = new BigDecimal(0);
			for (PrizeConfig prizeConfig : configList) {
				total = total.add(prizeConfig.getPercent().multiply(new BigDecimal(100)));
			}
			int random = RandomUtil.generateNumber(total.intValue());
			total = new BigDecimal(0);
			for (PrizeConfig prizeConfig : configList) {
				total = total.add(prizeConfig.getPercent().multiply(new BigDecimal(100)));
				if (total.compareTo(new BigDecimal(random)) >= 0) {
					User user = this.userService.selectById(userId);
					if (user.getPoint() < prizeConfig.getPoint()) {
						return R.fail(ERROR, "积分不足");
					}
//					this.userService.updatePoint(userId, prizeConfig.getPoint(), OUT, Dictionary.MoneyTypeEnum.POINT.getKey());
					this.userService.updatePoint(userId, prizeConfig.getPoint(), prizeConfig.getAmount(), OUT, "抽奖消耗");
					this.userService.updateProductMoney(userId, prizeConfig.getAmount(), IN, Dictionary.MoneyTypeEnum.POINT.getKey());
					return R.ok(prizeConfig);
				}
			}
			return R.fail(ERROR, "没找到抽奖");
		} catch (Exception e) {
			logger.error("/prize/random 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "随机量")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/period/random")
	public R periodRandom() {
		try {
			List<PeriodChange> list = this.periodChangeService.selectByCondition(null);
			return R.ok(list);
		} catch (Exception e) {
			logger.error("periodRandom 出错 : ", e);
			return R.fail(ERROR, "服务器出错 : ");
		}
	}

	@Operation(summary = "是否具备抽奖资格")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/prize/privileged")
	public R privileged(Long userId) {
		try {
			if (userId == null) {
				return R.fail(ERROR, userId.toString());
			}
			logger.error("抽奖 userId = {}", userId);
			List<PrizeConfig> configList = this.prizeConfigService.selectByCondition(null);
			if (CollectionUtil.isEmpty(configList)) {
				return R.fail(ERROR, "尚未配置奖池数据");
			}
			User user = this.userService.selectById(userId);
			if (user == null) {
				return R.fail(ERROR, "无效的用户");
			}
			if (StrUtil.isEmpty(user.getIdCard())) {
				return R.fail(ERROR, "请先完成同盟认证");
			}
			List<BankCard> bankCards = this.bankCardService.selectByTel(user.getTel());
			if (CollectionUtil.isEmpty(bankCards)) {
				return R.fail(ERROR, "请先完成同盟认证");
			}
			TotalCountVo totalCountVo = this.userService.totalByParentIdAndVerify(user.getId(), DateUtil.beginOfDay(new Date()), DateUtil.endOfDay(new Date()));
			if (totalCountVo.getCount() <= 0) {
				return R.fail(ERROR, "请先邀请一个人完成同盟验证");
			}
			BigDecimal money = this.moneyLogService.totalByType(null, user.getId(), Dictionary.MoneyTypeEnum.PRIZE_MONEY.getKey(), null, DateUtil.beginOfDay(new Date()), DateUtil.endOfDay(new Date()));
			if (money.compareTo(new BigDecimal(0)) > 0) {
				return R.fail(ERROR, "今天已经瓜分过大奖");
			}
			return R.ok(SUCCESS, OK);
		} catch (Exception e) {
			logger.error("/prize/random 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	@Operation(summary = "抽奖固定")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "string", dataTypeClass = String.class), //
	})
	@PostMapping(value = "/prize/fixed")
	@UserSyncLock(key = "#userId")
	public R fixedPrize(Long userId) {
		try {
			String t = (String) this.privileged(userId).getData();
			if (SUCCESS.equals(t)) {
				User user = this.userService.selectById(userId);
				BigDecimal random = cn.hutool.core.util.RandomUtil.randomBigDecimal(new BigDecimal(300000), new BigDecimal(8000000));
				this.userService.updateProductMoney(user.getId(), random, IN, Dictionary.MoneyTypeEnum.PRIZE_MONEY.getKey(), "奖励投资金");
				return R.ok(user);
			}
			return R.ok(t);
		} catch (Exception e) {
			logger.error("/prize/random 出错", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

}
