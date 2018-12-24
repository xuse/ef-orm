package com.github.geequery.spring.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import jef.database.annotation.BindDataSource;

@Entity
@Table(name = "ENTITY_2")
@BindDataSource("test2")
public class BindEntity2 extends jef.database.DataObject {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

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
        id, name
    }
}
