package org.easyframe.tutorial.lesson1.entity;

import java.io.Serializable;
import java.lang.Thread.State;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(name = "UNKNOWN", uniqueConstraints = { @UniqueConstraint(columnNames = {  }) })
@Entity
public class Foo implements Serializable{
	@Id
	@Column
	private int id;

	@Column
	private String name;

	@Column
	@GeneratedValue(generator="created")
	private Date created;

	@Column
	@GeneratedValue(generator="modified")
	private LocalDateTime modified;

	@Column
	private State state = State.NEW;

	@Column
	private Date dateOfBirth;

	@Lob
	@Column
	private String text="DEFAULT - CONTENT";

	@OneToMany(mappedBy = "pid")
	private List<Child> children;

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

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public LocalDateTime getModified() {
		return modified;
	}

	public void setModified(LocalDateTime modified) {
		this.modified = modified;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}


	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<Child> getChildren() {
		return children;
	}

	public void setChildren(List<Child> children) {
		this.children = children;
	}

	public enum Field implements jef.database.Field {
		id, name, created, modified, state, dateOfBirth, text
	}
}
