package jef.database.meta;

import jef.database.Field;

public interface TupleModificationListener {

	void onDelete(DynamicMetadata tupleMetadata, Field field);

	void onUpdate(DynamicMetadata tupleMetadata, Field field);

}
