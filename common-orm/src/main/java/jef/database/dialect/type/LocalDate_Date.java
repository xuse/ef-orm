package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.query.Func;
import jef.tools.DateUtils;

/**
 * DATE <-> java.sql.Date
 * 
 * @author jiyi
 *
 */
public class LocalDate_Date extends AbstractTimeMapping {

    public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
        st.setDate(index, DateUtils.toSqlDate((LocalDate) value));
        return value;
    }

    public int getSqlType() {
        return java.sql.Types.DATE;
    }

    @Override
    protected String getSqlExpression(Object value, DatabaseDialect profile) {
        return profile.getSqlDateExpression(DateUtils.fromLocalDate((LocalDate) value));
    }

    public Object jdbcGet(IResultSet rs, int n) throws SQLException {
        return DateUtils.toLocalDate(rs.getDate(n));
    }

    @Override
    public String getFunctionString(DatabaseDialect profile) {
        return profile.getFunction(Func.current_date);
    }

    @Override
    public Object getCurrentValue() {
        return LocalDate.now();
    }

    @Override
    protected Class<?> getDefaultJavaType() {
        return LocalDate.class;
    }

    @Override
    public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
        rs.updateDate(columnIndex, DateUtils.toSqlDate((LocalDate) value));
    }
}
