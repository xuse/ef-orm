package jef.database.meta;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;

import jef.database.annotation.Cascade;

/**
 * 描述一个级联引用关系
 * @author jiyi
 *
 */
public abstract class AbstractRefField implements ISelectProvider{
	/**
	 * 引用外部表的Field名称
	 */
	protected String sourceField;

	protected Reference reference;
	protected CascadeType[] cascade;
	protected FetchType  fetch;		//延迟加载特性
	private Class<?> sourceFieldType;
	private Cascade asMap;
	
	public AbstractRefField(Class<?> container,String fName,Reference ref,Cascade asMap) {
		this.sourceField=fName;
		this.reference=ref;
		this.sourceFieldType=container;
		this.asMap=asMap;
	}
	
	public String getName() {
		return sourceField;
	}
	
	public Reference getReference() {
		return reference;
	}

	public void setReference(Reference reference) {
		this.reference = reference;
	}

	public String getSourceField() {
		return sourceField;
	}
	public String toString(){
		return sourceField;
	}
	public CascadeType[] getCascade() {
		return cascade;
	}
	public void setCascade(CascadeType[] cascade,FetchType fetch) {
		this.cascade = cascade;
		this.fetch=fetch;
	}
	
	public FetchType getFetch() {
		return fetch;
	}

	public int getProjection() {
		return 0;
	}
	
	public abstract ISelectProvider toNestedDesc(String lastName);
	
	public abstract boolean isSingleColumn();
	
	public boolean isToOne(){
		return reference.getType().isToOne();
	}
	
	public boolean isForSelect(){
		for(CascadeType cascade: this.cascade){
			if(cascade==CascadeType.ALL || cascade==CascadeType.REFRESH){
				return true;
			}
		}
		return false;
	}
	
	public Class<?> getSourceContainerType(){
		return sourceFieldType;
	}

	public Cascade getAsMap() {
		return asMap;
	}

	public void setAsMap(Cascade asMap) {
		this.asMap = asMap;
	}
}
