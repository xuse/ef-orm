package org.easyframe.tutorial.lesson2.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import jef.database.DataObject;

/**
 * 课程的信息
 * 
 * @author jiyi
 * 
 */
@Entity
@Table(name="LESSON_INFO")
public class LessonInfo extends DataObject {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue
    private int id;
    
    /**
     * 课时
     */
    private int period;

    /**
     * 名称
     */
    private String name;

    /**
     * 年级
     */
    private String grade;

    /**
     * 课程详情
     */
    private String descrption;

    /**
     * 课程时间
     */
    private Date startDate;

    /**
     * 任课教师
     */
    private String teacher;

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

    public String getDescrption() {
        return descrption;
    }

    public void setDescrption(String descrption) {
        this.descrption = descrption;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public enum Field implements jef.database.Field {
        id, name, grade, descrption, startDate, teacher
    }
}
