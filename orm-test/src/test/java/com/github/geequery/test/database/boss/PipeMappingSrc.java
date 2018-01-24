package com.github.geequery.test.database.boss;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import jef.codegen.support.NotModified;
import jef.database.DataObject;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;
import com.github.geequery.orm.annotation.Comment;

@NotModified
@Entity
@Table(name = "pipe_mapping_src")
@JSONType(ignores = { "query", "updateValueMap" })
public class PipeMappingSrc extends DataObject {

	private static final long serialVersionUID = 4809844989121161372L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "i_id", precision = 16, nullable = false)
	@Comment("主键id")
	private Long id;

	@Column(name = "c_pipe_code", length = 64, nullable = false)
	@Comment("管道编号")
	private String pipeCode;

	@Column(name = "c_src_code", length = 64, nullable = false)
	@Comment("数据源编号")
	private String srcCode;
	
	@Column(name = "c_ias_code", length = 64, nullable = false)
	@Comment("ias编号，方便管道通知用")
	private String iasCode;

	@Column(name = "c_device_code", length = 64, nullable = false)
	@Comment("设备编号")
	private String deviceCode;
	
	@Column(name = "d_create_time", columnDefinition = "TIMESTAMP", nullable = false)
	@JSONField(serialize = false)
	@GeneratedValue(generator = "created")
	@Comment("创建时间")
	private Date createTime;

	public enum Field implements jef.database.Field {
		id, pipeCode, srcCode, iasCode, deviceCode, createTime
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPipeCode() {
		return pipeCode;
	}

	public void setPipeCode(String pipeCode) {
		this.pipeCode = pipeCode;
	}

	public String getSrcCode() {
		return srcCode;
	}

	public void setSrcCode(String srcCode) {
		this.srcCode = srcCode;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getIasCode() {
		return iasCode;
	}

	public void setIasCode(String iasCode) {
		this.iasCode = iasCode;
	}

	public String getDeviceCode() {
		return deviceCode;
	}

	public void setDeviceCode(String deviceCode) {
		this.deviceCode = deviceCode;
	}
	
}
