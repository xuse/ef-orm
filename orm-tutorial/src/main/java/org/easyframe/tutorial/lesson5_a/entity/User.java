package org.easyframe.tutorial.lesson5_a.entity;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import jef.database.DataObject;

@Entity
@Table(name="t_user")
public class User extends DataObject{
	@Id
	@GeneratedValue
	private int id;

	private String name;

	@ManyToMany()
	@JoinTable(name = "user_roles", 
		joinColumns = { @JoinColumn(name = "user_id", referencedColumnName = "id") }, 
		inverseJoinColumns = { @JoinColumn(name = "role_id", referencedColumnName = "id") }
	)
	private List<Role> roles;

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

	public enum Field implements jef.database.Field {
		id, name
	}

	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}
}
