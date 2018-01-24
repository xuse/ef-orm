package com.github.geequery.test.database.boss;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.Max;

import jef.codegen.support.NotModified;
import jef.database.DataObject;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;
import com.github.geequery.orm.annotation.Comment;

@NotModified
@Entity
@Table(name = "device_src")
@JSONType(ignores = { "query", "updateValueMap" })
public class DeviceSrc extends DataObject {

	private static final long serialVersionUID = 629489617126679232L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "i_id", precision = 16, nullable = false)
	@Comment("主键id")
	private Long id;

	@Column(name = "c_manufacturer", length = 64, nullable = false)
	@Comment("厂商")
	private String manufacturer;

	@Column(name = "c_protocol", length = 64, nullable = false)
	@Comment("协议")
	private String protocol;
	
	@Column(name = "c_index_code", length = 64, nullable = false)
	@Comment("编号")
	private String indexCode;

	@Column(name = "c_inside_code", length = 64)
	@Comment("联网编号")
	private String insideCode;

	@Column(name = "c_name", length = 64)
	@Comment("名称")
	private String name;

	@Column(name = "c_description", length = 64)
	@Comment("详情")
	private String description;

	@Column(name = "i_addr_type", precision = 16)
	@Comment("地址类型")
	private Integer addrType;

	@Column(name = "c_addr", length = 64)
	@Comment("IP地址或域名")
	private String addr;

	@Column(name = "i_port", precision = 16)
	@Comment("端口号")
	private Integer port;

	@Column(name = "c_user_name", length = 32)
	@Comment("设备账号")
	private String userName;

	@Column(name = "c_password", length = 128)
	@Comment("密码")
	private String password;

	@Column(name = "i_pwd_strength", precision = 16)
	@Comment("密码强度")
	private Integer pwdStrength;

	@Column(name = "c_extend", length = 1000)
	@Comment("扩展字段")
	private String extend;

	@Column(name = "i_net_zone_id", precision = 8)
	@Comment("网域id")
	private Integer zoneId;

	@Column(name = "i_src_status", precision = 8)
	@Comment("设备状态")
	private Integer srcStatus;
	
	@Column(name = "b_remote_status", precision = 8)
	@Comment("设备状态")
	private Boolean  remoteStatus;

	@Column(name = "c_abilities", length = 128)
	@Comment("能力集合")
	private String abilities;

	@Column(name = "c_ias_code", length = 64)
	@Comment("所属ias服务编号")
	private String iasCode;

	@Column(name = "d_update_time", columnDefinition = "TIMESTAMP", nullable = false)
	@JSONField(serialize = false)
	@GeneratedValue(generator = "modified")
	@Comment("更新时间")
	private Date updateTime;

	@Column(name = "d_create_time", columnDefinition = "TIMESTAMP", nullable = false)
	@JSONField(serialize = false)
	@GeneratedValue(generator = "created")
	@Comment("创建时间")
	private Date createTime;


	@Valid
	@Max(256)
	private List<String> deleteChannels;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getIndexCode() {
		return indexCode;
	}

	public void setIndexCode(String indexCode) {
		this.indexCode = indexCode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getAddrType() {
		return addrType;
	}

	public void setAddrType(Integer addrType) {
		this.addrType = addrType;
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getPwdStrength() {
		return pwdStrength;
	}

	public void setPwdStrength(Integer pwdStrength) {
		this.pwdStrength = pwdStrength;
	}

	public String getExtend() {
		return extend;
	}

	public void setExtend(String extend) {
		this.extend = extend;
	}

	public Integer getZoneId() {
		return zoneId;
	}

	public void setZoneId(Integer zoneId) {
		this.zoneId = zoneId;
	}

	public Integer getSrcStatus() {
		return srcStatus;
	}

	public void setSrcStatus(Integer srcStatus) {
		this.srcStatus = srcStatus;
	}

	public String getAbilities() {
		return abilities;
	}

	public void setAbilities(String abilities) {
		this.abilities = abilities;
	}

	public String getIasCode() {
		return iasCode;
	}

	public void setIasCode(String iasCode) {
		this.iasCode = iasCode;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getInsideCode() {
		return insideCode;
	}

	public void setInsideCode(String insideCode) {
		this.insideCode = insideCode;
	}

	public List<String> getDeleteChannels() {
		return deleteChannels;
	}

	public void setDeleteChannels(List<String> deleteChannels) {
		this.deleteChannels = deleteChannels;
	}
	
	public Boolean getRemoteStatus() {
		return remoteStatus;
	}

	public void setRemoteStatus(Boolean remoteStatus) {
		this.remoteStatus = remoteStatus;
	}

	public enum Field implements jef.database.Field {
		id, manufacturer, protocol, indexCode, insideCode, name, description, addrType, addr, port, userName, password, pwdStrength, extend, zoneId, srcStatus, remoteStatus, abilities, iasCode, updateTime, createTime
	}

}
