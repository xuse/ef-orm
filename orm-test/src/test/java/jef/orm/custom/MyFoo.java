package jef.orm.custom;

import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;

import jef.database.DataObject;
import jef.database.annotation.Type;

import com.alibaba.fastjson.JSON;

@Entity()
public class MyFoo extends DataObject {

	private int id;

	private String name;

	@Column(columnDefinition = "varchar(256)")
	@Type(JSONVarcharMapping.class)
	private List<String> data;

	@Column(name = "hstore_c", columnDefinition = "hstore")
	@Type(HstoreMapMapping.class)
	private Map<String, String> hstoreField;

	@Column(name = "jsonb_c", columnDefinition = "json")
	@Type(JsonJsonMapping.class)
	private JSON jsonField;

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

	public enum Field implements jef.database.Field {

		id, name, data, hstoreField, jsonField
	}
}
