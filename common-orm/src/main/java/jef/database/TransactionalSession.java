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
import java.sql.Savepoint;

import jef.database.Transaction.TransactionFlag;
import jef.database.support.SavepointNotSupportedException;

/**
 * 事务操作接口
 * 
 * @author jiyi
 * 
 */
public interface TransactionalSession{
	/**
	 * 提交事务
	 * 
	 * @param flag
	 *            提交后关闭
	 */
	void commit(boolean flag);

	/**
	 * 回滚事务
	 * 
	 * @param flag
	 *            回滚后关闭
	 */
	void rollback(boolean flag);

	/**
	 * 设置是否为仅可回滚
	 * 
	 * @param b
	 *            设置事务是否为仅可回滚
	 */
	void setRollbackOnly(boolean b);

	/**
	 * 是否为仅可回滚
	 * 
	 * @return 事务被标记为仅可回滚时，返回true
	 */
	boolean isRollbackOnly();

	/**
	 * 当前连接是否开启
	 * 
	 * @return 事务可用返回true。
	 */
	boolean isOpen();

	/**
	 * 建立保存点
	 * 
	 * @param savepointName
	 *            保存点名
	 * @return 保存点
	 * @throws SQLException
	 */
	Savepoint setSavepoint(String savepointName) throws SQLException, SavepointNotSupportedException;

	/**
	 * 建立恢复点
	 * 
	 * @return
	 * @throws SQLException
	 * @throws SavepointNotSupportedException
	 */
	public Savepoint setSavepoint() throws SQLException, SavepointNotSupportedException;

	/**
	 * 回滚到保存点
	 * 
	 * @param savepoint
	 *            保存点
	 * @throws SQLException
	 */
	void rollbackToSavepoint(Savepoint savepoint) throws SQLException;

	/**
	 * 释放回滚点
	 * 
	 * @param savepoint
	 *            保存点
	 */
	void releaseSavepoint(Savepoint savepoint) throws SQLException;

	/**
	 * 得到当前事务的内部标记。
	 * 
	 * @return 事务内部标记
	 * @see TransactionFlag
	 */
	TransactionFlag getTransactionFlag();

	/**
	 * 当前连接是否为自动提交状态（自动提交状态的即非事务）
	 * 
	 * @return
	 */
	boolean isAutoCommit();

	/**
	 * 设置当前连接的自动提交状态，如果设置为true则相当于无事务
	 * 
	 * @param autoCommit
	 * @return
	 */
	Transaction setAutoCommit(boolean autoCommit);

	/**
	 * 设置只读
	 * @param flag
	 */
	void setReadonly(boolean flag);

	/**
	 * 当前事务是否为只读事务
	 * 
	 * @return 如果是只读事务返回true。否则false
	 */
	boolean isReadonly();

	/**
	 * 获得当前事务的数据库隔离级别。要求JDBC驱动能支持
	 * 
	 * @return
	 */
	int getIsolationLevel();

	/**
	 * 设置当前事务的数据库隔离级别。要求JDBC驱动能支持
	 * 
	 * @param isolationLevel
	 */
	void setIsolationLevel(int isolationLevel);

	/**
	 * 关闭连接
	 */
	public abstract void close();

	/**
	 * 获取指定数据源的操作对象
	 * 
	 * @param dbKey 数据源名称
	 * 
	 * @return 指定数据源为dbKey的数据库操作对象
	 */
	abstract OperateTarget selectTarget(String dbKey);

}
