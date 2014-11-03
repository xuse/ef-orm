package org.easyframe.enterprise.spring;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jef.common.wrapper.Page;
import jef.database.IQueryableEntity;
import jef.database.NamedQueryConfig;
import jef.database.NativeQuery;

/**
 * 泛型的通用Dao子类必须实现泛型
 * 
 * @author Administrator
 * 
 * @param <T>
 */
public interface GenericDao<T extends IQueryableEntity> {
	/**
	 * 插入一条记录（无级联操作）
	 * 
	 * @param entity
	 *            要插入数据库的对象
	 * @return 被插入数据库的对象
	 */
	public T insert(T entity);

	/**
	 * 插入记录(带级联操作)
	 * 
	 * @param entity
	 *            要插入数据库的对象
	 * @return 被插入数据库对象
	 */
	public T insertCascade(T entity);

	/**
	 * 更新记录(无级联)
	 * 
	 * @param entity
	 *            要更新的对象模板
	 * @return 影响记录行数
	 */
	public int update(T entity);

	/**
	 * 更新记录
	 * 
	 * @param entity
	 *            要更新的对象模板
	 * @return 影响记录行数
	 */
	public int updateCascade(T entity);

	/**
	 * 删除记录（注意，根据入参Query中的条件可以删除多条记录） 无级联操作 ，如果要使用带级联操作的remove方法，可以使用
	 * {@link #removeCascade}
	 * 
	 * @param entity
	 *            要删除的对象模板
	 * @return 影响记录行数
	 */
	public int remove(T entity);

	/**
	 * 删除记录（注意，根据入参Query中的条件可以删除多条记录）
	 * 
	 * @param entity
	 *            要删除的对象模板
	 * @return 影响记录行数
	 */
	public int removeCascade(T entity);

	/**
	 * 持久化一条记录，(如果记录存在则update，否则执行insert)
	 * 
	 * @param entity
	 *            要写入数据库的对象
	 * @return 被写入的数据库的对象
	 */
	public T merge(T entity);

	/**
	 * 载入一条记录(无级联)
	 * 
	 * @param entity
	 *            查询对象模板
	 * @return 查询结果
	 */
	public T load(T entity);

	/**
	 * 载入一条记录(带级联)
	 * 
	 * @param entity
	 *            查询对象模板
	 * @return 查询结果
	 * @since 1.7.0
	 * 
	 */
	public T loadCascade(T entity);

	/**
	 * 根据示例的对象删除记录
	 * 
	 * @param entity
	 *            删除的对象模板
	 * @return 影响记录行数
	 */
	public int removeByExample(T entity);

	/**
	 * 当确定主键为单对象时，根据主键加载一个对象
	 * 
	 * @param key
	 *            主键
	 * @return 根据主键加载的记录
	 */
	public T get(Serializable key);

	/**
	 * 查找记录(不带级联)
	 * 
	 * @param Entity
	 *            查询对象模板
	 * @return 查询结果
	 */
	public List<T> find(T Entity);

	/**
	 * 查找记录(带级联)
	 * 
	 * @param Entity
	 *            查询对象模板
	 * @return 查询结果
	 */
	public List<T> findCascade(T Entity);

	/**
	 * 得到整张表的全部数据。(单表操作，不带级联)
	 * 
	 * @return 查询结果
	 */
	public List<T> getAll();

	/**
	 * 根据设置过值的字段进行查找
	 * 
	 * @param entity
	 *            查询的对象模板
	 * @return 查询结果
	 */
	public List<T> findByExample(T entity);

	/**
	 * 查找记录
	 * 
	 * @param query
	 *            查询请求
	 * @return 查询结果
	 */
	public List<T> find(jef.database.query.Query<T> query);

	/**
	 * 使用指定的SQL查找记录,此方法支持绑定变量，可以在SQL中使用 ?1 ?2的格式指定变量，并在param中输入实际参数
	 * 
	 * @param entity
	 *            返回结果类型
	 * @param query
	 *            E-SQL，变量必须以 ?1 ?2等形式写入
	 * @param param
	 *            绑定变量
	 * @return
	 */
	public List<T> find(Class<T> entity, String query, Object... param);

	/**
	 * 查找并分页(不带级联)
	 * 
	 * @param entity
	 *            返回结果类型
	 * @param start
	 *            分页开始记录，第一条记录从0开始
	 * @param limit
	 *            每页的大小
	 * @return 查询结果的当页数据
	 * @see Page
	 */
	public Page<T> findAndPage(T entity, int start, int limit);

	/**
	 * 查找并分页(带级联)
	 * 
	 * @param entity
	 *            返回结果类型
	 * @param start
	 *            开始记录数，第一条记录从0开始
	 * @param limit
	 *            每页的大小
	 * @return 查询结果的当页数据
	 * @see Page
	 */
	public Page<T> findAndPageCascade(T entity, int start, int limit);

	/**
	 * 批量插入
	 * 
	 * @param entities
	 *            要插入的对象列表
	 * @return 影响记录条数
	 */
	public int batchInsert(List<T> entities);

