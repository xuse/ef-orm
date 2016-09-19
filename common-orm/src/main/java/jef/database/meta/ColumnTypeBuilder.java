package jef.database.meta;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.GenerationType;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import jef.database.DbUtils;
import jef.database.annotation.Parameter;
import jef.database.dialect.ColumnType;
import jef.database.dialect.TypeDefImpl;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.create.ColumnDefinition;
import jef.database.meta.AnnotationProvider.FieldAnnotationProvider;
import jef.database.meta.def.GenerateTypeDef;
import jef.database.query.SqlExpression;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.BeanWrapper;

/**
 * 根据字段上的注解来计算ColumnType对象
 * @author publicxtgxrj10
 *
 */
public class ColumnTypeBuilder {
	/**
	 * 列定义
	 */
	private Column col;
	/**
	 * 字段
	 */
	private java.lang.reflect.Field field;
	/**
	 * AnnotationProvider
	 */
	private FieldAnnotationProvider fieldProvider;

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
	// ////////////////////////

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

	// ////////////////////////////////////
	/**
	 * 缺省值：仅当指定了default关键字后才有值
	 */
	private SqlExpression defaultExpression = null;
	/**
	 * 自增
	 */
	private GenerateTypeDef generatedValue;
	public ColumnTypeBuilder(Column col, java.lang.reflect.Field field, FieldAnnotationProvider fieldProvider) {
		this.col = col;
		this.field = field;
		this.fieldProvider = fieldProvider;
		init();
	}

	
	
