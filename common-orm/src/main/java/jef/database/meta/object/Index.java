/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.meta.object;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jef.database.Field;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.DbProperty;
import jef.database.meta.FBIField;
import jef.database.meta.ITableMetadata;
import jef.database.meta.def.IndexDef;
import jef.tools.ArrayUtils;
import jef.tools.StringUtils;

/**
 * 描述一个数据库中查到的索引信息 注意:内部的Columns都是数据库列名。
 * 
 * @author Administrator
 * 
 */
public class Index {
	/**
	 * 表的schema
	 */
	private String tableSchema;
	/**
	 * 表的indexQualifier
	 */
	private String indexQualifier;

	/**
	 * 表名
	 */
	private String tableName;
	/**
	 * 索引名
	 */
	private String indexName;
	/**
	 * 各个列 其中KEY是列名，value=true表示正序ASC，false表示倒序DESC
	 */
	private final List<IndexItem> columns = new ArrayList<IndexItem>(4);
	/**
	 * 是否唯一
	 */
	private boolean unique;

	/**
	 * 过滤条件
	 */
	private String filterCondition;

	/**
	 * 用户定义的索引关键字，如 hash partitioned CLUSTERED COLUMNSTORE 这些关键字
	 */
	private String userDefinition;

	/**
	 * <UL>
	 * <LI>tableIndexStatistic - this identifies table statistics that are
	 * returned in conjuction with a table's index descriptions</LI>
	 * <LI>tableIndexClustered - this is a clustered index</LI>
	 * <LI>tableIndexHashed - this is a hashed index</LI>
	 * <LI>tableIndexOther - this is some other style of index</LI>
	 * </UL>
	 */
	private int type;

	/**
	 * 描述索引的用途
	 */
	public enum Usage {
		/**
		 * 主键产生的索引
		 */
		PK,
		/**
		 * 外键产生的索引
		 */
		FK,
		/**
		 * Unique约束产生的索引
		 */
		UNIQUE,
		/**
		 * 独立索引
		 */
		INDEX
	}

