package jef.database.meta;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.GenerationType;
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
