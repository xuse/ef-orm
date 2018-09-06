package jef.database.dialect;

import java.sql.SQLException;

import jef.database.DbMetaData;

public class MySqlDialect extends AbstractDelegatingDialect {
	@Override
	protected DatabaseDialect decideDialect(DbMetaData meta) {
		try {
			String s=meta.getDatabaseVersion();	
			if(s.endsWith("MariaDB")) {
				return maridDb(s);
			}else {
				return mySql(s);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	private DatabaseDialect mySql(String s) {
		if(s.startsWith("5.6")) {
			return null;
		}else if(s.startsWith("5.7")) {
			return new MySQL57Dialect();
		}else if(s.startsWith("8.")) {
			return new MySQL8Dialect();
		}else {
			return null;
		}
	}

	private DatabaseDialect maridDb(String s) {
		if(s.startsWith("10.")) {
			return new MariaDb10Dialect();
		}else {
			return new MariaDbDialect();
		}
	}

	@Override
	protected DatabaseDialect createDefaultDialect() {
		return new MySQL55Dialect();
	}

}
