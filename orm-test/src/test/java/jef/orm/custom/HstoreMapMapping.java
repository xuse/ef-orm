package jef.orm.custom;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AColumnMapping;
import jef.database.jdbc.result.IResultSet;

import org.hsqldb.types.Types;

public class HstoreMapMapping extends AColumnMapping {

	@Override
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect dialect) throws SQLException {
		if (value == null) {
			st.setNull(index, Types.OTHER);
		} else {
			Map<String, String> map = (Map<String, String>) value;
			st.setObject(index, map);
		}
		return value;
	}

	private String toString(Map<String, String> map) {
		StringBuilder sb = new StringBuilder(map.size() * 10);
		Iterator<Map.Entry<String, String>> iter = map.entrySet().iterator();
		if (iter.hasNext()) {
			Map.Entry<String, String> entry = iter.next();
			sb.append(entry.getKey()).append("=>").append(entry.getValue());
		}
		for (; iter.hasNext();) {
			Map.Entry<String, String> entry = iter.next();
			sb.append(',').append(entry.getKey()).append("=>").append(entry.getValue());
		}
		return sb.toString();
	}

	@Override
	public int getSqlType() {
		return Types.OTHER;
	}

	@Override
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Map o = (Map)rs.getObject(n);
		return o;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		Map<String, String> map = (Map<String, String>) value;
		return this.wrapSqlStr(toString(map));
	}
	// PreparedStatement ps = c.prepareStatement(
	// "insert into xyz(id, data) values(?, hstore(?, ?))");
	//
	// ps.setLong(1, 23456L);
	// ps.setArray(2, c.createArrayOf("text", new String[]{"name", "city"}));
	// ps.setArray(3, c.createArrayOf("text", new String[]{"Duke", "Valley"}));

	@Override
	protected Class<?> getDefaultJavaType() {
		return Map.class;
	}
}
