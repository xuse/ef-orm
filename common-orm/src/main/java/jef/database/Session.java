/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database;

import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Provider;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.common.wrapper.Page;
import jef.database.Condition.Operator;
import jef.database.Transaction.TransactionFlag;
import jef.database.cache.Cache;
import jef.database.cache.CacheImpl;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.dialect.type.VersionSupportColumn;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.database.innerpool.MetadataService;
import jef.database.innerpool.PartitionSupport;
import jef.database.jdbc.result.IResultSet;
import jef.database.jdbc.result.ResultSetContainer;
import jef.database.jdbc.result.ResultSetWrapper;
import jef.database.jdbc.result.ResultSets;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.parser.StSqlParser;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.AbstractRefField;
import jef.database.meta.EntityType;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.Reference;
import jef.database.query.AllTableColumns;
import jef.database.query.ConditionQuery;
import jef.database.query.EntityMappingProvider;
import jef.database.query.Join;
import jef.database.query.JoinElement;
import jef.database.query.PKQuery;
import jef.database.query.Query;
import jef.database.query.Selects;
import jef.database.query.SqlContext;
import jef.database.query.SqlExpression;
import jef.database.query.TypedQuery;
import jef.database.routing.PartitionResult;
import jef.database.support.DbOperatorListener;
import jef.database.support.MultipleDatabaseOperateException;
import jef.database.support.RDBMS;
import jef.database.wrapper.ResultIterator;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.CountClause;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.clause.SqlBuilder;
import jef.database.wrapper.clause.UpdateClause;
import jef.database.wrapper.populator.AbstractResultSetTransformer;
import jef.database.wrapper.populator.ResultPopulatorImpl;
import jef.database.wrapper.populator.Transformer;
import jef.script.javascript.Var;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.JefConfiguration;
import jef.tools.PageLimit;
import jef.tools.StringUtils;

import org.easyframe.enterprise.spring.TransactionMode;

import com.querydsl.sql.SQLQueryFactory;

/**
 * 描述一个事务(会话)的数据库操作句柄，提供了各种操作数据库的方法供用户使用。
 * <p>
 * 大部分数据库操作方法都被封装在这个类上。这个类也是EF-ORM中用户使用最多的一个类。 该类有两类实现
 * <ul>
 * <li>Transaction 事务状态下的数据库封装，可以回滚和提交，设置SavePoint。</li>
 * <li>DbClient 非事务状态下的数据库连接，每次操作后自动提交不能回滚。但可以执行建表、删表等DDL语句。</li>
 * </ul>
 * 这个类是线程安全的。
 * 
 * @author Jiyi
 * @see DbClient
 * @see Transaction
 * @see #getSqlTemplate(String)
 */
public abstract class Session {
	// 这六个值在初始化的时候赋值
	protected SqlProcessor preProcessor;
	protected InsertProcessor insertp;
	protected UpdateProcessor updatep;
	protected DeleteProcessor deletep;
	protected SelectProcessor selectp;

	/**
	 * 获取数据库方言<br>
	 * 当有多数据源的时候，这个方法总是返回默认数据源的方言。
	 * 
	 * @return 方言
	 * @deprecated 为更好支持异构多数据库，尽量使用{@link #getProfile(String)}
	 */
	public DatabaseDialect getProfile() {
		return getProfile(null);
	}

	/**
	 * 获取指定数据库的方言，支持从不同数据源中获取<br/>
	 * 
	 * @param datasourceName
	 *            数据源名称，如果单数据源的场合，可以传入null
	 * @return 指定数据源的方言
	 */
	public abstract DatabaseDialect getProfile(String datasourceName);

	/*
	 * 内部使用
	 */
	abstract IUserManagedPool getPool();

	/*
	 * 内部使用 得到数据库连接
	 */
	abstract IConnection getConnection() throws SQLException;

	/*
	 * 内部使用 释放（当前线程）连接
	 */
	abstract void releaseConnection(IConnection conn);

	/*
	 * 内部使用 得到数据库名
	 */
	abstract protected String getDbName(String dbKey);

	/*
	 * 内部使用 得到缓存
	 */
	abstract protected Cache getCache();

	/*
	 * 得到数据库操作监听器（观测者模式的回调对象）
	 */
	abstract protected DbOperatorListener getListener();

	/*
	 * 当前数据库操作所在的事务，用于记录日志以跟踪SQL查询所在事务
	 */
	abstract protected String getTransactionId(String dbKey);//

	/*
	 * 返回目前已知的所有可连接的数据源名称
	 * 
	 * @return 多个数据源的名称
	 */
	abstract protected Collection<String> getAllDatasourceNames();

	/*
	 * 获取当前数据库的事务管理模式
	 */
	protected abstract TransactionMode getTxType();

	/*
	 * 当前操作是否位于一个JPA事务中。
	 * 
	 * @return true is current is in a JPA transaction.
	 */
	abstract boolean isJpaTx();

	/*
	 * 当前是否处理多数据库路由场景中
	 */
	abstract boolean isRoutingDataSource();

	/**
	 * 关闭数据库事务。<br>
	 * <ul>
	 * <li>在DbClient上调用此方法，无任何影响。</li>
	 * <li>在Transaction上调用此方法，将会关闭事务，未被提交的修改将会回滚。</li>
	 * </ul>
	 */
	public abstract void close();

	/**
	 * 清理一级缓存
	 * 
	 * @param entity
	 *            要清理的数据或查询
	 */
	public final void evict(IQueryableEntity entity) {
		getCache().evict(entity);
	}

	/**
	 * 清空全部的一级缓存
	 */
	public final void evictAll() {
		getCache().evictAll();
	}

	/**
	 * 判断Session是否有效
	 * 
	 * @return true if the session is open.
	 */
	public abstract boolean isOpen();

	/**
	 * 创建命名查询
	 * 
	 * <h3>什么是命名查询</h3>
	 * 事先将E-SQL编写在配置文件或者数据库中，运行时加载并解析，使用时按名称进行调用。这类SQL查询被称为NamedQuery。对应JPA规范当中的
	 * “命名查询”。
	 * 
	 * <h3>使用示例</h3>
	 * 
	 * <pre>
	 * <code>NativeQuery&lt;ResultWrapper&gt; query = db.createNamedQuery("unionQuery-1", ResultWrapper.class);
	 *List<ResultWrapper> result = query.getResultList();
	 *
	 *配置在named-queries.xml中的SQL语句
	 *&lt;query name="unionQuery-1"&gt;
	 *&lt;![CDATA[
	 *select * from(
	 *(select upper(t1.person_name) AS name, T1.gender, '1' AS GRADE, T2.NAME AS SCHOOLNAME
	 *	from T_PERSON T1
	 *	inner join SCHOOL T2 ON T1.CURRENT_SCHOOL_ID=T2.ID)	 
	 *  union 
	 *(select t.NAME,t.GENDER,t.GRADE,'Unknown' AS SCHOOLNAME from STUDENT t)) a
	 *]]&gt;
	 *&lt;/query&gt;</code>
	 * 
	 * <pre>
	 * 即使用本方法返回的NativeQuery对象上，可以执行和该SQL语句相关的各种操作。
	 * 
	 * @param name
	 *            数据库中或者文件中配置的命名查询的名称
	 * @param resultWrapper
	 *            想要的查询结果包装类型
	 * @return 查询对象(NativeQuery)
	 * @see NativeQuery
	 */
	abstract public <T> NativeQuery<T> createNamedQuery(String name, Class<T> resultWrapper);

	/**
	 * {@linkplain #createNamedQuery(String, Class) 什么是命名查询}
	 * 
	 * @param name
	 *            数据库中或者文件中配置的命名查询的名称
	 * @param resultMeta
	 *            想要的查询结果包装类型
	 * @return 查询对象(NativeQuery)
	 * @see NativeQuery
	 */
	abstract public <T> NativeQuery<T> createNamedQuery(String name, ITableMetadata resultMeta);

	/**
	 * 创建命名查询，不指定其返回类型，一般用于executeUpdate()的场合<br>
	 * {@linkplain #createNamedQuery(String, Class) 什么是命名查询}
	 * 
	 * @param name
	 *            命名查询的名称
	 * @return 查询对象(NativeQuery)
	 * @see NativeQuery
	 */
	public final <T> NativeQuery<T> createNamedQuery(String name) {
		return createNamedQuery(name, (Class<T>) null);
	}

	/**
	 * 返回SQL操作句柄，可以在该对象上使用SQL语句和JPQL语句操作数据库<br/>
	 * 
	 * <tt>特点：支持不同的数据源<tt>
	 * 
	 * @param datasourceName
	 *            数据源名称，如果单数据源的场合，可以传入null
	 * @return SQL语句操作句柄
	 * @see SqlTemplate
	 */
	public final SqlTemplate getSqlTemplate(String datasourceName) {
		return selectTarget(datasourceName);
	}

	protected abstract OperateTarget selectTarget(String dbKey);

	/**
	 * 创建SQL查询（支持绑定变量）
	 * 
	 * @param sqlString
	 *            SQL语句
	 * @return 查询对象(NativeQuery)
	 * @see NativeQuery
	 */
	public NativeQuery<?> createNativeQuery(String sqlString) {
		return selectTarget(null).createNativeQuery(sqlString, (Class<?>) null);
	}

	/**
	 * 创建SQL查询（支持绑定变量）
	 * 
	 * @param sqlString
	 *            SQL语句
	 * @param resultClass
	 *            返回结果类型
	 * @return 查询对象(NativeQuery)
	 * @see NativeQuery
	 */
	public <T> NativeQuery<T> createNativeQuery(String sqlString, Class<T> resultClass) {
		return selectTarget(null).createNativeQuery(sqlString, resultClass);
	}

	/**
	 * 创建SQL查询（支持绑定变量）
	 * 
	 * @param sqlString
	 *            SQL语句
	 * @param resultMeta
	 *            返回结果类型(元模型)
	 * @return 查询对象(NativeQuery)
	 * @see NativeQuery
	 */
	public <T> NativeQuery<T> createNativeQuery(String sqlString, ITableMetadata resultMeta) {
		return selectTarget(null).createNativeQuery(sqlString, resultMeta);
	}

	/**
	 * 创建一个对存储过程、函数的调用对象，允许带返回对象
	 * 
	 * @param procedureName
	 *            存储过程名称
	 * @param paramClass
	 *            参数的类型，用法如下：
	 *            <ul>
	 *            <li>凡是入参，直接传入类型，如String.class， Long.class</li>
	 *            <li>出参，单个的写作OutParam.typeOf(type)，例如OutParam.typeOf(Integer.
	 *            class)</li>
	 *            <li>出参，以游标形式返回多个的写作OutParam.listOf(type)，例如OutParam.listOf(
	 *            Entity .class)</li>
	 *            </ul>
	 * @return 调用对象NativeCall
	 * @throws SQLException
	 * @see NativeCall
	 */
	public NativeCall createNativeCall(String procedureName, Type... paramClass) throws SQLException {
		return selectTarget(null).createNativeCall(procedureName, paramClass);
	}

	/**
	 * 创建匿名过程(匿名块)调用对象
	 * 
	 * @param callString
	 *            SQL语句
	 * @param paramClass
	 *            参数的类型，用法如下
	 *            <ul>
	 *            <li>凡是入参，直接传入类型，如String.class， Long.class</li>
	 *            <li>出参，单个的写作OutParam.typeOf(type)，例如OutParam.typeOf(Integer.
	 *            class)</li>
	 *            <li>出参，以游标形式返回多个的写作OutParam.listOf(type)，例如OutParam.listOf(
	 *            Entity .class)</li>
	 *            </ul>
	 * @return 调用对象NativeCall
	 * @throws SQLException
	 * @see NativeCall
	 */
	public NativeCall createAnonymousNativeCall(String callString, Type... paramClass) throws SQLException {
		return selectTarget(null).createAnonymousNativeCall(callString, paramClass);
	}

