package jef.database.query;

import jef.tools.StringUtils;

/**
 * 描述一个分表计算的中间结果
 * 
 * @author Administrator
 * 
 */
public class DbTable {
	String dbName; // db名称(site名)
	String table; // 表名 (table名)
	boolean isDbRegexp; // db名是否为正则表达式
	boolean isTbRegexp; // table名是否为正则表达式

	public DbTable(String db, String table) {
		this(db, table, false, false);
	}

	public DbTable(String db, String table, boolean regexp, boolean dbRegexp) {
		this.dbName = db;
		this.table = table;
		this.isTbRegexp = regexp;
		this.isDbRegexp = dbRegexp;
		if (table == null || table.length() == 0)
			throw new IllegalArgumentException();
	}

	@Override
	public int hashCode() {
		return dbName == null ? table.hashCode() : dbName.hashCode() + table.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DbTable) {
			DbTable rhs = (DbTable) obj;
			if (!StringUtils.equals(dbName, rhs.dbName))
				return false;
			return table.equals(rhs.table);
		}
		return false;
	}

	@Override
	public String toString() {
		return StringUtils.toString(dbName) + "." + table;
	}
}
