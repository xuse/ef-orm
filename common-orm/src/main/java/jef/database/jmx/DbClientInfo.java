package jef.database.jmx;

import jef.database.DbClient;
import jef.database.jpa.JefEntityManagerFactory;
import jef.tools.StringUtils;

public class DbClientInfo implements DbClientInfoMBean {
	private DbClient db;
	private JefEntityManagerFactory emf=null;

	public DbClientInfo(DbClient db) {
		this.db=db;
	}

	public void setDbClientFactory(JefEntityManagerFactory emf) {
		this.emf = emf;
	}

	public String getEmfName() {
		return emf==null?"not available":emf.getName();
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

	public String getInnerConnectionPoolInfo() {
		return db.getInnerPoolStatics();
	}

	public void checkNamedQueryUpdate() {
		db.checkNamedQueryUpdate();
	}

	public void clearGlobalCache() {
		db.flush();
	}
}
