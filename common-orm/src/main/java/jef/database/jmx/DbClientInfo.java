package jef.database.jmx;

import org.apache.commons.lang.StringUtils;

import jef.database.DbClient;

public class DbClientInfo implements DbClientInfoMBean {
	private DbClient db;

	public DbClientInfo(DbClient db) {
		this.db=db;
	}

	public boolean isRoutingDbClient() {
		return db.isRoutingDataSource();
	}
	
	public boolean isConnected() {
		return db.isOpen();
	}

	public String getDatasourceNames() {
		return StringUtils.join(db.getAllDatasourceNames(),',');
	}

	public void checkNamedQueryUpdate() {
		db.checkNamedQueryUpdate();
	}

	public void clearGlobalCache() {
		db.flush();
	}
}
