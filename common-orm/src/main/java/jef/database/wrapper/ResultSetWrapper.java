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
package jef.database.wrapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.OperateTarget;
import jef.database.ResultSetReleaseHandler;
import jef.database.dialect.DatabaseDialect;


public class ResultSetWrapper extends ResultSetImpl{
	
	private ResultSetReleaseHandler handler;
	
	ResultSetWrapper(ResultSet rs,ColumnMeta columns,DatabaseDialect dialect) {
		super(rs,columns,dialect);
	}
	
	public ResultSetWrapper(ResultSet rs, DatabaseDialect dialect) {
		super(rs,dialect);
	}
	

	public ResultSetReleaseHandler getHandler() {
		return handler;
	}

	public void setHandler(ResultSetReleaseHandler handler) {
		this.handler = handler;
	}

	public void close() throws SQLException {
		super.close();
		if(handler!=null){
			handler.release();
		}
	}

	public OperateTarget getTarget() {
		if(handler==null)return null;
		return handler.getDb();
	}
}
