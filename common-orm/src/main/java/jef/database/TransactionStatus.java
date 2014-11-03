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

/**
 * 事务操作接口
 * 
 * @author jiyi
 * 
 */
public interface TransactionStatus {
	/**
	 * 提交事务
	 * @param flag 提交后关闭
	 */
	public void commit(boolean flag);

	/**
	 * 回滚事务
	 * @param flag 回滚后关闭
	 */
	public void rollback(boolean flag);

	/**
	 * 设置是否为仅可回滚
	 * @param b 设置事务是否为仅可回滚
	 */
	public void setRollbackOnly(boolean b);

	/**
	 * 是否为仅可回滚
	 * @return 事务被标记为仅可回滚时，返回true
	 */
	public boolean isRollbackOnly();

	/**
	 * 当前连接是否开启
	 * @return 事务可用返回true。
	 */
	public boolean isOpen();

	/**
	 * 建立保存点
	 * @param savepointName 保存点名
	 * @return 保存点
	 * @throws SQLException
	 */
	public Savepoint setSavepoint(String savepointName) throws SQLException;

	/**
	 * 回滚到保存点
	 * @param savepoint 保存点
	 * @throws SQLException
	 */
	public void rollbackToSavepoint(Savepoint savepoint) throws SQLException;

	/**
	 * 释放回滚点
	 * @param savepoint 保存点
	 */
	public void releaseSavepoint(Savepoint savepoint) throws SQLException;
	
	/**
	 * 得到当前事务的内部标记。
	 * @return 事务内部标记
	 * @see TransactionFlag 
	 */
	public TransactionFlag getTransactionFlag();
}