	private void init() {
		length = col.length();
		precision = col.precision();
		scale = col.scale();
		nullable = col.nullable();
		unique = col.unique();

		
		generatedValue = GenerateTypeDef.create(fieldProvider.getAnnotation(javax.persistence.GeneratedValue.class));
		version = fieldProvider.getAnnotation(javax.persistence.Version.class) != null;

		ColumnDefinition c;
		try {
			c = DbUtils.parseColumnDef(col.columnDefinition().toUpperCase());
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		this.def = c.getColDataType().getDataType();
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
		ColumnType ct = create();
		ct.setUnique(unique);
		return ct.setNullable(nullable).defaultIs(defaultExpression);
	}
	
	static ColumnType parseJavaFieldType(Column c, Class<?> type,FieldAnnotationProvider fieldProvider) {
		int len = c == null ? 0 : c.length();
		int precision = c == null ? 0 : c.precision();
		int scale = c == null ? 0 : c.scale();
		boolean nullable = c == null ? true : c.nullable();
		GenerateTypeDef geType = GenerateTypeDef.create(fieldProvider.getAnnotation(javax.persistence.GeneratedValue.class));

		boolean version = fieldProvider.getAnnotation(javax.persistence.Version.class) != null;
		if (geType != null && geType.isKeyGeneration() && type == String.class) {
			return new ColumnType.GUID();
		} else if (geType != null && geType.isKeyGeneration() && Number.class.isAssignableFrom(BeanUtils.toWrapperClass(type))) {
			return new ColumnType.AutoIncrement(precision, geType.getGeType(), fieldProvider);
		}
		Lob lob = fieldProvider.getAnnotation(Lob.class);
		if (type == String.class) {
			if (lob != null)
				return new ColumnType.Clob().setNullable(nullable);
			return new ColumnType.Varchar(len > 0 ? len : 255).setNullable(nullable);
		} else if (type == Integer.class) {
			return new ColumnType.Int(precision).setVersion(version).setNullable(nullable);
		} else if (type == Integer.TYPE) {
			return new ColumnType.Int(precision).setVersion(version).setNullable(nullable);
		} else if (type == Double.class) {
			return new ColumnType.Double(precision, scale).setNullable(nullable);
		} else if (type == Double.TYPE) {
			return new ColumnType.Double(precision, scale).setNullable(nullable);
		} else if (type == Float.class) {
			return new ColumnType.Double(precision, scale).setNullable(nullable);
		} else if (type == Float.TYPE) {
			return new ColumnType.Double(precision, scale).setNullable(nullable);
		} else if (type == Boolean.class) {
			return new ColumnType.Boolean().setNullable(nullable);
		} else if (type == Boolean.TYPE) {
			return new ColumnType.Boolean().setNullable(false);
		} else if (type == Long.class) {
			return createLong(precision,version,nullable,geType);
		} else if (type == Long.TYPE) {
			return  createLong(precision,version,nullable,geType);
		} else if (type == Character.class) {
			return new ColumnType.Char(1).setNullable(nullable);
		} else if (type == Character.TYPE) {
			return new ColumnType.Char(1).setNullable(nullable);
		} else if (type == BigDecimal.class) {
			return new ColumnType.Double(16, 6).setNullable(nullable);
		} else if (type == Date.class) {
			Temporal t = fieldProvider.getAnnotation(Temporal.class);
			return ColumnTypeBuilder.newDateTimeColumnDef(t == null ? TemporalType.TIMESTAMP : t.value(), geType, version).setNullable(nullable);
		} else if (type == java.sql.Date.class) {
			return ColumnTypeBuilder.newDateTimeColumnDef(TemporalType.DATE, geType, false).setNullable(nullable);
		} else if (type == java.sql.Timestamp.class) {
			return ColumnTypeBuilder.newDateTimeColumnDef(TemporalType.TIMESTAMP, geType, version).setNullable(nullable);
		} else if (type == java.sql.Time.class) {
			return ColumnTypeBuilder.newDateTimeColumnDef(TemporalType.TIME, geType, false).setNullable(nullable);
		} else if (Enum.class.isAssignableFrom(type)) {
			return new ColumnType.Varchar(len > 0 ? len : 32).setNullable(nullable);
		} else if (type.isArray() && type.getComponentType() == Byte.TYPE) {
			return new ColumnType.Blob().setNullable(nullable);
		} else if (type == File.class) {
			return new ColumnType.Blob().setNullable(nullable);
		} else {
			throw new IllegalArgumentException("Java type " + type.getName() + " can't mapping to a Db column type by default");
		}
	}


	

	private static ColumnType createLong(int precision, boolean version, boolean nullable,GenerateTypeDef gType) {
		ColumnType.Int i = new ColumnType.Int(precision > 0 ? precision : 16);
		i.setGenerateType(gType==null?null:gType.getDateGenerate());
		i.setVersion(version).setNullable(nullable);
		return i;
	}
	
	private ColumnType create() {
		if ("VARCHAR".equals(def) || "VARCHAR2".equals(def)) {
			if (generatedValue != null && generatedValue.isKeyGeneration()) {
				GenerationType generationType=generatedValue.getGeType();
				if (generationType == GenerationType.TABLE || generationType == GenerationType.SEQUENCE) {
					return new ColumnType.AutoIncrement(length, generationType, fieldProvider);
				} else {
					return new ColumnType.GUID();
				}
			}
			length = getParamInt(0, length);
			Assert.isTrue(length > 0, "The length of a varchar column must greater than 0!");
			return new ColumnType.Varchar(length);
		} else if ("CHAR".equalsIgnoreCase(def)) {
			if (generatedValue != null  && generatedValue.isKeyGeneration()) {
				GenerationType generationType=generatedValue.getGeType();
				if (generationType == GenerationType.TABLE || generationType == GenerationType.SEQUENCE) {
					return new ColumnType.AutoIncrement(length, generationType, fieldProvider);
				} else {
					return new ColumnType.GUID();
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
			if(this.precision<1)precision=16;
			if(this.scale<1)scale=8;
			return createNumberType();
		} else if ("FLOAT".equals(def)) {
			if(this.precision<1)precision=12;
			if(this.scale<1)scale=6;
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
		} else if ("TIMESTAMP".equalsIgnoreCase(def)) {
			Temporal temporal = fieldProvider.getAnnotation(Temporal.class);
			TemporalType t = temporal == null ? TemporalType.TIMESTAMP : temporal.value();
			return newDateTimeColumnDef(t, generatedValue, version);
		} else if ("BOOLEAN".equalsIgnoreCase(def)) {
			return new ColumnType.Boolean();
		} else if ("XML".equalsIgnoreCase(def)) {
			return new ColumnType.XML();
		} else if ("BIT".equalsIgnoreCase(def)) {
			return new ColumnType.Boolean();
		} else if (fieldProvider.getAnnotation(jef.database.annotation.Type.class) != null) {
			jef.database.annotation.Type t = fieldProvider.getAnnotation(jef.database.annotation.Type.class);
			ColumnMapping cm = BeanUtils.newInstance(t.value());
			try {
				applyParams(t.parameters(), cm);
			} catch (Exception e) {
				throw new IllegalArgumentException("@Type annotation on field " + field + " is invalid", e);
			}
			return new TypeDefImpl(def, cm.getSqlType(), field.getType());
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
			return new ColumnType.Double(precision, scale).setNullable(nullable).defaultIs(defaultExpression);
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
			ct = new ColumnType.Date().setGenerateType(gv==null?null:gv.getDateGenerate());
			break;
		case TIME:
			ct = new ColumnType.TimeStamp().setGenerateType(gv==null?null:gv.getDateGenerate());// FIXME
			break;
		case TIMESTAMP:
			ColumnType.TimeStamp ctt = new ColumnType.TimeStamp();
			ctt.setVersion(version);
			ctt.setGenerateType(gv==null?null:gv.getDateGenerate());
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
}
