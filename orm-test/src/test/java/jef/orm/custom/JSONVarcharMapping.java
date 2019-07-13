package jef.orm.custom;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AColumnMapping;
import jef.database.jdbc.result.IResultSet;
import jef.tools.ArrayUtils;

import  org.apache.commons.lang3.StringUtils;
import org.hsqldb.types.Types;

public class JSONVarcharMapping extends AColumnMapping{

	@SuppressWarnings("unchecked")
	@Override
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect dialect) throws SQLException {
		
		if(value==null){
			st.setNull(index, java.sql.Types.VARCHAR);
			return null;
		}else{
			List<String> data=(List<String>)value;
			String v=StringUtils.join(data, ',');
			st.setString(index,v);
			return v;
		}
	}

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		String s=rs.getString(n);
		if(s==null)return null;
		return ArrayUtils.asList(StringUtils.split(s,','));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		List<String> data=(List<String>)value;
		String v=StringUtils.join(data,',');
		return wrapSqlStr(v);
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return List.class;
	}
}
