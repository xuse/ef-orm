package com.github.geequery.extension.querydsl;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.PathMetadataFactory;
import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.types.EnumByNameType;
import com.querydsl.sql.types.EnumByOrdinalType;
import com.querydsl.sql.types.Type;
import jef.database.DataObject;
import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.dialect.SqlTypeSized;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;

import javax.persistence.Enumerated;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * GeeQuery DataObject对应的QueryDSL模型.
 */
public class SQLRelationalPath<T> extends RelationalPathBase<T> implements Cloneable {
	private static final long serialVersionUID = 1L;
	/**
	 * 字段列,值为SimpleExpression
	 */
	private final Map<Field, Expression<?>> map = new HashMap<>();
	/**
	 * 自定义类型
	 */
	protected final Map<ColumnMapping, Type<?>> typeMap = new HashMap<>(10);

	private Class<T> clz;

	/**
	 * 
	 * @return
	 */
	public Map<Field, Expression<?>> getMap() {
		return map;
	}

	/**
	 * 构造
	 * 
	 * @param type
	 */
	public SQLRelationalPath(Class<? extends DataObject> type) {
		this(MetaHolder.getMeta(type));
	}

	protected SQLRelationalPath(Class<? extends DataObject> type, String variable) {
		this(MetaHolder.getMeta(type), variable);
	}

	public SQLRelationalPath(ITableMetadata tm) {
		this(tm, tm.getTableName(false));
	}

	@SuppressWarnings("unchecked")
	protected SQLRelationalPath(ITableMetadata tm, String variable) {
		super((Class<T>) tm.getThisType(), PathMetadataFactory.forVariable(variable), tm.getSchema(), tm.getTableName(false));
		init(tm);
	}

	@SuppressWarnings("unchecked")
	private void init(ITableMetadata tm) {
		this.clz = (Class<T>) tm.getThisType();
		int i = 1;
		for (ColumnMapping cm : tm.getColumns()) {
			Expression<?> path = getBeanMappingType(cm);
			addMetadata(cm, (Path<?>) path, i++);
			map.put(cm.field(), path);
		}
		createPrimaryKey(tm);
	}

	protected SQLRelationalPath(Class<? extends T> type, String variable, String schema, String table) {
		super(type, variable, schema, table);
	}

	protected SQLRelationalPath(Class<? extends T> type, PathMetadata metadata, String schema, String table) {
		super(type, metadata, schema, table);
	}

	protected void createPrimaryKey(ITableMetadata tm) {
		int i = 0;
		Path<?>[] pkPaths = new Path[tm.getPKFields().size()];
		for (ColumnMapping cm : tm.getPKFields()) {
			pkPaths[i] = (Path<?>) map.get(cm.field());
			i++;
		}
		if (pkPaths.length > 0) {
			createPrimaryKey(pkPaths);
		}
	}

	protected void addMetadata(ColumnMapping cm, Path<?> path, int index) {
		String columnName = cm.rawColumnName();
		ColumnMetadata cmd = ColumnMetadata.named(columnName).ofType(cm.getSqlType()).withIndex(index);
		ColumnType type = cm.get();
		if (type instanceof SqlTypeSized) {
			cmd = cmd.withSize(((SqlTypeSized) type).getLength());
		}
		if (!type.isNullable()) {
			cmd = cmd.notNull();
		}
		addMetadata(path, cmd);
	}

	/**
	 * 可以转换为NumberPath,BooleanPath...<br>
	 * 限制:此处不可使用in数组(in数组存在类型转换失败问题);解决办法可以直接传入List或Set对象,或者使用原始的Path.
	 * 
	 * @param field
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <S> SimpleExpression<S> column(Field field) {
		return column(field, SimpleExpression.class);
	}

	/**
	 * @param field
	 * @param <S>
	 * @return
	 */
	public <S> SimpleExpression<S> expression(String expression, Class<S> type) {
		return super.createSimple(expression, type);
	}

	/**
	 * 得到一个表达式对象
	 * 
	 * @param expression
	 * @return
	 */
	public SimpleExpression<?> expression(String expression) {
		return super.createSimple(expression, Object.class);
	}

	public BooleanPath bool(Field field) {
		return column(field, BooleanPath.class);
	}

