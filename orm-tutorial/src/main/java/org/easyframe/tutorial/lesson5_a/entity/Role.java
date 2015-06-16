package org.easyframe.tutorial.lesson5_a.entity;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import jef.database.DataObject;

@Entity
@Table(name="t_role")
public class Role extends DataObject {
	@Id
	@GeneratedValue
	private int id;

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

	public enum Field implements jef.database.Field {
		id, name
	}
	
	
	@ManyToMany(cascade= {CascadeType.REFRESH})
	@JoinTable(name = "user_roles", 
			inverseJoinColumns	 = { @JoinColumn(name = "user_id", referencedColumnName = "id") }, 
				 joinColumns = { @JoinColumn(name = "role_id", referencedColumnName = "id") }
	)
	private List<User> users;

	public List<User> getUsers() {
		return users;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}
}
