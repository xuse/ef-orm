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

import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import com.github.geequery.springdata.annotation.Query;
import com.github.geequery.springdata.repository.support.Update;

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
	 * @param data
	 *            查询请求。
	 *            <ul>
	 *            <li>如果设置了Query条件，按query条件查询。 否则——</li>
	 *            <li>如果设置了主键值，按主键查询，否则——</li>
	 *            <li>按所有设置过值的字段作为条件查询。</li>
	 *            </ul>
	 * @return 结果
	 */
	List<T> find(T data);

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
	 * @param entity
	 * @param unique true表示结果必须唯一，false则允许结果不唯一仅获取第一条记录
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
}
