package org.easyframe.tutorial.lesson1.entity;

import java.util.Date;
import javax.persistence.Id;
import javax.persistence.Entity;

@Entity
public class Foo2 extends jef.database.DataObject {

    @Id
    private int id;

    private String name;

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

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public enum Field implements jef.database.Field {
        id, name, created
    }
}
