package org.easyframe.enterprise.spring;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.persistence.NonUniqueResultException;

import jef.common.wrapper.Page;
import jef.database.DbClient;
import jef.database.IQueryableEntity;
import jef.database.NamedQueryConfig;
import jef.database.NativeQuery;
import jef.database.RecordHolder;
import jef.database.RecordsHolder;
import jef.database.Session;
import jef.database.meta.ITableMetadata;
import jef.database.query.Query;
import jef.database.wrapper.ResultIterator;

import com.mysema.query.sql.SQLQuery;

/**
 * 通用Dao接口（非泛型），和泛型的{@link GenericDao}相对
 * 
 * 将业务逻辑上移后，dao层淡化的结果。这个commonDao提供了几乎所有实体的creatia api操作
 * @author jiyi
 *
 *@see GenericDao
 */
public interface CommonDao{
	/**
	 * 插入记录(不带级联)
	 * @param entity 要插入的记录
	 * @return 插入后的记录
	 */
	<T> T insert(T entity);
	
	/**
	 * 插入记录（带级联的插入记录）
	 * @param entity
	 * @return
	 */
	<T> T insertCascade(T entity);

	/**
	 * 在记录已经存在的情况下更新，否则插入记录
	 * 
	 * @param entity 要持久化的记录
	 */
	void persist(Object entity);

	/**
	 * 在记录已经存在的情况下更新，否则插入记录
	 * 
	 * @param entity
	 * @return
	 */
	<T> T merge(T entity);

	/**
	 * 删除记录(不带级联)
	 * 
	 * @param entity 待删除对象（模板）.
	 * <ul>
	 * <li>如果对象是{@link IQueryableEntity}设置了Query条件，按query条件查询。 否则——</li>
	 * <li>如果设置了主键值，按主键查询，否则——</li>
	 * <li>按所有设置过值的字段作为条件查询。</li></ul>
	 */
	int remove(Object entity);
	
	/**
	 * 删除记录(带级联)
	 * @param entity
	 * <ul>
	 * <li>如果对象是{@link IQueryableEntity}设置了Query条件，按query条件查询。 否则——</li>
	 * <li>如果设置了主键值，按主键查询，否则——</li>
	 * <li>按所有设置过值的字段作为条件查询。</li></ul>
	 */
	int removeCascade(Object entity);
	
	/**
	 * 根据模板的对象删除记录。
	 * 
	 * @param entity     作为删除条件的对象（模板）
	 * @param properties 作为删除条件的字段名。当不指定properties时，首先检查entity当中是否设置了主键，如果有主键按主键删除，否则按所有非空字段作为匹配条件。
	 * @return
	 */
	<T> int removeByExample(T entity,String... properties);
	
	/**
	 * 删除全部记录
	 * @param meta 元数据描述
	 */
	void removeAll(ITableMetadata meta);
	
	/**
	 * 批量删除指定属性值的记录
	 * @param meta 元数据描述
	 * @param propertyName 属性名称
	 * @param values 属性值列表
	 */
	void removeByProperty(ITableMetadata meta, String propertyName, List<?> values);

	/**
	 * 查询列表
	 * 
	 * @param data 查询请求。
	 * <ul>
	 * <li>如果设置了Query条件，按query条件查询。 否则——</li>
	 * <li>如果设置了主键值，按主键查询，否则——</li>
	 * <li>按所有设置过值的字段作为条件查询。</li></ul>
	 * @return 结果
	 */
	<T> List<T> find(T data);
	
	
	/**
	 * 根据主键查询
	 * @param type 类
	 * @param id 主键
	 * @return 查询结果 
	 * @since 1.10
	 */
	<T> T load(Class<T> type,Serializable... id);
	
	
	/**
	 * 查询数据
	 * @param data 查询请求
	 * @return 结果
	 * @since 1.10
	 */
	<T extends IQueryableEntity> List<T> find(Query<T> data);
	
