package jef.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jdbc.result.ResultSetImpl;
import jef.database.query.SqlContext;
import jef.database.wrapper.clause.BindSql;
import jef.tools.reflect.BeanWrapper;

public final class LobLazyLoadTask implements LazyLoadTask {
	private ColumnMapping mType;
	private String tableName;
	private String columnname;
	private String fieldName;
	private DatabaseDialect profile;

	public LobLazyLoadTask(ColumnMapping mtype, DatabaseDialect profile, String tableName) {
		this.mType = mtype;
		this.tableName = profile.getObjectNameToUse(tableName);
		this.columnname = mType.getColumnName(profile, true);
		this.profile = profile;
		this.fieldName = mtype.fieldName();
	}

	public void process(Session db, Object o) throws SQLException {
		IQueryableEntity obj = (IQueryableEntity) o;
		BindSql wherePart=db.rProcessor.toWhereClause(obj.getQuery(), new SqlContext(null, obj.getQuery()), null, profile);
		String sql = "select " + columnname + " from " + tableName + wherePart.getSql();
		ResultSet rs = db.getResultSet(sql, 10,wherePart.getBindAsParamArray());
		if (rs.next()) {
			Object value = mType.jdbcGet(new ResultSetImpl(rs, profile), 1);
			if (value != null) {
				BeanWrapper bw = BeanWrapper.wrap(o, BeanWrapper.FAST);
				bw.setPropertyValue(fieldName, value);
			}
		}
	}

	public Collection<String> getEffectFields() {
		return Arrays.asList(fieldName);
	}

}
