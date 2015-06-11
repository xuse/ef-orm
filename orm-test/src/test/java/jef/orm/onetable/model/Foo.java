package jef.orm.onetable.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import jef.database.DataObject;

@SuppressWarnings("serial")
@Entity
public class Foo extends DataObject{
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	@SequenceGenerator(name="s",sequenceName="s_FOO")
	@Column(name="ID",columnDefinition="INT")
	private int id;
	
//	@Column(name="DATE",columnDefinition="DATE")
	private String name;
	
	@GeneratedValue
	private Date modified;
	
	
	@Column(columnDefinition="number(1)")
	@Enumerated
	private java.lang.Thread.State state = java.lang.Thread.State.NEW;
	
	
	public java.lang.Thread.State getState() {
		return state;
	}

	public void setState(java.lang.Thread.State state) {
		this.state = state;
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
	public Date getModified() {
		return modified;
	}
	public void setModified(Date modified) {
		this.modified = modified;
	}
	public enum Field implements jef.database.Field{
		id,name,modified,state
	}
}
