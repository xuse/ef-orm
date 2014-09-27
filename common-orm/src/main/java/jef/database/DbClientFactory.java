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

import java.io.File;
import java.sql.SQLException;

import javax.sql.DataSource;

import jef.database.dialect.DbmsProfile;
import jef.tools.JefConfiguration;

import org.easyframe.enterprise.spring.TransactionMode;

/**
 * 提供了创建DbClient的若干工厂方法
 * 
 * @author Administrator
 * 
 */
public class DbClientFactory {
	private DbClientFactory() {
	}

	/**
	 * 获得实例
	 * 
	 * @param dbType
	 * @param host
	 * @param port
	 * @param localpath
	 * @param dbName
	 * @param user
	 * @param pass
	 * @return
	 * @throws SQLException
	 */
	public static DbClient getDbClient(String dbType, String host, int port, String pathOrName, String user, String pass, int maxConnection) throws SQLException {
		DbmsProfile profile = DbmsProfile.getProfile(dbType);
		if (profile == null) {
			throw new SQLException("The DBMS:[" + dbType + "] is not supported yet.");
		}
		String dbURL = profile.generateUrl(host, port, pathOrName);
		if (maxConnection == 0)
			maxConnection = JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL_MAX, 50);
		DbClient db = new DbClient(dbURL, user, pass, maxConnection);
		return db;
	}

	/**
	 * 获得实例
	 * @param dbType
	 * @param host
	 * @param port
	 * @param pathOrName
	 * @param user
	 * @param pass
	 * @param maxConnection
	 * @return
	 * @throws SQLException 
	 */
	public static DbClient getDbClient(String dbType, String host, int port, String pathOrName, String user, String pass) throws SQLException{
		return getDbClient(dbType, host, port, pathOrName, user, pass,0);
	}
	
	
	/**
	 * 工厂方法获得数据库实例（本地）
	 * 
	 * @param dbName
	 *            数据库名
	 * @param user
	 *            用户
	 * @param pass
	 *            密码
	 * @return
	 * @throws SQLException
	 */
	public static DbClient getDbClient(String dbType, File dbFolder, String user, String pass) throws SQLException {
		int port = JefConfiguration.getInt(DbCfg.DB_PORT, 0);
		String host = JefConfiguration.get(DbCfg.DB_HOST, "");
		return getDbClient(dbType, host, port, dbFolder.getAbsolutePath(), user, pass, 0);
	}

	/**
	 * 使用默认参数获得的静态工厂方法(singleton)
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static DbClient getDbClient() throws SQLException {
		return new DbClient();
	}

	/**
	 * 使用DataSource获得
	 * 
	 * @param ds
	 * @return
	 * @throws SQLException
	 */
	public static DbClient getDbClient(DataSource ds) throws SQLException {
		return getDbClient(ds, null);
	}

	/**
	 * 根据JDBC连接字符串和用户名密码得到
	 * 
	 * @param ds
	 * @return
	 * @throws SQLException
	 */
	public static DbClient getDbClient(String jdbcUrl, String user, String password) throws SQLException {
		return getDbClient(DbUtils.createSimpleDataSource(jdbcUrl, user, password), null);
	}

	/**
	 * 使用数据源和是否XA数据源的标志位
	 * 
	 * @param ds
	 * @return
	 * @throws SQLException
	 */
	public static DbClient getDbClient(DataSource ds, TransactionMode isXA) throws SQLException {
		return new DbClient(ds,JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL_MAX, 50),isXA);
	}
}
