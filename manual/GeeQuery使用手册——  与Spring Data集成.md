GeeQuery使用手册——  与Spring Data集成

[TOC]

# 与Spring Data集成

GeeQuery Release1.11.0支持使用Spring Data来操作数据库。Spring Data是一个完全统一的API，旨在统一和简化对各类型持久化存储， 而不拘泥于是关系型数据库还是NoSQL 数据存储。

Spring通过为用户统一创建和销毁EntityManager，进行事务管理，简化JPA的配置等使用户的开发简便。

Spring Data 是在Spring 的基础上，对持久层做了简化。用户只需声明持久层的接口，不需要实现该接口。Spring Data内部会根据不同的策略、通过不同的方法创建Query操作数据库。

相比而言，Spring Data**更加简洁**，主要针对的就是 Spring 唯一没有简化到的业务逻辑代码，至此，开发者连仅剩的实现持久层业务逻辑的工作都省了，唯一要做的，就只是声明持久层的接口。

## 配置

~~~xml
	<!-- 定义DataSource -->
	<bean id="dataSource" class="jef.database.datasource.SimpleDataSource"
		p:url="${db.url}"
		p:user="${db.user}"
		p:password="${db.password}" 
		/>

	<!-- 定义GeeQuery SessionFactory -->
	<bean id="entityManagerFactory" class="org.easyframe.enterprise.spring.SessionFactoryBean"
		p:dataSource-ref="dataSource" p:packagesToScan="com.github.geequery.springdata.test.entity"
		p:registeNonMappingTableAsDynamic="true" p:useSystemOut="true" />

	<!-- 定义事务和事务切面 -->
	<bean id="transactionManager" class="org.springframework.orm.jpa.JpaTransactionManager">
		<property name="entityManagerFactory" ref="entityManagerFactory" />
		<property name="jpaDialect">
			<bean class="org.easyframe.enterprise.spring.JefJpaDialect" />
		</property>
	</bean>
	<tx:annotation-driven transaction-manager="transactionManager"
		proxy-target-class="true" />
	<aop:aspectj-autoproxy />

	<!-- 【醒目】 这就是：Spring Data 的配置，简单吧 -->
	<gq:repositories base-package="com.github.geequery.springdata.test.repo"
		entity-manager-factory-ref="entityManagerFactory"
		transaction-manager-ref="transactionManager">
	</gq:repositories>
~~~

上面这段配置，数据源、GeeQuery SessionFactory、TransactionManager、基于注解的事务声明、Spring Data都有了。

## 使用

Spring Data简化持久层开发大致步骤如下：

1.  **声明持久层接口**，该接口继承Repository\<T, ID>或其子接口，其中T是领域实体类型，ID是领域实体的主键类型，例子如下：

~~~java
public interface FooDao extends GqRepository<Foo, Integer>
~~~

上述使用的主键是只有一个字段，当主键是复合类型的时候，则需如下：

~~~java
public interface ComplexFooDao extends GqRepository<ComplexFoo, int[]>
~~~

具体接口的声明，根据不同的实体类型和主键类型进行即可。

此外，还有一种编写Dao的方法，使用注解的方法，如下：

~~~java
@RepositoryDefinition(domainClass = Foo.class, idClass = Integer.class)
public interface FooEntityDao 
~~~

但需要注意的是，使用注解法会使Dao缺少来自父类的现成方法。

2. **在持久层的接口中声明需要的业务方法**(Dao层方法)

~~~java
public interface FooDao extends GqRepository<Foo, Integer> {
  	List<Foo> findByNameContainsAndAge(String name, int age);
}
~~~

Spring Data将会根据指定的策略为该方法生成实现代码，用户不需要实现该接口，直接使用即可。



## Repository的内置方法

Repository是SpringData的核心接口，它并不提供任何方法，用户需要自己定义需要的方法。

其他Repository接口

| Repository                               | 提供                                       |
| ---------------------------------------- | ---------------------------------------- |
| CrudRepository(Spring Data提供)            | 继承Repository，提供增删改查方法，可以直接调用。            |
| PagingAndSortingRepository(Spring Data提供) | 继承CrudRepository，增加了分页查询和排序两个方法          |
| JpaRepository(Spring Data JPA提供)         | 继承PagingAndSortingRepository，是针对JPA技术的接口，提供flush()，saveAndFlush()，deleteInBatch()等方法 |
| GqRepository(GeeQuery Spring Data )      | 继承PagingAndSortingRepository，提供deleteInBatch()、merge()、悲观锁更新lockItAndUpdate() 等方法 |

Spring Data的repository允许用户自定义操作数据库的方法。用户可以与原有的repository结合起来使用。

GqRepository的内置方法如下：

~~~java
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
    SQLQuery sql();
~~~

### 各种Repository的使用策略

