package jef.database.dialect;

import jef.tools.StringUtils;

public class HsqlDbFileDialect extends HsqlDbMemDialect{

	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		if(StringUtils.isEmpty(host)){
			return "jdbc:hsqldb:file:"+pathOrName;	
		}else{
			//生成形如的URL
			//jdbc:hsqldb:hsql://localhost:9001/testDbName
			if(port<=0)port=9001;
			if(!pathOrName.startsWith("/"))pathOrName="/"+pathOrName;
			return "jdbc:hsqldb:hsql://"+host+":"+port+pathOrName;
		}
	}
}
