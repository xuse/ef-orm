package jef.database.meta;

import jef.database.Field;
import jef.database.VarObject;
import jef.database.dialect.type.ColumnMapping;

/**
 * 支持动态表。
 * 
 * @author jiyi
 * 
 */
public final class TupleMetadata extends DynamicMetadata {
	/**
	 * 构造
	 * 
	 * @param tableName
	 *            表名
	 */
	public TupleMetadata(String tableName) {
		super(tableName);
	}

	/**
	 * 构造
	 * 
	 * @param schema
	 *            schema名
	 * @param tableName
	 *            表名
	 */
	public TupleMetadata(String schema, String tableName) {
		super(schema, tableName);
	}

	public VarObject newInstance() {
		return new VarObject(this);
	}

	public VarObject instance() {
		return new VarObject(this, false);
	}
	
	public ColumnMapping<?> getColumnDef(Field field) {
		return schemaMap.get(field);
	}
}
