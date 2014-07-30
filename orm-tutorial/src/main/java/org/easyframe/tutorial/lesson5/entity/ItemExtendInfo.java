package org.easyframe.tutorial.lesson5.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import jef.database.DataObject;

/**
 * 描述Item的扩展信息
 * @author jiyi
 *
 */
@Entity()
public class ItemExtendInfo extends DataObject {

    /**
	 * 所属Item
	 */
    @Id
    private int itemId;

    @Id
    private String key;

    private String value;

    
    public ItemExtendInfo(){
    }
    
    public ItemExtendInfo(String key,String value){
    	this.key=key;
    	this.value=value;
    }
    
    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public enum Field implements jef.database.Field {
        itemId, key, value
    }
}
