package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Feature;
import jef.database.wrapper.IResultSet;


public final class DelegatorBoolean extends ATypeMapping<Boolean>{
	private ATypeMapping<Boolean> real;
	private DatabaseDialect profile;
	
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect profile) throws SQLException {
		if(real==null || this.profile!=profile){
			init(profile);
		}
		return real.set(st, value, index, profile);
	}

	private void init(DatabaseDialect profile) {
		if(profile.has(Feature.SUPPORT_BOOLEAN)){
			real=new BooleanBoolMapping();
		}else{
			real=new CharBooleanMapping();
		}
		real.init(field, rawColumnName, ctype, meta);
	}

	public int getSqlType() {
		return real.getSqlType();
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		if(real==null){
			init(profile);
		}
		return real.getSqlExpression(value, profile);
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		if(real==null){
			init(rs.getProfile());
		}
		return real.getProperObject(rs, n);
	}
}