1. 如果持久层接口较多，且每一个接口都需要声明相似的增删改查方法，直接继承 Repository 就显得有些啰嗦，这时可以继承 CrudRepository，它会自动为域对象创建增删改查方法，供业务层直接使用。开发者只是多写了 "Crud" 四个字母，即刻便为域对象提供了开箱即用的十个增删改查方法。

> *使用 CrudRepository 也有副作用，它可能暴露了你不希望暴露给业务层的方法。比如某些接口你只希望提供增加的操作而不希望提供删除的方法。针对这种情况，开发者只能退回到 Repository 接口，然后到 CrudRepository 中把希望保留的方法声明复制到自定义的接口中即可.*

2. 分页查询和排序是持久层常用的功能，Spring Data 为此提供了 PagingAndSortingRepository 接口，它继承自 CrudRepository 接口，在 CrudRepository 基础上新增了两个与分页有关的方法。但是，我们很少会将自定义的持久层接口直接继承自 PagingAndSortingRepository，而是在继承 Repository 或 CrudRepository 的基础上，在自己声明的方法参数列表最后增加一个 Pageable 或 Sort 类型的参数，用于指定分页或排序信息即可，这比直接使用 PagingAndSortingRepository 提供了更大的灵活性。
3. JpaRepository 是继承自 PagingAndSortingRepository 的针对 JPA 技术提供的接口，它在父接口的基础上，提供了其他一些方法，比如 flush()，saveAndFlush()，deleteInBatch() 等。如果有这样的需求，则可以继承该接口。
4. GqRepository是继承自PagingAndSortingRepository和QueryByExampleExecutor的接口，它在付接口的基础上提供了其他一些方法，比如deleteInBatch()、merge()、悲观锁更新lockItAndUpdate() 等方法。



## Repository的扩展方法

GqRepository的扩展方法如下：

~~~java
	/**
	 * 此处是非Native方式，即E-SQL方式
	 * 
	 * @param name
	 * @return
	 */
	Foo findByName(@Param("name") @IgnoreIf(ParamIs.Empty) String name);

	List<Foo> findByNameLike(@Param("name") String name);
	
	int countByNameLike(@Param("name") String name);

	List<Foo> findByNameContainsAndAge(String name, int age);

	List<Foo> findByNameStartsWithAndAge(@Param("age") int age, @Param("name") String name);

	
	@FindBy(value={
	        @Condition("name"),
	        @Condition("age"),
	        @Condition("remark"),
	        @Condition("birthDay"),
	        @Condition("indexCode"),
	        @Condition("lastModified")
	},orderBy="name desc",type=Logic.OR)
	List<Foo> findByWhat(String name,int age,String term, Date birthDay, String indexCode,Date lastModified);
	
	
//	   @FindBy({
//	       @Condition(Foo.Field.name),
//	       @Condition(Foo.Field.age),
//	       @Condition(Foo.Field.remark),
//	       @Condition(Foo.Field.birthDay),
//	       @Condition(Foo.Field.indexCode),
//	       @Condition(value=Foo.Field.lastModified,op=Operator.GREAT)
//   })
//   List<Foo> findByWhat2(String name,int age,String term, Date birthDay, String indexCode,Date lastModified);
	
	/**
	 * 根据Age查找
	 * 
	 * @param age
	 * @return
	 */
	List<Foo> findByAgeOrderById(int age);

	/**
	 * 根据Age查找并分页
	 * 
	 * @param age
	 * @param page
	 * @return
	 */
	Page<Foo> findByAgeOrderById(int age, Pageable page);
	
	
	Page<Foo> findByAge(int age, Pageable page);
	   

	/**
	 * 使用in操作符
	 * 
	 * @param ages
	 * @return
	 */
	List<Foo> findByAgeIn(Collection<Integer> ages);
	
//	List<Foo> updateAgeById(int age,int id);
~~~

扩展方法可根据不同的实体类型进行个性化的设置。

## 不同的GeRepository操作

### 分页和排序

在分页操作中，GqRepository是继承PagingAndSortingRepository，PagingAndSortingRepository接口中有两个方法，具体如下：

~~~java
    Iterable<T> findAll(Sort var1);

    Page<T> findAll(Pageable var1);
~~~

在GqRepository扩展方法中，有一个分页和排序方法，具体如下：

~~~java
	/**
	 * 根据Age查找并分页
	 * 
	 * @param age
	 * @param page
	 * @return
	 */
	Page<Foo> findByAgeOrderById(int age, Pageable page);
~~~

这个是根据年龄进行查找，并分页，为了将结果限定的更加准确，我们会通过一些条件来限定返回结果，通常是进行排序，如根据时间的先后顺序返回等等。这个业务方法直接添加在相应的Dao层即可直接使用。

在这里是根据Id进行排序（实际上是固定排序），具体使用实例如下：

~~~java
// ==============使用分页，固定排序===========
{
    System.out.println("=== FindByAge Page ===");
    Page<Foo> fooPage = foodao.findByAgeOrderById(0, new PageRequest(1, 4));
    System.out.println(fooPage.getTotalElements());
    System.out.println(Arrays.toString(fooPage.getContent().toArray()));
}
~~~

