GeeQuery使用手册——Chapter-7  本地化查询

[TOC]

# Chapter-7  本地化查询

## 7.1.  本地化查询是什么

这里本地化查询即’NativeQuery’，是指利用SQL（或者JPQL）进行的数据库操作——不仅限于select，也可以执行insert、update、甚至create table as、truncate等DDL。

本地化查询让用户能根据临时拼凑的或者预先写好的SQL语句进行数据库查询，查询结果将被转换为用户需要的类型。

在EF-ORM中，NativeQuery也有SQL和JPQL两种语法。其中JPQL是JPA规范定义的查询语言。但JPQL因为模型差距较大，一直没有完全支持，目前提供的名称为JPQL的若干方法仅为向下兼容而保留，不推荐大家使用。

因此**本地化查询就是用SQL语句操作数据库的方法**。

您可能会问，如果是用SQL，那么我们直接用JDBC就好了，还用ORM框架做什么？

事实上，NativeQuery中用的SQL，是被EF-ORM增强过的SQL，在语法特性上作了很多的补充。下面的表格中列出了所有在SQL上发生的增强，我们可以在下面的表格中查看到这些激动人心的功能。某种意义上讲，增强过的SQL是一种新的查询语言。我们也可以将其称为E-SQL(Enhanced SQL)。

E-SQL语法作了哪些**改进**呢？ 

| 特性                | 说明                                       |
| ----------------- | ---------------------------------------- |
| Schema重定向         | 在Oracle,PG等数据库下，我们可以跨Schema操作。Oracle数据库会为每个用户启用独立Schema，在SQL语句或对象建模中，我们可以指定Entity所属的Schema。 但是实际部署时schema名称如果发生变化，事先写好的程序就不能正常工作。  schema重定向不仅仅应用于CriteriaAPI中，在SQL语句中出现的Schema也会在改写过程中被替换为当前实际的Schema。 |
| 数据库方言：  语法格式整理    | 对于SQL语句进行重写，将其表现为适应本地数据库的写法，比如 \|\| 运算符的使用。  又比如 as 关键字的使用。 |
| 数据库方言：  函数转换      | 对于translate、delete、sysdate、decode、nullif等数据库函数，能自动转换为当前数据库能够支持的表达形式。 |
| 增强的绑定变量           | 1、允许在语句中用占位符来描述变量。绑定变量占位符可以用 :名称 也可以用 ?序号 的形式。即JPQL语法的占位符。  2、绑定变量占位符中可以指定变量数据类型。这样，当传入String类型的参数时，能自动将其转换为绑定变量应当使用的类型，如java.sql.Date,、java.lang.Number等。 |
| 动态SQL功能：  自动省略    | 在SQL中，会自动扫描每个表达式的入参（占位符）是否被用到。如果某个参数未使用，那么该参数所在的表达式会被省略。例如  *select \* from t where id=:id and  name=:name*  中，如果name参数未传入，则and后面的整个条件表达式被略去。 |
| 动态SQL功能：  SQL片段参数 | 在SQL占位符中可以声明一个占位符是SQL片段。这个片段可以在运行时根据传入的SQL参数自动应用。 |

​									表 7-1 E-SQL的语法特性

EF-ORM会对用户输入的SQL进行解析，改写，从而使得SQL语句的使用更加方便，EF-ORM将不同数据库DBMS下的SQL语句写法进行了兼容处理。 并且提供给上层统一的SQL写法。

除了在SQL语法上的增强以外，通过EF-ORM  NativeQuery操作和直接操作JDBC相比，还有以下优势：

| **特点**          | **NativeQuery**                          | **JDBC**                 |
| --------------- | ---------------------------------------- | ------------------------ |
| **对象返回**        | 转换为需要的对象。转换规则和Criteria API一致。            | ResultSet对象              |
| **自定义返回对象转换映射** | 可以自定义ResultSet中字段返回的Mapping关系。           | --                       |
| **性能调优**        | 可以指定fetch-size , max-result 等参数，进行性能调优   | JDBC提供各种性能调优的参数          |
| **复用**          | 一个NativeQuery可携带不同的绑定变量参数值，反复多次使用。       | PreparedStatment对象可以反复使用 |
| **一级缓存**        | 会刷新和维护一级缓存中的数据。  比如用API插入一个对象，一级缓存中即缓存了这个对象。  虽然用SQL语句去update这条记录。一级缓存中的该对象会被自动刷新。 | 无此功能                     |
| **SQL自动选择**     | SQL改写功能不能解决一切跨库移植问题。用户可以对不兼容跨库的SQL写成多个版本，运行时自动选择。 | 无此功能                     |
| **性能**          | SQL解析和改写需要花费0.3~0.6毫秒。其他操作基本和JDBC直接操作保持一致。  对象结果转换会额外花费一点时间，但采用了策略模式和ASM无反射框架，性能优于大多数同类框架。 | 原生方式，性能最佳                |

​					表 7-2 使用NativeQuery和直接使用JDBC的区别

>*关于JPQL支持*
>
>​	*EF-ORM中，也可以用”JPQL“来构造NativeQuery，但并不推荐。因为EF并未实现JPQL的大部分功能。目前提供的JPQL功能其实只有将Java字段名替换为数据库列名的功能，离JPA规范的JPQL差距较大，而且由于设计理念等差异，要完整支持JPQL基本不可能。*
>
>​	*现有若干伪JPQL功能是早期遗留的产物，后来在对SQL的特性作了大量改进后，E-SQL成为EF-ORM主要的查询语言。JPQL方面暂无改进计划，因此不建议使用。* 

## 7.2.  使用本地化查询

### 7.2.1.  NamedQuery和NativeQuery

NativeQuery的用法可以分为两类。一类是在java代码中直接传入E-SQL语句的；另外一类是事先将E-SQL编写在配置文件或者数据库中，运行时加载并解析，使用时按名称进行调用。这类SQL查询被称为NamedQuery。对应JPA规范当中的“命名查询”。

命名查询也就是Named-Query，在某H框架和JPA中都有相关的功能定义。简单来说，命名查询就是将查询语句(SQL,HQL,JPQL等)事先编写好， 然后为其指定一个名称。在使用ORM框架时，取出事先解析好的查询，向其中填入绑定变量的参数，形成完整的查询。

EF-ORM的命名查询和OpenJPA以及某H框架中的命名查询用法稍有些不同。

* 命名查询默认定义在配置文件 named-queries.xml中。不支持使用Annotation等方法定义
* 命名查询也可以定义在数据库表中，数据库表的名称可由用户配置 
* 命名查询可以支持 E-SQL和JPQL两种语法（后者特性未全部实现） 
* 由于支持E-SQL，命名查询可以实现动态SQL语句的功能，可以模拟出与IBatis相似的用法。 

为什么不使用JPA规范中的基于Annotation的方式来注册命名查询呢？因为考虑到ORM中一般只有跨表的复杂查询才会使用命名查询。而将一个多表的复杂查询注解在任何一个DAO上都是不合适的。分别注解在DAO上的SQL语句除了语法受限之外，还有以下缺点：

* 归属不明确，很难正确评判某个SQL语句应当属于某个DAO。而且不能被其他DAO使用？
* Java代码中写SQL涉及转义问题
* DAO太分散，不利于SQL语句的统一维护。

EF-ORM默认设计了两种方式来配置命名查询

* classpath下创建一个名为named-queries.xml的配置文件
* 存放在数据库中，表名可自定义，默认JEF_NAMED_QUERIES

NativeQuery是在EF-ORM 1.05开始增加的功能。在1.6开始支持数据库配置，在1.6.7开始支持动态改写和SQL片段。

### 7.2.2.  API和用法

我们分别来看Named-Query和Native Query的使用

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~java
@Test
public void testNativeQuery() {
	// 方法1 NamedQuery
	{
		NativeQuery<ResultWrapper> query = db.createNamedQuery("unionQuery-1", ResultWrapper.class);
		List<ResultWrapper> result = query.getResultList();
		System.out.println(result);
	}
	// 方法2 直接传入SQL
	{
		String sql = "select * from((select upper(t1.person_name) AS name, T1.gender, '1' AS GRADE,"+ "T2.NAME AS SCHOOLNAME from T_PERSON T1 inner join SCHOOL T2 ON T1.CURRENT_SCHOOL_ID=T2.ID"+ ") union  (select t.NAME,t.GENDER,t.GRADE,'Unknown' AS SCHOOLNAME from STUDENT t  )) a";
		NativeQuery<ResultWrapper> query = db.createNativeQuery(sql,	ResultWrapper.class);
		List<ResultWrapper> result = query.getResultList();
		System.out.println(result);
	}
}
~~~

上面的例子中，两次查询使用的SQL语句是一样的，区别在于前者配置在named-queries.xml中，后者直接写在代码中。

>*实践建议：*
>
>​	*EF-ORM的SQL语句已经解决了动态化的问题（这点可在以后的例子中发现），在我看来，在代码中编写SQL语句带来的灵活性，远不如拼凑SQL带来的可读性和可维护性上的损失来得大。因此建议在使用时，尽可能多的使用命名查询，而少用拼凑SQL的查询。*
>
>​	*上面的方法一相比方法二还有一个性能上的优势。EF-ORM会对传入的SQL进行解析和重写，如果是命名查询，解析只进行一次，解析结果会缓存下来，而传入的SQL语句则必须每次解析和重写。*

