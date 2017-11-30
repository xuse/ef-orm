package com.github.geequery.test.database.boss;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import jef.database.DataObject;

/**
 * 订单实体
 * 
 * @author jianghaiyang5
 */
@Entity
@Table(name = "CR_SALE_ORDER")
public class SaleOrder extends DataObject {
	private static final long serialVersionUID = -2364338786728778428L;

	/**
	 * 主键
	 */
	@Id
	@Column(name = "ID", length = 32, updatable = false, nullable = false)
	private String id;

	/**
	 * 订单号
	 */
	@Column(name = "CODE", length = 32, updatable = false, nullable = false)
	private String code;

	/**
	 * 客户ID
	 */
	@Column(name = "CUSTOMER_ID", length = 32, nullable = false)
	private String customerId;

	/**
	 * 合作伙伴ID
	 */
	@Column(name = "PARTNER_ID", length = 32, nullable = false)
	private String partnerId;

	/**
	 * 分公司ID
	 */
	@Column(name = "ORG_ID", length = 32, nullable = false)
	private String orgId;

	/**
	 * 订单状态：1已保存，2已完成，3未开通
	 */
	@Column(name = "STATUS")
	private Integer status;

	/**
	 * 设备金额折扣，比如75折的话就存储75，上浮10%存储110
	 */
	@Column(name = "DEV_DISCOUNT")
	private Integer devDiscount;

	/**
	 * 服务金额折扣，比如75折的话就存储75，上浮10%存储110
	 */
	@Column(name = "serv_DISCOUNT")
	private Integer servDiscount;

	/**
	 * 订单总金额（单位：分）
	 */
	@Column(name = "SUM_INCOME")
	private Long sumIncome;

	/**
	 * 设备总金额（单位：分）
	 */
	@Column(name = "DEV_INCOME")
	private Long devIncome;

	/**
	 * 服务总金额（单位：分）
	 */
	@Column(name = "SERV_INCOME")
	private Long servIncome;

	/**
	 * 项目地址ID
	 */
	@Column(name = "PROJECT_ADDR_ID", length = 32)
	private String projectAddrId;

	/**
	 * 项目地址明细
	 */
	@Column(name = "PROJECT_ADDR", length = 512)
	private String projectAddr;

	/**
	 * 联系人名称
	 */
	@Column(name = "RECIPIENT_NAME", length = 16)
	private String recipientName;

	/**
	 * 联系人电话
	 */
	@Column(name = "RECIPIENT_PHONE", length = 16)
	private String recipientPhone;

	/**
	 * 自定义数据
	 */
	@Lob
	@Column(name = "EXT_DATA")
	private String extData;

	/**
	 * 创建时间
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CREATE_TIME")
	private Date createTime;

	/**
	 * 更新时间
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "UPDATE_TIME")
	private Date updateTime;

	/**
	 * 删除标记（0：否1：是）
	 */
	@Column(name = "IS_DELETE")
	private Integer isDelete;
	
	/**
	 * <p>添加字段IS_STATISTIC</p>
	 *
	 * @author  chenhao30 
	 * @version V1.0  
	 * @date 2016年10月26日 下午7:51:55 
	 * @modificationHistory=========================逻辑或功能性重大变更记录
	 * @modify by user: {修改人} 2016年10月26日
	 * @since  
	 */
	/**
	 * 订单是否已被统计
	 */
	@Column(name = "IS_STATISTIC")
	private Integer isStatistic;
	
	/**
	 * 订单下单时间
	 */
	@Column(name = "ORDER_TIME")
	private Date orderTime;
	
	/**
	 * 是否试用订单
	 */
	@Column(name = "IS_TRIAL")
	private Integer isTrial;

//	/**
//	 * 订单中的设备产品
//	 */
//	private List<SaleOrderDevice> orderDevList = new ArrayList<SaleOrderDevice>();

//	/**
//	 * 订单中的服务产品
//	 */
//	private List<SaleOrderServ> orderServList = new ArrayList<SaleOrderServ>();
	
	public enum Field implements jef.database.Field {
		id, code, customerId, partnerId, orgId, status, devDiscount, servDiscount, sumIncome, devIncome, servIncome, projectAddrId, projectAddr, recipientName, recipientPhone, extData, createTime, updateTime, isDelete,isStatistic, orderTime, isTrial
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getPartnerId() {
		return partnerId;
	}

	public void setPartnerId(String partnerId) {
		this.partnerId = partnerId;
	}

	public String getOrgId() {
		return orgId;
	}

	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getDevDiscount() {
		return devDiscount;
	}

	public void setDevDiscount(Integer devDiscount) {
		this.devDiscount = devDiscount;
	}

	public Integer getServDiscount() {
		return servDiscount;
	}

	public void setServDiscount(Integer servDiscount) {
		this.servDiscount = servDiscount;
	}

	public Long getSumIncome() {
		return sumIncome;
	}

	public void setSumIncome(Long sumIncome) {
		this.sumIncome = sumIncome;
	}

	public Long getDevIncome() {
		return devIncome;
	}

	public void setDevIncome(Long devIncome) {
		this.devIncome = devIncome;
	}

	public Long getServIncome() {
		return servIncome;
	}

	public void setServIncome(Long servIncome) {
		this.servIncome = servIncome;
	}

	public String getProjectAddrId() {
		return projectAddrId;
	}

	public void setProjectAddrId(String projectAddrId) {
		this.projectAddrId = projectAddrId;
	}

	public String getProjectAddr() {
		return projectAddr;
	}

	public void setProjectAddr(String projectAddr) {
		this.projectAddr = projectAddr;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public void setRecipientName(String recipientName) {
		this.recipientName = recipientName;
	}

	public String getRecipientPhone() {
		return recipientPhone;
	}

	public void setRecipientPhone(String recipientPhone) {
		this.recipientPhone = recipientPhone;
	}

	public String getExtData() {
		return extData;
	}

	public void setExtData(String extData) {
		this.extData = extData;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Integer getIsDelete() {
		return isDelete;
	}

	public void setIsDelete(Integer isDelete) {
		this.isDelete = isDelete;
	}
	
	public Integer getIsStatistic() {
		return isStatistic;
	}

	
	public void setIsStatistic(Integer isStatistic) {
		this.isStatistic = isStatistic;
	}

	public Date getOrderTime() {
		return orderTime;
	}

	
	public void setOrderTime(Date orderTime) {
		this.orderTime = orderTime;
	}

	
	public Integer getIsTrial() {
		return isTrial;
	}

	
	public void setIsTrial(Integer isTrial) {
		this.isTrial = isTrial;
	}

}
