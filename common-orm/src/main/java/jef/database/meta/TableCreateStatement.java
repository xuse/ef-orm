package jef.database.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jef.common.PairIS;
import jef.database.dialect.ColumnType;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.ColumnType.Varchar;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.ColumnMapping;
import jef.tools.StringUtils;

/**
 * 建表任务操作
 * 
 * @author jiyi
 * 
 */
public class TableCreateStatement {
	/**
	 * 表定义
	 */
	private final List<TableDef> tables = new ArrayList<TableDef>();

	public void addTableMeta(String tablename, ITableMetadata meta, DatabaseDialect profile) {
		TableDef tableDef = new TableDef();
		tableDef.tablename = tablename;
		for (ColumnMapping<?> column : meta.getColumns()) {
			processField(column, tableDef, profile);
		}
		if (!tableDef.NoPkConstraint && !meta.getPKFields().isEmpty()) {
			tableDef.addPkConstraint(meta.getPKFields(), profile);
		}
		this.tables.add(tableDef);
	}

	private void processField(ColumnMapping<?> entry, TableDef result, DatabaseDialect profile) {
		StringBuilder sb = result.getColumnDef();
		if (sb.length() > 0)
			sb.append(",\n");

		sb.append("    ").append(entry.getColumnName(profile, true)).append(" ");
		ColumnType vType = entry.get();
		if (entry.isPk()) {
			vType.setNullable(false);
			if (vType instanceof Varchar) {
				Varchar vcType = (Varchar) vType;
				int check = profile.getPropertyInt(DbProperty.INDEX_LENGTH_LIMIT);
				if (check > 0 && vcType.getLength() > check) {
					throw new IllegalArgumentException("The varchar column in " + profile.getName() + " will not be indexed if length is >" + check);
				}
				check = profile.getPropertyInt(DbProperty.INDEX_LENGTH_LIMIT_FIX);
				if (check > 0 && vcType.getLength() > check) {
					result.charSetFix = profile.getProperty(DbProperty.INDEX_LENGTH_CHARESET_FIX);
				}
			}
		}
		if (entry instanceof AutoIncrementMapping) {
			if (profile.has(Feature.AUTOINCREMENT_NEED_SEQUENCE)) {
				int precision = ((AutoIncrement) vType).getPrecision();
				addSequence(((AutoIncrementMapping<?>) entry).getSequenceName(profile), precision);

			}
			if (profile.has(Feature.AUTOINCREMENT_MUSTBE_PK)) { // 在一些数据库上，只有主键才能自增，并且此时不能再单独设置主键.
				result.NoPkConstraint = true;
			}
		}
		if (entry.getMeta().getEffectPartitionKeys() != null) { // 如果是分表的，自增键退化为常规字段
			if (vType instanceof AutoIncrement) {
				vType = ((AutoIncrement) vType).toNormalType();
			}
		}
		sb.append(profile.getCreationComment(vType, true));
	}

	private void addSequence(String seq, int precision) {
		sequences.add(new PairIS(precision, seq));
	}
	
	/**
	 * 要创建的Sequence
	 */
	private final List<PairIS> sequences = new ArrayList<PairIS>();

	static class TableDef {
		private String tablename;
		/**
		 * MySQL专用。字符集编码
		 */
		private String charSetFix;
		/**
		 * 列定义
		 */
		private final StringBuilder columnDefinition = new StringBuilder();

		private boolean NoPkConstraint;

		public String getTableSQL() {
			String sql = "create table " + tablename + "(\n" + columnDefinition + "\n)";
			if (charSetFix != null) {
				sql = sql + charSetFix;
			}
			return sql;
		}

		public StringBuilder getColumnDef() {
			return columnDefinition;
		}

		public void addPkConstraint(List<ColumnMapping<?>> pkFields, DatabaseDialect profile) {
			StringBuilder sb = getColumnDef();
			sb.append(",\n");
			String[] columns = new String[pkFields.size()];
			for (int n = 0; n < pkFields.size(); n++) {
				columns[n] = pkFields.get(n).getColumnName(profile, true);
			}
			if (tablename.indexOf('.') > -1) {
				tablename = StringUtils.substringAfter(tablename, ".");
				sb.append("    constraint " + "PK_" + tablename + " primary key(" + StringUtils.join(columns, ',') + ")");
			} else {
				sb.append("    constraint PK_" + tablename + " primary key(" + StringUtils.join(columns, ',') + ")");
			}
		}

	}

	public List<String> getTableSQL() {
		List<String> result = new ArrayList<String>(tables.size());
		for (TableDef table : tables) {
			result.add(table.getTableSQL());
		}
		return result;
	}

	public List<String> getOtherContraints() {
		return Collections.emptyList();
	}

	public List<PairIS> getSequences() {
		return sequences;
	}
	
	public List<ITableMetadata> getReferenceTable() {
		return referenceTable;
	}

	public void setReferenceTable(List<ITableMetadata> referenceTable) {
		this.referenceTable = referenceTable;
	}

	private List<ITableMetadata> referenceTable; 
}
