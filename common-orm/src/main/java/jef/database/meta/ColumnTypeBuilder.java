package jef.database.meta;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GenerationType;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import jef.database.DbUtils;
import jef.database.annotation.Parameter;
import jef.database.dialect.ColumnType;
import jef.database.dialect.TypeDefImpl;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jsqlparser.statement.create.ColumnDefinition;
import jef.database.meta.AnnotationProvider.FieldAnnotationProvider;
import jef.database.meta.def.GenerateTypeDef;
import jef.database.query.SqlExpression;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.Primitives;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapper;

/**
 * 根据字段上的注解来计算ColumnType对象
 * 
 * @author publicxtgxrj10
 * 
 */
public class ColumnTypeBuilder {
	/**
	 * java 字段
	 */
	private java.lang.reflect.Field field;

	/**
	 * Java数据类型（不一定等于field.getType()，可能受注解@Type影响）
	 */
	private Class<?> javaType;
	/**
	 * AnnotationProvider
	 */
	private FieldAnnotationProvider fieldProvider;

	// ////////////////////////////初步解析后的数据////////////////////////////////////////
	/**
	 * 注解中的类型
	 * 
	 * Eg. varchar(100)中的varchar
	 */
	private String def;
	/**
	 * 注解中的类型参数
	 * 
	 * Eg. varchar(100)中的100
	 */
	private String[] typeArgs;
	// ///////////完全解析后的数据/////////////

	/**
	 * 是否为null
	 */
	private boolean nullable;
	/**
	 * 是否唯一
	 */
	private boolean unique;
	/**
	 * 长度
	 */
	private int length;
	/**
	 * 精度
	 */
	private int precision;
	/**
	 * 小数位数
	 */
	private int scale;
	/**
	 * 是否为版本字段
	 */
	private boolean version;
	/**
	 * 是否为LOB字段
	 */
	private boolean lob;
	// ////////////////////////////////////
	/**
	 * 缺省值：仅当指定了default关键字后才有值
	 */
	private SqlExpression defaultExpression = null;
	/**
	 * 自增
	 */
	private GenerateTypeDef generatedValue;

	/**
	 * 自定义的映射
	 */
	private ColumnMapping customType;

	public ColumnTypeBuilder(Column col, java.lang.reflect.Field field, Class<?> treatJavaType, FieldAnnotationProvider fieldProvider) {
		this.field = field;
		this.javaType = treatJavaType;
		this.fieldProvider = fieldProvider;
		init(col);
	}

	private void init(Column col) {
		generatedValue = GenerateTypeDef.create(fieldProvider.getAnnotation(javax.persistence.GeneratedValue.class));
		version = fieldProvider.getAnnotation(javax.persistence.Version.class) != null;
		lob = fieldProvider.getAnnotation(Lob.class) != null;
		if (col != null) {
			length = col.length();
			precision = col.precision();
			scale = col.scale();
			nullable = col.nullable();
			unique = col.unique();
			if (col.columnDefinition().length() > 0) {
				parseColumnDef(col.columnDefinition());
			}
		} else {
			nullable = !javaType.isPrimitive();
		}
	}

	private void parseColumnDef(String columnDef) {
		ColumnDefinition c;
		c = DbUtils.parseColumnDef(columnDef);
		this.def = c.getColDataType().getDataType().toUpperCase();
		List<String> params = c.getColDataType().getArgumentsStringList();
		this.typeArgs = params == null ? ArrayUtils.EMPTY_STRING_ARRAY : params.toArray(new String[params.size()]);
		// 在ColumnDef中定义的属性，优先级更高
		if (c.getColumnSpecStrings() != null) {
			for (int i = 0; i < c.getColumnSpecStrings().size(); i++) {
				String spec = c.getColumnSpecStrings().get(i);
				if ("not".equalsIgnoreCase(spec)) {
					i++;
					String nextWord = c.getColumnSpecStrings().get(i);
					if ("null".equalsIgnoreCase(nextWord)) {
						nullable = false;
					}
				} else if ("null".equalsIgnoreCase(spec)) {
					nullable = true;
				} else if ("unique".equalsIgnoreCase(spec)) {
					unique = true;
				} else {
					if ("default".equalsIgnoreCase(spec)) {
						String ex = c.getColumnSpecStrings().get(++i);
						if (ex.length() > 0) {
							defaultExpression = new SqlExpression(ex);
						}
					}
				}
			}
		}
	}

