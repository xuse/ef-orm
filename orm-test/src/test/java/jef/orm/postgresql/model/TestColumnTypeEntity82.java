package jef.orm.postgresql.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * 用于测试由Entity生成PostgreSQL(8.2版本)数据库表对象的实体类
 * 
 * @Company Asiainfo-Linkage Technologies (China), Inc.
 * @author luolp@asiainfo-linkage.com
 * @Date 2012-7-25
 */
@Entity()
@Table(name = "test_columntypes_from_entity")
public class TestColumnTypeEntity82 extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	private int intField;

	private Integer intField2;

	private Long bigintField;

	/**
	 * 因指定precision=10, 故db中的类型是int.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(precision = 10, scale = 5)
	private long longField;

	/**
	 * 因指定precision, 故db中的类型是double.
	 */
	private float floatField;

	private Float floatField2;

	@Column(precision = 5, scale = 2)
	private Float floatField3;

	private double doubleField;

	private Double doubleField2;

	@Column(name = "field_1")
	private String field1;

	@Column(name = "field_2")
	private String field2;

	@Column
	private Date dateField;

	@Column(name = "timestampField", columnDefinition = "TimeStamp")
	private Date dateField2;

	private boolean boolField;

	private Boolean boolField2;

	private byte[] binaryField;

	/**
	 * 因标注Lob, 故db中的类型是text.
	 */
	@Lob
	private String textField;

	private List<TestColumnTypeEntity82> tt1;

	private TestColumnTypeEntity82[] tt2;

	private Map<String, TestColumnTypeEntity82> tt3;

	public int getIntField() {
		return intField;
	}

	public void setIntField(int intField) {
		this.intField = intField;
	}

	public Integer getIntField2() {
		return intField2;
	}

	public void setIntField2(Integer intField2) {
		this.intField2 = intField2;
	}

	public Long getBigintField() {
		return bigintField;
	}

	public void setBigintField(Long bigintField) {
		this.bigintField = bigintField;
	}

	public long getLongField() {
		return longField;
	}

	public void setLongField(long longField) {
		this.longField = longField;
	}

	public float getFloatField() {
		return floatField;
	}

	public void setFloatField(float floatField) {
		this.floatField = floatField;
	}

	public Float getFloatField2() {
		return floatField2;
	}

	public void setFloatField2(Float floatField2) {
		this.floatField2 = floatField2;
	}

	public Float getFloatField3() {
		return floatField3;
	}

	public void setFloatField3(Float floatField3) {
		this.floatField3 = floatField3;
	}

	public double getDoubleField() {
		return doubleField;
	}

	public void setDoubleField(double doubleField) {
		this.doubleField = doubleField;
	}

	public Double getDoubleField2() {
		return doubleField2;
	}

	public void setDoubleField2(Double doubleField2) {
		this.doubleField2 = doubleField2;
	}

	public String getField1() {
		return field1;
	}

	public void setField1(String field1) {
		this.field1 = field1;
	}

	public String getField2() {
		return field2;
	}

	public void setField2(String field2) {
		this.field2 = field2;
	}

	public Date getDateField2() {
		return dateField2;
	}

	public void setDateField2(Date dateField2) {
		this.dateField2 = dateField2;
	}

	public Date getDateField() {
		return dateField;
	}

	public void setDateField(Date dateField) {
		this.dateField = dateField;
	}

	public byte[] getBinaryField() {
		return binaryField;
	}

	public void setBinaryField(byte[] binaryField) {
		this.binaryField = binaryField;
	}

	public boolean isBoolField() {
		return boolField;
	}

	public void setBoolField(boolean boolField) {
		this.boolField = boolField;
	}

	public Boolean getBoolField2() {
		return boolField2;
	}

	public void setBoolField2(Boolean boolField2) {
		this.boolField2 = boolField2;
	}

	public String getTextField() {
		return textField;
	}

	public void setTextField(String textField) {
		this.textField = textField;
	}

	public List<TestColumnTypeEntity82> getTt1() {
		return tt1;
	}

	public void setTt1(List<TestColumnTypeEntity82> tt1) {
		this.tt1 = tt1;
	}

	public TestColumnTypeEntity82[] getTt2() {
		return tt2;
	}

	public void setTt2(TestColumnTypeEntity82[] tt2) {
		this.tt2 = tt2;
	}

	public Map<String, TestColumnTypeEntity82> getTt3() {
		return tt3;
	}

	public void setTt3(Map<String, TestColumnTypeEntity82> tt3) {
		this.tt3 = tt3;
	}

	public enum Field implements jef.database.Field {
		intField, intField2, bigintField, longField, floatField, floatField2, floatField3, doubleField, doubleField2, field1, field2, dateField, dateField2, boolField, boolField2, binaryField, textField
	}
}