在上面的例子中，无论是createNamedQuery方法还是createNativeQuery方法，返回的对象都是名为NativeQuyery的对象。其行为和功能也完全一样。

下面是用于获得NativeQuery的方法API

| 方法                                       | 用途                             |
| ---------------------------------------- | ------------------------------ |
| Session.createNamedQuery(String)         | 构造一个命名查询对象。不指定返回类型。            |
| Session.createNamedQuery(String,  Class\<T>) | 构造一个命名查询对象。指定返回类型为某class。      |
| Session.createNamedQuery(String,  ITableMetadata) | 构造一个命名查询对象。指定返回类型为某表元模型所对应的类。  |
| Session.createNativeQuery(String)        | 构造一个SQL查询对象。不指定返回类型。           |
| Session.createNativeQuery(String,  Class\<T>) | 构造一个SQL查询对象。指定返回类型为某class。     |
| Session.createNativeQuery(String,  ITableMetadata) | 构造一个SQL查询对象。指定返回类型为某表元模型所对应的类。 |
| Session.createQuery(String)              | 构造一个JPQL查询对象。不指定返回类型。          |
| Session.createQuery(String,Class\<T>)    | 构造一个JPQL查询对象。指定返回类型为某class。    |

上述方法都可以返回NativeQuery对象。

NativeQuery并不一定就是select语句。在NativeQuery中完全可以使用update delete insert等语句，甚至是create table等DDL语句。（当执行DDL时会造成事务被提交，需谨慎）。显然，在指定非Select操作时，传入一个返回结果类型是多此一举，可以指定NativeQuery返回的结果类型，也可以不指定。

在得到了NativeQuery以后，我们有很多种用法去使用这个Query对象。这里先列举一下主要的方法。

| 方法                                       | 用途                                       |
| ---------------------------------------- | ---------------------------------------- |
| **执行查询动作**                               |                                          |
| NativeQuery.getResultList()              | 查询出由多行记录转换的对象列表                          |
| NativeQuery.getSingleResult()            | 查询出单行记录转换的对象，如果有多行返回第一行                  |
| NativeQuery.getResultCount()             | 将SQL改为count语句后查出结果总数                     |
| NativeQuery.getResultIterator()          | 查询出多行记录，并用遍历器方式返回                        |
| NativeQuery.executeUpdate()              | 执行SQL语句，返回影响的记录行数                        |
| NativeQuery.getSingleOnlyResult()        | 和getSingleResult的区别是如果有多行则抛出异常           |
| **性能参数**                                 |                                          |
| NativeQuery.setMaxResults(int)           | 设置查询最大返回结果数                              |
| NativeQuery.getMaxResults()              | 获取查询最大返回结果数                              |
| NativeQuery.getFetchSize()               | 获取结果集加载批次大小                              |
| NativeQuery.setFetchSize(int)            | 设置结果集加载批次大小                              |
| **结果范围限制（分页相关）**                         |                                          |
| NativeQuery.setFirstResult(int)          | 设置查询结果的偏移，0表示不跳过记录。                      |
| NativeQuery.getFirstResult()             | 返回查询结果的偏移，0表示不跳过记录                       |
| NativeQuery.setRange(IntRange)           | 设置查询区间（含头含尾）                             |
| **绑定变量参数**                               |                                          |
| NativeQuery.setParameter(String,  Object) | 设置绑定变量参数                                 |
| NativeQuery.setParameter(int, Object)    | 设置绑定变量参数                                 |
| NativeQuery.setParameterByString(String,  String) | 设置绑定变量参数，传入String后根据变量类型自动转换             |
| NativeQuery.setParameterByString(String,  String[]) | 设置绑定变量参数，传入String[]后根据变量类型自动转换           |
| NativeQuery.setParameterByString(int,  String) | 设置绑定变量参数，传入String后根据变量类型自动转换             |
| NativeQuery.setParameterByString(int,  String[]) | 设置绑定变量参数，传入String[]后根据变量类型自动转换           |
| NativeQuery.setParameterMap(Map\<String,  Object>) | 设置多组绑定变量参数                               |
| NativeQuery.getParameterValue(String)    | 获得绑定变量参数                                 |
| NativeQuery.getParameterValue(int)       | 获得绑定变量参数                                 |
| NativeQuery.containsParam(Object)        | 检查某个绑定变量是否已经设置了参数                        |
| NativeQuery.clearParameters()            | 清除目前已经设入的绑定变量参数。当需要重复使用一个NativeQuery对象进行多次查询时，建议每次清空旧参数。 |
| NativeQuery.getParameterNames()          | 获得查询中所有的绑定变量参数名                          |
| **返回结果定义**                               |                                          |
| NativeQuery.getResultTransformer()       | 得到ResultTransformer对象，可定义返回结果转换动作。       |

根据上述API，我们简单的使用如下——

~~~java
@Test
public void testQueryParams(){
	String sql="select distinct(select grade from student s where s.name=person_name)                           grade,person_name,gender from t_person where id<:id";
	NativeQuery<Map> query = db.createNativeQuery(sql,Map.class);
	query.setParameter("id", 12);
	//自动改写为count语句进行查询
	System.out.println("预计查出"+query.getResultCount()+"条结果");
	//查询多条结果
	System.out.println(query.getResultList());
		
	//重新设置参数
	System.out.println("=== 重新设置参数 ===");
	query.setParameter("id", 2);
	System.out.println("预计查出"+query.getResultCount()+"条结果");
	System.out.println(query.getResultList());
	//查出第一条结果
	System.out.println(query.getSingleOnlyResult());
}
~~~

注意观察输出的SQL语句，上面的案例中，演示了

1. 绑定变量参数用法
2. Count语句在一些复杂情况下的转换逻辑
3. 通过重置参数，可以复用NativeQuery对象。
4. 仅返回单条结果的场景

### 7.2.3.  命名查询的配置

上一节中，我们基本了解了NativeQuery对象的构造和使用。本节来介绍命名查询如何配置。

前面说过，命名查询的配置方法有两种。我们先来看配置在文件中的场景。

#### 7.2.3.1.  配置在named-queries.xml中

在classpath下创建一个名为named-queries.xml的文件。

named-queries.xml

~~~xml
<queries>
	<query name = "getUserById">
	<![CDATA[
		   select * from t_person where person_name=:name<string>
	]]>
	</query>
	<query name = "testIn" type="sql" fetch-size="100" >
	<![CDATA[
		   select count(*) from Person_table where id in (:names<int>)
	]]>
	</query>
</queries>
~~~

上面就配置了两个命名查询，名称分别为“getUserById”以及”testIn”。

其中每个查询的SQL中，都有一个参数，参数在SQL中用绑定变量占位符表示。后面的参数使用in条件，使用时可以传入int数组。

在query元素中，可以设置以下属性

| 属性名        | 作用                                   | 备注               |
| ---------- | ------------------------------------ | ---------------- |
| name       | 指定查询的名称                              | 必填               |
| type       | sql或jpql，表示语句的类型                     | 可选，默认为SQL        |
| fetch-size | 指定结果集每次获取批次大小                        | 可选，默认0即JDBC驱动默认值 |
| tag        | 当DbClient连接到多数据源时，可以指定该查询默认使用哪个数据源连接 | 可选               |
| remark     | 备注信息，可不写                             | 可选               |

最后，当classpath下有多个named-queries.xml时，所有配置均会生效。如果多个文件中配置的同名的查询，那么后面加载的会覆盖前面的。当覆盖现象发生时，日志中会输出警告。

#### 7.2.3.2.  配置在数据库中

你可以将命名查询配置在指定的数据库表中。要启用此功能，需要在jef.properties配置 

~~~properties
db.query.table.name=表名
~~~

当EF-ORM初始化时，会自动检测这张表并加载数据，如果没有则会自动创建。表的结构是固定的(数据类型在不同的数据库上会自动转换为可支持的类型)

命名查询数据表的结构是固定的，结构如下——

| **Column** | **Type**                            | **备注**                               |
| ---------- | ----------------------------------- | ------------------------------------ |
| NAME       | varchar2(256)  not null primary key | 指定查询的名称                              |
| SQL_TEXT   | varchar2(4000)                      | SQL/JPQL语句                           |
| TAG        | varchar2(256)                       | 当DbClient连接到多数据源时，可以指定该查询默认使用哪个数据源连接 |
| TYPE       | number(1)  default 0                | 语句类型。0:SQL 1:JPQL                    |
| FETCH_SIZE | number(6)  default 0                | 指定结果集每次获取批次大小                        |
| REMARK     | varchar2(256)                       | 备注                                   |

​						表7-3 命名查询的数据库表结构

您可以同时使用文件配置和数据库配置命名查询。但如果出现同名的查询，数据库的配置会覆盖文件的配置。

#### 7.2.3.3.  数据源绑定

由于EF-ORM是一种支持多数据源自动路由的ORM框架。因此在命名查询中，还可以在tag属性中指定偏好的数据源ID。这样，如果你在query://getUserByName这样的例子中请求数据时，即使不通过_dsname参数来指定数据源id，也可以到正确的数据库中查询数据。 

我们先配置两个命名查询，区别是一个指定了数据源，另一个未指定数据源。

orm-tutorial\src\main\resources\named-queries.xml

