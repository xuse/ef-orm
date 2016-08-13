package jef.database.dialect.type;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;

import jef.accelerator.bean.BeanAccessor;
import jef.common.log.LogUtil;
import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.populator.ColumnDescription;
import jef.tools.DateUtils;

/**
 * Java对数据库的各种数据类型映射方式
 * 
 * @author jiyi
 * 
 */
public final class ColumnMappings {
	private ColumnMappings() {
	}

	public static ColumnMapping getMapping(Field field, ITableMetadata meta, String columnName, ColumnType type, boolean pk) {
		BeanAccessor beanAccessor = meta.getContainerAccessor();
		Class<?> fieldType = beanAccessor.getPropertyType(field.name());
		ColumnMapping mType = type.getMappingType(fieldType);
		mType.init(field, columnName, type, meta);
		return mType;
	}

	public static final ResultSetAccessor RAW = new ResultRawAccessor();
	public static final ResultSetAccessor STRING = new ResultStringAccessor();
	public static final ResultSetAccessor INT = new ResultIntAccessor();
	public static final ResultSetAccessor LONG = new ResultLongAccessor();
	public static final ResultSetAccessor DOUBLE = new ResultDoubleAccessor();
	public static final ResultSetAccessor CHARACTER = new ResultCharacterAccessor(null);
	public static final ResultSetAccessor FLOAT = new ResultFloatAccessor();
	public static final ResultSetAccessor BOOLEAN = new ResultBooleanAccessor();
	public static final ResultSetAccessor CHAR_BOOLEAN = new CharBooleanAccessor();
	public static final ResultSetAccessor DATE = new ResultDateAccessor();
	public static final ResultSetAccessor BIGDECIMAL = new ResultBigDecimalAccessor();
	public static final ResultSetAccessor SHORT = new ResultShortAccessor();
	public static final ResultSetAccessor BYTE = new ResultByteAccessor();
	public static final ResultSetAccessor TIME = new ResultTimeAccessor();
	public static final ResultSetAccessor TIMESTAMP = new ResultTimestampAccessor();
	public static final ResultSetAccessor ROWID = new ResultRowidAccessor();
	public static final ResultSetAccessor BYTES = new ResultBytesAccessor();

	private static final ColumnType StringType = new ColumnType.Varchar(10);
	private static final ColumnType IntType = new ColumnType.Int(4);
	private static final ColumnType LongType = new ColumnType.Int(12);
	private static final ColumnType FloatType = new ColumnType.Double(4, 2);
	private static final ColumnType DoubleType = new ColumnType.Double(10, 8);
	private static final ColumnType ClobType = new ColumnType.Clob();
	private static final ColumnType BlobType = new ColumnType.Blob();
	private static final ColumnType DateType = new ColumnType.Date();
	private static final ColumnType TimestampType = new ColumnType.TimeStamp();

	/**
	 * Accessor of primitive int.
	 */
	public static final ResultSetAccessor I = new ResultSetAccessor() {
		public Object jdbcGet(IResultSet rs, int n) throws SQLException {
			return rs.getInt(n);
		}

		public boolean applyFor(int type) {
			return Types.INTEGER == type || Types.SMALLINT == type || Types.TINYINT == type || Types.NUMERIC == type;
		}
	};
	/**
	 * Accessor of primitive long.
	 */
	public static final ResultSetAccessor J = new ResultSetAccessor() {
		public Object jdbcGet(IResultSet rs, int n) throws SQLException {
			return rs.getLong(n);
		}

		public boolean applyFor(int type) {
			return Types.INTEGER == type || Types.SMALLINT == type || Types.TINYINT == type || Types.BIGINT == type || Types.NUMERIC == type;
		}
	};
	
	
	/**
	 * Accessor of primitive short.
	 */
	public static final ResultSetAccessor S = new ResultSetAccessor() {
		public Object jdbcGet(IResultSet rs, int n) throws SQLException {
			return rs.getShort(n);
		}

		public boolean applyFor(int type) {
			return Types.SMALLINT == type || Types.TINYINT == type || Types.NUMERIC == type;
		}
	};
	
