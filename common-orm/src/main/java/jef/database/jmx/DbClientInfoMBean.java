package jef.database.jmx;

public interface DbClientInfoMBean {

	String getInnerConnectionPoolInfo();

	String getEmfName();
	
	String getDatasourceNames();

	boolean isRoutingDbClient();

	boolean isConnected();
	
	void checkNamedQueryUpdate();
}
