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


/**
 * Access数据库的meta 
 * @author Administrator
 *
 */
public class AccessDialect extends DbmsProfile{
	
	public AccessDialect() {
		super();
		setProperty(DbProperty.ADD_COLUMN, "ADD COLUMN");
		setProperty(DbProperty.MODIFY_COLUMN, "MODIFY COLUMN");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");

	}
	public String getGeneratedFetchFunction() {
		return null;
	}

	public String getDriverClass(String url) {
		return "sun.jdbc.odbc.JdbcOdbcDriver";
	}

	public int getPort() {
		return 0;
	}

	public RDBMS getName() {
		return RDBMS.access;
	}
	//TODO
	public String currentDateFunction() {
		throw new UnsupportedOperationException();
	}
	//TODO
	public String currentTimeFunction() {
		throw new UnsupportedOperationException();
	}
	//TODO
	public String currentTimestampFunction() {
		throw new UnsupportedOperationException();
	}

	public String getFunction(DbFunction function, Object... params) {
		return null;
	}

	public void parseDbInfo(ConnectInfo connectInfo) {
		throw new UnsupportedOperationException();
	}

	public String toPageSQL(String sql, IntRange range) {
		throw new UnsupportedOperationException();
	}
}
