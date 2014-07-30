package org.easyframe.tutorial.lesson5.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import jef.database.DataObject;

/**
 * 老师和班级的关系、课程
 * @author jiyi
 *
 */
@Entity
public class TeacherLesson extends DataObject {

    @Id
    private int teacherId;

    @Id
    private int classId;
    

    private String lessonName;
    
    
    public TeacherLesson(){
    }
    
    
    public TeacherLesson(int tid,int cid,String lesson){
    	this.teacherId=tid;
    	this.classId=cid;
    	this.lessonName=lesson;
    	
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public String getLessonName() {
		return lessonName;
	}


	public void setLessonName(String lessonName) {
		this.lessonName = lessonName;
	}

	public enum Field implements jef.database.Field {
        teacherId, classId, lessonName
    }

	@Override
	public String toString() {
		return lessonName+" 教师ID:"+ teacherId;
	}
	
	
}
