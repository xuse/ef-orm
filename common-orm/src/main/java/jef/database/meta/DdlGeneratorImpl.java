package jef.database.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jef.common.SimpleSet;
import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.support.RDBMS;
import jef.tools.StringUtils;
import jef.tools.string.RandomData;

public class DdlGeneratorImpl implements DdlGenerator {
	private DatabaseDialect profile;
	private final String BRUKETS_LEFT;
	private final String BRUKETS_RIGHT;
	private boolean commandForEach;

	public DdlGeneratorImpl(DatabaseDialect profile) {
		this.profile = profile;
		commandForEach = profile.has(Feature.ALTER_FOR_EACH_COLUMN);
		if (profile.has(Feature.BRUKETS_FOR_ALTER_TABLE)) {
			BRUKETS_LEFT = " (";
			BRUKETS_RIGHT = ")";
		} else {
			BRUKETS_LEFT = " ";
			BRUKETS_RIGHT = "";
		}
	}

	/*
	 * 生成建表语句 (non-Javadoc)
	 * 
	 * @see
	 * jef.database.SqlProcessor#toTableCreateClause(jef.database.DataObject,
	 * java.lang.String)
	 */
	public TableCreateStatement toTableCreateClause(ITableMetadata meta, String tablename) {
		TableCreateStatement result=new TableCreateStatement();
		result.addTableMeta(tablename, meta, profile);
		return result;
	}

	/**
	 * 转换成索引创建语句
	 */
	public List<String> toIndexClause(ITableMetadata meta, String tablename) {
		List<String> sqls = new ArrayList<String>();
		Set<String> indexNames = new HashSet<String>();
		if (tablename.indexOf('.') > -1) {
			tablename = StringUtils.substringAfter(tablename, ".");
		}

		for (jef.database.annotation.Index index : meta.getIndexDefinition()) {
			StringBuilder iNameBuilder = new StringBuilder();
			StringBuilder fieldsb = new StringBuilder();
			iNameBuilder.append("IDX_").append(StringUtils.truncate(StringUtils.removeChars(tablename, '_'), 14)).append("_");
			int maxField = ((28 - iNameBuilder.length()) / index.fields().length) - 1;
			if (maxField < 1)
				maxField = 1;
			for (String fieldname : index.fields()) {
				if (fieldsb.length() > 0) {
					fieldsb.append(",");
					iNameBuilder.append("_");
				}
				Field field = meta.getField(fieldname);
				if (field == null)
					field = new FBIField(fieldname);
				String column = meta.getColumnDef(field).getColumnName(profile, false);
				if (field instanceof FBIField) {
					String fname = StringUtils.truncate(StringUtils.randomString(), maxField);
					iNameBuilder.append(fname);
				} else {
					String fname = StringUtils.truncate(column, maxField);
					iNameBuilder.append(fname);
				}
				fieldsb.append(column);
			}
			String indexName = index.name().length() == 0 ? iNameBuilder.toString() : index.name();
			String escapedName = indexName;

			while (indexNames.contains(escapedName)) {
				escapedName = indexName + RandomData.randomInteger(0, 10);
			}
			indexNames.add(escapedName);
			if (indexName.length() > 30)
				indexName = indexName.substring(0, 30);
			StringBuilder sb = new StringBuilder("create ");
			if (index.definition().length() > 0) {
				sb.append(index.definition()).append(' ');
			}
			sb.append("index ");
			sb.append(indexName).append(" on ");
			sb.append(tablename).append("(").append(fieldsb.toString()).append(")");
			sb.append(profile.getProperty(DbProperty.INDEX_USING_HASH, ""));
			sqls.add(sb.toString());
		}
		return sqls;
	}

	public List<String> toViewCreateClause() {
		return null;
	}

	// ALTER [ COLUMN ] column TYPE type [ USING expression ]
	// ALTER [ COLUMN ] column SET DEFAULT expression
	// ALTER [ COLUMN ] column DROP DEFAULT
	// ALTER [ COLUMN ] column { SET | DROP } NOT NULL

	public List<String> toTableModifyClause(ITableMetadata meta, String tableName, Map<String, ColumnType> insert, List<ColumnModification> changed, List<String> delete) {
		List<String> sqls = new ArrayList<String>();
		if (!insert.isEmpty()) {
			if (profile.has(Feature.ONE_COLUMN_IN_SINGLE_DDL)) {// 某些数据库一次ALTER
																// TABLE语句只能修改一列
				for (Entry<String, ColumnType> entry : insert.entrySet()) {
					Set<Entry<String, ColumnType>> ss = new SimpleSet<Entry<String, ColumnType>>();
					ss.add(entry);
					sqls.add(toAddColumnSql(tableName, ss));
				}
			} else {
				sqls.add(toAddColumnSql(tableName, insert.entrySet()));
			}

		}

		if (!changed.isEmpty()) {
			boolean complexSyntax = profile.has(Feature.COLUMN_ALTERATION_SYNTAX);
			if (profile.has(Feature.ONE_COLUMN_IN_SINGLE_DDL) || complexSyntax) {// 某些数据库一次ALTER
																					// TABLE语句只能修改一列
				for (ColumnModification entry : changed) {
					if (complexSyntax) {// complex operate here
						for (ColumnChange change : entry.getChanges()) {// 要针对每种Change单独实现SQL语句,目前已知Derby和postgresql是这样的，而且两者的语法有少量差别，这里尽量用兼容写法
							sqls.add(toChangeColumnSql(tableName, entry.getFrom().getColumnName(), change, profile));
						}
					} else {
						// 简单语法时
						sqls.add(toChangeColumnSql(tableName, Arrays.asList(entry)));
					}
				}
			} else {
				sqls.add(toChangeColumnSql(tableName, changed));
			}
		}

		if (!delete.isEmpty()) {
			if (profile.has(Feature.ONE_COLUMN_IN_SINGLE_DDL)) {// 某些数据库一次ALTER
																// TABLE语句只能修改一列
				for (String entry : delete) {
					sqls.add(toDropColumnSql(tableName, Arrays.asList(entry)));
				}
			} else {
				sqls.add(toDropColumnSql(tableName, delete));
			}

		}
		return sqls;
	}

