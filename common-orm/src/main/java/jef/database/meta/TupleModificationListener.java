package jef.database.meta;

import jef.database.dialect.type.ColumnMapping;

public interface TupleModificationListener {

	void onDelete(DynamicMetadata tupleMetadata, ColumnMapping field);

	void onUpdate(DynamicMetadata tupleMetadata, ColumnMapping field);

}
