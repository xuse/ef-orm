package org.easyframe.tutorial.lesson2.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import  org.apache.commons.lang3.builder.ToStringBuilder;

import jef.database.annotation.UnsavedValue;

@Entity
public class Student extends jef.database.DataObject {
	/**
	 * 学号
	 */
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int id;

    /**
     * 姓名
     */
    @UnsavedValue(UnsavedValue.NullOrEmpty)
    private String name;

    /**
     * 年级
     */
    private String grade;

    /**
     * 性别 
     */
    @Column(columnDefinition="varchar",length=12)
    private String gender;
    
    /**
     * 出生日期
     */
    @Column(name="DATE_OF_BIRTH")
    private Date dateOfBirth;
    

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
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

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    
    public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}



	public enum Field implements jef.database.Field {
        id, name, grade, gender, dateOfBirth
    }

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	
}