	/*
	 * DERBY has much complexity than Oracle in modifying table columns.
	 * 
	 * key words must column-Name SET DATA TYPE VARCHAR(integer) | column-Name
	 * SET DATA TYPE VARCHAR FOR BIT DATA(integer) | column-name SET INCREMENT
	 * BY integer-constant | column-name RESTART WITH integer-constant |
	 * column-name [ NOT ] NULL | column-name [ WITH | SET ] DEFAULT
	 * default-value | column-name DROP DEFAULT
	 */
	private String toChangeColumnSql(String tableName, String columnName, ColumnChange change, DatabaseDialect profile) {
		StringBuilder sb = new StringBuilder();
		sb.ensureCapacity(128);
		sb.append("ALTER TABLE ");
		sb.append(tableName).append(' ').append(profile.getProperty(DbProperty.MODIFY_COLUMN)).append(' ');
		sb.append(columnName).append(' ');
		String setDataType;
		String setNull;
		String setNotNull;
		if (RDBMS.postgresql == profile.getName()) {// PG
			setDataType = "TYPE";
			setNull = "DROP NOT NULL";
			setNotNull = "SET NOT NULL";
		} else if (RDBMS.derby == profile.getName()) {// DERBY
			setDataType = "SET DATA TYPE";
			setNull = "NULL";
			setNotNull = "NOT NULL";
		} else {// HSQLDB
			setDataType = "";
			setNull = "SET NULL";
			setNotNull = "SET NOT NULL";
		}
		switch (change.getType()) {
		case CHG_DATATYPE:
			sb.append(setDataType).append(' ').append(change.getTo());
			return sb.toString();
		case CHG_DEFAULT:
			sb.append("SET DEFAULT ").append(change.getTo());
			return sb.toString();
		case CHG_DROP_DEFAULT:
			sb.append("DROP DEFAULT");
			return sb.toString();
		case CHG_TO_NOT_NULL:
			sb.append(setNotNull);
			return sb.toString();
		case CHG_TO_NULL:
			sb.append(setNull);
			return sb.toString();
		default:
			throw new IllegalStateException("Unknown change type" + change.getType());
		}
	}

	private String toChangeColumnSql(String tableName, List<ColumnModification> entrySet) {
		StringBuilder sb = new StringBuilder();
		sb.ensureCapacity(128);
		sb.append("ALTER TABLE ");
		sb.append(tableName).append(' ').append(profile.getProperty(DbProperty.MODIFY_COLUMN)).append(BRUKETS_LEFT);
		int n = 0;
		for (ColumnModification entry : entrySet) {
			if (n > 0) {
				sb.append(",\n");
				if (commandForEach) {
					sb.append(profile.getProperty(DbProperty.MODIFY_COLUMN)).append(' ');
				}
			}
			sb.append(entry.getFrom().getColumnName()).append(' ');
			sb.append(profile.getCreationComment(entry.getNewColumn(), true));
			n++;
		}
		sb.append(BRUKETS_RIGHT);
		return sb.toString();
	}

	private String toDropColumnSql(String tableName, List<String> entrySet) {
		StringBuilder sb = new StringBuilder();
		sb.ensureCapacity(128);
		sb.append("ALTER TABLE ");
		sb.append(tableName).append(' ').append(profile.getProperty(DbProperty.DROP_COLUMN)).append(BRUKETS_LEFT);
		int n = 0;
		for (String entry : entrySet) {
			if (n > 0) {
				sb.append(",\n");
				if (commandForEach) {
					sb.append(profile.getProperty(DbProperty.DROP_COLUMN)).append(' ');
				}
			}
			sb.append(entry);
			n++;
		}
		sb.append(BRUKETS_RIGHT);
		return sb.toString();
	}

	private String toAddColumnSql(String tableName, Set<Entry<String, ColumnType>> entrySet) {
		StringBuilder sb = new StringBuilder();
		sb.ensureCapacity(128);
		sb.append("ALTER TABLE ");
		sb.append(tableName).append(' ').append(profile.getProperty(DbProperty.ADD_COLUMN)).append(BRUKETS_LEFT);
		int n = 0;
		for (Entry<String, ColumnType> entry : entrySet) {
			if (n > 0) {
				sb.append(", ");
				if (commandForEach) {
					sb.append(profile.getProperty(DbProperty.ADD_COLUMN)).append(' ');
				}
			}
			sb.append(entry.getKey()).append(' ');
			sb.append(profile.getCreationComment(entry.getValue(), true));
			n++;
		}
		sb.append(BRUKETS_RIGHT);
		return sb.toString();
	}
}
