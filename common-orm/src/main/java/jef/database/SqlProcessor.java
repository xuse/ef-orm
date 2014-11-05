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

import jef.database.annotation.PartitionResult;
import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.PartitionSupport;
import jef.database.query.JoinElement;
import jef.database.query.SqlContext;
import jef.database.wrapper.clause.BindSql;

/**
 * SQL语句生成器
 * @author jiyi
 *
 */
public interface SqlProcessor{
	/**
	 * 形成count语句
	 * @param sql SQL语句
	 */
	public String toCountSql(String sql)  throws SQLException;
	/**
	 * 转换为Where子句 
	 * @param obj
	 * @param context SQL语句上下文
	 * @param update  是否在更新时
	 */
	public BindSql toPrepareWhereSql(JoinElement obj,SqlContext context,boolean update,DatabaseDialect profile) ;
	
	/**
	 * 转换为Where子句
	 * @param joinElement
	 * @param context SQL语句上下文
	 * @return
	 */
	public BindSql toWhereClause(JoinElement joinElement,SqlContext context,boolean update,DatabaseDialect profile);
	
	
	
	/**
	 *  收集结果数据中的数据，用需要的容器包装并返回
	 *  @param subs
	 *  @param container
	 *  @param targetField
	 *  @return
	 */
	public Object collectValueToContainer(List<? extends IQueryableEntity> subs, Class<?> container, String targetField);
	/**
	 * 获取数据库Dialect
	 * @return
	 * @deprecated
	 */
	public DatabaseDialect getProfile();
	public DatabaseDialect getProfile(PartitionResult[] prs);
	public PartitionSupport getPartitionSupport();
}