第二种方式是通过分页并传入排序参数

~~~java
// ==============分页+传入排序参数===========
{
	System.out.println("=== FindAll(page+sort) ===");
	Page<Foo> p = foodao.findAll(new PageRequest(0, 3, new Sort(new Order(Direction.DESC, "age"))));
	System.out.println(p.getTotalElements());
	System.out.println(Arrays.toString(p.getContent().toArray()));
}
~~~

第三种方式是不分页，并传入排序参数

~~~java
// ===================不分页，传入排序参数===========================
{
	System.out.println("=== FindAll(sort) ===");
	Iterable<Foo> iters = foodao.findAll(new Sort(new Order(Direction.DESC, "id")));
	System.out.println("list=" + iters);
}
~~~



### 更新和删除

更新和删除只能用继承自GqRepository的save()、deleteXXX()等方法。

GqRepository中增加的更新操作的方法如下：

~~~java
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
~~~

注意：GqRepository也有很多继承的update方法，可供使用。

例子如下：

```java
//从数据库查询这条记录
Foo foo = foodao.findByName("张三");
// 更新这条记录
foo.setName("EF-ORM is very simple.");
dao.update(foo);
```

GqRepository中增加的删除操作的方法例子如下：

~~~java
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
~~~

注意，GqRepository也有很多继承的删除方法，可供使用。

具体删除的实例

~~~java
{
	// 删除全部
	System.out.println("=== DeleteAll() ===");
	foodao.deleteAll();
}
~~~



### 创建Query的三种方式

在Spring Data JPA中除了提供通过解析方法名的方式来创建Query之外，还提供了@Query和NamedQueries两种方法。

1. 解析方法名的方式

框架在进行方法名解析时，会先把方法名多余的前缀截取掉，比如 find、findBy、read、readBy、get、getBy，然后对剩下部分进行解析。并且如果方法的最后一个参数是 Sort 或者 Pageable 类型，也会提取相关的信息，以便按规则进行排序或者分页查询。

在创建查询时，我们通过在方法名中使用属性名称来表达，比如 findByUserAddressZip ()。框架在解析该方法时，首先剔除 findBy，然后对剩下的属性进行解析，详细规则如下（此处假设该方法针对的域对象为 AccountInfo 类型）：

- 先判断 userAddressZip （根据 POJO 规范，首字母变为小写，下同）是否为 AccountInfo 的一个属性，如果是，则表示根据该属性进行查询；如果没有该属性，继续第二步；
- 从右往左截取第一个大写字母开头的字符串（此处为 Zip），然后检查剩下的字符串是否为 AccountInfo 的一个属性，如果是，则表示根据该属性进行查询；如果没有该属性，则重复第二步，继续从右往左截取；最后假设 user 为 AccountInfo 的一个属性；
- 接着处理剩下部分（ AddressZip ），先判断 user 所对应的类型是否有 addressZip 属性，如果有，则表示该方法最终是根据 "AccountInfo.user.addressZip" 的取值进行查询；否则继续按照步骤 2 的规则从右往左截取，最终表示根据 "AccountInfo.user.address.zip" 的值进行查询。

在查询时，通常需要同时根据多个属性进行查询，且查询的条件也格式各样（大于某个值、在某个范围等等），Spring Data JPA 为此提供了一些表达条件查询的关键字，大致如下：

| **Keyword**       | **Sample**                           | **JPQL snippet**                         |
| ----------------- | ------------------------------------ | ---------------------------------------- |
| And               | findByLastnameAndFirstname           | … where x.lastname = ?1 and x.firstname = ?2 |
| Or                | findByLastnameOrFirstname            | … where x.lastname = ?1 or x.firstname = ?2 |
| Between           | findByStartDateBetween               | … where x.startDate between 1? and ?2    |
| LessThan          | findByAgeLessThan                    | … where x.age < ?1                       |
| GreaterThan       | findByAgeGreaterThan                 | … where x.age > ?1                       |
| IsNull            | findByAgeIsNull                      | … where x.age is null                    |
| IsNotNull,NotNull | findByAge(Is)NotNull                 | … where x.age not null                   |
| Like              | findByFirstnameLike                  | … where x.firstname like ?1              |
| NotLike           | findByFirstnameNotLike               | … where x.firstname not like ?1          |
| OrderBy           | findByAgeOrderByLastnameDesc         | … where x.age = ?1 order by x.lastname desc |
| Not               | findByLastnameNot                    | … where x.lastname <> ?1                 |
| In                | findByAgeIn(Collection\<Age> ages)   | … where x.age in ?1                      |
| NotIn             | findByAgeNotIn(Collection\<Age> age) | … where x.age not in ?1                  |

2. 使用@Query

@Query 注解的使用非常简单，只需在声明的方法上面标注该注解，可以在自定义的查询方法上使用@Query来指定该方法要执行的查询语句，比如：



3. NamedQueries











