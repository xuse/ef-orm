package jef.database.wrapper.processor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.support.SqlLog;
import jef.tools.IOUtils;

public final class BindVariableContext {
	private PreparedStatement psmt;
	private DatabaseDialect db;
	private final SqlLog logMessage;

	
	
	public BindVariableContext(PreparedStatement psmt,DatabaseDialect profile,SqlLog sb){
		this.psmt=psmt;
		this.logMessage=sb;
		this.db=profile;
	}
	
	
	public SqlLog getLogMessage(){
		return logMessage;
	}

	
	public void log(int count,Object fieldName,Object value){
		logMessage.append(count, fieldName, value);
	}
	
	public void setObject(int count, Object value) throws SQLException {
		psmt.setObject(count, value);
	}
	
	public void setObject(int count, Object value,int type) throws SQLException {
		psmt.setObject(count, value,type);
	}
	
	public void setObject(int count, Object value,int type,int length) throws SQLException {
		psmt.setObject(count, value,type,length);
	}
	
	public Object setValueInPsmt(int count, Object value) throws SQLException {
		if (value != null) {
			if ((value instanceof File)) {
				File file=(File)value;
				try {
					psmt.setBinaryStream(count, IOUtils.getInputStream(file),file.length());
				} catch (IOException e) {
					throw new IllegalArgumentException();
				}
				return value;
			}else if(value instanceof byte[]){
				byte[] buf=(byte[])value;
				psmt.setBinaryStream(count, new ByteArrayInputStream(buf),buf.length);
				return value;
			} else if (value instanceof Enum<?>) {
				value = ((Enum<?>) value).name();
			} else if (value instanceof Character) {
				value=value.toString();
			}
		}
		psmt.setObject(count, value);
		return value;
	}
	
	/**
	 * 对于绑定变量的SQL对象进行参数赋值
	 * 
	 * @param psmt
	 * @param count
	 * @param value
	 * @param cType
	 * @throws SQLException
	 */
	@SuppressWarnings({ "rawtypes" })
	public Object setValueInPsmt(int count, Object value, ColumnMapping cType) throws SQLException {
		if(cType==null){
			if(value.getClass()==java.util.Date.class){
				value=db.toTimestampSqlParam((Date)value);
			}
			psmt.setObject(count,value);
		}else{
			value = cType.jdbcSet(psmt, value, count, db);	
		}
		return value;
	}
}