	public ColumnType build() {
		ColumnType result;
		if (customType != null) {
			int sqlType = customType.getSqlType();
			if (lob) {
				if (isCharType(sqlType)) {
					sqlType = Types.CLOB;
				} else {
					sqlType = Types.BLOB;
				}
			}
			result = new TypeDefImpl(def, sqlType).javaType(field.getType()).spec(length, precision, scale);
		} else if (this.def == null) {
			result = createDefault();
		} else {
			result = createByDef();
		}
		result.setUnique(unique);
		result.setDefault(defaultExpression);
		return result.setNullable(nullable);
	}

	private boolean isCharType(int sqlType) {
		return sqlType == Types.CHAR || sqlType == Types.VARCHAR || sqlType == Types.CLOB;
	}

	private ColumnType createDefault() {
		if (this.generatedValue != null && this.generatedValue.isKeyGeneration() && javaType == String.class) {
			return new ColumnType.GUID(length);
		} else if (generatedValue != null && generatedValue.isKeyGeneration() && Number.class.isAssignableFrom(Primitives.toWrapperClass(javaType))) {
			return new ColumnType.AutoIncrement(precision, generatedValue.getGeType(), fieldProvider);
		}

		if (javaType == String.class) {
			if (lob)
				return new ColumnType.Clob();
			return new ColumnType.Varchar(length > 0 ? length : 255);
		} else if (javaType == Integer.class || javaType == Integer.TYPE) {
			return new ColumnType.Int(precision).setVersion(version);
		} else if (javaType == Double.class || javaType == Double.TYPE) {
			return new ColumnType.Double(precision, scale);
		} else if (javaType == Float.class || javaType == Float.TYPE) {
			return new ColumnType.Double(precision, scale);
		} else if (javaType == Boolean.class || javaType == Boolean.TYPE) {
			return new ColumnType.Boolean();
		} else if (javaType == Long.class || javaType == Long.TYPE) {
			return createLong(precision, version, generatedValue);
		} else if (javaType == Character.class || javaType == Character.TYPE) {
			return new ColumnType.Char(1);
		} else if (javaType == BigDecimal.class) {
			return new ColumnType.Double(16, 6);
		} else if (javaType == Date.class) {
			Temporal t = fieldProvider.getAnnotation(Temporal.class);
			return ColumnTypeBuilder.newDateTimeColumnDef(t == null ? TemporalType.TIMESTAMP : t.value(), generatedValue, version);
		} else if (javaType == java.sql.Date.class || javaType == LocalDate.class) {
			return ColumnTypeBuilder.newDateTimeColumnDef(TemporalType.DATE, generatedValue, false);
		} else if (javaType == java.sql.Time.class || javaType == LocalTime.class) {
			return ColumnTypeBuilder.newDateTimeColumnDef(TemporalType.TIME, generatedValue, false);
		} else if (javaType == java.sql.Timestamp.class || javaType == LocalDateTime.class || javaType == Instant.class) {
			return ColumnTypeBuilder.newDateTimeColumnDef(TemporalType.TIMESTAMP, generatedValue, version);
		} else if (Enum.class.isAssignableFrom(javaType)) {
			Enumerated e = fieldProvider.getAnnotation(Enumerated.class);
			if (e.value() == EnumType.ORDINAL) {
				return new ColumnType.Int(2);
			} else {
				return new ColumnType.Varchar(length > 0 ? length : 32);
			}
		} else if (javaType.isArray() && javaType.getComponentType() == Byte.TYPE) {
			return new ColumnType.Blob();
		} else if (javaType == File.class) {
			return new ColumnType.Blob();
		} else if (javaType == YearMonth.class) {
			return new ColumnType.Char(7);
		} else {
			throw new IllegalArgumentException("Java type " + javaType.getName() + " can't mapping to a Db column type by default");
		}
	}

	@Deprecated
	private static ColumnType createLong(int precision, boolean version, GenerateTypeDef gType) {
		ColumnType.Int i = new ColumnType.Int(precision > 0 ? precision : 16);
		i.setGenerateType(gType == null ? null : gType.getDateGenerate());
		i.setVersion(version);
		return i;
	}

