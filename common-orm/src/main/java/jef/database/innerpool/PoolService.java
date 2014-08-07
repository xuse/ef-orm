package jef.database.innerpool;

import javax.sql.DataSource;

import jef.database.DbCfg;
import jef.database.datasource.DataSources;
import jef.database.datasource.IRoutingDataSource;
import jef.database.datasource.SimpleDataSource;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolService {
	private static Logger log = LoggerFactory.getLogger(PoolService.class);

	/**
	 * 获得一个可用的池
	 * 
	 * @param ds
	 * @param profile
	 * @param max
	 * @return
	 */
	public static IUserManagedPool getPool(DataSource ds, int max) {
		String noPoolStr = JefConfiguration.get(DbCfg.DB_NO_POOL, "auto");
		boolean auto = "auto".equalsIgnoreCase(noPoolStr);
		boolean noPool = StringUtils.toBoolean(noPoolStr, false);

		int min = JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL, 3);
		if (ds instanceof IRoutingDataSource) {
			IRoutingDataSource rds = (IRoutingDataSource) ds;
			if (auto || !noPool) {
				return new RoutingManagedConnectionPool(rds, max, min, auto);
			} else {
				return new RoutingDummyConnectionPool(rds);
			}
		} else {
			if (auto) {
				noPool = DataSources.isPool(ds);
				if (noPool) {
					log.info("There is Connection-Pool in datasource {}, EF-Inner Pool was disabled.", ds.getClass());
				} else {
					log.info("There is NO Connection-Pool detected in datasource {}, EF-Inner Pool was enabled.", ds.getClass());
				}
			}
			if (noPool) {
				return new SingleDummyConnectionPool(ds);
			} else {
				return new SingleManagedConnectionPool(ds, min, max);
			}
		}
	}

	public static DataSource getPooledDatasource(String url, String driverClass, String user, String password, int min, int max) {
		SimpleDataSource ds = new SimpleDataSource();
		ds.setUrl(url);
		ds.setDriverClass(driverClass);
		ds.setUser(user);
		ds.setPassword(password);
		return new SingleManagedConnectionPool(ds, min, max);
	}

	/**
	 * 正常情況下，關閉時兩者應該相等
	 * 
	 * @param pollCount
	 * @param offerCount
	 */
	static void logPoolStatic(String name, long pollCount, long offerCount) {
		log.info("The connection {} poll-count:{} offer-count:{}", name, pollCount, offerCount);
	}
}
