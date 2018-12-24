package jef.database.dialect.type;

import java.lang.annotation.Annotation;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.persistence.Column;

import com.github.geequery.entity.Entities;

import jef.accelerator.bean.BeanAccessor;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.annotation.UnsavedValue;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.tools.Assert;
import jef.tools.DateUtils;
import jef.tools.Primitives;
import jef.tools.StringUtils;
import jef.tools.reflect.Property;

public abstract class AColumnMapping implements ColumnMapping {
	/**
	 * 原始的ColumnName
	 */
	public String rawColumnName;
	protected transient String cachedEscapeColumnName;
	private transient String lowerColumnName;
	private transient String upperColumnName;

	protected ITableMetadata meta;
	private String fieldName;
	protected Field field;
	protected ColumnType columnDef;

	protected Class<?> clz;
	private boolean pk;
	protected transient DatabaseDialect bindedProfile;
	protected Property fieldAccessor;

	private boolean unsavedValueDeclared;
	private Predicate<Object> unsavedValue;
	private boolean notInsert;
	private boolean notUpdate;

	public AColumnMapping() {
		this.clz = getDefaultJavaType();
	}

	abstract protected Class<?> getDefaultJavaType();

	public String name() {
		return fieldName;
	}

	public boolean isPk() {
		return pk;
	}

	private static class ConstantFilter implements Predicate<Object> {
		private Object object1;

		ConstantFilter(Object obj) {
			this.object1 = obj;
		}

		public boolean test(Object object2) {
			if (object1 == object2) {
				return true;
			}
			if ((object1 == null) || (object2 == null)) {
				return false;
			}
			return object1.equals(object2);
		}
	}

	private static final Predicate<Object> Null = new Predicate<Object>() {
		public boolean test(Object obj) {
			return obj == null;
		}
	};

	private static final Predicate<Object> NullOrEmpty = new Predicate<Object>() {
		public boolean test(Object obj) {
			if (obj == null)
				return true;
			return String.valueOf(obj).length() == 0;
		}
	};

	private static final Predicate<Object> MinusNumber = new Predicate<Object>() {
		public boolean test(Object obj) {
			if (obj == null)
				return true;
			if (obj instanceof Number) {
				return ((Number) obj).longValue() < 0;
			} else {
				return false;
			}
		}
	};

	private static final Predicate<Object> ZeroAndMinus = new Predicate<Object>() {
		public boolean test(Object obj) {
			if (obj == null)
				return true;
			if (obj instanceof Number) {
				return ((Number) obj).longValue() <= 0;
			} else {
				return false;
			}
		}
	};

