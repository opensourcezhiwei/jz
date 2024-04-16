package com.ruoyi.web.controller.admin.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.zny.entity.SysConfig;
import com.ruoyi.system.zny.entity.WebsiteParam;
import com.ruoyi.system.zny.service.PayOrderService;
import com.ruoyi.system.zny.service.SysConfigService;
import com.ruoyi.system.zny.service.SysParamService;
import com.ruoyi.system.zny.service.WithdrawService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ruoyi.system.zny.constants.Dictionary.PayStatusEnum.MAYBE;

/**
 * 监控
 *
 * @author jks
 *
 */
@Api(value = "公告相关")
@RestController
@RequestMapping(value = "/new-notice")
public class NewNoticeController extends BaseController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private WithdrawService withdrawService;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private SysParamService sysParamService;

    /**
     * 所有公告
     */
    @Operation(summary = "首页新闻公告列表")
    @ApiImplicitParams({ //
    })
    @PostMapping(value = "/query")
    public R query() {
        try {
            List<SysConfig> configs1 = this.sysConfigService.selectByType(115); // 首页滚屏公告
            List<SysConfig> configs2 = this.sysConfigService.selectByType(116); // 其他页面滚屏公告
            List<SysConfig> configs3 = this.sysConfigService.selectByType(108); // 弹窗公告
            // 网站配置(滚动文字， 客服链接1， 客服链接2， 客服名称， 客服二维码1， 客服名称， app下载链接， logo图， 启动图)
            WebsiteParam websiteParam = this.sysParamService.selectWebsiteParam();
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("115", configs1);
            resultMap.put("116", configs2);
            resultMap.put("108", configs3);
            resultMap.put("websiteParam", websiteParam);
            return R.ok(resultMap);
        } catch (Exception e) {
            logger.error("/new-notice/query出错:", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }
}
