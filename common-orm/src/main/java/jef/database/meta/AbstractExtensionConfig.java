package jef.database.meta;

import java.lang.reflect.Method;

import jef.database.Field;

public abstract class AbstractExtensionConfig implements ExtensionConfig{
	
	protected String name;
	private Method getter;
	private Method setter;
	
	@Override
	public Field getField(String key) {
		TupleMetadata tuple=MetaHolder.getDynamicMeta(name);
		return tuple.getField(key);
	}
	
	@Override
	public String getName() {
		return name;
	}
	

}
