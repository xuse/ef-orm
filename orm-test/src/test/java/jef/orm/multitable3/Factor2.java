package jef.orm.multitable3;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

@Entity()
public class Factor2 extends jef.database.DataObject {

	@Id
	@GeneratedValue
	private int id;
	
	private String name;

	@ManyToMany
	@JoinTable(name = "tenant_to_role", joinColumns = { @JoinColumn(name = "tenantid", referencedColumnName = "id") }, inverseJoinColumns = { @JoinColumn(name = "roleid", referencedColumnName = "id") })
	private List<Role> roles;
	
	@OneToMany(mappedBy="factorId")
	private List<Sub1> sub1;

	public List<Sub1> getSub1() {
		return sub1;
	}

	public void setSub1(List<Sub1> sub1) {
		this.sub1 = sub1;
	}

	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
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

	public enum Field implements jef.database.Field {
		id, name
	}
}
