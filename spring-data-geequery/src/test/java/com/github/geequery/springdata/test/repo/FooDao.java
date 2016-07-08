package com.github.geequery.springdata.test.repo;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.github.geequery.springdata.repository.Query;
import com.github.geequery.springdata.test.entity.Foo;

public interface FooDao extends Repository<Foo, Integer> {
	public Foo save(Foo foo);

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
	public Foo findByName(@Param("name") String name);
}