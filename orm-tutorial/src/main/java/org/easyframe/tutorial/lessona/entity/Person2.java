package org.easyframe.tutorial.lessona.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import jef.database.DataObject;
import jef.database.annotation.BindDataSource;

/**
 * 垂直拆分的实体。
 * 所谓数据垂直拆分，意思是将一类表放到不同的数据库上。从而降低负载。
 *  
 * Person2 就是绑定到2号数据源上操作的实体。
 * 
 * @author jiyi
 *
 */
@Entity
@BindDataSource("datasource2")
public class Person2 extends DataObject {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
    private int id;

	@Column(length=64)
    private String name;

	@Column(name="DATA_DESC",length=255)
    private String desc;

    @GeneratedValue(generator="created")
    private Date created;

    @GeneratedValue(generator="modified")
    private Date modified;

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

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public enum Field implements jef.database.Field {
        id, name, desc, created, modified
    }
}
