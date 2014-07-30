package org.easyframe.tutorial.lesson4.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import jef.database.DataObject;

/**
 * 数据字典对象
 * @author jiyi
 *
 */
@Entity
@Table(name="data_dict")
public class DataDict extends DataObject {

    @Id
    @GeneratedValue
    private int id;
    
    @Column(name="dict_type")
    private String type;

    @Column(name="value")
    private String value;

    @Column(name="text")
    private String text;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    
    public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	public DataDict(){}
	
	/**
	 * 构造
	 * @param type 字典类型，如 USER.GENDER  CAMERY.TYPE 等。
	 * @param value  存储的值
	 * @param text   显示的值
	 */
	public DataDict(String type, String value, String text) {
		super();
		this.type = type;
		this.value = value;
		this.text = text;
	}



	public enum Field implements jef.database.Field {
        id, type, value, text
    }
}
