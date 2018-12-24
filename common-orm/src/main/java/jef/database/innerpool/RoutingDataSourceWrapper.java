package jef.database.innerpool;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;
import javax.sql.DataSource;

import com.github.geequery.support.spring.DataSourceProvider;

import jef.common.Callback;
import jef.database.ConnectInfo;
import jef.database.DbMetaData;
import jef.database.datasource.RoutingDataSource;
import jef.database.dialect.DatabaseDialect;

/**
 * 启用了外部连接池后的路由伪连接池。 实际上所有真正的连接都是从外部连接池中获取的。每次释放的时候这些连接都被关闭（即释放回外部连接池）
 * 
 * 
 * @author jiyi
 *
 */
public class RoutingDataSourceWrapper implements ConnectionAndMetadataProvider {
	protected Provider<IConnection> provider;
	private RoutingDataSource datasource;
	private final Map<String, DbMetaData> metadatas = new HashMap<String, DbMetaData>(8, 0.5f);

	public RoutingDataSourceWrapper(RoutingDataSource ds) {
		this.provider = ds.toProvider();
		this.datasource = ds;
	}

	public RoutingDataSourceWrapper(Provider<IConnection> provider, RoutingDataSource ds) {
		this.provider = provider;
		this.datasource = ds;
	}

	public DataSource getDatasource() {
		throw new UnsupportedOperationException();
	}

	public Collection<String> getAllDatasourceNames() {
		return datasource.getDataSourceNames();
	}

	public RoutingDataSource getRoutingDataSource() {
		return datasource;
	}

	public DbMetaData getMetadata(String dbkey) {
		dbkey = wrapNullKey(dbkey);
		DbMetaData meta = metadatas.get(dbkey);
		if (meta != null)
			return meta;
		meta = createMetadata(dbkey);
		return meta;
	}

	private String wrapNullKey(String dbkey) {
		if (dbkey != null) {
			return dbkey;
		}
		String e = datasource.getDefaultKey();
		if (e != null) {
			return e;
		} else {
			
			throw new IllegalArgumentException("No default datasource found in " + datasource + "!");
		}
	}

	private synchronized DbMetaData createMetadata(String key) {
		DataSource ds = datasource.getDataSource(key);// 必须放在双重检查锁定逻辑的外面，否则会因为回调对象的存在而造成元数据对象初始化两遍。。。
		DbMetaData meta = metadatas.get(key);
		if (meta == null) {
			meta = new DbMetaData(new DataSourceProvider(ds));
			metadatas.put(key, meta);
		}
		// 反向修正
		meta.getProfile().accept(meta);
		return meta;
	}

	public DatabaseDialect getProfile(String dbkey) {
		return getMetadata(dbkey).getProfile();
	}

	public ConnectInfo getInfo(String dbkey) {
		return getMetadata(dbkey).getInfo();
	}

	public boolean isRouting() {
		return true;
	}

	public void registeDbInitCallback(Callback<String, SQLException> callback) {
		this.datasource.setCallback(callback);
	}

	public boolean isDummy() {
		return true;
	}

	@Override
	public IConnection get() {
		return this.provider.get();
	}
}
