package org.easyframe.tutorial.lesson2.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 复合主键的类
 * @author jiyi
 *
 */
@Entity
@Table(name="STUDENT_TO_LESSON")
public class StudentToLesson extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;
	@Id
    private int lessionId;
    @Id
    private int studentId;
    private Date createTime;

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getLessionId() {
        return lessionId;
    }

    public void setLessionId(int lessionId) {
        this.lessionId = lessionId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public enum Field implements jef.database.Field {
    	studentId,lessionId, createTime
    }
}
