package jef.database.support;

public class VarAttribute {
	public static final String ATTR_TABLE_NAME="_TABLE_NAME";
	public static final String ATTR_CLASS_NAME="_CLASS_NAME";
	
	private String name;
	private String value;
	private String dataType;
	public VarAttribute() {
	}
	public VarAttribute(String name, Object value) {
		this.name=name;
		this.value=String.valueOf(value);
		this.dataType=value.getClass().getName();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getDataType() {
		return dataType;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
}
