package com.ruoyi.system.enums;

/**
 * 登录类型枚举
 */
public enum LoginTypeEnum {
    PASSWORD(1, "密码登录"),
    QQ(2, "qq登录"),
    WECHAT(3, "微信登录");

    private Integer key;
    private String value;

    LoginTypeEnum(Integer key, String value) {
        this.key = key;
        this.value = value;
    }

    public Integer getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
