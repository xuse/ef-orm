package org.easyframe.enterprise.spring;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import jef.common.wrapper.Page;
import jef.database.IQueryableEntity;
import jef.database.NamedQueryConfig;
import jef.database.NativeQuery;
import jef.database.RecordHolder;
import jef.database.RecordsHolder;

import com.github.geequery.springdata.repository.support.Update;
import com.mysema.query.sql.SQLQuery;

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
	T insert(T entity);

	/**
	 * 插入记录(带级联操作)
	 * 
	 * @param entity
	 *            要插入数据库的对象
	 * @return 被插入数据库对象
	 */
	T insertCascade(T entity);

	/**
	 * 更新记录(无级联)
	 * 
	 * @param entity
	 *            要更新的对象模板
	 * @return 影响记录行数
	 */
	int update(T entity);

	/**
	 * 更新记录
	 * 
	 * @param entity
	 *            要更新的对象模板
	 * @return 影响记录行数
	 */
	int updateCascade(T entity);

	/**
	 * 删除记录（注意，根据入参Query中的条件可以删除多条记录） 无级联操作 ，如果要使用带级联操作的remove方法，可以使用
	 * {@link #removeCascade}
	 * 
	 * @param entity
	 *            要删除的对象模板
	 * @return 影响记录行数
	 */
	int remove(T entity);

	/**
	 * 删除记录（注意，根据入参Query中的条件可以删除多条记录）
	 * 
	 * @param entity
	 *            要删除的对象模板
	 * @return 影响记录行数
	 */
	int removeCascade(T entity);

	/**
	 * 持久化一条记录，(如果记录存在则update，否则执行insert)
	 * 
	 * @param entity
	 *            要写入数据库的对象
	 * @return 被写入的数据库的对象
	 */
	T merge(T entity);

	/**
	 * 载入一条记录(无级联)
	 * 
	 * @param entity
	 *            查询对象模板
	 * @param unique
	 *            是否要求结果唯一
	 * @return 查询结果
	 */
	T load(T entity, boolean unique);

	/**
	 * 载入一条记录(带级联)
	 * 
	 * @param entity
	 *            查询对象模板
	 * @param unique
	 *            是否要求结果唯一
	 * @return 查询结果
	 * @since 1.7.0
	 * 
	 */
	T loadCascade(T entity, boolean unique);

	/**
	 * 根据示例的对象删除记录
	 * 
	 * @param entity
	 *            删除的对象模板
	 * @return 影响记录行数
	 */
	int removeByExample(T entity);

	/**
	 * 当确定主键为单对象时，根据主键加载一个对象
	 * 
	 * @param key
	 *            主键
	 * @return 根据主键加载的记录
	 */
	T get(Serializable key);

	/**
	 * 查找记录(不带级联)
	 * 
	 * @param Entity
	 *            查询对象模板
	 * @return 查询结果
	 */
	List<T> find(T Entity);

	/**
	 * 查找记录(带级联)
	 * 
	 * @param Entity
	 *            查询对象模板
	 * @return 查询结果
	 */
	List<T> findCascade(T Entity);

	/**
	 * 得到整张表的全部数据。(单表操作，不带级联)
	 * 
	 * @return 查询结果
	 */
	List<T> getAll();

	/**
	 * 根据设置过值的字段进行查找
	 * 
	 * @param entity
	 *            查询的对象模板
	 * @return 查询结果
	 */
	List<T> findByExample(T entity);

	/**
	 * 查找记录
	 * 
	 * @param query
	 *            查询请求
	 * @return 查询结果
	 */
	List<T> find(jef.database.query.Query<T> query);

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
	List<T> find(Class<T> entity, String query, Object... param);

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
	Page<T> findAndPage(T entity, int start, int limit);

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
	Page<T> findAndPageCascade(T entity, int start, int limit);

	/**
	 * 批量插入
	 * 
	 * @param entities
	 *            要插入的对象列表
	 * @return 影响记录条数
	 */
	int batchInsert(List<T> entities);

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
	int batchInsert(List<T> entities, boolean doGroup);

	/**
	 * 批量删除
	 * 
	 * @param entities
	 *            要删除的对象列表
	 * @return 影响的记录条数
	 */
	int batchRemove(List<T> entities);

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
	int batchRemove(List<T> entities, boolean doGroup);

	/**
	 * 批量（按主键）更新
	 * 
	 * @param entities
	 *            要更新的记录
	 * @return 影响的记录条数
	 */
	int batchUpdate(List<T> entities);

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
	int batchUpdate(List<T> entities, boolean doGroup);

	/**
	 * 使用命名查询查找. {@linkplain NamedQueryConfig 什么是命名查询}
	 * 
	 * @param nqName
	 *            命名查询的名称
	 * @param param
	 *            绑定变量参数
	 * @return 查询结果
	 */
	List<T> findByNq(String nqName, Map<String, Object> param);

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
	Page<T> findAndPageByNq(String nqName, Map<String, Object> param, int start, int limit);

	/**
	 * 执行命名查询. {@linkplain NamedQueryConfig 什么是命名查询}
	 * 
	 * @param nqName
	 *            命名查询的名称
	 * @param param
	 *            绑定变量参数
	 * @return 影响记录行数
	 */
	int executeNq(String nqName, Map<String, Object> param);

	/**
	 * 执行指定的SQL语句 这里的Query可以是insert或update，或者其他DML语句
	 * 
	 * @param sql
	 *            SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * @param param
	 *            绑定变量参数
	 * @return 影响记录行数
	 */
	int executeQuery(String sql, Map<String, Object> param);

	/**
	 * 根据指定的SQL查找
	 * 
	 * @param sql
	 *            SQL语句,可使用 {@linkplain NativeQuery 增强的SQL (参见什么是E-SQL条目)}。
	 * @param param
	 *            绑定变量参数
	 * @return 查询结果
	 */
	List<T> findByQuery(String sql, Map<String, Object> param);

	/**
	 * 根据单个的字段条件查找结果(仅返回第一条)
	 * 
	 * @param field
	 *            条件字段名
	 * @param value
	 *            条件字段值
	 * @return 符合条件的结果。如果查询到多条记录，也只返回第一条
	 */
	T loadByField(String field, Serializable value, boolean unique);
	
	   
    /**
     * 悲观锁更新 使用此方法将到数据库中查询一条记录并加锁，然后用Update的回调方法修改查询结果。 最后写入到数据库中。
     * 
     * @return 如果没查到数据，或者数据没有发生任何变化，返回false
     */
    boolean lockItAndUpdate(Serializable id, Update<T> update);   
    

	/**
	 * 根据单个的字段条件查找结果
	 * 
	 * @param field
	 *            条件字段名
	 * @param value
	 *            条件字段值
	 * @return 符合条件的结果
	 */
	List<T> findByField(String field, Serializable value);

	/**
	 * 根据单个的字段条件删除记录
	 * 
	 * @param field
	 *            条件字段名
	 * @param value
	 *            条件字段值
	 * @return 删除的记录数
	 */
	int deleteByField(String field, Serializable value);

	/**
	 * 按个主键的值读取记录 (只支持单主键，不支持复合主键)
	 * 
	 * @param pkValues
	 * @return
	 */
	List<T> batchLoad(List<? extends Serializable> pkValues);

	/**
	 * 按主键批量删除 (只支持单主键，不支持复合主键)
	 * 
	 * @param pkValues
	 * @return
	 */
	int batchDelete(List<? extends Serializable> pkValues);

	/**
	 * 根据单个字段的值读取记录（批量）
	 * 
	 * @param field
	 *            条件字段
	 * @param values
	 *            查询条件的值
	 * @return 符合条件的记录
	 */
	List<T> batchLoadByField(String field, List<? extends Serializable> values);

	/**
	 * 根据单个字段的值删除记录（批量）
	 * 
	 * @param field
	 * @param values
	 * @return
	 */
	int batchDeleteByField(String field, List<? extends Serializable> values);

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
	RecordsHolder<T> selectForUpdate(T query);

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
	RecordHolder<T> loadForUpdate(T obj);
	   
    /**
     * 获得一个QueryDSL查询对象
     * @return SQLQuery
     * @see SQLQuery
     */
    SQLQuery sql();
}
