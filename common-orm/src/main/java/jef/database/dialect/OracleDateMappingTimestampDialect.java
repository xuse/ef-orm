package jef.database.dialect;


/**
 * 默认的OracleDialect中，DATE类型的Column是映射到jef的java.sql.Date类型上的，在这个Profile中，则是映射到java.sql.TimeStamp 
 * @author Administrator
 *
 */
public class OracleDateMappingTimestampDialect extends OracleDialect {
	public ColumnType getProprtMetaFromDbType(jef.database.meta.Column column) {
		if("NUMBER".equals(column.getDataType())){
			if(column.getDecimalDigit()>0){//小数
				return new ColumnType.Double(column.getColumnSize(),column.getDecimalDigit());
			}else{//整数
				return new ColumnType.Int(column.getColumnSize());
			}
		}else if("LONG".equals(column.getDataType())){
			return new ColumnType.Varchar(1000);
		}else if("DATE".equals(column.getDataType())){
			return new ColumnType.TimeStamp();
		}else{
			return super.getProprtMetaFromDbType(column);
		}
	}
}