	/**
	 * 查找对象并且返回遍历器
	 * @param obj 查询请求
	 * <ul>
	 * <li>如果设置了Query条件，按query条件查询。 否则——</li>
	 * <li>如果设置了主键值，按主键查询，否则——</li>
	 * <li>按所有设置过值的字段作为条件查询。</li></ul>
	 * @return 结果遍历器
	 */
	<T> ResultIterator<T> iterate(T obj);

	
	/**
	 * 根据样例查找
	 * @param entity 查询条件
	 * @param property 作为查询条件的字段名。当不指定properties时，首先检查entity当中是否设置了主键，如果有主键按主键删除，否则按所有非空字段作为匹配条件。
	 * @return 查询结果
	 */
	<T> List<T> findByExample(T entity,String... properties);
	
	
	/**
	 * 查询并分页
	 * @param data 查询请求
	 * @param start 起始记录，offset。从0开始。
	 * @param limit 限制记录条数。如每页10条传入10。
	 * @return
	 */
	<T> Page<T> findAndPage(T data,int start,int limit);
	

	/**
	 * 查询一条记录，如果结果不唯一则抛出异常
	 * 
	 * @param data
	 * @param unique
	 *            要求查询结果是否唯一。为true时，查询结果不唯一将抛出异常。为false时，查询结果不唯一仅取第一条。
	 * @throws NonUniqueResultException
	 *             结果不唯一
	 * @return 查询结果
	 */	
	<T> T load(T data);
	
	/**
	 * 根据查询查询一条记录
	 * @param entity
	 * @param unique true表示结果必须唯一，false则允许结果不唯一仅获取第一条记录
	 * @return 查询结果
	 * @throws NonUniqueResultException
	 *             结果不唯一
	 */
	public <T> T load(T entity, boolean unique); 
	
	
	/**
	 * 更新记录(不带级联)
	 * @param entity 要更新的对象
	 * @return 影响的记录条数
	 */
	<T> int update(T entity);
	
	/**
	 * 更新记录（带级联）
	 * @param entity
	 * @return
	 */
	<T> int updateCascade(T entity);
	
	/**
	 * 更新记录
	 * @param entity 要更新的对象
	 * @param property where字段值
	 * @return 影响的记录条数
	 */
	<T> int updateByProperty(T entity,String... property);
	
	/**
	 * 带有附加条件的记录更新
	 * @param entity 实体仅作WHERE条件使用
	 * @param setValues 要更新哪些字段的Map
	 * @param property 哪些字段用作Where条件
	 * @return
	 */
	<T> int update(T entity,Map<String,Object> setValues,String... property);
	
	/**
	 * 执行命名查询
	 * {@linkplain NamedQueryConfig 什么是命名查询}
	 * @param nqName 命名查询名称
	 * @param type  返回类型
	 * @param params  查询参数
	 * @return 查询结果
	 */
	<T> List<T> findByNq(String nqName, Class<T> type,Map<String, Object> params);

	/**
	 * 执行命名查询
	 * {@linkplain NamedQueryConfig 什么是命名查询}
	 * @param nqName 命名查询名称
	 * @param meta  返回类型
	 * @param params 查询参数
	 * @return 查询结果
	 */
	<T> List<T> findByNq(String nqName, ITableMetadata meta,Map<String, Object> params);

	/**
	 * 使用命名查询查找并分页
	 * {@linkplain NamedQueryConfig 什么是命名查询}
	 * @param nqName  命名查询名称
	 * @param type    返回类型
	 * @param params 查询参数
	 * @param start 开始记录数，从0开始
	 * @param limit 限制结果条数
	 * @return 查询结果
	 */
	<T> Page<T> findAndPageByNq(String nqName, Class<T> type,Map<String, Object> params, int start,int limit);
	
	/**
	 * 使用命名查询查找并分页
	 * {@linkplain NamedQueryConfig 什么是命名查询}
	 * @param nqName 命名查询名称
	 * @param meta  返回类型
	 * @param params 查询参数
	 * @param start 开始记录数，从0开始
	 * @param limit 限制结果条数
	 * @return 查询结果
	 */
	<T> Page<T> findAndPageByNq(String nqName, ITableMetadata meta,Map<String, Object> params, int start,int limit);
	
	/**
	 * 使用命名查询执行（更新/创建/删除）等操作
	 * {@linkplain NamedQueryConfig 什么是命名查询}
	 * @param nqName 命名查询名称
	 * @param params sql参数
	 * @return 查询结果
	 */
	int executeNq(String nqName,Map<String,Object> params);
	

	/**
	 * 执行指定的SQL（更新/创建/删除）等操作
	 * @param sql SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * @param param
	 * @return 查询结果
	 */
	int executeQuery(String sql,Map<String,Object> param);
	
