

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
| GqRepository(GeeQuery Spring Data )      | 继承PagingAndSortingRepository和QueryByExampleExecutor，提供deleteInBatch()、merge()、悲观锁更新lockItAndUpdate() 等方法 |

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

当然，还有GqRepository继承PagingAndSortingRepository和QueryByExampleExecutor的方法。

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

## 不同的GqRepository操作

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



### 删除

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



## 三种查询扩展方式

在Spring Data中除了提供通过解析方法名的方式来创建Query之外，还提供了@Query+语句和@Query+查询名两种方法。

### 解析方法名的方式

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

具体使用实例如下：

采用的方法如下：

~~~sql
Foo findByName(@Param("name") @IgnoreIf(ParamIs.Empty) String name);
~~~

这里的@Param和@IgnoreIf可以详见下文。


~~~java
// =============== 单字段查找 ==========
{
    System.out.println("=== FindByName ===");
    Foo foo = foodao.findByName("张三");
    System.out.println(foo.getName());
    System.out.println(foo.getId());
}
~~~

~~~sql
=== FindByName ===
select t.* from FOO t where t.NAME_A=? | [derby:db@1]
(1)nameEQUALS:   	[张三]
Result Count:1	 Time cost([ParseSQL]:50ms, [DbAccess]:52ms, [Populate]:3ms) max:2/fetch:0/timeout:60 |[derby:db@1]
Result:1:张三
张三
1
~~~

### 使用@Query+语句

@Query 注解的使用非常简单，只需在声明的方法上面标注该注解，可以在自定义的查询方法上使用@Query来指定该方法要执行的查询语句，比如：

如果要使用原生的查询，例子如下：

~~~java
@Query(value = "select * from foo u where u.name like ?1", nativeQuery =true)
public Foo findByusername(String username);
~~~

当不适用原生的查询,则需如下

~~~java
@Query(value = "select * from foo u where u.name like ?1<string$>",nativeQuery=false)
public Foo findByusername(String username);
~~~

在这里\<string$>表示在条件后加入%，如果\$在前面，对应的是在前面加入通配符%，以此类推。

注意：方法的参数个数必须和@Query里面需要的参数个数一致，如果是like，后面的参数需要前面或者后面加“%”。单纯的Like运算符不会在查询子条件 上增加通配符。因此需要自己传入通配符 %条件%

在这里，不使用原生的查询生成的查询语句如下：

