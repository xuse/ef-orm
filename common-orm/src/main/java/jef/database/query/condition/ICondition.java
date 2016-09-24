package jef.database.query.condition;

import jef.database.IQueryableEntity;
import jef.database.SqlProcessor;
import jef.database.dialect.DatabaseDialect;
import jef.database.query.SqlContext;

public interface ICondition {

	String toSqlClause
	(SqlContext context, SqlProcessor processor, IQueryableEntity instance, DatabaseDialect profile, boolean batch);

}
