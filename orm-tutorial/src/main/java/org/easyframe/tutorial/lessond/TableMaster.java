package org.easyframe.tutorial.lessond;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import jef.database.annotation.Indexed;

/**
 * 例子 
 * 1 @Indexed普通索引created 
 * 2 复合索引： IDX_NAME_AID
 * 3 UNIQUE约束 name 
 * 4 复合unique约束
 * 
 * 
 * @author publicxtgxrj10
 * 
 */
@Entity
@Table(
		indexes = { @Index(name = "IDX_NAME_AID", columnList = "aid, name", unique = true) }, 
		uniqueConstraints = { @UniqueConstraint(columnNames = { "aid,bid" })

})
public class TableMaster extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private int id;

	@Column(columnDefinition = "varchar(100)", unique = true)
	private String name;

	@GeneratedValue(generator = "modified")
	private Date modified;

	@GeneratedValue(generator = "created")
	@Indexed
	private Date created;

	@Column(name = "account_id")
	private int aid;

	@Column(name = "busi_id")
	private int bid;

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

	public int getAid() {
		return aid;
	}

	public void setAid(int aid) {
		this.aid = aid;
	}

	public int getBid() {
		return bid;
	}

	public void setBid(int bid) {
		this.bid = bid;
	}


	public enum Field implements jef.database.Field {
		id, name, modified, created, aid, bid
	}
}
