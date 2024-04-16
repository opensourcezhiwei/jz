package com.ruoyi.web.controller.admin.controller;

import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.service.PayOrderService;
import com.ruoyi.system.zny.service.WithdrawService;
import com.ruoyi.system.zny.vo.ChildCount;
import com.ruoyi.system.zny.vo.TotalCountVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ruoyi.system.zny.constants.StatusCode.*;

/**
 * 监控
 * 
 * @author jks
 *
 */
@Api(value = "监控相关")
@RestController
@RequestMapping(value = "/monitor")
public class MonitorController extends BaseController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private PayOrderService payOrderService;

	@Autowired
	private WithdrawService withdrawService;

	/**
	 * 近几分钟的支付情况
	 */
	@Operation(summary = "支付条数监控")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/payTotal")
	public R payTotal(Long userId, String tel, Integer lastMinutes, Integer pageNum, Integer pageSize) {
		try {
			PageInfo<TotalCountVo> result = this.payOrderService.payTotalByMinutes(userId, tel, lastMinutes, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(result);
		} catch (Exception e) {
			logger.error("支付payTotal出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

	/**
	 * 近几分钟的提现情况
	 */
	@Operation(summary = "提现条数监控")
	@ApiImplicitParams({ //
	})
	@PostMapping(value = "/withdrawTotal")
	public R withdrawTotal(Long userId, String tel, Integer lastMinutes, Integer pageNum, Integer pageSize) {
		try {
			PageInfo<ChildCount> result = this.withdrawService.withdrawTotalByMinutes(userId, tel, lastMinutes, //
					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
			return R.ok(result);
		} catch (Exception e) {
			logger.error("支付payTotal出错:", e);
			return R.fail(MAYBE, "服务器出错");
		}
	}

}
