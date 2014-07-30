package org.easyframe.tutorial.lesson4.entity;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import jef.database.DataObject;

/**
 * 实体:学校
 * 描述一个学校的ID和名称
 */
public class School extends DataObject{
	@Column
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	
	@Column(length=64,nullable=false)
	private String name;
	//元模型
	public enum Field implements jef.database.Field{
		id,name
	}
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
	public School(){
	}
	public School(String name){
		setName(name);
	}
	@Override
	public String toString() {
		return "school:"+id+":"+name;
	}
}