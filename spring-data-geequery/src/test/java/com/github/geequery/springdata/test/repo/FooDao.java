package com.github.geequery.springdata.test.repo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.github.geequery.springdata.annotation.Query;
import com.github.geequery.springdata.repository.GqRepository;
import com.github.geequery.springdata.test.entity.Foo;

public interface FooDao extends GqRepository<Foo,Integer> {

	/**
	 * 此处适应Spring-date-JPA中的自定义查询方式 后续考虑增加一个注解可不依赖Spring-data-jpa
	 * 
	 * @param username
	 * @return
	 */
	@Query(value = "select * from foo u where u.name like ?1", nativeQuery = true)
	public Foo findByusername(String username);

	/**
	 * 此处是非Native方式，即E-SQL方式
	 * @param name
	 * @return
	 */
	@Query("select * from foo u where u.name=:name")
	public Foo findBysName(@Param("name") String name);
	

	/**
	 * 此处是非Native方式，即E-SQL方式
	 * @param name
	 * @return
	 */
	public Foo findByName(@Param("name") String name);
	
	
	/**
	 * 根据Age查找
	 * @param age
	 * @return
	 */
	public List<Foo> findByAgeOrderById(int age);
	
	/**
	 * 根据Age查找并分页
	 * @param age
	 * @param page
	 * @return
	 */
	public Page<Foo> findByAgeOrderById(int age,Pageable page);
	
	
}