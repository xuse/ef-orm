package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.MonthDay;
import java.time.YearMonth;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

/**
 * CHAR <-> java.time.MonthDay
 * 
 * @author jiyi
 *
 */
public class LocalMonthDay_Char extends AColumnMapping {

    public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
        st.setString(index, toString(value));
        return value;
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    public int getSqlType() {
        return java.sql.Types.CHAR;
    }

    @Override
    protected String getSqlExpression(Object value, DatabaseDialect profile) {
        if (value instanceof YearMonth) {
            return value.toString();
        }
        throw new IllegalArgumentException("The input param can not cast to Date.");
    }

    public Object jdbcGet(IResultSet rs, int n) throws SQLException {
        String s = rs.getString(n);
        return s == null ? null : MonthDay.parse(s);
    }

    @Override
    protected Class<?> getDefaultJavaType() {
        return MonthDay.class;
    }

    @Override
    public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
        rs.updateString(columnIndex, toString(value));
    }
}