	private ColumnType createByDef() {
		if ("VARCHAR".equals(def) || "VARCHAR2".equals(def)) {
			if (generatedValue != null && generatedValue.isKeyGeneration()) {
				GenerationType generationType = generatedValue.getGeType();
				if (generationType == GenerationType.TABLE || generationType == GenerationType.SEQUENCE) {
					return new ColumnType.AutoIncrement(length, generationType, fieldProvider);
				} else {
					return new ColumnType.GUID(length);
				}
			}
			length = getParamInt(0, length);
			Assert.isTrue(length > 0, "The length of a varchar column must greater than 0!");
			return new ColumnType.Varchar(length);
		} else if ("CHAR".equalsIgnoreCase(def)) {
			if (generatedValue != null && generatedValue.isKeyGeneration()) {
				GenerationType generationType = generatedValue.getGeType();
				if (generationType == GenerationType.TABLE || generationType == GenerationType.SEQUENCE) {
					return new ColumnType.AutoIncrement(length, generationType, fieldProvider);
				} else {
					return new ColumnType.GUID(length);
				}
			}
			length = getParamInt(0, length);
			Assert.isTrue(length > 0, "The length of a char column must greater than 0!");
			return new ColumnType.Char(length);
		} else if ("NUMBER".equals(def) || "NUMERIC".equals(def)) {
			this.precision = getParamInt(0, precision);
			this.scale = getParamInt(1, scale);
			return createNumberType();
		} else if ("DOUBLE".equals(def)) {
			if (this.precision < 1)
				precision = 16;
			if (this.scale < 1)
				scale = 8;
			return createNumberType();
		} else if ("FLOAT".equals(def)) {
			if (this.precision < 1)
				precision = 12;
			if (this.scale < 1)
				scale = 6;
			return createNumberType();
		} else if ("INT".equals(def) || "INTEGER".equals(def)) {
			return createNumberType();
		} else if ("CLOB".equalsIgnoreCase(def)) {
			return new ColumnType.Clob();
		} else if ("BLOB".equalsIgnoreCase(def)) {
			return new ColumnType.Blob();
		} else if ("Date".equalsIgnoreCase(def)) {
			Temporal temporal = fieldProvider.getAnnotation(Temporal.class);
			TemporalType t = temporal == null ? TemporalType.DATE : temporal.value();
			return newDateTimeColumnDef(t, generatedValue, version);
		} else if ("TIMESTAMP".equalsIgnoreCase(def) || "DATETIME".equalsIgnoreCase(def)) {
			Temporal temporal = fieldProvider.getAnnotation(Temporal.class);
			TemporalType t = temporal == null ? TemporalType.TIMESTAMP : temporal.value();
			return newDateTimeColumnDef(t, generatedValue, version);
		} else if ("BOOLEAN".equalsIgnoreCase(def)) {
			return new ColumnType.Boolean();
		} else if ("XML".equalsIgnoreCase(def)) {
			return new ColumnType.XML();
		} else if ("BIT".equalsIgnoreCase(def)) {
			return new ColumnType.Boolean();
		} else {
			throw new IllegalArgumentException("Unknow column Definition[" + def + "] in entity " + field.getDeclaringClass());
		}
	}

	private ColumnType createNumberType() {
		// 修正precision精度
		if (precision == 0 && (length > 0 && length < 100)) {
			precision = length;
		}

		if (generatedValue != null && generatedValue.isKeyGeneration()) {
			return new ColumnType.AutoIncrement(precision, generatedValue.getGeType(), fieldProvider);
		} else if (scale > 0) {
			return new ColumnType.Double(precision, scale);
		} else {
			ColumnType.Int cType = new ColumnType.Int(precision);
			cType.setVersion(version);
			return cType;
		}
	}

	private int getParamInt(int index, int defaultValue) {
		if (typeArgs.length > index) {
			return StringUtils.toInt(typeArgs[index], defaultValue);
		}
		return defaultValue;
	}

	static ColumnType newDateTimeColumnDef(TemporalType temporalType, GenerateTypeDef gv, boolean version) {
		ColumnType ct;
		switch (temporalType) {
		case DATE:
			ct = new ColumnType.Date().setGenerateType(gv == null ? null : gv.getDateGenerate());
			break;
		case TIME:
			ct = new ColumnType.TimeStamp().setGenerateType(gv == null ? null : gv.getDateGenerate());// FIXME
			break;
		case TIMESTAMP:
			ColumnType.TimeStamp ctt = new ColumnType.TimeStamp();
			ctt.setVersion(version);
			ctt.setGenerateType(gv == null ? null : gv.getDateGenerate());
			ct = ctt;
			break;
		default:
			throw new UnsupportedOperationException();
		}
		return ct;
	}

	static void applyParams(Parameter[] parameters, ColumnMapping type) {
		if (parameters == null || parameters.length == 0)
			return;
		BeanWrapper bw = BeanWrapper.wrap(type);
		for (Parameter p : parameters) {
			if (bw.isWritableProperty(p.name())) {
				bw.setPropertyValueByString(p.name(), p.value());
			}
		}
	}

	public ColumnTypeBuilder withCustomType(ColumnMapping type) {
		this.customType = type;
		return this;
	}
}