	/**
	 * 根据指定的SQL查找
	 * @param sql SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * <pre><code>
	 *  String sql="select * from table where 1=1 and id=:id<int> and name like :name<$string>";
	 *  Map<String, Object> params = new HashMap<String,Object>();
	 *  params.put(id,123);
	 *  params.put(name,"Join");
	 *  session.findByQuery(sql,ResultClass.class, params); //根据SQL语句查询，返回类型为ResultClass。
	 * <code></pre>
	 * @param retutnType 返回类型
	 * @param params 绑定变量参数
	 * @return 查询结果
	 */
	<T> List<T> findByQuery(String sql,Class<T> retutnType, Map<String, Object> params);
	
	/**
	 * 查找对象并且返回遍历器
	 * @param sql SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * @param returnType
	 * @param params 参数
	 * @return
	 */
	<T> ResultIterator<T> iterateByQuery(String sql,Class<T> returnType,Map<String,Object> params);
	
	/**
	 * 查找对象并且返回遍历器
	 * @param sql SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * @param returnType 返回类型的元数据模型
	 * @param params 参数
	 * @return
	 */
	<T> ResultIterator<T> iterateByQuery(String sql, ITableMetadata returnType, Map<String, Object> params);
	
	/**
	 * 根据指定的SQL查找
	 * @param sql SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * @param retutnType
	 * @param params
	 * @return 查询结果
	 */
	<T> List<T> findByQuery(String sql,ITableMetadata retutnType, Map<String, Object> params);
	
	/**
	 * 根据指定的SQL查找并分页
	 * @param sql SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * @param retutnType 返回结果类型
	 * @param params     绑定变量参数
	 * @param start      起始记录行，第一条记录从0开始。
	 * @param limit		  每页记录数
	 * @return 查询结果
	 */
	<T> Page<T> findAndPageByQuery(String sql,Class<T> retutnType, Map<String, Object> params,int start,int limit);
	
	/**
	 * 根据指定的SQL查找并分页
	 * @param sql SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * @param retutnType 返回结果类型
	 * @param params 绑定变量参数
	 * @param start 起始记录行，第一条记录从0开始。
	 * @param limit  每页记录数
	 * @return
	 */
	<T> Page<T> findAndPageByQuery(String sql,ITableMetadata retutnType, Map<String, Object> params,int start,int limit);
	
	/**
	 * 根据主键的值加载一条记录
	 * @param entityClass
	 * @param primaryKey
	 * @return 查询结果
	 */
	<T> T loadByPrimaryKey(Class<T> entityClass,  Serializable primaryKey);
	
	/**
	 * 根据主键的值批量加载记录 (不支持复合主键)
	 * @param entityClass 实体类
	 * @param values 多个主键值
	 */
	<T> List<T> loadByPrimaryKeys(Class<T> entityClass, List<? extends Serializable> values);
	
	/**
	 * 根据主键的值加载一条记录
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param id
	 * @return  查询结果
	 */
	<T> T loadByPrimaryKey(ITableMetadata meta, Object id);
	
	/**
	 * 根据某个指定字段进行查找
	 * @param meta
	 * @param propertyName
	 * @param value
	 * @return
	 */
	<T> List<T> findByField(Class<T> meta, String propertyName, Object value);
	
	/**
	 * 根据某个指定属性的值查找
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param propertyName
	 * @param value
	 * @return  查询结果
	 */
	List<?> findByField(ITableMetadata meta, String propertyName, Object value);
	
	/**
	 * 根据指定的字段批量查找记录
	 * @param meta
	 * @param propertyName
	 * @param value, 要查找的记录的字段值（多个）
	 * @return
	 */
	List<?> findByField(ITableMetadata meta, String propertyName, List<? extends Serializable> value);
	
	/**
	 * 使用已知的属性查找一个结果
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param field 字段名
	 * @param unique
	 *            true要求查询结果必须唯一。false允许结果不唯一，但仅取第一条。
	 * @param id
	 * @return  查询结果
	 * @throws NonUniqueResultException
	 *             结果不唯一
	 */
	<T>  T loadByField(ITableMetadata meta,String field,Serializable id,boolean unique);
	
	
	/**
	 * 根据指定的字段查找单条记录。如果结果不唯一抛出NonUniqueResultException
	 * @param field 条件字段
	 * @param value 条件值
	 * @return 查询结果
	 * @throws NonUniqueResultException
	 *             结果不唯一 
	 */
	<T extends IQueryableEntity> T loadByField(jef.database.Field field, Object value);
	
