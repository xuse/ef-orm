package org.easyframe.tutorial.lessona.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Table;

import jef.database.DataObject;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;
import jef.database.routing.function.KeyFunction;

@PartitionTable(key = {
// 分区关键字1为name字段，取头3个字符
		@PartitionKey(field = "name", length = 3),
		// 分区关键字2为created字段(日期型，取其月份数，长度不足2则补充到2)，
		@PartitionKey(field = "created", function = KeyFunction.MONTH, length = 2), })
@Table(name = "PEOPLE_TABLE")
@Entity
public class PeopleTable extends DataObject {
	private int id;

	private String name;

	private Date created;

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

	public enum Field implements jef.database.Field {
		id, name, created 
	}
}
