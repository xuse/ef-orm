package com.github.geequery.springdata.test.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class Foo{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private int id;

	@Column(name="NAME_A")
	private String name;

	@Column(name="REMARK_A")
	private String remark;

	@Temporal(TemporalType.DATE)
	@Column(name="BIRTHDAY_A")
	private Date birthDay;

	private int age;
	
	private String indexCode;
	
	@Temporal(TemporalType.TIMESTAMP)
	@GeneratedValue(generator="modified")
	private Date lastModified;

	public Foo() {
	}

	public Foo(String string) {
		this.name = string;
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

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Date getBirthDay() {
		return birthDay;
	}

	public void setBirthDay(Date birthDay) {
		this.birthDay = birthDay;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getIndexCode() {
        return indexCode;
    }

    public void setIndexCode(String indexCode) {
        this.indexCode = indexCode;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public enum Field implements jef.database.Field {
		id, name, remark, birthDay, age, indexCode, lastModified
	}

	@Override
	public String toString() {
		return id + ":" + name;
	}

}
