package jef.orm.multitable2.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import jef.database.annotation.FieldOfTargetEntity;
import jef.database.annotation.JoinDescription;

@Entity()
public class Child extends jef.database.DataObject {
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
	@JoinDescription(filterCondition="type='1'")
    private String codeText;

	@ManyToOne(targetEntity=EnumationTable.class)
	@JoinColumn(name="code",referencedColumnName="code")
	@JoinDescription(filterCondition="type='1'")
	private EnumationTable codeObj;
	
	private Integer parentId;
    
    
    
    @OneToMany()
    @JoinColumn(name="id",referencedColumnName="childId")
    private List<Leaf> children;
    
    @ManyToOne
    @JoinColumn(name="parentId",referencedColumnName="id")
    private Parent parent;

    public List<Leaf> getChildren() {
		return children;
	}

	public void setChildren(List<Leaf> children) {
		this.children = children;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
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

	public Integer getParentId() {
		return parentId;
	}

	public void setParentId(Integer parentId) {
		this.parentId = parentId;
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

	public EnumationTable getCodeObj() {
		return codeObj;
	}

	public void setCodeObj(EnumationTable codeObj) {
		this.codeObj = codeObj;
	}


	public enum Field implements jef.database.Field {
        id, name, parentId,code
    }
}
