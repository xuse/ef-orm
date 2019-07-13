package jef.database.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;

import jef.common.PairIS;
import jef.common.PairSS;
import jef.database.DbUtils;
import jef.database.dialect.ColumnType;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.ColumnType.Varchar;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.def.UniqueConstraintDef;
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

	public void addTableMeta(String tablename, ITableMetadata meta, DatabaseDialect dialect) {
		TableDef tableDef = new TableDef();
		tableDef.escapedTablename = DbUtils.escapeColumn(dialect, tablename);
		tableDef.profile=dialect;
		Map<String,String> comments=meta.getColumnComments();

		tableDef.tableComment=comments.get("#TABLE");	
		for (ColumnMapping column : meta.getColumns()) {
			String c=comments.get(column.fieldName());
			processField(column, tableDef, dialect,c);
		}
		if (!tableDef.NoPkConstraint && !meta.getPKFields().isEmpty()) {
			tableDef.addPkConstraint(meta.getPKFields(), dialect, tablename);
		}
		for(UniqueConstraintDef unique: meta.getUniqueDefinitions()){
			tableDef.addUniqueConstraint(unique,meta,dialect);
		}
		this.tables.add(tableDef);
	}

	private void processField(ColumnMapping entry, TableDef result, DatabaseDialect dialect,String comment) {
		StringBuilder sb = result.getColumnDef();
		if (sb.length() > 0)
			sb.append(",\n");
		String escapedColumnName=entry.getColumnName(dialect, true);
		sb.append("    ").append(escapedColumnName).append(" ");
		
		ColumnType vType = entry.get();
		if (entry.isPk()) {
			vType.setNullable(false);
			if (vType instanceof Varchar) {
				Varchar vcType = (Varchar) vType;
				int check = dialect.getPropertyInt(DbProperty.INDEX_LENGTH_LIMIT);
				if (check > 0 && vcType.getLength() > check) {
					throw new IllegalArgumentException("The varchar column in " + dialect.getName() + " will not be indexed if length is >" + check);
				}
				check = dialect.getPropertyInt(DbProperty.INDEX_LENGTH_LIMIT_FIX);
				if (check > 0 && vcType.getLength() > check) {
					result.charSetFix = dialect.getProperty(DbProperty.INDEX_LENGTH_CHARESET_FIX);
				}
			}
		}
		if (entry instanceof AutoIncrementMapping) {
			if (dialect.has(Feature.AUTOINCREMENT_NEED_SEQUENCE)) {
				int precision = ((AutoIncrement) vType).getPrecision();
				addSequence(((AutoIncrementMapping) entry).getSequenceName(dialect), precision);

			}
			if (dialect.has(Feature.AUTOINCREMENT_MUSTBE_PK)) { // 在一些数据库上，只有主键才能自增，并且此时不能再单独设置主键.
				result.NoPkConstraint = true;
			}
		}
		if (entry.getMeta().getEffectPartitionKeys() != null) { // 如果是分表的，自增键退化为常规字段
			if (vType instanceof AutoIncrement) {
				vType = ((AutoIncrement) vType).toNormalType();
			}
		}
		sb.append(dialect.getCreationComment(vType, false));
		if(StringUtils.isNotEmpty(comment)) {
			if(dialect.has(Feature.SUPPORT_COMMENT)) {
				result.ccmments.add(new PairSS(escapedColumnName,comment));
			}else if(dialect.has(Feature.SUPPORT_INLINE_COMMENT)) {
				sb.append(" comment '"+comment.replace("'", "''")+"'");
			}
		}
	}

	private void addSequence(String seq, int precision) {
		sequences.add(new PairIS(precision, seq));
	}

	/**
	 * 要创建的Sequence
	 */
	private final List<PairIS> sequences = new ArrayList<PairIS>();

	static class TableDef {
		private String escapedTablename;
		/**
		 * MySQL专用。字符集编码
		 */
		private String charSetFix;
		/**
		 * 列定义
		 */
		private final StringBuilder columnDefinition = new StringBuilder();

		/**
		 * 表备注
		 */
		private String tableComment;
		/**
		 * 数据库类型
		 */
		private DatabaseDialect profile;

		/**
		 * 各个字段备注
		 */
		private List<PairSS> ccmments = new ArrayList<PairSS>();
		/**
		 * 没有主键约束
		 */
		private boolean NoPkConstraint;

		public String getTableSQL() {
			String sql = "CREATE TABLE " + escapedTablename + "(\n" + columnDefinition + "\n)";
			if (charSetFix != null) {
				sql = sql + charSetFix;
			}
			if(StringUtils.isNotEmpty(tableComment) && profile.has(Feature.SUPPORT_INLINE_COMMENT)) {
				sql=sql+" comment '"+tableComment.replace("'", "''")+"'";
			}
			return sql;
		}

		public void addUniqueConstraint(UniqueConstraintDef unique,ITableMetadata meta, DatabaseDialect dialect) {
			List<String> columns=unique.getColumnNames(meta, dialect);
			StringBuilder sb = getColumnDef();
			sb.append(",\n");
			String cname=unique.name();
			if(StringUtils.isEmpty(cname)){
				cname="UC_"+RandomStringUtils.randomAlphanumeric(8).toUpperCase();
			}
			sb.append("    CONSTRAINT ").append(cname).append(" UNIQUE (");
			StringUtils.joinTo(columns, ",", sb);
			sb.append(')');
		}

		public StringBuilder getColumnDef() {
			return columnDefinition;
		}

		public void addPkConstraint(List<ColumnMapping> pkFields, DatabaseDialect profile, String tablename) {
			StringBuilder sb = getColumnDef();
			sb.append(",\n");
			String[] columns = new String[pkFields.size()];
			for (int n = 0; n < pkFields.size(); n++) {
				columns[n] = pkFields.get(n).getColumnName(profile, true);
			}
			if (tablename.indexOf('.') > -1) {
				tablename = StringUtils.substringAfter(tablename, ".");
			}
			String pkName = profile.getObjectNameToUse("PK_" + tablename);
			sb.append("    CONSTRAINT " + pkName + " PRIMARY KEY(" + StringUtils.join(columns, ',') + ")");
		}

		public void addTableComment(List<String> result) {
			if (StringUtils.isNotEmpty(tableComment) && profile.has(Feature.SUPPORT_COMMENT)) {
				result.add("COMMENT ON TABLE " + escapedTablename + " IS '" + tableComment.replace("'", "''") + "'");
			}
		}

		public void addColumnComment(List<String> result) {
			for (PairSS column : ccmments) {
				String comment=column.getSecond();
				if (StringUtils.isNotEmpty(comment)) {
					result.add("comment on column " + escapedTablename +"."+column.first+ " is '" + comment.replace("'", "''") + "'");	
				}
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

	public List<String> getComments() {
		List<String> result = new ArrayList<String>();
		for (TableDef table : tables) {
			table.addTableComment(result);
			table.addColumnComment(result);
		}
		return result;
	}
}
