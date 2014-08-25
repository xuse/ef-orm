package org.easyframe.tutorial.lesson7.entity;

import javax.persistence.Entity;

import jef.database.DataObject;
import jef.tools.StringUtils;

@Entity
public class NodeTable extends DataObject {

    private int id;

    private int pid;

    private String name;
    
    public NodeTable(){
    }

    public NodeTable(int i, int j, String string) {
    	this.id=i;
    	this.pid=j;
    	this.name=string;
	}

	public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum Field implements jef.database.Field {
        id, pid, name
    }

	@Override
	public String toString() {
		return "pid="+StringUtils.toFixLengthString(pid, 3)+"  "+name;
	}
}
