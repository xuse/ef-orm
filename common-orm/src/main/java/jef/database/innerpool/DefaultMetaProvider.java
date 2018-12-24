package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Provider;
import javax.persistence.PersistenceException;

import jef.common.Callback;
import jef.database.ConnectInfo;
import jef.database.DbMetaData;
import jef.database.dialect.DatabaseDialect;

public class DefaultMetaProvider implements ConnectionAndMetadataProvider {
	protected Provider<Connection> datasource;
	private volatile DbMetaData metadata;

	public DefaultMetaProvider(Provider<Connection> ds) {
		this.datasource = ds;
		this.metadata = new DbMetaData(ds);
		metadata.getProfile().accept(metadata);
	}

	public Collection<String> getAllDatasourceNames() {
		return Collections.emptyList();
	}

	public DbMetaData getMetadata(String dbkey) {
		return metadata;
	}

	public DatabaseDialect getProfile(String dbkey) {
		return metadata.getProfile();
	}

	public ConnectInfo getInfo(String dbkey) {
		return metadata.getInfo();
	}

	public void registeDbInitCallback(Callback<String, SQLException> callback) {
		if (callback != null) {
			try {
				callback.call(null);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}
	}

	public boolean isRouting() {
		return false;
	}

	public boolean isDummy() {
		return true;
	}

	@Override
	public IConnection get() {
		return new Conn(datasource.get());
	}
}