	/**
	 * 批量插入
	 * 
	 * @param entities
	 *            要插入的对象列表
	 * @param doGroup
	 *            是否对每条记录重新分组。
	 *            {@linkplain jef.database.Batch#isGroupForPartitionTable
	 *            什么是重新分组}
	 * @return 影响记录条数
	 */
	public int batchInsert(List<T> entities, boolean doGroup);

	/**
	 * 批量删除
	 * 
	 * @param entities
	 *            要删除的对象列表
	 * @return 影响的记录条数
	 */
	public int batchRemove(List<T> entities);

	/**
	 * 批量删除
	 * 
	 * @param entities
	 *            要删除的对象列表
	 * @param doGroup
	 *            是否对每条记录重新分组。
	 *            {@linkplain jef.database.Batch#isGroupForPartitionTable
	 *            什么是重新分组}
	 * @return 影响的记录条数
	 */
	public int batchRemove(List<T> entities, boolean doGroup);

	/**
	 * 批量（按主键）更新
	 * 
	 * @param entities
	 *            要更新的记录
	 * @return 影响的记录条数
	 */
	public int batchUpdate(List<T> entities);

	/**
	 * 批量（按主键）更新
	 * 
	 * @param entities
	 *            要更新的记录
	 * @param doGroup
	 *            是否对每条记录重新分组。
	 *            {@linkplain jef.database.Batch#isGroupForPartitionTable
	 *            什么是重新分组}
	 * @return 影响的记录条数
	 */
	public int batchUpdate(List<T> entities, boolean doGroup);

	/**
	 * 使用命名查询查找. {@linkplain NamedQueryConfig 什么是命名查询}
	 * 
	 * @param nqName
	 *            命名查询的名称
	 * @param param
	 *            绑定变量参数
	 * @return 查询结果
	 */
	public List<T> findByNq(String nqName, Map<String, Object> param);

	/**
	 * 使用命名查询查找并分页. {@linkplain NamedQueryConfig 什么是命名查询}
	 * 
	 * @param nqName
	 *            命名查询的名称
	 * @param param
	 *            绑定变量参数
	 * @param start
	 *            开始记录数，第一条记录从0开始
	 * @param limit
	 *            每页的大小
	 * @return 查询结果
	 * @see Page
	 */
	public Page<T> findAndPageByNq(String nqName, Map<String, Object> param, int start, int limit);

	/**
	 * 执行命名查询. {@linkplain NamedQueryConfig 什么是命名查询}
	 * 
	 * @param nqName
	 *            命名查询的名称
	 * @param param
	 *            绑定变量参数
	 * @return 影响记录行数
	 */
	public int executeNq(String nqName, Map<String, Object> param);

	/**
	 * 执行指定的SQL语句 这里的Query可以是insert或update，或者其他DML语句
	 * 
	 * @param sql
	 *            SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * @param param
	 *            绑定变量参数
	 * @return 影响记录行数
	 */
	public int executeQuery(String sql, Map<String, Object> param);

	/**
	 * 根据指定的SQL查找
	 * 
	 * @param sql
	 *            SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * @param param
	 *            绑定变量参数
	 * @return 查询结果
	 */
	public List<T> findByQuery(String sql, Map<String, Object> param);

	/**
	 * 根据单个的字段条件查找结果(仅返回第一条)
	 * 
	 * @param field
	 *            条件字段名
	 * @param value
	 *            条件字段值
	 * @return 符合条件的结果。如果查询到多条记录，也只返回第一条
	 */
	public T loadByField(String field, Serializable value);

	/**
	 * 根据单个的字段条件查找结果
	 * 
	 * @param field
	 *            条件字段名
	 * @param value
	 *            条件字段值
	 * @return 符合条件的结果
	 */
	public List<T> findByField(String field, Serializable value);

	/**
	 * 根据单个的字段条件删除记录
	 * 
	 * @param field
	 *            条件字段名
	 * @param value
	 *            条件字段值
	 * @return 删除的记录数
	 */
	public int deleteByField(String field, Serializable value);

	/**
	 * 按个主键的值读取记录 (只支持单主键，不支持复合主键)
	 * 
	 * @param pkValues
	 * @return
	 */
	public List<T> batchLoad(List<? extends Serializable> pkValues);

	
	/**
	 * 按主键批量删除 (只支持单主键，不支持复合主键)
	 * @param pkValues
	 * @return
	 */
	public int batchDelete(List<? extends Serializable> pkValues);
	
	
	/**
	 * 根据单个字段的值读取记录（批量）
	 * 
	 * @param field
	 *            条件字段
	 * @param values
	 *            查询条件的值
	 * @return 符合条件的记录
	 */
	public List<T> batchLoadByField(String field, List<? extends Serializable> values);

	/**
	 * 根据单个字段的值删除记录（批量）
	 * @param field
	 * @param values
	 * @return
	 */
	public int batchDeleteByField(String field,List<? extends Serializable> values);
}
