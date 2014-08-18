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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.PersistenceException;

import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.Transaction.TransactionFlag;
import jef.database.annotation.PartitionResult;
import jef.database.cache.CacheImpl;
import jef.database.cache.TransactionCache;
import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.IUserManagedPool;
import jef.database.innerpool.MetadataService;
import jef.database.innerpool.ReentrantConnection;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.parser.StSqlParser;
import jef.database.meta.AbstractRefField;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.Reference;
import jef.database.query.AllTableColumns;
import jef.database.query.ConditionQuery;
import jef.database.query.EntityMappingProvider;
import jef.database.query.Join;
import jef.database.query.JoinElement;
import jef.database.query.Query;
import jef.database.query.Selects;
import jef.database.query.SqlContext;
import jef.database.query.SqlExpression;
import jef.database.query.TypedQuery;
import jef.database.support.DbOperatorListener;
import jef.database.wrapper.ResultIterator;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.CountClause;
import jef.database.wrapper.clause.InMemoryDistinct;
import jef.database.wrapper.clause.InMemoryPaging;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.populator.ResultPopulatorImpl;
import jef.database.wrapper.populator.ResultSetTransformer;
import jef.database.wrapper.populator.Transformer;
import jef.database.wrapper.result.IResultSet;
import jef.database.wrapper.result.MultipleResultSet;
import jef.database.wrapper.result.ResultSetWrapper;
import jef.database.wrapper.result.ResultSets;
import jef.script.javascript.Var;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapper;

/**
 * 描述一个事务（或无事务）的数据库操作句柄。
 * <p>
 * 该类有两种实现
 * <ul>
 * <li>Transaction 事务状态下的数据库封装，可以回滚和提交，设置SavePoint。</li>
 * <li>DbClient 非事务状态下的数据库连接，每次操作后自动提交不能回滚。但可以执行建表、删表等DDL语句。</li>
 * </ul>
 * 
 * @author Jiyi
 * @see DbClient
 * @see Transaction
 * @see #getSqlTemplate(String)
 */
public abstract class Session {
	// 这六个值在初始化的时候赋值
	protected SqlProcessor rProcessor;
	protected DbOperateProcessor p;
	protected SelectProcessor selectp;
	protected InsertProcessor insertp;
	protected InsertProcessor batchinsertp;

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
	 * @param key
	 *            数据源名称，如果单数据源的场合，可以传入null
	 * @return 指定数据源的方言
	 */
	public abstract DatabaseDialect getProfile(String key);

	/*
	 * 内部使用
	 */
	abstract IUserManagedPool getPool();

	/*
	 * 内部使用 得到数据库连接
	 */
	abstract ReentrantConnection getConnection() throws SQLException;

	/*
	 * 内部使用 释放（当前线程）连接
	 */
	abstract void releaseConnection(ReentrantConnection conn);

	/*
	 * 内部使用 得到数据库名
	 */
	abstract protected String getDbName(String dbKey);

	/*
	 * 内部使用 得到缓存
	 */
	abstract protected TransactionCache getCache();

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
	 * 获取指定数据源的操作对象
	 * 
	 * @param dbKey 数据源名称
	 * 
	 * @return 指定数据源为dbKey的数据库操作对象
	 */
	abstract protected OperateTarget asOperateTarget(String dbKey);

	/**
	 * 清理一级缓存
	 * 
	 * @param entity
	 *            要清理的数据或查询
	 */
	public void evict(IQueryableEntity entity) {
		getCache().evict(entity);
	}

	/**
	 * 清空全部的一级缓存
	 */
	public void evictAll() {
		getCache().evictAll();
	}

	/**
	 * 关闭数据库操作，或者回滚事务。<br>
	 * <ul>
	 * <li>在DbClient上调用此方法，将会关闭所有连接。</li>
	 * <li>在Transaction上调用此方法，将会关闭事务，未被提交的修改将会回滚。</li>
	 * </ul>
	 */
	public abstract void close();

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
	 * 事先将E-SQL编写在配置文件或者数据库中，运行时加载并解析，使用时按名称进行调用。这类SQL查询被称为NamedQuery。对应JPA规范当中的“命名查询”。

