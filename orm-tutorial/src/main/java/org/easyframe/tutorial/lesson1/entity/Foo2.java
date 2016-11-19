package org.easyframe.tutorial.lesson1.entity;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import jef.database.annotation.Type;
import jef.database.dialect.extension.ObjectJsonMapping;

import com.github.geequery.orm.annotation.Comment;

@Entity
public class Foo2 extends jef.database.DataObject {

    @Id
    private int id;

    private String name;

    private Date created;
    
    @Column(name="volume",columnDefinition="FLOAT")
    private double volume;
    
    @Lob
    private String comments;
    
    @Column(name = "groups")
	@Lob
	@Type(ObjectJsonMapping.class)
	@Comment("应用模板的服务(组件)分组,对应模板的group组,格式:{\"g1\":[\"mysql\",\"wordpress\"]}")
	private Map<String, List<String>> groups;

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
    
    public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public Map<String, List<String>> getGroups() {
		return groups;
	}

	public void setGroups(Map<String, List<String>> groups) {
		this.groups = groups;
	}



	public enum Field implements jef.database.Field {
        id, name, created,comments, volume,groups
    }
}
