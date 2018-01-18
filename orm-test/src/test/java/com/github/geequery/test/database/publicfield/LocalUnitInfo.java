package com.github.geequery.test.database.publicfield;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import jef.codegen.support.NotModified;

import com.github.geequery.orm.annotation.Comment;

@NotModified
@Entity()
@Table(name = "localunit_info")
public class LocalUnitInfo extends jef.database.DataObject {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	@Comment("主键id")
	private Long id;

	private Long unitId;

	private String indexCode;

	private String name;

	private Long parentId;

	private Long controlUnitId;

	private Integer useVtdu;

	private Integer vtduId;

	private Integer unitType;

	private Integer queryStatus;

	private String elseEx;

	private String parentIndex;

	public String getElseEx() {
		return elseEx;
	}

	public void setElseEx(String elseEx) {
		this.elseEx = elseEx;
	}

	@Column(name = "updataTime")
	@Comment("更新时间")
	@GeneratedValue(generator = "modified")
	private String updateTime;

	private LocalUnitInfo parentUnit;

	private UnitShareInfo unitShareInfo;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUnitId() {
		return unitId;
	}

	public void setUnitId(Long unitId) {
		this.unitId = unitId;
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

	public Integer getUseVtdu() {
		return useVtdu;
	}

	public void setUseVtdu(Integer useVtdu) {
		this.useVtdu = useVtdu;
	}

	public Integer getVtduId() {
		return vtduId;
	}

	public void setVtduId(Integer vtduId) {
		this.vtduId = vtduId;
	}

	public Integer getUnitType() {
		return unitType;
	}

	public void setUnitType(Integer unitType) {
		this.unitType = unitType;
	}

	public Integer getQueryStatus() {
		return queryStatus;
	}

	public void setQueryStatus(Integer queryStatus) {
		this.queryStatus = queryStatus;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public Long getControlUnitId() {
		return controlUnitId;
	}

	public void setControlUnitId(Long controlUnitId) {
		this.controlUnitId = controlUnitId;
	}

	public LocalUnitInfo getParentUnit() {
		return parentUnit;
	}

	public void setParentUnit(LocalUnitInfo parentUnit) {
		this.parentUnit = parentUnit;
	}

	public UnitShareInfo getUnitShareInfo() {
		return unitShareInfo;
	}

	public void setUnitShareInfo(UnitShareInfo unitShareInfo) {
		this.unitShareInfo = unitShareInfo;
	}

	public String getParentIndex() {
		return parentIndex;
	}

	public void setParentIndex(String parentIndex) {
		this.parentIndex = parentIndex;
	}

	public enum Field implements jef.database.Field {

		id, unitId, indexCode, name, parentId, controlUnitId, useVtdu, vtduId, unitType, queryStatus, updateTime, elseEx
	}
}
