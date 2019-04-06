package com.github.geequery.springdata.test.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class ComplexFoo {
	private static final long serialVersionUID = 5239860603423609583L;

	public ComplexFoo() {
	}

	public ComplexFoo(int userId, int classId) {
		this.userId = userId;
		this.classId = classId;
	}

	@Id
	private int userId;

	@Id
	private int classId;

	private String message;

	@GeneratedValue(generator = "created")
	private Date created;

	@GeneratedValue(generator = "modified")
	private Date modified;

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getClassId() {
		return classId;
	}

	public void setClassId(int classId) {
		this.classId = classId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public enum Field implements jef.database.Field {
		userId, classId, message, created, modified;
	}

	@Override
	public String toString() {
		return this.userId + "-" + this.classId + ":" + message;
	}

}
