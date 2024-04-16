package com.ruoyi.web.controller.admin.controller;

import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.ChargeOrder;
import com.ruoyi.system.zny.entity.UsdtCharge;
import com.ruoyi.system.zny.service.ChargeOrderService;
import com.ruoyi.system.zny.service.UsdtChargeService;
import com.ruoyi.system.zny.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.*;

@Api(value = "usdt相关")
@RestController
@RequestMapping(value = "/usdt")
public class UsdtController extends BaseController {

    @Autowired
    private UsdtChargeService usdtChargeService;

    @Autowired
    private ChargeOrderService chargeOrderService;

    @Autowired
    private UserService userService;

    @Operation(summary = "保存usdt渠道")
    @ApiImplicitParams({ //
    })
    @PostMapping(value = "/save")
    public R save(UsdtCharge usdt) {
        try {
            this.usdtChargeService.save(usdt);
            return R.ok(usdt);
        } catch (Exception e) {
            logger.error("/save 出错: ", e);
            return R.fail(MAYBE, "服务器异常");
        }
    }

    @Operation(summary = "查詢usdt渠道")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "status", value = "1啓用 0禁用", required = false, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/query")
    public R query(UsdtCharge usdt) {
        try {
            List<UsdtCharge> list = this.usdtChargeService.selectByCondition(usdt);
            return R.ok(list);
        } catch (Exception e) {
            logger.error("/save 出错: ", e);
            return R.fail(MAYBE, "服务器异常");
        }
    }

    @Operation(summary = "充值")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "pay", value = "充值", required = false, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/charge/save")
    public R charge(ChargeOrder order, Integer usdtChargeId) {
        try {
            UsdtCharge config = this.usdtChargeService.selectById(usdtChargeId);
            if (config == null) {
                return R.fail(ERROR, "充值的u配置不存在");
            }
            order.setRate(config.getRate());
            order.setStatus(Dictionary.PayStatusEnum.PAYING.getKey());
            order.setUvalue(order.getAmount().multiply(order.getRate()));
            this.chargeOrderService.save(order);
            return R.ok(order);
        } catch (Exception e) {
            logger.error("/save 出错: ", e);
            return R.fail(MAYBE, "服务器异常");
        }
    }

    @Operation(summary = "查询充值")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "pay", value = "充值", required = false, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/charge/query")
    public R queryCharge(ChargeOrder order, Integer pageNum, Integer pageSize) {
        try {
            PageInfo<ChargeOrder> page = this.chargeOrderService.selectPageByCondition(order, //
                    pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
                    pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
            return R.ok(page);
        } catch (Exception e) {
            logger.error("/save 出错: ", e);
            return R.fail(MAYBE, "服务器异常");
        }
    }

    @Operation(summary = "审核充值")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "status", value = "2通過  3失敗", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/charge/audit")
    public R auditCharge(Long chargeOrderId, Byte status) {
        try {
            ChargeOrder order = this.chargeOrderService.selectById(chargeOrderId);
            if (order == null) {
                return R.fail(ERROR, "订单不存在");
            }
            order.setStatus(status);
            if (status.byteValue() == 2) {
                this.userService.updateProductMoney(order.getUserId(), order.getAmount(), IN, Dictionary.MoneyTypeEnum.CHARGE.getKey());
                this.userService.addCharge(order.getUserId(), Dictionary.PayType.MONEY.getKey(), order.getAmount());
            }
            this.chargeOrderService.save(order);
            return R.ok(order);
        } catch (Exception e) {
            logger.error("/save 出错: ", e);
            return R.fail(MAYBE, "服务器异常");
        }
    }

}
