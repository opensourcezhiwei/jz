package com.ruoyi.web.controller.admin.vo;

import com.ruoyi.system.zny.vo.TotalCountVo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

public class TotalVo implements Serializable {

	private static final long serialVersionUID = -4954041476489349631L;

	private TotalCountVo all; // 用户数 用户总充值
	private TotalCountVo today; // 今天充值用户数 和 金额
	private TotalCountVo yesterday; // 昨天充值用户数 和 金额
	private TotalCountVo month; // 当月充值用户数和金额

	private TotalCountVo todayRegister; // 当天注册用户数和 注册用户充值金额
	private TotalCountVo yesterdayRegister;// 昨天注册用户数和 注册用户充值金额
	private TotalCountVo monthRegister; // 当月充值用户数和 注册用户充值金额

	private BigDecimal productMoney; // 用户未提金额
	private TotalCountVo payingWithdraw; // 待审核提现条数和金额
	private TotalCountVo successWithdraw;// 提现条数和金额
	private TotalCountVo todayWithdraw; // 今天提现条数和金额
	private TotalCountVo yesterdayWithdraw; // 昨天提现条数和金额
	private BigDecimal buyMoney; // 用户购买金额

	// 总充值
	private Map<Integer, BigDecimal> todayCharge;
	private Map<Integer, BigDecimal> yesterdayCharge;
	private Map<Integer, BigDecimal> monthCharge;

	public TotalVo(TotalCountVo all, TotalCountVo today, TotalCountVo yesterday, TotalCountVo month, TotalCountVo todayRegister, TotalCountVo yesterdayRegister, TotalCountVo monthRegister, BigDecimal productMoney, TotalCountVo payingWithdraw, TotalCountVo successWithdraw, TotalCountVo todayWithdraw, TotalCountVo yesterdayWithdraw, BigDecimal buyMoney, Map<Integer, BigDecimal> todayCharge, Map<Integer, BigDecimal> yesterdayCharge, Map<Integer, BigDecimal> monthCharge) {
		super();
		this.all = all;
		this.today = today;
		this.yesterday = yesterday;
		this.month = month;
		this.todayRegister = todayRegister;
		this.yesterdayRegister = yesterdayRegister;
		this.monthRegister = monthRegister;
		this.productMoney = productMoney;
		this.payingWithdraw = payingWithdraw;
		this.successWithdraw = successWithdraw;
		this.todayWithdraw = todayWithdraw;
		this.yesterdayWithdraw = yesterdayWithdraw;
		this.buyMoney = buyMoney;
		this.todayCharge = todayCharge;
		this.yesterdayCharge = yesterdayCharge;
		this.monthCharge = monthCharge;
	}

	public TotalVo() {
		super();
	}

	public TotalCountVo getAll() {
		return all;
	}

	public void setAll(TotalCountVo all) {
		this.all = all;
	}

	public TotalCountVo getToday() {
		return today;
	}

	public void setToday(TotalCountVo today) {
		this.today = today;
	}

	public TotalCountVo getYesterday() {
		return yesterday;
	}

	public void setYesterday(TotalCountVo yesterday) {
		this.yesterday = yesterday;
	}

	public TotalCountVo getMonth() {
		return month;
	}

	public void setMonth(TotalCountVo month) {
		this.month = month;
	}

	public BigDecimal getProductMoney() {
		return productMoney;
	}

	public void setProductMoney(BigDecimal productMoney) {
		this.productMoney = productMoney;
	}

	public TotalCountVo getPayingWithdraw() {
		return payingWithdraw;
	}

	public void setPayingWithdraw(TotalCountVo payingWithdraw) {
		this.payingWithdraw = payingWithdraw;
	}

	public TotalCountVo getSuccessWithdraw() {
		return successWithdraw;
	}

	public void setSuccessWithdraw(TotalCountVo successWithdraw) {
		this.successWithdraw = successWithdraw;
	}

	public TotalCountVo getTodayWithdraw() {
		return todayWithdraw;
	}

	public void setTodayWithdraw(TotalCountVo todayWithdraw) {
		this.todayWithdraw = todayWithdraw;
	}

	public TotalCountVo getYesterdayWithdraw() {
		return yesterdayWithdraw;
	}

	public void setYesterdayWithdraw(TotalCountVo yesterdayWithdraw) {
		this.yesterdayWithdraw = yesterdayWithdraw;
	}

	public BigDecimal getBuyMoney() {
		return buyMoney;
	}

	public void setBuyMoney(BigDecimal buyMoney) {
		this.buyMoney = buyMoney;
	}

	public TotalCountVo getTodayRegister() {
		return todayRegister;
	}

	public void setTodayRegister(TotalCountVo todayRegister) {
		this.todayRegister = todayRegister;
	}

	public TotalCountVo getYesterdayRegister() {
		return yesterdayRegister;
	}

	public void setYesterdayRegister(TotalCountVo yesterdayRegister) {
		this.yesterdayRegister = yesterdayRegister;
	}

	public TotalCountVo getMonthRegister() {
		return monthRegister;
	}

	public void setMonthRegister(TotalCountVo monthRegister) {
		this.monthRegister = monthRegister;
	}

	public Map<Integer, BigDecimal> getTodayCharge() {
		return todayCharge;
	}

	public void setTodayCharge(Map<Integer, BigDecimal> todayCharge) {
		this.todayCharge = todayCharge;
	}

	public Map<Integer, BigDecimal> getYesterdayCharge() {
		return yesterdayCharge;
	}

	public void setYesterdayCharge(Map<Integer, BigDecimal> yesterdayCharge) {
		this.yesterdayCharge = yesterdayCharge;
	}

	public Map<Integer, BigDecimal> getMonthCharge() {
		return monthCharge;
	}

	public void setMonthCharge(Map<Integer, BigDecimal> monthCharge) {
		this.monthCharge = monthCharge;
	}

}
