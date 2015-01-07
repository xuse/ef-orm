package org.easyframe.enterprise.spring;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jef.common.wrapper.Page;
import jef.database.DbClient;
import jef.database.IQueryableEntity;
import jef.database.NamedQueryConfig;
import jef.database.NativeQuery;
import jef.database.Session;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.ResultIterator;

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
	 * 插入记录
	 * @param entity 要插入的记录
	 * @return 插入后的记录
	 */
	<T> T insert(T entity);

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
	 * 删除记录
	 * 
	 * @param entity 待删除对象（模板）.
	 * <ul>
	 * <li>如果对象是{@link IQueryableEntity}设置了Query条件，按query条件查询。 否则——</li>
	 * <li>如果设置了主键值，按主键查询，否则——</li>
	 * <li>按所有设置过值的字段作为条件查询。</li></ul>
	 */
	void remove(Object entity);
	
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
	 * @return
	 */
	<T> List<T> findByExample(T entity,String... properties);
	
	
	/**
	 * 查询并分页
	 * @param data
	 * @return
	 */
	<T> Page<T> findAndPage(T data,int start,int limit);
	

	/**
	 * 根据主键查询一条记录。
	 * @param data
	 * @return 查询结果
	 */	
	<T> T load(T data);
	
	
	/**
	 * 更新记录
	 * @param entity 要更新的对象
	 * @return 影响的记录条数
	 */
	<T> int update(T entity);
	
	/**
	 * 更新记录
	 * @param entity 要更新的对象
	 * @param property where字段值
	 * @return 影响的记录条数
	 */
	<T> int updateByProperty(T entity,String... property);
	
	/**
	 * 更新记录
	 * @param entity 要更新的对象
	 * @param setValues 要设置的属性和值
	 * @param property where字段值
	 * @return 影响的记录条数
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
	 * 执行命名查询
	 * {@linkplain NamedQueryConfig 什么是命名查询}
	 * @param nqName 命名查询名称
	 * @param params sql参数
	 * @return 查询结果
	 */
	int executeNq(String nqName,Map<String,Object> params);
	

	/**
	 * 执行指定的SQL查询
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
	 * @param retutnType
	 * @param params
	 * @return
	 */
	<T> Page<T> findAndPageByQuery(String sql,Class<T> retutnType, Map<String, Object> params,int start,int limit);
	
	/**
	 * 根据指定的SQL查找并分页
	 * @param sql SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * @param retutnType
	 * @param params
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
	 * 根据主键的值批量加载记录
	 */
	<T> List<T> loadByPrimaryKeys(Class<T> entityClass, List<? extends Serializable> primaryKey);
	
	/**
	 * 根据主键的值加载一条记录
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param id
	 * @return  查询结果
	 */
	<T> T loadByPrimaryKey(ITableMetadata meta, Object id);
	
	/**
	 * 根据某个指定属性的值查找
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param propertyName
	 * @param value
	 * @return  查询结果
	 */
	List<?> findByKey(ITableMetadata meta, String propertyName, Object value);
	
	/**
	 * 根据指定的字段批量查找记录
	 * @param meta
	 * @param propertyName
	 * @param value, 要查找的记录的字段值（多个）
	 * @return
	 */
	List<?> findByKeys(ITableMetadata meta, String propertyName, List<? extends Serializable> value);
	
	/**
	 * 使用已知的属性查找一个结果
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param id
	 * @return  查询结果
	 */
	<T>  T loadByKey(ITableMetadata meta,String field,Serializable id);
	
	/**
	 * 根据指定的字段值读取单个记录
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param field
	 * @param id
	 * @return  查询结果
	 */
	<T> T loadByKey(Class<T> meta,String field,Serializable key);
	
	/**
	 * 根据指定的字段值删除记录 
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param field
	 * @param key
	 * @return 影响记录行数
	 */
	<T> int removeByKey(Class<T> meta,String field,Serializable key);
	
	/**
	 * 根据指定的字段值删除记录
	 * @param meta 数据库表的元模型. {@linkplain ITableMetadata 什么是元模型}
	 * @param field
	 * @param key
	 * @return  影响记录行数
	 */
	int removeByKey(ITableMetadata meta,String field,Serializable key);
	
	/**
	 * 得到当前的JEF Session
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
	 * 批量插入
	 * @param entities  要写入的对象列表
	 * @param doGroup  是否对每条记录重新分组。{@linkplain jef.database.Batch#isGroupForPartitionTable 什么是重新分组}
	 * @return 影响记录行数
	 */
	<T> int batchInsert(List<T> entities,Boolean doGroup);

	/**
	 * 批量删除
	 * 
	 * @param entities  要删除的对象列表
	 * @return 影响记录行数
	 */
	<T> int batchRemove(List<T> entities);
	
	/**
	 * 批量删除
	 * @param entities  要删除的对象列表
	 * @param doGroup  是否对每条记录重新分组。{@linkplain jef.database.Batch#isGroupForPartitionTable 什么是重新分组}
	 * @return 影响记录行数
	 */
	<T>  int batchRemove(List<T> entities,Boolean doGroup);

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
	
}
