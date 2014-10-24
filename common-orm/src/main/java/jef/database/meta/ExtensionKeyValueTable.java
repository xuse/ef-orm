package jef.database.meta;

import jef.database.annotation.DynamicKeyValueExtension;
import jef.database.query.Query;


public final class ExtensionKeyValueTable extends AbstractExtensionConfig implements ExtensionConfigFactory{
	public ExtensionKeyValueTable(DynamicKeyValueExtension dkv,Class<?> entityClass){
		this.name=dkv.metadata();
	}

	

	@Override
	public ExtensionConfig valueOf(Query<?> q) {
		return this;
	}
	@Override
	public ExtensionConfig valueOf(String value) {
		return this;
	}



	@Override
	public void doPropertySet(Object entity, String property, Object value) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public Object doPropertyGet(Object entity, String property) {
		// TODO Auto-generated method stub
		return null;
	}
}
