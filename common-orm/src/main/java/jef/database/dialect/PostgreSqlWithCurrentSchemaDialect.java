package jef.database.dialect;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import jef.database.DbMetaData;
import jef.database.meta.Feature;
import jef.database.meta.object.Constraint;

/**
 * Postgres 9.4开始支持的若干类型
 * 
 * @author jiyi
 *
 */
public class PostgreSqlWithCurrentSchemaDialect extends PostgreSql94Dialect {
	public PostgreSqlWithCurrentSchemaDialect() {
		features.add(Feature.USER_AS_SCHEMA);
	}
	@Override
	public List<Constraint> getConstraintInfo(DbMetaData conn, String schema, String tablename, String constraintName)
			throws SQLException {
		return Collections.emptyList();
	}
}