~~~xml
<query name="getUserById" tag="datasource2">
<![CDATA[
	   select * from t_person where person_name=:name<string>
]]>
</query>
<query name="getUserById-not-bind-ds">
<![CDATA[
	   select * from t_person where person_name=:name<string>
]]>
</query>
~~~

然后编写代码如下——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case2.java

~~~java
@Test(expected=PersistenceException.class)
public void testDataSourceBind() throws SQLException{
	//Person对象所在的数据源为DataSource2		
	//由于配置中指定默认数据源为datasource2，因此可以正常查出
	List<Person> persons=db.createNamedQuery("getUserById",Person.class).setParameter("name", "张三").getResultList();
	System.out.println(persons);
	
	//这个配置未指定绑定的数据源，因此会抛出异常
	try{
		List<Person> p2=db.createNamedQuery("getUserById-not-bind-ds",Person.class)                 .setParameter("name", "张三").getResultList();	
	}catch(RuntimeException e){
		e.printStackTrace();
		throw e;
	}
}
~~~

从日志上可以观察到，第一次操作时，操作是在datasource2数据库上执行的，因此能正确查出。第二次操作时，操作在datasource1上执行，由于表不存在，因此抛出异常。

#### 7.2.3.4.  动态更新命名查询配置

考虑到性能因素，NamedQuery在首次使用时从配置中加载SQL语句并解析，之后解析结果就缓存在内存中了。作为服务器运行来说，这一点没什么问题。但在Web应用开发中，用户如果修改了SQL语句，不得不重启服务才能调试。这就会给开发者造成一定困扰。为了解决配置刷新的问题，框架中增加了配置变更检测的功能。

在jef.properties中配置

~~~properties
db.named.query.update=true
~~~

开启更新检测功能后，每次获取命名查询都会检查配置文件是否修改，如果配置来自于数据库，也会加载数据库中的配置。在开发环境可以开启这个参数；考虑到性能，在生产环境建议关闭该参数。

当大量并发请求使用命名查询时，为了避免短时间重复检查更新，一旦检测过一次，10秒内不会再次检测。

一般情况下，我们无需配置该参数。因为该参数的默认值会保持和 db.debug 一致，一般来说开发时我们肯定会设置db.debug=true。而在对性能要求较高的生产环境，需要指定该参数为false。

命名查询自动更新的配置项可以在JMX的ORMConfigMBean中开启或关闭。JMX相关介绍请参阅13章。

除了自动检测更新机制外，还有手动刷新命名查询配置的功能。比如在生产环境，关闭自动检测配置更新功能后，可以手工进行更新检测。在DbClient中可以强制立刻检查命名查询的更新。

~~~
DbClient.checkNamedQueryUpdate()
~~~

强制检测命名查询功能也可以在JMX的Bean中调用。DbClientInfoMBean中有checkNamedQueryUpdate()方法。

 ![7.2.3.4](images/7.2.3.4.png)

## 7.3.  NativeQuery特性使用

### 7.3.1.  Schema重定向

Schema重定向多使用在Oracle数据库上。在Oracle上，数据库操作可以跨用户(Schema)访问。当跨Schema访问时，SQL语句中会带有用户名的前缀。（这样的应用虽然不多，但是在电信和金融系统中还是经常看到）。

例如USERA用户下和USERB用户下都有一张名为TT的表。 我们可以在一个SQL语句中访问两个用户下的表

~~~sql
select * from USERA.TT
   union all
select * from USERB.TT 
~~~

 当使用ORM进行此类映射时，一般用如下方式指定

~~~java
@Entity
@Table(schema="USERA",name="TT")
public class TT {
	....
}
~~~

这样就带来一个问题，在某些场合，实际部署的数据库用户是未定的，在编程时开发人员无法确定今后系统将会以什么用户部署。如果将“USERA”硬编码到程序中，实际部署时数据库就只能建在USERA用户下，部署时缺乏灵活性。

EF-ORM的Schema重定向功能对Query模型和SQL语句都有效。在开发时，用户根据设计中的虚拟Schema名编写代码，而在实际部署时，可以配置文件jef.properties指定虚拟schema对应到真实环境中的schema上。

例如，在jef.properties中配置

~~~properties
schema.mapping=USERA:APP, USERB:APP
~~~

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~java
/* 
 * SQL语句中的usera userb都是不存在的schema，通过jef.properties中的配置，被重定向到APP schema下
 * @throws SQLException
 */
@Test
public void testSchemaMapping() throws SQLException{
	String sql="select * from usera.t_person union all select * from userb.t_person";
	db.createNativeQuery(sql).getResultList();
}
~~~

上例中的SQL语句本来是无法执行的，但是因为Schema重定向功能SQL语句在实际执行时，就变为 

~~~sql
select * from app.t_person
   union all
select * from app.t_person 
~~~

这样就能正常执行了。

使用schema重定向功能，可以解决开发和部署的 schema耦合问题，为测试、部署等带来更大的灵活性。

一个特殊的配置方式是

~~~properties
schema.mapping=USERA:, USERB:
~~~

即不配置重定向后的schema，这个操作实际上会将原先制定的schema信息去除，相当于使用数据库连接的当前Schema。

最后，正如前文所述，重定向功能并不仅仅作用于本地化查询中，如果是在类的注解上配置了Schema，那么其映射会在所有Criteria API查询中也都会生效。

### 7.3.2.  数据库方言——语法格式整理

根据不同的数据库语法，EF-ORM会在执行SQL语句前根据本地方言对SQL进行修改，以适应当前数据库的需要。

**例1**    

~~~sql
select t.id||t.name as u from t
~~~

在本例中||表示字符串相连，这在大部分数据库上执行都没有问题，但是如果在MySQL上执行就不行了，MySQL中||表示或关系，不表示字符串相加。因此，EF-ORM在MySQL上执行上述E-SQL语句时，实际在数据库上执行的语句变为

~~~sql
select concat(t.id, t.name) as u from t //for MySQL
~~~

这保证了SQL语句按大多数人的习惯在MYSQL上正常使用。

**例2**

~~~sql
select count(*) total from t
~~~

这句SQL语句在Oracle上是能正常运行的，但是在postgresql上就不行了。因为postgresql要求每个列的别名前都有as关键字。对于这种情况EF-ORM会自动为这样的SQL语句加上缺少的as关键字，从而保证SQL语句在Postgres上也能正常执行。 

~~~sql
select count(*) as total from t    //for Postgresql
~~~

上述修改过程是全自动的，无需人工干涉。EF-ORM会为所有传入本地化查询进行语法修正，以适应当前操作的数据库。

这些功能提高了SQL语句的兼容性，能对用户屏蔽数据库方言的差异，避免操作者因为使用了SQL而遇到数据库难以迁移的情况。 

我们看一下orm-tutorial中的例子——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~sql
/**
 * concat(person_name, gender) 在实际使用时会改写为 person_name||gender
*/
@Test
public void testRewrite1() throws SQLException{
	String sql="select concat(person_name, gender) from usera.t_person";
	db.createNativeQuery(sql).getResultList();
}
~~~

众所周知，Derby数据库中无法使用concat(a,b)的函数。所以经过方言转换会将其表示为

~~~sql
select person_name||gender from app.t_person 
~~~

**注意**

并不是所有情况都能实现自动改写SQL，比如有些Oracle的使用者喜欢用+号来表示外连接，写成仅有Oracle能识别的外连接格式。

~~~sql
select t1.*,t2.* from t1,t2 where t1.id=t2.id(+)
~~~

目前EF-ORM还**不支持**将这种SQL语句改写为其他数据库支持的语法(今后可能会支持)。 因此如果要编写能跨数据库的SQL语句，还是要使用‘OUTER JOIN’这样标准的SQL语法。 

### 7.3.3.  数据库方言——函数转换

#### 7.3.3.1.  示例

在EF-ORM对SQL的解析和改写过程中，还能处理SQL语句当中的数据库函数问题。EF-ORM在为每个数据库建立的方言当中，都指定了常用函数的支持方式。在解析时，EF-ORM能够自动识别SQL语句中的函数，并将其转换为在当前数据库上能够使用的函数。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~sql
@Test
public void testRewrite2() throws SQLException{
	Stringsql="selectreplace(person_name,'张','王')person_name,
				decode(nvl(gender,'M'),'M','男','女') gender from t_person";
	System.out.println(db.createNativeQuery(sql).getResultList());
}
~~~

我们观察一下在Derby数据库时输出的实际SQL语句

~~~sql
select replace(person_name, '张', '王') AS person_name,
       CASE
         WHEN coalesce(gender, 'M') = 'M' 
         THEN '男'
         ELSE '女'
       END AS gender
from t_person
~~~

解说一下，上面作了处理的函数包括——

nvl函数被转换为coalesce函数

Decode函数被转换为 case ... When ... Then ... Else...语句。

Replace函数也是很特殊的——Derby本没有Replace函数，这里的replace函数其实是一个用户自定义的java函数。也是由EF-ORM自动注入的自定义函数。

再看一个例子

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~java
@Test
public void testRewrite3() throws SQLException{
	//获得：当前日期减去1个月，和学生生日相差的天数。
	String sql="select datediff(add_months(sysdate, -1), DATE_OF_BIRTH),DATE_OF_BIRTH from student";
	System.out.println(db.createNativeQuery(sql).getResultList());
		
	//获得：在当前日期上加上1年
	sql="select addDate(sysdate, INTERVAL 1 YEAR)  from student";
	System.out.println(db.createNativeQuery(sql).getResultList());
}
~~~

