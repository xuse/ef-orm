package com.github.geequery.test.database.boss;

import java.io.Serializable;

/**
 * 
 * <p>
 * 销售金额统计数据
 * </p>
 * 
 * @author jianghaiyang5
 */
public class StatisticSaleIncomeGenDto implements Serializable {
	private static final long serialVersionUID = -6014543226131440299L;

	/**
	 * 分公司ID
	 */
	private String orgId;

	/**
	 * 合作伙伴ID
	 */
	private String partnerId;

	/**
	 * 设备产品销售金额
	 */
	private long deviceIncome;

	/**
	 * 服务产品销售金额
	 */
	private long servIncome;

	public String getOrgId() {
		return orgId;
	}

	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	public String getPartnerId() {
		return partnerId;
	}

	public void setPartnerId(String partnerId) {
		this.partnerId = partnerId;
	}

	public long getDeviceIncome() {
		return deviceIncome;
	}

	public void setDeviceIncome(long deviceIncome) {
		this.deviceIncome = deviceIncome;
	}

	public long getServIncome() {
		return servIncome;
	}

	public void setServIncome(long servIncome) {
		this.servIncome = servIncome;
	}

}
