package jef.database.innerpool;


public interface CheckablePool {
	Iterable<? extends CheckableConnection> getConnectionsToCheck();

	String getTestSQL();

	void setTestSQL(String string);
}
