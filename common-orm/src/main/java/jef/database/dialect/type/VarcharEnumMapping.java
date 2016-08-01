package jef.database.dialect.type;

import java.lang.annotation.Annotation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import jef.accelerator.bean.BeanAccessor;
import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.meta.ITableMetadata;
import jef.tools.StringUtils;

public class VarcharEnumMapping extends AColumnMapping {
	private boolean isOrdinal;

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if (value == null) {
			st.setNull(index, java.sql.Types.VARCHAR);
		} else {
			st.setString(index, toString(value));
		}
		return value;
	}

	private String toString(Object value) {
		if(isOrdinal) {
			return String.valueOf(((Enum<?>)value).ordinal());
		}else {
			return ((Enum<?>)value).name();
		}
	}

	public int getSqlType() {
		return java.sql.Types.VARCHAR;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return super.wrapSqlStr(toString(value));
	}

	@SuppressWarnings("unchecked")
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		String s = rs.getString(n);
		if (s == null || s.length() == 0)
			return null;
		if(isOrdinal) {
			int cnt=StringUtils.toInt(s, 0);
			Enum<?>[] enums=clz.asSubclass(Enum.class).getEnumConstants(); 
			return enums[cnt];
		}else {
			return Enum.valueOf(clz.asSubclass(Enum.class), s);
		}
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Enum.class;
	}

	@Override
	public void init(Field field, String columnName, ColumnType type, ITableMetadata meta) {
		super.init(field, columnName, type, meta);
		BeanAccessor ba = meta.getContainerAccessor();
		Map<Class<?>,Annotation> map=ba.getAnnotationOnField(field.name());
		Enumerated anno = map==null?null:(Enumerated)map.get(Enumerated.class);
		if (anno != null) {
			this.isOrdinal = anno.value() == EnumType.ORDINAL;
		}
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		rs.updateString(columnIndex, toString(value));
	}

}
