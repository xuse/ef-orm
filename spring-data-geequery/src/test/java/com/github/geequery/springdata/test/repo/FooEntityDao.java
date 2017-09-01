package com.github.geequery.springdata.test.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.query.Param;

import com.github.geequery.springdata.annotation.IgnoreIf;
import com.github.geequery.springdata.annotation.Modifying;
import com.github.geequery.springdata.annotation.ParamIs;
import com.github.geequery.springdata.annotation.Query;
import com.github.geequery.springdata.test.entity.Foo;

/**
 * 此处适应Spring-date-JPA中的自定义查询方式 后续考虑增加一个注解可不依赖Spring-data-jpa
 * Query换成spring-JPA中的Query。
 * 
 * @author Administrator
 *
 */
@RepositoryDefinition(domainClass = Foo.class, idClass = Integer.class)
public interface FooEntityDao {

	/**
	 * @Query(value = "select * from foo u where u.name like ?1", nativeQuery =
	 *              true)
	 * @param username
	 * @return
	 */
	@Query(value = "select * from foo u where u.name like ?1<string$>",nativeQuery=false)
	public Foo findByusername(String username);

	/**
	 * @Query("select * from foo u where u.name=:name")
	 * 
	 * @param name
	 * @return
	 */
	@Query(value="select * from foo u where u.name=:name",nativeQuery=false)
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
	 * @Query("select * from foo where name like ?1 and birthday=?2")
	 * @param name
	 * @param birthDay
	 * @return
	 */
	@Query(name = "selectByNameAndBirthDay2", nativeQuery = true)
	public List<Foo> findBySql2(String name, Date birthDay);

	/**
	 * @Query("select * from foo where name like :name and age=:age")
	 * @param name
	 * @param birthDay
	 * @return
	 */
	@Query(value = "select * from foo where name like :name and age=:age",nativeQuery=false)
	public Foo findBySql3(@Param("name") String name, @Param("age") int age);

	/**
	 * @Query("select * from foo where name like :name and age=:age")
	 * @param birthDay
	 * @param name
	 * @return
	 */
	@Query(value="select * from foo where name like ?2<string$> and age=?1",nativeQuery=false)
	public Foo findBySql4(int birthDay, String name);

	/**
	 * @Query("select * from foo where name like :name and age=:age")
	 * @param birthDay
	 * @param name
	 * @param page
	 * @return
	 */
	@Query("select * from foo where name like :name and age=:age")
	public Page<Foo> findBySql5(@Param("age") @IgnoreIf(ParamIs.Zero) int age, @Param(value = "name") @IgnoreIf(ParamIs.Null) String name, Pageable page);

	/**
	 * @Query("select * from foo where age=?1 and name like ?2<$string$>")
	 * @param age
	 * @param name
	 * @param sort
	 * @return
	 */
	@Query(value="select * from foo where age=?1 and name like ?2<$string$>",nativeQuery=false)
	public List<Foo> findBySql6(int age, String name, Sort sort);

	@Query(value="select * from foo where age=?1 and name like ?2<$string$> order by ?3<sql>"
			,nativeQuery=false)
	public List<Foo> findBySql62(@IgnoreIf(ParamIs.Zero) int age, @IgnoreIf(ParamIs.Empty) String name, String orderField);

	/**
	 * (value="select * from foo where age=?1 and name like ?2",nativeQuery=true
	 * )
	 * 
	 * @param age
	 * @param name
	 * @param sort
	 * @return
	 */
	@Query(value = "select * from foo where age=?1 and name like ?2<$string$>",nativeQuery=false)
	public Page<Foo> findBySql7(int age, @IgnoreIf(ParamIs.Empty) String name, Pageable page);

	/**
	 * insert into foo(remark,name,age,birthday) values
	 * (:remark,:name,:age,:birthday)
	 * 
	 * @param name
	 * @param age
	 * @param remark
	 * @param birthDay
	 */
	@Modifying
	@Query(value="insert into foo(remark,name,age,birthday) values (?3, ?1, ?2, ?4)",nativeQuery=false)
	public int insertInto(String name, int age, String remark, Date birthDay);

	@Modifying
	@Query(value="insert into foo(remark,name,age,birthday) values (:remark, :name, :age, :birthday)",nativeQuery=false)
	public int insertInto2(@Param("name") String name, @Param("age") int age, @Param("remark") String remark, @Param("birthday") Date birthDay);

	@Modifying
	@Query(value="update foo set age=age+1,birthDay=?1 where age=?2 and id=?3",nativeQuery=false)
	public int updateFooSetAgeByAgeAndId1(Date birth, int age, int id);
	   
	/**
	 * update foo set age=age+1,birthDay=:birth where age=:age and id=:id
	 * 
	 * @param birthDay
	 * @param age
	 * @param id
	 */
	@Modifying
	@Query(value="update foo set age=age+1,birthDay=:birth where age=:age and id=:id",nativeQuery=false)
	public int updateFooSetAgeByAgeAndId(@Param("birth") Date birth, @Param("age") int age, @Param("id") int id);
	
	


}