package jef.database;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.MappingType;
import jef.database.support.LogFormat;
import jef.tools.IOUtils;
import jef.tools.JefConfiguration;

final class BindVariableContext {
	private PreparedStatement psmt;
	private DatabaseDialect db;
	private final StringBuilder logMessage;
	private static LogFormat formatter=getLogFormat();
	

	/**
	 * 构造
	 * @param psmt
	 * @param db
	 * @param sb
	 */
	BindVariableContext(PreparedStatement psmt,OperateTarget profile,StringBuilder sb){
		this.psmt=psmt;
		this.logMessage=sb;
		this.db=profile.getProfile();
	}
	
	BindVariableContext(PreparedStatement psmt,DatabaseDialect profile,StringBuilder sb){
		this.psmt=psmt;
		this.logMessage=sb;
		this.db=profile;
	}
	
	private static LogFormat getLogFormat() {
		if("no_wrap".equalsIgnoreCase(JefConfiguration.get(DbCfg.DB_LOG_FORMAT))){
			return new jef.database.support.LogFormat.NowrapLineLogFormat();
		}
		return new jef.database.support.LogFormat.Default();
	}


	public CharSequence getLogMessage(){
		return logMessage;
	}
	
	public void log(int count,String fieldName,Object value){
		if (logMessage != null) {
			formatter.log(logMessage, count, fieldName, value);
		}
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
	public Object setValueInPsmt(int count, Object value, MappingType cType) throws SQLException {
		if(cType==null){
			psmt.setObject(count,value);
		}else{
			value = cType.set(psmt, value, count, db);	
		}
		return value;
	}
}