​	上例涉及日期时间的计算，最终结果是一个日期天数。执行上面的代码，可以看到SQL语句变为

~~~sql
select {fn timestampdiff(SQL_TSI_DAY, DATE_OF_BIRTH, {fn timestampadd(SQL_TSI_MONTH, -1, 	current_timestamp) }) }, DATE_OF_BIRTH
from student

select {fn timestampadd(SQL_TSI_YEAR, 1, current_timestamp) } from student
~~~

其中——

datediff函数被转换为JDBC函数 timestampdiff

add_months和adddate函数被转换为JDBC函数timestampadd

Sysdate函数被转换为current_timestamp。

通过上面的转换过程，EF-ORM尽最大努力的保证查询的跨数据库兼容性。

#### 7.3.3.2.  函数支持

EF-ORM对函数支持的原则是，尽可能将常用函数提取出来，并保证其能在任何数据库上使用。

目前整理定义的常用函数被定义在两个枚举类中，其用法含义如下表所示

jef.database.query.Func

| 函数                | 作用                                       | 参数格式                              |
| ----------------- | ---------------------------------------- | --------------------------------- |
| abs               | 单参数。获取数值的绝对值。                            | (number)                          |
| add_months        | 双参数。在日期上增加月数。（来自于Oracle）。                | (date,  number)                   |
| adddate           | 在日期上增加天数，adddate(current_date, 1)即返回明天  支持Interval语法。Adddate(current_date, interval 1 month)即返回下月当天   (来自于MYSQL,和DATE_ADD含义相同） | (date,  numer)  (date,  interval) |
| avg               | 返回分组中数据的平均值                              |                                   |
| cast              | 类型转换函数，格式为 cast(arg as varchar) cast(arg  as timestamp)等等 |                                   |
| ceil              | 单参数。按大数取整                                | (float)                           |
| coalesce          | 多参数。可以在括号中写多个参数，返回参数中第一个非null值           |                                   |
| concat            | 多参数。连接多个字符串，虽然大部分数据库都用 \|\| 表示两个字符串拼接，但是MYSQL是必须用concat函数的 |                                   |
| count             | 计算行数                                     |                                   |
| current_date      | 无参数。返回当前日期                               |                                   |
| current_time      | 无参数。返回当前不含日期的时间                          |                                   |
| current_timestamp | 无参数。返回当前日期和时间                            |                                   |
| date              | 单参数。截取年月日                                | (timestamp)                       |
| datediff          | 返回第一个日期减去第二个日期相差的天数。                     | (date,date)                       |
| day               | 获得日，如 day(current_date)返回当天是几号。          | (date)                            |
| decode            | 类似于多个if分支。 (来自于Oracle)                   |                                   |
| floor             | 按较小的整数取整                                 |                                   |
| hour              | 获得小时数                                    |                                   |
| length            | 文字长度计算。基于字符个数（英文和汉字都算1）                  | (varchar)                         |
| lengthb           | 文字长度按实际存储大小计算，因此在UTF8编码的数据库中，汉字实际占3个字符。  | (varchar)                         |
| locate            | 双参数,在参数2中查找参数1，返回匹配的序号（序号从1开始）。          | (varchar,varchar)                 |
| lower             | 字符串转小写                                   | (varchar)                         |
| lpad              | 在字符串或数字左侧添加字符，拼到指定的长度。                   | (expr,  varchar, int)             |
| ltrim             | 截去字符串左边的空格                               | (varchar)                         |
| max               | 返回分组中的最大值                                |                                   |
| min               | 返回分组中的最小值                                |                                   |
| minute            | 获得分钟数                                    |                                   |
| mod               | 取模运算。前一个参数除以后一个时的余数。                     | (int,  int)                       |
| month             | 获得月份数                                    |                                   |
| now               | 无参数，和CURRENT_TIMESTAMP含义相同。              |                                   |
| nullif            | 双参数，nullif ( L, R )。两值相等返回null，否则返回第一个参数值。 | (expr,expr)                       |
| nvl               | 双参数，返回第一个非null值 (来自于Oracle)              |                                   |
| replace           | 三参数，查找并替换字符 replace(sourceStr, searchStr,  replacement) | (varchar,varchar,varchar)         |
| round             | 四舍五入取整                                   | (float)                           |
| rpad              | 在字符串或数字右侧添加字符，拼到指定的长度。                   | (expr,  varchar, int)             |
| rtrim             | 单参数，截去字符串右边的空格 (来自于Oracle)               | (varchar)                         |
| second            | 单参数，获得秒数                                 | (timestamp)                       |
| sign              | 单参数，判断数值符号。正数返回1，负数返回-1，零返回0             | (number)                          |
| str               | 但参数，将各种类型变量转为字符串类型 (来自HQL)               |                                   |
| subdate           | 双参数，在日期上减去天数，支持Interval语法。 (来自于MYSQL,和DATE_SUB含义相同) |                                   |
| substring         | 取字符的子串 (第一个字符号为1) ，例如 substring('abcde',2,3)='bcd' | (source,  startIndex, length)     |
| sum               | 返回分组中的数据的总和                              |                                   |
| time              | 单参数，截取时分秒                                |                                   |
| timestampadd      | 时间调整 第一个参数是时间调整单位，第二个参数是调整数值，第三个参数是日期时间表达式。例如 timestampadd( MINUTE, 10,  current_timestamp)   （来自MySQL，同时是JDBC函数） | (SQL_TSI,  number, timestamp)     |
| timestampdiff     | 返回两个时间差值多少，第一个参数为返回差值的单位取值范围是SQL_TSI的枚举，后两个参数是日期1和日期2，返回日期2减去日期1（注意和datediff刚好相反）  例如Timestampdiff(MINUTE, date1,  date2)返回两个时间相差的分钟数。 （来自MySQL，同时是JDBC函数） | (SQL_TSI,  time1, time2)          |
| translate         | 三参数，针对单个字符的批量查找替换。Translate(‘Hello,World’,’eo’,’oe’).  将字符中的e替换为o，o替换为e。 (来自于Oracle)。  注意：在部分数据库上使用多个replace语句来模拟效果，但由于模拟相当于执行多次函数，因此如果先替换字符列表中出现重复替换现象，结果可能和Oracle不一致。 | (varchar,varchar,  varchar)       |
| trim              | 截去字符串两头的空格。                              | (varchar)                         |
| trunc             | 适用于数字，保留其小数点后的指定位数。在Oracle上trunc还可用于截断日期。此处不支持。 | (float)                           |
| upper             | 字符串转大写                                   | (varchar)                         |
| year              | 获得年份数                                    | (timestamp)                       |

 

jef.database.query.Scientific

| 函数      | 作用                                |
| ------- | --------------------------------- |
| acos    | 反余弦函数                             |
| asin    | 反正弦函数                             |
| atan    | 反正切函数                             |
| cos     | 余弦函数                              |
| cosh    | 双曲余弦函数                            |
| cot     | 余切函数                              |
| degrees | 弧度转角度 即value/3.1415926*180        |
| exp     | 返回自然对数的底(e)的n次方。                  |
| ln      | 自然对数等同于log                        |
| log10   | 以10为底的对数                          |
| power   | 乘方                                |
| radians | 角度转弧度 即value/180*3.1415926        |
| rand    | 随机数，返回0..1之间的浮点随机数.               |
| sin     | 正弦函数                              |
| sinh    | 双曲正弦函数                            |
| soundex | 字符串发音特征。不是科学计算，但只对英语有用，对汉语几乎没有作用。 |
| sqrt    | 平方根                               |
| tan     | 正切函数                              |
| tanh    | 双曲正切函数                            |

常用函数中，某些函数并非来自于任何数据库的SQL函数中，而是EF-ORM定义的，比如str函数。

 除了上述被提取的常用函数外，数据库原生的各种函数和用户自定义的函数仍然能够被使用。但是EF-ORM无法保证包含这些函数的SQL语句被移植到别的RDBMS后还能继续使用。

#### 7.3.3.3.  方言扩展

函数的支持和改写规则定义是通过各个数据库方言来定义的。因此，要支持更多的函数，以及现有的一些不兼容的场景，可以通过扩展方言来实现。

方言扩展的方法是配置自定义的方言类。在jef.properties中，我们可以指定自定义方言配置文件，来覆盖EF-ORM内置的方言。

jef.properties

~~~properties
#the custom dialect mapping file 
db.dialect.config=my_dialects.properties
~~~

上面定义了自定义方言映射文件的名称是my_dialacts.properties，然后在my_dialacts.properties中配置自定义的方言映射。

在见示例工程目录：orm-tutorial\src\main\resources\my_dialects.properties

~~~properties
derby=org.easyframe.tutorial.lessonc.DerbyExtendDialect
~~~

文件中，前面是要定义的数据库类型，后面是方言类。

我们编写的自定义方言类如下。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\dialect\DerbyExtendDialect.java

~~~java
import jef.database.dialect.DerbyDialect;

public class DerbyExtendDialect extends DerbyDialect{
	public DerbyExtendDialect() {
		super();
		//编写自己的方言逻辑
		//....
	}

   	//覆盖父类方法
}
~~~

自定义方言可以控制数据库各种本地化行为，包括分页实现方式、数据类型等。这些实现可以参考EF-ORM内置的方言代码。

**示例1：让Derby支持反三角函数TAN2**

DERBY数据库支持反三角函数TAN2函数。但是因为方言中没有注册这个函数，因此我们在E-SQL中是无法使用的。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\CaseDialectExtend.java

~~~java
@Test
public void testExtendDialact() throws SQLException{
	String sql="select atan2(12, 1) from t_person";
	System.out.println(db.createNativeQuery(sql).getResultList());
	……
}
~~~

上面的代码会抛出异常信息

这个信息其实是不对的，Derby数据库支持该函数，而方言中遗漏了这个函数。现在我们可以在方言中补上注册

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonc\DerbyExtendDialect.java

~~~java
public DerbyExtendDialect() {
	super();
	registerNative(new StandardSQLFunction("atan2"));
}
~~~

然后再运行上面的案例，可以发现案例能够正确运行。

**示例2：让Derby支持ifnull函数**

MySQL中有一个ifnull函数。返回参数中第一个非空值。

如果我们要在Derby上支持带有这个函数的SQL语句，要怎么写呢？我们可以在自定义方言的构造函数中，注册这样一个函数。

~~~java
registerCompatible(null, new TemplateFunction("ifnull", "(CASE WHEN %1$s is null THEN %2$s ELSE 				%1$s END)"),"ifnull");
~~~

这个函数的作用是在实际运行时，用一组CASE WHEN... THEN... ELSE... 语句来代替原先的ifnull函数。

我们试一下

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\ DerbyExtendDialect.java

~~~java
public void testExtendDialact() throws SQLException {
	……
	sql = "select ifnull(gender, 'F') from t_person";
	System.out.println(db.createNativeQuery(sql).getResultList());
}
~~~

可以发现，在注册ifnull函数之前，上面的SQL语句是无法运行的，而注册了函数之后，SQL语句就可以运行了。

​EF-ORM中注册的函数有三种方式

| 方法                   | 作用                                       |
| -------------------- | ---------------------------------------- |
| registerNative()     | 注册一个函数的本地实现。本地实现就是数据库原生支持的函数。比如 count() sum() avg()等函数。 |
| registerAlias()      | 注册一个函数的替换实现。替换实现就是数据库虽然不支持指定的函数，但是有其他函数能兼容或者包含需要的函数功能。在实际执行时，函数名将被替换为本地函数名，但对参数等不作处理。 |
| registerCompatible() | 注册一个函数的模拟实现。模拟实现就是完全改写整个函数的用法。从函数名称到参数，都会被重写。 |

### 7.3.4.  绑定变量占位符

E-SQL中表示参数变量有两种方式 : 

* :param-name　(如:id :name，用名称表示参数) 
* ?param-index　 (如 ?1 ?2，用序号表示参数)。 

上述绑定变量占位符是和JPA规范完全一致的。

E-SQL中，绑定变量的参数类型可以声明也可以不声明。比如上例的

~~~sql
select count(*) from Person_table where id in (:ids<int>)
~~~

也可以写作

~~~sql
select count(*) from Person_table where id in (:ids)
~~~

但是如果不声明类型，那么如果传入的参数为List<String>，那么数据库是否能正常执行这个SQL语句取决于JDBC驱动能否支持。（因为数据库里的id字段是number类型而传入了string）。

指定参数类型是一个好习惯，尤其是当参数来自于Web页面时，这个特性尤其实用。

很多时候我们从Web页面或者配置中得到的参数都是string类型的，而数据库操作的类型可能是int,date,boolean等类型。此时我们可以在Native Query语句中指定参数类型，使其可以自动转换。

参数的类型有：date,timestamp,int,string,long,short,float,double,boolean。

参数可以为数组，如上例，可以用数组表示in条件参数中的列表。

目前我们支持的参数类型包括(类型不区分大小写)：

| 类型名       | 效果                                       |
| --------- | ---------------------------------------- |
| DATE      | 参数将被转换为java.sql.Date                     |
| TIMESTAMP | 参数将被转换为java.sql.Timestamp                |
| INT       | 参数将被转换为int                               |
| STRING    | 参数将被转换为string                            |
| LONG      | 参数将被转换为long                              |
| SHORT     | 参数将被转换为short                             |
| FLOAT     | 参数将被转换为float                             |
| DOUBLE    | 参数将被转换为double                            |
| BOOLEAN   | 参数将被转换为boolean                           |
| STRING$   | 参数将被转换为string,并且后面加上%，一般用于like xxx% 的场合  |
| $STRING$  | 参数将被转换为string,并且两端加上%，一般用于like %xxx% 的场合 |
| $STRING   | 参数将被转换为string,并且前面加上%，一般用于like %xxx 的场合  |
| SQL       | SQL片段。参数将直接作为SQL语句的一部分，而不是作为SQL语句的绑定变量处理（见后文例子） |

上面的STRING$、$STRING$、$STRING三种参数转换，其效果是将$符号替换为%，主要用于从WEB页面传输模糊匹配的查询条件到后台。使用该数据类型后，%号的添加交由框架自动处理，业务代码可以更为清晰简洁。看下面的例子

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~java
/**
* 绑定变量中使用Like条件
*/
@Test
public void testLike(){
	String sql ="select * from t_person where person_name like :name<$string$>";
	System.out.println(db.createNativeQuery(sql).setParameter("name","张").getResultList());
}
~~~

SQL类型是将参数作为SQL片段处理，该功能使用参见7.3.6节。

### 7.3.5.  动态SQL——表达式省略

EF-ORM可以根据未传入的参数，动态的省略某些SQL片段。这个特性往往用于某些参数不传场合下的动态条件，避免写大量的SQL。有点类似于IBatis的动态SQL功能。

我们先来看一个例子

~~~java
public void testDynamicSQL(){
	//SQL语句中写了四个查询条件
	String sql="select * from t_person where id=:id " +
			"and person_name like :person_name<$string$> " +
			"and currentSchoolId=:schoolId " +
			"and gender=:gender";
	NativeQuery<Person> query=db.createNativeQuery(sql,Person.class);
	{
		System.out.println("== 按ID查询 ==");
		query.setParameter("id", 1);
		Person p=query.getSingleResult();  //只传入ID时，其他三个条件消失
		System.out.println(p.getId());
		System.out.println(p);	
	}
	{
		System.out.println("== 由于参数'ID'并未清除，所以变为 ID + NAME查询 ==");
		query.setParameter("person_name", "张"); //传入ID和NAME时，其他两个条件消失
		System.out.println(query.getResultList());
	}
	{
		System.out.println("== 参数清除后，只传入NAME，按NAME查询 ==");
		query.clearParameters();
		query.setParameter("person_name", "张"); //只传入NAME时，其他三个条件消失
		System.out.println(query.getResultList());
	}
	{
		System.out.println("== 按NAME+GENDER查询 ==");
		query.setParameter("gender", "F");  //传入GENDER和NAME时，其他两个条件消失
		System.out.println(query.getResultList());
	}
	{
		query.clearParameters();    //一个条件都不传入时，整个where子句全部消失
		System.out.println(query.getResultList());
	}
}
~~~

上面列举了五种场合，每种场合都没有完整的传递四个WHERE条件。

上述实现是基于SQL抽象词法树（AST）的。表达式省略功能的定义是，如果一个绑定变量参数条件（如 = > < in like等）一端无效，那么整个条件都无效。如果一个二元表达式（如and or等）的一端无效，那么就退化成剩余一端的表达式。基于这种规则，NativeQuery能够将未设值的条件从查询语句中去除。来满足动态SQL的常见需求。

>  **使用 绑定变量 + 动态SQL 开发Web应用的列表视图**
>
>
>​       *这种常见需求一般发生在按条件查询中，比较典型的一个例子是用户Web界面上的搜索工具栏，当用户输入条件时，按条件搜索。当用户未输入条件时，该字段不作为搜索条件。使用动态SQL功能后，一个固定的SQL**语句就能满足整个视图的所有查询场景，极大的简化了视图查询的业务操作。*
>​       *在一些传统应用中，开发者不得不使用许多IF分支去拼装SQL/HQL语句。为此产生了大量的重复编码。除此之外，还有一个丑陋的 1=1” 条件（写过这类代码的人应该知道我在说什么。）这种SQL除了消耗额外的数据库解析时间外，也令数据库优化变得更为困难。*
>​       *配合上一节讲到的绑定变量类型自动转换功能，使用EF-ORM在开发此类Web应用时，只要一个SQL语句加上极少的代码，就能完成所需的业务逻辑。如果在Web控制层(Action)中进行少量封装后，基本可以做到后台逻辑开发零编码。*
>

最后，还要澄清一点——什么叫“不传入参数”。实时上，不传入参数表示自从NativeQuery构造或上次清空参数之后，都没有调用过setParameter()方法来设置参数的值。将参数设置为””或者null并不表示不设置参数的值。下面的例子说明了这一点。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~java
@Test
public void testDynamicSQL3(){
	String sql="select * from t_person where id not in (:ids)";
	NativeQuery<Person>  query=db.createNativeQuery(sql,Person.class);
	//将参数值设置为null，并不能起到清空参数的作用
	query.setParameter("ids", null); 
	System.out.println(query.getResultList());
}
~~~

目前动态表达式省略可以用于两种场景，一是where条件，二是update语句中的set部分（见下面例子）。其他场合，如Insertinto语句中的列等不支持这种用法。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~java
/**
 * 动态表达式省略——不仅仅是where条件可以省略，update中的赋值表达式也可以省略
 */
@Test
public void testDynamicSQL4(){
	String sql="update t_person set person_name=:new_name,  current_school_id=:
	            new_schoold_id,gender=:new_gender where id=:id";
	NativeQuery<Person>  query=db.createNativeQuery(sql,Person.class);
	query.setParameter("new_name", "孟德");
	query.setParameter("id", 1);
	int count=query.executeUpdate();
}
~~~

### 7.3.6.  动态SQL片段

有一种特别的NativeQuery参数类型，\<SQL>表示一个SQL片段。严格来说，这其实不是一种绑定变量的实现。凡是标记为\<SQL>类型的变量，都是被直接加入到SQL语句中的。

比如：

 orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~java
@Test
public void testDynamicSqlExpression(){
	String sql="select :columns<sql> from t_person where " +
			"id in (:ids<int>) and person_name like :person_name<$string$> " +
			"order by :orders<sql>";
	
	NativeQuery<Person> query=db.createNativeQuery(sql,Person.class);
	//查询哪些列、按什么列排序，都是在查询创建以后动态指定的。
	query.setParameter("columns", "id, person_name, gender");
	query.setParameter("orders", "gender asc");
	
	System.out.println(query.getResultList());
	
	//动态SQL片段和动态表达式省略功能混合使用
	query.setParameter("ids", new int[]{1,2,3});
	query.setParameter("columns", "person_name, id + 1000 as id");
	System.out.println(query.getResultList());
}
~~~

上面的例子中 :columns :orderby中被设置的值，都是SQL语句的一部分，通过这些动态的片段来重写SQL。第一次的查询，在Where条件没有任何输入的情况下where子句被省略。最后实际执行的SQL语句是这样——

~~~sql
select id, person_name, gender from t_person order by gender asc
~~~

第二次查询，select表达式发生了一些变化，最后执行的SQL语句是这样——

~~~sql
select person_name, id + 1000 as id from t_person where id IN (?,?,?) order by gender asc
~~~

前面说了，在查询语句中也可以省略参数类型，比如上例，我们写作

~~~sql
select :columns from t_person order by :orders
~~~

此时，我们还能将orderBy当做是SQL片段，而不是运行时的绑定变量参数处理吗？

答案是可能的，只要我们传入的参数是SqlExpression对象，那么就会被当做是SQL片段，直接添加到SQL语句中。

~~~java
query.setParameter("orders", new SqlExpression("id desc"));
~~~

### 7.3.7.  分页查询

NativeQuery对象支持分页查询。除了之前7.2.2中我们了解到的getResultCount()方法和setRange()方法外，还有已经封装好的方法。在Session对象中，我们可以从NativeQuery对象得到PagingIterator对象。

为了简化操作Session中还提供两个方法，直接将传入的SQL语句包装为NativeQuery，再从NativeQuery得到PagingIterator对象。

| 方法                                       | 返回值                | 用途                                       |
| ---------------------------------------- | ------------------ | ---------------------------------------- |
| Session.pageSelect(NativeQuery\<T>,  int) | PagingIterator\<T> | 从NativeQuery得到PagingIterator对象。          |
| Session.pageSelect(String,  Class\<T>, int) | PagingIterator\<T> | 将传入的SQL语句包装为NativeQuery，再从NativeQuery得到PagingIterator对象。 |
| Session.pageSelect(String,  ITableMetadata, int) | PagingIterator\<T> | 将传入的SQL语句包装为NativeQuery，再从NativeQuery得到PagingIterator对象。 |

上述API的实际用法如下

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~java
@Test
public void testNativeQueryPage() throws SQLException{
	//SQL语句中写了四个查询条件
	String sql="select * from t_person where id=:id " +
			"and person_name like :person_name<$string$> " +
			"and currentSchoolId=:schoolId " +
			"and gender=:gender";
	NativeQuery<Person> query=db.createNativeQuery(sql,Person.class);
	query.setParameter("gender", 'F');
	
	//每页5条，跳过最前面的2条记录
	Page<Person> page=db.pageSelect(query, 5).setOffset(2).getPageData();
	System.out.println("总共:"+page.getTotalCount()+" "+page.getList());
}
~~~

PagingIteraor对象的用法在前面已经介绍过了。这个对象为所有不同的查询方式提供了统一的分页查询接口。

NativeQuery分页应用的要点和前文一样——开发者无需关心COUNT语句和分页的写法，将这部分工作交给框架自行完成。

### 7.3.8.  为不同RDBMS分别编写SQL

为了让SQL语句能被各种数据库识别，EF-ORM会在SQL语句和数据库函数层面进行解析和重写，但这不可能解决一切问题。在命名查询中，还允许开发者为不同的RDBMS编写专门的SQL语句。在运行时根据当前数据库动态选择合适的SQL语句。

一个实际的例子是这样——

~~~properties
<query name = "test-tree#oracle" type="sql">
	<![CDATA[
		select * from t_person t 
		START WITH t.id IN (:value)
		CONNECT BY PRIOR t.id = t.parent_id
		]]>
</query>
<query name = "test-tree" type="sql">
	<![CDATA[
		select * from t_person
	]]>
</query>
~~~

在上例中，名为”*test-tree*”的命名查询有了两句SQL，一句是Oracle专用的，一句是其他数据库通用的。

一般来说，我们配置命名查询的名称是这样的，

~~~properties
<query name = "test-tree" >
~~~

​		在所有RDBMS上使用的语句

但我们可以为查询名后面加上一个修饰——

~~~properties
<query name = "test-tree"#oracle >
~~~

                 优先在Oracle上使用的语句

这个查询的名称依然是叫“test-tree”，但这个配置只会在特定的数据库(Oracle)上生效。

~~~java
@Test
public void testNativeQueryPage2() throws SQLException{
    //查询名还是叫test-tree。#后面的是修饰，不是查询名称的一部分
	NativeQuery<Person> query=db.createNamedQuery("test-tree");
	if(query.containsParam("value")){ //检查SQL是否需要该参数
		query.setParameter("value", 100);
	}
	System.out.println(query.getResultList());
}
~~~

在运行时，如果当前数据库是oracle，那么会使用专用的SQL语句，如果当前数据库是其他的，那么会使用通用的SQL语句。由于编码时不确定会使用哪个SQL语句，所以在设置参数前可以用containsParam检查一下是否需要该参数。

一般情况下，我们的查询名称中不用带RDBMS类型。这意味着该SQL语句可以在所有数据库上生效。

当RDBMS下SQL写法差别较大时，开发者可以使用这种用法，针对性的为不同的数据库编写SQL语句。

### 7.3.9.  对Oracle Hint的支持

正常情况下，解析并重写后的SQL语句中的注释都会被除去。但是在Oracle数据库上，我们可能会用Oracle Hint的方式来提示优化器使用特定的执行计划，或者并行查询等。一个Oracle Hint的语法可能如下所述

~~~sql
select /* + all_rows*/ * from dave;
~~~

基于抽象词法树的解析器对注释默认是忽略的，但为了支持Oracle Hint的用法，EF-ORM作了特别的处理，在特殊处理后，只有紧跟着SELECT  UPDATE  DELETE INSERT四个关键字的SQL注释可以被保留下来。一般情况下，Oracle Hint也就在这个位置上。

### 7.3.10.  对Limit m,n / Limit n Offset n的支持

在PostgresSQL和MySQL中，都支持这种限定结果集范围的写法。这也是最简单的数据库分页实现方式。

我们首先来回顾一下SQL中Limit Offset的写法和几种变体：

1. LIMIT nOFFSET m    跳过前m条记录，取n条记录。
2. LIMITm,n             跳过前m条记录，取n条记录。(注意这种写法下 n,m的顺序是相反的)
3. LIMIT ALLOFFSET m  跳过前m条记录，取所有条记录。
4. OFFSETm            跳过前m条记录，取所有条记录。(即省略LIMIT ALL部分)
5. LIMIT nOFFSET 0     取n条记录。
6. LIMIT n               取n条记录。(即省略 OFFSET 0部分)


以上就是LIMIT的SQL语法。

​在E-SQL中，如果用户传入的SQL语句是按照上述语法进行分页的，那么EF-ORM会将其改写成适合当前RDBMS的SQL语句。即——

在非Postgresql或MySQL上，也能正常进行结果集分页。

​在支持LIMIT语句的RDBMS上（如MySQL/Postgresql）上，LIMIT关键字将出现在SQL语句中。

### 7.3.11.  对Start with ... Connect by的有限支持

Oracle支持的递归查询是一个让其他数据库用户很“怨念”的功能。这种语法几乎无法在任何其他数据库上使用，然而其用途却无可替代，并且难以用其他函数模拟。除了Postgres也有类似的递归查询用法外，在其他数据库上只有通过复杂的存储过程了……这使得开发要支持多数据库的产品变得更为困难。

EF-ORM却可以在所有数据库上，在一定程度上支持这种操作。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~java
/**
 * 在非Oracle数据库上支持递归查询
 */
@Test
public void testStartWithConnectBy() throws SQLException {
	//准备一些数据
	db.truncate(NodeTable.class);
	List<NodeTable> data=new ArrayList<NodeTable>();
	data.add(new NodeTable(1,0,"-Root"));
	data.add(new NodeTable(2,1,"水果"));
	data.add(new NodeTable(5,2," 西瓜"));
	data.add(new NodeTable(10,2," 葡萄"));		
	data.add(new NodeTable(4,2," 苹果"));
	data.add(new NodeTable(8,4,"  青涩的苹果"));
	data.add(new NodeTable(12,4,"  我的小苹果"));
	data.add(new NodeTable(3,1,"家电"));
	data.add(new NodeTable(6,3," 电视机"));
	data.add(new NodeTable(7,3," 洗衣机"));
	data.add(new NodeTable(11,6,"  彩色电视机"));
	db.batchInsert(data);
		
	String sql = "select * from nodetable t START WITH t.id IN (4,6) CONNECT BY PRIOR t.id = t.pid";
	NativeQuery<NodeTable> query = db.createNativeQuery(sql, NodeTable.class);
	List<NodeTable> result=query.getResultList();
	for(NodeTable p:result){
		System.out.println(p);
	}
}
~~~

上面的语句看似是不可能在Derby数据库上执行的，然而运行这个案例，你可以看见正确的结果。为什么呢？

这是因为EF-ORM 1.9.2以后，开始支持查询结果”后处理“。所谓“后处理”就是指对查询结果在内存中再进行过滤、排序等处理。这一功能本来是为了满足多数据库路由操作下的排序、group by、distinct等复杂操作而设计的，不过递归查询也得以从这个功能中获益。对于一些简单使用递归查询的场合，EF-ORM可以在内存中高效的模拟出递归效果。当然，在递归计算过程中需要占用一定的内存。

为什么说是”一定程度上支持这种操作“呢？因为目前对此类操作的支持限制还非常多，当前版本下，要使用内存计算模拟递归查询功能，有以下条件。

1. start with... Connect by条件必须在顶层的select中，不能作为一个嵌套查询的内层。
2. Connect by的键值只允许一对。
3. Start with条件和connect by的键值这些列都必须在查询的结果中。
4. Start with目前还只支持一个条件，不支持AND OR。



## 7.4.  存储过程调用

使用EF-ORM封装后的存储过程调用，可以——

* 指定存储过程的入参出参，并帮助用户传递。
* 将存储过程传出的游标，映射为合适的java对象。
* 支持匿名存储过程
* 和其他操作处于一个事务中


只要是数据库能支持的存储过程，EF-ORM都可以调用。存储过程调用过程会封装为一个NativeCall对象。使用该对象即可传入/取出存储过程的相关参数。对于游标类的传出参数，还可以直接转换为java bean。

### 7.4.1.  使用存储过程

我们还是先从一个例子开始。由于Derby在存储过程上功能较弱，我们这个例子需要在MySQL下运行。存储过程为——

~~~sql
CREATE PROCEDURE update_salary (IN employee_number CHAR(6), IN rating INT)
LANGUAGE SQL
BEGIN

CASE rating
WHEN 1 THEN 
UPDATE employee
SET salary = salary * 1.10, bonus = 1000
WHERE empno = employee_number;
WHEN 2 THEN 
UPDATE employee
SET salary = salary * 1.05, bonus = 500
WHERE empno = employee_number;
ELSE
UPDATE employee
SET salary = salary * 1.03, bonus = 0
WHERE empno = employee_number;
END CASE;
END @
~~~

数据准备和测试代码：

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case3.java

~~~java
@BeforeClass
public static void setup() throws SQLException {
	new EntityEnhancer().enhance("org.easyframe.tutorial");
		
	db = new DbClient("jdbc:mysql://localhost:3307/test","root","admin",5);
	ORMConfig.getInstance().setDebugMode(false);
	db.dropTable(Employee.class);
	db.createTable(Employee.class);
	Employee e=new Employee();
	e.setId("100000");
	e.setName("刘备");
	e.setSalary(10000.0);
	db.insert(e);
	e=new Employee();
	e.setId("100001");
	e.setName("关羽");
	e.setSalary(8000.0);
	db.insert(e);
	e=new Employee();
	e.setId("100002");
	e.setName("张飞");
	e.setSalary(7000.0);
	db.insert(e);
	ORMConfig.getInstance().setDebugMode(true);
	DbMetaData meta=db.getMetaData(null);
	if(!meta.existsProcdure(null, "update_salary")){		
			//如果存储过程不存在，就创建存储过程
			meta.executeScriptFile(Case3.class.getResource("/update_salary.sql"),"@");
	}
}
	
@Test
public void testProducre() throws SQLException{
	System.out.println("调整工资前——");
	System.out.println(db.selectAll(Employee.class));
		
	NativeCall nc=db.createNativeCall("update_salary", String.class,int.class);
	nc.execute("100002",2);
	nc.execute("100001", 1);
	System.out.println("调整工资后——");
	System.out.println(db.selectAll(Employee.class));
}
~~~

在运行上面的案例前请先修改

~~~~java
 db = new DbClient("jdbc:mysql://localhost:3307/test","root","admin",5);
~~~~

行中的URL，将其配置为一个可以连接的MySQL地址。在testProducre() 方法中对100001和100002号雇员进行了不同的加薪处理。通过存储过程调用前后输出的雇员信息变化，可以看到存储过程的效果。

### 7.4.2.  存储过程传出参数

假设有存储过程如下

~~~sql
create procedure check_user(IN _name varchar(40),OUT userCount int)
BEGIN
  select count(*) into userCount from D where name like _name;
END
$$
~~~

该存储过程传出一个数值。

~~~java
@Test
public void testProducre2() throws SQLException{
	NativeCall call2 = db.createNativeCall("check_user", 
			String.class, OutParam.typeOf(Integer.class));  //设置存储过程入参和出参
	call2.execute("张三");
	Object obj = call2.getOutParameter(2);
	LogUtil.show(obj);
}
~~~

在上例中，存储过程的出参设置，需要用一个名为OutParam的工具类。使用这个类，可以生成若干出参的类型表示。OutParam.*typeOf*(Integer.**class**)表示一个Integer类型的传出参数。

 在存储过程执行完后，使用getOutParameter(2);方法可以得到存储过程的传出参数，这里的2表示出参是第二个参数，这个序号需要和之前定义的参数需要一致，一个存储过程可以传出多个参数，因此要用序号加以区别。得到的传出参数类型和之前定义的一致，是Integer类型。

### 7.4.3.  存储过程传出游标

Oracle数据库上的存储过程可以传出游标。存储过程如下：

~~~sql
CREATE OR REPLACE PACKAGE TESTPACKAGE  AS
 TYPE TYPE_PERSON IS REF CURSOR;
end TESTPACKAGE;
/

create or replace PROCEDURE GET_ALL(p_CURSOR out TESTPACKAGE.TYPE_PERSON) IS
BEGIN
  OPEN p_CURSOR FOR SELECT * FROM T_PERSON;
END GET_ALL_USER;
/
~~~

这里的游标类型就是t_person表。因此该存储过程相当于返回了一个在t_person表上的查询结果集。

EF-ORM可以将游标类型的结果集重新映射为java对象。

~~~java
@Test
public void testProducre3() throws SQLException{
	NativeCall call3 = db.createNativeCall("GET_ALL", OutParam.listOf(Person.class));
	call3.execute();
	List<Person> obj = call3.getOutParameterAsList(1, Person.class);
	call3.close();
	Assert.assertTrue(obj.size() > 0);
}
~~~

在上例中，存储过程的出参设置，需要用一个名为OutParam的工具类。使用这个类，可以生成若干出参的类型表示。OutParam.*listOf*(Person.**class**)表示传出的游标将被包装为Person类型的List。

在存储过程执行完后，使用getOutParameterAsList(1,Person.class);方法可以得到存储过程的传出参数。其中，1是传出参数在存储过程定义中的序号。

大部分数据库都不支持传出游标。这个案例仅对支持的数据库（如Oracle）有效。

最后，要注意的是由于游标的存在，需要显式的去关闭NativeCall对象，否则会发生游标泄露问题。

### 7.4.4.  使用匿名过程（匿名块）

在Oracle中，还可以执行临时编写的未命名的匿名块。匿名块的语法和存储过程基本一致。

对应到EF-ORM可以这样操作——

~~~java
@Test
public void testProducre4() throws SQLException{

    String sql = "declare " + 
    "    l_line    varchar2(255); " + 
    "    l_done    number; " + 
    "    l_buffer long; " + 
    "begin " + 
    "  loop " +
    "    exit when length(l_buffer)+255 > :maxbytes OR l_done = 1; " + 
    "    dbms_output.get_line( l_line, l_done ); "	+
    "    l_buffer := l_buffer || l_line || chr(10); " + 
    "  end loop; " + 
    " :done := l_done; " + 
    " :buffer := l_buffer; " + 
    "end;";

    NativeCall call = db.createAnonymousNativeCall(sql, Integer.class, 
           OutParam.typeOf(Integer.class), OutParam.typeOf(String.class));
    call.execute(1, 26000);
    System.out.println(call3.getOutParameter(2) + "   ||   " + call3.getOutParameter(3));
}
~~~

在上例中，用户临时定义了一个匿名块。EF-ORM中调用的方法为createAnonymousNativeCall()。该方法允许用户得到一个匿名块构成的NativeCall对象。匿名块的入参设置和出参获取和存储过程完全一样。

## 7.5.  原生SQL使用

### 7.5.1.  使用原生SQL查询

前面我们已经了解了EF-ORM对SQL的封装和改进。基于这种改进，我们使用E-SQL来享受改进所带来的优点——让SQL在各种RDBMS上运行；和业务代码更好的集成等等。

但是，麻烦必然伴随而来。SQL解析和改写器并不是总能完美的工作——

* SQL解析和改写是一个复杂的过程，尽管经过很多努力优化，但是每个SQL的解析依然要花费0.05到1毫秒不等的时间。可能不满足追求性能极限的场合。
* 一些过于复杂的，或者我们开发时没有测试到的SQL写法可能会解析错误。（**请将解析错误的SQL语句发给我们，谢谢。**）

EF-ORM内置的SQL分析器能处理绝大多数数据库DDL和DML语句。包括各种建表、删表、truncate、Create Table as、Select嵌套、Oracle分析函数、Oracle树型关系选择语句等。但是RDBMS的多样性和SQL语句的复杂性使得完全解析多少有些难度，因此EF-ORM依然保留原生的，不经过任何改写的SQL查询方式，作为NativeQuery在碰到以下麻烦时的”逃生手段“。

​原生SQL和NativeQuery不同，不进行解析和改写。直接用于数据库操作。

​明显的影响，原生SQL中，绑定变量占位符和E-SQL不同，用一个问号表示，和我们直接操作JDBC时一样------

~~~sql
select * from t_person where id=? and name like ?
~~~

首先我们可以用Session对象(DbClient)中的selectBySql方法进行查询。看这个例子

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case1.java

~~~sql
@Test
public void testRawSQL() throws SQLException{
	String sql="select id, person_name,gender from t_person";
	{ 	//普通的原生SQL查询
		List<Person> result=db.selectBySql(sql,Person.class);
		System.out.println(result);
		assertEquals(3, result.size());	
	}
	{ 	//限定结果范围——分页
		List<Person> result=db.selectBySql(sql, new Transformer(Person.class), new IntRange(2,3));	
		System.out.println(result);
		assertEquals(2, result.size());
	}
	{  //使用绑定变量
		sql="select * from t_person where person_name like ? or gender=?";
		List<Person> result=db.selectBySql(sql, Person.class,"刘","F");	
		System.out.println(result);
		assertEquals(3, result.size());
	}
	{	//执行原生SQL
		sql="insert into t_person(person_name,gender) values(?,?)";
		db.executeSql(sql, "曹操","M");
		db.executeSql(sql, "郭嘉","M");
		assertEquals(5, db.getSqlTemplate(null).countBySql("select count(*) from t_person"));
	}	
}
~~~

上面使用了Session对象中的selectBySql方法，在Session中，这个系列的方法是可以使用原生SQL的------

| 方法                                       | 说明                                       |
| ---------------------------------------- | ---------------------------------------- |
| Session.executeSql(String,  Object...)   | 执行指定的SQL语句                               |
| Session.getResultSet(String,  int, Object...) | 根据SQL语句获得ResultSet对象                     |
| Session.selectBySql(String,  Class\<T>, Object...) | 根据SQL查询，返回指定的对象                          |
| Session.loadBySql(String,  Class\<T>, Object...) | 根据SQL查询，返回指定的对象（单行）                      |
| Session.selectBySql(String,  Transformer, IntRange, Object...) | 根据SQL查询，传入自定义的结果转换器和分页信息                 |
| Session.getSqlTemplate(String)           | 获得指定数据源下的SqlTemplate对象。SQLTempate是一个可以执行各种SQL和本地化查询的操作句柄。 |

Session对象中，凡是xxxxBySql()这样的方法，都是传入原生SQL语句的。同时这些方法都提供了可变参数，其中的Obejct... 对象就是绑定变量参数。使用时按顺序传入绑定变量就可以了。

最后一个方法，可以得到SqlTemplate对象，SqlTemplate对象是一个可以执行各种SQL和本地化查询的操作句柄。

**在拼装SQL时处理Schema映射和函数本地化问题**

​使用原生SQL，意味着开发者要自行解决schema重定向和数据库函数本地化的问题。可以使用下面两个方法来帮助获得相关的信息。

| 方法                                       | 说明                                       |
| ---------------------------------------- | ---------------------------------------- |
| MetaHolder.getMappingSchema(String)      | 传入开发时的虚拟schema，返回实际运行环境对应的schema。用于拼装到原生SQL中。 |
| Session.func(DbFunction,  Object...)     | 传入函数名和参数。返回该函数在当前数据库下的方言实现。              |
| SqlTemplate.func(DbFunction,  Object...) | 效果同上，区别是使用了特定数据源的方言。                     |

上述三个方法都可以返回String，供开发人员自行拼装SQL语句使用。

### 7.5.2.  SqlTemplate

由于EF-ORM支持多数据源，因此要在特定数据源上执行SQL操作时，都要先获得对应的SqlTemplate对象。前面的各种示例中，都是在Session上直接操作本地化查询和SQL的，这种操作方式只会在默认数据源上操作。因此SQLTemplate除了提供更多原生SQL的操作方法以外，还是操作多数据源时必须使用的一个对象。

要获得一个数据源的SqlTemplate对象，可以使用——

~~~java
db.getSqlTemplate(null);//获得默认数据源的SqlTemplate

db.getSqlTemplate("datasource1");//获得datasource1的SqlTemplate
~~~

得到了SqlTemplate

SqlTemplate对象的使用。SqlTemplate中有很多方法是和本地化查询有关的，也有使用原生SQL语句的查询。(详情参阅API-DOC)

| 方法                                       | 作用                         |
| ---------------------------------------- | -------------------------- |
| getMetaData()                            | 获得数据源元数据操作句柄               |
| createNativeQuery(String,  Class\<T>)    | 创建本地化查询                    |
| createNativeQuery(String,  ITableMetadata) | 创建本地化查询                    |
| createNativeCall(String,  Type...)       | 创建存储过程调用。                  |
| createAnonymousNativeCall(String,  Type...) | 创建匿名过程调用。                  |
| pageSelectBySql(String,  Class\<T>, int) | 按原生SQL分页查询                 |
| pageSelectBySql(String,  ITableMetadata, int) | 按原生SQL分页查询                 |
| countBySql(String,  Object...)           | 按SQL语句查出Long型的结果           |
| executeSql(String,  Object...)           | 执行SQL语句                    |
| loadBySql(String,  Class\<T>, Object...) | 按SQL语句查出指定类型结果（单条记录）       |
| selectBySql(String,  Class\<T>, Object...) | 按SQL语句查出指定类型结果             |
| selectBySql(String,  Transformer, IntRange, Object...) | 按SQL语句查出指定类型结果(带分页范围)      |
| iteratorBySql(String,  Transformer, int, int, Object...) | 按SQL语句查出指定类型结果，以遍历器形式返回。   |
| executeSqlBatch(String,  List<?>...)     | 执行SQL语句，可以传入多组参数并在一个批次内执行。 |

 使用SqlTemplate的示例——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson7\Case2.java

~~~java
@Test
public void testSqlTemplate() throws SQLException{
	//获得在datasource2上执行SQL操作的句柄
	SqlTemplate t=db.getSqlTemplate("datasource2");
	List<Person> person=t.selectBySql("select * from t_person where gender=?", Person.class, "F");
	System.out.println(person);
}
~~~

SqlTemplate中的其他方法以此类推，详见java-doc。

## 7.6.  无表查询

无表查询是一类特殊的SQL查询。比如，查询当前数据库时间，在Oracle中，是通过一张虚拟表DUAL完成的——

~~~sql
select sysdate from dual
~~~

在MySQL中，无表查询是这样的------

~~~sql
select current_timestamp
~~~

在Derby中，无表查询是这样的------

~~~sql
values current_timestamp
~~~

显然，不同的数据库的无表查询语法是不一样的。为了兼容不同数据库的无表查询场景，框架提供了相应的API。和无表查询相关的API主要有getExpressionValue()系列的方法，该方法可以执行一次无表查询。

**API列表**

| 方法                                       | 用途                                |
| ---------------------------------------- | --------------------------------- |
| **Session****中的方法**                      |                                   |
| getExpressionValue(String,  Class\<T>)   | 传入SqlExpression对象，计算表达式的值。        |
| **SqlTempate****中的方法**                   |                                   |
| getExpressionValue(String,  Class\<T>, Object...) | 传入String对象。得到指定的SQL表达式的查询结果(无表查询) |
| getExpressionValue(DbFunction,  Class\<T>, String...) | 得到指定的SQL函数的查询结果(无表查询)             |

 

使用举例

~~~java
@Test
public void testExpressionValue() throws SQLException{
	//传入复杂表达式时，其函数和语法会被改写
	String s="'今天是'||str(cast(year(sysdate)/100+1 as int))||'世纪'";
	assertEquals("今天是21世纪", db.getExpressionValue(s, String.class));
	
	//要在某个特定数据库上执行无表查询，可以用SqlTemplate
	SqlTemplate t=db.getSqlTemplate(null);
	//直接传入数据库函数
	Date dbTime=t.getExpressionValue(Func.current_timestamp, Date.class);
	System.out.println("当前时间为:"+dbTime);
}
~~~

​Session中的无表查询方法，将在默认数据源上计算指定的数据库表达式；SqlTemplate中的同名方法可以在指定数据源上计算表达式。表达式中出现的函数会被解析和改写。此外SqlTemplate还提供了一个直接传入Func对象计算的函数。