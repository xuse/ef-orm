package jef.database.dialect;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.database.DbMetaData;

public class MySqlDialect extends AbstractDelegatingDialect {
	@Override
	protected DatabaseDialect decideDialect(DbMetaData meta) {
		try {
			String s = meta.getDatabaseVersion();
			if (s.endsWith("MariaDB")) {
				return maridDb(s, meta);
			} else {
				return mySql(s, meta);
			}
		} catch (SQLException e) {
			LogUtil.error("Database metadata error", e);
		}
		return null;
	}

	private DatabaseDialect mySql(String dbVersion, DbMetaData meta) throws SQLException {
		int driver = meta.callDatabaseMetadata(e -> e.getDriverMajorVersion());
		String driverVersion = meta.getDriverVersion();
		if (dbVersion.startsWith("5.6")) {
			if (driver == 8) {
				throw driverVersionException(dbVersion, driverVersion);
			}
			return null;
		} else if (dbVersion.startsWith("5.7")) {
			if (driver == 8) {
				throw driverVersionException(dbVersion, driverVersion);
			}
			return new MySQL57Dialect();
		} else if (dbVersion.startsWith("8.")) {
			if (driver < 8) {
				throw driverVersionException(dbVersion, driverVersion);
			}
			return new MySQL8Dialect();
		} else {
			return null;
		}
	}

	private PersistenceException driverVersionException(String dbVersion, String driverVersion) {
		return new PersistenceException("The JDBC driver version is not match the database, database is [" + dbVersion + "], but driver is " + driverVersion);
	}

	private DatabaseDialect maridDb(String dbVersion, DbMetaData meta) throws SQLException {
		String driverVersion = meta.getDriverVersion();
		if (dbVersion.startsWith("10.")) {
			return new MariaDb10Dialect();
		} else {
			if(dbVersion.startsWith("5.") && driverVersion.startsWith("8.")) {
				throw driverVersionException(dbVersion, driverVersion);
			}
			return new MariaDbDialect();
		}
	}

	@Override
	protected DatabaseDialect createDefaultDialect() {
		return new MySQL55Dialect();
	}

}
