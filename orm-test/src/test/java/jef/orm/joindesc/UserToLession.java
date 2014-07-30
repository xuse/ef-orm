package jef.orm.joindesc;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import jef.tools.DateUtils;

@Entity()
public class UserToLession extends jef.database.DataObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2187565257971890565L;
	@Id
	private int userId;
	@Id
	private int lessionId;

	private Date testTime;

	private int score;

	@ManyToOne
	@JoinColumn(name = "userId", referencedColumnName = "id")
	private Student user;

	@ManyToOne(targetEntity=Lesson.class,fetch=FetchType.LAZY)
	@JoinColumn(name = "lessionId", referencedColumnName = "id")
	private Lesson lession;

	public enum Field implements jef.database.Field {

		userId, lessionId, testTime, score
	}

	public UserToLession() {
	}

	public UserToLession(int uid, int lid, int score) {
		this.userId=uid;
		this.lessionId=lid;
		this.score=score;
		this.testTime=new Date();
	}

	public Student getUser() {
		return user;
	}

	public void setUser(Student user) {
		this.user = user;
	}

	public Lesson getLession() {
		return lession;
	}

	public void setLession(Lesson lession) {
		this.lession = lession;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getLessionId() {
		return lessionId;
	}

	public void setLessionId(int lessionId) {
		this.lessionId = lessionId;
	}

	public Date getTestTime() {
		return testTime;
	}

	public void setTestTime(Date testTime) {
		this.testTime = testTime;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}

	@Override
	public String toString() {
		return (lession==null?null:lession.getName())+":"+score+"  at "+DateUtils.formatDateTime(this.testTime);
	}
	
	
}
