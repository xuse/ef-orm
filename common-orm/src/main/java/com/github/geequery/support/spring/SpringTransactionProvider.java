/*
 * Copyright 2015, The Querydsl Team (http://www.querydsl.com/team)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.geequery.support.spring;

import java.sql.Connection;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jef.database.Transaction.TransactionFlag;
import jef.database.datasource.RoutingDataSource;
import jef.database.innerpool.JDBCRoutingConnection;
import jef.tools.StringUtils;

/**
 * <H3>用途</H3> 当使用
 * Spring或者外部JTA等事务托管机制时，所有的连接均由托管框架负责开启、setAutoCommit状态、setReadOnly、comiit、Rollback、close。
 * 此类支持Spring的DataSourceTransactionManager。用于解析Spring事务状态并获取连接。
 * 
 * <H3>基于JDBC事务的多连接事务</H3> 当多数据源时，将多个连接模拟成单个连接，由Spring进行事务管理（JDBC模式）。
 * 显然在这种模式下，不能严格意义上确保事务一致性，（取决与将多个连接模拟成单个连接的实现{@link JDBCRoutingConnection}）。
 * 该实现的方式目前是顺序执行多库提交而不是两阶段提交。 大部分情况下，commit并不容易发生数据库错误。因此一致性基本可以保证。
 * 使用两阶段提交的方法可以严格保证一致性，但在现代Java应用开发中基本趋于淘汰。
 * 
 * <H3>非托管事务下的操作支持</H3>
 * 大部分ORM框架如某H和某M在和Spring集成时，如果使用事务托管模式，那么不在Spring事务下直接抛出异常。
 * 本框架设计中为了简化使用，不在@Transactional状态下，将会获得一个开启了AutoCommit特性的连接。而框架在每次数据库操作后都会调用close()方法以释放回连接池。
 * 也就是说，其实本类支持了托管方式下的事务和非托管方式下的数据库操作。
 * 
 * 作者自读：理论上，对于非事务托管连接，使用本ORM框架的startTransaction方法也可以进行编程式事务操作，但目前为了防止意外情况禁止了此种操作方式。
 * 是否要修改后续再论证。
 * 
 * <H3>关于JTA支持</H3> 1.12版使用JPA/JDBC等多种方式管理事务，1.13作了简化，因此目前是不支持JTA的。
 * 如果真的要开启JTA支持，可以考虑自行编写一个JTARoutingConnection（参考JDBCRoutingConnection）。
 * 同时对应的TransactionProvider需要重写。在放弃了框架管理事务、Spring管理事务之后，转为JTA事务管理。
 * 目前设计思路是将事务管理机制从框架核心中剔除，交由外围的Provider来支持。因此通过编写Provider并覆盖Connection应该可以实现。
 * 
 * <p>
 * Usage example
 * </p>
 * 
 * <pre>
 * {
 * 	&#64;code
 * 	Provider<Connection> provider = new MultDsSpringTransactionProvider(dataSource());
 * }
 * </pre>
 * 
 * @see JDBCRoutingConnection
 */
public class SpringTransactionProvider implements Provider<Connection>, TransactionProvider {

	private final DataSource dataSource;

	public SpringTransactionProvider(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Connection get() {
		Connection connection = DataSourceUtils.getConnection(dataSource);
		if (!DataSourceUtils.isConnectionTransactional(connection, dataSource)) {
			return connection;
		} else {
			return new ManagedConnection(connection);
		}

	}

	@Override
	public String getTransactionId(String parentName) {
		ConnectionHolder conn = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
		StringBuilder sb = new StringBuilder();
		if (conn != null && conn.isSynchronizedWithTransaction()) {
			sb.append('[').append(TransactionFlag.Managed.name());
			if (conn.isRollbackOnly()) {
				sb.append("(R)");
			}
			sb.append(StringUtils.toFixLengthString(conn.hashCode(), 8)).append('@').append(parentName).append('@').append(Thread.currentThread().getId()).append(']');
		} else {
			sb.append('[').append(parentName).append('@').append(Thread.currentThread().getId()).append(']');
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "SpringTransactionProvider [dataSource=" + dataSource + "]";
	}

	@Override
	public boolean isRouting() {
		return dataSource instanceof RoutingDataSource;
	}

	@Override
	public RoutingDataSource getRoudingDataSource() {
		if (dataSource instanceof RoutingDataSource) {
			return (RoutingDataSource) dataSource;
		}
		throw new UnsupportedOperationException();
	}
}