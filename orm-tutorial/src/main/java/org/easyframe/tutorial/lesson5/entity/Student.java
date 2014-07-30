package org.easyframe.tutorial.lesson5.entity;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;

/**
 * 学生
 * @author jiyi
 *
 */
@Entity()
public class Student extends jef.database.DataObject {

    @Id
    private int id;

    private int classId;

    private String name;

    private String gender="M";
    
    public Student(){
    }
    
    public Student(int id,int classId,String name){
    	this.id=id;
    	this.classId=classId;
    	this.name=name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public enum Field implements jef.database.Field {
        id, classId, name, gender
    }
    
    @ManyToMany
    @JoinColumn(name="classId",referencedColumnName="classId")
    private List<TeacherLesson> lessons;

	public List<TeacherLesson> getLessons() {
		return lessons;
	}

	public void setLessons(List<TeacherLesson> lessons) {
		this.lessons = lessons;
	}
}
