package jef.database.dialect;

public interface SqlTypeVersioned {
	
	boolean isVersion();
	
	ColumnType setVersion(boolean flag);
}
