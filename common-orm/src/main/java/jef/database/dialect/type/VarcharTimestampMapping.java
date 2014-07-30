package jef.database.dialect.type;


public class VarcharTimestampMapping extends CharTimestampMapping{
	
	
	public VarcharTimestampMapping() {
		super();
	}

	public VarcharTimestampMapping(String format) {
		super(format);
	}

	public int getSqlType() {
		return java.sql.Types.VARCHAR;
	}
	
	
}
