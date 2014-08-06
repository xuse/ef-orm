package jef.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.MappingType;
import jef.database.query.SqlContext;
import jef.database.wrapper.ResultSetImpl;
import jef.tools.reflect.BeanWrapper;

public final class LobLazyLoadTask implements LazyLoadTask {
	private MappingType<?> mType;
	private String tableName;
	private String columnname;
	private String fieldName;
	private DatabaseDialect profile;
	
	public LobLazyLoadTask(MappingType<?> mtype, DatabaseDialect profile,String tableName) {
		this.mType = mtype;
		this.tableName=tableName;
		this.columnname=mType.getColumnName(profile, true);
		this.profile=profile;
		this.fieldName=mtype.fieldName();
	}

	public void process(Session db, Object o) throws SQLException {
		IQueryableEntity obj = (IQueryableEntity) o;
		String sql = "select " + columnname + " from " + tableName + db.rProcessor.toWhereClause(obj.getQuery(), new SqlContext(null, obj.getQuery()), false);
		ResultSet rs = db.getResultSet(sql, 10);
		if (rs.next()) {
			Object value = mType.getProperObject(new ResultSetImpl(rs, profile), 1);
			if(value!=null){
				BeanWrapper bw=BeanWrapper.wrap(o,BeanWrapper.FAST);
				bw.setPropertyValue(fieldName, value);
			}
		}
	}

	public Collection<String> getEffectFields() {
		return Arrays.asList(fieldName);
	}

}
