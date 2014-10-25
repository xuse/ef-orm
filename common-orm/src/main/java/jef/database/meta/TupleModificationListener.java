package jef.database.meta;

import jef.database.Field;

public interface TupleModificationListener {

	void onDelete(TupleMetadata tupleMetadata, Field field);

	void onUpdate(TupleMetadata tupleMetadata, Field field);

}
