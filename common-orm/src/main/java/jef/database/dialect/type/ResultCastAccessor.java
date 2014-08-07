package jef.database.dialect.type;

import java.sql.SQLException;

import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.result.IResultSet;

public final class ResultCastAccessor implements ResultSetAccessor{
	private Class<?> targetClass;
	private DatabaseDialect profile;
	private ColumnType cType;
	
	public ResultCastAccessor(Class<?> javaClass, DatabaseDialect profile,ColumnType ctype) {
		this.targetClass=javaClass;
		this.profile=profile;
		this.cType=ctype;
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		return profile.getJavaValue(cType, rs.getObject(n));
	}

	public Class<?> getReturnType() {
		return targetClass;
	}

	public boolean applyFor(int type) {
		return true;
	}

}
