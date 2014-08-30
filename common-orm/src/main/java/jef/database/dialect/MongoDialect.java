package jef.database.dialect;

import jef.common.wrapper.IntRange;
import jef.database.ConnectInfo;
import jef.database.support.RDBMS;

public class MongoDialect extends DbmsProfile{

	public RDBMS getName() {
		return RDBMS.mongo;
	}

	public String getGeneratedFetchFunction() {
		return null;
	}

	public String getDriverClass(String url) {
		return null;
	}

	public String toPageSQL(String sql, IntRange range) {
		// TODO Auto-generated method stub
		return null;
	}

	public void parseDbInfo(ConnectInfo connectInfo) {
		// TODO Auto-generated method stub
		
	}

	public void addKeyword(String... keys) {
	}

}
