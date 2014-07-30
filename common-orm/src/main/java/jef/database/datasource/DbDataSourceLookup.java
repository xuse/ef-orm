package jef.database.datasource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.tools.Assert;
import jef.tools.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 到数据库里去取的数据源的配置信息。以此进行数据源的查找。
 * 
 * <p/>
 * 如果利用一个配置库来维护其他各种数据库的连接信息，那么系统会到这个数据库中去寻找数据源
 * 
 * 这个类同时实现了DataSourceInfoLookup和DataSourceLookup两个接口。在spring中配置的例子如下
 * 
 * <pre>
 * <code>&lt;bean class="jef.database.datasource.DbDataSourceLookup"
 * p:configDataSource-ref="dataSource" 
 * p:configDbTable="DATASOURCE_CONFIG"
 * p:whereCondition="enable='1'"
 * p:columnOfId="DATABASE_NAME"
 * p:columnOfUrl="JDBC_URL"
 * p:columnOfUser="DB_USER"
 * p:columnOfPassword="DB_PASSWORD"
 * p:columnOfDriver=""
 * p:datasourceIdOfconfigDB="" 
 * p:defaultDsName="" &gt;
 *  &lt;property name="passwordDecryptor"&gt;
 *   &lt;!-- 自定义的数据库口令解密器 --&gt;
 *   &lt;bean class="org.googlecode.jef.spring.MyPasswordDecryptor" /&gt;
 *  &lt;/property&gt;
 * &lt;/bean&gt;	
 * </code>
 * </pre>
 * 
 * @author jiyi
 * 
 */
public class DbDataSourceLookup implements DataSourceInfoLookup, DataSourceLookup {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	// 配置库的信息(和配置信息二选一)
	private String configDbUrl;
	private String configDbUser;
	private String configDbPassword;
	// 配置库的连接(和配置信息二选一)
	private DataSource configDataSource;

	// 配置库中的其他信息
	private String configDbTable;
	private String whereCondition;// where条件
	private String columnOfId;
	private String columnOfUrl;
	private String columnOfUser;
	private String columnOfPassword;
	private String columnOfDriver;

	private boolean ignoreCase=true;
	// 配置如果也作为数据源之一，那么需要设置ID
	private String datasourceIdOfconfigDB;
	//允许用户指定缺省数据源的名称
	protected String defaultDsName;
	// 解密器
	private PasswordDecryptor passwordDecryptor = PasswordDecryptor.DUMMY;
	// 缓存
	private Map<String, DataSourceInfo> cache;

	private int maxCache = 100;// 为了防止数据库里配置了即大量的数据源，首次查询只会返回限定的记录条数

	public DataSourceInfo getDataSourceInfo(String dataSourceName) {
		if (cache == null) {
			initCache();
		}
		DataSourceInfo dsi = cache.get(ignoreCase?dataSourceName.toLowerCase():dataSourceName);
		if (dsi == null) {
			dsi = tryLoad(dataSourceName);
		}
		return dsi;
	}

	public DataSource getDataSource(String dataSourceName) {
		return DataSources.getAsDataSource(getDataSourceInfo(dataSourceName));
	}

	private DataSourceInfo tryLoad(String dataSourceName) {
		DataSource configDs = getConfigDS();
		DataSourceInfo dsi = process(cache, configDs, dataSourceName);
		return dsi;
	}

	private synchronized void initCache() {
		Map<String, DataSourceInfo> result = new HashMap<String, DataSourceInfo>();
		DataSource configDs = getConfigDS();
		process(result, configDs, null);
		// 将配置源也加入
		if (StringUtils.isNotEmpty(datasourceIdOfconfigDB)) {
			DataSourceInfo dsi = DataSources.wrapFor(configDs);
			if (dsi != null) {
				put(datasourceIdOfconfigDB.trim(), dsi,result);
			} else {
				log.warn("The Configuration datasource {} Can not be wrapped.",configDs.getClass());
			}
		}
		this.cache=result;
	}

