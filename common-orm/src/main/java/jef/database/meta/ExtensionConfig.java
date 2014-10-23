package jef.database.meta;

import jef.database.Field;

public interface ExtensionConfig {
	
	String getName();

	Field getField(String key);

	

}
