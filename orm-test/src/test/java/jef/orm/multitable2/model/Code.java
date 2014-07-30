package jef.orm.multitable2.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import jef.database.annotation.FieldOfTargetEntity;

@Entity()
public class Code extends jef.database.DataObject {

 
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	@SequenceGenerator(sequenceName="MAIN1", name = "id")
	private int id;
	
	private String code1;
	
	private String code2;

    private String name;

    private Integer parentId;
     
    @ManyToOne(targetEntity=EnumationTable.class)
    @JoinColumn(name="code1",referencedColumnName="code")
    @FieldOfTargetEntity("name")
    private String textValue1;
    
    @ManyToOne(targetEntity=EnumationTable.class)
    @JoinColumn(name="code2",referencedColumnName="code")
    @FieldOfTargetEntity("name")
    private String textValue2;

	public String getCode1() {
		return code1;
	}

	public void setCode1(String code1) {
		this.code1 = code1;
	}

	public String getCode2() {
		return code2;
	}

	public void setCode2(String code2) {
		this.code2 = code2;
	}
	
	public String getTextValue1() {
		return textValue1;
	}

	public void setTextValue1(String textValue1) {
		this.textValue1 = textValue1;
	}

	public String getTextValue2() {
		return textValue2;
	}

	public void setTextValue2(String textValue2) {
		this.textValue2 = textValue2;
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

	public Integer getParentId() {
		return parentId;
	}

	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}

	public enum Field implements jef.database.Field {
        id, name, parentId,code1,code2
    }
}
