package com.github.geequery.springdata.test.repo;

import java.awt.print.Pageable;
import java.util.Date;
import java.util.List;

import jef.common.wrapper.Page;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.query.Param;

import com.github.geequery.springdata.annotation.Modifying;
import com.github.geequery.springdata.annotation.Query;
import com.github.geequery.springdata.test.entity.Foo;

/**
 * 此处适应Spring-date-JPA中的自定义查询方式 后续考虑增加一个注解可不依赖Spring-data-jpa
 * Query换成spring-JPA中的Query。
 * @author Administrator
 *
 */
@RepositoryDefinition(domainClass = Foo.class, idClass = Integer.class)
public interface FooDao2 {

	/**
	 * @Query(value = "select * from foo u where u.name like ?1", nativeQuery = true)
	 * @param username
	 * @return
	 */
	@Query(value = "select * from foo u where u.name like ?1", nativeQuery = true)
	public Foo findByusername(String username);

	/**
	 * @Query("select * from foo u where u.name=:name")
	 * 
	 * @param name
	 * @return
	 */
	@Query("select * from foo u where u.name=:name")
	public Foo findBysName(@Param("name") String name);

	/**
	 * @Query("select * from foo where name like :name and age=:age")
	 * @param birthDay
	 * @param name
	 * @return
	 */
	@Query(name = "selectByNameAndBirthDay")
	public List<Foo> findBySql(@Param("birth") Date birthDay, @Param("name") String name);

	/**
	 * @Query("select * from foo where name like :name and age=:age")
	 * @param name
	 * @param birthDay
	 * @return
	 */
	@Query(name = "selectByNameAndBirthDay",nativeQuery=true)
	public List<Foo> findBySql2(String name, Date birthDay);

	/**
	 * @Query("select * from foo where name like :name and age=:age")
	 * @param name
	 * @param birthDay
	 * @return
	 */
	@Query("select * from foo where name like :name and age=:age")
	public List<Foo> findBySql3(String name, Date birthDay);

	/**
	 * @Query("select * from foo where name like :name and age=:age")
	 * @param birthDay
	 * @param name
	 * @return
	 */
	@Query("select * from foo where name like :name and age=:age")
	public List<Foo> findBySql4(Date birthDay, String name);
	
	/**
	 * @Query("select * from foo where name like :name and age=:age")
	 * @param birthDay
	 * @param name
	 * @param page
	 * @return
	 */
	@Query("select * from foo where name like :name and age=:age")
	public Page<Foo> findBySql5(Date birthDay, String name,Pageable page);
	
	/**
	 * @Query("select * from foo")
	 * @param age
	 * @param name
	 * @param sort
	 * @return
	 */
	@Query("select * from foo")
	public Page<Foo> findBySql6(int age, String name,Sort sort);
	
	/**
	 * (value="select * from foo where age=?1 and name like ?2",nativeQuery=true) 
	 * @param age
	 * @param name
	 * @param sort
	 * @return
	 */
	@Query(value="select * from foo where age=?1 and name like ?2",nativeQuery=true)
	public Page<Foo> findBySql7(int age, String name,Sort sort);
	
	/**
	 * insert into foo(remark,name,age,birthday) values (:remark,:name,:age,:birthday)
	 * @param name
	 * @param age
	 * @param remark
	 * @param birthDay
	 */
	@Modifying
	@Query("insert into foo(remark,name,age,birthday) values (:remark,:name,:age,:birthday)")
	public void insertInto(String name, int age,String remark,Date birthDay);
	
	/**
	 * update foo set age=age+1,birthDay=:birth where age=:age and id=:id
	 * @param birthDay
	 * @param age
	 * @param id
	 */
	@Modifying
	@Query("update foo set age=age+1,birthDay=:birth where age=:age and id=:id")
	public void updateFooSetAgeByAgeAndId(Date birthDay,int age, int id);
	
}