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
import java.sql.Statement;
import java.util.Date;

import jef.database.Session.PopulateStrategy;
import jef.database.jdbc.result.IResultSet;
import jef.database.support.SqlLog;

/**
 * 直接对JDBC结果集进行操作的转换器
 * 
 * @author jiyi
 * 
 * @param <T>
 */
public interface ResultSetExtractor<T> {
	/**
	 * 将结果集转换为需要的类型
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	T transformer(IResultSet rs) throws SQLException;

	/**
	 * 设置查询参数
	 * @param maxRows 最大返回记录数
	 * @return this
	 */
	ResultSetExtractor<T> setMaxRows(int maxRows);

	/**
	 *  设置查询参数
	 * @param fetchSize 每批获取数
	 * @return this
	 */
	ResultSetExtractor<T> setFetchSize(int fetchSize);

	/**
	 *  设置查询参数
	 * @param timeout 查询超时
	 * @return this
	 */
	ResultSetExtractor<T> setQueryTimeout(int timeout);

	/**
	 * Apply 3 parameters
	 * @param st
	 * @throws SQLException
	 */
	void apply(Statement st) throws SQLException;

	/**
	 * 返回结果拼装的策略特性。
	 * @return
	 */
	PopulateStrategy[] getStrategy();
	
	/**
	 * 转换完成后是否要关闭结果集，释放资源。
	 * 正常情况下应该返回true。外部程序判断此方法为true后，即释放资源。
	 * 如果是Iterator的形式返回结果，此时资源不能释放。
	 * @return 结果转换完成后可以释放资源返回true，不能释放资源返回false。
	 */
	boolean autoClose();
	
	/**
	 * 向日志对象写入输出信息
	 * @param log
	 */
	void appendLog(SqlLog log,T result);

	public static final ResultSetExtractor<Long> GET_FIRST_LONG = new AbstractResultSetTransformer<Long>() {
		public Long transformer(IResultSet rs) throws SQLException {
			if (rs.next()) {
				return rs.getLong(1);
			} else {
				throw new SQLException("Result incorrect.count result must not be empty.");
			}
		}

		@Override
		public void appendLog(SqlLog log, Long result) {
			if(result!=null){
				log.append("Count:",result);
			}
//			if (debug) {
//				long dbAccess = System.currentTimeMillis();
//				LogUtil.show(StringUtils.concat("Count:", String.valueOf(total), "\t ([DbAccess]:", String.valueOf(dbAccess - start), "ms) |", db.getTransactionId()));
//			}
		}
		
		
	}.setMaxRows(1);

	public static final ResultSetExtractor<Integer> GET_FIRST_INT = new AbstractResultSetTransformer<Integer>() {
		public Integer transformer(IResultSet rs) throws SQLException {
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new SQLException("Result incorrect.count result must not be empty.");
			}
		}
	}.setMaxRows(1);

	public static final ResultSetExtractor<Date> GET_FIRST_TIMESTAMP = new AbstractResultSetTransformer<Date>() {
		public Date transformer(IResultSet rs) throws SQLException {
			if (rs.next()) {
				java.sql.Timestamp ts = rs.getTimestamp(1);
				return new java.util.Date(ts.getTime());
			} else {
				throw new SQLException("Result incorrect.count result must not be empty.");
			}
		}
	};
}
