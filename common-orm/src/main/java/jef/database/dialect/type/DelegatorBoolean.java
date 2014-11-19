package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

/**
 * 要求的数据库类型为Boolean，但实际上根据数据库特性，有BIT、CHAR(1)、BOOLEAN等多种实现
 * @author jiyi
 *
 */
public final class DelegatorBoolean extends AColumnMapping<Boolean>{
	private AColumnMapping<Boolean> real;
	private DatabaseDialect profile;
	
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect profile) throws SQLException {
		if(real==null || this.profile!=profile){
			init(profile);
		}
		return real.set(st, value, index, profile);
	}

	private void init(DatabaseDialect profile) {
		int type=profile.getImplementationSqlType(Types.BOOLEAN);
		if(type==Types.BIT){
			real=new BitBooleanMapping();
		}else if(type==Types.CHAR){
			real=new CharBooleanMapping();	
		}else if(type==Types.TINYINT || type==Types.NUMERIC || type==Types.INTEGER){
			real=new NumIntBooleanMapping();
		}else{
			real=new BooleanBoolMapping();
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
