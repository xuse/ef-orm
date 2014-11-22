/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.dialect;

import java.sql.Types;

import jef.database.ConnectInfo;
import jef.database.jdbc.statement.UnionJudgement;
import jef.database.query.function.NoArgSQLFunction;
import jef.tools.string.JefStringReader;

/**
 * 
修改列名SQLServer：exec sp_rename't_student.name','nn','column';
sp_rename：SQLServer 内置的存储过程，用与修改表的定义。
 * @author jiyi
 * 
 * 
 * SQL Server 2005 (9.x), SQLSever 2008（10.0.x）, 2008 R2(10.5.x)可以使用此方言。
 * 
 */
public class SQLServer2005Dialect extends SQLServer2000Dialect{
	
	public SQLServer2005Dialect() {
		typeNames.put(Types.BLOB, "varbinary(MAX)", Types.VARBINARY);
		typeNames.put(Types.VARBINARY, "varbinary(MAX)", 0);
		typeNames.put(Types.VARBINARY, 8000, "varbinary($l)", 0);
		typeNames.put(Types.LONGVARBINARY, "varbinary(MAX)", 0);
		typeNames.put(Types.CLOB, "varchar(MAX)", Types.VARCHAR);
		typeNames.put(Types.LONGVARCHAR, "varchar(MAX)", Types.VARCHAR);
		typeNames.put(Types.VARCHAR, "varchar(MAX)",0);
		typeNames.put(Types.VARCHAR, 8000, "varchar($l)",0);
		typeNames.put(Types.BIT, "bit",0);
		registerNative(new NoArgSQLFunction("row_number"));
	}

	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader=new JefStringReader(connectInfo.getUrl());
		reader.setIgnoreChars(' ');
		reader.consumeIgnoreCase("jdbc:sqlserver:");
		reader.consumeChars('@','/');
		String host=reader.readToken(':','/');
		connectInfo.setHost(host);
		if(reader.omitAfterKeyIgnoreCase("databasename=", ' ')!=-1){
			String dbname=reader.readToken(' ',';',':');
			connectInfo.setDbname(dbname);
		}
	}
	
	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		StringBuilder sb=new StringBuilder("jdbc:");
		//jdbc:sqlserver:localhost:1433; DatabaseName =allandb
		sb.append("sqlserver:");
		sb.append("//").append(host).append(":").append(port<=0?1433:port);
		sb.append("; DatabaseName=").append(pathOrName);
		String url=sb.toString();
		return url;
	}
	
	public String getDriverClass(String url) {
		return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	}

	@Override
	protected LimitHandler generateLimitHander() {
		if(UnionJudgement.isDruid()){
			return new SQL2005LimitHandler();
		}else{
			return new SQL2005LimitHandlerSlowImpl();
		}
	}
	
	
}

