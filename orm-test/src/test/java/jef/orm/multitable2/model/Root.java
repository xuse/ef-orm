package jef.orm.multitable2.model;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
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
public class Root extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;
	
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	@SequenceGenerator(sequenceName="MAIN1", name = "id")
	@Column(name="ID1")
	private int id; 
	
	@Column(name="THE_NAME",columnDefinition="Char",length=40)
    private String name;
	
	@Column(name="LAST_MODIFIED",columnDefinition="timestamp default current_timestamp")
	@GeneratedValue
	private Date lastModified;
    
	private String code;
	
	@ManyToOne(targetEntity=EnumationTable.class)
	@JoinColumn(name="code",referencedColumnName="code")
	@FieldOfTargetEntity("name")
	@JoinDescription(filterCondition="type='1'")
	private String codeText;
	
	private int range;
	
    public Root(){
    }

	public Root(int id) {
		this.id = id;
	}



	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}


	@OneToMany(targetEntity=Parent.class)
    @JoinColumn(name="id",referencedColumnName="rootId")
    private List<Parent> children;

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

	public enum Field implements jef.database.Field {
        id, name, lastModified,range,code
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

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public List<Parent> getChildren() {
		return children;
	}

	public void setChildren(List<Parent> children) {
		this.children = children;
	}
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("id:").append(id).append(",");
		sb.append("name:").append(name).append(",");
		sb.append("children:").append(children).append(",");
		return sb.toString();
	}
}
