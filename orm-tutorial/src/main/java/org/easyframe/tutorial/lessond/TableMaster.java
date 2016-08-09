package org.easyframe.tutorial.lessond;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class TableMaster extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue
    private int id;

    @Column(columnDefinition="varchar(100) unique")
    private String name;

    @GeneratedValue(generator = "modified")
    private Date modified;

    @GeneratedValue(generator = "created")
    private Date created;

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

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public enum Field implements jef.database.Field {
        id, name, modified, created
    }
}
