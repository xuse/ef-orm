package jef.orm.multitable2.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import jef.database.annotation.FieldOfTargetEntity;
import jef.database.annotation.JoinDescription;

@Entity()
public class Leaf extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	@Id 
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	@SequenceGenerator(sequenceName="MAIN1", name = "id")
	private int id;
 
	private String name;
	
	private String code; 
	
	@ManyToOne(targetEntity=EnumationTable.class)
	@JoinColumn(name="code",referencedColumnName="code")
	@FieldOfTargetEntity("name")
	@JoinDescription(filterCondition="type='4'")
	private String codeText;

    private Integer childId;
    
    @ManyToOne()
    @JoinColumn(name="childId",referencedColumnName="id")
    private Child parent;
    
    @ManyToOne(targetEntity=Child.class)
    @FieldOfTargetEntity("name")
    @JoinColumn(name="childId",referencedColumnName="id")
    private String parentName;

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	public Child getParent() {
		return parent;
	}

	public void setParent(Child parent) {
		this.parent = parent;
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

	public Integer getChildId() {
		return childId;
	}

	public void setChildId(Integer childId) {
		this.childId = childId;
	}
	
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCodeText() {
		return codeText;
	}

	public void setCodeText(String codeText) {
		this.codeText = codeText;
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("id:").append(id).append(",");
		sb.append("name:").append(name).append(",");
		sb.append("childId:").append(childId).append(",");
		return sb.toString();
	}
	
	public enum Field implements jef.database.Field {
        id, name, childId,code
    }
}
