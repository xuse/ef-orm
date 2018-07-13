/*
 * Copyright 2008-2016 the original author or authors.
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
package com.github.geequery.springdata.repository;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;

import jef.database.NamedQueryConfig;
import jef.database.NativeQuery;
import jef.database.query.ConditionQuery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import com.github.geequery.springdata.annotation.Query;
import com.github.geequery.springdata.repository.support.Update;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;

/**
 * GQ specific extension of
 * {@link org.springframework.data.repository.Repository}.
 *
 * 
 * @author Jiyi
 */
@NoRepositoryBean
public interface GqRepository<T, ID extends Serializable> extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T> {
    /**
     * Deletes the given entities in a batch which means it will create a single
     * {@link Query}. Assume that we will clear the
     * {@link javax.persistence.EntityManager} after the call.
     * 
     * @param entities
     */
    void deleteInBatch(Iterable<T> entities);

    /**
     * Deletes all entities in a batch call.
     */
    void deleteAllInBatch();

    /**
     * Returns a reference to the entity with the given identifier.
     * 
     * @param id
     *            must not be {@literal null}.
     * @return a reference to the entity with the given identifier.
     * @see EntityManager#getReference(Class, Object)
     */
    T getOne(ID id);

    /**
     * Returns Object equals the example
     * 
     * @param example
     *            样例对象
     * @param fields
     *            哪些字段作为查询条件参与
     * @return
     */
    List<T> findByExample(T example, String... fields);

    /**
     * 查询列表
     * 
     * @param query
     *            查询请求。
     *            <ul>
     *            <li>如果设置了Query条件，按query条件查询。 否则——</li>
     *            <li>如果设置了主键值，按主键查询，否则——</li>
     *            <li>按所有设置过值的字段作为条件查询。</li>
     *            </ul>
     * @return 结果
     */
    List<T> find(T query);
    
    /**
     * 根据查询请求查询
     * @param query 请求
     * @param sort 排序
     * @return 结果
     */
    List<T> find(T query, Sort sort);
    
    /**
     * 根据查询请求查询
     * @param query 请求
     * @param pageable 分页
     * @return 结果
     */
    Page<T> find(T query, Pageable pageable);

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
    T load(T data);

    /**
     * 根据查询查询一条记录
     * 
     * @param entity
     * @param unique
     *            true表示结果必须唯一，false则允许结果不唯一仅获取第一条记录
     * @return 查询结果
     * @throws NonUniqueResultException
     *             结果不唯一
     */
    T load(T entity, boolean unique);

    /**
     * 悲观锁更新 使用此方法将到数据库中查询一条记录并加锁，然后用Update的回调方法修改查询结果。 最后写入到数据库中。
     * 
     * @return 如果没查到数据，或者数据没有发生任何变化，返回false
     */
    boolean lockItAndUpdate(ID id, Update<T> update);

    /**
     * 合并记录 
     * @param entity
     * @return
     */
    T merge(T entity);
    
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
     * 根据示例的对象删除记录
     * 
     * @param entity
     *            删除的对象模板
     * @return 影响记录行数
     */
    int removeByExample(T entity);
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
     * 获得一个QueryDSL查询对象
     * @return SQLQuery
     * @see SQLQuery
     */
    SQLQueryFactory sql();
    

	/**
	 * Returns a single entity matching the given {@link ConditionQuery}.
	 * 
	 * @param spec
	 * @return
	 */
	T load(ConditionQuery spec);

	/**
	 * Returns all entities matching the given {@link ConditionQuery}.
	 * 
	 * @param spec
	 * @return
	 */
	List<T> find(ConditionQuery spec);

	/**
	 * Returns a {@link Page} of entities matching the given {@link ConditionQuery}.
	 * 
	 * @param spec
	 * @param pageable
	 * @return
	 */
	Page<T> find(ConditionQuery spec, Pageable pageable);

	/**
	 * Returns all entities matching the given {@link ConditionQuery} and {@link Sort}.
	 * 
	 * @param spec
	 * @param sort
	 * @return
	 */
	List<T> find(ConditionQuery spec, Sort sort);

	/**
	 * Returns the number of instances that the given {@link ConditionQuery} will return.
	 * 
	 * @param spec the {@link Specification} to count instances for
	 * @return the number of instances
	 */
	long count(ConditionQuery spec);
}
