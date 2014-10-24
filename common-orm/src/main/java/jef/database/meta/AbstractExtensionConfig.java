package jef.database.meta;

import jef.database.Field;

public abstract class AbstractExtensionConfig implements ExtensionConfig{
	protected String name;
	private TupleMetadata tuple;
	
	public TupleMetadata getMeta(){
		if(tuple==null){
			tuple=MetaHolder.getDynamicMeta(name);
			if(tuple==null){
				throw new IllegalArgumentException(); 
			}
		}
		return tuple;
	}
	

	@Override
	public Field getField(String key) {
		return getMeta().getField(key);
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public boolean isProperty(String str){
		return getMeta().getField(str)!=null;
	}
	
	
}
