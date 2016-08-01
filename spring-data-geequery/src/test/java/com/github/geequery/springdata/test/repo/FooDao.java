package com.github.geequery.springdata.test.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.github.geequery.springdata.annotation.IgnoreIf;
import com.github.geequery.springdata.annotation.ParamIs;
import com.github.geequery.springdata.repository.GqRepository;
import com.github.geequery.springdata.test.entity.Foo;

public interface FooDao extends GqRepository<Foo, Integer> {
	/**
	 * 此处是非Native方式，即E-SQL方式
	 * 
	 * @param name
	 * @return
	 */
	public Foo findByName(@Param("name") @IgnoreIf(ParamIs.Empty) String name);

	public List<Foo> findByNameLike(@Param("name") String name);

	public List<Foo> findByNameContainsAndAge(String name, int age);

	public List<Foo> findByNameStartsWithAndAge(@Param("age") int age, @Param("name") String name);

	/**
	 * 根据Age查找
	 * 
	 * @param age
	 * @return
	 */
	public List<Foo> findByAgeOrderById(int age);

	/**
	 * 根据Age查找并分页
	 * 
	 * @param age
	 * @param page
	 * @return
	 */
	public Page<Foo> findByAgeOrderById(int age, Pageable page);

	/**
	 * 使用in操作符
	 * 
	 * @param ages
	 * @return
	 */
	public List<Foo> findByAgeIn(Collection<Integer> ages);
}