	/**
	 * 根据指定的字段查找单条记录。
	 * @param field 条件字段
	 * @param value 条件值
	 * @param unique 是否要求结果唯一。为true时如果结果不唯一会抛出NonUniqueResultException
	 * @return 查询结果
	 * @throws NonUniqueResultException
	 *             结果不唯一
	 */
	<T extends IQueryableEntity> T loadByField(jef.database.Field field, Object value, boolean unique);
	
	
	/**
	 * 根据指定的字段值读取单个记录
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param unique
	 *            true要求查询结果必须唯一。false允许结果不唯一，但仅取第一条。
	 * @param field
	 * @param id
	 * @return  查询结果
	 * @throws NonUniqueResultException
	 *             结果不唯一
	 */
	<T> T loadByField(Class<T> meta,String field,Serializable key,boolean unique);
	
	/**
	 * 根据指定的字段值删除记录 
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param field
	 * @param value
	 * @return 影响记录行数
	 */
	<T> int removeByField(Class<T> meta,String field,Serializable value);
	
	/**
	 * 根据指定的字段值删除记录
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param field
	 * @param value
	 * @return  影响记录行数
	 */
	int removeByField(ITableMetadata meta,String field,Serializable value);
	
	/**
	 * 得到当前的数据库操作Session
	 */
	Session getSession();
	
	/**
	 * 得到当前无事务的操作Session，可以在其上执行各种DDL。访问Database MetaData等。<br>
	 * 因为DDL和DML混合执行的话，会造成事务被误提交。使用 getNoTransactionSession()方法得到的Session上执行DDL时，不会引起当前事务的变化。
	 * @see DbClient
	 * @see DbClient#getMetaData(String)
	 */
	DbClient getNoTransactionSession();

	/**
	 * 批量插入
	 * 
	 * @param entities  要写入的对象列表
	 * @return 影响记录行数
	 */
	<T> int batchInsert(List<T> entities);
	
	/**
	 * 批量插入，使用extreme模式插入
	 * @param entities  要写入的对象列表
	 * @return 影响记录行数
	 */
	<T> int extremeInsert(List<T> entities);
	
	/**
	 * 批量插入
	 * @param entities  要写入的对象列表
	 * @param doGroup  是否对每条记录重新分组。{@linkplain jef.database.Batch#isGroupForPartitionTable 什么是重新分组}
	 * @return 影响记录行数
	 */
	<T> int batchInsert(List<T> entities,Boolean doGroup);

	/**
	 * 批量删除(按主键)
	 * 
	 * @param entities  要删除的对象列表
	 * @return 影响记录行数
	 */
	<T> int batchDelete(List<T> entities);
	
	/**
	 * 批量删除(按主键)
	 * @param entities  要删除的对象列表
	 * @param doGroup  是否对每条记录重新分组。{@linkplain jef.database.Batch#isGroupForPartitionTable 什么是重新分组}
	 * @return 影响记录行数
	 */
	<T>  int batchDelete(List<T> entities,Boolean doGroup);

	/**
	 * 批量（按主键）更新
	 * 
	 * @param entities  要写入的对象列表
	 * @return 影响记录行数
	 */
	<T>  int batchUpdate(List<T> entities);

	/**
	 * 批量（按主键）更新
	 * @param entities 要写入的对象列表
	 * @param doGroup  是否对每条记录重新分组。{@linkplain jef.database.Batch#isGroupForPartitionTable 什么是重新分组}
	 * @return 影响记录行数
	 */
	<T>  int batchUpdate(List<T> entities,Boolean doGroup);
	
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
	<T extends IQueryableEntity> RecordsHolder<T> selectForUpdate(Query<T> query);
	
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
	<T extends IQueryableEntity> RecordHolder<T> loadForUpdate(Query<T> obj);
	
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
	<T extends IQueryableEntity> RecordHolder<T> loadForUpdate(T query);
	
	
	
	
	/**
	 * 获得一个QueryDSL查询对象
	 * @return SQLQuery
	 * @see SQLQuery
	 */
	SQLQuery q();
}
