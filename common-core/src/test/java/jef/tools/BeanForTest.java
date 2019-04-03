package jef.tools;

import java.util.Date;

import jef.tools.string.RandomValue;
import jef.tools.string.ValueType;

public class BeanForTest {
	private int id;
	
	@RandomValue(ValueType.NAME)
	private String name;
	
	private Date birthDay;
	
	@RandomValue(numberMax=80)
	private int age;
	private Date date;
	@RandomValue(ignore=true)
	private byte[] array;
	
	@RandomValue(ValueType.EMAIL)
	private String email;
	
	@RandomValue(value=ValueType.NUMBER,numberMin=100000,numberMax=999999999)
	private String qq;
	
	@RandomValue(ValueType.PHONE)
	private String phone;
	
	private BeanForTest person;

	private String comment;
	private String alias;
	private boolean flag; 
	private float weight;
	private float height;
	
	@RandomValue(value=ValueType.OPTIONS,options= {"http://baidu.com","http://google.com","http://sogou.com"})
	private String fest;
	
	@RandomValue(count=3,length=4)
	private String[] relations;
	
	public BeanForTest getPerson() {
		return person;
	}

	public void setPerson(BeanForTest person) {
		this.person = person;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String[] getRelations() {
		return relations;
	}

	public void setRelations(String[] relations) {
		this.relations = relations;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getQq() {
		return qq;
	}

	public void setQq(String qq) {
		this.qq = qq;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}



	private int cSessionId;
	private int i;
	private int iC;
	private boolean iB;
	private boolean isBoolean;
	private boolean iSBoolean;
	
	
	public boolean isiSBoolean() {
		return iSBoolean;
	}

	public void setiSBoolean(boolean iSBoolean) {
		this.iSBoolean = iSBoolean;
	}

	public boolean isBoolean() {
		return isBoolean;
	}

	public void setBoolean(boolean isBoolean) {
		this.isBoolean = isBoolean;
	}

	public boolean isiB() {
		return iB;
	}

	public void setiB(boolean iB) {
		this.iB = iB;
	}

	public int getiC() {
		return iC;
	}

	public void setiC(int iC) {
		this.iC = iC;
	}

	public int getI() {
		return i;
	}

	public void setI(int i) {
		this.i = i;
	}

	public int getcSessionId() {
		return cSessionId;
	}

	public void setcSessionId(int cSessionId) {
		this.cSessionId = cSessionId;
	}

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	public float getHeight() {
		return height;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public String getFest() {
		return fest;
	}

	public void setFest(String fest) {
		this.fest = fest;
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

	public Date getBirthDay() {
		return birthDay;
	}

	public void setBirthDay(Date birthDay) {
		this.birthDay = birthDay;
	}


	public byte[] getArray() {
		return array;
	}

	public void setArray(byte[] array) {
		this.array = array;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}
