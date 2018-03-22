package com.github.geequery.test.database.complexjoin;

import java.awt.geom.Area;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import jef.database.DataObject;
import jef.database.annotation.Indexed;

import com.github.geequery.orm.annotation.Comment;

/**
 * 组织表
 */
@Entity
@Table(name = "uc_org_info")
@Comment("组织信息表")
public class Org extends DataObject {

	private static final long serialVersionUID = 1L;

	@Id
	@Comment("组织ID")
	@Column(length = 32)
	private String id;

	/**
	 * 组织名称
	 */
	@Column(name = "org_name", nullable = false, length = 128)
	@Comment("组织名称")
	private String orgName;

	@Comment("组织维度")
	@Column(name = "org_category")
	@Enumerated(EnumType.ORDINAL)
	private OrgDimension orgCategory;

	@Comment("组织类型:0：单位 1：普通部门  2：维修小组")
	@Column(name = "org_type")
	@Enumerated(EnumType.ORDINAL)
	private OrgType type;

	/**
     *
     */
	@Comment("上级组织")
	@Column(name = "parent", length = 32)
	private String parent;

	@Comment("单位地址")
	@Column(name = "org_address")
	private String orgAddress;

	@Lob
	@Column(length = 512)
	@Comment("组织描述")
	private String desc;

	// 公共字段
	@Comment("创建人员")
	@Column(name = "create_by", length = 32)
	private String createBy;

	@Comment("创建时间")
	@Temporal(TemporalType.TIMESTAMP)
	private Date created;

	@Comment("修改人员")
	@Column(name = "modify_by", length = 32)
	private String modifyBy;

	@Comment("修改时间")
	@Temporal(TemporalType.TIMESTAMP)
	private Date modified;

	@Comment("所属组织")
	@Indexed
	@Column(name = "owner_org", length = 32)
	private String ownerOrg;

	@Comment("true：有效 false：无效（逻辑删除）;默认为有效")
	@Indexed
	@Column(name = "data_valid", columnDefinition = " boolean DEFAULT true ")
	private Boolean dataValid = Boolean.TRUE;

	private List<Area> areas;

	public enum Field implements jef.database.Field {
		id, orgName, orgCategory, type, parent, orgAddress, desc,
		// 额外属性
		ownerOrg, createBy, created, modifyBy, modified, dataValid
	}

	public List<Area> getAreas() {
		return areas;
	}

	// public Org getOrg() {
	// return org;
	// }
	//
	// public void setOrg(Org org) {
	// this.org = org;
	// }

	public void setAreas(List<Area> areas) {
		this.areas = areas;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

	public OrgDimension getOrgCategory() {
		return orgCategory;
	}

	public void setOrgCategory(OrgDimension orgCategory) {
		this.orgCategory = orgCategory;
	}

	public OrgType getType() {
		return type;
	}

	public void setType(OrgType type) {
		this.type = type;
	}

	public String getOrgAddress() {
		return orgAddress;
	}

	public void setOrgAddress(String orgAddress) {
		this.orgAddress = orgAddress;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public String getModifyBy() {
		return modifyBy;
	}

	public void setModifyBy(String modifyBy) {
		this.modifyBy = modifyBy;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public String getOwnerOrg() {
		return ownerOrg;
	}

	public void setOwnerOrg(String ownerOrg) {
		this.ownerOrg = ownerOrg;
	}

	public Boolean getDataValid() {
		return dataValid;
	}

	public void setDataValid(Boolean dataValid) {
		this.dataValid = dataValid;
	}
}