~~~sql
select * from Foo u where u.NAME_A LIKE ? ESCAPE '/' | [derby:db@1]
(1):             	[张%]
Result Count:2	Time cost([DbAccess]:4ms, [Populate]:0ms | [derby:db@1]
1:张三
~~~

很多开发者在创建 JPQL 时喜欢使用命名参数来代替位置编号，@Query 也对此提供了支持。JPQL 语句中通过": 变量"的格式来指定参数，同时在方法的参数前面使用 @Param 将方法参数与 JPQL 中的命名参数对应，示例如下：

~~~java
@Query(value="select * from foo u where u.name=:name",nativeQuery=false)
public Foo findBysName(@Param("name") String name);
~~~

如果使用 @Param 对应 :name，那么方法的参数先后顺序可以随意修改。反之，如果是 ?1 ?2方式进行参数绑定，则方法参数顺序有要求。

~~~java
@Query(value="select * from foo where name like ?2<string$> and age=?1",nativeQuery=false)
public Foo findBySql4(int birthDay, String name);
~~~

对应的例子如下：

~~~java
{
  /**
  * 用?1 ?2绑定时，顺序要注意。 如果在SQL语句中指定LIKE的查询方式是 ‘匹配头部’，那么查询就能符合期望
   */
  System.out.println("=== findBySql4() ====");
  Foo foo = foodao2.findBySql4(0, "李");
  System.out.println(foo);
}
~~~

对应的查询语句：

~~~java
//=== findBySql4() ====
select * from Foo where NAME_A LIKE ? ESCAPE '/' AND AGE = ? | [derby:db@1]
(1):             	[李%]
(2):             	[0]
~~~

#### @Query中的表达式忽略

SQL语句中支持分页功能，默认情况下传入null不可以表示忽略对应的查询条件，即不能进行动态SQL的生成，但是使用@IgnoreIf()注解则可以忽略对应的查询条件。

~~~java
@Query("select * from foo where name like :name and age=:age")
public Page<Foo> findBySql5(@Param("age") @IgnoreIf(ParamIs.Zero) int age, @Param(value = "name") @IgnoreIf(ParamIs.Null) String name, Pageable page);
~~~

其中，IgnoreIf注解的接口

~~~java
/**
 * 用来描述参数为某个指定的值的时候，参数不设置（忽略）不作为查询条件或更新字段
 * 配合GeeQuery中的NativeQuery子句自动省略功能。
 */
@Target({ java.lang.annotation.ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreIf {
	ParamIs value() default ParamIs.Null;
}
~~~

接下来，我们可以观察findBySql5对应生成的SQL语句，如下：

~~~sql
select count(*) from foo | [derby:db@1] Count:15	Time cost([DbAccess]:11ms, [Populate]:0ms | [derby:db@1]
Count:15	 ([DbAccess]:11ms) |[derby:db@1]
select * from foo offset 4 row fetch next 4 rows only | [derby:db@1]
Result Count:4	Time cost([DbAccess]:3ms, [Populate]:1ms | [derby:db@1]
15
[5:张鑫, 6:测试, 7:张三丰, 8:李元吉]
~~~

当使用@IgnoreIf注解的时候，当条件为空时，可以自动省略条件，例子如下：

接口定义的方法如下：

~~~java
@Query(value = "select * from foo where age=?1 and name like ?2<$string$>",nativeQuery=false)
public Page<Foo> findBySql7(int age, @IgnoreIf(ParamIs.Empty) String name, Pageable page);
~~~

~~~java
page = foodao2.findBySql7(0, "", new PageRequest(3, 5));
System.out.println(page.getContent());
~~~

对应生成的查询语句如下：

~~~sql
select count(*) from Foo where AGE = ?  | [derby:db@1]
(1):             	[0] Count:15	Time cost([DbAccess]:2ms, [Populate]:1ms | [derby:db@1]
Count:15	 ([DbAccess]:3ms) |[derby:db@1]
~~~

这里，就忽略了name的参数，是因为name的赋值为“”。

此外，当如果条件中带有 % _等特殊符号，会自动转义，具体实例如下：

~~~java
Page<Foo> page = foodao2.findBySql7(0, "李%", new PageRequest(3, 5));
System.out.println(page.getContent());
~~~

对应生成的查询语句如下：

~~~java
select count(*) from Foo where AGE = ? AND NAME_A LIKE ? ESCAPE '/' | [derby:db@1]
(1):             	[0]
(2):             	[%李/%%] Count:0	Time cost([DbAccess]:4ms, [Populate]:0ms | [derby:db@1]
Count:0	 ([DbAccess]:4ms) |[derby:db@1]
~~~

#### @Query中的分页与排序实现

由于Query的特殊性——目前无法自由传入Sort对象进行排序。

~~~java
@Query(value="select * from foo where age=?1 and name like ?2<$string$>",nativeQuery=false)
public List<Foo> findBySql6(int age, String name, Sort sort);
~~~

~~~java
System.out.println("=== findBySql6() ====");
List<Foo> result = foodao2.findBySql6(0, "张", new Sort(new Order(Direction.DESC, "id")));
System.out.println(result);
~~~

生成的查询语句如下：

~~~sql
[main] WARN com.github.geequery.springdata.repository.query.GqNativeQuery - The input parameter Sort [id: DESC]can not be set into a SQL Query, and was ignored.
select * from Foo where AGE = ? AND NAME_A LIKE ? ESCAPE '/' | [derby:db@1]
(1):             	[0]
(2):             	[%张%]
Result Count:4	Time cost([DbAccess]:3ms, [Populate]:0ms | [derby:db@1]
[1:张三, 4:张昕, 5:张鑫, 7:张三丰]
~~~

注意，在这里，是按照默认升序的顺序排列的，降序的语句并没有起作用。

如果需要进行排序实现的话，有一个可变通的方法，直接指定属性进行降序排列即可，这个还是相当于改变SQL语句来达到按顺序来排列的效果。

SQL中能够支持分页与排序的功能，具体方法如下：

~~~java
@Query(value="select * from foo where age=?1 and name like ?2<$string$> order by ?3<sql>"
,nativeQuery=false)
public List<Foo> findBySql62(@IgnoreIf(ParamIs.Zero) int age, @IgnoreIf(ParamIs.Empty) String name, String orderField);
~~~

~~~java
System.out.println("=== findBySql6-2() ====");
result = foodao2.findBySql62(0, "张", "id desc");
System.out.println(result);
~~~

生成的SQL语句查询如下：

~~~sql
//=== findBySql6-2() ====
select * from Foo where  NAME_A LIKE ? ESCAPE '/' order by id desc | [derby:db@1]
(1):             	[%张%]
Result Count:4	Time cost([DbAccess]:4ms, [Populate]:0ms | [derby:db@1]
[7:张三丰, 5:张鑫, 4:张昕, 1:张三]
~~~

#### @Query中的插入

上述都是针对查询的@Query进行，当要生成更新类的Query语句，在@Query之前添加@Modifying即可。首先需要在接口中定义所需方法：

实现插入的方法：

~~~java
@Modifying
@Query(value="insert into foo(remark,name,age,birthday) values (?3, ?1, ?2, ?4)",nativeQuery=false)
public int insertInto(String name, int age, String remark, Date birthDay);
~~~

具体使用实例如下：

~~~java
/**
* 使用SQL语句插入记录
*/
int ii = foodao2.insertInto("六河", 333, "测试", new Date());
System.out.println(ii);
~~~

对应生成的插入语句如下：

~~~sql
insert into Foo (REMARK_A,NAME_A,AGE,BIRTHDAY_A) values (?,?,?,?) | [derby:db@1]
(1):             	[测试]
(2):             	[六河]
(3):             	[333]
(4):             	[2017-09-28]
Executed:1	 Time cost([DbAccess]:4ms) |[derby:db@1]
1
~~~

第二种方法是采用@Param方式进行的

~~~java
@Modifying
@Query(value="insert into foo(remark,name,age,birthday) values (:remark, :name, :age, :birthday)",nativeQuery=false)
public int insertInto2(@Param("name") String name, @Param("age") int age, @Param("remark") String remark, @Param("birthday") Date birthDay);
~~~

实例如下：

~~~java
ii = foodao2.insertInto2("狂四", 555, "测试", new Date());
System.out.println(ii);
~~~

生成的插入语句如下：

~~~sql
insert into Foo (REMARK_A,NAME_A,AGE,BIRTHDAY_A) values (?,?,?,?) | [derby:db@1]
(1):             	[测试]
(2):             	[狂四]
(3):             	[555]
(4):             	[2017-09-28]
Executed:1	 Time cost([DbAccess]:1ms) |[derby:db@1]
1
~~~

#### @Query中的更新

更新时候采用的方法：

~~~java
@Modifying
@Query(value="update foo set age=age+1,birthDay=:birth where age=:age and id=:id",nativeQuery=false)
public int updateFooSetAgeByAgeAndId(@Param("birth") Date birth, @Param("age") int age, @Param("id") int id);
~~~

例子如下：

~~~java
/**
* 使用SQL语句来update
*/
int ii = foodao2.updateFooSetAgeByAgeAndId(new Date(), 12, 2);
System.out.println(ii);
~~~

生成的更新语句如下：

~~~sql
update Foo set AGE = age + 1,BIRTHDAY_A = ? where AGE = ? AND ID = ? | [derby:db@1]
(1):             	[2017-09-28]
(2):             	[12]
(3):             	[2]
~~~

### 使用@Query+语句

值得一提的是，在@Query(name='xxx')可以从预定义的命名查询中获得一个配置好的查询语句

例子如下：

~~~java
@Query(name = "selectByNameAndBirthDay")
public List<Foo> findBySql(@Param("birth") Date birthDay, @Param("name") String name);
~~~

~~~java
List<Foo> foos = foodao2.findBySql(new Date(), "李四");
~~~

生成的SQL语句如下：

~~~
select * from Foo u where u.NAME_A LIKE ? AND u.BIRTHDAY_A = ? | [derby:db@1]
(1):             	[李四]
(2):             	[2017-09-28]
~~~

## Spring Data 对事务的支持

默认情况下，Spring Data实现的方法都是使用事务的。针对查询类型的方法，其等价于 @Transactional(readOnly=true)；增删改类型的方法，等价于 @Transactional。可以看出，除了将查询的方法设为只读事务外，其他事务属性均采用默认值。

如果用户觉得有必要，可以在接口方法上使用 @Transactional 显式指定事务属性，该值覆盖 Spring Data 提供的默认值。同时，开发者也可以在业务层方法上使用 @Transactional 指定事务属性，这主要针对一个业务层方法多次调用持久层方法的情况。持久层的事务会根据设置的事务传播行为来决定是挂起业务层事务还是加入业务层的事务。具体 @Transactional 的使用可以参考Spring的参考文档。

## 未来将支持

• 存储过程

• 匿名存储过程块

• 在Repository中扩展自定义实现

在Repository中扩展自定义实现方法如下：

~~~java
package com.github.geequery.springdata.test.repo;

import com.github.geequery.springdata.test.entity.ComplexFoo;

public interface CustomComplexDao{
	public void someCustomMethod(ComplexFoo user);
}
~~~

具体实现如下：

~~~java
package com.github.geequery.springdata.test.repo;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.github.geequery.springdata.test.entity.ComplexFoo;

public class CustomComplexDaoImpl implements CustomComplexDao{
	@PersistenceContext
	private EntityManager em;
	
	@Override
	public void someCustomMethod(ComplexFoo user) {
		System.out.println(user);
	}

	public ComplexFoo someOtherMethod() {
//		em.merge(user);
		ComplexFoo cf=new ComplexFoo();
//		cf.setUserId(user.getId());
		cf.setClassId(100);
		return cf;
	}

}
~~~

如果需要在自己的Dao中使用，可以直接继承即可

~~~java
package com.github.geequery.springdata.test.repo;

import com.github.geequery.springdata.repository.GqRepository;
import com.github.geequery.springdata.test.entity.ComplexFoo;

public interface ComplexFooDao extends GqRepository<ComplexFoo, int[]>,CustomComplexDao{

}
~~~










