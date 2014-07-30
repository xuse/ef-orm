package org.easyframe.tutorial.lesson5.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import jef.database.DataObject;
import jef.database.annotation.Cascade;

@Entity()
public class Item extends DataObject {

    @Id
    @GeneratedValue
    private int id;

    private String name;

    private int catalogyId;
    
    //扩展字段--写入用
    @OneToMany(targetEntity=ItemExtendInfo.class,mappedBy="itemId",cascade={CascadeType.MERGE,CascadeType.REMOVE})
    private List<ItemExtendInfo> itemExtInfos;
    
    public Item(){}
    
    public Item(String name){
    	this.name=name;
    }
    
    public Item(int id){
    	this.id=id;
    }

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

    public int getCatalogyId() {
        return catalogyId;
    }

    public void setCatalogyId(int catalogyId) {
        this.catalogyId = catalogyId;
    }

    public enum Field implements jef.database.Field {
        id, name, catalogyId
    }

	public List<ItemExtendInfo> getItemExtInfos() {
		return itemExtInfos;
	}

	public void setItemExtInfos(List<ItemExtendInfo> itemExtInfos) {
		this.itemExtInfos = itemExtInfos;
	}

	public Item setExtendInfo(String code, String length, String locale) {
		itemExtInfos=new ArrayList<ItemExtendInfo>();
		itemExtInfos.add(new ItemExtendInfo("录像编号",code));
		itemExtInfos.add(new ItemExtendInfo("拍摄时长",length));
		itemExtInfos.add(new ItemExtendInfo("拍摄地点",locale));
		return this;
	}
}
