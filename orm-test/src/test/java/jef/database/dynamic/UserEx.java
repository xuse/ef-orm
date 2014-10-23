package jef.database.dynamic;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import jef.database.DataObject;
import jef.database.annotation.DynamicKeyValueExtension;
import jef.database.annotation.DynamicPropertyGetter;
import jef.database.annotation.DynamicPropertySetter;
import jef.database.annotation.EasyEntity;

@EasyEntity
@DynamicKeyValueExtension(table="USER_EXTENDS",keyColumn="key",valueColumn="value",metadata="USER_EX")
public class UserEx extends DataObject {
	@Id
	@GeneratedValue
	private int id;
	
	@Column
	private String comm;

	@Column
	private String name;
	
	private Map<String,Object> specProps;
	
	public Map<String, Object> getSpecProps() {
		return specProps;
	}

	/**
	 * 设置扩展属性
	 * @param prop
	 * @param value
	 */
	@DynamicPropertySetter
	public void setExtProp(String prop,Object value){
		if(specProps==null){
			specProps=new HashMap<String,Object>();
		}
		specProps.put(prop, value);
	}
	
	/**
	 * 获取扩展属性
	 * @param prop
	 * @return
	 */
	@DynamicPropertyGetter
	public Object getExtProp(String prop){
		if(specProps==null){
			return null;
		}
		return specProps.get(prop);
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
		id, name, comm
	}
	
	

}
