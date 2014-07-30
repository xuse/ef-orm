package jef.database.dialect;

import jef.common.wrapper.IntRange;
import jef.database.ConnectInfo;
import jef.database.DbFunction;
import jef.database.meta.DbProperty;
import jef.database.support.RDBMS;
import jef.tools.string.JefStringReader;

public class Db2Dialect extends DbmsProfile{

	public Db2Dialect() {
		super();
		setProperty(DbProperty.ADD_COLUMN, "ADD COLUMN");
		setProperty(DbProperty.MODIFY_COLUMN, "MODIFY COLUMN");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
	}

	public RDBMS getName() {
		return RDBMS.db2;
	}

	public String getGeneratedFetchFunction() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getDriverClass(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getFunction(DbFunction function, Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

	//jdbc:db2://aServer.myCompany.com:50002/name"
	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader=new JefStringReader(connectInfo.getUrl());
		reader.consume("jdbc:db2:");
		reader.omitChars('/');
		String host=reader.readToken('/');
		String dbname=reader.readToken('?',';','/');
		connectInfo.setHost(host);
		connectInfo.setDbname(dbname);
	}

	public String toPageSQL(String sql, IntRange range) {
		throw new UnsupportedOperationException();
	}
}
