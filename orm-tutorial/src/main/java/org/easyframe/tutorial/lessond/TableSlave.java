package org.easyframe.tutorial.lessond;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(indexes={},
uniqueConstraints={}
)
@Entity
public class TableSlave {
	@Id
	@GeneratedValue
	private int id;

	private String name;

	@GeneratedValue(generator = "modified")
	private Date modified;

	@GeneratedValue(generator = "created")
	private Date created;
	
	private int accountId;
	
	private String accountPeriod;

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

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public enum Field implements jef.database.Field {
		id, name, modified, created,accountId,accountPeriod
	}

	public int getAccountId() {
		return accountId;
	}

	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}

	public String getAccountPeriod() {
		return accountPeriod;
	}

	public void setAccountPeriod(String accountPeriod) {
		this.accountPeriod = accountPeriod;
	}
}
