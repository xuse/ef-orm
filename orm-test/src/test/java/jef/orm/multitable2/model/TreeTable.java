package jef.orm.multitable2.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import jef.database.DataObject;
import jef.database.annotation.FieldOfTargetEntity;

@Entity
public class TreeTable extends DataObject{
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	@SequenceGenerator(sequenceName="MAIN1", name = "id")
	private int id;
	private int parentId;
	private String name;
	@Column(name="desc1")
	private String desc;
	
	@OneToOne(targetEntity=Leaf.class)
	@FieldOfTargetEntity("name")
	@JoinColumns(value = { 
		@JoinColumn(name="id",referencedColumnName="id")
	})
	private String refField;
	
	public String getRefField() {
		return refField;
	}
	public void setRefField(String refField) {
		this.refField = refField;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getParentId() {
		return parentId;
	}
	public void setParentId(int parentId) {
		this.parentId = parentId;
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
	
	public enum Field implements jef.database.Field{
		id,parentId,name,desc
	}
}
