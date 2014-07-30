package jef.database.dialect;

import jef.database.support.RDBMS;

public class MariaDbDialect extends MySqlDialect{
	@Override
	public String getDriverClass(String url) {
		return "org.mariadb.jdbc.Driver";
	}

	@Override
	public RDBMS getName() {
		return RDBMS.mariadb;
	}

}
