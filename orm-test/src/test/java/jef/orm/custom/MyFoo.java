package jef.orm.custom;

import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import jef.database.DataObject;
import jef.database.annotation.Type;

import com.alibaba.fastjson.JSON;

@Entity()
public class MyFoo extends DataObject {
	@Id
	@GeneratedValue
	private int id;

	private String name;

	@Column(columnDefinition = "varchar(256)")
	@Type(JSONVarcharMapping.class)
	private List<String> data;

	/**
	 * 默认PG没有用开启hstore。需要人工执行
	 * CREATE EXTENSION hstore; 
	 */
	@Column(name = "hstore_c", columnDefinition = "hstore")
	@Type(HstoreMapMapping.class)
	private Map<String, String> hstoreField;

	@Column(name = "json_col", columnDefinition = "json")
	@Type(ObjectJsonMapping.class)
	private JSON jsonField;

	@Column(name = "jsonb_col", columnDefinition = "jsonb")
	@Type(ObjectJsonbMapping.class)
	private Map<String,Object> jsonbField;
	
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

	public List<String> getData() {
		return data;
	}

	public void setData(List<String> data) {
		this.data = data;
	}

	public Map<String, String> getHstoreField() {
		return hstoreField;
	}

	public void setHstoreField(Map<String, String> hstoreField) {
		this.hstoreField = hstoreField;
	}

	public JSON getJsonField() {
		return jsonField;
	}

	public void setJsonField(JSON jsonField) {
		this.jsonField = jsonField;
	}

	public Map<String, Object> getJsonbField() {
		return jsonbField;
	}

	public void setJsonbField(Map<String, Object> jsonbField) {
		this.jsonbField = jsonbField;
	}

	public enum Field implements jef.database.Field {

		id, name, data, hstoreField, jsonField,jsonbField
	}
}
