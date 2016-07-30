package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

/**
 * 要求的数据库类型为Boolean，但实际上根据数据库特性，有BIT、CHAR(1)、BOOLEAN等多种实现
 * @author jiyi
 *
 */
public final class DelegatorBoolean extends AColumnMapping{
	private AColumnMapping real;
	private DatabaseDialect profile;
	
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect dialect) throws SQLException {
		if(real==null || this.profile!=dialect){
			init(dialect);
		}
		return real.jdbcSet(st, value, index, dialect);
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
		real.init(field, rawColumnName, columnDef, meta);
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

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		if(real==null){
			init(rs.getProfile());
		}
		return real.jdbcGet(rs, n);
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Boolean.class;
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		if(real==null || this.profile!=dialect){
			init(profile);
		}
		real.jdbcUpdate(rs, columnIndex, value, dialect);
	}
}
