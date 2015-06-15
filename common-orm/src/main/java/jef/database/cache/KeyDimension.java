package jef.database.cache;

import java.util.List;

import jef.common.PairSO;
import jef.database.cache.WhereParser.DruidImpl;
import jef.database.cache.WhereParser.NativeImpl;
import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.statement.UnionJudgement;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.visitor.Expression;

import org.apache.commons.lang.builder.HashCodeBuilder;

public class KeyDimension {
	protected String where;
	protected String order;

	// 多表查询时的表
	private String tableDefinition;

	// 单表查询维度时的表名
	// private String table;
	// 多表查询时的维度表名
	private List<String> affectedTables;

	// 唯一标识
	protected int hashCode;

	public KeyDimension newKeyDimensionOf(String newTable, DatabaseDialect profile) {
		KeyDimension result = new KeyDimension(newTable,profile, where, order);
		return result;
	}
	
	/*
	 * 内部构造，为newKeyDimensionOf方法专用
	 */
	private KeyDimension(String table, DatabaseDialect profile, String where, String order) {
		// 解析Table
		PairSO<List<String>> tableParsed = wp.parseTables(table, profile);
		this.tableDefinition = tableParsed.first;
		this.affectedTables = tableParsed.second;
		this.where = where;
		this.order = order;
		this.hashCode = new HashCodeBuilder().append(where).append(affectedTables).append(order).toHashCode();
	}
	

	/**
	 * 当确定是单表时使用此方法减少解析
	 * 
	 * @param table
	 * @param where
	 * @param order
	 * @param profile
	 * @return
	 */
	public static KeyDimension forSingleTable(String table, String where, String order, DatabaseDialect profile) {
		return new KeyDimension(profile, table, where, order);
	}



	/**
	 * 构造。 table直接转大写，不解析
	 * 
	 * @param profile
	 * @param table
	 *            不解析
	 * @param where
	 * @param order
	 */
	private KeyDimension(DatabaseDialect profile, String table, String where, String order) {
		// 解析Where
		if (where == null || where.length() == 0) {
			this.where = where;
		} else {
			this.where = wp.process(where, profile);
		}
		// 解析Table
		this.tableDefinition = table.toUpperCase();
		this.affectedTables = null;

		this.order = order == null ? "" : order;
		this.hashCode = new HashCodeBuilder().append(where).append(affectedTables).append(order).toHashCode();
	}

	/**
	 * 构造
	 * 
	 * @param where
	 * @param order
	 * @param profile
	 */
	public KeyDimension(String table, String where, String order, DatabaseDialect profile) {
		// 解析Where
		if (where == null || where.length() == 0) {
			this.where = where;
		} else {
			this.where = wp.process(where, profile);
		}
		// 解析Table
		PairSO<List<String>> tableParsed = wp.parseTables(table, profile);
		this.tableDefinition = tableParsed.first;
		this.affectedTables = tableParsed.second;
		// 如果是单表查询，清空
		if (affectedTables.size() == 1) {
			affectedTables = null;
		}

		this.order = order == null ? "" : order;
		this.hashCode = new HashCodeBuilder().append(where).append(affectedTables).append(order).toHashCode();
	}

	public String getTableDefinition() {
		return tableDefinition;
	}

	public List<String> getTables() {
		return affectedTables;
	}

	/**
	 * 构造
	 * 
	 * @param where2
	 * @param order2
	 */
	public KeyDimension(Table table, Expression where2, Expression order2) {
		WhereParser.removeAliasAndCase(table);
		WhereParser.removeAliasAndCase(where2);

		this.tableDefinition = table.toString();
		this.affectedTables = null;
		this.where = where2.toString();
		this.order = order2 == null ? "" : order2.toString();
		this.hashCode = where.hashCode() + order.hashCode();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof KeyDimension) {
			KeyDimension rhs = (KeyDimension) obj;
			return this.where.equals(rhs.where) && this.order.equals(rhs.order) && tableDefinition.equals(rhs.tableDefinition);
		}
		return false;
	}

	@Override
	public String toString() {
		if (order == null) {
			return where;
		} else {
			return where + order;
		}
	}

	private static final WhereParser wp;

	static {
		if (UnionJudgement.isDruid()) {
			wp = new DruidImpl();
		} else {
			wp = new NativeImpl();
		}
	}
}