	public StringPath string(Field field) {
		return column(field, StringPath.class);
	}

	@SuppressWarnings("unchecked")
	public <P extends Number & Comparable<?>> NumberPath<P> number(Field field) {
		return column(field, NumberPath.class);
	}

	@SuppressWarnings("unchecked")
	public <P extends Enum<P>> EnumPath<P> enums(Field field) {
		return column(field, EnumPath.class);
	}

	@SuppressWarnings("unchecked")
	public <P> SimplePath<P> simple(Field field) {
		return column(field, SimplePath.class);
	}

	@SuppressWarnings("unchecked")
	public <A, E> ArrayPath<A, E> array(Field field) {
		return column(field, ArrayPath.class);
	}

	@SuppressWarnings("unchecked")
	public <P extends Comparable<P>> ComparablePath<P> comparable(Field field) {
		return column(field, ComparablePath.class);
	}

	@SuppressWarnings("unchecked")
	public <P extends Comparable<P>> DatePath<P> date(Field field) {
		return column(field, DatePath.class);
	}

	@SuppressWarnings("unchecked")
	public <P extends Comparable<P>> DateTimePath<P> dateTime(Field field) {
		return column(field, DateTimePath.class);
	}

	@SuppressWarnings("unchecked")
	public <P extends Comparable<P>> TimePath<P> time(Field field) {
		return column(field, TimePath.class);
	}

	/**
	 * 不支持
	 * 
	 * @param field
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <E, Q extends SimpleExpression<? super E>> CollectionPath<E, Q> collection(Field field) {
		return column(field, CollectionPath.class);
	}

	/**
	 * 不支持
	 * 
	 * @param field
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <E, Q extends SimpleExpression<? super E>> SetPath<E, Q> set(Field field) {
		return column(field, SetPath.class);
	}

	/**
	 * 不支持
	 * 
	 * @param field
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <E, Q extends SimpleExpression<? super E>> ListPath<E, Q> list(Field field) {
		return column(field, ListPath.class);
	}

	/**
	 * 不支持
	 * 
	 * @param field
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <K, V, E extends SimpleExpression<? super V>> MapPath<K, V, E> map(Field field) {
		return column(field, MapPath.class);
	}

	@SuppressWarnings("unchecked")
	private <P> P column(Field field, Class<P> clz) {
		Expression<?> path = map.get(field);
		if (path == null) {// 字段不存在
			throw new NoSuchElementException("field not exists!");
		}
		if (clz != null) {
			if (clz.isAssignableFrom(path.getClass())) {
				return (P) path;
			} else {
				// 类型转换错误
				throw new ClassCastException("field cast error!");
			}
		} else {
			return (P) path;
		}
	}

	protected Expression<?> getBeanMappingType(ColumnMapping cm) {
		Class<?> mirror = cm.getFieldType();
		String fieldName = cm.fieldName();
		int sqlType = cm.getSqlType();
		// String and char
		if (mirror == String.class) {
			return createString(fieldName);
		}
		// Boolean
		if (mirror == Boolean.class || mirror == Boolean.TYPE) {
			return createBoolean(fieldName);
		}
		// Byte
		if (mirror == Byte.class || mirror == Byte.TYPE) {
			return createNumber(fieldName, Byte.class);
		}
		// Short
		if (mirror == Short.class || mirror == Short.TYPE) {
			return createNumber(fieldName, Short.class);
		}
		// Int
		if (mirror == Integer.class || mirror == Integer.TYPE) {
			return createNumber(fieldName, Integer.class);
		}
		// Float
		if (mirror == Float.class || mirror == Float.TYPE) {
			return createNumber(fieldName, Float.class);
		}
		// Double
		if (mirror == Double.class || mirror == Double.TYPE) {
			return createNumber(fieldName, Double.class);
		}
		// Long
		if (mirror == Long.class || mirror == Long.TYPE) {
			return createNumber(fieldName, Long.class);
		}
		// BigDecimal
		if (mirror == BigDecimal.class) {
			return createNumber(fieldName, BigDecimal.class);
		}
		// BigInteger
		if (mirror == BigInteger.class) {
			return createNumber(fieldName, BigInteger.class);
		}

		// Enum
		if (mirror.isEnum()) {
			Enumerated enumerated = mirror.getAnnotation(Enumerated.class);
			if (enumerated != null) {
				switch (enumerated.value()) {
				case ORDINAL:
					typeMap.put(cm, new EnumByNameType(mirror));
					break;
				case STRING:
					typeMap.put(cm, new EnumByOrdinalType(mirror));
					break;
				default:
					break;
				}
			}
			return createEnum(fieldName, (Class) mirror);
		}
		// Char
		if (mirror == CharSequence.class || mirror == Character.TYPE) {
			return createSimple(fieldName, Character.class);
		}

		// jdk8日期
		if (mirror == LocalDate.class) {
			return createDate(fieldName, LocalDate.class);
		}
		if (mirror == LocalDateTime.class) {
			return createDateTime(fieldName, LocalDateTime.class);
		}
		if (mirror == LocalTime.class) {
			return createTime(fieldName, LocalTime.class);
		}
		// 日期处理
		if (Date.class.isAssignableFrom(mirror)) {
			if (sqlType != 0) {
				switch (sqlType) {
				case Types.DATE:
					return createDate(fieldName, (Class<? super Comparable>) mirror);
				case Types.TIME:
					return createTime(fieldName, (Class<? super Comparable>) mirror);
				case Types.TIMESTAMP:
					return createDateTime(fieldName, (Class<? super Comparable>) mirror);
				default:
					break;
				}
			}
			return createDateTime(fieldName, (Class<? super Comparable>) mirror);
		}

		// byte[]
		if (mirror.isArray()) {
			return createArray(fieldName, (Class<? super Object>) mirror);
		}
		// 默认情况
		return createSimple(fieldName, (Class<? super Object>) mirror);
	}

	/**
	 * 新创建,该方法可以实现将枚举值强行转换为string使用.
	 * 
	 * @param field
	 * @return
	 */
	public StringPath stringNew(Field field) {
		AbstractMetadata tm = MetaHolder.getMeta(this.clz);
		ColumnMapping cm = tm.getColumnDef(field);
		if (cm == null) {
			throw new RuntimeException("field not found");
		}
		String fieldName = cm.fieldName();
		StringPath path = createString(fieldName);
		// addMetadata(cm, path);
		return path;
	}

