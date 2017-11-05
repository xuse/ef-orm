package jef.database.datasource;

import javax.sql.DataSource;

import jef.database.DbUtils;
import jef.database.dialect.AbstractDialect;
import jef.database.dialect.DatabaseDialect;

/**
 * DataSource工具类
 * 
 * @author jiyi
 * 
 */
public class DataSources {
	private DataSources() {
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
		DatabaseDialect profile = AbstractDialect.getDialect(dbType);
		if (profile == null) {
			throw new UnsupportedOperationException("The database {" + dbType + "} was not supported yet..");
		}
		String url = profile.generateUrl(host, port, database);
		return DbUtils.createSimpleDataSource(url, user, password);
	}
}
