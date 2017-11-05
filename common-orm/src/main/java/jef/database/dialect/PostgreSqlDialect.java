package jef.database.dialect;

import java.sql.SQLException;

import jef.database.DbMetaData;

public class PostgreSqlDialect extends AbstractDelegatingDialect {
	@Override
	protected DatabaseDialect decideDialect(DbMetaData meta) {
		try {
			String s = meta.getDatabaseVersion();
			if (s.startsWith("10.")) {
				String driverVersion = meta.getDriverVersion();
				//PG10 must be connected with driver version 42.x
				if (driverVersion.startsWith("PostgreSQL 9.") || driverVersion.startsWith("PostgreSQL 8.")) {
					throw new UnsupportedOperationException(
							"The JDBC Driver is " + driverVersion + " and database version is " + s + ".	");
				}
				return new PostgreSql10Dialect();
			} else if (s.startsWith("8.")) {
				return new PostgreSql8Dialect();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected DatabaseDialect createDefaultDialect() {
		return new PostgreSql94Dialect();
	}

}
