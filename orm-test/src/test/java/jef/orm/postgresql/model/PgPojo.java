package jef.orm.postgresql.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity()
public class PgPojo extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	private int id;

	private int age;
	
	@Column(columnDefinition="numeric",precision=16,scale=12)
	private float test1;

	@Column(columnDefinition="number",precision=15,scale=9)
	private double test2;

	private String name;

	private Date dob;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public float getTest1() {
		return test1;
	}

	public void setTest1(float test1) {
		this.test1 = test1;
	}

	public double getTest2() {
		return test2;
	}

	public void setTest2(double test2) {
		this.test2 = test2;
	}

	public enum Field implements jef.database.Field {
		id, age, name, dob, test1, test2;
	}
}
