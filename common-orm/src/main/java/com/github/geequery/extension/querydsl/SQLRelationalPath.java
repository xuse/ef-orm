package com.github.geequery.extension.querydsl;

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

import jef.database.DataObject;
import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.dialect.SqlTypeSized;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.PathMetadataFactory;
import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.CollectionPath;
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.ListPath;
import com.querydsl.core.types.dsl.MapPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.SetPath;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.types.dsl.TimePath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;

/**
 * GeeQuery DataObject对应的QueryDSL模型.
 */
public class SQLRelationalPath<T> extends RelationalPathBase<T> implements Cloneable {
	private static final long serialVersionUID = 1L;
	/**
	 * 字段列,值为SimpleExpression
	 */
	private final Map<Field, Expression<?>> map = new HashMap<>();

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
			Expression<?> path = getBeanMappingType(cm.fieldName(), cm.getFieldType(), cm.getSqlType());
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

	protected Expression<?> getBeanMappingType(String columnName, Class<?> type, int sqlType) {
		Class<?> mirror = type;
		// String and char
		if (mirror == String.class) {
			return createString(columnName);
		}
		// Boolean
		if (mirror == Boolean.class || mirror == Boolean.TYPE) {
			return createBoolean(columnName);
		}
		// Byte
		if (mirror == Byte.class || mirror == Byte.TYPE) {
			return createNumber(columnName, Byte.class);
		}
		// Short
		if (mirror == Short.class || mirror == Short.TYPE) {
			return createNumber(columnName, Short.class);
		}
		// Int
		if (mirror == Integer.class || mirror == Integer.TYPE) {
			return createNumber(columnName, Integer.class);
		}
		// Float
		if (mirror == Float.class || mirror == Float.TYPE) {
			return createNumber(columnName, Float.class);
		}
		// Double
		if (mirror == Double.class || mirror == Double.TYPE) {
			return createNumber(columnName, Double.class);
		}
		// Long
		if (mirror == Long.class || mirror == Long.TYPE) {
			return createNumber(columnName, Long.class);
		}
		// BigDecimal
		if (mirror == BigDecimal.class) {
			return createNumber(columnName, BigDecimal.class);
		}
		// BigInteger
		if (mirror == BigInteger.class) {
			return createNumber(columnName, BigInteger.class);
		}

		// Enum
		if (mirror.isEnum()) {
			return createEnum(columnName, (Class) mirror);
		}
		// Char
		if (mirror == CharSequence.class || mirror == Character.TYPE) {
			return createSimple(columnName, Character.class);
		}

		// jdk8日期
		if (mirror == LocalDate.class) {
			return createDate(columnName, LocalDate.class);
		}
		if (mirror == LocalDateTime.class) {
			return createDateTime(columnName, LocalDateTime.class);
		}
		if (mirror == LocalTime.class) {
			return createTime(columnName, LocalTime.class);
		}
		// 日期处理
		if (Date.class.isAssignableFrom(mirror)) {
			if (sqlType != 0) {
				switch (sqlType) {
				case Types.DATE:
					return createDate(columnName, (Class<? super Comparable>) mirror);
				case Types.TIME:
					return createTime(columnName, (Class<? super Comparable>) mirror);
				case Types.TIMESTAMP:
					return createDateTime(columnName, (Class<? super Comparable>) mirror);
				default:
					break;
				}
			}
			return createDateTime(columnName, (Class<? super Comparable>) mirror);
		}

		// byte[]
		if (mirror.isArray()) {
			return createArray(columnName, (Class<? super Object>) mirror);
		}
		// 默认情况
		return createSimple(columnName, (Class<? super Object>) mirror);
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
