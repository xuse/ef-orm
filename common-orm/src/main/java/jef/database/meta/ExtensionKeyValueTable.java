package jef.database.meta;

import jef.database.annotation.DynamicKeyValueExtension;
import jef.database.query.Query;


public final class ExtensionKeyValueTable extends AbstractExtensionConfig implements ExtensionFactory{
	
	
	
	public ExtensionKeyValueTable(DynamicKeyValueExtension dkv){
		this.name=dkv.metadata();
	}

	@Override
	public ExtensionConfig valueOf(Query<?> q) {
		return this;
	}
}
