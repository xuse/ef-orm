package org.easyframe.tutorial.lesson4.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import jef.database.DataObject;
import jef.database.annotation.FieldOfTargetEntity;
import jef.database.annotation.Indexed;
import jef.database.annotation.JoinDescription;

@Entity
@Table(name = "t_person")
public class Person extends DataObject {
	private static final long serialVersionUID = -7665847365763812610L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column
	private Integer id;

	@Column(name = "person_name", length = 20, nullable = false)
	@Indexed(unique = true)
	private String name;

	@Column(name = "current_school_id", columnDefinition = "integer")
	private int currentSchoolId;
	/**
	 * 性别,男为M 女为F
	 */
	@Column(name = "gender", columnDefinition = "char(1)", length = 1)
	private Character gender;

	/**
	 * 学校映射
	 */
	@ManyToOne(targetEntity = School.class)
	@JoinColumn(name = "currentSchoolId", referencedColumnName = "id")
	private School currentSchool;
  
	/**
	 * 性别的显示名称“男”“女”
	 */
	// @ManyToOne(targetEntity=DataDict.class)
	// @JoinColumn(name="gender",referencedColumnName="value")
	// @JoinDescription(filterCondition="type='USER.GENDER'") //在引用时还要增加过滤条件
	// @FieldOfTargetEntity("text")
	// private String genderName;

	@Column(name = "dt")
	private String dictType;

	/**
	 * 性别的显示名称“男”“女”
	 */
	@ManyToOne(targetEntity = DataDict.class)
	@JoinColumns(@JoinColumn(name = "gender", referencedColumnName = "value"))
	//此处用了一个特殊的用法，即关联关系不是静态的，而是取决于一个和当前表字段有关的表达式的，这种用法要求这个字段必须是
	//一对一或多对一的，并且是在两表外连接时才能正常使用的查询。
	//为了方式全局参数对这一关联关系产生影响，必须使用setCascadeViaOuterJoin(true)才能确保这一点
	@JoinDescription(filterCondition="this$dictType+'.GENDER'=that$type") 
	@FieldOfTargetEntity("text")
	public String genderName;

	//是否启用缓存开关。。。
	
	/**
	 * 创建时间
	 */
	@GeneratedValue(generator = "created")
	private Date created;

	public enum Field implements jef.database.Field {
		id, name, currentSchoolId, gender, created, dictType
	}

	public Person() {
	}

	public Person(int id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public String getDictType() {
		return dictType;
	}

	public void setDictType(String dictType) {
		this.dictType = dictType;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCurrentSchoolId() {
		return currentSchoolId;
	}

	public void setCurrentSchoolId(int currentSchoolId) {
		this.currentSchoolId = currentSchoolId;
	}

	public School getCurrentSchool() {
		return currentSchool;
	}
	public void setCurrentSchool(School currentSchool) {
		this.currentSchool = currentSchool;
	}

	public Character getGender() {
		return gender;
	}

	public void setGender(Character gender) {
		this.gender = gender;
	}

	public String getGenderName() {
		return genderName;
	}

	public void setGenderName(String genderName) {
		this.genderName = genderName;
	}

	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
 
	@Override
	public String toString() {
		return id + " :" + name + " " + gender;
	}
}