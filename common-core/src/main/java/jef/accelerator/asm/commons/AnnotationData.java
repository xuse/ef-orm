package jef.accelerator.asm.commons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import  org.apache.commons.lang3.builder.ToStringBuilder;
import  org.apache.commons.lang3.builder.ToStringStyle;

import com.github.geequery.asm.TypePath;

public class AnnotationData {
	private final String classname;
	private final boolean visible;
	private final Map<String, Object> properties = new HashMap<String, Object>();

	public AnnotationData(String desc, boolean visible) {
		this.classname = toClass(desc);
		this.visible = visible;
	}

	public String getClassname() {
		return classname;
	}

	private String toClass(String desc) {
		return desc.substring(1, desc.length() - 1).replace('/', '.');
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public boolean isVisible() {
		return visible;
	}

	@Override
	public String toString() {
		return "@" + classname + properties;
	}

	public void put(String name, List<Object> value) {
		properties.put(name, value);
	}

	public void put(String name, EnumDef value) {
		properties.put(name, value);

	}

	public void put(String name, Object value) {
		properties.put(name, value);
	}

}
