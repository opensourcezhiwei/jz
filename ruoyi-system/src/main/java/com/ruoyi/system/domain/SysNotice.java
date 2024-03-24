package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 通知公告对象 sys_notice
 *
 * @author ruoyi
 * @date 2024-03-23
 */
public class SysNotice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 公告ID */
    private Integer noticeId;

    /** 公告标题 */
    @Excel(name = "公告标题")
    private String noticeTitle;

    /** 公告类型（1通知 2公告） */
    @Excel(name = "公告类型", readConverterExp = "1=通知,2=公告")
    private String noticeType;

    /** 公告内容 */
    @Excel(name = "公告内容")
    private String noticeContent;

    /** 公告状态（0正常 1关闭） */
    @Excel(name = "公告状态", readConverterExp = "0=正常,1=关闭")
    private String status;

    /** 通知或公告的生效日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "通知或公告的生效日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 通知或公告的失效日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "通知或公告的失效日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 通知或公告的重要程度 */
    @Excel(name = "通知或公告的重要程度")
    private Long importance;

    /** 是否对公众可见 */
    @Excel(name = "是否对公众可见")
    private Long isPublic;

    public void setNoticeId(Integer noticeId)
    {
        this.noticeId = noticeId;
    }

    public Integer getNoticeId()
    {
        return noticeId;
    }
    public void setNoticeTitle(String noticeTitle)
    {
        this.noticeTitle = noticeTitle;
    }

    public String getNoticeTitle()
    {
        return noticeTitle;
    }
    public void setNoticeType(String noticeType)
    {
        this.noticeType = noticeType;
    }

    public String getNoticeType()
    {
        return noticeType;
    }
    public void setNoticeContent(String noticeContent)
    {
        this.noticeContent = noticeContent;
    }

    public String getNoticeContent()
    {
        return noticeContent;
    }
    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }
    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getStartDate()
    {
        return startDate;
    }
    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }
    public void setImportance(Long importance)
    {
        this.importance = importance;
    }

    public Long getImportance()
    {
        return importance;
    }
    public void setIsPublic(Long isPublic)
    {
        this.isPublic = isPublic;
    }

    public Long getIsPublic()
    {
        return isPublic;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("noticeId", getNoticeId())
                .append("noticeTitle", getNoticeTitle())
                .append("noticeType", getNoticeType())
                .append("noticeContent", getNoticeContent())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .append("startDate", getStartDate())
                .append("endDate", getEndDate())
                .append("importance", getImportance())
                .append("isPublic", getIsPublic())
                .toString();
    }
}
