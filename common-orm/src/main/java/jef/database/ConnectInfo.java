package jef.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.inject.Provider;
import javax.persistence.PersistenceException;

import jef.database.datasource.DataSourceInfo;
import jef.database.dialect.AbstractDialect;
import jef.database.dialect.DatabaseDialect;
import jef.tools.Assert;

/**
 * 描述数据库连接的基本信息
 * 
 * @author Administrator
 *
 */
public class ConnectInfo {
	// 三项基本信息
	String url;
	String user;
	String password;
	// 三项高级信息
	DatabaseDialect profile;
	String dbname;
	String host;

	/**
	 * 获得JDBC地址
	 * 
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * 设置地址
	 * 
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * 获得用户名
	 * 
	 * @return
	 */
	public String getUser() {
		return user;
	}

	/**
	 * 设置用户名
	 * 
	 * @param user
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * 获得口令
	 * 
	 * @return
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * 设置口令
	 * 
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * 获得方言
	 * 
	 * @return database dialect
	 */
	public DatabaseDialect getProfile() {
		return profile;
	}

	/**
	 * 获得数据库名
	 * 
	 * @return 数据库名
	 */
	public String getDbname() {
		return dbname;
	}

	/**
	 * 设置数据库名
	 * 
	 * @param dbname
	 */
	public void setDbname(String dbname) {
		if (profile != null) {
			dbname = profile.getObjectNameToUse(dbname);
		}
		this.dbname = dbname;
	}

	/**
	 * 获得数据库地址
	 * 
	 * @return
	 */
	public String getHost() {
		return host;
	}

	/**
	 * 设置数据库地址
	 * 
	 * @param host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("type=").append(profile != null ? profile.getName() : null);
		sb.append("\thost=").append(host);
		sb.append("\tdb=").append(dbname);
		return sb.toString();
	}

	/**
	 * 
	 * 目前已知的所有JDBC URL开头和驱动之间的关系 jdbc:derby org.apache.derby.jdbc.EmbeddedDriver
	 * jdbc:mysql com.mysql.jdbc.Driver jdbc:oracle
	 * oracle.jdbc.driver.OracleDriver jdbc:microsoft
	 * com.microsoft.jdbc.sqlserver.SQLServerDriver jdbc:sybase:Tds
	 * com.sybase.jdbc2.jdbc.SybDriver jdbc:jtds
	 * net.sourceforge.jtds.jdbc.Driver jdbc:postgresql org.postgresql.Driver
	 * jdbc:hsqldb org.hsqldb.jdbcDriver jdbc:db2 COM.ibm.db2.jdbc.app.DB2Driver
	 * DB2的JDBC Driver十分混乱，这个匹配不一定对 jdbc:sqlite org.sqlite.JDBC jdbc:ingres
	 * com.ingres.jdbc.IngresDriver jdbc:h2 org.h2.Driver jdbc:mckoi
	 * com.mckoi.JDBCDriver jdbc:cloudscape COM.cloudscape.core.JDBCDriver
	 * jdbc:informix-sqli com.informix.jdbc.IfxDriver jdbc:timesten
	 * com.timesten.jdbc.TimesTenDriver jdbc:as400
	 * com.ibm.as400.access.AS400JDBCDriver jdbc:sapdb
	 * com.sap.dbtech.jdbc.DriverSapDB jdbc:JSQLConnect
	 * com.jnetdirect.jsql.JSQLDriver jdbc:JTurbo
	 * com.newatlanta.jturbo.driver.Driver jdbc:firebirdsql
	 * org.firebirdsql.jdbc.FBDriver jdbc:interbase interbase.interclient.Driver
	 * jdbc:pointbase com.pointbase.jdbc.jdbcUniversalDriver jdbc:edbc
	 * ca.edbc.jdbc.EdbcDriver jdbc:mimer:multi1 com.mimer.jdbc.Driver
	 * 
	 * @return
	 */
	DatabaseDialect parse() {
		Assert.notNull(url);
		int start = url.indexOf("jdbc:");
		if (start == -1) {
			throw new IllegalArgumentException("The jdbc url [" + url + "] cann't be recognized.");
		}
		try {
			int end = url.indexOf(':', start + 5);
			String dbType = url.substring(start + 5, end);
			profile = AbstractDialect.getDialect(dbType); // 传入时会自动转为小写
			if (profile == null) {
				throw new PersistenceException("database not supported:" + dbType);
			}
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("The jdbc url [" + url + "] is invalid!");
		}
		if (url.length() > 0)
			profile.parseDbInfo(this);
		return profile;
	}
	
	
	/**
	 * 根据datasource解析连接信息
	 * 
	 * @param ds
	 * @param updateDataSourceProperties
	 *            在能解析出ds的情况下，向datasource的连接属性执行注入
	 * @return
	 * @throws SQLException
	 */
	public static ConnectInfo tryAnalyzeInfo(Provider<Connection> ds, boolean updateDataSourceProperties) {
		if (ds instanceof DataSourceInfo) {
			DataSourceInfo dsw = (DataSourceInfo) ds;
			ConnectInfo info = new ConnectInfo();
			DbUtils.processDataSourceOfEnCrypted(dsw);

			info.url = dsw.getUrl();
			info.user = dsw.getUser();
			info.password = dsw.getPassword();
			DatabaseDialect profile = info.parse();// 解析，获得profile, 解析出数据库名等信息
			if (updateDataSourceProperties)
				profile.processConnectProperties(dsw);
			return info;// 理想情况
		}
		return null;
	}
	
	/**
	 * 根据已有的连接解析连接信息
	 * 
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public static ConnectInfo tryAnalyzeInfo(Connection conn) throws SQLException {
		DatabaseMetaData meta = conn.getMetaData();
		ConnectInfo info = new ConnectInfo();
		info.user = meta.getUserName();
		info.url = meta.getURL();
		info.parse();// 解析，获得profile, 解析出数据库名等信息
		return info;
	}
}