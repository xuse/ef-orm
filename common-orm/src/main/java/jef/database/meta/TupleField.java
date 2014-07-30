package jef.database.meta;

import jef.database.dialect.DatabaseDialect;


@SuppressWarnings("serial")
public class TupleField implements jef.database.Field{
	private ITableMetadata meta;
	private String name;
	TupleField(ITableMetadata meta,String name){
		this.meta=meta;
		this.name=name;
	}
	public String name() {
		return name;
	}
	public ITableMetadata getMeta() {
		return meta;
	}
	public String toColumnName(String tableAlias, DatabaseDialect feature) {
		return meta.getColumnName(this, tableAlias,feature);
	}
	@Override
	public String toString() {
		return name;
	}
	
}
