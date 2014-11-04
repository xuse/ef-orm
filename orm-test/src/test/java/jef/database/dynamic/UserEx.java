package jef.database.dynamic;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import jef.database.EntityExtensionSupport;
import jef.database.annotation.DynamicKeyValueExtension;
import jef.database.annotation.EasyEntity;

@EasyEntity
@Table(name="TUSER")
@DynamicKeyValueExtension(table="USER_EXTENDS",keyColumn="key",valueColumn="value_text",metadata="USER_EX")
public class UserEx extends EntityExtensionSupport {
	@Id
	@GeneratedValue
	private int id;
	
	@Column
	private String comm;

	@Column
	private String name;
	
	@Column
	private int status;
	
	@ManyToOne
	@JoinColumn(name="status",referencedColumnName="code")
	private Status stObj;
	

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Status getStObj() {
		return stObj;
	}

	public void setStObj(Status stObj) {
		this.stObj = stObj;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getComm() {
		return comm;
	}

	public void setComm(String comm) {
		this.comm = comm;
	}

	public enum Field implements jef.database.Field {
		id, name, comm, status
	}
}
