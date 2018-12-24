package jef.accelerator.asm.commons;

public class EnumDef {
	private String desc;
	private String value;
	
	public EnumDef(String desc, String value) {
		this.desc=desc;
		this.value=value;
	}
	
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}	