	/**
	 * 新创建,该方法可以实现将枚举值强行转换为数字类型使用.
	 * 
	 * @param field
	 * @param tClass
	 * @param <T>
	 * @return
	 */
	public <T extends Number & Comparable<?>> NumberPath<T> numberNew(Field field, Class<T> tClass) {
		AbstractMetadata tm = MetaHolder.getMeta(this.clz);
		ColumnMapping cm = tm.getColumnDef(field);
		if (cm == null) {
			throw new RuntimeException("field not found");
		}
		String fieldName = cm.fieldName();
		NumberPath<T> path = createNumber(fieldName, tClass);
		// addMetadata(cm, path);
		return path;
	}

	/**
	 * 新创建
	 * 
	 * @param field
	 * @return
	 */
	public BooleanPath booleanNew(Field field) {
		AbstractMetadata tm = MetaHolder.getMeta(this.clz);
		ColumnMapping cm = tm.getColumnDef(field);
		if (cm == null) {
			throw new RuntimeException("field not found");
		}
		String fieldName = cm.fieldName();
		BooleanPath path = createBoolean(fieldName);
		// addMetadata(cm, path);
		return path;
	}

	/**
	 * 该方法注册自定义类型有用
	 * 
	 * @return
	 */
	public Map<ColumnMapping, Type<?>> getTypeMap() {
		return typeMap;
	}

	public Class<T> getClz() {
		return clz;
	}

	protected void setClz(Class<T> clz) {
		this.clz = clz;
	}

	/**
	 * 克隆一个新的原始对象.
	 * 
	 * @param <T>
	 * @return
	 */
	public <T extends DataObject> SQLRelationalPath<T> cloneNew() {
		SQLRelationalPath<T> relationalPath = new SQLRelationalPath(clz);
		return relationalPath;
	}

	public <T extends DataObject> SQLRelationalPath<T> cloneNew(String variable) {
		SQLRelationalPath<T> relationalPath = new SQLRelationalPath(clz, variable);
		return relationalPath;
	}
}
