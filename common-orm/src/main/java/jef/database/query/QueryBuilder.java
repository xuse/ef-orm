package jef.database.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IConditionField;
import jef.database.IConditionField.And;
import jef.database.IConditionField.Exists;
import jef.database.IConditionField.Not;
import jef.database.IConditionField.NotExists;
import jef.database.IConditionField.Or;
import jef.database.IQueryableEntity;
import jef.database.PojoWrapper;
import jef.database.QueryAlias;
import jef.database.VarObject;
import jef.database.annotation.JoinType;
import jef.database.meta.FBIField;
import jef.database.meta.ITableMetadata;
import jef.database.meta.JoinKey;
import jef.database.meta.JoinPath;
import jef.database.meta.MetaHolder;
import jef.database.meta.TupleMetadata;
import jef.tools.Assert;
import jef.tools.StringUtils;

/**
 * Criteria API的工具类。
 * 
 * JEF提供的查询方式包括 1. 简单对象查询（提供各种单表的、批的、级联的数据库操作）<br/>
 * 2、Criteria API（还可以提供基于多表的、Or/Not、Join的、exists/not
 * exists、Union的、group/having（投影操作）等各种SQL）。<br/>
 * 基本上能涵盖标准SQL语法可以执行的各种操作<br/>
 * 3.NamedQuery/NativeQuery。 （可以实现各种DDL，还有数据库专有语法：如Oracle的分析函数、递归(connect
 * by）等原生的数据库操作<br/>
 * 4、NativeCall. 提供对于存储过程的操作。<br/>
 * 5、其他(如简易接口：selectBySql等，不推荐使用)<br/>
 * 
 * 这个类提供的是各种Criteria API中的操作。<br/>
 * 可以帮助实现：自定义Join、指定选择列等功能<br/>
 * 
 * @author Administrator
 * 
 */
public class QueryBuilder {
	protected QueryBuilder() {
	};

	/**
	 * 创建一个查询请求,当不赋任何条件的时候，这个查询带有一个默认条件为为 AllRecordsCondition.
	 * 
	 * @param clz
	 *            要查询的表
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends IQueryableEntity> Query<T> create(Class<T> clz) {
		ITableMetadata meta = MetaHolder.getMeta(clz);
		QueryImpl<T> query = (QueryImpl<T>) meta.newInstance().getQuery();
		query.allRecords = true;
		return query;
	}

	/**
	 * 创建查询条件
	 * 
	 * @param clz
	 * @return
	 */
	public static <T extends IQueryableEntity> Terms terms(Class<T> clz) {
		return create(clz).terms();
	}