	 * <h3>使用示例</h3>
	 * 
	 * <pre><code>NativeQuery&lt;ResultWrapper&gt; query = db.createNamedQuery("unionQuery-1", ResultWrapper.class);
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
	 *&lt;/query&gt;</code><pre>
	 * 即使用本方法返回的NativeQuery对象上，可以执行和该SQL语句相关的各种操作。
	 *
	 * @param name           数据库中或者文件中配置的命名查询的名称
	 * @param resultWrapper  想要的查询结果包装类型
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
	 *  {@linkplain #createNamedQuery(String, Class) 什么是命名查询}
	 * 
	 * @param name
	 *            命名查询的名称
	 * @return 查询对象(NativeQuery)
	 * @see NativeQuery
	 */
	public <T> NativeQuery<T> createNamedQuery(String name) {
		return createNamedQuery(name, (Class<T>) null);
	}

	/**
	 * 返回SQL操作句柄，可以在该对象上使用SQL语句和JPQL语句操作数据库<br/>
	 * 
	 * <tt>特点：支持不同的数据源<tt>
	 * 
	 * @param dbKey
	 *            数据源名称，如果单数据源的场合，可以传入null
	 * @return SQL语句操作句柄
	 * @see SqlTemplate
	 */
	public final SqlTemplate getSqlTemplate(String dbKey) {
		return asOperateTarget(dbKey);
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
	public PartitionResult[] getPartitionResults(IQueryableEntity entity) {
		return DbUtils.toTableNames(entity, null, entity.getQuery(), getPool().getPartitionSupport());
	}

	/**
	 * 创建SQL查询（支持绑定变量）
	 * 
	 * @param sqlString
	 *            SQL语句
	 * @return 查询对象(NativeQuery)
	 * @see NativeQuery
	 */
	public NativeQuery<?> createNativeQuery(String sqlString) {
		return asOperateTarget(null).createNativeQuery(sqlString, (Class<?>) null);
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
		return asOperateTarget(null).createNativeQuery(sqlString, resultClass);
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
		return asOperateTarget(null).createNativeQuery(sqlString, resultMeta);
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
	 *            <li>
	 *            出参，单个的写作OutParam.typeOf(type)，例如OutParam.typeOf(Integer.class)
	 *            </li>
	 *            <li>
	 *            出参，以游标形式返回多个的写作OutParam.listOf(type)，例如OutParam.listOf(Entity
	 *            .class)</li>
	 *            </ul>
	 * @return 调用对象NativeCall
	 * @throws SQLException
	 * @see NativeCall
	 */
	public NativeCall createNativeCall(String procedureName, Type... paramClass) throws SQLException {
		return asOperateTarget(null).createNativeCall(procedureName, paramClass);
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
	 *            <li>
	 *            出参，单个的写作OutParam.typeOf(type)，例如OutParam.typeOf(Integer.class)
	 *            </li>
	 *            <li>
	 *            出参，以游标形式返回多个的写作OutParam.listOf(type)，例如OutParam.listOf(Entity
	 *            .class)</li>
	 *            </ul>
	 * @return 调用对象NativeCall
	 * @throws SQLException
	 * @see NativeCall
	 */
	public NativeCall createAnonymousNativeCall(String callString, Type... paramClass) throws SQLException {
		return asOperateTarget(null).createAnonymousNativeCall(callString, paramClass);
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
			return asOperateTarget(null).createQuery(jpql, null);
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
			return asOperateTarget(null).createQuery(jpql, resultClass);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * 支持级联表的更新<br>如果和其他表具有关联的关系，那么插入时会自动维护其他表中的数据，这些操作包括了Delete操作（删除子表的部分数据）
	 * 
	 * @param obj
	 *            被更新的对象
	 * @return 更新的记录数
	 * @throws SQLException
	 * @see {@link #update(IQueryableEntity)}
	 */
	public int updateCascade(IQueryableEntity obj) throws SQLException {
		if (this instanceof Transaction) {
			return CascadeUtil.updateWithRefInTransaction(obj, (Transaction) this);
		} else if (this instanceof DbClient) {
			Transaction trans = new Transaction((DbClient) this, TransactionFlag.Cascade, false);
			try {
				int i = CascadeUtil.updateWithRefInTransaction(obj, trans);
				trans.commit();
				return i;
			} catch (SQLException e) {
				trans.rollback();
				throw e;
			} catch (RuntimeException e) {
				trans.rollback();
				throw e;
			}
		} else {
			throw new IllegalArgumentException("unknown DbClient");
		}
	}

	/**
	 * 支持关联表的删除 如果和其他表具有关联的关系，那么插入时会自动维护其他表中的数据，这些操作包括了Delete操作
	 * 
	 * @param obj 删除请求的Entity对象
	 * @return 影响的记录数
	 * @throws SQLException
	 */
	public int deleteCascade(IQueryableEntity obj) throws SQLException {
		if (this instanceof Transaction) {
			return CascadeUtil.deleteWithRefInTransaction(obj, (Transaction) this);
		} else if (this instanceof DbClient) {
			Transaction trans = new Transaction((DbClient) this, TransactionFlag.Cascade,false);
			try {
				int i = CascadeUtil.deleteWithRefInTransaction(obj, trans);
				trans.commit();
				return i;
			} catch (SQLException e) {
				trans.rollback();
				throw e;
			} catch (RuntimeException e) {
				trans.rollback();
				throw e;
			}
		} else {
			throw new IllegalArgumentException("unknown DbClient");
		}
	}

	/**
	 * 支持关联表的插入 如果和其他表具有1VS1、1VSN的关系，那么插入时会自动维护其他表中的数据。这些操作包括了Insert或者update.
	 * 
	 * @param obj 插入的对象
	 * @throws SQLException
	 */
	public void insertCascade(IQueryableEntity obj) throws SQLException {
		insertCascade(obj, ORMConfig.getInstance().isDynamicInsert());
	}

	/**
	 * 支持关联表的插入 如果和其他表具有1VS1或1VSN的关系，那么插入时会自动维护其他表中的数据，这些操作包括了Insert或者update.
	 * 
	 * @param obj 插入的对象
	 * @param dynamic
	 *            智能插入模式:忽略掉没有set过的属性值
	 * @throws SQLException
	 */
	public void insertCascade(IQueryableEntity obj, boolean dynamic) throws SQLException {
		if (this instanceof Transaction) {
			CascadeUtil.insertWithRefInTransaction(obj, (Transaction) this, dynamic);
		} else if (this instanceof DbClient) {
			Transaction trans = new Transaction((DbClient) this, TransactionFlag.Cascade,false);
			try {
				CascadeUtil.insertWithRefInTransaction(obj, trans, dynamic);
				trans.commit();
			} catch (SQLException e) {
				LogUtil.exception(e);
				trans.rollback();
				throw e;
			} catch (RuntimeException e) {
				trans.rollback();
				throw e;
			}
		} else {
			throw new IllegalArgumentException("unknown DbClient");
		}
	}

	/**
	 * 插入对象。如果使用dynamic模式将会忽略掉没有set过的属性值
	 * @param obj 插入的对象。
	 * @param dynamic dynamic模式：某些字段在数据库中设置了defauelt value，此时如果在实体中为null，那么会将null值插入数据库，造成数据库的缺省值无效。
	 * 为了使用dynamic模式后，只有手工设置为null的属性，插入数据库时才是null。如果没有设置过值，在插入数据库时将使用数据库的默认值。
	 */
	public void insert(IQueryableEntity obj, boolean dynamic) throws SQLException {
		insert(obj, null, dynamic);
	}

	/**
	 * 插入对象
	 * 
	 * @param obj 插入的对象。
	 * @throws SQLException
	 */
	public void insert(IQueryableEntity obj) throws SQLException {
		insert(obj, null, ORMConfig.getInstance().isDynamicInsert());
	}

	/**
	 * 插入对象
	 * 
	 * @param obj
	 *            要插入的对象
	 * @param myTableName
	 *            自定义表名称，一旦自定义了表名，将直接使用此表；不再计算表名
	 * @throws SQLException
	 * 
	 */
	public void insert(IQueryableEntity obj, String myTableName) throws SQLException {
		insert(obj, myTableName, ORMConfig.getInstance().isDynamicInsert());
	}

	/**
	 * 插入对象
	 * 
	 * @param obj
	 *            要插入的对象
	 * @param myTableName
	 *            自定义表名称，一旦自定义了表名，将直接使用此表；不再计算表名(支持Schema重定向)
	 * @param smart
	 *            智能模式，当开启后自动忽略掉那些没有set过的property
	 * @throws SQLException
	 */
	public void insert(IQueryableEntity obj, String myTableName, boolean dynamic) throws SQLException {
		getListener().beforeInseret(obj, this);
		myTableName = MetaHolder.toSchemaAdjustedName(myTableName);

		long start = System.currentTimeMillis();
		InsertSqlClause sqls = insertp.toInsertSql(obj, myTableName, dynamic, false);
		if(sqls.getCallback()!=null){
			sqls.getCallback().callBefore(Arrays.asList(obj));
		}
		sqls.setTableNames(DbUtils.toTableName(obj, myTableName, obj.hasQuery() ? obj.getQuery() : null, getPool().getPartitionSupport()));
		
		long parse = System.currentTimeMillis();
		insertp.processInsert(asOperateTarget(sqls.getTable().getDatabase()), obj, sqls, start, parse);

		obj.clearUpdate();
		getCache().onInsert(obj);
		getListener().afterInsert(obj, this);
	}


	/**
	 * 删除对象
	 * @param clz  类型
	 * @param keys  主键的值。
	 * @return
	 * @throws SQLException
	 */
	public <T> int delete(Class<T> entityClass, Serializable... keys) throws SQLException {
		try {
			Object obj = entityClass.newInstance();
			if(obj instanceof IQueryableEntity){
				IQueryableEntity data=(IQueryableEntity)obj;
				DbUtils.setPrimaryKeyValue(data, keys);
				return delete(data);	
			}else{
				ITableMetadata meta=MetaHolder.getMeta(entityClass);
				PojoWrapper data=meta.transfer(obj,false);
				DbUtils.setPrimaryKeyValue(data, keys);
				return delete(data); 	
			}
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	
	/**
	 * 删除对象
	 * 
	 * @param obj 删除请求
	 * @return 影响的记录数
	 * @throws SQLException
	 */
	public int delete(IQueryableEntity obj) throws SQLException {
		long start = System.currentTimeMillis();
		String myTableName = (String) obj.getQuery().getAttribute(Query.CUSTOM_TABLE_NAME);
		myTableName = MetaHolder.toSchemaAdjustedName(StringUtils.trimToNull(myTableName));
		PartitionResult[] sites = DbUtils.toTableNames(obj, myTableName, obj.getQuery(), getPool().getPartitionSupport());
		
		getListener().beforeDelete(obj, this);
		int count = 0;
		if (getProfile().has(Feature.NO_BIND_FOR_DELETE)) {// 非绑定删除
			String where = rProcessor.toWhereClause(obj.getQuery(), new SqlContext(null, obj.getQuery()), false);
			for (PartitionResult site : sites) {
				count += p.processDeleteNormal(asOperateTarget(site.getDatabase()), obj, site, start, where);
			}
			if (count > 0) {
				getCache().onDelete(myTableName == null ? obj.getClass().getName() : myTableName, where, null);
			}
		} else {
			BindSql where = rProcessor.toPrepareWhereSql(obj.getQuery(), new SqlContext(null,obj.getQuery()), false);
			for (PartitionResult site : sites) {
				count += p.processDeletePrepared(asOperateTarget(site.getDatabase()), obj, site, start, where);
			}
			if (count > 0) {
				getCache().onDelete(myTableName == null ? obj.getClass().getName() : myTableName, where.getSql(), CacheImpl.toParamList(where.getBind()));
			}
		}
		getListener().afterDelete(obj, count,this);
		return count;
		
	}

	/**
	 * 根据一个Query条件删除数据
	 * 
	 * @param query  删除请求
	 * @return 影响的记录数
	 * @throws SQLException
	 */
	public int delete(Query<?> query) throws SQLException {
		return delete(query.getInstance());
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
	public int update(IQueryableEntity obj) throws SQLException {
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
	 * @see #update(IQueryableEntity)
	 */
	public int update(IQueryableEntity obj, String myTableName) throws SQLException {
		if (!obj.needUpdate()) {
			if (ORMConfig.getInstance().isDebugMode())
				LogUtil.show(obj.getClass().getSimpleName().concat(" Update canceled..."));
			return 0;
		}
		getListener().beforeUpdate(obj, this);
		int n;
		myTableName = MetaHolder.toSchemaAdjustedName(myTableName);
		if (getProfile().has(Feature.NO_BIND_FOR_UPDATE)) {
			n = innerUpdateNormal(obj, myTableName);
		} else {
			n = innerUpdatePrepared(obj, myTableName);
		}
		getListener().afterUpdate(obj, n, this);
		return n;
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
	 * 当需要指定结果范围（分页）时 {@link #select(IQueryableEntity, IntRange)}
	 * 
	 * 
	 * 
	 * @param <T>
	 *            泛型： 被查询的数据类型
	 * @param obj
	 *            被查询的对象（可携带{@link Query}以表示复杂的查询）
	 * @return 查询结果列表。不会返回null.
	 * @throws SQLException
	 * @see Query
	 */
	public <T extends IQueryableEntity> List<T> select(T obj) throws SQLException {
		return select(obj, null);
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
	 */
	public <T> List<T> select(TypedQuery<T> queryObj) throws SQLException {
		return select(queryObj, null);
	}

	/**
	 * 根据指定的条件查询结果，带分页返回
	 * 
	 * @param obj
	 *            查询对象，
	 * @param range
	 *            范围，含头含尾的区间，比如new IntRange(1,10)表示从第1条到第10条。
	 * @return 查询结果
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public <T extends IQueryableEntity> List<T> select(T obj, IntRange range) throws SQLException {
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
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> select(ConditionQuery queryObj, IntRange range) throws SQLException {
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
	 * 根据拼装好的Query进行查询。
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
	 * 
	 * @see {@link #select(ConditionQuery, IntRange)}
	 * 
	 *after calling {@code queryObj.getResultTransformer().setResultType(resultClass)} then use {@link #select(ConditionQuery, IntRange)} instead.
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> selectAs(ConditionQuery queryObj, Class<T> resultClz, IntRange range) throws SQLException {
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
	 * 查询并用指定的结果返回
	 * 
	 * @param obj
	 *            查询
	 * @param resultType
	 *            每条记录将被转换为指定的类型 
	 * @return 查询结果
	 * @throws SQLException
	 * @see {@link ConditionQuery#getResultTransformer()} and {@link Transformer#setResultType(Class)}
	 */
	public <T> List<T> selectAs(ConditionQuery obj, Class<T> resultType) throws SQLException {
		return selectAs(obj, resultType, null);
	}

	/**
	 * 遍历器模式查找，一般用于超大结果集的返回。
	 * <h3>作用</h3> 当结果集超大时，如果用List<T>返回，内存占用很大甚至会溢出。<br>
	 * JDBC设计时考虑到这个问题，因此其返回的ResultSet对象只是查询结果视图的一段，用户向后滚动结果集时，数据库才将需要的数据传到客户端。
	 * 如果客户端不缓存整个结果集，那么前面已经滚动过的结果数据就被释放。<p>
	 * 这种处理方式实际上是一种流式处理模型，iteratedSelect就是这种模型的封装。<br>
	 * iteratedSelect并不会将查询出的所有数据放置到一个List对象中（这常常导致内存溢出）。而是返回一个Iterator对象，用户不停的调用next方法向后滚动，
	 * 同时释放掉之前处理过的结果对象。这就避免了超大结果返回时内存溢出的问题。
	 * 
	 * 
	 * <h3>注意事项</h3> 由于 ResultIterator
	 * 对象中有尚未关闭的ResultSet对象，因此必须确保使用完后关闭ResultIteratpr.如下示例
	 * <pre><tt>ResultIterator<TestEntity> iter = db.iteratedSelect(QB.create(TestEntity.class), null);
	 * try{
	 * for(; iter.hasNext();) {
	 * 	iter.next();
	 * 	//do something.
	 *  }	
	 * }finally{
	 *  //必须在finally块中关闭。否则一旦业务逻辑抛出异常，则ResultIterator未释放造成游标泄露.
	 *   iter.close(); 
	 * }</tt></pre>如果ResultSet不释放，相当于数据库上打开了一个不关闭的游标，而数据库的游标数是很有限的，耗尽后将不能执行任何数据库操作。<br>
	 * 
	 * @param queryObj
	 *            查询条件,可以是一个普通的Query,也可以是一个UnionQuery
	 * @param range
	 *            限制结果返回的条数，即分页信息。（传入null表示不限制）
	 * @param strategies
	 *            结果拼装参数
	 * @return 遍历器，可以用于遍历查询结果。
	 * @throws SQLException
	 * @see ResultIterator
	 */
	public <T extends IQueryableEntity> ResultIterator<T> iteratedSelect(TypedQuery<T> queryObj, IntRange range) throws SQLException {
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
	 * 遍历器模式查找，一般用于超大结果集的返回。 {@linkplain #iteratedSelect(ConditionQuery, IntRange) 什么是结果遍历器}
	 * 注意ResultIterator对象需要释放。如果不释放，相当于数据库上打开了一个不关闭的游标，而数据库的游标数是很有限的，耗尽后将不能执行任何数据库操作。
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
	 * @since 1.1
	 * @see ResultIterator
	 * @deprecated use {@link #iteratedSelect(ConditionQuery, IntRange)} instead.
	 */
	public <T> ResultIterator<T> iteratedSelect(ConditionQuery queryObj, Class<T> resultClz, IntRange range) throws SQLException {
		queryObj.getResultTransformer().setResultType(resultClz);
		return iteratedSelect(queryObj, range);
	}
	
	
	/**
	 * 遍历器模式查找，一般用于超大结果集的返回。
	 * <h3>作用</h3> 当结果集超大时，如果用List<T>返回，内存占用很大甚至会溢出。<br>
	 * JDBC设计时考虑到这个问题，因此其返回的ResultSet对象只是查询结果视图的一段，用户向后滚动结果集时，数据库才将需要的数据传到客户端。
	 * 如果客户端不缓存整个结果集，那么前面已经滚动过的结果数据就被释放。<p>
	 * 这种处理方式实际上是一种流式处理模型，iteratedSelect就是这种模型的封装。<br>
	 * iteratedSelect并不会将查询出的所有数据放置到一个List对象中（这常常导致内存溢出）。而是返回一个Iterator对象，用户不停的调用next方法向后滚动，
	 * 同时释放掉之前处理过的结果对象。这就避免了超大结果返回时内存溢出的问题。
	 * 
	 * 
	 * <h3>注意事项</h3> 由于 ResultIterator
	 * 对象中有尚未关闭的ResultSet对象，因此必须确保使用完后关闭ResultIteratpr.如下示例
	 * <pre><tt>ResultIterator<TestEntity> iter = db.iteratedSelect(QB.create(TestEntity.class), null);
	 * try{
	 * for(; iter.hasNext();) {
	 * 	iter.next();
	 * 	//do something.
	 *  }	
	 * }finally{
	 *  //必须在finally块中关闭。否则一旦业务逻辑抛出异常，则ResultIterator未释放造成游标泄露.
	 *   iter.close(); 
	 * }</tt></pre>如果ResultSet不释放，相当于数据库上打开了一个不关闭的游标，而数据库的游标数是很有限的，耗尽后将不能执行任何数据库操作。<br>
	 * 
	 * 
	 * @param queryObj
	 *            查询条件，可以是一个普通Query,或者UnionQuery,或者Join.
	 * @param range
	 *            限制结果返回的条数，即分页信息。（传入null表示不限制）
	 * @param strategies
	 *            结果拼装参数
	 * @return 遍历器，可以用于遍历查询结果。
	 * @throws SQLException
	 * @since 1.2
	 * @see ResultIterator
	 */
	public <T> ResultIterator<T> iteratedSelect(ConditionQuery queryObj,IntRange range) throws SQLException {
		QueryOption option;
		if (queryObj instanceof JoinElement) {
			option = QueryOption.createFrom((JoinElement) queryObj);
		} else {
			option = QueryOption.DEFAULT;
		}
		return innerIteratedSelect(queryObj, range, option);
	}

	/**
	 * 使用指定的查询对象查询，返回结果遍历器。
	 * {@linkplain #iteratedSelect(ConditionQuery, IntRange) 什么是结果遍历器}
	 * 
	 * @param obj 查询请求
	 * @param range 查询对象范围
	 * @return 结果遍历器(ResultIterator)
	 * @throws SQLException
	 * @see {@link ResultIterator}
	 */
	public <T extends IQueryableEntity> ResultIterator<T> iteratedSelect(T obj,IntRange range) throws SQLException {
		@SuppressWarnings("unchecked")
		Query<T> query = obj.getQuery();
		QueryOption option = QueryOption.createFrom(query);

		// 预处理
		Transformer t = query.getResultTransformer();
		if (!t.isLoadVsOne() || query.getMeta().getRefFieldsByName().isEmpty()) {
			return innerIteratedSelect(query, range, option);
		}
		// 拼装出带连接的查询请求
		JoinElement q = DbUtils.toReferenceJoinQuery(query, null);
		return innerIteratedSelect(q, range, option);
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
	 * 返回一个可以更新操作的结果数据集合 实质对用JDBC中ResultSet的updateRow,deleteRow,insertRow等方法，<br>
	 * 该操作模型需要持有ResultSet对象，因此注意使用完毕后要close()方法关闭结果集<br>
	 * 
	 * RecordsHolder可以对选择出来结果集进行更新、删除、新增三种操作，操作完成后调用commit方法<br>
	 * 
	 * @param obj
	 * @return RecordsHolder对象，这是一个可供操作的数据库结果集句柄。注意使用完后一定要关闭。
	 * @throws SQLException
	 * @see RecordsHolder
	 */
	@SuppressWarnings("unchecked")
	public <T extends IQueryableEntity> RecordsHolder<T> selectForUpdate(T query) throws SQLException {
		Assert.notNull(query);
		return selectForUpdate(query.getQuery(), null);
	}

	/**
	 * 返回一个可以更新操作的结果数据集合——RecordsHolder，可以在这个RecordsHolder上直接针对单表添加记录、删除记录、修改记录。
	 * RecordsHolder是JDBC ResultSet的封装。目的是使用ResultSet上的updateRow,deleteRow,insertRow等方法直接在JDBC数据集上对数据库进行写操作。
	 * 
	 * 类似于PLSQL Developer中的select for update操作。select for update会锁定查询出来的记录。
	 *  
	 * 
	 * @param query
	 * @param range
	 * @return RecordsHolder对象，这是一个可供操作的数据库结果集句柄。注意使用完后一定要关闭。
	 * @throws SQLException
	 * @see RecordsHolder
	 */
	public <T extends IQueryableEntity> RecordsHolder<T> selectForUpdate(Query<T> query, IntRange range) throws SQLException {
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
		MultipleResultSet rawrs = option.getRs();
		if (rawrs.size() > 1) {
			throw new UnsupportedOperationException("select from update operate can only support one table.");
		}
		IResultSet rset = option.getRs().toSimple(null);
		result.init((ResultSetWrapper) rset, objs, rset.getProfile());
		return result;
	}

	/**
	 * 查出单个对象
	 * 
	 * @param <T>
	 * @param obj
	 * @return 使用传入的对象进行查询，结果返回记录的第一条。
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public <T extends IQueryableEntity> T load(T obj) throws SQLException {
		Assert.notNull(obj);
		Query<T> q = (Query<T>) obj.getQuery();
		QueryOption option = QueryOption.createMax1Option(q);

		List<T> l = typedSelect(q, null, option);
		if (l.isEmpty())
			return null;
		return l.get(0);
	}
	
	/**
	 * 查询并指定返回结果。
	 * @param queryObj 查询
	 * @param resultClz 单条记录返回结果类型
 
	 * @return 查询结果将只返回第一条。如果查询结果数量为0，那么将返回null
	 * 
	 * @throws SQLException
	 */
	public <T> T loadAs(ConditionQuery queryObj,Class<T> resultClz) throws SQLException {
		queryObj.getResultTransformer().setResultType(resultClz);
		return load(queryObj);
	}

	/**
	 * 查询并指定返回结果。
	 * @param queryObj 查询
 
	 * @return 查询结果将只返回第一条。如果查询结果数量为0，那么将返回null
	 * 
	 * @throws SQLException
	 */
	public <T> T load(ConditionQuery queryObj) throws SQLException {
		Assert.notNull(queryObj);
		QueryOption option;
		Map<Reference, List<Condition>> filters = null;
		
		if (queryObj instanceof JoinElement) {
			if(queryObj instanceof Query<?>){
				Query<?> qq=(Query<?>)queryObj;
				Transformer t = queryObj.getResultTransformer();
				if (t.isLoadVsOne() && !qq.getMeta().getRefFieldsByName().isEmpty()) {
					queryObj= DbUtils.toReferenceJoinQuery(qq, null);
				}
				filters=qq.getFilterCondition();
			}
			option = QueryOption.createFrom((JoinElement) queryObj);
		} else {
			option = QueryOption.DEFAULT_MAX1;
		}
		@SuppressWarnings("unchecked")
		List<T> l = innerSelect(queryObj, null, filters, option);
		if (l.isEmpty())
			return null;
		T result = l.get(0);
		if (ORMConfig.getInstance().isDebugMode()) {
			LogUtil.show("Result:" + result);
		}
		return result;
	}
	
	/**
	 * 按主键获取一条记录
	 * @param clz  类型
	 * @param keys  主键的值。 
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public <T> T load(Class<T> entityClass, Serializable... keys) throws SQLException {
		try {
			Object obj = entityClass.newInstance();
			if(obj instanceof IQueryableEntity){
				IQueryableEntity data=(IQueryableEntity)obj;
				
				DbUtils.setPrimaryKeyValue(data, keys);
				return (T)load(data);	
			}else{
				ITableMetadata meta=MetaHolder.getMeta(entityClass);
				PojoWrapper data=meta.transfer(obj,false);
				DbUtils.setPrimaryKeyValue(data, keys);
				data=load(data);
				return data==null?null:(T)data.get(); 	
			}
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 查询某个对象表中的所有数据
	 * 
	 * @param <T>
	 * @param cls
	 * @return 该类型所对应的表中的所有数据
	 * @throws SQLException
	 */
	public <T extends IQueryableEntity> List<T> selectAll(Class<T> cls) throws SQLException {
		return selectAll(MetaHolder.getMeta(cls));
	}

	/**
	 * 查询某个模型的表中的所有数据
	 * @param meta
	 * @return 该类型所对应的表中的所有数据
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public <T extends IQueryableEntity> List<T> selectAll(ITableMetadata meta) throws SQLException {
		return (List<T>) select(QB.create(meta));
	}

	/**
	 * 允许所有条件的分页查询
	 * 
	 * @param <T>
	 * @param pageSize
	 * @param elements
	 * @return
	 * @throws SQLException
	 * 
	 * please use
	 *             {@link #jef.database.Session.pageSelect(ConditionQuery, int)}<br>
	 *             and
	 *             <tt> query.getResultTransformer().setResultType(type) to assign return type.</tt>
	 * @see
	 */
	public <T> PagingIterator<T> pageSelect(ConditionQuery query, Class<T> resultWrapper, int pageSize) throws SQLException {
		query.getResultTransformer().setResultType(resultWrapper);
		return new PagingIteratorObjImpl<T>(query, pageSize, this);// 因为query包含了路由信息，所以可以允许直接传入session
	}

	/**
	 * 按自定义的条件查询
	 * 
	 * @param query
	 *            查询请求
	 * @param pageSize
	 *            分页
	 * @return
	 * @throws SQLException
	 */
	public <T> PagingIterator<T> pageSelect(ConditionQuery query, int pageSize) throws SQLException {
		return new PagingIteratorObjImpl<T>(query, pageSize, this);// 因为query包含了路由信息，所以可以允许直接传入session
	}

	/**
	 * 将传入的SQL语句创建为NativeQuery，然后再以此进行分页查询
	 * 
	 * @param sql E-SQL语句
	 * @param resultClass 返回结果类型
	 * @param pageSize     每页大小
	 * @return  PagingIterator对象
	 * @throws SQLException
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
	 * @param sql E-SQL语句
	 * @param meta 返回结果类型
	 * @param pageSize 每页大小
	 * @return PagingIterator对象
	 * @throws SQLException
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
	 * @param pageSize
	 * @return
	 * @throws SQLException
	 */
	public final <T> PagingIterator<T> pageSelect(NativeQuery<T> sql, int pageSize) throws SQLException {
		PagingIterator<T> result = new PagingIteratorNativeQImpl<T>(sql, pageSize);
		return result;
	}

	/**
	 * 根据查询对象和表名实现分页查询
	 * 
	 * @param <T>
	 * @param obj
	 * @param pageSize
	 * @return
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> PagingIterator<T> pageSelect(T obj, int pageSize) throws SQLException {
		PagingIterator<T> result = new PagingIteratorObjImpl<T>(obj, pageSize, this);
		return result;
	}

	/*
	 * 查询实现，解析查询对象，将单表对象解析为Join查询
	 */
	@SuppressWarnings("unchecked")
	protected final <T extends IQueryableEntity> List<T> typedSelect(Query<T> queryObj, IntRange range, QueryOption option) throws SQLException {
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
	final <T> ResultIterator<T> innerIteratedSelect(ConditionQuery queryObj, IntRange range, QueryOption option) throws SQLException {
		if (range != null && range.size() <= 0) {
			return new ResultIterator.Impl<T>(new ArrayList<T>().iterator(), null);
		}

		long start = System.currentTimeMillis();// 开始时间
		QueryClause sql = selectp.toQuerySql(queryObj, range, option.tableName,true);
		if(sql.isEmpty())
			return new ResultIterator.Impl<T>(new ArrayList<T>().iterator(), null);
		

		ResultIterator<T> result;
		MultipleResultSet rs = new MultipleResultSet(false, ORMConfig.getInstance().isDebugMode());
		long parse = System.currentTimeMillis();
		if (sql.getTables() == null) {// 没有分表结果，采用当前连接的默认表名操作
			OperateTarget trans = wrapThisWithEmptyKey(rs, true);
			selectp.processSelect(trans, sql,null, queryObj, rs, option);
		} else {
			for (PartitionResult site : sql.getTables()) {
				selectp.processSelect(asOperateTarget(site.getDatabase()), sql,site, queryObj, rs, option);
			}
			if(sql.isMultiDatabase()){
				if (sql.getOrderbyPart().isNotEmpty()){
					rs.setInMemoryOrder(sql.getOrderbyPart().parseAsSelectOrder(sql.getSelectPart(),rs.getColumns()));
				}
				if(sql.getGrouphavingPart().isNotEmpty()){
					rs.setInMemoryGroups(sql.getGrouphavingPart().parseSelectFunction(sql.getSelectPart()));
				}
			}
		}
		long dbselect = System.currentTimeMillis();
		LogUtil.show(StringUtils.concat("Result: Iterator", "\t Time cost([ParseSQL]:", String.valueOf(parse - start), "ms, [DbAccess]:", String.valueOf(dbselect - parse), "ms) |", getTransactionId(null)));
		EntityMappingProvider mapping = DbUtils.getMappingProvider(queryObj);
		Transformer transformer= queryObj.getResultTransformer();
		IResultSet irs=rs.toSimple(null, transformer.getStrategy());
		result = new ResultIterator.Impl<T>(iterateResultSet(irs, mapping,transformer), irs);
		return result;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	final List innerSelect(ConditionQuery queryObj, IntRange range, Map<Reference, List<Condition>> filters, QueryOption option) throws SQLException {
		boolean debugMode=ORMConfig.getInstance().isDebugMode();
		if (range != null && range.size() <= 0) {
			if (debugMode)
				LogUtil.show("Query has limit to no range. return empty list. " + range);
			return Collections.EMPTY_LIST;
		}

		long start = System.currentTimeMillis();// 开始时间
		// 生成 SQL
		QueryClause sql = selectp.toQuerySql(queryObj, range, option.tableName,true);
		if(sql.isEmpty())
			return Collections.EMPTY_LIST;
		//缓存命中
		List cachedResult = getCache().load(sql.getCacheKey());
		if (cachedResult != null)
			return cachedResult;
		
		MultipleResultSet rs = new MultipleResultSet(option.cacheResultset && !option.holdResult, debugMode);// 只有当非读写模式并且开启结果缓存才缓存结果集
		long parse = System.currentTimeMillis();
		if (sql.getTables()==null) {// 没有分表结果，采用当前连接的默认表名操作
			OperateTarget trans = wrapThisWithEmptyKey(rs, option.holdResult); // 如果是结果集持有的，那么必须在事务中
			selectp.processSelect(trans, sql, null,queryObj, rs, option);
		} else {
			for (PartitionResult site : sql.getTables()) {
				selectp.processSelect(asOperateTarget(site.getDatabase()), sql,site, queryObj, rs, option);
			}
			if(sql.isMultiDatabase()){// 最复杂的情况，多数据库下的排序
				if (sql.getOrderbyPart().isNotEmpty()) {
					rs.setInMemoryOrder(sql.getOrderbyPart().parseAsSelectOrder(sql.getSelectPart(),rs.getColumns()));
				}
				if(sql.getGrouphavingPart().isNotEmpty()){
					rs.setInMemoryGroups(sql.getGrouphavingPart().parseSelectFunction(sql.getSelectPart()));
				}
				if(sql.isDistinct()){
					rs.setInMemoryDistinct(InMemoryDistinct.instance);
				}
				if(range!=null){
					rs.setInMemoryPage(new InMemoryPaging(range));
				}
			}
		}
		long dbselect = System.currentTimeMillis(); // 查询完成时间
		List list;

		Transformer transformer = queryObj.getResultTransformer();
		try {
			EntityMappingProvider mapping = DbUtils.getMappingProvider(queryObj);
			list = populateResultSet(rs.toSimple(filters,transformer.getStrategy()), mapping, transformer);
			if (debugMode) {
				LogUtil.show(StringUtils.concat("Result Count:", String.valueOf(list.size()), "\t Time cost([ParseSQL]:", String.valueOf(parse - start), "ms, [DbAccess]:", String.valueOf(dbselect - parse), "ms, [Populate]:", String.valueOf(System.currentTimeMillis() - dbselect),
						"ms) max:", option.toString(), " |", getTransactionId(null)));
			}
		} finally {
			if (option.holdResult) {
				option.setRs(rs);
			} else {
				rs.close();
			}
		}
		if (!option.holdResult) {
			getCache().onLoad(sql.getCacheKey(), list, transformer.getResultClazz());
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
				CascadeUtil.fillOneVsManyReference(list, entry, filters == null ? Collections.EMPTY_MAP : filters,this);
			}
		}
		return list;
	}
	
	

	/**
	 * 执行原生SQL语句。 {@linkplain #selectBySql(String, Class, Object...) 什么是原生SQL}
	 * 
	 * @param sql    SQL语句
	 * @param params 参数绑定变量
	 * @return 影响的记录条数
	 * @throws SQLException
	 */
	public final int executeSql(String sql, Object... params) throws SQLException {
		return asOperateTarget(null).innerExecuteSql(sql, Arrays.asList(params));
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
	 */
	public final ResultSet getResultSet(String sql, int maxReturn, Object... params) throws SQLException {
		return asOperateTarget(null).innerSelectBySql(sql, ResultSetTransformer.CACHED_RESULTSET, maxReturn, 0, Arrays.asList(params));
	}

	/**
	 * 原生SQL查询 <br/>
	 * 	
	 * <h3>原生SQL</h3>
	 * 原生SQL和NativeQuery不同。凡是NativeQuery系列的方法都是对SQL进行解析和改写处理的,而原生SQ不作任何解析和改写，直接用于数据库操作。<p>
	 * 
	 * 原生SQL中，绑定变量占位符和E-SQL不同，用一个问号表示——<pre><tt>select * from t_person where id=? and name like ?</tt></pre>
	 * 
	 * 原生SQL适用于不希望进行SQL解析和改写场合，一般情况下用在SQL解析器解析不了的SQL语句上，用作规避手段。<br>
	 * 建议，在需要保证应用的可移植性的场合下，尽可能使用{@link #createNativeQuery(String, Class)}代替。
	 * 
	 * @param sql SQL语句
	 * @param resultClz
	 *            要返回的数据类型
	 * @param params
	 *            绑定变量参数
	 * @return 查询结果
	 * @throws SQLException
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
	 * @param range  限定结果集范围
	 * @param params 绑定变量的参数
	 * @return 查询结果
	 * @throws SQLException
	 * @see #createNativeQuery(String)
	 * @see #createNativeQuery(String,Class)
	 */
	public final <T> List<T> selectBySql(String sql, Transformer transformer, IntRange range, Object... params) throws SQLException {
		return asOperateTarget(null).selectBySql(sql, transformer, range, params);
	}

	/**
	 * 原生SQL查询，返回单条记录的结果。
	 * {@linkplain #selectBySql(String, Class, Object...) 什么是原生SQL}
	 *             
	 * @param sql SQL语句
	 * @param returnType   返回结果类型
	 * @param params 绑定变量参数
	 * @return 查询结果对象
	 * @throws SQLException
	 */
	public final <T> T loadBySql(String sql, Class<T> returnType, Object... params) throws SQLException {
		return asOperateTarget(null).loadBySql(sql, returnType, params);
	}

	/**
	 * 查询符合条件的记录条数
	 * 
	 * @param obj 查询请求
	 * @return 记录条数
	 * @throws SQLException
	 */
	public final int count(ConditionQuery obj) throws SQLException {
		if(obj instanceof Query<?>){
			Query<?> q=(Query<?>)obj;
			String s=(String)q.getAttribute("_table_name");
			if(StringUtils.isNotEmpty(s)){
				return count(obj,s);
			}
		}
		return count(obj, null);
	}

	/**
	 * 查记录条数
	 * 
	 * @param obj       查询请求
	 * @param tableName 如果表名较为特殊的情况下，允许手工传入，一般情况下传入null。
	 * @return          记录条数
	 * @throws SQLException
	 */
	public final int count(IQueryableEntity obj, String tableName) throws SQLException {
		return count(obj.getQuery(), tableName);
	}

	/**
	 * 查询符合条件的记录条数
	 * 
	 * @param obj
	 * @param myTableName
	 *            支持Schema重定向
	 * @return
	 * @throws SQLException
	 */
	private int count(ConditionQuery obj, String myTableName) throws SQLException {
		long start = System.currentTimeMillis(); // 开始时间
		long parse = 0; // 解析时间
		boolean debugMode=ORMConfig.getInstance().isDebugMode();
		if (obj instanceof Query<?>) {
			// 预处理
			Transformer t = obj.getResultTransformer();
			if (t.isLoadVsOne()) {
				obj = DbUtils.toReferenceJoinQuery((Query<?>) obj, null);
			}
		}
		Assert.notNull(obj);
		myTableName = MetaHolder.toSchemaAdjustedName(myTableName);
		@SuppressWarnings("deprecation")
		CountClause sqls = selectp.toCountSql(obj, myTableName);

		parse = System.currentTimeMillis();
		int total = 0;
		for (Map.Entry<String, List<BindSql>> sql : sqls.getSqls().entrySet()) {
			total += selectp.processCount(asOperateTarget(sql.getKey()), sql.getValue());
		}
		if (debugMode) {
			long dbAccess = System.currentTimeMillis() - parse; // 数据库查询时间
			parse = parse - start; // 解析SQL时间
			LogUtil.show(StringUtils.concat("Total Count:", String.valueOf(total), "\t Time cost([ParseSQL]:", String.valueOf(parse), "ms, [DbAccess]:", String.valueOf(dbAccess), "ms) |", getTransactionId(null)));
		}
		return total;
	}

	/**批量删除数据
	 * 
	 * @param entities
	 * @param group
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> void batchDelete(List<T> entities) throws SQLException {
		batchDelete(entities,null);
	}
	
	/**
	 * 批量删除数据
	 * 
	 * @param <T>
	 * @param entities
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> void batchDelete(List<T> entities,Boolean group) throws SQLException {
		if (entities.isEmpty())
			return;
		Batch<T> batch = this.startBatchDelete(entities.get(0), null);
		if(group!=null){
			batch.setGroupForPartitionTable(group);
		}
		batch.execute(entities);
	}

	/**
	 * 批量删除数据（按主键），如果没有主键会报错
	 * 
	 * @param clz
	 *            要删除的数据类
	 * @param entities
	 *            要删除的数据集合
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public final <T extends IQueryableEntity> void batchDeleteByPrimaryKey(List<T> entities) throws SQLException {
		if (entities.isEmpty())
			return;

		ITableMetadata meta = MetaHolder.getMeta(entities.get(0));
		if (meta.getPKField().isEmpty()) {
			throw new SQLException("The type " + meta.getTableName(false) + " has no primary key, can not execute batch remove by primarykey");
		}
		// 位于批当中的绑定变量
		long start = System.nanoTime();
		Batch.Delete<T> batch= new Batch.Delete<T>(this, meta);
		T template;
		try {
			template = (T) meta.instance();
			IQueryableEntity first=entities.get(0);
			DbUtils.fillPKConditions(first, meta, BeanWrapper.wrap(first,BeanWrapper.FAST), template.getQuery(), false, true);
		} catch (Exception e) {
			throw new SQLException(e.getMessage());
		}
		BindSql wherePart = rProcessor.toPrepareWhereSql(template.getQuery(), new SqlContext(null,template.getQuery()), false);
		for (BindVariableDescription bind : wherePart.getBind()) {
			bind.setInBatch(true);
		}
		batch.setWherePart(wherePart);
		batch.parseTime = System.nanoTime() - start;
		batch.pkMpode = true;
		batch.execute(entities);
	}

	/**
	 * 获得一个Bach对象，这个batch对象上可以执行批量删除操作。
	 * 
	 * @param template
	 *            批操作的模板。传入的对象必须是一个构成delete的完整请求（含查询条件（默认为主键））。
	 *            后续的所有批量操作都按此模板執行操作。
	 * @param tableName
	 *            强制指定表名，也就是说template当中的表名无效。（传入的表名支持Schema重定向）
	 * @return Batch操作句柄
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> Batch<T> startBatchDelete(T template, String tableName) throws SQLException {
		// 位于批当中的绑定变量
		long start = System.nanoTime();
		BindSql wherePart = rProcessor.toPrepareWhereSql(template.getQuery(),new SqlContext(null, template.getQuery()), false);
		for (BindVariableDescription bind : wherePart.getBind()) {
			bind.setInBatch(true);
		}
		ITableMetadata meta = MetaHolder.getMeta(template);
		Batch.Delete<T> batch = new Batch.Delete<T>(this, meta);
		batch.setWherePart(wherePart);
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
	 */
	public final <T extends IQueryableEntity> void batchInsert(List<T> entities) throws SQLException {
		batchInsert(entities, null);
	}
	
	
	/**
	 * 执行批量插入操作。
	 * 
	 * @param entities
	 *            要插入的对象
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> void batchInsert(List<T> entities,Boolean group) throws SQLException {
		if (entities.isEmpty())
			return;
		Batch<T> batch = startBatchInsert(entities.get(0), null, ORMConfig.getInstance().isDynamicInsert(),false);
		if(group!=null)
			batch.setGroupForPartitionTable(group);
		batch.execute(entities);
	}
	
	/**
	 * 极限模式下的批量操作
	 * @param entities
	 * @param group
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> void extremeInsert(List<T> entities,Boolean group) throws SQLException {
		if (entities.isEmpty())
			return;
		Batch<T> batch = startBatchInsert(entities.get(0), null, false,true);
		if(group!=null)
			batch.setGroupForPartitionTable(group);
		batch.execute(entities);
	}
	
	/**
	 * 执行批量插入操作。
	 * 
	 * @param entities
	 *            要插入的对象
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> void batchInsertDynamic(List<T> entities,Boolean group) throws SQLException {
		if (entities.isEmpty())
			return;
		Batch<T> batch = startBatchInsert(entities.get(0), null, true,false);
		if(group!=null)
			batch.setGroupForPartitionTable(group);
		batch.execute(entities);
	}
	

	/**
	 * 获得一个Bach对象，这个batch对象上可以执行批量插入操作。
	 * 
	 * @param template
	 *            批操作的模板。传入的对象是可以插入的。后续的所有批量操作都按此模板執行操作。 如果开启了
	 *            {@link JefConfiguration.Item#DB_DYNAMIC_INSERT
	 *            DB_DYNAMIC_INSERT}
	 *            功能，那么模板中插入数据库的字段就是后续任务中插入数据库的字段。后续数据库中其他字段即使赋值了也不会入库。
	 * 
	 * @param tableName
	 *            强制指定表名，也就是说template当中的表名无效。（传入的表名支持Schema重定向）
	 * @return Batch操作句柄
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> Batch<T> startBatchInsert(T template, boolean dynamic) throws SQLException {
		return startBatchInsert(template,null,dynamic,false);
	}
	
	public <T extends IQueryableEntity> Batch<T> startBatchInsert(T template, String tableName, boolean dynamic,boolean extreme) throws SQLException {
		long start = System.nanoTime();
		ITableMetadata meta = MetaHolder.getMeta(template);
		Batch.Insert<T> b = new Batch.Insert<T>(this, meta);
		InsertSqlClause insertPart = batchinsertp.toInsertSql((IQueryableEntity) template, tableName, dynamic, extreme);
		b.setInsertPart(insertPart);
		b.setForceTableName(tableName);
		b.extreme=extreme;
		b.parseTime = System.nanoTime() - start;
		return b;
	}

	/**
	 * 获得一个Bach对象，这个batch对象上可以执行批量更新操作。
	 * 
	 * @param template
	 *            批操作的模板。传入的对象必须是一个构成update的完整请求（包含update的字段和查询条件（默认为主键））。
	 *            后续的所有批量操作都按此模板執行操作。 <strong>注意这个模板并未加入到批任务当中</strong>
	 * @param tableName
	 *            强制指定表名，也就是说template当中的表名无效。（传入的表名支持Schema重定向）
	 * @return Batch操作句柄
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> Batch<T> startBatchUpdate(T template, String tableName,boolean dynamic) throws SQLException {
		if (dynamic && !template.needUpdate()) {
			throw new IllegalArgumentException("The input object is not a valid update query Template, since its update value map is empty, change to ");
		}
		long start = System.nanoTime();
		Entry<List<String>, List<Field>> updatePart = rProcessor.toPrepareUpdateClause((IQueryableEntity) template,dynamic);
		// 位于批当中的绑定变量
		BindSql wherePart = rProcessor.toPrepareWhereSql(template.getQuery(), new SqlContext(null, template.getQuery()), true);
		for (BindVariableDescription bind : wherePart.getBind()) {
			bind.setInBatch(true);
		}
		ITableMetadata meta = MetaHolder.getMeta(template);
		Batch.Update<T> batch = new Batch.Update<T>(this, meta);
		batch.forceTableName = MetaHolder.toSchemaAdjustedName(tableName);
		batch.setUpdatePart(updatePart);
		batch.setWherePart(wherePart);
		batch.parseTime = System.nanoTime() - start;
		return batch;
	}

	/**
	 * 批量更新
	 * 
	 * @param entities  要更新的操作请求
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> void batchUpdate(List<T> entities) throws SQLException {
		batchUpdate(entities,null);
	}
	
	/**
	 * 批量更新
	 * 
	 * @param entities 要更新的操作请求
	 * @param group 是否要对操作请求重新分组（数据路由）
	 * @throws SQLException
	 */
	public final <T extends IQueryableEntity> void batchUpdate(List<T> entities,Boolean group) throws SQLException {
		if (entities.isEmpty())
			return;
		T template = null;
		for(int i=0;i<3;i++){
			template=entities.get(i);
			if(!template.getUpdateValueMap().isEmpty()){
				break;
			}
		}
		Batch<T> batch = this.startBatchUpdate(template, null,!template.getUpdateValueMap().isEmpty());
		if(group!=null){
			batch.setGroupForPartitionTable(group);
		}
		batch.execute(entities);
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
		return asOperateTarget(null).func(func, params);
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
	 * @see SqlTemplate#getExpressionValue(String, Class, Object...)
	 */
	public final <T> T getExpressionValue(String expression, Class<T> clz) throws SQLException {
		return asOperateTarget(null).getExpressionValue(expression.toString(), clz);
	}
	
	public <T> T getExpressionValue(DbFunction func, Class<T> clz,Object... params) throws SQLException {
		return asOperateTarget(null).getExpressionValue(func,clz,params);
	}

	/**
	 * 提交session（事务）中没有提交的更改
	 * 
	 * @throws SQLException
	 */
	public abstract void commit() throws SQLException;

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
		 * 禁用内存重新排序功能<br>
		 * 当查询分解为多个select语句分别得到结果集后，每个结果集显然是已经按照order by的要求排好了序<br>
		 * 但是多个结果集拼在一起以后，这个顺序未必就是order by的顺序了。<br>
		 * 为此我们会在内存中对结果集再次排序，以满足原先的SQL的Order by子句的要求。<br>
		 * 禁用这个选项可以节省性能开销<br>
		 * 此外，我们的重排序算法主要还是影响速度，并不占用多少内存。所以您无需为了内存原因而禁用重排序功能<br>
		 */
		NO_RESORT,
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
		boolean plain = ArrayUtils.contains(transformers.getStrategy(), PopulateStrategy.PLAIN_MODE) || (mapping == null && !IQueryableEntity.class.isAssignableFrom(returnClz));
		if (plain) {
			return ResultPopulatorImpl.instance.iteratorPlain(rs, transformers);
		}
		return ResultPopulatorImpl.instance.iteratorNormal(this, rs, mapping, transformers);
	}

	@SuppressWarnings("unchecked")
	<T> List<T> populateResultSet(IResultSet rsw, EntityMappingProvider mapping, Transformer transformers) throws SQLException {
		Class<T> returnClz = (Class<T>) transformers.getResultClazz();

		if (returnClz == null) {//未指定时。如果结果只有1列直接返回;如果有多列，Map返回。
			if (rsw.getColumns().length() > 1) {
				returnClz = (Class<T>) Var.class;
			} else {
				return (List<T>) ResultSets.toObjectList(rsw, 1, Integer.MAX_VALUE);
			}
		}
		//基础类型返回
		if (ArrayUtils.fastContains(MetadataService.SIMPLE_CLASSES, returnClz)) {
			return ResultPopulatorImpl.instance.toSimpleObjects(rsw, returnClz);
		}
		//数组返回——模式1：每张表映射成一个元素 模式2：每列映射成一个元素  模式3：自定义Mapper
		if(returnClz.isArray()){
			return (List<T>) ResultPopulatorImpl.instance.toDataObjectMap(rsw, mapping, transformers);
		}
		//Map返回。
		if (returnClz == Var.class || returnClz == Map.class) {
			return (List<T>) ResultPopulatorImpl.instance.toVar(rsw, transformers);
		}
		//动态表返回
		if (transformers.isVarObject()) {
			return (List<T>) ResultPopulatorImpl.instance.toJavaObject(this, rsw, mapping, transformers);
		}
		boolean plain = transformers.hasStrategy(PopulateStrategy.PLAIN_MODE) || (mapping == null && !IQueryableEntity.class.isAssignableFrom(returnClz));
		if (plain) {
			return (List<T>) ResultPopulatorImpl.instance.toPlainJavaObject(rsw, transformers);
		}
		return (List<T>) ResultPopulatorImpl.instance.toJavaObject(this, rsw, mapping, transformers);
	}

	// 包装当前AbsDbClient,包装为缺省的操作对象即无dbkey.
	private final OperateTarget wrapThisWithEmptyKey(MultipleResultSet rs, boolean mustTx) throws SQLException {
		if (mustTx && this instanceof DbClient) {// 如果不是在事务中，那么就用一个内嵌事务将其包裹住，作用是在resultSet的生命周期内，该连接不会被归还。并且也预防了基于线程的连接模型中，该连接被本线程的其他SQL操作再次取用然后释放回池
			Transaction tx = new Transaction((DbClient) this, TransactionFlag.ResultHolder,true);
			return new OperateTarget(tx, null);
		} else {
			return new OperateTarget(this, null);

		}
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

	/*
	 * 内部使用 使用绑定变量的更新
	 */
	final protected int innerUpdatePrepared(IQueryableEntity obj, String myTableName) throws SQLException {
		long start = System.currentTimeMillis();
		Query<?> query=obj.getQuery();
		BindSql whereValues = rProcessor.toPrepareWhereSql(query, new SqlContext(null,query), true);
		if (!obj.needUpdate()) {
			return 0;
		}
		Entry<List<String>, List<Field>> setValues = rProcessor.toPrepareUpdateClause((IQueryableEntity) obj,true);
		PartitionResult[] tables = DbUtils.toTableNames(obj, myTableName, obj.getQuery(), getPool().getPartitionSupport());
		int count = 0;
		for (PartitionResult part : tables) {
			count += p.processUpdatePrepared(asOperateTarget(part.getDatabase()), obj, setValues, whereValues, part, start);
		}
		if (count > 0) {
			getCache().onUpdate(myTableName == null ? obj.getClass().getName() : myTableName, whereValues.getSql(), CacheImpl.toParamList(whereValues.getBind()));
		}
		return count;
	}

	/*
	 * 内部使用 不使用绑定变量的插入
	 */
	final protected int innerUpdateNormal(IQueryableEntity obj, String myTableName) throws SQLException {
		long start = System.currentTimeMillis();

		String where = rProcessor.toWhereClause(obj.getQuery(), new SqlContext(null, obj.getQuery()), true);
		if (!obj.needUpdate()) {
			return 0;
		}
		String update = rProcessor.toUpdateClause(obj,true);
		int count = 0;
		for (PartitionResult site : DbUtils.toTableNames(obj, myTableName, obj.getQuery(), getPool().getPartitionSupport())) {
			count += p.processUpdateNormal(asOperateTarget(site.getDatabase()), obj, start, where, update, site);
		}
		if (count > 0) {
			getCache().onUpdate(myTableName == null ? obj.getClass().getName() : myTableName, where, null);
		}
		return count;
	}

}
