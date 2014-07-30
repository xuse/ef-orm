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
public class Parent extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;
	@Id 
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	@SequenceGenerator(sequenceName="MAIN1", name = "id")
	private int id;

    private String name;
 
    private Integer rootId;

    @ManyToOne(targetEntity=Root.class)
    @JoinColumn(name="rootId",referencedColumnName="id")
    private Root root;
    
    @OneToMany(targetEntity=Child.class)
    @JoinColumn(name="id",referencedColumnName="parentId")
    private List<Child> children;
    
    
	private String code;
	
	@ManyToOne(targetEntity=EnumationTable.class)
	@JoinColumn(name="code",referencedColumnName="code")
	@FieldOfTargetEntity("name")
	@JoinDescription(filterCondition="type='2'")
	private String codeText;
	
	
    public Root getRoot() {
		return root;
	}

	public List<Child> getChildren() {
		return children;
	}

	public void setChildren(List<Child> children) {
		this.children = children;
	}

	public void setRoot(Root root) {
		this.root = root;
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

	public Integer getRootId() {
		return rootId;
	}

	public void setRootId(Integer rootId) {
		this.rootId = rootId;
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

	public enum Field implements jef.database.Field {
        id, name, rootId, code
    }
}
