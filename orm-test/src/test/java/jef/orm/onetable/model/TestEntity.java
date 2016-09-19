package jef.orm.onetable.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import jef.database.annotation.EasyEntity;

@EasyEntity
@Table(name="test_entity")
public class TestEntity extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	@Column(name="field_1")
	private String field1;

	@Column(name="field_2")
    private String field2;

	@Column(name="dateField",columnDefinition="TimeStamp")
    private Date dateField;
	
	@Column(name="create_time",columnDefinition="TimeStamp")
	@GeneratedValue(generator="created")
    private Date createTime;

	@Lob
    private byte[] binaryData;

    
    private boolean boolField;

    private Boolean boolField2;

	@Column(name="int_field_1",columnDefinition="integer")
    private int intFiled;

	@Column(name="int_field_2",columnDefinition="integer")
    private Integer intField2;

    @Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
    @Column(precision=10,scale=5)
    private long longField;

    private Long longField2;

    private double doubleField;

    private Double doubleField2;

    private float floatField;

    private Float folatField2;

    private List<TestEntity> tt1;
    
    private TestEntity[] tt2;
    
    private Map<String,TestEntity> tt3;
    
    public List<TestEntity> getTt1() {
		return tt1;
	}

	public void setTt1(List<TestEntity> tt1) {
		this.tt1 = tt1;
	}

	public TestEntity[] getTt2() {
		return tt2;
	}

	public void setTt2(TestEntity[] tt2) {
		this.tt2 = tt2;
	}

	public Map<String, TestEntity> getTt3() {
		return tt3;
	}

	public void setTt3(Map<String, TestEntity> tt3) {
		this.tt3 = tt3;
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

    public Date getDateField() {
        return dateField;
    }

    public void setDateField(Date dateField) {
        this.dateField = dateField;
    }

    public byte[] getBinaryData() {
        return binaryData;
    }

    public void setBinaryData(byte[] binaryDate) {
        this.binaryData = binaryDate;
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

    public int getIntFiled() {
        return intFiled;
    }

    public void setIntFiled(int intFiled) {
        this.intFiled = intFiled;
    }

    public Integer getIntField2() {
        return intField2;
    }

    public void setIntField2(Integer intField2) {
        this.intField2 = intField2;
    }

    public long getLongField() {
        return longField;
    }

    public void setLongField(long longField) {
        this.longField = longField;
    }

    public Long getLongField2() {
        return longField2;
    }

    public void setLongField2(Long longField2) {
        this.longField2 = longField2;
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

    public float getFloatField() {
        return floatField;
    }

    public void setFloatField(float floatField) {
        this.floatField = floatField;
    }

    public Float getFolatField2() {
        return folatField2;
    }

    public void setFolatField2(Float folatField2) {
        this.folatField2 = folatField2;
    }

    public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}



	public enum Field implements jef.database.Field {

        field1, field2, dateField, binaryData, boolField, boolField2, intFiled, intField2, longField, longField2, doubleField, doubleField2, floatField, folatField2,createTime
    }
}
