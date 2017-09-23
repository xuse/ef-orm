package jef.database.dialect.type;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.query.Func;
import jef.tools.DateUtils;

/**
 * Instanct <-> Datetime
 * 
 * @author jiyi
 *
 */
public class Instant_DateTime extends AbstractTimeMapping {

    public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
        st.setTimestamp(index, DateUtils.toSqlTimeStamp((java.time.Instant) value));
        return value;
    }

    public int getSqlType() {
        return java.sql.Types.TIMESTAMP;
    }

    @Override
    protected String getSqlExpression(Object value, DatabaseDialect profile) {
        Instant inst = (java.time.Instant) value;
        return profile.getSqlTimestampExpression(DateUtils.fromInstant(inst));
    }

    public Object jdbcGet(IResultSet rs, int n) throws SQLException {
        return DateUtils.toInstant(rs.getDate(n));
    }

    @Override
    public String getFunctionString(DatabaseDialect profile) {
        return profile.getFunction(Func.current_timestamp);
    }

    @Override
    public Object getCurrentValue() {
        return Instant.now();
    }

    @Override
    protected Class<?> getDefaultJavaType() {
        return Instant.class;
    }

    @Override
    public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
        rs.updateTimestamp(columnIndex,DateUtils.toSqlTimeStamp((java.time.Instant) value));
    }
}
