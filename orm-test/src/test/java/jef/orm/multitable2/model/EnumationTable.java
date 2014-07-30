package jef.orm.multitable2.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import jef.database.DataObject;

/**
 * 字典表
 * @author jiyi
 *
 */
@SuppressWarnings("serial")
@Entity
public class EnumationTable extends DataObject{
	@Id
	private String type;
	
	@Id
	private String code;
	
	@Column(name="text")
	private String name;
	
	@Column(name="descrption")
	private String desc;
	
	@Column(name="enable",columnDefinition="boolean not null default true")
	private Boolean enable;

	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	public Boolean getEnable() {
		return enable;
	}
	public void setEnable(Boolean enable) {
		this.enable = enable;
	}



	public enum Field implements jef.database.Field{
		code,type,name,desc,enable
	}
}
