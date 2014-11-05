package org.easyframe.tutorial.lesson5.entity;

import java.util.HashMap;
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

	// 扩展字段--写入用
	@Cascade(keyOfMap="key",valueOfMap="value")
	@OneToMany(targetEntity = ItemExtendInfo.class, mappedBy = "itemId", cascade = { CascadeType.MERGE, CascadeType.REMOVE })
	private Map<String,Object> itemExtInfos;

	public Item() {
	}

	public Item(String name) {
		this.name = name;
	}

	public Item(int id) {
		this.id = id;
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

	public Map<String, Object> getItemExtInfos() {
		return itemExtInfos;
	}

	public void setItemExtInfos(Map<String, Object> itemExtInfos) {
		this.itemExtInfos = itemExtInfos;
	}

	public Item setExtendInfo(String code, String length, String locale) {
		itemExtInfos = new HashMap<String,Object>();
//		itemExtInfos.put("录像编号",new ItemExtendInfo("录像编号", code));
//		itemExtInfos.put("拍摄时长",new ItemExtendInfo("拍摄时长", length));
//		itemExtInfos.put("拍摄地点",new ItemExtendInfo("拍摄地点", locale));
		
		itemExtInfos.put("录像编号", code);
		itemExtInfos.put("拍摄时长", length);
		itemExtInfos.put("拍摄地点", locale);
		
		return this;
	}
}
