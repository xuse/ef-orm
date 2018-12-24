package jef.database.jpa;

import java.util.Collection;

import jef.database.meta.AbstractMetadata;

public interface MetaProvider {
	public Collection<AbstractMetadata> getEntityTypes();
	
	public AbstractMetadata managedType(Class<?> type);	
}
