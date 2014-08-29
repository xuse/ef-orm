package jef.database.datasource;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.DbmsProfile;
import jef.tools.StringUtils;

/**
 * DataSource工具类
 * 
 * @author jiyi
 * 
 */
public class DataSources {
	private static String[] DataSourceClz = { "org.logicalcobwebs.proxool.ProxoolDataSource", "com.mchange.v2.c3p0.ComboPooledDataSource", "org.apache.commons.dbcp.BasicDataSource", "org.springframework.jdbc.datasource.AbstractDriverBasedDataSource",
			"com.alibaba.druid.pool.DruidAbstractDataSource", "com.jolbox.bonecp.BoneCPDataSource", "org.apache.tomcat.jdbc.pool.DataSource" };

	private static String[] adapterClz = { "jef.database.datasource.ProxoolDataSourceWrapper", "jef.database.datasource.C3p0DataSourceWrapper", "jef.database.datasource.DbcpDataSourceWrapper", "jef.database.datasource.DriverManagerDataSourceWrapper",
			"jef.database.datasource.DruidDataSourceWrapper", "jef.database.datasource.BoneCpWrapper", "jef.database.datasource.TomcatCpWrapper" };

	private DataSources() {
	}

	/**
	 * 将给出的DataSource包装为DataSourceWrapper对象
	 * DataSourceWrapper对象能够提供各种DataSource配置信息的获取和修改 这个功能目前能识别Spring,Apache
	 * DBCP,c3p0,Druid, Proxool等若干框架的DataSource
	 * 
	 * @param datasource
	 * @return 如果无法包装将返回null
	 * @see DataSourceWrapper
	 * @see DataSourceInfo
	 */
	public static DataSourceWrapper wrapFor(DataSource datasource) {
		Class<? extends DataSource> iface = datasource.getClass();
		if (datasource instanceof DataSourceWrapper) {
			return (DataSourceWrapper) datasource;
		}
		
		for (int index=0;index<DataSourceClz.length;index++) {
			try {
				String s=DataSourceClz[index];
				if (isAssiableFrom(iface, s)) {
					Class<?> c = Class.forName(adapterClz[index]);
					DataSourceWrapper wrapper = (DataSourceWrapper) c.newInstance();
					wrapper.setWrappedDataSource(datasource);
					return wrapper;
				}
			} catch (NoClassDefFoundError e) {
				// 不用任何输出
			} catch (ClassNotFoundException e) {
				// 不用任何输出
			} catch (Exception e) {
				LogUtil.exception(e);
			}
		}
		if (System.getProperty("no.refelcting.datasource.wrapper") == null) {
			try {
				RefectingHackDataSourceWrapper w = new RefectingHackDataSourceWrapper(datasource);
				LogUtil.warn(StringUtils.concat("Unknown datasource type:", datasource.getClass().getName(), " Using refelcting datasource wrapper to access it. "));
				return w;
			} catch (Exception e) {
			}
		}
		return null;
	}

	private static boolean isAssiableFrom(Class<? extends DataSource> iface, String s) {
		Class<?> clz=iface;
		while(clz!=Object.class && clz!=null){
			if(s.equals(clz.getName())){
				return true;
			}
			clz=clz.getSuperclass();
		}
		return false;
	}

	public static DataSource getAsDataSource(DataSourceInfo dsi) {
		if (dsi != null) {
			if (dsi instanceof DataSource) {
				return (DataSource) dsi;
			} else {
				return new SimpleDataSource(dsi);
			}
		}
		return null;
	}

	public static boolean isPool(DataSource ds) {
		if (ds instanceof ConnectionPoolDataSource) {// 如果是JDK标准的连接池直接返回true
			return true;
		}

		DataSourceWrapper dsw = DataSources.wrapFor(ds);

		if (dsw == null) {// 無法包裝就認為不是池
			return false;
		}
		return dsw.isConnectionPool();
	}

	/**
	 * 按给出的数据创建DataSource
	 * 
	 * @param dbType
	 * @param host
	 * @param port
	 * @param database
	 * @param user
	 * @param password
	 * @return
	 */
	public static DataSource create(String dbType, String host, int port, String database, String user, String password) {
		DatabaseDialect profile = DbmsProfile.getProfile(dbType);
		if (profile == null) {
			throw new UnsupportedOperationException("The database {" + dbType + "} was not supported yet..");
		}
		String url = profile.generateUrl(host, port, database);
		return DbUtils.createSimpleDataSource(url, user, password);
	}
}
