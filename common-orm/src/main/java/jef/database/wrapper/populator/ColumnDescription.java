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
package jef.database.wrapper.populator;

import java.sql.SQLException;

import jef.common.log.LogUtil;
import jef.database.dialect.type.ResultSetAccessor;
import jef.database.wrapper.result.IResultSet;

public class ColumnDescription{
	private int n;
	private int type;
	private String name;
	private String simpleName;
	private ResultSetAccessor accessor;
	private String dbSchema;
	private String table;
	
	public ColumnDescription(int n,int type,String name,String table,String dbSchema){
		this.n=n;
		this.type=type;
		this.name=name;
		this.table=table;
		this.dbSchema=dbSchema;
	}

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public int getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDbSchema() {
		return dbSchema;
	}

	public String getTable() {
		return table;
	}

	/**
	 * 总是返回小写的SimpleName
	 * @return
	 */
	public String getSimpleName() {
		return simpleName;
	}

	public void setSimpleName(String simpleName) {
		this.simpleName = simpleName.toLowerCase();
	}

	@Override
	public String toString() {
		return "("+n+")"+name+" [Type:"+type+"]";
	}

	public ResultSetAccessor getAccessor() {
		return accessor;
	}

	public void setAccessor(ResultSetAccessor accessor) {
		if(this.accessor!=null && this.accessor!=accessor){
			LogUtil.warn("Column "+this.name+"("+type+") received two different ResultSetAccessor!"+this.accessor+" : "+accessor);
		}
		this.accessor = accessor;
	}

	public Object getValue(IResultSet rs) throws SQLException {
		return accessor.getProperObject(rs, n);
	}
	
}
