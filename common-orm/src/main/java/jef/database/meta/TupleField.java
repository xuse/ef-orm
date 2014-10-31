package jef.database.meta;

import jef.database.DbUtils;
import jef.database.MetadataContainer;
import jef.database.dialect.DatabaseDialect;

@SuppressWarnings("serial")
public class TupleField implements jef.database.Field, MetadataContainer {
	private ITableMetadata meta;
	private String name;

	TupleField(ITableMetadata meta, String name) {
		this.meta = meta;
		this.name = name;
	}

	public String name() {
		return name;
	}

	public ITableMetadata getMeta() {
		return meta;
	}

	@Override
	public String toString() {
		return name;
	}

}
