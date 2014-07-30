package jef.orm.multitable.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import jef.database.DataObject;

/**
 * JEF-ORM 演示用例
 * 
 * 描述 人 <-> 课程(考试、得分)关系（一对多）
 * @author Administrator
 * @Date 2011-4-12
 */
@Entity
public class Score extends DataObject {
	private static final long serialVersionUID = 1L;
	
	@Column
	@Id
	private int pid;
	
	@Id
	@Column(length=80,nullable=false)
	private String subject;
	
	@Column
	private int score;
	
	@Column(columnDefinition="Date")
	private Date testTime;
	
	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Date getTestTime() {
		return testTime;
	}

	public void setTestTime(Date testTime) {
		this.testTime = testTime;
	}
	public String toString(){
		return pid+"-"+this.subject+"="+this.score;
	}
	/**
	 * 元模型
	 */
	public enum Field implements jef.database.Field{
		pid, subject, score, testTime
	}
}
