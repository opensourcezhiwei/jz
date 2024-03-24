package com.ruoyi.common.core.domain.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户登录对象
 * 
 * @author ruoyi
 */
@ApiModel(value = "LoginBodyEntity", description = "注册实体")
@Data
public class LoginBody
{
    /**
     * 用户名
     */
    @ApiModelProperty("用户名")
    private String username;

    /**
     * 用户密码
     */
    @ApiModelProperty("用户密码")
    private String password;

    /**
     * 验证码
     */
    @ApiModelProperty("验证码")
    private String code;

    /**
     * 银行卡号
     */
    @ApiModelProperty("银行卡号")
    private String cardNumbers;

    /**
     * 余额
     */
    @ApiModelProperty("余额")
    private BigDecimal balance;

    /**
     * 出生日期
     */

    @ApiModelProperty("出生日期")
    private Date dateOfBirth;

    /**
     * 住址
     */
    @ApiModelProperty("住址")
    private String address;


    /**
     * 唯一标识
     */
    private String uuid;

    /**
     * 邀请码
     */
    @ApiModelProperty("邀请码")
    private String invitationCode;

    /**
     * 提现密码
     */
    @ApiModelProperty("提现密码")
    private String withdrawalPassword;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }
}