	/**
	 * Accessor of primitive float.
	 */
	public static final ResultSetAccessor F = new ResultSetAccessor() {
		public Object jdbcGet(IResultSet rs, int n) throws SQLException {
			return rs.getFloat(n);
		}

		public boolean applyFor(int type) {
			return Types.FLOAT == type || Types.DOUBLE == type || Types.DECIMAL == type || Types.NUMERIC == type;
		}
	};
	/**
	 * Accessor of primitive double.
	 */
	public static final ResultSetAccessor D = new ResultSetAccessor() {
		public Object jdbcGet(IResultSet rs, int n) throws SQLException {
			return rs.getDouble(n);
		}

		public boolean applyFor(int type) {
			return Types.FLOAT == type || Types.DOUBLE == type || Types.DECIMAL == type || Types.NUMERIC == type;
		}
	};
	
	/**
	 * Accessor of primitive boolean.
	 */
	public static final ResultSetAccessor Z = new ResultSetAccessor() {
		public Object jdbcGet(IResultSet rs, int n) throws SQLException {
			return rs.getBoolean(n);
		}

		public boolean applyFor(int type) {
			return true;// Types.INTEGER==type || Types.SMALLINT==type
						// ||Types.TINYINT==type ||Types.BIGINT==type ||
						// Types.BOOLEAN==type||Types.NUMERIC==type
						// ||Types.BIT==type;
		}
	};
	
	/**
	 * Accessor of primitive byte.
	 */
	public static final ResultSetAccessor B = new ResultSetAccessor() {
		public Object jdbcGet(IResultSet rs, int n) throws SQLException {
			return rs.getByte(n);
		}

		public boolean applyFor(int type) {
			return true;
		}
	};


	private static final Map<Class<?>, ResultSetAccessor> FAST_ACCESSOR_MAP_PRIMTIVE = new IdentityHashMap<Class<?>, ResultSetAccessor>();
	private static final Map<Class<?>, ResultSetAccessor> FAST_ACCESSOR_MAP = new IdentityHashMap<Class<?>, ResultSetAccessor>();
	static {
		FAST_ACCESSOR_MAP_PRIMTIVE.put(Integer.TYPE, I);
		FAST_ACCESSOR_MAP_PRIMTIVE.put(Long.TYPE, J);
		FAST_ACCESSOR_MAP_PRIMTIVE.put(Float.TYPE, F);
		FAST_ACCESSOR_MAP_PRIMTIVE.put(Short.TYPE, S);
		FAST_ACCESSOR_MAP_PRIMTIVE.put(Double.TYPE, D);
		FAST_ACCESSOR_MAP_PRIMTIVE.put(Boolean.TYPE, Z);
		FAST_ACCESSOR_MAP_PRIMTIVE.put(Byte.TYPE, B);
		FAST_ACCESSOR_MAP_PRIMTIVE.put(Character.TYPE, new ResultCharacterAccessor((char)0));

		FAST_ACCESSOR_MAP.put(Integer.TYPE, INT);
		FAST_ACCESSOR_MAP.put(Long.TYPE, LONG);
		FAST_ACCESSOR_MAP.put(Float.TYPE, FLOAT);
		FAST_ACCESSOR_MAP.put(Double.TYPE, DOUBLE);
		FAST_ACCESSOR_MAP.put(Boolean.TYPE, BOOLEAN);
		FAST_ACCESSOR_MAP.put(Short.TYPE, SHORT);
		FAST_ACCESSOR_MAP.put(Byte.TYPE, BYTE);
		FAST_ACCESSOR_MAP.put(Character.TYPE, CHARACTER);

		FAST_ACCESSOR_MAP.put(Integer.class, INT);
		FAST_ACCESSOR_MAP.put(Long.class, LONG);
		FAST_ACCESSOR_MAP.put(Float.class, FLOAT);
		FAST_ACCESSOR_MAP.put(Double.class, DOUBLE);
		FAST_ACCESSOR_MAP.put(Boolean.class, BOOLEAN);
		FAST_ACCESSOR_MAP.put(Short.class, SHORT);
		FAST_ACCESSOR_MAP.put(Byte.class, BYTE);
		FAST_ACCESSOR_MAP.put(Character.class, CHARACTER);

		FAST_ACCESSOR_MAP.put(String.class, STRING);
		FAST_ACCESSOR_MAP.put(java.util.Date.class, TIMESTAMP);
		FAST_ACCESSOR_MAP.put(BigDecimal.class, BIGDECIMAL);
		FAST_ACCESSOR_MAP.put(java.sql.Date.class, DATE);
		FAST_ACCESSOR_MAP.put(java.sql.Time.class, TIME);
		FAST_ACCESSOR_MAP.put(java.sql.Timestamp.class, TIMESTAMP);
		FAST_ACCESSOR_MAP.put(byte[].class, BYTES);
		FAST_ACCESSOR_MAP.put(Object.class, RAW);
	}