	/**
	 * 针对动态表模板，创建指定KEY的查询
	 * 
	 * @param clz
	 * @param key
	 * @return
	 */
	public static <T extends IQueryableEntity> Query<T> create(Class<T> clz, String key) {
		T d;
		try {
			d = clz.newInstance();
			QueryImpl<T> query = new QueryImpl<T>(d, key);
			query.allRecords = true;
			return query;
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 创建一个非NAtive的Query.
	 * 
	 * @param clz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Query<PojoWrapper> createForPOJO(Class<?> clz) {
		ITableMetadata meta = MetaHolder.getMeta(clz);
		return (Query<PojoWrapper>) create(meta);
	}

	/**
	 * 创建一个查询请求,当不赋任何条件的时候，这个查询带有一个默认条件为为 AllRecordsCondition.
	 * 
	 * @param clz
	 *            要查询的表的元模型 {@linkplain ITableMetadata 什么是元模型}
	 * @return
	 */
	public static Query<?> create(ITableMetadata meta) {
		IQueryableEntity d = meta.newInstance();
		Query<?> query = d.getQuery();
		query.setAllRecordsCondition();
		return query;
	}

	/**
	 * 创建一个查询请求,当不赋任何条件的时候，这个查询带有一个默认条件为为 AllRecordsCondition.
	 * 
	 * @param clz
	 *            要查询的表(动态表)的元模型，{@linkplain ITableMetadata 什么是元模型}
	 * @return
	 */
	public static Query<VarObject> create(TupleMetadata meta) {
		IQueryableEntity d = meta.newInstance();
		@SuppressWarnings("unchecked")
		Query<VarObject> query = d.getQuery();
		query.setAllRecordsCondition();
		return query;

	}

	/**
	 * 将传入的SQLExpression作为条件
	 * 
	 * @param sql
	 * @return
	 */
	public static Condition sqlExpression(String sql) {
		return Condition.get(new SqlExpression(sql), Operator.EQUALS, null);
	}

	/**
	 * 创建一个Exists条件
	 * 
	 * @param subQuery
	 * @param joinCondition
	 * @return
	 */
	public static Condition exists(Query<?> subQuery, Condition... joinCondition) {
		Exists ex = new Exists(subQuery);
		for (Condition c : joinCondition) {
			subQuery.addCondition(c);
		}
		return Condition.get(ex, Operator.EQUALS, null);
	}

	/**
	 * 创建一个Not Exists条件
	 * 
	 * @param subQuery
	 * @param joinCondition
	 * @return
	 */
	public static Condition notExists(Query<?> subQuery, Condition... joinCondition) {
		NotExists ex = new NotExists(subQuery);
		for (Condition c : joinCondition) {
			subQuery.addCondition(c);
		}
		return Condition.get(ex, Operator.EQUALS, null);
	}

	/**
	 * 产生Or条件
	 * 
	 * @param c
	 *            多个条件
	 * @return 将传入的多个条件用or连接，形成一个新的条件
	 */
	public static Condition or(Condition... c) {
		IConditionField or = new Or(c);
		return Condition.get(or, Operator.EQUALS, null);
	}

	/**
	 * 产生And条件
	 * 
	 * @param c
	 *            多个条件
	 * @return 将传入的多个条件用and连接，形成一个新的条件
	 */
	public static Condition and(Condition... c) {
		IConditionField or = new And(c);
		return Condition.get(or, Operator.EQUALS, null);
	}

	/**
	 * 产生Not条件
	 * 
	 * @param c
	 *            传入一个条件
	 * @return 返回c条件相反的条件
	 */
	public static Condition not(Condition c) {
		IConditionField or = new Not(c);
		return Condition.get(or, Operator.EQUALS, null);
	}

	/**
	 * 产生相等条件 (=)
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field = value} 这样的条件
	 */
	public static Condition eq(Field field, Object value) {
		return Condition.get(field, Operator.EQUALS, value);
	}

	/**
	 * 产生不等条件 (<> 或 !=)
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field != value} 这样的条件
	 */
	public static Condition ne(Field field, Object value) {
		return Condition.get(field, Operator.NOT_EQUALS, value);
	}

	/**
	 * 产生大于条件 ( >)
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field > value} 这样的条件
	 */
	public static Condition gt(Field field, Object value) {
		return Condition.get(field, Operator.GREAT, value);
	}

	/**
	 * 产生大于等于条件 （ >= ）
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field >= value} 这样的条件
	 */
	public static Condition ge(Field field, Object value) {
		return Condition.get(field, Operator.GREAT_EQUALS, value);
	}

	/**
	 * 产生小于条件 （<）
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field < value} 这样的条件
	 */
	public static Condition lt(Field field, Object value) {
		return Condition.get(field, Operator.LESS, value);
	}

	/**
	 * 产生小于等于条件 （ <= )
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field <= value} 这样的条件
	 */
	public static Condition le(Field field, Object value) {
		return Condition.get(field, Operator.LESS_EQUALS, value);
	}

	/**
	 * 产生IN条件 (in)
	 * 
	 * @param fieldqq		q											
	 *            表的字段（也可以是函数表达式）
	 * @param value
	 *            条件的值 数组
	 * @return 表达式为 {@code field in (value,...)} 这样的条件
	 * @deprecated 请使用 {@link #in(Field, Comparable[])}
	 */
	public static Condition in(Field field, Object[] values) {
		return Condition.get(field, Operator.IN, values);																												
	}

	/**
	 * 产生IN条件 (in)
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param value
	 *            条件的值 数组
	 * @return 表达式为 {@code field in (value,...)} 这样的条件
	 */
	public static Condition in(Field field, Comparable<?>[] values) {
		return Condition.get(field, Operator.IN, values);
	}
	
	/**
	 * 产生IN条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param value
	 *            条件的值 数组
	 * @return 表达式为 {@code field in (value,...)} 这样的条件
	 */
	public static Condition in(Field field, int[] values) {
		return Condition.get(field, Operator.IN, values);
	}

	/**
	 * 产生IN条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param value
	 *            条件的值 数组
	 * @return @return 表达式为 {@code field in (value,...)} 这样的条件
	 */
	public static Condition in(Field field, long[] values) {
		return Condition.get(field, Operator.IN, values);
	}

	/**
	 * 产生IN条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param value
	 *            条件的值 集合
	 * @return @return 表达式为 {@code field in (value,...)} 这样的条件
	 */
	public static Condition in(Field field, Collection<?> values) {
		return Condition.get(field, Operator.IN, values);
	}

	/**
	 * 产生is null条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @return 表达式为 {@code field is null} 这样的条件
	 */
	public static Condition isNull(Field field) {
		return Condition.get(field, Operator.IS_NULL, null);
	}

	/**
	 * 产生 is not null条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @return 表达式为 {@code field is not null} 这样的条件
	 */
	public static Condition notNull(Field field) {
		return Condition.get(field, Operator.IS_NOT_NULL, null);
	}

	/**
	 * 产生not in条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param values
	 *            not in的条件值
	 * @return 表达式为 {@code field is not in (value,...)} 这样的条件
	 */
	public static Condition notin(Field field, Object[] values) {
		return Condition.get(field, Operator.NOT_IN, values);
	}

	/**
	 * 产生between条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param begin
	 *            比较值的下限
	 * @param end
	 *            比较值的上限
	 * @return 产生形如 {@code field between begin and end} 这样的条件
	 */
	public static <T extends Comparable<T>> Condition between(Field field, T begin, T end) {
		return Condition.get(field, Operator.BETWEEN_L_L, new Object[] { begin, end });
	}

	/**
	 * 产生MatchEnd条件，
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like '%str' } 这样的条件，str中原来的的'%' '_'符号会被转义
	 */
	public static Condition matchEnd(Field field, String str) {
		return Condition.get(field, Operator.MATCH_END, str);
	}

	/**
	 * 产生MatchStart条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like 'str%' } 这样的条件，str中原来的的'%' '_'符号会被转义
	 */
	public static Condition matchStart(Field field, String str) {
		return Condition.get(field, Operator.MATCH_START, str);
	}

	/**
	 * 得到一个Like %str%的条件
	 * 
	 * @param field
	 *            field 表的字段（也可以是函数表达式）
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like '%str%' } 这样的条件，str中原来的的'%' '_'符号会被转义
	 */
	public static Condition matchAny(Field field, String str) {
		return Condition.get(field, Operator.MATCH_ANY, str);
	}

	/**
	 * 得到一个Like条件，参数为自定义的字符串。 例如
	 * <p>
	 * {@code like(field, "%123_456%")}
	 * <p>
	 * 相当于
	 * <p>
	 * {@code WHERE field LIKE '%123_456%'  }
	 * <p>
	 * 
	 * <h3>注意</h3> 这个方法可以自由定义复杂的匹配模板外，但是和matchxxx系列的方法相比，不会对字符串中的'%'
	 * '_'做转义。因此实际使用不当会有SQL注入风险。
	 * <p>
	 * 
	 * @param field
	 *            field 表的字段（也可以是函数表达式）
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like 'str' } 这样的条件，str中原来的的'%' '_'符号会被保留
	 */
	public static Condition like(Field field, String str) {
		return Condition.get(field, Operator.MATCH_ANY, new SqlExpression(str));
	}

	/**
	 * 将多个普通查询拼为union查询, 要求每个查询返回的字段个数和字段类型必须一致
	 * 
	 * @param queries
	 *            多个查询
	 * @return UnionQuery对象。
	 */
	public static <T> UnionQuery<T> union(TypedQuery<T>... queries) {
		Assert.isTrue(queries.length > 0);
		UnionQuery<T> q = new UnionQuery<T>(Arrays.<ConditionQuery> asList(queries), queries[0].getMeta());
		q.setAll(false);
		return q;
	}

	/**
	 * 将多个普通查询拼为union查询。 要求每个查询返回的字段个数和字段类型必须一致
	 * 
	 * @param clz
	 *            UNION返回结果的类型
	 * @param queries
	 *            queries 多个查询
	 * @return UnionQuery对象。
	 */
	public static <T> UnionQuery<T> union(Class<T> clz, ConditionQuery... queries) {
		Assert.isTrue(queries.length > 0);
		UnionQuery<T> q = new UnionQuery<T>(Arrays.<ConditionQuery> asList(queries), clz);
		q.setAll(false);
		return q;
	}

	/**
	 * 将多个普通查询拼为union查询
	 * 
	 * @param queries
	 *            构成union的多个查询
	 * @return UnionQuery对象。
	 */
	public static <T> UnionQuery<T> union(ITableMetadata clz, ConditionQuery... queries) {
		UnionQuery<T> q = new UnionQuery<T>(Arrays.asList(queries), clz);
		q.setAll(false);
		return q;
	}

	/**
	 * 将多个普通查询拼为union all查询, 要求每个查询返回的字段个数和字段类型必须一致
	 * 
	 * @param queries
	 *            多个查询
	 * @return UnionQuery对象。
	 */
	public static <T> UnionQuery<T> unionAll(TypedQuery<T>... queries) {
		Assert.isTrue(queries.length > 0);
		UnionQuery<T> q = new UnionQuery<T>(Arrays.<ConditionQuery> asList(queries), queries[0].getMeta());
		return q;
	}

	/**
	 * 将多个普通查询拼为union all查询 并指定返回类型
	 * 
	 * @param clz
	 *            union all查询要返回的类型
	 * @param queries
	 *            构成union all的多个查询
	 * @return UnionQuery对象。
	 */
	public static <T> UnionQuery<T> unionAll(Class<T> clz, ConditionQuery... queries) {
		UnionQuery<T> q = new UnionQuery<T>(Arrays.asList(queries), clz);
		return q;
	}

	/**
	 * 将多个普通查询拼为union all查询
	 * 
	 * @param clz
	 *            union all查询要返回的类型
	 * @param queries
	 *            构成union all的多个查询
	 * @return UnionQuery对象。
	 */
	public static <T> UnionQuery<T> unionAll(ITableMetadata clz, ConditionQuery... queries) {
		UnionQuery<T> q = new UnionQuery<T>(Arrays.asList(queries), clz);
		return q;
	}

	/**
	 * 创建内连接
	 * 
	 * @param left
	 *            连接左边的查询。可以是{@link Query}或者{@link Join}对象
	 * @param right
	 *            右边的查询
	 * @param keys
	 *            连接路径，可省略。一般在编程中QB.on()方法来构造
	 * @return Join查询对象
	 * @see #on(Field, Field)
	 * @see #on(Field, Number)
	 * @see #on(Field, String)
	 * @see #on(Field, Operator, Object)
	 * @see #on(Query, Field, Query, Field)
	 */
	public static Join innerJoin(JoinElement left, Query<?> right, JoinKey... keys) {
		JoinPath path = null;
		if (keys.length > 0) {
			path = new JoinPath(JoinType.INNER, keys);
		}
		Join join = JoinUtil.create(left, right, path, JoinType.INNER, false);
		if (join == null)
			join = JoinUtil.create(left, right, path, JoinType.INNER, true);
		Assert.notNull(join, "Join create failure on !");
		return join;
	}

	/**
	 * 创建内连接
	 * 
	 * @param left
	 *            连接左边的查询。可以是{@link Query}或者{@link Join}对象
	 * @param right
	 *            右边的查询
	 * @param keys
	 *            连接路径，可省略。一般在编程中QB.on()方法来构造
	 * @return Join查询对象
	 * @see #on(Field, Field)
	 * @see #on(Field, Number)
	 * @see #on(Field, String)
	 * @see #on(Field, Operator, Object)
	 * @see #on(Query, Field, Query, Field)
	 * @deprecated see https://github.com/GeeQuery/ef-orm/issues/46
	 */
	public static Join innerJoinWithRef(Query<?> left, Query<?> right, JoinKey... keys) {
		JoinElement jl = DbUtils.toReferenceJoinQuery(left, null);
		return innerJoin(jl, right, keys);
	}

	/**
	 * 创建左外连接
	 * 
	 * @param left
	 *            连接左边的查询。可以是{@link Query}或者{@link Join}对象
	 * @param right
	 *            右边的查询
	 * @param keys
	 *            连接路径，可省略。一般在编程中QB.on()方法来构造
	 * @return Join查询对象
	 * @see #on(Field, Field)
	 * @see #on(Field, Number)
	 * @see #on(Field, String)
	 * @see #on(Field, Operator, Object)
	 * @see #on(Query, Field, Query, Field)
	 */
	public static Join leftJoin(JoinElement left, Query<?> right, JoinKey... keys) {
		JoinPath path = null;
		if (keys.length > 0) {
			path = new JoinPath(JoinType.LEFT, keys);
		}
		Join join = JoinUtil.create(left, right, path, JoinType.LEFT, false);
		if (join == null)
			join = JoinUtil.create(left, right, path, JoinType.LEFT, true);
		Assert.notNull(join, "Join create failure on !");
		return join;
	}

	/**
	 * 创建右外连接
	 * 
	 * @param left
	 *            连接左边的查询。可以是{@link Query}或者{@link Join}对象
	 * @param right
	 *            右边的查询
	 * @param keys
	 *            连接路径，可省略。一般在编程中QB.on()方法来构造
	 * @return Join查询对象
	 * @see #on(Field, Field)
	 * @see #on(Field, Number)
	 * @see #on(Field, String)
	 * @see #on(Field, Operator, Object)
	 * @see #on(Query, Field, Query, Field)
	 */
	public static Join rightJoin(JoinElement left, Query<?> right, JoinKey... keys) {
		JoinPath path = null;
		if (keys.length > 0) {
			path = new JoinPath(JoinType.RIGHT, keys);
		}
		Join join = JoinUtil.create(left, right, path, JoinType.RIGHT, false);
		if (join == null)
			join = JoinUtil.create(left, right, path, JoinType.RIGHT, true);
		Assert.notNull(join, "Join create failure on !");
		return join;
	}

	/**
	 * 创建全外连接
	 * 
	 * @param left
	 *            连接左边的查询。可以是{@link Query}或者{@link Join}对象
	 * @param right
	 *            右边的查询
	 * @param keys
	 *            连接路径，可省略。一般在编程中QB.on()方法来构造
	 * @return Join查询对象
	 * @see #on(Field, Field)
	 * @see #on(Field, Number)
	 * @see #on(Field, String)
	 * @see #on(Field, Operator, Object)
	 * @see #on(Query, Field, Query, Field)
	 */
	public static Join outerJoin(JoinElement left, Query<?> right, JoinKey... keys) {
		JoinPath path = null;
		if (keys.length > 0) {
			path = new JoinPath(JoinType.RIGHT, keys);
		}
		Join join = JoinUtil.create(left, right, path, JoinType.RIGHT, false);
		if (join == null)
			join = JoinUtil.create(left, right, path, JoinType.RIGHT, true);
		Assert.notNull(join, "Join create failure on !");
		return join;
	}

	/**
	 * 保留left查询中一对一关系或者多对一关系产生的外连接查询，然后再关联右侧查询对象(左外连接)。 <h3>使用举例</h3> TODO
	 * documented.
	 * 
	 * @param left
	 *            连接左边的查询。是{@link Query}对象
	 * @param right
	 *            右边的查询
	 * @param keys
	 *            连接路径，可省略。一般在编程中QB.on()方法来构造
	 * @return Join查询对象
	 * @deprecated see https://github.com/GeeQuery/ef-orm/issues/46
	 */
	public static Join leftJoinWithRef(Query<?> left, Query<?> right, JoinKey... keys) {
		JoinElement jl = DbUtils.toReferenceJoinQuery(left, null);
		return leftJoin(jl, right, keys);
	}

	/**
	 * 保留left查询中一对一关系或者多对一关系产生的外连接查询，然后再关联右侧查询对象（右外连接）。 <h3>使用举例</h3> TODO
	 * documented.
	 * 
	 * @param left
	 *            连接左边的查询。是{@link Query}对象
	 * @param right
	 *            右边的查询
	 * @param keys
	 *            连接路径。一般在编程中QB.on()方法来构造
	 * @return Join查询对象
	 * @deprecated see https://github.com/GeeQuery/ef-orm/issues/46
	 */
	public static Join rightJoinWithRef(Query<?> left, Query<?> right, JoinKey... keys) {
		JoinElement jl = DbUtils.toReferenceJoinQuery(left, null);
		return rightJoin(jl, right, keys);
	}

	/**
	 * 保留left查询中一对一关系或者多对一关系产生的外连接查询，然后再关联右侧查询对象（全外连接）。 <h3>使用举例</h3> TODO
	 * documented.
	 * 
	 * @param left
	 *            连接左边的查询。是{@link Query}对象
	 * @param right
	 *            右边的查询
	 * @param keys
	 *            连接路径。一般在编程中QB.on()方法来构造
	 * @return Join查询对象
	 * @deprecated see https://github.com/GeeQuery/ef-orm/issues/46
	 */
	public static Join outerJoinWithRef(Query<?> left, Query<?> right, JoinKey... keys) {
		JoinElement jl = DbUtils.toReferenceJoinQuery(left, null);
		return outerJoin(jl, right, keys);
	}

	/**
	 * 创建一个列选择操作器（SelectItems），SelectItems提供了若干方法用于指定select语句中的选择列，包括distinct,
	 * group by, hvaing等语法支持。 使用此API可以构造出投影操作，如max() min() sum()等分组函数。
	 * 
	 * @param query
	 *            查询
	 * @return 列选择操作器
	 * @see Selects
	 */
	public static Selects selectFrom(JoinElement query) {
		// if(query instanceof Query<?>){//如果是非连接，检查是否有自动的
		// query=DbUtils.toReferenceJoinQuery((Query<?>)query);
		// }
		if (query.getSelectItems() != null && (query.getSelectItems() instanceof Selects))
			return (Selects) query.getSelectItems();
		List<QueryAlias> qs;
		if (query instanceof Join) {
			qs = query.prepare().queries;
		} else {
			Query<?> q = (Query<?>) query;
			QueryAlias qa = new QueryAlias("t", q);
			qs = Arrays.asList(qa);
		}
		SelectsImpl select = new SelectsImpl(qs);
		query.setSelectItems(select);
		return select;
	}

	/**
	 * 清除自定义的选择列，恢复到默认状态
	 * 
	 * @param query
	 *            请求
	 */
	public static void clearCustomSelection(JoinElement query) {
		query.setSelectItems(null);
	}

	/**
	 * 创建一个连接键 <h3>使用举例</h3>
	 * 
	 * <pre>
	 * <code>
	 *  Query<Department> q1=QB.create(Department.class); //产生一个查询(部门表)
	 *  Query<Person> q2=QB.create(Person.class); //产生另一个查询(人员表)
	 *  Join join = QB.innerJoin(q1, q2, QB.on(Department.Field.id, Person.Field.deptId)); //人员表关联部门表，按人员表中的deptId字段关联部门表的id字段。
	 *  session.select()
	 *  </code>
	 * </pre>
	 * 
	 * @param left
	 *            左边的字段
	 * @param right
	 *            右边的字段
	 * @return 连接键
	 * @see #innerJoin(JoinElement, Query, JoinKey...)
	 * @see #leftJoin(JoinElement, Query, JoinKey...)
	 * @see #rightJoin(JoinElement, Query, JoinKey...)
	 * @see #outerJoin(JoinElement, Query, JoinKey...)
	 */
	public static JoinKey on(Field left, Field right) {
		return new JoinKey(left, right);
	}

	/**
	 * 创建一个连接键 <h3>使用举例</h3>
	 * 
	 * @param left
	 *            左边的字段
	 * @param right
	 *            等于的值
	 * @return 生成一个形如 {@code on field = value}这样的SQL字句。
	 * @see #innerJoin(JoinElement, Query, JoinKey...)
	 * @see #leftJoin(JoinElement, Query, JoinKey...)
	 * @see #rightJoin(JoinElement, Query, JoinKey...)
	 * @see #outerJoin(JoinElement, Query, JoinKey...)
	 */
	public static JoinKey on(Field left, Number value) {
		return new JoinKey(left, new FBIField(String.valueOf(value)));
	}

	/**
	 * 创建一个连接键(用于复杂的连接描述，右侧可以是各种常量)
	 * 
	 * @param field
	 * @param oper
	 * @param value
	 * @return 连接键
	 */
	public static JoinKey on(Field field, Operator oper, Object value) {
		return new JoinKey(field, oper, value);
	}

	/**
	 * 创建连接键描述
	 * 
	 * @param left
	 *            左边的字段
	 * @param value
	 *            常量string，即要求等于右侧的常量值
	 * @return
	 */
	public static JoinKey on(Field left, String value) {
		value = value.replace("'", "''");
		String expression = StringUtils.concat("'", value, "'");
		return new JoinKey(left, new FBIField(expression));
	}

	/**
	 * 指定所属Query的方式来使用，当同一张表字表关联的时候需要使用。
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	public static JoinKey on(Query<?> t1, Field left, Query<?> t2, Field right) {
		return new JoinKey(new RefField(t1, left), new RefField(t2, right));
	}

	/**
	 * 描述一个特定Query对象中的Field，但是此时尚不指定该Query对象，要到查询指定时，自动匹配对应的Query对象。
	 * 此时，要求该Field所使用的表在Join查询中只出现一次。
	 * 
	 * @param field
	 *            field
	 * @return
	 */
	public static Field fieldOf(Field field) {
		return new RefField(field);
	}

	/**
	 * 描述一个特定Query对象中的Field
	 * 
	 * @param q
	 *            特定Query对象
	 * @param field
	 *            field
	 * @return
	 */
	public static Field fieldOf(Query<?> q, Field field) {
		return new RefField(q, field);
	}

	/**
	 * 描述一个特定Query对象中的Field
	 * 
	 * @param q
	 *            特定Query对象
	 * @param field
	 *            field名称
	 * @return
	 */
	public static Field fieldOf(Query<?> q, String field) {
		return new RefField(q, field);
	}

	/**
	 * 用于在对象的更新字段表中产生一个自增（或自减）的更新描述 <h3>示例</h3>
	 * {@code QB.fieldAdd(foo,Foo.Field.age, 100);}
	 * <p>
	 * 将会形成如下的SQL语句<br />
	 * <code>
	 * update foo set age = age + ? （实际执行时为 age = age + 100）
	 * </code>
	 * 
	 * @param entity
	 *            要更新的对象
	 * @param field
	 *            需要自增的字段
	 * @param i
	 *            自增值
	 */
	public static void fieldAdd(IQueryableEntity entity, Field field, int i) {
		entity.prepareUpdate(field, new JpqlExpression(field.name() + " + :amount_"));
		entity.getQuery().setAttribute("amount_", i);
	}

	/**
	 * 用于在对象的更新字段表中产生一个自增（或自减）的更新描述 <h3>示例</h3>
	 * {@code QB.fieldAdd(foo,Foo.Field.age, 100);}
	 * <p>
	 * 将会形成如下的SQL语句<br />
	 * <code>
	 * update foo set age = age + ? （实际执行时为 age = age + 100）
	 * </code>
	 * 
	 * @param entity
	 *            要更新的对象
	 * @param field
	 *            需要自增的字段
	 * @param i
	 *            自增值
	 */
	public static void fieldAdd(IQueryableEntity entity, Field field, double i) {
		entity.prepareUpdate(field, new JpqlExpression(field.name() + " + :amount_"));
		entity.getQuery().setAttribute("amount_", i);
	}
}
