package jef.orm.custom;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AColumnMapping;
import jef.database.jdbc.result.IResultSet;

import org.hsqldb.types.Types;
import org.postgresql.util.PGobject;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;

public class ObjectJsonbMapping extends AColumnMapping {

	@Override
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect dialect) throws SQLException {
		if (value == null) {
			st.setNull(index, Types.STRUCT);
			return null;
		} else {
			PGobject jsonObject = new PGobject();
			jsonObject.setType("jsonb");
			jsonObject.setValue(JSONUtils.toJSONString(value));
			st.setObject(index, jsonObject);
			return jsonObject;
		}
	}

	@Override
	public int getSqlType() {
		return Types.STRUCT;
	}

	@Override
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object o = rs.getObject(n);
		if(o==null)return null;
		if(o instanceof PGobject){
			PGobject v=(PGobject)o;
			Object obj=JSON.parseObject(v.getValue(), this.getFieldAccessor().getGenericType());
			return obj;
		}else{
			throw new IllegalArgumentException("Expect type from JDBC drirver is PGobject, got "+o.getClass().getName());
		}
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		String json=JSONUtils.toJSONString(value);
		return this.wrapSqlStr(json);
	}
	// PreparedStatement ps = c.prepareStatement(
	// "insert into xyz(id, data) values(?, hstore(?, ?))");
	//
	// ps.setLong(1, 23456L);
	// ps.setArray(2, c.createArrayOf("text", new String[]{"name", "city"}));
	// ps.setArray(3, c.createArrayOf("text", new String[]{"Duke", "Valley"}));

	@Override
	protected Class<?> getDefaultJavaType() {
		return Object.class;
	}
}
