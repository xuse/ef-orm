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

import jef.common.wrapper.IntRange;
import jef.database.ConnectInfo;
import jef.database.DbFunction;
import jef.database.meta.DbProperty;
import jef.database.support.RDBMS;
import jef.tools.string.JefStringReader;

/**
 * 
修改列名SQLServer：exec sp_rename't_student.name','nn','column';
sp_rename：SQLServer 内置的存储过程，用与修改表的定义。
 * @author jiyi
 */
public class SqlServerDialect extends DbmsProfile{
	
	public SqlServerDialect() {
		super();
		setProperty(DbProperty.ADD_COLUMN, "ADD COLUMN");
		setProperty(DbProperty.MODIFY_COLUMN, "MODIFY COLUMN");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
		setProperty(DbProperty.CHECK_SQL, "select 1");

	}

	public String getGeneratedFetchFunction() {
		return "SELECT @@IDENTITY";
	}

	public String getDriverClass(String url) {
		return "com.microsoft.jdbc.sqlserver.SQLServerDriver";
	}

	public int getPort() {
		return 1433;
	}

	public RDBMS getName() {
		return RDBMS.sqlserver;
		
		
	}

	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		StringBuilder sb=new StringBuilder("jdbc:");
		//jdbc:microsoft:sqlserver:@localhost:1433; DatabaseName =allandb
		sb.append("microsoft:sqlserver:");
		sb.append("@").append(host).append(":").append(port<=0?1433:port);
		sb.append("; DatabaseName=").append(pathOrName);
		String url=sb.toString();
		return url;
	}

	public String getFunction(DbFunction function, Object... params) {
		throw new UnsupportedOperationException();
	}

	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader=new JefStringReader(connectInfo.getUrl());
		reader.setIgnoreChars(' ');
		reader.consumeIgnoreCase("jdbc:microsoft:sqlserver:");
		reader.consumeChars('@','/');
		String host=reader.readToken(':','/');
		connectInfo.setHost(host);
		if(reader.omitAfterKeyIgnoreCase("databasename=", ' ')!=-1){
			String dbname=reader.readToken(' ',';',':');
			connectInfo.setDbname(dbname);
		}
		if(reader.omitAfterKeyIgnoreCase("user=", ' ')!=-1){
			String user=reader.readToken(' ',';',':');
			connectInfo.setUser(user);
		}
		if(reader.omitAfterKeyIgnoreCase("password=", ' ')!=-1){
			String password=reader.readToken(' ',';',':');
			connectInfo.setPassword(password);
		}
	}

	public String toPageSQL(String sql, IntRange range) {
		throw new UnsupportedOperationException();
	}
}
