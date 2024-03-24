package com.ruoyi.web.controller.system;

import java.util.Date;
import java.util.List;

import com.ruoyi.common.core.domain.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.service.ISysNoticeService;

/**
 * 公告 信息操作处理
 * 
 * @author ruoyi
 */
@Api("公告/信息管理")
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController extends BaseController
{
    @Autowired
    private ISysNoticeService noticeService;

    /**
     * 获取通知公告列表
     */
    @ApiOperation("获取通知公告列表")
    @GetMapping("/list")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "noticeId", value = "公告/通知id", dataType = "Long", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "noticeTitle", value = "标题", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "noticeType", value = "公告类型", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "noticeContent", value = "公告内容", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "status", value = "0正常 1关闭", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startDate", value = "通知或公告的生效日期", dataType = "Date", dataTypeClass = Date.class),
            @ApiImplicitParam(name = "endDate", value = "通知或公告的失效日期", dataType = "Date", dataTypeClass = Date.class),
            @ApiImplicitParam(name = "importance", value = "通知或公告的重要程度", dataType = "Long", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "isPublic;", value = "是否对公众可见", dataType = "Long", dataTypeClass = Long.class)

    })
    public R list(@RequestBody SysNotice notice)
    {
        startPage();
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        return R.ok(list);
    }

    /**
     * 根据通知公告编号获取详细信息
     */
    @ApiOperation("根据通知公告编号获取详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "noticeId", value = "公告/通知id", dataType = "Long", dataTypeClass = Long.class),
    })
    @GetMapping(value = "/{noticeId}")
    public R getInfo(@PathVariable Long noticeId)
    {
        return R.ok(noticeService.selectNoticeById(noticeId));
    }

    /**
     * 新增通知公告
     */
    @ApiOperation("新增通知公告")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "noticeTitle", value = "标题", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "noticeType", value = "公告类型", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "noticeContent", value = "公告内容", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startDate", value = "通知或公告的生效日期", dataType = "Date", dataTypeClass = Date.class),
            @ApiImplicitParam(name = "endDate", value = "通知或公告的失效日期", dataType = "Date", dataTypeClass = Date.class),
            @ApiImplicitParam(name = "importance", value = "通知或公告的重要程度", dataType = "Long", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "isPublic;", value = "是否对公众可见", dataType = "Long", dataTypeClass = Long.class)
    })
    @Log(title = "新增通知公告", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public R add(@Validated @RequestBody SysNotice notice)
    {
        notice.setCreateBy(getUsername());
        return R.ok(noticeService.insertNotice(notice));
    }

    /**
     * 修改通知公告
     */
    @ApiOperation("修改通知公告")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "noticeTitle", value = "标题", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "noticeType", value = "公告类型", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "noticeContent", value = "公告内容", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startDate", value = "通知或公告的生效日期", dataType = "Date", dataTypeClass = Date.class),
            @ApiImplicitParam(name = "endDate", value = "通知或公告的失效日期", dataType = "Date", dataTypeClass = Date.class),
            @ApiImplicitParam(name = "Long", value = "通知或公告的重要程度", dataType = "Long", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "isPublic;", value = "是否对公众可见", dataType = "Long", dataTypeClass = Long.class)
    })
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping("/edit")
    public R edit(@Validated @RequestBody SysNotice notice)
    {
        notice.setUpdateBy(getUsername());
        return R.ok(noticeService.updateNotice(notice));
    }

    /**
     * 删除通知公告
     */
    @ApiOperation("删除通知公告")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "noticeIds", value = "通知/公告id集合", dataType = "List", dataTypeClass = List.class),
    })
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public R remove(@PathVariable Long[] noticeIds)
    {
        return R.ok(noticeService.deleteNoticeByIds(noticeIds));
    }
}
