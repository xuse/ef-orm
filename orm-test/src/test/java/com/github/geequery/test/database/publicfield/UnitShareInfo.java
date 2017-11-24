package com.github.geequery.test.database.publicfield;

import com.github.geequery.orm.annotation.Comment;
import jef.codegen.support.NotModified;

import javax.persistence.*;

@Entity()
@Table(name = "unitshare_info")
public class UnitShareInfo extends jef.database.DataObject {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	@Comment("主键id")
	private Long id;

	private Integer unitId;

	private String indexCode;

	private String shareIndexCode;

	private String shareName;

	private String shareParent;

	private Integer shareUnitType;

	private Integer downCascadeId;

	private String downCascadeCode;

	private Integer upCascadeId;

	private String upCascadeCode;

	private Integer operateStatus;

	@Column(name = "updataTime")
	@Comment("更新时间")
	@GeneratedValue(generator = "modified")
	private String updateTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getUnitId() {
		return unitId;
	}

	public void setUnitId(Integer unitId) {
		this.unitId = unitId;
	}

	public String getIndexCode() {
		return indexCode;
	}

	public void setIndexCode(String indexCode) {
		this.indexCode = indexCode;
	}

	public String getShareIndexCode() {
		return shareIndexCode;
	}

	public void setShareIndexCode(String shareIndexCode) {
		this.shareIndexCode = shareIndexCode;
	}

	public String getShareName() {
		return shareName;
	}

	public void setShareName(String shareName) {
		this.shareName = shareName;
	}

	public String getShareParent() {
		return shareParent;
	}

	public void setShareParent(String shareParent) {
		this.shareParent = shareParent;
	}

	public Integer getShareUnitType() {
		return shareUnitType;
	}

	public void setShareUnitType(Integer shareUnitType) {
		this.shareUnitType = shareUnitType;
	}

	public Integer getDownCascadeId() {
		return downCascadeId;
	}

	public void setDownCascadeId(Integer downCascadeId) {
		this.downCascadeId = downCascadeId;
	}

	public String getDownCascadeCode() {
		return downCascadeCode;
	}

	public void setDownCascadeCode(String downCascadeCode) {
		this.downCascadeCode = downCascadeCode;
	}

	public Integer getUpCascadeId() {
		return upCascadeId;
	}

	public void setUpCascadeId(Integer upCascadeId) {
		this.upCascadeId = upCascadeId;
	}

	public String getUpCascadeCode() {
		return upCascadeCode;
	}

	public void setUpCascadeCode(String upCascadeCode) {
		this.upCascadeCode = upCascadeCode;
	}

	public Integer getOperateStatus() {
		return operateStatus;
	}

	public void setOperateStatus(Integer operateStatus) {
		this.operateStatus = operateStatus;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	public enum Field implements jef.database.Field {

		id, indexCode, shareIndexCode, shareName, shareParent, downCascadeId, downCascadeCode, upCascadeId, upCascadeCode, updateTime
	}
}
