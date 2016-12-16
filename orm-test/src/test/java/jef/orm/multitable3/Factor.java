package jef.orm.multitable3;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import jef.database.annotation.FieldOfTargetEntity;

@Entity
public class Factor extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	private int id;

	private int name1Id;

	private int name2Id;
	
	@ManyToOne(targetEntity=Names.class)
	@JoinColumn(name="name1Id",referencedColumnName="id")
	@FieldOfTargetEntity("name")
	private String name1;
	
	@ManyToOne(targetEntity=Names.class)
	@JoinColumn(name="name2Id",referencedColumnName="id")
	@FieldOfTargetEntity("name")
	private String name2;

	public String getName1() {
		return name1;
	}

	public void setName1(String name1) {
		this.name1 = name1;
	}

	public String getName2() {
		return name2;
	}

	public void setName2(String name2) {
		this.name2 = name2;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getName1Id() {
		return name1Id;
	}

	public void setName1Id(int name1Id) {
		this.name1Id = name1Id;
	}

	public int getName2Id() {
		return name2Id;
	}

	public void setName2Id(int name2Id) {
		this.name2Id = name2Id;
	}

	public enum Field implements jef.database.Field {
		id, name1Id, name2Id
	}
}
