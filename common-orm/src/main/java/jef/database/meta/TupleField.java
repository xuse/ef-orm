package jef.database.meta;

import jef.database.MetadataContainer;

@SuppressWarnings("serial")
public class TupleField implements jef.database.Field, MetadataContainer {
	private ITableMetadata meta;
	private String name;
	private int ordinal;

	TupleField(ITableMetadata meta, String name, int ordinal) {
		this.meta = meta;
		this.name = name;
		this.ordinal = ordinal;
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

	@Override
	public int asEnumOrdinal() {
		return ordinal;
	}

	public int getOrdinal() {
		return ordinal;
	}

}
