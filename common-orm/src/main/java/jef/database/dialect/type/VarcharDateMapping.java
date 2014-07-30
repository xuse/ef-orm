package jef.database.dialect.type;


public class VarcharDateMapping extends CharDateMapping{
	
	public VarcharDateMapping(String format){
		super(format);
	}
	
	public VarcharDateMapping(){
		super();
	}
	
	public int getSqlType() {
		return java.sql.Types.VARCHAR;
	}
}
