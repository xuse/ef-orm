package jef.database.routing.sql;

import java.util.List;
import java.util.Map;

import jef.database.OperateTarget;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.AbstractMetadata;

/**
 * 容器
 * @author jiyi
 *
 * @param <T>
 */
final class StatementContext<T> {
	/**
	 * 表元数据
	 */
	AbstractMetadata meta;
	/**
	 * 参数表
	 */
	Map<Expression, Object> paramsMap;
	/**
	 * 参数列
	 */
	List<Object> params;
	/**
	 * SQL AST
	 */
	T statement;
	/**
	 * SQL AST 表名修改句柄
	 */
	List<Table> modifications;
	/**
	 * DB操作句柄
	 */
	OperateTarget db;
	
	/**
	 * 构造
	 * @param sql
	 * @param meta
	 * @param paramsMap
	 * @param values
	 * @param db
	 * @param modificationPoints
	 */
	public StatementContext(T sql, AbstractMetadata meta, Map<Expression, Object> paramsMap, List<Object> values, OperateTarget db, List<Table> modificationPoints) {
		this.db=db;
		this.meta=meta;
		this.modifications=modificationPoints;
		this.params=values;
		this.paramsMap=paramsMap;
		this.statement=sql;
	}
}
