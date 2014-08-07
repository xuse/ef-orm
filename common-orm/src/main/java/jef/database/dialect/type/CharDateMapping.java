package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.result.IResultSet;
import jef.tools.DateFormats;
import jef.tools.StringUtils;

public class CharDateMapping extends ATypeMapping<java.util.Date> {
	private ThreadLocal<DateFormat> format;
	private ThreadLocal<DateFormat> dateOnly;

	public CharDateMapping(String format) {
		this.format = DateFormats.getThreadLocalDateFormat(format);
		this.dateOnly = DateFormats.DATE_CS;
	}

	public CharDateMapping() {
		this.format = DateFormats.DATE_TIME_CS;
		this.dateOnly = DateFormats.DATE_CS;
	}

	public int getSqlType() {
		return java.sql.Types.CHAR;
	}

	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		String s = format.get().format((Date) value);
		st.setString(index, s);
		return value;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return super.wrapSqlStr(value.toString());
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		String s = rs.getString(n);
		if (StringUtils.isEmpty(s)) {
			return null;
		}
		try {
			if (s.length() < 14) {
				return dateOnly.get().parse(s);
			} else {
				return format.get().parse(s);

			}
		} catch (ParseException e) {
			throw new SQLException(e);
		}
	}
}
