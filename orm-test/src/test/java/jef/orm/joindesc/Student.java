package jef.orm.joindesc;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import jef.database.annotation.JoinDescription;

@Entity()
public class Student extends jef.database.DataObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private int id;

	private String name;

	public enum Field implements jef.database.Field {
		id, name
	}

	public Student() {
	}

	public Student(String string) {
		this.name = string;
	}

	public Student(int i) {
		this.id = i;
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

	@OneToMany(targetEntity = UserToLession.class)
	@JoinColumn(name = "id", referencedColumnName = "userId")
	@JoinDescription(filterCondition="testTime > date_sub(current_timestamp , 10)" ,maxRows=10)
	@OrderBy("testTime desc")
	private List<UserToLession> toLession;
	
	@OneToMany(targetEntity = UserToLession.class)
	@JoinColumn(name = "id", referencedColumnName = "userId")
	@OrderBy("score desc")
	@JoinDescription(filterCondition="testTime > date_sub(current_timestamp , 10)" ,maxRows=1)
	private UserToLession maxScoreLession;
	
	
	public UserToLession getMaxScoreLession() {
		return maxScoreLession;
	}

	public void setMaxScoreLession(UserToLession maxScoreLession) {
		this.maxScoreLession = maxScoreLession;
	}

	public List<UserToLession> getToLession() {
		return toLession;
	}

	public void setToLession(List<UserToLession> toLession) {
		this.toLession = toLession;
	}
}
