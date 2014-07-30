package jef.orm.joindesc;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

@Entity()
public class Lesson extends jef.database.DataObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int level;

	private String name;
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private int id;

	public enum Field implements jef.database.Field {

		level, name, id
	}
	
	@OneToMany(targetEntity=UserToLession.class)
	@JoinColumn(name="id",referencedColumnName="lessionId")
	private List<UserToLession> tests;

	public Lesson() {
	}

	public Lesson(String string, int level) {
		this.name = string;
		this.level = level;
	}

	public Lesson(String string) {
		this.name = string;
	}

	public int getLevel() {
		return level;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getId() {
		return id;
	}

	public List<UserToLession> getTests() {
		return tests;
	}

	public void setTests(List<UserToLession> tests) {
		this.tests = tests;
	}

	public void setId(int id) {
		this.id = id;
	}
}
