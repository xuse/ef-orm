package com.github.geequery.springdata.test.entity;

import java.util.Date;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;
import javax.persistence.Entity;

@Entity()
public class VersionLog extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue
    private int id;

    private String name;

    @GeneratedValue(generator = "modified-sys")
    @Version
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

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public enum Field implements jef.database.Field {
        id, name, modified
    }
}