	/*
	 * 索引的用途 一般来说有大部分数据库有四种约束 PK FK UNIQUE CHECK (NOT NULL &
	 * DEFAULT不算)，前三种都会产生Index。 一个索引能否直接删除，取决于其是属于约束。 问题是现在无法判断一个Index究竟属于谁。
	 * 由于JDBC不支持直接获得Index的类型，也不支持获得数据库CONSTRAINT。因此我们只能用获得Index的方法
	 * 来间接的获得UNIQUE约束。此时如何判定INDEX是一个独立的INDEX还是一个隶属于表的CONSTRAINT。是个麻烦的问题。
	 * 
	 * 在MySQL上，Unique index等同于UNIQUE
	 * CONSTRAINT。MySQL上的Constraint增加的索引，除了PK外都可以drop index删除。
	 * 在ORALCE/Derby/Postgres上，UNIQIE index是UNIQUE CONSTRAINT的一个组成部分，不能直接删除
	 * 
	 * 因此，究竟是一个独立索引，还是一个附属于约束 (PK.FK.UNIQUE)的索引，这是个问题。 怎么才能判断出来呢？ 相对来说，PK FK
	 * 比较好判断。
	 * 
	 * 复杂的方法不是没有，对于MySQL可以用 show create
	 * table得到建表语句，然后解析可以得到所有约束——反正除了主键受保护，别的随便删 MySQL也支持 select * from
	 * INFORMATION_SCHEMA.TABLE_CONSTRAINTS
	 * 
	 * ORACLE麻烦一点，查系统表，或者用select
	 * table_name,dbms_metadata.get_ddl('TABLE','TABLEMASTER')from user_tables
	 * where table_name='TABLEMASTER'; select A.* , B.column_name
	 * 
	 * from all_constraints A inner join all_cons_columns B on A.constraint_name
	 * = B.constraint_name and A.table_name = B.table_name and A.owner = b.owner
	 * 
	 * where A. owner = 'TEST' order by a.CONSTRAINT_NAME
	 * 
	 * 
	 * 得到建表语句。PG需要查系统表pg_constraint。
	 * 
	 * Derby？ MSSQL ？
	 * 
	 * 要么先不支持。反正索引如果删不掉那就是约束。
	 */
	// TODO 准确判断索引的类型
	// private Usage usage;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("index ");
		sb.append(indexName).append(" on ").append(tableName);
		sb.append("(");
		Iterator<IndexItem> iter = columns.iterator();
		sb.append(iter.next());
		for (; iter.hasNext();) {
			sb.append(',').append(iter.next());
		}
		sb.append(")");
		if (unique)
			sb.append("\t[UNIQUE]");
		return sb.toString();
	}

	public Index(String indexName) {
		this.indexName = indexName;
	}

	public Index() {
	}

	/**
	 * 索引类型，有以下几类
	 * <UL>
	 * <LI>tableIndexStatistic(0) - this identifies table statistics that are
	 * returned in conjuction with a table's index descriptions
	 * <LI>tableIndexClustered(1) - this is a clustered index
	 * <LI>tableIndexHashed (2) - this is a hashed index
	 * <LI>tableIndexOther (3) - this is some other style of index
	 * </UL>
	 * 
	 * @return 索引类型
	 */
	public int getType() {
		return type;
	}

	/**
	 * 设置索引类型
	 * 
	 * @param type
	 *            索引类型
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * 得到索引中的所有列，不会包含ASC.DESC等信息
	 * 
	 * @return 所有列名
	 */
	public String[] getColumnNames() {
		String[] result = new String[columns.size()];
		for (int i = 0; i < columns.size(); i++) {
			result[i] = columns.get(i).column;
		}
		return result;
	}

	/**
	 * 得到索引中的所有列，如果是倒序带有" DESC"字样
	 * 
	 * @return
	 */
	public String[] getColumnNamesWithOrder() {
		String[] result = new String[columns.size()];
		for (int i = 0; i < columns.size(); i++) {
			result[i] = columns.get(i).toString();
		}
		return result;
	}

	/**
	 * 添加一个列，在最后的位置
	 * 
	 * @param column
	 *            列名称
	 */
	public void addColumn(String column, Boolean isAsc) {
		columns.add(new IndexItem(column, isAsc, columns.size() + 1));
	}

	/**
	 * 添加一个列
	 */
	public void addColumn(String column, Boolean isAsc, int seq) {
		columns.add(seq - 1, new IndexItem(column, isAsc, seq));
	}

	/**
	 * 得到列的长度
	 * 
	 * @return
	 */
	public int columnSize() {
		return columns == null ? 0 : columns.size();
	}

	/**
	 * 获得索引名称
	 * 
	 * @return 索引名称
	 */
	public String getIndexName() {
		return indexName;
	}

	/**
	 * 设置索引名称
	 * 
	 * @param indexName
	 *            索引名称
	 */
	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	/**
	 * 该索引是否唯一约束
	 * 
	 * @return 如有唯一约束返回true
	 */
	public boolean isUnique() {
		return unique;
	}

	public boolean isClustered() {
		return StringUtils.contains(userDefinition, "clustered", true) || type == DatabaseMetaData.tableIndexClustered;
	}

	/**
	 * 设置该索引是否有唯一约束
	 * 
	 * @param unique
	 *            是否有唯一约束
	 */
	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public List<IndexItem> getColumns() {
		return columns;
	}

	/**
	 * 获得索引所在的表名
	 * 
	 * @return 索引所在的表名
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * 设置索引所在的表名
	 * 
	 * @param tableName
	 *            索引所在的表名
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getTableSchema() {
		return tableSchema;
	}

	public void setTableSchema(String tableSchema) {
		this.tableSchema = tableSchema;
	}

	public String getIndexQualifier() {
		return indexQualifier;
	}

	public void setIndexQualifier(String indexQualifier) {
		this.indexQualifier = indexQualifier;
	}

	public String getFilterCondition() {
		return filterCondition;
	}

	public void setFilterCondition(String filterCondition) {
		this.filterCondition = filterCondition;
	}

	public String getUserDefinition() {
		return userDefinition;
	}

	public void setUserDefinition(String userDefinition) {
		this.userDefinition = userDefinition;
	}

	/**
	 * 描述一个索引中的单个列
	 * 
	 * @author jiyi
	 */
	public static class IndexItem {
		/**
		 * 列名
		 */
		public String column;
		/**
		 * 正序倒序。
		 */
		public boolean asc;
		/**
		 * 字段顺序
		 */
		public int seq;

		public IndexItem(String column, boolean asc, int seq) {
			this.column = column;
			this.asc = asc;
			this.seq = seq;
		}

		public IndexItem() {
		}

		@Override
		public String toString() {
			if (asc) {
				return column;
			} else {
				return column + " DESC";
			}
		}

	}

	/**
	 * 生成CREATE INDEX语句
	 * 
	 * @param profile
	 * @return
	 */
	public String toCreateSql(DatabaseDialect profile) {
		StringBuilder sb = new StringBuilder("CREATE ");
		if (this.unique) {
			sb.append("UNIQUE ");
		}
		if (this.type == DatabaseMetaData.tableIndexClustered) {
			sb.append("CLUSTERED ");
		}
		if (StringUtils.isNotEmpty(userDefinition)) {
			sb.append(userDefinition).append(' ');
		}
		sb.append("INDEX ");
		sb.append(profile.getObjectNameToUse(StringUtils.isEmpty(indexName) ? generateRandomName() : indexName)).append(" ON ");
		sb.append(getTableWithSchem()).append("(");
		Iterator<IndexItem> iter = columns.iterator();
		sb.append(iter.next());
		for (; iter.hasNext();) {
			sb.append(',').append(iter.next());
		}
		sb.append(")");
		sb.append(profile.getProperty(DbProperty.INDEX_USING_HASH, ""));
		return sb.toString();
	}

	public String generateRandomName() {
		if (StringUtils.isEmpty(this.indexName)) {
			StringBuilder iNameBuilder = new StringBuilder();
			iNameBuilder.append("IDX_").append(StringUtils.truncate(StringUtils.removeChars(tableName, '_'), 14));
			iNameBuilder.append(StringUtils.randomString());
			this.indexName = iNameBuilder.toString();
		}
		return indexName;
	}

	public String getTableWithSchem() {
		if (StringUtils.isEmpty(tableSchema)) {
			return tableName;
		} else {
			return tableSchema + "." + tableName;
		}
	}

	public boolean isOnSingleColumn(String columnName) {
		if (columnName == null)
			return false;
		if (this.columns.size() != 1)
			return false;
		for (IndexItem item : this.columns) {
			if (columnName.equals(item.column)) {
				return true;
			}
		}
		return false;
	}

	public static String generateName(String tablename, List<Field> fields, DatabaseDialect dialect, ITableMetadata meta) {
		StringBuilder iNameBuilder = new StringBuilder();
		iNameBuilder.append("IDX_").append(StringUtils.truncate(StringUtils.removeChars(tablename, '_'), 14));
		int maxField = ((28 - iNameBuilder.length()) / fields.size()) - 1;
		if (maxField < 1)
			maxField = 1;
		for (Field field : fields) {
			iNameBuilder.append('_');
			if (field instanceof FBIField) {
				iNameBuilder.append(StringUtils.truncate(StringUtils.randomString(), maxField));
			} else {
				iNameBuilder.append(StringUtils.truncate(meta.getColumnDef(field).getColumnName(dialect, false), maxField));
			}
		}
		if (iNameBuilder.length() > 30)
			iNameBuilder.setLength(30);
		return iNameBuilder.toString();
	}

	/**
	 * 将IndexDef对象转换为Index对象
	 * 
	 * @param indexDef
	 *            索引定义
	 * @param meta
	 *            所属的表
	 * @param dialect
	 *            当前数据库方言，需要用方言转换为合适的列大小写以及是否需要加引号
	 * @param tablename
	 *            可以传入null，如果表名不是默认的，需要传入
	 * @return
	 */
	public static Index valueOf(IndexDef indexDef, ITableMetadata meta, DatabaseDialect dialect, String tablename) {
		String schema = null;
		if (StringUtils.isNotEmpty(tablename)) {
			int n = tablename.indexOf('.');
			if (n > -1) {
				schema = tablename.substring(0, n);
				tablename = tablename.substring(n + 1);
			}
		} else {
			schema = meta.getSchema();
			tablename = meta.getTableName(false);
		}

		List<Field> fields = new ArrayList<Field>();
		List<IndexItem> columns = new ArrayList<IndexItem>();
		for (String fieldname : indexDef.getColumns()) {
			boolean asc = true;
			// 去掉列定义后方的asc或者desc
			if (fieldname.toLowerCase().endsWith(" desc")) {
				asc = false;
				fieldname = fieldname.substring(0, fieldname.length() - 5).trim();
			} else if (fieldname.toLowerCase().endsWith(" asc")) {
				fieldname = fieldname.substring(0, fieldname.length() - 4).trim();
			}
			Field field = null;
			ColumnMapping column = meta.getColumnDef(fieldname);
			if (column == null) {
				// 可能是一个函数索引
				field = new FBIField(fieldname);
				columns.add(new IndexItem(fieldname, asc, 0));
			} else {
				field=column.field();
				columns.add(new IndexItem(column.getColumnName(dialect, true), asc, 0));
			}
			fields.add(field);
		}

		String indexName = indexDef.getName();
		if (StringUtils.isEmpty(indexName)) {
			indexName = Index.generateName(tablename, fields, dialect, meta);
		}
		Index indexobj = new Index(indexName);
		indexobj.setTableSchema(schema);
		indexobj.setTableName(tablename);
		indexobj.setUnique(indexDef.isUnique());
		if (indexDef.isClustered()) {
			indexobj.setType(DatabaseMetaData.tableIndexClustered);
		}
		indexobj.setUserDefinition(indexDef.getDefinition());
		for (IndexItem c : columns) {
			indexobj.addColumn(c.column, c.asc);
		}
		return indexobj;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Index) {
			Index other = (Index) obj;
			return this.isUnique() == other.isUnique() && this.isClustered() == other.isClustered() && ArrayUtils.equals(this.getColumnNamesWithOrder(), other.getColumnNamesWithOrder());
		}
		return false;
	}

}