	/**
	 * 根据值得到ResultSetAccessor
	 * 
	 * @param javaType
	 * @param ctype
	 * @param c
	 * @param allowPrmitive
	 * @return
	 */
	public static ResultSetAccessor getAccessor(Class<?> javaType, ColumnMapping ctype, ColumnDescription c, boolean allowPrmitive) {
		/*
		 * 已知字段映射
		 */
		if (ctype != null) {
			if (javaType == null || javaType == ctype.getFieldType() || javaType.isAssignableFrom(ctype.getFieldType())) {
				return ctype;
			}
		}
		ResultSetAccessor rsa;
		if (allowPrmitive && javaType.isPrimitive()) {
			rsa = FAST_ACCESSOR_MAP_PRIMTIVE.get(javaType);
			if (rsa != null) {
				return rsa;
			}
		} else {
			rsa = FAST_ACCESSOR_MAP.get(javaType);
			if (rsa != null && rsa.applyFor(c.getType())) {
				return rsa;
			}
		}
		if (c.getType() == 0) {
			return RAW;
		}
		try {

			// LogUtil.warn("Dynamic Mapping creating from [{}] to sql-type:[{}]",
			// javaType.getName(), c.getType());
			switch (c.getType()) {
			case Types.LONGNVARCHAR:
			case Types.LONGVARCHAR:
			case Types.NVARCHAR:
			case Types.NCHAR:
			case Types.VARCHAR:
			case Types.CHAR:
				return StringType.getMappingType(javaType);
			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
				return IntType.getMappingType(javaType);
			case Types.BIGINT:
				return LongType.getMappingType(javaType);
			case Types.REAL:
			case Types.DOUBLE:
				return DoubleType.getMappingType(javaType);
			case Types.FLOAT:
				return FloatType.getMappingType(javaType);
				// case Types.NUMERIC:
				// case Types.DECIMAL:
				// case Types.REAL:
			case Types.BINARY:
			case Types.LONGVARBINARY:
			case Types.BLOB:
				return BlobType.getMappingType(javaType);
			case Types.CLOB:
			case Types.NCLOB:
				return ClobType.getMappingType(javaType);
			case Types.BOOLEAN:
				return new BooleanBoolMapping();
			case Types.DATE:
				return DateType.getMappingType(javaType);
			case Types.TIME:
			case Types.TIMESTAMP:
				return TimestampType.getMappingType(javaType);
			case Types.ROWID:
				return ROWID;
			default:
				if (rsa != null) {
					LogUtil.warn("The result accessor [{}] was nor extractly match the column type[{}], but no better accessor was found.", rsa, c.getType());
					return rsa;
				}
				throw new IllegalArgumentException("No Proper Accessor found! " + c+"expect java:"+javaType);
			}
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(c.toString(), e);
		}
	}

	/**
	 * 得到某个值在该数据库下的表达式
	 * 
	 * @param value
	 * @param profile
	 * @return
	 */
	public static String getSqlStr(Object value, DatabaseDialect profile) {
		if (value == null)
			return "null";
		if (value instanceof Expression) {
			return value.toString();
		} else if (value instanceof String) {
			return AColumnMapping.wrapSqlStr((String) value);
		} else if (value instanceof java.lang.Number) {
			return value.toString();
		} else if (value instanceof java.util.Date) {
			Date date = (Date) value;
			if (DateUtils.isDayBegin(date)) {
				return profile.getSqlDateExpression(date);
			} else {
				return profile.getSqlTimestampExpression(date);
			}
		}
		throw new NullPointerException("Unknown javaField Type." + value);
	}
}