	/**
	 * 创建JPQL的NativeQuery查询
	 * 
	 * @param jpql
	 *            JPQL语句
	 * @return 查询对象NativeQuery
	 * @see NativeQuery
	 */
	public NativeQuery<?> createQuery(String jpql) {
		try {
			return selectTarget(null).createQuery(jpql, null);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * 创建JPQL的NativeQuery查询
	 * 
	 * @param jpql
	 *            JPQL语句
	 * @param resultClass
	 *            返回数据类型
	 * @return 查询对象NativeQuery
	 * @see NativeQuery
	 */
	public <T> NativeQuery<T> createQuery(String jpql, Class<T> resultClass) {
		try {
			return selectTarget(null).createQuery(jpql, resultClass);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * 更新数据（带级联）<br>
	 * 如果和其他表具有关联的关系，那么插入时会自动维护其他表中的数据，这些操作包括了Delete操作（删除子表的部分数据）
	 * 
	 * @param obj
	 *            被更新的对象
	 * @return 更新的记录数 (仅主表，级联修改的记录行数未算在内。)
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see {@link #update(IQueryableEntity)}
	 */
	public int updateCascade(Object entity) throws SQLException {
		if (entity == null)
			return 0;
		IQueryableEntity obj;
		if (entity instanceof IQueryableEntity) {
			obj = (IQueryableEntity) entity;
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity);
			obj = meta.transfer(entity, true);
		}
		return updateCascade0(obj, 0);
	}

	/**
	 * 删除数据（带级联） <br>
	 * 如果和其他表具有关联的关系，那么插入时会自动维护其他表中的数据，这些操作包括了Delete操作
	 * 
	 * @param obj
	 *            删除请求的Entity对象
	 * @return 影响的记录数 (仅主表，级联修改的记录行数未算在内。)
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public int deleteCascade(Object entity) throws SQLException {
		if (entity == null)
			return 0;
		IQueryableEntity obj;
		if (entity instanceof IQueryableEntity) {
			obj = (IQueryableEntity) entity;
			return deleteCascade0(obj, 0);
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity);
			obj = meta.transfer(entity, true);
			return delete(obj.getQuery());
		}
	}

	/**
	 * 合并记录——记录如果已经存在，则比较并更新；如果不存在则新增。（无级联操作）
	 * 
	 * @param entity
	 *            要合并的记录数据
	 * @return 如果插入返回对象本身，如果是更新则返回旧记录的值(如果插入，返回null;如果没修改，返回原对象;如果修改，返回旧对象。)
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("unchecked")
	public <T> T merge(T entity) throws SQLException {
		if (entity instanceof IQueryableEntity) {
			return (T) merge0((IQueryableEntity) entity, null);
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
			PojoWrapper wrapper = meta.transfer(entity, false);
			wrapper = merge0(wrapper, null);
			return wrapper == null ? null : (T) wrapper.get();
		}
	}

	/**
	 * 合并记录——记录如果已经存在，则比较并更新；如果不存在则新增。（无级联操作）
	 * 
	 * @param entity
	 *            要合并的记录数据
	 * @param keys
	 *            业务主键字段名
	 * @return 如果插入返回对象本身，如果是更新则返回旧记录的值(如果插入，返回null;如果没修改，返回原对象;如果修改，返回旧对象。)
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("unchecked")
	public <T> T merge(T entity, String[] keys) throws SQLException {
		if (keys != null && keys.length == 0) {
			keys = null;
		}
		if (entity instanceof IQueryableEntity) {
			return (T) merge0((IQueryableEntity) entity, keys);
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
			PojoWrapper wrapper = meta.transfer(entity, false);
			wrapper = merge0(wrapper, keys);
			return wrapper == null ? null : (T) wrapper.get();
		}
	}

	/**
	 * @param entity
	 * @return the old value.
	 * @throws SQLException
	 */
	private final <T extends IQueryableEntity> T merge0(T entity, String[] keys) throws SQLException {
		@SuppressWarnings("unchecked")
		Query<T> q = entity.getQuery();
		q.setCascade(false);
		ITableMetadata meta = q.getMeta();
		if (keys == null && meta.getPKFields().isEmpty()) {
			throw new UnsupportedOperationException("The tables has no primark key, must assign compare keys.");
		}

		T old = null;
		if (keys == null) {
			if (DbUtils.getPrimaryKeyValue(entity) != null) {
				old = load(entity, true);
				entity.clearQuery();
			}
		} else {
			for (String s : keys) {
				ColumnMapping cdef = meta.getColumnDef(meta.getField(s));
				q.addCondition(cdef.field(), cdef.getFieldAccessor().get(entity));
			}
			old = load(entity, false);
			entity.clearQuery();
		}
		if (old != null) {
			DbUtils.compareToUpdateMap(entity, old);
			if (old.needUpdate()) {
				update(old);
				return old;
			} else {
				return entity;
			}
		}
		// 如果旧数据不存在
		insert(entity);
		return null;
	}

	/**
	 * 合并记录——记录如果已经存在，则比较并更新；如果不存在则新增。（带级联操作）
	 * 
	 * @param entity
	 *            要合并的记录数据
	 * @return 如果插入返回对象本身，如果是更新则返回旧记录的值
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("unchecked")
	public <T> T mergeCascade(T entity) throws SQLException {
		if (entity instanceof IQueryableEntity) {
			return (T) mergeCascade0((IQueryableEntity) entity);
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
			PojoWrapper wrapper = meta.transfer(entity, false);
			wrapper = mergeCascade0(wrapper);
			return (T) wrapper.get();
		}
	}

	private <T extends IQueryableEntity> T mergeCascade0(T entity) throws SQLException {
		T old = null;
		ITableMetadata meta = MetaHolder.getMeta(entity);
		// 无主键匹配法
		if (meta.getPKFields().isEmpty()) {
			throw new UnsupportedOperationException("Do not support merge operate on table without primary key.");
		} else if (DbUtils.getPrimaryKeyValue(entity) != null) {
			old = load(entity, true);
			if (old != null) {
				CascadeUtil.compareToNewUpdateMap(entity, old);// 之所以是将对比结果放到新对象中，是为了能将新对象中级联关系也保存到数据库中。
				updateCascade(entity);
				return old;
			}
		}
		// 如果旧数据不存在
		insertCascade(entity);
		return null;
	}

	/**
	 * 插入数据（带级联）<br>
	 * 如果和其他表具有1VS1、1VSN的关系，那么插入时会自动维护其他表中的数据。这些操作包括了Insert或者update.
	 * 
	 * @param obj
	 *            插入的对象
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public void insertCascade(Object obj) throws SQLException {
		insertCascade0(obj, ORMConfig.getInstance().isDynamicInsert(), 0);
	}

	/**
	 * 插入数据（带级联）<br>
	 * 如果和其他表具有1VS1或1VSN的关系，那么插入时会自动维护其他表中的数据，这些操作包括了Insert或者update.
	 * 
	 * @param obj
	 *            插入的对象
	 * @param dynamic
	 *            dynamic模式：某些字段在数据库中设置了defauelt value。
	 *            如果在实体中为null，那么会将null值插入数据库，造成数据库的缺省值无效。 为了使用dynamic模式后，
	 *            只有手工设置为null的属性，插入数据库时才是null。如果没有设置过值，在插入数据库时将使用数据库的默认值。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public void insertCascade(Object obj, boolean dynamic) throws SQLException {
		insertCascade0(obj, dynamic, 0);
	}

	/**
	 * 插入对象 <br>
	 * 不处理级联关系。如果要支持级联插入，请使用{@link #insertCascade(IQueryableEntity)}
	 * 
	 * @param obj
	 *            插入的对象。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public void insert(Object obj) throws SQLException {
		insert(obj, null, ORMConfig.getInstance().isDynamicInsert());
	}

	/**
	 * 插入对象。<br>
	 * 如果使用dynamic模式将会忽略掉没有set过的属性值
	 * 
	 * @param obj
	 *            插入的对象。
	 * @param dynamic
	 *            dynamic模式：某些字段在数据库中设置了defauelt value。
	 *            如果在实体中为null，那么会将null值插入数据库，造成数据库的缺省值无效。 为了使用dynamic模式后，
	 *            只有手工设置为null的属性，插入数据库时才是null。如果没有设置过值，在插入数据库时将使用数据库的默认值。
	 */
	public void insert(IQueryableEntity obj, boolean dynamic) throws SQLException {
		insert(obj, null, dynamic);
	}

	/**
	 * 插入对象(自定义插入的表名)
	 * 
	 * @param obj
	 *            要插入的对象
	 * @param myTableName
	 *            自定义表名称，一旦自定义了表名，将直接使用此表；不再计算表名
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * 
	 */
	public void insert(IQueryableEntity obj, String myTableName) throws SQLException {
		insert(obj, myTableName, ORMConfig.getInstance().isDynamicInsert());
	}

	/**
	 * 插入对象(自定义插入的表名)
	 * 
	 * @param obj
	 *            要插入的对象
	 * @param myTableName
	 *            自定义表名称，一旦自定义了表名，将直接使用此表；不再计算表名(支持Schema重定向)
	 * @param dynamic
	 *            dynamic模式：某些字段在数据库中设置了defauelt
	 *            value，此时如果在实体中为null，那么会将null值插入数据库，造成数据库的缺省值无效。
	 *            为了使用dynamic模式后，
	 *            只有手工设置为null的属性，插入数据库时才是null。如果没有设置过值，在插入数据库时将使用数据库的默认值。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public void insert(Object entity, String myTableName, boolean dynamic) throws SQLException {
		if (entity == null)
			return;
		ITableMetadata meta = MetaHolder.getMeta(entity);
		if (meta.getExtendsTable() != null) {
			if (myTableName != null) {
				throw new UnsupportedOperationException();// 暂不支持扩展表时定制表名
			}
			insertCascade0(entity, dynamic, 5);
			return;
		}
		IQueryableEntity obj;
		if (entity instanceof IQueryableEntity) {
			obj = (IQueryableEntity) entity;
		} else {
			obj = meta.transfer(entity, false);
		}
		insert0(obj, myTableName, dynamic);
	}

	/**
	 * 按主键删除<strong>单条</strong>对象
	 * 
	 * @param clz
	 *            实体类型
	 * @param keys
	 *            主键的值。<br>
	 *            (注意，可变参数不是用于传入多行记录的值，而是用于传入单条记录的复合主键， 要批量删除多条请用
	 *            {@linkplain #batchDelete}方法)
	 * @return 删除记录条数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public int delete(Class<?> entityClass, Serializable... keys) throws SQLException {
		ITableMetadata meta = MetaHolder.getMeta(entityClass);
		return delete(meta, keys);
	}

	/**
	 * 按主键删除<strong>单条</strong>对象
	 * 
	 * @param entityClass
	 *            实体的元模型
	 * @param keys
	 *            主键的值。<br>
	 *            (注意，可变参数不是用于传入多行记录的值，而是用于传入单条记录的复合主键， 要批量删除多条请用
	 *            {@linkplain #batchDelete}方法)
	 * @return 删除记录条数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("rawtypes")
	public int delete(ITableMetadata meta, Serializable... keys) throws SQLException {
		return delete(new PKQuery(meta, keys));
	}

	/**
	 * 按指定字段的值删除对象。<br>
	 * 如果要按该字段批量删除对象，请使用 {@link #batchDeleteByField(Field, List) }方法。
	 * 
	 * 
	 * @param field
	 *            作为删除条件的字段
	 * @param value
	 *            删除条件值
	 * @return 删除的行数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public <T> int deleteByField(Field field, Object value) throws SQLException {
		ITableMetadata meta = DbUtils.getTableMeta(field);
		Query<?> query = meta.newInstance().getQuery();
		query.addCondition(field, Operator.EQUALS, value);
		return this.delete(query);
	}

	/**
	 * 删除对象
	 * 
	 * @param obj
	 *            删除请求
	 * @return 影响的记录数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public int delete(Object entity) throws SQLException {
		if (entity == null)
			return 0;
		IQueryableEntity obj;
		if (entity instanceof IQueryableEntity) {
			obj = (IQueryableEntity) entity;
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity);
			obj = meta.transfer(entity, true);
		}
		return delete(obj.getQuery());
	}

	/**
	 * 根据一个Query条件删除数据
	 * 
	 * @param query
	 *            删除请求
	 * @return 影响的记录数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public int delete(Query<?> query) throws SQLException {
		ITableMetadata meta = query.getMeta();
		if (meta.getExtendsTable() != null) {
			return deleteCascade0(query.getInstance(), 5);
		}
		return delete0(query);
	}

	/**
	 * 更新对象，无级联操作
	 * 
	 * @param obj
	 *            被更新的对象
	 * @return 影响的记录行数
	 * @throws SQLException
	 * @see {@link #updateCascade(IQueryableEntity)}
	 */
	public int update(Object obj) throws SQLException {
		int n = update(obj, null);
		return n;
	}

	/**
	 * 更新对象，无级联操作，可以指定操作的表名
	 * 
	 * @param obj
	 *            被更新的对象
	 * @param myTableName
	 *            要操作的表名，支持Schema重定向
	 * @return 影响的记录行数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see #update(IQueryableEntity)
	 */
	public int update(Object entity, String myTableName) throws SQLException {
		if (entity == null)
			return 0;
		ITableMetadata meta = MetaHolder.getMeta(entity);
		IQueryableEntity obj;
		if (entity instanceof IQueryableEntity) {
			obj = (IQueryableEntity) entity;
		} else {
			obj = meta.transfer(entity, true);
		}
		if (meta.getExtendsTable() != null) {
			if (myTableName != null) {
				throw new UnsupportedOperationException();// 暂不支持扩展表时定制表名
			}
			return updateCascade0(obj, 5);
		} else {
			return update0(obj, myTableName);
		}
	}

	/**
	 * <h3>最基本的查询方法</h3> 根据指定的条件查询数据库
	 * 
	 * <h3>支持级联操作</h3> 根据在实体中的定义，查询时会自动填充关联到其他表的字段<br>
	 * 在OneToOne和ManyToOne当中，会使用左连接一次查询出来（默认情况下）<br>
	 * 在OneToMany和ManyToMany中，会使用延迟加载，当访问字段属性时去数据库查询。<br>
	 * 
	 * 
	 * <h3>性能的平衡</h3>
	 * ORM框架会尽可能减少查询操作次数以确保性能。但是如果您返回一个很大的结果集，并且每个结果都需要关联到其他表再做一次查询的话，<br>
	 * 可能会给性能带来一定的影响。如果您确定不需要这些填充关联字段的，可以使用{@link Query#setCascade(boolean)}，例如：
	 * 
	 * <pre>
	 * <code>
	 *  Person p=new Person();
	 *  p.setId(1);
	 *  p.getQuery().setCascade(false); //关闭级联查询
	 *  List<Person> result = db.select(p);
	 * </code>
	 * </pre>
	 * 
	 * <h3>和其他方法关系</h3> 这两种写法是完全等效的
	 * 
	 * <pre>
	 * <tt>
	 * List<Person> result = db.select(p);
	 * List<Person> result = db.select(p.getQuery());
	 * </tt>
	 * </pre>
	 * <p>
	 * 当需要指定结果范围（分页）时 {@link #select(IQueryableEntity, PageLimit)}
	 * 
	 * 
	 * 
	 * @param <T>
	 *            泛型： 被查询的数据类型
	 * @param obj
	 *            被查询的对象（可携带{@link Query}以表示复杂的查询）
	 * @return 查询结果列表。不会返回null.
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see Query
	 */
	public <T extends IQueryableEntity> List<T> select(T obj) throws SQLException {
		return select(obj, (PageLimit) null);
	}

	/**
	 * <h3>基本的查询方法</h3> 根据指定的条件(Query格式)查询结果<br>
	 * 可能的查询操作包括多表连接查询和多次查询。（一对一和多对一查询使用多表连接，一对多和多对多使用多次查询。<br>
	 * JEF会尽可能减少查询操作次数以确保性能，但是要注意，如果您返回一个很大的结果集，并且每个结果都需要关联到其他表再做一次查询的话，<br>
	 * 可能会给性能带来一定的影响。如果您确定不需要这些填充关联字段的，请尽量使用selectSingel方法。<br>
	 * 
	 * @param queryObj
	 *            查询对象
	 * @return 查询结果，不会返回null
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public <T> List<T> select(TypedQuery<T> queryObj) throws SQLException {
		return select(queryObj, (PageLimit) null);
	}

	/**
	 * 根据指定的条件查询结果，带分页返回
	 * 
	 * @param obj
	 *            查询对象，
	 * @param range
	 *            范围，Start and limit
	 * @return 查询结果
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("unchecked")
	public <T extends IQueryableEntity> List<T> select(T obj, PageLimit range) throws SQLException {
		Query<T> query = (Query<T>) obj.getQuery();
		QueryOption option = QueryOption.createFrom(query);
		return typedSelect(query, range, option);
	}

	/**
	 * 根据拼装好的Query进行查询
	 * 
	 * @param cq
	 *            查询条件
	 * @param metadata
	 *            拼装结果类型
	 * @param range
	 *            分页范围，如果无须分页，查询语句已经包含了范围，此处可传null
	 * @return 查询结果
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> select(ConditionQuery queryObj, PageLimit range) throws SQLException {
		QueryOption option;
		if (queryObj instanceof JoinElement) {
			option = QueryOption.createFrom((JoinElement) queryObj);
			if (queryObj instanceof Query<?>) {
				Query<?> simpleQuery = (Query<?>) queryObj;
				JoinElement element = DbUtils.toReferenceJoinQuery(simpleQuery, null);
				return this.innerSelect(element, range, simpleQuery.getFilterCondition(), option);
			} else {
				return this.innerSelect(queryObj, range, null, option);
			}
		} else {
			option = QueryOption.DEFAULT;
			return this.innerSelect(queryObj, range, null, option);
		}
	}

	/**
	 * 根据拼装好的Query进行查询。并将结果转换为期望的对象。
	 * 
	 * @param <T>
	 * 
	 * @param queryObj
	 *            查询条件
	 * @param resultClz
	 *            结果转换类型
	 * @param range
	 *            分页范围，如果无须分页，查询语句已经包含了范围，此处可传null
	 * 
	 * @return 查询结果
	 * 
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * 
	 * @see {@link #select(ConditionQuery, PageLimit)} As same as after calling
	 *      {@code queryObj.getResultTransformer().setResultType(resultClass)}
	 *      then use {@link #select(ConditionQuery, PageLimit)} instead.
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> selectAs(ConditionQuery queryObj, Class<T> resultClz, PageLimit range) throws SQLException {
		queryObj.getResultTransformer().setResultType(resultClz);
		QueryOption option;
		if (queryObj instanceof JoinElement) {
			option = QueryOption.createFrom((JoinElement) queryObj);
		} else {
			option = QueryOption.DEFAULT;
		}
		return this.innerSelect(queryObj, range, null, option);
	}

	/**
	 * 查询并用指定的结果返回。并将结果转换为期望的对象。
	 * 
	 * @param obj
	 *            查询
	 * @param resultType
	 *            每条记录将被转换为指定的类型
	 * @return 查询结果
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see {@link ConditionQuery#getResultTransformer()} and
	 *      {@link Transformer#setResultType(Class)}
	 */
	public <T> List<T> selectAs(ConditionQuery obj, Class<T> resultType) throws SQLException {
		return selectAs(obj, resultType, (PageLimit) null);
	}

	/**
	 * 流式接口，一般用于超大结果集的返回。 <h3>作用</h3> 当结果集超大时，如果用List<T>返回，内存占用很大甚至会溢出。<br>
	 * JDBC设计时考虑到这个问题，因此其返回的ResultSet对象只是查询结果视图的一段，用户向后滚动结果集时，数据库才将需要的数据传到客户端。
	 * 如果客户端不缓存整个结果集，那么前面已经滚动过的结果数据就被释放。 在java8的Stream
	 * API出现之前，只有用Iterator来返回对象了。
	 * <p>
	 * 这种处理方式实际上是一种流式处理模型，iteratedSelect就是这种模型的封装。<br>
	 * iteratedSelect并不会将查询出的所有数据放置到一个List对象中（这常常导致内存溢出）。而是返回一个Iterator对象，
	 * 用户不停的调用next方法向后滚动， 同时释放掉之前处理过的结果对象。这就避免了超大结果返回时内存溢出的问题。
	 * 
	 * 
	 * <h3>注意事项</h3> 由于 ResultIterator
	 * 对象中有尚未关闭的ResultSet对象，因此必须确保使用完后关闭ResultIteratpr.如下示例
	 * 
	 * <pre>
	 * <tt>ResultIterator<TestEntity> iter = db.iteratedSelect(QB.create(TestEntity.class), null);
	 * try{
	 * for(; iter.hasNext();) {
	 * 	iter.next();
	 * 	//do something.
	 *  }	
	 * }finally{
	 *  //必须在finally块中关闭。否则一旦业务逻辑抛出异常，则ResultIterator未释放造成游标泄露.
	 *   iter.close(); 
	 * }</tt>
	 * </pre>
	 * 
	 * 如果ResultSet不释放，相当于数据库上打开了一个不关闭的游标，而数据库的游标数是很有限的，耗尽后将不能执行任何数据库操作。<br>
	 * 
	 * @param queryObj
	 *            查询条件,可以是一个普通的Query,也可以是一个UnionQuery
	 * @param range
	 *            限制结果返回的条数，即分页信息。（传入null表示不限制）
	 * @param strategies
	 *            结果拼装参数
	 * @return 遍历器，可以用于遍历查询结果。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see ResultIterator
	 */
	public <T extends IQueryableEntity> ResultIterator<T> iteratedSelect(TypedQuery<T> queryObj, PageLimit range) throws SQLException {
		QueryOption option;
		ConditionQuery query;
		if (queryObj instanceof JoinElement) {
			option = QueryOption.createFrom((JoinElement) queryObj);
			query = (JoinElement) queryObj;
			if (queryObj instanceof Query<?>) {
				Query<?> q = (Query<?>) queryObj;
				query = DbUtils.toReferenceJoinQuery(q, null);
			}
		} else {
			option = QueryOption.DEFAULT;
			query = queryObj;
		}
		return innerIteratedSelect(query, range, option);
	}

	/**
	 * 流式接口，一般用于超大结果集的返回
	 * 
	 * @param queryObj
	 * @param offset
	 * @param limit
	 * @return
	 * @throws SQLException
	 *             必需特别注意，返回的元素必需要完成遍历，否则结果集就不会被关闭，会产生泄漏！
	 */
	public <T extends IQueryableEntity> Stream<T> iteratedSelect(TypedQuery<T> queryObj, long offset, int limit) throws SQLException {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iteratedSelect(queryObj, limit == 0 ? null : new PageLimit(offset, limit)), 0), false);
	}

	/**
	 * 遍历器模式查找，一般用于超大结果集的返回。 <h3>作用</h3> 当结果集超大时，如果用List<T>返回，内存占用很大甚至会溢出。<br>
	 * JDBC设计时考虑到这个问题，因此其返回的ResultSet对象只是查询结果视图的一段，用户向后滚动结果集时，数据库才将需要的数据传到客户端。
	 * 如果客户端不缓存整个结果集，那么前面已经滚动过的结果数据就被释放。
	 * <p>
	 * 这种处理方式实际上是一种流式处理模型，iteratedSelect就是这种模型的封装。<br>
	 * iteratedSelect并不会将查询出的所有数据放置到一个List对象中（这常常导致内存溢出）。而是返回一个Iterator对象，
	 * 用户不停的调用next方法向后滚动， 同时释放掉之前处理过的结果对象。这就避免了超大结果返回时内存溢出的问题。
	 * 
	 * 
	 * <h3>注意事项</h3> 由于 ResultIterator
	 * 对象中有尚未关闭的ResultSet对象，因此必须确保使用完后关闭ResultIteratpr.如下示例
	 * 
	 * <pre>
	 * <tt>ResultIterator<TestEntity> iter = db.iteratedSelect(QB.create(TestEntity.class), null);
	 * try{
	 * for(; iter.hasNext();) {
	 * 	iter.next();
	 * 	//do something.
	 *  }	
	 * }finally{
	 *  //必须在finally块中关闭。否则一旦业务逻辑抛出异常，则ResultIterator未释放造成游标泄露.
	 *   iter.close(); 
	 * }</tt>
	 * </pre>
	 * 
	 * 如果ResultSet不释放，相当于数据库上打开了一个不关闭的游标，而数据库的游标数是很有限的，耗尽后将不能执行任何数据库操作。<br>
	 * 
	 * 
	 * @param queryObj
	 *            查询条件，可以是一个普通Query,或者UnionQuery,或者Join.
	 * @param pageinfo
	 *            限制结果返回的条数，即分页信息。（传入null表示不限制）
	 * @param strategies
	 *            结果拼装参数
	 * @return 遍历器，可以用于遍历查询结果。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @since 1.2
	 * @see ResultIterator
	 */
	public <T> ResultIterator<T> iteratedSelect(ConditionQuery queryObj, PageLimit pageinfo) throws SQLException {
		QueryOption option;
		if (queryObj instanceof JoinElement) {
			option = QueryOption.createFrom((JoinElement) queryObj);
		} else {
			option = QueryOption.DEFAULT;
		}
		return innerIteratedSelect(queryObj, pageinfo, option);
	}

	/**
	 * 使用指定的查询对象查询，返回结果遍历器。
	 * {@linkplain #iteratedSelect(ConditionQuery, PageLimit) 什么是结果遍历器}
	 * 
	 * @param obj
	 *            查询请求
	 * @param pageinfo
	 *            查询对象范围
	 * @return 结果遍历器(ResultIterator)
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see {@link ResultIterator}
	 */
	public <T extends IQueryableEntity> ResultIterator<T> iteratedSelect(T obj, PageLimit pageinfo) throws SQLException {
		@SuppressWarnings("unchecked")
		Query<T> query = obj.getQuery();
		QueryOption option = QueryOption.createFrom(query);

		// 预处理
		Transformer t = query.getResultTransformer();
		if (!t.isLoadVsOne() || query.getMeta().getRefFieldsByName().isEmpty()) {
			return innerIteratedSelect(query, pageinfo, option);
		}
		// 拼装出带连接的查询请求
		JoinElement q = DbUtils.toReferenceJoinQuery(query, null);
		return innerIteratedSelect(q, pageinfo, option);
	}

	/**
	 * 返回一个可以更新操作的结果数据{@link RecordHolder}<br>
	 * 用户可以在这个RecordHolder上直接更新数据库中的数据，包括插入记录和删除记录<br>
	 * 
	 * <h3>实现原理</h3> RecordHolder对象，是JDBC ResultSet的封装<br>
	 * 实质对用JDBC中ResultSet的updateRow,deleteRow,insertRow等方法，<br>
	 * 该操作模型需要持有ResultSet对象，因此注意使用完毕后要close()方法关闭结果集。 <h3>注意事项</h3>
	 * RecordHolder对象需要手动关闭。如果不关闭将造成数据库游标泄露。 <h3>使用示例</h3>
	 * 
	 * 
	 * @param obj
	 *            查询对象
	 * @return 查询结果被放在RecordHolder对象中，用户可以直接在查询结果上修改数据。最后调用
	 *         {@link RecordHolder#commit}方法提交到数据库。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see RecordHolder
	 */
	public <T extends IQueryableEntity> RecordHolder<T> loadForUpdate(T obj) throws SQLException {
		Assert.notNull(obj);
		@SuppressWarnings("unchecked")
		RecordsHolder<T> r = selectForUpdate(obj.getQuery(), null);
		if (r.size() == 0) {
			r.close();// must close it!!
			return null;
		}
		return r.get(0);
	}

	/**
	 * 返回一个可以更新操作的结果数据集合 实质对用JDBC中ResultSet的updateRow,deleteRow,insertRow等方法， <br>
	 * 该操作模型需要持有ResultSet对象，因此注意使用完毕后要close()方法关闭结果集<br>
	 * 
	 * RecordsHolder可以对选择出来结果集进行更新、删除、新增三种操作，操作完成后调用commit方法<br>
	 * 
	 * @param obj
	 *            查询请求
	 * @return RecordsHolder对象，这是一个可供操作的数据库结果集句柄。注意使用完后一定要关闭。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see RecordsHolder
	 */
	@SuppressWarnings("unchecked")
	public <T extends IQueryableEntity> RecordsHolder<T> selectForUpdate(T query) throws SQLException {
		Assert.notNull(query);
		return selectForUpdate(query.getQuery(), null);
	}

	/**
	 * 返回一个可以更新操作的结果数据集合——RecordsHolder，可以在这个RecordsHolder上直接针对单表添加记录、删除记录、修改记录。
	 * RecordsHolder是JDBC ResultSet的封装。目的是使用ResultSet上的updateRow,deleteRow,
	 * insertRow等方法直接在JDBC数据集上对数据库进行写操作。
	 * 
	 * 类似于PLSQL Developer中的select for update操作。select for update会锁定查询出来的记录。
	 * 
	 * 
	 * @param query
	 *            查询请求
	 * @param range
	 *            限定结果范围
	 * @return RecordsHolder对象，这是一个可供操作的数据库结果集句柄。注意使用完后一定要关闭。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see RecordsHolder
	 */
	public <T extends IQueryableEntity> RecordsHolder<T> selectForUpdate(Query<T> query, PageLimit range) throws SQLException {
		Assert.notNull(query);
		QueryOption option = QueryOption.createFrom(query);
		option.holdResult = true;

		// 对于Oracle来说，如果用select * 会造成结果集强制变为只读，因此必须显式指定列名称。
		if (DbUtils.getMappingProvider(query) == null) {
			Selects select = QB.selectFrom(query);
			AllTableColumns at = select.allColumns(query).noVirtualColumn();
			at.setAliasType(AllTableColumns.AliasMode.RAWNAME);
		}
		@SuppressWarnings("unchecked")
		List<T> objs = innerSelect(query, range, query.getFilterCondition(), option);
		RecordsHolder<T> result = new RecordsHolder<T>(query.getMeta());
		ResultSetContainer rawrs = option.getRs();
		try {
			if (rawrs.size() > 1) {
				throw new UnsupportedOperationException("select from update operate can only support one table.");
			}
			IResultSet rset = option.getRs().toProperResultSet(null);
			result.init((ResultSetWrapper) rset, objs, rset.getProfile());
			return result;
		} catch (RuntimeException t) {
			rawrs.close();// 如果出现异常必须关闭，防止泄漏
			throw t;
		} catch (Error t) {
			rawrs.close();// 如果出现异常必须关闭，防止泄漏
			throw t;
		}
	}

	/**
	 * 查出单个对象。如果结果不唯一抛出NonUniqueResultException。
	 * 
	 * @param obj
	 *            查询条件
	 * @return 使用传入的对象进行查询，结果返回记录的第一条。如未查到返回null。。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @throws NonUniqueResultException
	 *             结果不唯一
	 */
	public <T> T load(T obj) throws SQLException {
		return load(obj, true);

	}

	/**
	 * 查出单个对象
	 * 
	 * @param obj
	 *            查询条件
	 * @param unique
	 *            true要求查询结果必须唯一。false允许结果不唯一，但仅取第一条。
	 * @return 使用传入的对象进行查询，结果返回记录的第一条。如未查到返回null
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @throws NonUniqueResultException
	 *             当unique为true且查询结果不唯一时。
	 */
	@SuppressWarnings("unchecked")
	public <T> T load(T obj, boolean unique) throws SQLException {
		Assert.notNull(obj);
		if (obj instanceof IQueryableEntity) {
			return (T) innerLoad0((IQueryableEntity) obj, unique);
		} else {
			PojoWrapper vw = innerLoad0(MetaHolder.getMeta(obj.getClass()).transfer(obj, true), unique);
			return vw == null ? null : (T) vw.get();
		}
	}

	private <T extends IQueryableEntity> T innerLoad0(T obj, boolean unique) throws SQLException {
		@SuppressWarnings("unchecked")
		Query<T> q = obj.getQuery();
		QueryOption option = QueryOption.createMax1Option(q);
		List<T> l = typedSelect(q, null, option);
		if (l.isEmpty()) {
			return null;
		} else if (l.size() > 1 && unique) {
			throw new NonUniqueResultException("Result is not unique." + q);
		}
		return l.get(0);
	}

	/**
	 * 根据样例查找
	 * 
	 * @param entity
	 *            查询条件
	 * @param property
	 *            作为查询条件的字段名。当不指定properties时，首先检查entity当中是否设置了主键，如果有主键按主键删除，
	 *            否则按所有非空字段作为匹配条件。
	 * @return 查询结果
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> selectByExample(T entity, String... propertyName) throws SQLException {
		if (entity instanceof IQueryableEntity) {
			return select(DbUtils.populateExampleConditions((IQueryableEntity) entity, propertyName), (PageLimit) null);
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
			Query<PojoWrapper> q;
			if (propertyName.length == 0) {
				q = (Query<PojoWrapper>) meta.transfer(entity, true).getQuery();
			} else {
				q = DbUtils.populateExampleConditions(meta.transfer(entity, false), propertyName);
			}
			return PojoWrapper.unwrapList(select(q));
		}
	}

	/**
	 * 根据字段查询
	 * 
	 * @param meta
	 *            表元数据
	 * @param propertyName
	 *            字段名
	 * @param value
	 *            字段值
	 * @return 查询结果
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public List<?> selectByField(ITableMetadata meta, String propertyName, Object value) throws SQLException {
		if (meta == null || propertyName == null)
			return null;
		ColumnMapping field = meta.findField(propertyName);
		if (field == null) {
			throw new IllegalArgumentException("There's no property named " + propertyName + " in type of " + meta.getName());
		}
		Query<?> q = QB.create(meta);
		q.addCondition(field.field(), Operator.EQUALS, value);
		if (meta.getType() == EntityType.POJO) {
			return PojoWrapper.unwrapList(select((Query<PojoWrapper>) q));
		} else {
			return select(q);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> selectByField(Class<T> meta, String propertyName, Object value) throws SQLException {
		return (List<T>) selectByField(MetaHolder.getMeta(meta), propertyName, value);
	}

	/**
	 * 按指定的字段的值加载记录<br>
	 * 如果要根据该字段的值批量加载记录，可使用 {@link #batchLoadByField(Field, List) }方法。
	 * 
	 * @param field
	 *            作为查询条件的字段
	 * @param values
	 *            要查询的值
	 * @return 符合条件的记录
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> selectByField(jef.database.Field field, Object value) throws SQLException {
		ITableMetadata meta = DbUtils.getTableMeta(field);
		@SuppressWarnings("rawtypes")
		Query query = QB.create(meta);
		query.addCondition(field, Operator.EQUALS, value);
		return typedSelect(query, null, QueryOption.DEFAULT);
	}

	/**
	 * 按指定的字段的值加载记录<br>
	 * 如果查询结果不唯一，抛出NonUniqueResultException异常
	 * 
	 * @param field
	 *            作为查询条件的字段
	 * @param value
	 *            要查询的值
	 * @return 符合条件的记录
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @throws NonUniqueResultException
	 *             结果不唯一
	 */
	public <T> T loadByField(jef.database.Field field, Object value) throws SQLException {
		return loadByField(field, value, true);

	}

	/**
	 * 按指定的字段的值加载记录<br>
	 * 如果指定的字段不是主键，也只返回第一条数据。
	 * 
	 * @param field
	 *            作为查询条件的字段
	 * @param value
	 *            要查询的值
	 * @param unique
	 *            是否要求结果唯一，为true时如结果不唯一将抛出NonUniqueResultException异常
	 * 
	 * @return 符合条件的记录
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @throws NonUniqueResultException
	 *             结果不唯一
	 */
	@SuppressWarnings("unchecked")
	public <T> T loadByField(jef.database.Field field, Object value, boolean unique) throws SQLException {
		ITableMetadata meta = DbUtils.getTableMeta(field);
		Query<?> query = meta.newInstance().getQuery();
		query.addCondition(field, Operator.EQUALS, value);
		List<?> list = typedSelect(query, null, QueryOption.DEFAULT_MAX1);
		if (list.isEmpty()) {
			return null;
		} else if (list.size() > 1 && unique) {
			throw new NonUniqueResultException();
		}
		if (meta.getType() == EntityType.POJO) {
			return (T) ((PojoWrapper) list.get(0)).get();
		} else {
			return (T) list.get(0);
		}
	}

	/**
	 * 按指定的字段查询记录
	 * 
	 * @param type
	 *            实体类型
	 * @param field
	 *            字段名
	 * @param value
	 *            查询值
	 * @return 查询结果
	 * @throws SQLException
	 */
	public <T> T loadByField(Class<T> type, String field, Object value) throws SQLException {
		ITableMetadata meta = MetaHolder.getMeta(type);
		return loadByField(meta, field, value, true);
	}

	/**
	 * 按指定的字段查询记录
	 * 
	 * @param meta
	 *            实体类型
	 * @param field
	 *            字段名
	 * @param value
	 *            查询值
	 * @param unique
	 *            是否唯一
	 * @return 查询结果
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public <T> T loadByField(ITableMetadata meta, String field, Object value, boolean unique) throws SQLException {
		if (meta == null || field == null) {
			return null;
		}
		ColumnMapping def = meta.findField(field);
		if (def == null) {
			throw new IllegalArgumentException("There's no field [" + field + "] in " + meta.getName());
		}
		return (T) loadByField(def.field(), value, unique);
	}

	/**
	 * 按主键获取一条记录。注意这里的可变参数是为了支持复合主键，并不是加载多条记录。<br>
	 * 如需加载多条记录，请用 {@link #batchLoad(Class, List) } 方法
	 * 
	 * @param meta
	 *            元数据
	 * @param keys
	 *            主键的值。
	 * @return 查询结果
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> T load(ITableMetadata meta, Serializable... keys) throws SQLException {
		if (meta.getType() == EntityType.POJO) {
			PKQuery<PojoWrapper> query = new PKQuery<PojoWrapper>(meta, keys);
			List<PojoWrapper> result = innerSelect(query, null, null, QueryOption.DEFAULT_MAX1);
			if (result.isEmpty())
				return null;
			return (T) result.get(0).get();
		} else {
			PKQuery query = new PKQuery(meta, keys);
			List<T> result = innerSelect(query, null, null, QueryOption.DEFAULT_MAX1);
			if (result.isEmpty())
				return null;
			return result.get(0);
		}
	}

	/**
	 * 按主键获取一条记录。注意这里的可变参数是为了支持复合主键，并不是加载多条记录。
	 * 
	 * @param clz
	 *            类型
	 * @param keys
	 *            主键的值。
	 * @return 查询结果
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public <T> T load(Class<T> entityClass, Serializable... keys) throws SQLException {
		if (keys.length == 0 || keys[0] == null) {
			throw new IllegalArgumentException("Please input a valid value as primary key.");
		}
		AbstractMetadata meta = MetaHolder.getMetaOrTemplate(entityClass);
		return load(meta, keys);
	}

	/**
	 * 按主键加载多条记录。适用与拥有大量主键值，需要在数据库中查询与之对应的记录时。<br>
	 * 查询会使用IN条件来减少操作数据库的次数。如果要查询的条件超过了500个，会自动分多次进行查询。
	 * <p>
	 * <strong>注意：在多库操作下，这一方法不支持对每条记录单独分组并计算路由。</strong>
	 * <strong>注意：此方法不支持复合主键</strong>
	 * 
	 * @param clz
	 *            实体类
	 * @param pkValues
	 *            主键的值(多值)
	 * @return 查询结果
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final <T> List<T> batchLoad(Class<T> clz, List<? extends Serializable> pkValues) throws SQLException {
		ITableMetadata meta = MetaHolder.getMeta(clz);
		return batchLoad(meta, pkValues);
	}

	/**
	 * 按主键加载多条记录。适用与拥有大量主键值，需要在数据库中查询与之对应的记录时。<br>
	 * 查询会使用IN条件来减少操作数据库的次数。如果要查询的条件超过了500个，会自动分多次进行查询。
	 * <p>
	 * <strong>注意：在多库操作下，这一方法不支持对每条记录单独分组并计算路由。</strong>
	 * <strong>注意：此方法不支持复合主键</strong>
	 * 
	 * @param meta
	 *            实体元数据
	 * @param pkValues
	 *            主键的值(多值)
	 * @return 查询结果
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("unchecked")
	public final <T> List<T> batchLoad(ITableMetadata meta, List<? extends Serializable> pkValues) throws SQLException {
		int MAX_IN_CONDITIONS = ORMConfig.getInstance().getMaxInConditions();
		if (pkValues.size() < MAX_IN_CONDITIONS) {
			return batchLoadByPK0(meta, pkValues);
		}
		List<T> result = new ArrayList<T>(MAX_IN_CONDITIONS);
		int offset = 0;
		while (pkValues.size() - offset > MAX_IN_CONDITIONS) {
			List<T> r = batchLoadByPK0(meta, pkValues.subList(offset, offset + MAX_IN_CONDITIONS));
			result.addAll(r);
			offset += MAX_IN_CONDITIONS;
		}
		if (pkValues.size() > offset) {
			result.addAll(batchLoadByPK0(meta, pkValues.subList(offset, pkValues.size())));
		}
		return result;
	}

	/**
	 * 按指定的字段加载多条记录.适用与拥有大量键值，需要在数据库中查询与之对应的记录时。<br>
	 * 查询会使用IN条件来减少操作数据库的次数。如果要查询的条件超过了500个，会自动分多次进行查询。 <strong>注意：</strong>
	 * <ol>
	 * <li>在多库操作下，这一方法不支持对每条记录单独分组并计算路由。</li>
	 * <li>不支持复合主键。</li>
	 * </ol>
	 * 
	 * @param field
	 *            字段
	 * @param values
	 *            条件值
	 * @return 查询结果
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final <T> List<T> batchLoadByField(jef.database.Field field, List<?> values) throws SQLException {
		int MAX_IN_CONDITIONS = ORMConfig.getInstance().getMaxInConditions();
		if (values.size() < MAX_IN_CONDITIONS)
			return batchLoadByField0(field, values);

		List<T> result = new ArrayList<T>(800);
		int offset = 0;
		while (values.size() - offset > MAX_IN_CONDITIONS) {
			List<T> r = batchLoadByField0(field, values.subList(offset, offset + MAX_IN_CONDITIONS));
			result.addAll(r);
			offset += MAX_IN_CONDITIONS;
		}
		if (values.size() > offset) {
			List<T> r = batchLoadByField0(field, values.subList(offset, values.size()));
			result.addAll(r);
		}
		return result;
	}

	/**
	 * 执行数据库查询。并将结果转换为期望的对象。查询结果不唯一抛出NonUniqueResultException
	 * 
	 * @param queryObj
	 *            查询
	 * @param resultClz
	 *            单条记录返回结果类型
	 * @return 查询结果将只返回第一条。如果查询结果数量为0，那么将返回null
	 * 
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @throws NonUniqueResultException
	 *             结果不唯一，如不想接收到异常，请用
	 *             {@link #loadAs(ConditionQuery, Class, boolean)}
	 * 
	 */
	public <T> T loadAs(ConditionQuery queryObj, Class<T> resultClz) throws SQLException {
		return loadAs(queryObj, resultClz, true);
	}

	/**
	 * 执行数据库查询。并将结果转换为期望的对象。
	 * 
	 * @param queryObj
	 *            查询
	 * @param resultClz
	 *            单条记录返回结果类型
	 * @param unique
	 *            要求查询结果唯一。为true时如果查询结果不唯一抛出NonUniqueResultException
	 * @return 查询结果将只返回第一条。如果查询结果数量为0，那么将返回null
	 * 
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @throws NonUniqueResultException
	 *             结果不唯一
	 * 
	 */
	public <T> T loadAs(ConditionQuery queryObj, Class<T> resultClz, boolean unique) throws SQLException {
		queryObj.getResultTransformer().setResultType(resultClz);
		return load(queryObj, unique);
	}

	/**
	 * 查询并指定返回结果。要求查询结果必须唯一。
	 * 
	 * @param queryObj
	 *            查询
	 * @return 查询结果将只返回第一条。如果查询结果数量为0，那么将返回null。
	 * 
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @throws NonUniqueResultException
	 *             结果不唯一
	 */
	public <T> T load(ConditionQuery queryObj) throws SQLException {
		return load(queryObj, true);
	}

	/**
	 * 查询并指定返回结果。
	 * 
	 * @param queryObj
	 *            查询
	 * @param unique
	 *            要求查询结果是否唯一。为true时，查询结果不唯一将抛出异常。为false时，查询结果不唯一仅取第一条。
	 * 
	 * @return 查询结果将只返回第一条。如果查询结果数量为0，那么将返回null。
	 * 
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @throws NonUniqueResultException
	 *             结果不唯一
	 */
	public <T> T load(ConditionQuery queryObj, boolean unique) throws SQLException {
		Assert.notNull(queryObj);
		QueryOption option;
		Map<Reference, List<Condition>> filters = null;

		if (queryObj instanceof JoinElement) {
			if (queryObj instanceof Query<?>) {
				Query<?> qq = (Query<?>) queryObj;
				Transformer t = queryObj.getResultTransformer();
				if (t.isLoadVsOne() && !qq.getMeta().getRefFieldsByName().isEmpty()) {
					queryObj = DbUtils.toReferenceJoinQuery(qq, null);
				}
				filters = qq.getFilterCondition();
			}
		}
		option = queryObj.getMaxResult() > 0 ? QueryOption.createFrom(queryObj) : QueryOption.DEFAULT_MAX1;
		@SuppressWarnings("unchecked")
		List<T> l = innerSelect(queryObj, null, filters, option);
		if (l.isEmpty()) {
			return null;
		} else if (l.size() > 1 && unique) {
			throw new NonUniqueResultException("Result is not unique." + queryObj);
		}
		T result = l.get(0);
		// if (ORMConfig.getInstance().isDebugMode()) {
		// LogUtil.show("Result:" + result);
		// }
		return result;
	}

	/**
	 * 查询某个对象表中的所有数据
	 * 
	 * @param cls
	 *            实体类型
	 * @return 该类型所对应的表中的所有数据
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public <T extends IQueryableEntity> List<T> selectAll(Class<T> cls) throws SQLException {
		return selectAll(MetaHolder.getMeta(cls));
	}

	/**
	 * 查询某个模型的表中的所有数据
	 * 
	 * @param meta
	 *            表的元模型
	 * @return 该类型所对应的表中的所有数据
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("unchecked")
	public <T extends IQueryableEntity> List<T> selectAll(ITableMetadata meta) throws SQLException {
		return (List<T>) select(QB.create(meta));
	}

	/**
	 * 允许所有条件的分页查询，查询结果将转换为指定的类型。
	 * <p>
	 * 也可以用以下的方法指定查询结果要转换的类型
	 * {@link #jef.database.Session.pageSelect(ConditionQuery, int)}<br>
	 * and <br>
	 * <tt> query.getResultTransformer().setResultType(type) to assign return type.</tt>
	 * 
	 * @param query
	 *            查询请求
	 * @param resultWrapper
	 *            查询返回结果类型
	 * @param pageSize
	 *            每页记录条数
	 * @return PagingIterator对象，可实现结果范围限定和前后翻页的对象。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * 
	 * @see PagingIterator
	 */
	public <T> PagingIterator<T> pageSelect(ConditionQuery query, Class<T> resultWrapper, int pageSize) throws SQLException {
		query.getResultTransformer().setResultType(resultWrapper);
		return new PagingIteratorObjImpl<T>(query, pageSize, this);// 因为query包含了路由信息，所以可以允许直接传入session
	}

	/**
	 * 按自定义的条件实现分页查询
	 * 
	 * @param query
	 *            查询请求
	 * @param pageSize
	 *            每页记录条数
	 * @return PagingIterator对象，可实现结果范围限定和前后翻页的对象。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see PagingIterator
	 */
	public <T> PagingIterator<T> pageSelect(ConditionQuery query, int pageSize) throws SQLException {
		return new PagingIteratorObjImpl<T>(query, pageSize, this);// 因为query包含了路由信息，所以可以允许直接传入session
	}

	/**
	 * 将传入的SQL语句创建为NativeQuery，然后再以此进行分页查询
	 * 
	 * @param sql
	 *            E-SQL语句
	 * @param resultClass
	 *            返回结果类型
	 * @param pageSize
	 *            每页记录条数
	 * @return PagingIterator对象，可实现结果范围限定和前后翻页的对象。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see #createNativeQuery(String, Class)
	 * @see NativeQuery
	 * @see PagingIterator
	 */
	public <T> PagingIterator<T> pageSelect(String sql, Class<T> resultClass, int pageSize) throws SQLException {
		NativeQuery<T> q = this.createNativeQuery(sql, resultClass);
		PagingIterator<T> result = new PagingIteratorNativeQImpl<T>(q, pageSize);
		return result;
	}

	/**
	 * 将传入的SQL语句创建为NativeQuery，然后再以此进行分页查询
	 * 
	 * @param sql
	 *            E-SQL语句
	 * @param meta
	 *            返回结果类型
	 * @param pageSize
	 *            每页数据条数
	 * @return PagingIterator对象，可实现结果范围限定和前后翻页的对象。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see #createNativeQuery(String, ITableMetadata)
	 * @see NativeQuery
	 * @see PagingIterator
	 */
	public <T> PagingIterator<T> pageSelect(String sql, ITableMetadata meta, int pageSize) throws SQLException {
		NativeQuery<T> q = this.createNativeQuery(sql, meta);
		PagingIterator<T> result = new PagingIteratorNativeQImpl<T>(q, pageSize);
		return result;
	}

	/**
	 * 根据NativeQuery进行分页查询，SQL中不必写分页逻辑。JEF会自动编写count语句查询总数，并且限定结果
	 * 
	 * @param sql
	 *            查询Query对象
	 * @param pageSize
	 *            每页数据条数
	 * @return PagingIterator对象，可实现结果范围限定和前后翻页的对象。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see PagingIterator
	 */
	public final <T> PagingIterator<T> pageSelect(NativeQuery<T> sql, int pageSize) throws SQLException {
		PagingIterator<T> result = new PagingIteratorNativeQImpl<T>(sql, pageSize);
		return result;
	}

	/**
	 * 根据查询对象和表名实现分页查询
	 * 
	 * @param obj
	 *            查询请求
	 * @param pageSize
	 *            每页数据条数
	 * @return PagingIterator对象，可实现结果范围限定和前后翻页的对象。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see PagingIterator
	 */
	public final <T extends IQueryableEntity> PagingIterator<T> pageSelect(T obj, int pageSize) throws SQLException {
		PagingIterator<T> result = new PagingIteratorObjImpl<T>(obj, pageSize, this);
		return result;
	}

	/**
	 * 
	 * @param entity
	 *            查询请求
	 * @param offset
	 *            跳过offset条
	 * @param limit
	 *            限制limit条
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public <T> Page<T> selectPage(T entity, int offset, int limit) throws SQLException {
		if (entity instanceof IQueryableEntity) {
			return (Page<T>) pageSelect((IQueryableEntity) entity, limit).setOffset(offset).getPageData();
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
			PojoWrapper vw = meta.transfer(entity, true);
			Page<PojoWrapper> page = pageSelect(vw, limit).setOffset(offset).getPageData();
			return PojoWrapper.unwrapPage(page);
		}

	}

	/*
	 * 查询实现，解析查询对象，将单表对象解析为Join查询
	 */
	@SuppressWarnings("unchecked")
	protected final <T extends IQueryableEntity> List<T> typedSelect(Query<T> queryObj, PageLimit range, QueryOption option) throws SQLException {
		// 预处理
		Transformer t = queryObj.getResultTransformer();
		if (!t.isLoadVsOne() || queryObj.getMeta().getRefFieldsByName().isEmpty()) {
			return innerSelect(queryObj, range, null, option);
		}
		// 拼装出带连接的查询请求
		JoinElement q = DbUtils.toReferenceJoinQuery(queryObj, null);
		return innerSelect(q, range, queryObj.getFilterCondition(), option);
	}

	@SuppressWarnings("unchecked")
	final <T> ResultIterator<T> innerIteratedSelect(ConditionQuery queryObj, PageLimit range, QueryOption option) throws SQLException {
		if (range != null && range.getLimit() <= 0) {
			return new ResultIterator.Impl<T>(new ArrayList<T>().iterator(), null);
		}

		long start = System.currentTimeMillis();// 开始时间
		QueryClause sql = selectp.toQuerySql(queryObj, range, true);
		if (sql.isEmpty())
			return new ResultIterator.Impl<T>(new ArrayList<T>().iterator(), null);

		ResultIterator<T> result;
		ResultSetContainer rs = new ResultSetContainer(false);
		long parse = System.currentTimeMillis();
		selectp.processSelect(sql, this, queryObj, rs, option, 1);
		long dbselect = System.currentTimeMillis();
		LogUtil.show(StringUtils.concat("Result: Iterator", "\t Time cost([ParseSQL]:", String.valueOf(parse - start), "ms, [DbAccess]:",
				String.valueOf(dbselect - parse), "ms) |", getTransactionId(null)));
		EntityMappingProvider mapping = DbUtils.getMappingProvider(queryObj);
		Transformer transformer = queryObj.getResultTransformer();
		IResultSet irs = rs.toProperResultSet(null, transformer.getStrategy());
		result = new ResultIterator.Impl<T>(iterateResultSet(irs, mapping, transformer), irs);
		return result;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	final List innerSelect(ConditionQuery queryObj, PageLimit range, Map<Reference, List<Condition>> filters, QueryOption option) throws SQLException {
		boolean debugMode = ORMConfig.getInstance().isDebugMode();
		if (range != null && range.getLimit() <= 0) {
			if (debugMode)
				LogUtil.show("Query has limit to no range. return empty list. " + range);
			return Collections.EMPTY_LIST;
		}

		long start = System.currentTimeMillis();// 开始时间
		// 生成 SQL
		QueryClause sql = selectp.toQuerySql(queryObj, range, true);
		if (sql.isEmpty())
			return Collections.EMPTY_LIST;

		Transformer transformer = queryObj.getResultTransformer();
		boolean noCache = !queryObj.isCacheable() || queryObj.isSelectCustomized() || range != null;

		// 缓存命中
		List resultList = noCache ? null : getCache().load(sql.getCacheKey());
		if (resultList == null) {
			ResultSetContainer rs = new ResultSetContainer(option.cacheResultset && !option.holdResult);// 只有当非读写模式并且开启结果缓存才缓存结果集
			long parse = System.currentTimeMillis();
			selectp.processSelect(sql, this, queryObj, rs, option, option.holdResult ? 2 : 0);
			long dbselect = System.currentTimeMillis(); // 查询完成时间
			try {
				EntityMappingProvider mapping = DbUtils.getMappingProvider(queryObj);
				resultList = populateResultSet(rs.toProperResultSet(filters, transformer.getStrategy()), mapping, transformer);
				if (debugMode) {
					LogUtil.show(StringUtils.concat("Result Count:", String.valueOf(resultList.size()), "\t Time cost([ParseSQL]:",
							String.valueOf(parse - start), "ms, [DbAccess]:", String.valueOf(dbselect - parse), "ms, [Populate]:",
							String.valueOf(System.currentTimeMillis() - dbselect), "ms) max:", option.toString(), " |", getTransactionId(null)));
				}
			} finally {
				if (option.holdResult) {
					option.setRs(rs);
				} else {
					rs.close();
				}
			}

			// Jiyi modified 2014-11-4 如果查询结果为空，不缓存
			// 目的是减少CacheKey的计算，这部分计算有一定开销，权衡之下，缓存空结果意义不大。
			if (!noCache && !option.holdResult && !resultList.isEmpty()) {
				getCache().onLoad(sql.getCacheKey(), resultList, transformer.getResultClazz());
			}
		}

		// 凡是对多查询都通过分次查询来解决，填充1vsN字段
		if (transformer.isLoadVsMany() && transformer.isQueryableEntity()) {
			Map<Reference, List<AbstractRefField>> map;
			if (queryObj instanceof Query<?>) {
				// 说明由于既无自动关联，也无手动关联，此时所有字段都需要作为延迟加载字段处理
				map = DbUtils.getLazyLoadRef(transformer.getResultMeta(), option.skipReference == null ? Collections.EMPTY_LIST : option.skipReference);
			} else if (queryObj instanceof Join) {
				// 说明可能是手动关联，也可能是自动关联，还可能是自由关联。
				Join jj = (Join) queryObj;
				Query<?> root = jj.elements().get(0);// RootObject
				map = DbUtils.getLazyLoadRef(transformer.getResultMeta(), root.isCascadeViaOuterJoin() ? null : jj.getIncludedCascadeOuterJoin());
			} else {
				// 常规处理
				map = DbUtils.getLazyLoadRef(transformer.getResultMeta(), null);
			}

			for (Map.Entry<Reference, List<AbstractRefField>> entry : map.entrySet()) {
				// 如果是缓存命中的情况，依然要重新更新缓存中的级联对象
				CascadeUtil.fillOneVsManyReference(resultList, entry, filters == null ? Collections.EMPTY_MAP : filters, this);
			}
		}
		return resultList;
	}

	/**
	 * 执行原生SQL语句。 {@linkplain #selectBySql(String, Class, Object...) 什么是原生SQL}
	 * 
	 * @param sql
	 *            SQL语句
	 * @param params
	 *            参数绑定变量
	 * @return 影响的记录条数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final int executeSql(String sql, Object... params) throws SQLException {
		return selectTarget(null).executeSql(sql, params);
	}

	/**
	 * 用SQL查询出结果集 <br>
	 * <b>不支持schema重定向，不支持Sql本地化改写</b>，因此尽量用{@link #createNativeQuery(String)}或者
	 * {@link #createNativeQuery(String, Class)}
	 * 
	 * @param sql
	 *            SQL语句
	 * @param maxReturn
	 *            限制结果最多返回若干条记录设置为-1表示不限制
	 * @return 缓存的结果集，所有结果将被缓存在内存中，不会持续占用连接，也不会接收数据库中的数据变化
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final ResultSet getResultSet(String sql, int maxRows, Object... params) throws SQLException {
		return selectTarget(null).innerSelectBySql(sql, AbstractResultSetTransformer.cacheResultSet(maxRows, 0), Arrays.asList(params), null);
	}

	/**
	 * 原生SQL查询 <br/>
	 * 
	 * <h3>原生SQL</h3>
	 * 原生SQL和NativeQuery不同。凡是NativeQuery系列的方法都是对SQL进行解析和改写处理的,而原生SQ不作任何解析和改写，
	 * 直接用于数据库操作。
	 * <p>
	 * 
	 * 原生SQL中，绑定变量占位符和E-SQL不同，用一个问号表示——
	 * 
	 * <pre>
	 * <tt>select * from t_person where id=? and name like ?</tt>
	 * </pre>
	 * 
	 * 原生SQL适用于不希望进行SQL解析和改写场合，一般情况下用在SQL解析器解析不了的SQL语句上，用作规避手段。<br>
	 * 建议，在需要保证应用的可移植性的场合下，尽可能使用{@link #createNativeQuery(String, Class)}代替。
	 * 
	 * @param sql
	 *            SQL语句
	 * @param resultClz
	 *            要返回的数据类型
	 * @param params
	 *            绑定变量参数
	 * @return 查询结果
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final <T> List<T> selectBySql(String sql, Class<T> resultClz, Object... params) throws SQLException {
		return selectBySql(sql, new Transformer(resultClz), null, params);
	}

	/**
	 * 原生SQL查询 <br/>
	 * {@linkplain #selectBySql(String, Class, Object...) 什么是原生SQL}
	 * 
	 * @param sql
	 *            SQL语句
	 * @param transformer
	 *            返回的数据类型转换器
	 * @param range
	 *            限定结果集范围
	 * @param params
	 *            绑定变量的参数
	 * @return 查询结果
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see #createNativeQuery(String)
	 * @see #createNativeQuery(String,Class)
	 */
	public final <T> List<T> selectBySql(String sql, Transformer transformer, PageLimit range, Object... params) throws SQLException {
		return selectTarget(null).selectBySql(sql, transformer, range, params);
	}

	/**
	 * 原生SQL查询，返回单条记录的结果。 {@linkplain #selectBySql(String, Class, Object...)
	 * 什么是原生SQL}
	 * 
	 * @param sql
	 *            SQL语句
	 * @param returnType
	 *            返回结果类型
	 * @param params
	 *            绑定变量参数
	 * @return 查询结果对象
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final <T> T loadBySql(String sql, Class<T> returnType, Object... params) throws SQLException {
		return selectTarget(null).loadBySql(sql, returnType, params);
	}

	/**
	 * 查询符合条件的记录条数
	 * 
	 * @param obj
	 *            查询请求
	 * @return 记录条数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final int count(ConditionQuery obj) throws SQLException {
		long total = countLong(obj);
		if (total > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Result count too big, please use method 'countLong()'");
		}
		return (int) total;
	}

	/**
	 * 查询符合条件的记录条数
	 * 
	 * @param obj
	 *            查询请求
	 * @return 记录条数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final long countLong(ConditionQuery obj) throws SQLException {
		long start = System.currentTimeMillis(); // 开始时间
		long parse = 0; // 解析时间
		boolean debugMode = ORMConfig.getInstance().isDebugMode();
		if (obj instanceof Query<?>) {
			// 预处理
			Transformer t = obj.getResultTransformer();
			if (t.isLoadVsOne()) {
				obj = DbUtils.toReferenceJoinQuery((Query<?>) obj, null);
			}
		}
		Assert.notNull(obj);
		CountClause sqls = selectp.toCountSql(obj);

		parse = System.currentTimeMillis();
		long total = selectp.processCount(this, sqls);
		if (debugMode) {
			long dbAccess = System.currentTimeMillis() - parse; // 数据库查询时间
			parse = parse - start; // 解析SQL时间
			LogUtil.info(StringUtils.concat("Total Count:", String.valueOf(total), "\t Time cost([ParseSQL]:", String.valueOf(parse), "ms, [DbAccess]:",
					String.valueOf(dbAccess), "ms) |", getTransactionId(null)));
		}
		return total;
	}

	/**
	 * 查记录条数
	 * 
	 * @param obj
	 *            查询请求
	 * @param tableName
	 *            如果表名较为特殊的情况下，允许手工传入，一般情况下传入null。
	 * @return 记录条数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final int count(IQueryableEntity obj) throws SQLException {
		return count(obj.getQuery());
	}

	/**
	 * 批量删除数据。每个传入参数都是一个实体对象。可以表示多组参数。 因此这一批量删除可以按相同的SQL语句执行多组参数。并不仅仅用于删除多条记录。
	 * 
	 * @param entities
	 *            要删除的实体对象
	 * @return 实际删除记录行数
	 * @throws SQLException
	 */
	public final <T> int executeBatchDeletion(List<T> entities) throws SQLException {
		return executeBatchDeletion(entities, null);
	}

	/**
	 * 批量删除数据。每个传入参数都是一个实体对象。可以表示多组参数。 因此这一批量删除可以按相同的SQL语句执行多组参数。并不仅仅用于删除多条记录。
	 * 
	 * @param entities
	 *            要删除的实体对象。
	 * @param group
	 *            是否对传入的对象按所属表重新分组。<br>
	 *            在启用分库分表后，用户如果不确定传入的多个对象在路由计算后属于同一张表，则需打开此开关。<br>
	 *            开关开启后会对每个对象进行路由计算并重新分组操作（这一操作将损耗一定的性能）。
	 * @return 实际删除记录行数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("unchecked")
	public final <T> int executeBatchDeletion(List<T> entities, Boolean group) throws SQLException {
		if (entities == null || entities.isEmpty())
			return 0;
		T t = entities.get(0);
		if (t instanceof IQueryableEntity) {
			return executeBatchDeletion0((List<IQueryableEntity>) entities, group);
		} else {
			List<PojoWrapper> list = PojoWrapper.wrap(entities, false);
			return executeBatchDeletion0(list, group);
		}
	}

	private final <T extends IQueryableEntity> int executeBatchDeletion0(List<T> entities, Boolean group) throws SQLException {
		Batch<T> batch = this.startBatchDelete(entities.get(0), null);
		if (group != null) {
			batch.setGroupForPartitionTable(group);
		}
		return batch.execute(entities);
	}

	/**
	 * 按主键删除多条记录。适用与拥有大量主键值，需要在数据库中查询与之对应的记录时。<br>
	 * 会使用IN条件来减少操作数据库的次数。如果要删除的条件超过了500个，会自动分多次进行删除。
	 * 
	 * <p>
	 * <strong>注意1：在多库操作下，这一方法不支持对每条记录单独分组并计算路由。</strong><br>
	 * 需要路由的场景下请使用 {@link #batchDelete(List, boolean)}方法
	 * <p>
	 * <strong>注意2：不支持复合主键</strong><br>
	 * 需要复合主键的场景下请使用 {@link #batchDelete(List, boolean)}方法
	 * 
	 * @param clz
	 *            要删除的记录类型
	 * @param keys
	 *            主键列表。复合主键不支持。如需批量删除复合主键的类请用{@link #batchDelete(List, boolean)}
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final <T> int batchDelete(Class<?> clz, List<? extends Serializable> keys) throws SQLException {
		ITableMetadata meta = MetaHolder.getMeta(clz);
		return batchDelete(meta, keys);
	}

	/**
	 * 按主键删除多条记录。适用与拥有大量主键值，需要在数据库中查询与之对应的记录时。<br>
	 * 会使用IN条件来减少操作数据库的次数。如果要删除的条件超过了500个，会自动分多次进行删除。
	 * 
	 * <p>
	 * <strong>注意：在多库操作下，这一方法不支持对每条记录单独分组并计算路由。</strong>
	 * 
	 * @param meta
	 *            要删除的数据类
	 * @param pkValues
	 *            主键值列表。复合主键不支持。如需批量删除复合主键的类请用{@link #batchDelete(List)}
	 * @return 实际删除数量
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final int batchDelete(ITableMetadata meta, List<? extends Serializable> pkValues) throws SQLException {
		if (pkValues.isEmpty())
			return 0;
		if (meta.getPKFields().size() != 1) {
			throw new SQLException("Only supports [1] column as primary key, but " + meta.getSimpleName() + " has " + meta.getPKFields().size() + " columns.");
		}

		int MAX_IN_CONDITIONS = ORMConfig.getInstance().getMaxInConditions();

		if (pkValues.size() < MAX_IN_CONDITIONS) {
			return batchDeleteByPK0(meta, pkValues);
		}
		int total = 0;
		int offset = 0;
		while (pkValues.size() - offset > MAX_IN_CONDITIONS) {
			total += batchDeleteByPK0(meta, pkValues.subList(offset, offset + MAX_IN_CONDITIONS));
			offset += MAX_IN_CONDITIONS;
		}
		if (pkValues.size() > offset) {
			total += batchDeleteByPK0(meta, pkValues.subList(offset, pkValues.size()));
		}
		return total;
	}

	/**
	 * 批量删除数据(按主键)<br>
	 * <ol>
	 * <li>本方法需要传入要删除的实体对象。（对象中只要主键设置了值即可，其他字段无需设值）。</li>
	 * <li>本方法可以批量删除支持复合主键的实体。</li>
	 * </ol>
	 * 
	 * @param entities
	 *            要删除的实体对象
	 * 
	 * @return 实际删除数量
	 * @throws SQLException
	 *             如果没有主键或者数据库操作错误，抛出SQLException。
	 */
	public final <T> int batchDelete(List<T> entities) throws SQLException {
		return batchDelete(entities, false);
	}

	/**
	 * 批量删除数据(按主键)<br>
	 * <ol>
	 * <li>本方法需要传入要删除的实体对象。（对象中只要主键设置了值即可，其他字段无需设值）。</li>
	 * <li>本方法可以批量删除支持复合主键的实体。</li>
	 * <li>本方法可以支持数据路由（第二个参数传入true的场景下）</li>
	 * </ol>
	 * 
	 * @param entities
	 *            要删除的实体对象
	 * @param group
	 *            在分库分表情况下，是否对每条记录进行路由计算并重新分组。<br>
	 *            在启用分库分表后，用户如果不确定传入的多个对象在路由计算后属于同一张表，则需打开此开关。<br>
	 *            开关开启后会对每个对象进行路由计算并重新分组操作（这一操作将损耗一定的性能）。
	 * 
	 * @return 实际删除数量
	 * @throws SQLException
	 *             如果没有主键或者数据库操作错误，抛出SQLException。
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final <T> int batchDelete(List<T> entities, boolean group) throws SQLException {
		if (entities.isEmpty())
			return 0;

		ITableMetadata meta = MetaHolder.getMeta(entities.get(0));
		if (meta.getPKFields().isEmpty()) {
			throw new SQLException("The type " + meta.getTableName(false) + " has no primary key, can not execute batch remove by primarykey");
		}

		List<? extends IQueryableEntity> data;
		if (meta.getType() == EntityType.POJO) {
			data = PojoWrapper.wrap(entities, false);
		} else {
			data = (List<? extends IQueryableEntity>) entities;
		}
		long start = System.nanoTime();

		PKQuery<?> query = new PKQuery(meta, DbUtils.getPKValueSafe((IQueryableEntity) data.get(0)), meta.newInstance());
		BindSql wherePart = preProcessor.toWhereClause(query, new SqlContext(null, query), null, getProfile(null), true);
		Batch.Delete batch = new Batch.Delete(this, meta, wherePart);
		batch.parseTime = System.nanoTime() - start;
		batch.pkMpode = true;
		batch.setGroupForPartitionTable(group);
		return batch.execute(data);
	}

	/**
	 * 按某个字段值进行批量删除。
	 * 
	 * @param field
	 *            要作为删除条件的字段。
	 * @param values
	 *            需要删除的值
	 * @return 实际删除行数。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final <T extends IQueryableEntity> int batchDeleteByField(Field field, List<? extends Serializable> values) throws SQLException {
		int MAX_IN_CONDITIONS = ORMConfig.getInstance().getMaxInConditions();
		if (values.size() < MAX_IN_CONDITIONS)
			return batchDeleteByField0(field, values);

		int total = 0;
		int offset = 0;
		while (values.size() - offset > MAX_IN_CONDITIONS) {
			total += batchDeleteByField0(field, values.subList(offset, offset + MAX_IN_CONDITIONS));
			offset += MAX_IN_CONDITIONS;
		}
		if (values.size() > offset) {
			total += batchDeleteByField0(field, values.subList(offset, values.size()));
		}
		return total;
	}

	/**
	 * 获得一个Bach对象，这个batch对象上可以执行批量删除操作。
	 * 
	 * @param template
	 *            批操作的模板。传入的对象必须是一个构成delete的完整请求（含查询条件（默认为主键））。
	 *            后续的所有批量操作都按此模板執行操作。
	 * @param tableName
	 *            强制指定表名，也就是说template当中的表名无效。（传入的表名支持Schema重定向）
	 * @return Batch对象，可执行批操作
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see Batch
	 */
	public final <T extends IQueryableEntity> Batch<T> startBatchDelete(T template, String tableName) throws SQLException {
		// 位于批当中的绑定变量
		long start = System.nanoTime();
		BindSql wherePart = preProcessor.toWhereClause(template.getQuery(), new SqlContext(null, template.getQuery()), null, getProfile(null), true);
		ITableMetadata meta = MetaHolder.getMeta(template);
		Batch.Delete<T> batch = new Batch.Delete<T>(this, meta, wherePart);
		batch.forceTableName = MetaHolder.toSchemaAdjustedName(tableName);
		batch.parseTime = System.nanoTime() - start;
		return batch;
	}

	/**
	 * 执行批量插入操作。
	 * 
	 * @param entities
	 *            要插入的对象
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final <T> void batchInsert(List<T> entities) throws SQLException {
		batchInsert(entities, null, null);
	}

	/**
	 * 执行批量插入操作。
	 * 
	 * @param entities
	 *            要插入的对象
	 * @param group
	 *            是否对传入的对象按所属表重新分组。<br>
	 *            在启用分库分表后，用户如果不确定传入的多个对象在路由计算后属于同一张表，则需打开此开关。<br>
	 *            开关开启后会对每个对象进行路由计算并重新分组操作（这一操作将损耗一定的性能）。
	 * @throws SQLException
	 */
	public final <T> void batchInsert(List<T> entities, Boolean group) throws SQLException {
		batchInsert(entities, group, null);
	}

	/**
	 * 执行批量插入操作。
	 * 
	 * @param entities
	 *            要插入的对象
	 * @param group
	 *            是否对传入的对象按所属表重新分组
	 * @param dynamic
	 *            是否Dynamic模式插入，Dynamic模式下会跳过未设值的字段。<br>
	 *            某些字段在数据库中设置了defauelt value。
	 *            如果在实体中为null，那么会将null值插入数据库，造成数据库的缺省值无效。 为了使用dynamic模式后，
	 *            只有手工设置为null的属性，插入数据库时才是null。如果没有设置过值，在插入数据库时将使用数据库的默认值。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("unchecked")
	public final <T> void batchInsert(List<T> entities, Boolean doGroup, Boolean dynamic) throws SQLException {
		if (entities == null || entities.isEmpty())
			return;

		T t = entities.get(0);
		if (t instanceof IQueryableEntity) {
			batchInsert0((List<IQueryableEntity>) entities, doGroup, dynamic);
		} else {
			List<PojoWrapper> list = PojoWrapper.wrap(entities, false);
			batchInsert0(list, doGroup, dynamic);
		}
	}

	private final <T extends IQueryableEntity> void batchInsert0(List<T> entities, Boolean group, Boolean dynamic) throws SQLException {
		boolean flag = dynamic == null ? ORMConfig.getInstance().isDynamicInsert() : dynamic.booleanValue();
		Batch<T> batch = startBatchInsert(entities.get(0), null, flag, false);
		if (group != null)
			batch.setGroupForPartitionTable(group);
		batch.execute(entities);
	}

	/**
	 * 极限模式下的批量插入操作<br>
	 * extreme模式：extreme是为了性能而优化的特殊模式，该模式下数据库自增主键将不会被回写到对象中。
	 * 此外在一些特殊的数据库上会使用特定的语法来加速。<br>
	 * 比如Oracle上，会使用 / *+ APPEND * /等特殊的SQL语法来提高性能。
	 * 
	 * @param entities
	 *            要插入的对象
	 * 
	 * @param group
	 *            在分库分表情况下，是否对每条记录进行路由计算并重新分组。<br>
	 *            在启用分库分表后，用户如果不确定传入的多个对象在路由计算后属于同一张表，则需打开此开关。<br>
	 *            开关开启后会对每个对象进行路由计算并重新分组操作（这一操作将损耗一定的性能）。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public final <T> void extremeInsert(List<T> entities, Boolean group) throws SQLException {
		if (entities.isEmpty())
			return;
		T t = entities.get(0);
		Batch batch;
		if (t instanceof IQueryableEntity) {
			batch = startBatchInsert((IQueryableEntity) t, null, false, true);
			if (group != null)
				batch.setGroupForPartitionTable(group);
			batch.execute(entities);
		} else {
			List<PojoWrapper> list = PojoWrapper.wrap(entities, false);
			batch = startBatchInsert(list.get(0), null, false, true);
			if (group != null)
				batch.setGroupForPartitionTable(group);
			batch.execute(list);
		}
	}

	/**
	 * 极限模式下的批量更新操作 extreme模式：extreme是为了性能而优化的特殊模式，该模式下数据库自增主键将不会被回写到对象中。
	 * 此外在一些特殊的数据库上会使用特定的语法来加速。<br>
	 * 比如Oracle上，会使用 / *+ APPEND * /等特殊的SQL语法来提高性能。
	 * 
	 * @param entities
	 *            要插入的对象
	 * @param group
	 *            在分库分表情况下，是否对每条记录进行路由计算并重新分组。<br>
	 *            在启用分库分表后，用户如果不确定传入的多个对象在路由计算后属于同一张表，则需打开此开关。<br>
	 *            开关开启后会对每个对象进行路由计算并重新分组操作（这一操作将损耗一定的性能）。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final <T extends IQueryableEntity> void extremeUpdate(List<T> entities, Boolean group) throws SQLException {
		if (entities.isEmpty())
			return;
		Batch<T> batch = startBatchUpdate(entities.get(0), null, false, true);
		if (group != null)
			batch.setGroupForPartitionTable(group);
		batch.execute(entities);
	}

	/**
	 * 获得一个Bach对象，这个batch对象上可以执行批量插入操作。<br>
	 * 一个Batch对象就是一个已经编译好的SQL语句。用户可以传入一批参数批量执行。<br>
	 * batch对象可以反复使用，每次执行一批的参数。
	 * 
	 * @param template
	 *            批操作的模板。传入的对象是可以插入的。后续的所有批量操作都按此模板執行操作。 如果开启了
	 *            {@link JefConfiguration.Item#DB_DYNAMIC_INSERT
	 *            DB_DYNAMIC_INSERT}
	 *            功能，那么模板中插入数据库的字段就是后续任务中插入数据库的字段。后续数据库中其他字段即使赋值了也不会入库。
	 * 
	 * @param dynamic
	 *            是否跳过未赋值的字段。 强制指定表名，也就是说template当中的表名无效。（传入的表名支持Schema重定向）
	 * @return Batch操作句柄
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see Batch
	 */
	public final <T extends IQueryableEntity> Batch<T> startBatchInsert(T template, boolean dynamic) throws SQLException {
		return startBatchInsert(template, null, dynamic, false);
	}

	/**
	 * 创建一个Batch对象。<br>
	 * 一个Batch对象就是一个已经编译好的SQL语句。用户可以传入一批参数批量执行。<br>
	 * batch对象可以反复使用，每次执行一批的参数。
	 * 
	 * @param template
	 *            批操作的模板。传入的对象是可以插入的。后续的所有批量操作都按此模板執行操作。 如果开启了
	 *            {@link JefConfiguration.Item#DB_DYNAMIC_INSERT
	 *            DB_DYNAMIC_INSERT}
	 *            功能，那么模板中插入数据库的字段就是后续任务中插入数据库的字段。后续数据库中其他字段即使赋值了也不会入库。
	 * @param tableName
	 *            强制指定表名，也就是说template当中的表名无效。（传入的表名支持Schema重定向） *
	 * @param dynamic
	 *            dynamic模式：某些字段在数据库中设置了defauelt value。
	 *            如果在实体中为null，那么会将null值插入数据库，造成数据库的缺省值无效。 为了使用dynamic模式后，
	 *            只有手工设置为null的属性，插入数据库时才是null。如果没有设置过值，在插入数据库时将使用数据库的默认值。
	 * @param extreme
	 *            extreme模式：extreme是为了性能而优化的特殊模式，该模式下数据库自增主键将不会被回写到对象中。
	 *            此外在一些特殊的数据库上会使用特定的语法来加速。<br>
	 *            比如Oracle上，会使用 / *+ APPEND * /等特殊的SQL语法来提高性能。
	 * 
	 * @return Batch操作句柄
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see Batch
	 */
	public final <T extends IQueryableEntity> Batch<T> startBatchInsert(T template, String tableName, boolean dynamic, boolean extreme) throws SQLException {
		long start = System.nanoTime();
		ITableMetadata meta = MetaHolder.getMeta(template);
		Batch.Insert<T> b = new Batch.Insert<T>(this, meta);
		InsertSqlClause insertPart = insertp.toInsertSqlBatch((IQueryableEntity) template, tableName, dynamic, extreme, null);
		b.setInsertPart(insertPart);
		b.setForceTableName(tableName);
		b.extreme = extreme;
		b.parseTime = System.nanoTime() - start;
		return b;
	}

	/**
	 * 获得一个Bach对象，这个batch对象上可以执行批量更新操作。
	 * 
	 * @param template
	 *            批操作的模板。传入的对象必须是一个构成update的完整请求（包含update的字段和查询条件（默认为主键））。
	 *            后续的所有批量操作都按此模板執行操作。 <strong>注意这个模板并未加入到批任务当中</strong>
	 * @param dynamic
	 *            dynamic模式：如果开启，则只更新被修改过的字段。如果关闭则更新除了主键以外的所有字段。<br>
	 *            该参数可传入null。当传入null时，按照全局设置的db.dynamic.update参数确定是否使用动态更新。
	 *            此外，当传入对象的无任何字段被修改过时，也会更新除了主键以外的所有字段。
	 * @return Batch对象，可执行批操作
	 * @throws SQLException
	 * @see Batch
	 */
	public final <T extends IQueryableEntity> Batch<T> startBatchUpdate(T template, boolean dynamic) throws SQLException {
		return startBatchUpdate(template, null, dynamic, false);
	}

	/**
	 * 获得一个Bach对象，这个batch对象上可以执行批量更新操作。
	 * 
	 * @param template
	 *            批操作的模板。传入的对象必须是一个构成update的完整请求（包含update的字段和查询条件（默认为主键））。
	 *            后续的所有批量操作都按此模板執行操作。 <strong>注意这个模板并未加入到批任务当中</strong>
	 * @param tableName
	 *            强制指定表名，也就是说template当中的表名无效。（传入的表名支持Schema重定向）
	 * @param dynamic
	 *            dynamic模式：如果开启，则只更新被修改过的字段。如果关闭则更新除了主键以外的所有字段。<br>
	 *            该参数可传入null。当传入null时，按照全局设置的db.dynamic.update参数确定是否使用动态更新。
	 *            此外，当传入对象的无任何字段被修改过时，也会更新除了主键以外的所有字段。
	 * @param extreme
	 *            extreme模式：extreme是为了性能而优化的特殊模式，该模式下数据库自增主键将不会被回写到对象中。
	 *            此外在一些特殊的数据库上会使用特定的语法来加速。<br>
	 *            比如Oracle上，会使用 / *+ APPEND * /等特殊的SQL语法来提高性能。
	 * @return Batch对象，可执行批操作
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @see Batch
	 */
	public final <T extends IQueryableEntity> Batch<T> startBatchUpdate(T template, String tableName, boolean dynamic, boolean extreme) throws SQLException {
		if (dynamic && !template.needUpdate()) {
			throw new IllegalArgumentException("The input object is not a valid update query Template, since its update value map is empty, change to ");
		}
		long start = System.nanoTime();
		UpdateClause updatePart = updatep.toUpdateClauseBatch((IQueryableEntity) template, null, dynamic);
		// 位于批当中的绑定变量
		UpdateContext context = new UpdateContext(template.getQuery().getMeta().getVersionColumn());
		BindSql wherePart = preProcessor.toWhereClause(template.getQuery(), new SqlContext(null, template.getQuery()), context, getProfile(null), true);
		ITableMetadata meta = MetaHolder.getMeta(template);
		Batch.Update<T> batch = new Batch.Update<T>(this, meta);
		batch.forceTableName = MetaHolder.toSchemaAdjustedName(tableName);
		batch.setUpdatePart(updatePart);
		batch.setWherePart(wherePart);
		batch.extreme = extreme;
		batch.parseTime = System.nanoTime() - start;
		return batch;
	}

	/**
	 * 用传入的Query作为条件，将数据库中的记录更为为对象中的值
	 * 
	 * @param query
	 * @param entities
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> void batchUpdateBy(Query<T> query, List<T> entities) throws SQLException {

	}

	/**
	 * 批量更新
	 * 
	 * @param entities
	 *            要更新的操作请求。<br>
	 *            批量更新时第一个对象会作为整批的模板。该对象中的Query部分作为where条件。
	 *            该对象本身被修改过的值作为set部分。后续的对象仅作为操作参数使用。
	 * @return 实际修改的记录行数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final <T> void batchUpdate(List<T> entities) throws SQLException {
		batchUpdate(entities, null, null);
	}

	/**
	 * 批量更新
	 * 
	 * @param entities
	 *            要更新的操作请求。<br>
	 *            批量更新时第一个对象会作为整批的模板。该对象中的Query部分作为where条件。
	 *            该对象本身被修改过的值作为set部分。后续的对象仅作为操作参数使用。
	 * @param group
	 *            在分库分表情况下，是否对每条记录进行路由计算并重新分组。<br>
	 *            在启用分库分表后，用户如果不确定传入的多个对象在路由计算后属于同一张表，则需打开此开关。<br>
	 *            开关开启后会对每个对象进行路由计算并重新分组操作（这一操作将损耗一定的性能）。
	 * @return 实际修改的记录行数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	public final <T> int batchUpdate(List<T> entities, Boolean group) throws SQLException {
		return batchUpdate(entities, group, null);
	}

	/**
	 * 批量更新
	 * 
	 * @param entities
	 *            要更新的操作请求。<br>
	 *            批量更新时第一个对象会作为整批的模板。该对象中的Query部分作为where条件。
	 *            该对象本身被修改过的值作为set部分。后续的对象仅作为操作参数使用。
	 * @param group
	 *            在分库分表情况下，是否对每条记录进行路由计算并重新分组。<br>
	 *            在启用分库分表后，用户如果不确定传入的多个对象在路由计算后属于同一张表，则需打开此开关。<br>
	 *            开关开启后会对每个对象进行路由计算并重新分组操作（这一操作将损耗一定的性能）。
	 * @param dynamic
	 *            dynamic模式：如果开启，则只更新被修改过的字段。如果关闭则更新除了主键以外的所有字段。<br>
	 *            该参数可传入null。当传入null时，按照全局设置的db.dynamic.update参数确定是否使用动态更新。
	 *            此外，当传入对象的无任何字段被修改过时，也会更新除了主键以外的所有字段。
	 * @return 实际修改的记录行数
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 */
	@SuppressWarnings("unchecked")
	public final <T> int batchUpdate(List<T> entities, Boolean group, Boolean dynamic) throws SQLException {
		if (entities == null || entities.isEmpty())
			return 0;

		T t = entities.get(0);
		if (t instanceof IQueryableEntity) {
			return batchUpdate0((List<IQueryableEntity>) entities, group, null);
		} else {
			List<PojoWrapper> list = PojoWrapper.wrap(entities, false);
			return batchUpdate0(list, group, null);
		}
	}

	private final <T extends IQueryableEntity> int batchUpdate0(List<T> entities, Boolean group, Boolean dynamic) throws SQLException {
		if (entities.isEmpty())
			return 0;
		T template = null;
		boolean dyna;
		if (dynamic == null) {// 如果未指定时
			dyna = ORMConfig.getInstance().isDynamicUpdate();
		} else {
			dyna = dynamic.booleanValue();
		}
		for (int i = 0; i < 3 && i < entities.size(); i++) {
			template = entities.get(i);
			if (!dyna || template.needUpdate()) {
				break;
			}
		}
		if (template == null) {
			// 没有需要更新的对象
			return 0;
		}
		if (!template.needUpdate()) {
			dyna = false;
		}
		Batch<T> batch = this.startBatchUpdate(template, null, dyna, false);
		if (group != null) {
			batch.setGroupForPartitionTable(group);
		}
		return batch.execute(entities);
	}

	/**
	 * 返回数据库函数表达式
	 * 
	 * @param func
	 *            函数，在{@link DbFunction}中枚举
	 * @param params
	 *            函数的参数
	 * @return 符合数据库方言的函数表达式
	 */
	public final SqlExpression func(DbFunction func, Object... params) {
		return selectTarget(null).func(func, params);
	}

	/**
	 * 在数据库中查询得到表达式的值 <h3>Example.</h3>
	 * 
	 * <pre>
	 * <code>
	 *  //在任何数据库上获得当前时间
	 * Date d=session.getExpressionValue(session.func(Func.current_timestamp), Date.class);
	 *  //获取Sequence的下一个值
	 *  long next=session.getExpressionValue(new SqlExpression("seq_name.nextval"),Long.class);
	 * </code>
	 * </pre>
	 * 
	 * 如果要指定操作的数据源，可以先获得SqlTemplate对象，然后调用其同名方法
	 * 
	 * @param expression
	 *            SQL表达式
	 * @param clz
	 *            返回值类型
	 * @return 查询结果
	 * @throws SQLException
	 *             当数据库异常时抛出
	 * @see SqlTemplate#getExpressionValue(String, Class, Object...)
	 */
	public final <T> T getExpressionValue(String expression, Class<T> clz) throws SQLException {
		return selectTarget(null).getExpressionValue(expression.toString(), clz);
	}

	/**
	 * 在数据库中查询得到函数表达式的值 <h3>Example.</h3>
	 * 
	 * @param func
	 *            函数种类
	 * @param clz
	 *            返回结果类型
	 * @param params
	 *            函数入参
	 * @return 该函数的执行结果
	 * @throws SQLException
	 *             当数据库异常时抛出
	 * @see SqlTemplate#getExpressionValue(DbFunction, Class, Object...)
	 */
	public final <T> T getExpressionValue(DbFunction func, Class<T> clz, Object... params) throws SQLException {
		return selectTarget(null).getExpressionValue(func, clz, params);
	}

	/**
	 * 分库分表计算（数据路由）
	 * <p>
	 * 根据查询/插入/更新/删除的请求来计算其影响到的表
	 * 
	 * @param entity
	 *            要分表的对象或查询
	 * @return PartitionResult数组，数组的每个元素(PartitionResult)表示一个独立的数据库， 其名称可以用
	 *         getDatabase()获得，对于每一个数据库，可能会有多张表，用getTables()获得
	 *         如果你能确定本次操作只会操作一张表，可以用getAsOneTable()获得表名.
	 * @see PartitionResult
	 */
	public final PartitionResult[] getPartitionResults(IQueryableEntity entity) {
		return DbUtils.toTableNames(entity, null, entity.getQuery(), getPartitionSupport());
	}

	/**
	 * 得到DbClient对象，该对象上能够执行更多的DDL指令。
	 * 
	 * 
	 * @return DbClient对象
	 * @see DbClient
	 * @see Session
	 */
	public abstract DbClient getNoTransactionSession();

	/**
	 * <h3>枚举对象，描述查询结果拼装到对象的策略</h3>
	 * <ul>
	 * <li>{@link #PLAIN_MODE}<br>
	 * 强制使用PLAIN_MODE.(即使是dataobject子类，也使用Plain模式拼装)</li>
	 * <li>{@link #SKIP_COLUMN_ANNOTATION}<br>
	 * 拼装时，忽略Column名，直接使用类的Field名作为列名/li>
	 * <li>{@link #NO_RESORT}<br>
	 * 禁用内存重新排序功能</li>
	 * </ul>
	 */
	public enum PopulateStrategy {
		/**
		 * 忽略@Column注释。<br/>
		 * 
		 * 一个名为 createTime 的字段，其注解为@Column(name="CREATE_TIME")。<br>
		 * 正常情况下，标记为的字段会对应查询结果中的"CREATE_TIME"列。<br>
		 * 使用SKIP_COLUMN_ANNOTATION参数后，对应到查询结果中的createtime列。
		 * 
		 */
		SKIP_COLUMN_ANNOTATION,
		/**
		 * <b>关于PLAIN_MODE的用法</b>
		 * 正常情况下，检测到查询结果是DataObject的子类时，就会采用嵌套法(NESTED)拼装结果，比如
		 * 这种情况下将结果集作为立体结构，将不同表选出的字段作为不同的实体的属性。如:<br>
		 * <li>Person.name <-> 'T1__name'</li> <li>Person.school.id <-> 'T2__id'
		 * </li>
		 * <p>
		 * 嵌套法拼装时，会忽略没有绑定到数据库表的字段的拼装（即非数据元模型中定义的字段）。
		 * 如果我们要将结果集中的列直接按名称对应到实体上，那么就需要使用PLAIN_MODE.
		 */
		PLAIN_MODE,
		/**
		 * 当返回结果是数组时，将查出的每个列作为一个元素，用数组的形式返回
		 */
		COLUMN_TO_ARRAY
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	Iterator iterateResultSet(IResultSet rs, EntityMappingProvider mapping, Transformer transformers) throws SQLException {
		Class returnClz = transformers.getResultClazz();
		if (ArrayUtils.contains(MetadataService.SIMPLE_CLASSES, returnClz)) {
			return ResultPopulatorImpl.instance.iteratorSimple(rs, returnClz);
		}
		if (Object[].class == returnClz) {
			return ResultPopulatorImpl.instance.iteratorMultipie(rs, mapping, transformers);
		}
		if (returnClz == Var.class || returnClz == Map.class) {
			return ResultPopulatorImpl.instance.iteratorMap(rs, transformers);
		}
		if (transformers.isVarObject()) {
			return ResultPopulatorImpl.instance.iteratorNormal(this, rs, mapping, transformers);
		}
		boolean plain = ArrayUtils.contains(transformers.getStrategy(), PopulateStrategy.PLAIN_MODE)
				|| (mapping == null && !IQueryableEntity.class.isAssignableFrom(returnClz));
		if (plain) {
			return ResultPopulatorImpl.instance.iteratorPlain(rs, transformers);
		}
		return ResultPopulatorImpl.instance.iteratorNormal(this, rs, mapping, transformers);
	}

	@SuppressWarnings("unchecked")
	<T> List<T> populateResultSet(IResultSet rsw, EntityMappingProvider mapping, Transformer transformers) throws SQLException {
		Class<T> returnClz = (Class<T>) transformers.getResultClazz();

		if (returnClz == null) {// 未指定时。如果结果只有1列直接返回;如果有多列，Map返回。
			if (rsw.getColumns().length() > 1) {
				returnClz = (Class<T>) Var.class;
			} else {
				return (List<T>) ResultSets.toObjectList(rsw, 1, Integer.MAX_VALUE);
			}
		}
		// 基础类型返回
		if (ArrayUtils.fastContains(MetadataService.SIMPLE_CLASSES, returnClz)) {
			return ResultPopulatorImpl.instance.toSimpleObjects(rsw, returnClz);
		}
		// 数组返回——模式1：每张表映射成一个元素 模式2：每列映射成一个元素 模式3：自定义Mapper
		if (returnClz.isArray()) {
			return (List<T>) ResultPopulatorImpl.instance.toDataObjectMap(rsw, mapping, transformers);
		}
		// Map返回。
		if (returnClz == Var.class || returnClz == Map.class) {
			return (List<T>) ResultPopulatorImpl.instance.toVar(rsw, transformers);
		}
		// 动态表返回
		if (transformers.isVarObject()) {
			return (List<T>) ResultPopulatorImpl.instance.toJavaObject(this, rsw, mapping, transformers);
		}
		boolean plain = transformers.hasStrategy(PopulateStrategy.PLAIN_MODE) || !IQueryableEntity.class.isAssignableFrom(returnClz);
		if (plain) {
			return (List<T>) ResultPopulatorImpl.instance.toPlainJavaObject(rsw, transformers);
		}
		return (List<T>) ResultPopulatorImpl.instance.toJavaObject(this, rsw, mapping, transformers);
	}

	// 包装当前AbsDbClient,包装为缺省的操作对象即无dbkey.
	protected final OperateTarget wrapTarget(String dbName, int mustTx) throws SQLException {
		if (mustTx > 0 && this instanceof DbClient) {// 如果不是在事务中，那么就用一个内嵌事务将其包裹住，作用是在resultSet的生命周期内，该连接不会被归还。并且也预防了基于线程的连接模型中，该连接被本线程的其他SQL操作再次取用然后释放回池
			Transaction tx = new TransactionImpl((DbClient) this, TransactionFlag.ResultHolder, mustTx == 1);
			OperateTarget target = new OperateTarget(tx, dbName);
			if (target.getProfile().getName() == RDBMS.sqlite) {
				tx.setReadonly(false);// The new Driver (3.8.11) do not support
										// setReadOnly() while connection is
										// created.
			}
			return target;
		} else {
			return new OperateTarget(this, dbName);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> batchLoadByField0(Field field, List<?> values) throws SQLException {
		ITableMetadata meta = DbUtils.getTableMeta(field);
		Query<?> q = meta.newInstance().getQuery();
		q.addCondition(field, Operator.IN, values);
		return innerSelect(q, null, null, QueryOption.DEFAULT);
	}

	private int batchDeleteByField0(Field field, List<? extends Serializable> values) throws SQLException {
		ITableMetadata meta = DbUtils.getTableMeta(field);
		Query<?> q = meta.newInstance().getQuery();
		q.addCondition(field, Operator.IN, values);
		return this.delete(q);
	}

	private int batchDeleteByPK0(ITableMetadata meta, List<? extends Serializable> pkValues) throws SQLException {
		if (meta.getType() == EntityType.POJO) {
			Query<?> q = meta.newInstance().getQuery();
			q.addCondition(meta.getPKFields().get(0).field(), Operator.IN, pkValues);
			return this.delete(q);
		} else {
			Query<?> q = meta.newInstance().getQuery();
			q.addCondition(meta.getPKFields().get(0).field(), Operator.IN, pkValues);
			return this.delete(q);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List batchLoadByPK0(ITableMetadata meta, List<? extends Serializable> pkValues) throws SQLException {
		if (meta.getPKFields().size() != 1) {
			return batchLoadEachTimes(meta, pkValues);
		}
		if (meta.getType() == EntityType.POJO) {
			Query<?> q = meta.newInstance().getQuery();
			q.addCondition(meta.getPKFields().get(0).field(), Operator.IN, pkValues);
			return PojoWrapper.unwrapList(innerSelect(q, null, null, QueryOption.DEFAULT));
		} else {
			Query<?> q = meta.newInstance().getQuery();
			q.addCondition(meta.getPKFields().get(0).field(), Operator.IN, pkValues);
			return innerSelect(q, null, null, QueryOption.DEFAULT);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List batchLoadEachTimes(ITableMetadata meta, List<? extends Serializable> pkValues) throws SQLException {
		List list = new ArrayList(pkValues.size());
		for (Serializable id : pkValues) {
			list.add(load(meta, (Serializable[]) id));
		}
		return list;
	}

	// 计算手工执行的各种SQL语句下缓存刷新问题
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void checkCacheUpdate(String sql, List list) {
		if (getCache().isDummy())
			return;
		jef.database.jsqlparser.visitor.Statement st = null;
		StSqlParser parser = new StSqlParser(new StringReader(sql));
		try {
			st = parser.Statement();
		} catch (ParseException e) {
			// 解析错误就不管
		}
		if (st instanceof jef.database.jsqlparser.statement.insert.Insert) {
			getCache().process((jef.database.jsqlparser.statement.insert.Insert) st, list);
		} else if (st instanceof jef.database.jsqlparser.statement.update.Update) {
			getCache().process((jef.database.jsqlparser.statement.update.Update) st, list);
		} else if (st instanceof jef.database.jsqlparser.statement.delete.Delete) {
			getCache().process((jef.database.jsqlparser.statement.delete.Delete) st, list);
		} else if (st instanceof jef.database.jsqlparser.statement.truncate.Truncate) {
			getCache().process((jef.database.jsqlparser.statement.truncate.Truncate) st, list);
		}
	}

	protected int delete0(Query<?> query) throws SQLException {
		long start = System.currentTimeMillis();
		IQueryableEntity obj = query.getInstance();
		String myTableName = (String) query.getAttribute(Query.CUSTOM_TABLE_NAME);
		myTableName = MetaHolder.toSchemaAdjustedName(StringUtils.trimToNull(myTableName));
		PartitionResult[] sites = DbUtils.toTableNames(obj, myTableName, query, getPartitionSupport());

		if (sites != null && sites.length > 0) {
			DatabaseDialect profile = this.getProfile(sites[0].getDatabase());
			getListener().beforeDelete(obj, this);

			BindSql where = deletep.toWhereClause(query, new SqlContext(null, query), profile);
			int count = deletep.processDelete(this, obj, where, sites, start);
			if (count > 0) {
				getCache().onDelete(myTableName == null ? query.getMeta().getTableName(false) : myTableName, where.getSql(),
						CacheImpl.toParamList(where.getBind()));
			}
			getListener().afterDelete(obj, count, this);
			return count;
		} else {
			return 0;
		}
	}

	static class UpdateContext {
		VersionSupportColumn versionColumn;
		Object bean;
		private Boolean isPkQuery;

		public UpdateContext(VersionSupportColumn versionColumn) {
			this.versionColumn = versionColumn;
		}

		public boolean checkIsPKCondition() {
			return versionColumn != null && isPkQuery == null;
		}

		public void setIsPkQuery(boolean flag) {
			this.isPkQuery = flag;
		}

		public boolean needVersionCondition() {
			return versionColumn != null && isPkQuery != null && isPkQuery.booleanValue();
		}

		public void appendVersionCondition(SqlBuilder builder, SqlContext context, SqlProcessor processor, IQueryableEntity instance, DatabaseDialect profile,
				boolean batch) {
			Object value = versionColumn.getFieldAccessor().get(instance);
			if (value != null) {
				Condition cond = QB.eq(versionColumn.field(), value);
				builder.startSection(" and ");
				// FIXME 此处有误,在BatchUpdate中，应当是产生一个始终从对象取值的Variable，而不是生成一个常量条件。
				// 由Condition转换而成的SQL绑定语句中，都是常量这是不对的。
				cond.toPrepareSqlClause(builder, versionColumn.getMeta(), context, processor, instance, profile, batch);
				builder.endSection();
			}
		}
	}

	protected int update0(IQueryableEntity obj, String myTableName) throws SQLException {
		myTableName = MetaHolder.toSchemaAdjustedName(myTableName);

		Query<?> query = obj.getQuery();
		long parseCost = System.currentTimeMillis();
		PartitionResult[] sites = DbUtils.toTableNames(obj, myTableName, obj.getQuery(), getPartitionSupport());
		if (sites.length == 0) {
			return 0;
		}
		DatabaseDialect profile = getProfile(sites[0].getDatabase());

		UpdateContext context = new UpdateContext(query.getMeta().getVersionColumn());
		BindSql whereClause = updatep.toWhereClause(query, new SqlContext(null, query), context, profile);
		if (ORMConfig.getInstance().isSafeMerge() && !obj.needUpdate()) {// 重新检查一遍
			return 0;
		}

		UpdateClause updateClause = updatep.toUpdateClause(obj, sites, ORMConfig.getInstance().isDynamicUpdate());
		parseCost = System.currentTimeMillis() - parseCost;
		if (updateClause.isEmpty()) {
			return 0;
		}
		getListener().beforeUpdate(obj, this);
		int count = updatep.processUpdate(this, obj, updateClause, whereClause, sites, parseCost);
		if (count > 0) {
			String tableName = myTableName == null ? query.getMeta().getTableName(false) : myTableName;
			getCache().onUpdate(tableName, whereClause.getSql(), CacheImpl.toParamList(whereClause.getBind()));
		} else if (context.needVersionCondition()) {// 基于版本的乐观锁并发检测，记录没有成功更新
			throw new OptimisticLockException("The row in database has been modified by others after the entity was loaded.", null, obj);
		}
		getListener().afterUpdate(obj, count, this);
		return count;
	}

	protected void insert0(IQueryableEntity obj, String myTableName, boolean dynamic) throws SQLException {
		getListener().beforeInseret(obj, this);
		myTableName = MetaHolder.toSchemaAdjustedName(myTableName);

		long start = System.currentTimeMillis();
		PartitionResult pr = null;
		try {
			pr = DbUtils.toTableName(obj, myTableName, obj.hasQuery() ? obj.getQuery() : null, getPartitionSupport());
		} catch (MultipleDatabaseOperateException e) {
			// 先路由方式失败。但是还是可以继续向后走。
			// 有一种情况下，后续操作可能成功。如果以Sequence作为分库分表主键，此时由于自增值尚未就绪，分库分表失败。
			// 待SQL语句解析完成后，分库分表就能成功。
		}
		InsertSqlClause sqls = insertp.toInsertSql(obj, myTableName, dynamic, pr);
		if (sqls.getCallback() != null) {
			sqls.getCallback().callBefore(Arrays.asList(obj));
		}
		// 回调完成，此时自增主键可能已经获得，因此有机会再执行一次分库分表
		if (pr == null) {
			pr = DbUtils.toTableName(obj, myTableName, obj.hasQuery() ? obj.getQuery() : null, getPartitionSupport());
			sqls.setTableNames(pr);
		}
		long parse = System.currentTimeMillis();
		insertp.processInsert(selectTarget(sqls.getTable().getDatabase()), obj, sqls, start, parse);

		obj.clearUpdate();
		getCache().onInsert(obj, myTableName);
		getListener().afterInsert(obj, this);
	}

	/**
	 * @param minPriority
	 *            一般性级联操作的优先级是0，KV表扩展时的级联操作是5
	 */
	private int updateCascade0(IQueryableEntity obj, int minPriority) throws SQLException {
		if ((this instanceof Transaction) || getTxType() != TransactionMode.JPA) {
			return CascadeUtil.updateWithRefInTransaction(obj, this, minPriority);
		} else if (this instanceof DbClient) {
			Transaction trans = new TransactionImpl((DbClient) this, TransactionFlag.Cascade, false);
			try {
				int i = CascadeUtil.updateWithRefInTransaction(obj, trans, minPriority);
				trans.commit(true);
				return i;
			} catch (SQLException e) {
				trans.rollback(true);
				throw e;
			} catch (RuntimeException e) {
				trans.rollback(true);
				throw e;
			}
		} else {
			throw new IllegalArgumentException("unknown DbClient");
		}
	}

	private int deleteCascade0(IQueryableEntity obj, int minPriority) throws SQLException {
		if ((this instanceof Transaction) || getTxType() != TransactionMode.JPA) {
			return CascadeUtil.deleteWithRefInTransaction(obj, this, 0);
		} else if (this instanceof DbClient) {
			Transaction trans = new TransactionImpl((DbClient) this, TransactionFlag.Cascade, false);
			try {
				int i = CascadeUtil.deleteWithRefInTransaction(obj, trans, 0);
				trans.commit(true);
				return i;
			} catch (SQLException e) {
				trans.rollback(true);
				throw e;
			} catch (RuntimeException e) {
				trans.rollback(true);
				throw e;
			}
		} else {
			throw new IllegalArgumentException("unknown DbClient");
		}
	}

	private void insertCascade0(Object entity, boolean dynamic, int minPriority) throws SQLException {
		if (entity == null)
			return;
		IQueryableEntity obj;
		if (entity instanceof IQueryableEntity) {
			obj = (IQueryableEntity) entity;
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
			obj = meta.transfer(entity, false);
		}
		if ((this instanceof Transaction) || getTxType() != TransactionMode.JPA) {
			CascadeUtil.insertWithRefInTransaction(Arrays.asList(obj), this, dynamic, minPriority);
		} else if (this instanceof DbClient) {
			Transaction trans = new TransactionImpl((DbClient) this, TransactionFlag.Cascade, false);
			try {
				CascadeUtil.insertWithRefInTransaction(Arrays.asList(obj), trans, dynamic, minPriority);
				trans.commit(true);
			} catch (SQLException e) {
				trans.rollback(true);
				throw e;
			} catch (RuntimeException e) {
				trans.rollback(true);
				throw e;
			}
		} else {
			throw new IllegalArgumentException("unknown DbClient");
		}
	}

	abstract PartitionSupport getPartitionSupport();

	/**
	 * @return 得到数据库当前的时间。此方法不是去数据库查询，而是直接使用时差进行计算。
	 */
	public Date getCurrentTime() {
		return getCurrentTime(null);
	}

	/**
	 * @param dataSourceName
	 * @return 得到数据库当前的时间。此方法不是去数据库查询，而是直接使用时差进行计算。
	 */
	public Date getCurrentTime(String dataSourceName) {
		return this.getPool().getMetadata(dataSourceName).getCurrentTime();
	}

	/**
	 * QueryDSL支持，返回一个QueryDSL的查询对象，可以使用QueryDSL进行数据库操作
	 * 
	 * @param datasourceName
	 *            数据源名称
	 * @return SQLQuery
	 * @see com.mysema.query.sql.SQLQuery
	 */
	public SQLQueryFactory sqlFactory(String datasourceName) {
		return new SQLQueryFactory(this.getProfile(datasourceName).getQueryDslDialect(), PROVIDER);
	}

	private final Provider<Connection> PROVIDER = new Provider<Connection>() {
		@Override
		public Connection get() {
			try {
				return getConnection();
			} catch (SQLException e) {
				throw DbUtils.toRuntimeException(e);
			}
		}
	};

	/**
	 * QueryDSL支持，返回一个QueryDSL的查询对象，可以使用QueryDSL进行数据库操作
	 * 
	 * @return SQLQuery
	 * @see com.mysema.query.sql.SQLQuery
	 */
	public SQLQueryFactory sqlFactory() {
		return sqlFactory(null);
	}

	// ////////////////////以下全部都是废弃方法//////////////////////

	/**
	 * 废弃，使用 {@link #iteratedSelect(TypedQuery, PageLimit)} 代替
	 * 
	 * @deprecated use {@link #iteratedSelect(TypedQuery, PageLimit)} instead.
	 */
	public <T extends IQueryableEntity> ResultIterator<T> iteratedSelect(TypedQuery<T> queryObj, IntRange range) throws SQLException {
		return iteratedSelect(queryObj, PageLimit.parse(range));
	}

	/**
	 * 废弃，使用{@link #select(IQueryableEntity, PageLimit)} 代替
	 * 
	 * @param obj
	 * @param range
	 * @return
	 * @throws SQLException
	 */
	@Deprecated
	public <T extends IQueryableEntity> List<T> select(T obj, IntRange range) throws SQLException {
		return select(obj, PageLimit.parse(range));
	}

	/**
	 * 根据拼装好的Query进行查询。并将结果转换为期望的对象。
	 * 
	 * @deprecated please using
	 *             {@link #selectAs(ConditionQuery, Class, PageLimit)}
	 * @throws SQLException
	 */
	public <T> List<T> selectAs(ConditionQuery queryObj, Class<T> resultClz, IntRange range) throws SQLException {
		return selectAs(queryObj, resultClz, PageLimit.parse(range));
	}

	/**
	 * 遍历器模式查找，一般用于超大结果集的返回。
	 * {@linkplain #iteratedSelect(ConditionQuery, IntRange) 什么是结果遍历器}
	 * 注意ResultIterator对象需要释放。如果不释放，相当于数据库上打开了一个不关闭的游标，而数据库的游标数是很有限的，
	 * 耗尽后将不能执行任何数据库操作。
	 * 
	 * @param queryObj
	 *            查询条件，可以是一个普通Query,或者UnionQuery,或者Join.
	 * @param resultClz
	 *            返回结果类型
	 * @param range
	 *            限制结果返回的条数，即分页信息。（传入null表示不限制）
	 * @param strategies
	 *            结果拼装参数
	 * @return 遍历器，可以用于遍历查询结果。
	 * @throws SQLException
	 *             如果数据库操作错误，抛出。
	 * @since 1.1
	 * @see ResultIterator
	 * @deprecated use {@link #iteratedSelect(ConditionQuery, IntRange)}
	 *             instead.
	 */
	public <T> ResultIterator<T> iteratedSelect(ConditionQuery queryObj, Class<T> resultClz, IntRange range) throws SQLException {
		queryObj.getResultTransformer().setResultType(resultClz);
		return iteratedSelect(queryObj, PageLimit.parse(range));
	}
}
