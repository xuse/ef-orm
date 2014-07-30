package jef.orm.multitable.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import jef.database.DataObject;

/**
 * JEF-ORM 演示用例
 * 
 * 描述 人 <-> 学校关系（多对一）
 * @author Administrator
 * @Date 2011-4-12
 */
@Entity
public class School extends DataObject{
	private static final long serialVersionUID = 1L;
	
	
	public School() {
	}
	
	public School(String name){
		this.name=name;
	}
	
	@Column
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	private int id;
	
	@Column(length=64,nullable=false,name="SCHOOL_NAME")
	private String name;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	//元模型
	public enum Field implements jef.database.Field{
		id,name
	}
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public String toString() {
		return id+":"+name;
	}
	
	
}
