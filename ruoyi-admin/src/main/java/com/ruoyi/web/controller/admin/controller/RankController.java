package com.ruoyi.web.controller.admin.controller;

import com.alibaba.fastjson2.JSONArray;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.service.MoneyLogService;
import com.ruoyi.system.zny.service.PayOrderService;
import com.ruoyi.system.zny.vo.RankVo;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.ruoyi.common.constant.HttpStatus.ERROR;

@Tag(name = "排行榜相关")
@RestController
@RequestMapping(value = "/rank")
public class RankController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private PayOrderService payOrderService;

	@Autowired
	private MoneyLogService moneyLogService;

	@Operation(summary = "排行榜根据产品")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "productId", value = "产品id(不传就是总购买)", required = false, dataType = "long", dataTypeClass = Long.class), //
			@ApiImplicitParam(name = "top", value = "前N名", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/rankByProductId")
	public R rankByProductId(Long productId, Integer top, Date startTime, Date endTime) {
		try {
			List<RankVo> voList = this.payOrderService.selectTopByProductIdAndTop(productId, top, startTime, endTime);
			Map<Long, BigDecimal> voMap = new TreeMap<>();
			Map<Long, String> realnameMap = new TreeMap<>();
			for (RankVo vo : voList) {
				// 组装vo
				BigDecimal amount = voMap.get(vo.getUserId());
				amount = amount == null ? new BigDecimal(0) : amount;
				voMap.put(vo.getUserId(), amount.add(vo.getPayAmount()));
				// 保存用户
				realnameMap.put(vo.getUserId(), vo.getRealname());
				if (vo.getParentId() != null) {
					BigDecimal parentAmount = voMap.get(vo.getParentId());
					parentAmount = parentAmount == null ? new BigDecimal(0) : parentAmount;
					voMap.put(vo.getParentId(), parentAmount.add(vo.getPayAmount()));
					realnameMap.put(vo.getParentId(), vo.getParentRealname());
				}
			}
			LinkedHashMap<Long, BigDecimal> collect = voMap.entrySet().stream().sorted((e1, e2) -> {
				return e2.getValue().compareTo(e1.getValue());
			}).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (oldV, newV) -> oldV, LinkedHashMap::new));
			Set<Entry<Long, BigDecimal>> entrySet = collect.entrySet();
			voList = new ArrayList<>();
			for (Entry<Long, BigDecimal> entry : entrySet) {
				RankVo vo = new RankVo();
				vo.setPayAmount(entry.getValue());
				vo.setUserId(vo.getUserId());
				vo.setRealname(realnameMap.get(entry.getKey()));
				voList.add(vo);
				if (voList.size() == top.intValue()) {
					break;
				}
			}
			collect.forEach((key, value) -> {
				System.out.println(key + ": " + value);
			});
			return R.ok(voList);
		} catch (Exception e) {
			logger.error("rankByProductId 出错:", e);
			return R.fail(ERROR, "服务器出错: ");
		}
	}

	@Operation(summary = "排行榜佣金")
	@ApiImplicitParams({ //
			@ApiImplicitParam(name = "top", value = "前N名", required = true, dataType = "int", dataTypeClass = Integer.class), //
	})
	@PostMapping(value = "/rankByRate")
	public R rankByRate(Integer top, String moneyTypes, Date startTime, Date endTime) {
		try {
			if (top > 100) {
				return R.fail(ERROR, "服务器限制");
			}
			List<String> moneyTypeList = null;
			if (!StringUtils.isNull(moneyTypes)) {
				moneyTypeList = JSONArray.parseArray(moneyTypes, String.class);
			}else {
				 moneyTypeList = new ArrayList<>();
				 moneyTypeList.add(Dictionary.MoneyTypeEnum.RATE_MONEY.getKey());
				 moneyTypeList.add(Dictionary.MoneyTypeEnum.RATE_MONEY2.getKey());
			}
			List<RankVo> list = this.moneyLogService.totalByType(top, moneyTypeList, startTime, endTime);
			return R.ok(list);
		} catch (Exception e) {
			logger.error("rankByProductId 出错:", e);
			return R.fail(ERROR, "服务器出错: ");
		}
	}
}
