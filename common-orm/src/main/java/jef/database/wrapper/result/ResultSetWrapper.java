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
package jef.database.wrapper.result;

import java.sql.ResultSet;
import java.sql.Statement;

import jef.database.OperateTarget;
import jef.database.wrapper.populator.ColumnMeta;

/**
 * IResultSet的最简实现相似。但CLose的时候能够将Statement和对应的数据库连接一起释放。
 * @author jiyi
 *
 */
public final class ResultSetWrapper extends ResultSetImpl{
	ResultSetWrapper(){
		super(null,null,null);
	}
	
	public ResultSetWrapper(OperateTarget tx,Statement st,ResultSet rs) {
		super(new ResultSetHolder(tx,st,rs),tx.getProfile());
	}
	
	public ResultSetWrapper(ResultSetHolder holder) {
		super(holder,holder.db.getProfile());
	}
	
	public ResultSetWrapper(ResultSetHolder holder,ColumnMeta columns) {
		super(holder,columns,holder.db.getProfile());
	}
	
	public OperateTarget getTarget() {
		return ((ResultSetHolder)super.rs).db;
	}
}
