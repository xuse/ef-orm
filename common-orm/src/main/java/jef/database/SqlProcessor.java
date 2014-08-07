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
package jef.database;

import java.sql.SQLException;
import java.util.List;

import jef.common.Entry;
import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.PartitionSupport;
import jef.database.query.JoinElement;
import jef.database.query.SqlContext;
import jef.database.wrapper.clause.BindSql;

public interface SqlProcessor{
	
	///////////////////////////////////////////////////////////////
	/**
	 * 形成count语句
	 */
	public String toCountSql(String sql)  throws SQLException;
	/**
	 * 转换为Where字句 
	 * context: 当传入的Obj为DataObject时， context可以为null
	 */
	public BindSql toPrepareWhereSql(JoinElement obj,SqlContext context,boolean update) ;
	
	
	/**
	 * 转换为Where字句
	 * @param j
	 * @param context
	 * @return
	 */
	public String toWhereClause(JoinElement j,SqlContext context,boolean update);
	
	/**
	 * 生成更新字句
	 * @param obj
	 * @return
	 * @throws SQLException
	 */
	public String toUpdateClause(IQueryableEntity obj,boolean dynamic) throws SQLException;
	
	/**
	 * 形成update语句 
	 */
	public Entry<List<String>, List<Field>> toPrepareUpdateClause(IQueryableEntity obj,boolean dynamic);
	
	/**
	 *  收集结果数据中的数据，用需要的容器包装并返回
	 */
	public Object collectValueToContainer(List<? extends IQueryableEntity> subs, Class<?> container, String targetField);
	


	/**
	 * 获取数据库Dialect
	 * @return
	 * @deprecated
	 */
	public DatabaseDialect getProfile();
	
	public PartitionSupport getPartitionSupport();
}