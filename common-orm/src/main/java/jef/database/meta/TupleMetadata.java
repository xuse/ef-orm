package jef.database.meta;

import java.util.Collection;
import java.util.Collections;

import jef.database.Field;
import jef.database.VarObject;
import jef.database.dialect.ColumnType;
import jef.database.dialect.type.ColumnMapping;

/**
 * 所有字段均为动态属性的动态表。
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
	
//	public ColumnMapping getColumnDef(Field field) {
//		return super.getcschemaMap.get(field);
//	}
	
	/**
	 * 向动态模型中放入一个字段
	 * @param field
	 * @param type
	 */
	public void putJavaField(Field field, ColumnType type) {
		boolean pk = (type instanceof ColumnType.AutoIncrement) || (type instanceof ColumnType.GUID);
		this.internalUpdateColumn(field, field.name(), type, pk, false);

	}

	/**
	 * 更新或添加一个列
	 * 
	 * @param columnName
	 * @param type
	 * @return
	 */
	public boolean updateColumn(String columnName, ColumnType type) {
		boolean pk = (type instanceof ColumnType.AutoIncrement) || (type instanceof ColumnType.GUID);
		return updateColumn(columnName, columnName, type, pk, true);
	}
	
	/**
	 * 删除指定的列
	 * 
	 * @param columnName
	 * @return false如果没找到此列
	 */
	public boolean removeColumn(String columnName) {
		if (columnName == null)
			return false;
		ColumnMapping field = lowerColumnToFieldName.get(columnName.toLowerCase());
		if (field != null) {
			removeColumnByFieldName(field.fieldName());
			return true;
		}
		return false;
	}

	@Override
	public Collection<ColumnMapping> getColumns() {
		return Collections.unmodifiableCollection(orderdColumns);
	}
}
