package jef.database.routing.sql;

import java.util.List;
import java.util.Map;

import jef.database.OperateTarget;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.AbstractMetadata;

final class StatementContext<T> {
	AbstractMetadata meta;
	Map<Expression, Object> paramsMap;
	List<Object> params;
	T statement;
	List<Table> modifications;
	OperateTarget db;
	
	public StatementContext(T sql, AbstractMetadata meta, Map<Expression, Object> paramsMap, List<Object> values, OperateTarget db, List<Table> modificationPoints) {
		this.db=db;
		this.meta=meta;
		this.modifications=modificationPoints;
		this.params=values;
		this.paramsMap=paramsMap;
		this.statement=sql;

	}
	

}
