package jef.orm.multitable.model;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import jef.database.DataObject;
import jef.database.annotation.FieldOfTargetEntity;
import jef.database.annotation.Indexed;
import jef.database.annotation.JoinDescription;
import jef.database.annotation.JoinType;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * JEF-ORM 演示用例
 * 
 * 描述 人 这一实体
 * @author Administrator
 * @Date 2011-4-12
 */
@Entity
@Table(name="person_table")
public class Person extends DataObject {
	private static final long serialVersionUID = -7665847365763812610L;
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(name)
			.append(gender)
			.append(phone)
			.append(super.hashCode())
			.toHashCode();
	}
	@Column
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	private Integer id;
	
	@Column(name="person_name",length=80,nullable=false)
//	@Indexed(definition="unique") // GBase 不支持 unique 语法
	@Indexed
	private String name;
	
	@Indexed
	@Column(columnDefinition="Char",length=1)
	private String gender;
	
	@Indexed
	@Column(length=80)
	private String homeTown;
	
	@Column(length=20)
	private String phone;
	
	@Column(length=20)
	private String cell;
	
	@Column
	private int age;
	
//	@Column // GBase 一张表只支持1个"时间戳"类型字段
	@Column(columnDefinition="Date")
	private Date birthDay;
	
	@Column(columnDefinition="TimeStamp")
	private Date lastModified;
	
	@Lob
	@Column(columnDefinition="Blob")
	private File photo;
	
	@Column
	private int schoolId;
	
	@Column
	private int parentId;
	
	/* 多表关系衍生字段 ，这些作为扩展字段，不需要在元模型中展现  */
	//一对一关系
	@OneToOne(targetEntity=Person.class)
	@FieldOfTargetEntity("name")
	@JoinColumns(value = { 
		@JoinColumn(name="parentId",referencedColumnName="id")
	})
	private String parentName;
	
	//多对一关系
	//自动计算连接方式
	@ManyToOne(targetEntity=School.class)
	@FieldOfTargetEntity("name")
	@JoinColumn(name="schoolId",referencedColumnName="id")
	@JoinDescription(type=JoinType.INNER)
	private String schoolName;
	
	//多对一关系2	
	@ManyToOne(targetEntity=School.class,cascade=CascadeType.ALL)
	@JoinColumn(name="schoolId",referencedColumnName="id")
	@JoinDescription(type=JoinType.INNER)
	private School school;
	
	//一对多关系
	@OneToMany(targetEntity=Score.class,cascade=CascadeType.ALL,fetch=FetchType.LAZY)
	@JoinColumn(name="id",referencedColumnName="pid")
	private Set<Score> scores;
	
	//多对多关系
	@ManyToMany(targetEntity=PersonFriends.class,cascade=CascadeType.ALL)
	@JoinColumn(name="id",referencedColumnName="pid")
	@JoinDescription(orderBy="friendId")
	private List<PersonFriends> friends;
	
	@ManyToMany(targetEntity=PersonFriends.class)
	@JoinColumn(name="id",referencedColumnName="pid")
	@FieldOfTargetEntity("comment")
	private String[] friendComment;
	
	
	/** 
	 * 元模型定义
	 * 从JEF 0.4 开始，JEF部分支持JPA的Annotaion定义，从而可以不在类中定义一个静态的TableMetadata字段. 
	 * 但是JPA规范确定并支持，在entity实体的相同目录下，定义一个名称加下划线的元模型类。
	 * 为了维持Entity的POJO特性。
	 * JEF认为，大多数情况下，不需要这样累赘的，重量级的元模型解决方案，仅仅通过一个枚举的定义，
	 * 即可起到JPA元模型的默认效果。
	 * 
	 * 为此，依然保留JEF现有的元模型定义
	 * */
	public enum Field implements jef.database.Field{
		id,name,gender,homeTown,phone,cell,
		age,birthDay,lastModified,photo,schoolId,parentId
	}

	public int getSchoolId() {
		return schoolId;
	}

	public void setSchoolId(int schoolId) {
		this.schoolId = schoolId;
	}

	public String getSchoolName() {
		return schoolName;
	}

	public void setSchoolName(String schoolName) {
		this.schoolName = schoolName;
	}
	public School getSchool() {
		return school;
	}
	public void setSchool(School school) {
		this.school = school;
	}


	public Person(String string) {
		this.name=string;
		this.gender="M";
		this.birthDay=new Date();
	}

	public Set<Score> getScores() {
		return scores;
	}

	public void setScores(Set<Score> scores) {
		this.scores = scores;
	}

	public Person() {
	}
	
	public List<PersonFriends> getFriends() {
		return friends;
	}

	public void setFriends(List<PersonFriends> friends) {
		this.friends = friends;
	}

	public String[] getFriendComment() {
		return friendComment;
	}

	public void setFriendComment(String[] friendComment) {
		this.friendComment = friendComment;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Date getBirthDay() {
		return birthDay;
	}

	public void setBirthDay(Date birthDay) {
		this.birthDay = birthDay;
	}

	public String getCell() {
		return cell;
	}

	public void setCell(String cell) {
		this.cell = cell;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getHomeTown() {
		return homeTown;
	}

	public void setHomeTown(String homeTown) {
		this.homeTown = homeTown;
	}

	public Integer getId() {
		return id;
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
	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	public File getPhoto() {
		return photo;
	}
	public void setPhoto(File photo) {
		this.photo = photo;
	}
	public int getParentId() {
		return parentId;
	}
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}
	public String getParentName() {
		return parentName;
	}
	public void setParentName(String parentName) {
		this.parentName = parentName;
	}
}
