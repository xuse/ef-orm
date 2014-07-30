package org.easyframe.tutorial.lesson5.entity;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import jef.database.DataObject;
import jef.database.annotation.JoinDescription;
import jef.database.annotation.JoinType;

@Entity
public class Catalogy extends DataObject {

    @Id
    @GeneratedValue
    private int id;
    
    private int parentId;

    private String name;

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

    public enum Field implements jef.database.Field {
        id, name,parentId
    }
    
    @OneToMany(mappedBy="catalogyId")
    //@JoinColumn(name="id",referencedColumnName="catalogyId")
    //当使用主键和其他实体关联时，可以将@JoinColumn(name="id",referencedColumnName="catalogyId")简写为mappedBy="catalogyId"
    private List<Item> items;
    
    @ManyToOne()
    @JoinColumn(name="parentId",referencedColumnName="id")
    private Catalogy parent;

    
	public int getParentId() {
		return parentId;
	}

	public void setParentId(int parentId) {
		this.parentId = parentId;
	}

	public Catalogy getParent() {
		return parent;
	}

	public void setParent(Catalogy parent) {
		this.parent = parent;
	}

	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}
}
