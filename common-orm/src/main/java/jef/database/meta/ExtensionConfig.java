package jef.database.meta;

import jef.database.Field;

public interface ExtensionConfig {

	String getName();

	Field getField(String key);

	TupleMetadata getMeta();

	void doPropertySet(Object entity, String property, Object value);

	Object doPropertyGet(Object entity, String property);

}
