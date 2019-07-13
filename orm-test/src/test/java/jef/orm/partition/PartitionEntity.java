package jef.orm.partition;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import  org.apache.commons.lang3.builder.ToStringBuilder;

import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;
import jef.database.routing.function.KeyFunction;

@PartitionTable(key = {
		@PartitionKey(field = "intField", length = 2, function = KeyFunction.MODULUS, functionConstructorParams = { "3" }), 
		@PartitionKey(field = "attr:dbkey", isDbName = true) })
@Entity
@Table(name="partitionentity")
public class PartitionEntity extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private int id;

	@Column(name = "DATE_FIELD")
	private Date dateField;

	@Column(name = "INT_FIELD")
	private int intField;

	private String name;

	@Column(name = "LONG_FIELD")
	private long longField;

	public int getIntField() {
		return intField;
	}

	public void setIntField(int intField) {
		this.intField = intField;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getLongField() {
		return longField;
	}

	public void setLongField(long longField) {
		this.longField = longField;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDateField() {
		return dateField;
	}

	public void setDateField(Date dateField) {
		this.dateField = dateField;
	}

	public enum Field implements jef.database.Field {
		id, dateField, name, intField, longField
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