	private DataSourceInfo process(Map<String, DataSourceInfo> result, DataSource configDs, String key) {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		DataSourceInfo dsi = null;
		try {
			conn = configDs.getConnection();
			st = conn.createStatement();
			String sql = getSql(key);
			LogUtil.show("Executing:" + sql);
			st.setMaxRows(maxCache);
			rs = st.executeQuery(sql);
			int count = 0;
			while (rs.next()) {
				count++;
				String id = rs.getString(columnOfId);
				String url = rs.getString(columnOfUrl);
				String user = StringUtils.isEmpty(columnOfUser) ? null : rs.getString(columnOfUser);
				String password = StringUtils.isEmpty(columnOfPassword) ? null : rs.getString(columnOfPassword);
				String driver = StringUtils.isEmpty(columnOfDriver) ? null : rs.getString(columnOfDriver);
				dsi = createDsi(url, user, password, driver);
				if (StringUtils.isNotEmpty(id) && dsi != null) {
					put(id, dsi,result);
				}
			}
			LogUtil.show("got " + count + " datasource from database.");

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			DbUtils.close(rs);
			DbUtils.close(st);
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					LogUtil.exception(e);
				}
			}
		}
		return dsi;
	}

	private void put(String id, DataSourceInfo dsi,Map<String,DataSourceInfo> map) {
		if(ignoreCase)id=id.toLowerCase();
		map.put(id, dsi);
		log.info("Datasource {} was resoloved.",id, dsi);
	}

	private DataSourceInfo createDsi(String url, String user, String password, String driver) {
		if (StringUtils.isEmpty(url))
			return null;
		DataSourceInfo dsi = new DataSourceInfoImpl();
		dsi.setUrl(url);
		dsi.setUser(user);
		dsi.setPassword(passwordDecryptor.decrypt(password));
		dsi.setDriverClass(driver);
		return dsi;
	}

	private String getSql(String id) {
		Assert.isNotEmpty(columnOfId);
		Assert.isNotEmpty(columnOfUrl);
		Assert.isNotEmpty(configDbTable);
		StringBuilder sb = new StringBuilder("select ");
		sb.ensureCapacity(128);
		sb.append(columnOfId).append(',').append(columnOfUrl);
		if (StringUtils.isNotEmpty(columnOfUser)) {
			sb.append(',').append(columnOfUser);
		}
		if (StringUtils.isNotEmpty(columnOfPassword)) {
			sb.append(',').append(columnOfPassword);
		}
		if (StringUtils.isNotEmpty(columnOfDriver)) {
			sb.append(',').append(columnOfDriver);
		}
		sb.append(" from ").append(configDbTable);
		if (StringUtils.isNotEmpty(whereCondition)) {
			if (id == null) {
				sb.append(" where ").append(whereCondition);
			} else {
				sb.append(" where (").append(whereCondition).append(") and ").append(columnOfId).append("='").append(id).append('\'');
			}
		} else {
			if (id != null) {
				sb.append(" where ").append(columnOfId).append("='").append(id).append('\'');
			}
		}
		return sb.toString();
	}

	private DataSource getConfigDS() {
		if (configDataSource != null) {
			return configDataSource;
		}
		configDataSource = DbUtils.createSimpleDataSource(configDbUrl, configDbUser, configDbPassword);
		return configDataSource;
	}

	public void setPasswordDecryptor(PasswordDecryptor passwordDecryptor) {
		this.passwordDecryptor = passwordDecryptor;
	}

	/**
	 * 配置库的连接串<br/>
	 * 有两种方式可以指定配置库的连接信息，一种是直接指定DataSource对象
	 * {@link #setConfigDataSource(DataSource)}。 另一种是通过
	 * {@link #setConfigDbUrl(String)} {@link #setConfigDbPassword(String)}
	 * {@link #setConfigDbUser(String)}三个方法来提供配置库的信息。两种方法都可以，其中
	 * {@link #setConfigDataSource(DataSource)优先}
	 * 
	 * @param configDbUrl
	 */
	public void setConfigDbUrl(String configDbUrl) {
		this.configDbUrl = configDbUrl;
	}

	/**
	 * 配置库的用户名<br/>
	 * 有两种方式可以指定配置库的连接信息，一种是直接指定DataSource对象
	 * {@link #setConfigDataSource(DataSource)}。 另一种是通过
	 * {@link #setConfigDbUrl(String)} {@link #setConfigDbPassword(String)}
	 * {@link #setConfigDbUser(String)}三个方法来提供配置库的信息。两种方法都可以，其中
	 * {@link #setConfigDataSource(DataSource)优先}
	 * 
	 * @param configDbUser
	 */
	public void setConfigDbUser(String configDbUser) {
		this.configDbUser = configDbUser;
	}

	/**
	 * 配置库的密码（必须明文，不支持密文）<br>
	 * 有两种方式可以指定配置库的连接信息，一种是直接指定DataSource对象
	 * {@link #setConfigDataSource(DataSource)}。 另一种是通过
	 * {@link #setConfigDbUrl(String)} {@link #setConfigDbPassword(String)}
	 * {@link #setConfigDbUser(String)}三个方法来提供配置库的信息。两种方法都可以，其中
	 * {@link #setConfigDataSource(DataSource)优先}
	 * 
	 * @param configDbPassword
	 */
	public void setConfigDbPassword(String configDbPassword) {
		this.configDbPassword = configDbPassword;
	}

	/**
	 * 配置库的数据源 有两种方式可以指定配置库的连接信息，一种是直接指定DataSource对象，即本方法。 另一种是通过
	 * {@link #setConfigDbUrl(String)} {@link #setConfigDbPassword(String)}
	 * {@link #setConfigDbUser(String)}三个方法来提供配置库的信息。两种方法都可以，其中
	 * {@link #setConfigDataSource(DataSource)}优先
	 * 
	 * @param configDataSource
	 */
	public void setConfigDataSource(DataSource configDataSource) {
		this.configDataSource = configDataSource;
	}

	/**
	 * 查询数据源时的表名（视图名）
	 * 
	 * @param configDbTable
	 */
	public void setConfigDbTable(String configDbTable) {
		this.configDbTable = configDbTable;
	}

	/**
	 * 查询数据源时的where条件，可不设置(查全表) 配置时注意只要配条件，不需要写 ‘where’举例：
	 * 
	 * <pre>
	 *    enable=1 AND type='oracle'
	 * </pre>
	 * 
	 * @param whereCondition
	 */
	public void setWhereCondition(String whereCondition) {
		this.whereCondition = whereCondition;
	}

	/**
	 * 数据源唯一标志 列的名称
	 * 
	 * @param columnOfId
	 */
	public void setColumnOfId(String columnOfId) {
		this.columnOfId = columnOfId;
	}

	/**
	 * 数据源连接串 列的名称
	 * 
	 * @param columnOfUrl
	 */
	public void setColumnOfUrl(String columnOfUrl) {
		this.columnOfUrl = columnOfUrl;
	}

	/**
	 * 数据源用户名 列的名称
	 * 
	 * @param columnOfUser
	 */
	public void setColumnOfUser(String columnOfUser) {
		this.columnOfUser = columnOfUser;
	}

	/**
	 * 数据源password 列的名称。
	 * 
	 * @param columnOfPassword
	 */
	public void setColumnOfPassword(String columnOfPassword) {
		this.columnOfPassword = columnOfPassword;
	}

	/**
	 * 数据源DriverClassName列的名称。允许为null
	 * 
	 * @param columnOfDriver
	 */
	public void setColumnOfDriver(String columnOfDriver) {
		this.columnOfDriver = columnOfDriver;
	}

	/**
	 * 如果配置数据源的库本身也作为数据库之一，那么需要为其设置一个DataSource的ID
	 * 
	 * @param datasourceIdOfconfigDB
	 */
	public void setDatasourceIdOfconfigDB(String datasourceIdOfconfigDB) {
		this.datasourceIdOfconfigDB = StringUtils.trimToNull(datasourceIdOfconfigDB);
	}

	
	public String getDefaultDsName() {
		return defaultDsName;
	}

	public void setDefaultDsName(String defaultBeanName) {
		this.defaultDsName = StringUtils.trimToNull(defaultBeanName);
	}

	public String getDefaultKey() {
		if (cache == null) {
			initCache();
		}
		if (cache.size() == 1) {
			return cache.keySet().iterator().next();
		}
		if(defaultDsName==null){
			log.warn("You have not assign the default datasource name.");
			defaultDsName=datasourceIdOfconfigDB;
		}
		return defaultDsName;
	}

	public Collection<String> getAvailableKeys() {
		if (cache == null) {
			initCache();
		}
		return Collections.unmodifiableCollection(cache.keySet());
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}

	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}
	
	
}
