package com.ruoyi.web.controller.admin.controller.notify.vo;

import java.math.BigDecimal;

public class JinniuVo {

    private String memberid;
    private String orderid;
    private BigDecimal amount;
    private String transaction_id;
    private String datetime;
    private String returncode;
    private String sign;

    public String getMemberid() {
        return memberid;
    }

    public void setMemberid(String memberid) {
        this.memberid = memberid;
    }

    public String getOrderid() {
        return orderid;
    }

    public void setOrderid(String orderid) {
        this.orderid = orderid;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(String transaction_id) {
        this.transaction_id = transaction_id;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getReturncode() {
        return returncode;
    }

    public void setReturncode(String returncode) {
        this.returncode = returncode;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    @Override
    public String toString() {
        return "JinniuVo{" +
                "memberid='" + memberid + '\'' +
                ", orderid='" + orderid + '\'' +
                ", amount=" + amount +
                ", transaction_id='" + transaction_id + '\'' +
                ", datetime='" + datetime + '\'' +
                ", returncode='" + returncode + '\'' +
                ", sign='" + sign + '\'' +
                '}';
    }
}
