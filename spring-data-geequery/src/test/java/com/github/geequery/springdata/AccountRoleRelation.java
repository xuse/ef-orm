package com.github.geequery.springdata;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

@Entity()
public class AccountRoleRelation extends jef.database.DataObject {
	
	
	@OneToMany(targetEntity = Role.class, mappedBy = "id")
	private String roleId;

	@OneToMany(targetEntity = Account.class, mappedBy = "accountName")
	private String account;

	public enum Field implements jef.database.Field {
		roleId, account
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}
}