	public void init(Field field, String columnName, ColumnType type, ITableMetadata meta) {
		this.field = field;
		this.fieldName = field.name();
		this.rawColumnName = columnName;
		this.lowerColumnName = columnName.toLowerCase();
		this.upperColumnName = columnName.toUpperCase();
		this.meta = meta;
		this.columnDef = type;

		BeanAccessor ba = meta.getContainerAccessor();

		fieldAccessor = ba.getProperty(field.name());
		Assert.notNull(fieldAccessor, ba.toString() + field.toString());
		Class<?> containerType = fieldAccessor.getType();
		if (clz.isAssignableFrom(containerType)) {
			this.clz = containerType;
		}
		Map<Class<?>, Annotation> map = ba.getAnnotationOnField(field.name());
		UnsavedValue unsaveValue = map == null ? null : (UnsavedValue) map.get(UnsavedValue.class);
		if (unsaveValue != null) {
			unsavedValueDeclared = true;
			unsavedValue = parseValue(containerType, unsaveValue.value());
		} else if (containerType.isPrimitive()) {
			unsavedValue = new ConstantFilter(Primitives.defaultValueOfPrimitive(containerType));
		}
		Column column = map == null ? null : (Column) map.get(Column.class);
		if (column != null) {
			this.notInsert = !column.insertable();
			this.notUpdate = !column.updatable();
		}
	}

	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect)
			throws SQLException {
		rs.updateObject(columnIndex, value);
	}

	private Predicate<Object> parseValue(Class<?> containerType, String value) {
		// int 226
		// short 215
		// long 221
		// boolean 222
		// float 219
		// double 228
		// char 201
		// byte 237
		Object condition = null;
		if (containerType.isPrimitive()) {
			String s = containerType.getName();
			switch (s.charAt(1) + s.charAt(2)) {
			case 226:
				condition = StringUtils.toInt(value, 0);
				break;
			case 215:
				condition = StringUtils.toInt(value, 0);
				break;
			case 221:
				condition = StringUtils.toLong(value, 0L);
				break;
			case 222:
				condition = StringUtils.toBoolean(value, false);
				break;
			case 219:
				condition = StringUtils.toFloat(value, 0f);
				break;
			case 228:
				condition = StringUtils.toDouble(value, 0d);
				break;
			case 201:
				if (value.length() == 0) {
					condition = (char) 0;
				} else {
					condition = value.charAt(0);
				}
				break;
			case 237:
				condition = (byte) StringUtils.toInt(value, 0);
				break;
			default:
			}
		} else if ("null".equalsIgnoreCase(value)) {
			return Null;
		} else if (UnsavedValue.MinusNumber.equals(value)) {
			return MinusNumber;
		} else if (UnsavedValue.ZeroAndMinus.equals(value)) {
			return ZeroAndMinus;
		} else if (UnsavedValue.NullOrEmpty.equals(value)) {
			return NullOrEmpty;
		} else if (String.class == containerType) {
			condition = value;
		} else if (Date.class == containerType) {
			condition = DateUtils.autoParse(value);
		}
		return condition == null ? Null : new ConstantFilter(condition);
	}

	public String fieldName() {
		return fieldName;
	}

	public String upperColumnName() {
		return upperColumnName;
	}

	public String lowerColumnName() {
		return lowerColumnName;
	}

	@Override
	public String rawColumnName() {
		return rawColumnName;
	}

	public Field field() {
		return field;
	}

	public ITableMetadata getMeta() {
		return meta;
	}

	public ColumnType get() {
		return columnDef;
	}

	public Class<?> getFieldType() {
		return clz;
	}

	public String getColumnName(DatabaseDialect profile, boolean escape) {
		if (!escape) {
			return profile.getColumnNameToUse(this);
		}
		if (bindedProfile == profile) {
			return cachedEscapeColumnName;
		}
		String escapedColumn = DbUtils.escapeColumn(profile, profile.getColumnNameToUse(this));
		rebind(escapedColumn, profile);
		return escapedColumn;
	}

	protected void rebind(String escapedColumn, DatabaseDialect profile) {
		bindedProfile = profile;
		cachedEscapeColumnName = escapedColumn;
	}

	public void setPk(boolean b) {
		this.pk = b;
	}

	/**
	 * 如果是文本，那么用单引号包括并转义。前提是尚未用单引号包裹的。
	 * @param value
	 * @return
	 */
	public static String quotWith(Object value, int sqlType) {
		if (value instanceof String) {
			String s = (String) value;
			if (s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
				return s;
			} else {
				return wrapSqlStr(s);
			}
		}
		return String.valueOf(value);
	}
	
	/**
	 * 用单引号包围字符串，并将其中的单引号按SQL转义
	 * 
	 * @param s
	 * @return
	 */
	public final static String wrapSqlStr(String s) {
		StringBuilder sb = new StringBuilder(s.length() + 16);
		sb.append('\'');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\'') {
				sb.append("''");
			} else {
				sb.append(c);
			}
		}
		sb.append('\'');
		return sb.toString();
	}

	public String getSqlStr(Object value, DatabaseDialect profile) {
		if (value == null) {
			return "null";
		} else if (value instanceof Expression) {
			return value.toString();
		}
		return getSqlExpression(value, profile);
	}

	/**
	 * 获得SQL表达式的写法。
	 * 
	 * @param value
	 *            不为null，不为Expression
	 */
	protected abstract String getSqlExpression(Object value, DatabaseDialect profile);

	public void processPreparedInsert(Object obj, List<String> cStr, List<String> vStr,
			InsertSqlClause result, boolean dynamic) throws SQLException {
		if (dynamic && !Entities.isUsed(obj,field)) {
			return;
		}
		String columnName = getColumnName(result.profile, true);
		cStr.add(columnName);
		vStr.add("?");
		result.addField(this);
	}

	public boolean isLob() {
		return false;
	}

	public boolean applyFor(int type) {
		return type == getSqlType();
	}

	public Property getFieldAccessor() {
		return fieldAccessor;
	}

	@Override
	public String toString() {
		return this.fieldName;
	}

	public boolean isNotInsert() {
		return notInsert;
	}

	public boolean isNotUpdate() {
		return notUpdate;
	}

	/**
	 * 用户是否通过注解配置了UnsavedValue
	 * 
	 * @return
	 */
	public boolean isUnsavedValueDeclared() {
		return unsavedValueDeclared;
	}

    /**
     * 判断是否为UnsavedValue。 UnsavedValue是系统认为不会存入数据库的一种值。 如果显式声明 用于判断主键无效、查询条件无效等情况
     */
    @Override
    public boolean isUnsavedValue(Object object) {
		return unsavedValue.test(object);
	}

	public boolean isGenerated() {
		return false;
	}
}
