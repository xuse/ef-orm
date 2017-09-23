GeeQuery使用手册——Chapter-11  与Spring集成

[TOC]

# Chapter-11  与Spring集成

EF-ORM主要通过实现部分JPA接口和Spring集成。EF-ORM将被Spring认为是一个JPA实现一样提供支持。

## 11.1.  典型配置（快速入门） 

对于只想在自己的项目中快速使用EF-ORM，看本节就够了。后面的章节可以跳过。

本节内容适合于熟悉Spring事务管理机制的同学，在面对日常单个数据库连接时，可以直接使用下面的典型配置并自行修改。

~~~xml
<bean id="dataSource" class="jef.database.datasource.SimpleDataSource"
	p:url="jdbc:mysql://127.0.0.1:3306/cms?useUnicode=true" p:user="root" 
	p:password="12345"/>

<bean id="entityManagerFactory" class="org.easyframe.enterprise.spring.SessionFactoryBean">
	<property name="dataSource" ref="dataSource" />
</bean>
<bean id="commonDao" class="org.easyframe.enterprise.spring.CommonDaoImpl" />

<tx:annotation-driven transaction-manager="transactionManager" proxy-target-class="true" />
<aop:aspectj-autoproxy />
<context:component-scan base-package="com.company.my.application"/>
	
<bean id="transactionManager" class="org.springframework.orm.jpa.JpaTransactionManager">
	<property name="entityManagerFactory" ref="entityManagerFactory" />
	<property name="jpaDialect">
		<bean class="org.easyframe.enterprise.spring.JefJpaDialect" />
	</property>
</bean>
~~~

上面这段配置，数据源、SessionFactory、TransactionManager、基于注解的事务声明都有了。然后在编写一个自己的DAO------

~~~java
package com.company.my.application;

import org.easyframe.enterprise.spring.GenericDaoSupport;
@Repository
public class StudentDaoImpl extends GenericDaoSupport<Student> {
	public void gradeUp(Collection<Integer> ids) {
		Student st=new Student();
		st.getQuery().addCondition(Student.Field.id, Operator.IN, ids);
		st.prepareUpdate(Student.Field.grade, new JpqlExpression("grade+1"));
		try {
			//super.getSession()可以得到EF的Session对象
			getSession().update(st);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}
} 
~~~

在Dao中，使用super.getSession()方法可以得到当前事务中的Session对象进行数据库操作。

在Service中，使用@Transactional注解进行事务控制。（请参阅Spring官方文档）

## 11.2.  配置和使用

### 11.2.1.  SessionFactory的配置 

前面的所有例子中，EF的核心对象是一个DbClient对象。DbClient里封装了所有的数据库连接和对应的ORM操作逻辑。

在JPA中，起到类似作用的对象是javax.persistence.EntityManagerFactory类，其地位就和某H框架的SessionFactory一样。

~~~xml
	<!-- 配置数据源，可以带连接池也可以不带 -->
	<bean id="dataSource" class="jef.database.datasource.SimpleDataSource"
		p:url="${db.url}" p:username="${db.user}" p:password="${db.password}" />

	<bean id="entityManagerFactory" class="org.easyframe.enterprise.spring.SessionFactoryBean">
		<property name="dataSource" ref="dataSource" />
	</bean>
	<bean id="commonDao" class="org.easyframe.enterprise.spring.CommonDaoImpl" />
~~~

​					 代码 11-1 单数据源的EntityManagerFactory配置 

上面的配置中，用Spring的FactoryBean创建EntityManagerFactory对象。EntityManagerFactory是JPA规范中的数据操作句柄。其类似于某H框架和某batis的SessionFactory。

上面的配置，除了定义了EntityManagerFactory对象以外，还定义一个CommonDao对象，该对象实现了org.easyframe.enterprise.spring.CommonDao接口。其作用是仿照传统Dao习惯，提供一个能完成基本数据库操作的对象。具体用法参见11.1.4节。

org.easyframe.enterprise.spring.SessionFactoryBean这个Bean支持以下的配置参数：

| 参数                              | 用途                                       | 备注或示例                                    |
| ------------------------------- | ---------------------------------------- | :--------------------------------------- |
| dataSource                      | 指定一个数据源。                                 | javax.sql.DataSource的子类即可。               |
| dataSources                     | map类型，指定多个数据源                            | \<property  name="dataSources">  \<map> \<entry  key="ds1" value-ref="ds1" />\<entry  key="ds2" value-ref="ds2" /> \</map>                                                                                                         \</property> |
| defaultDatasource               | 多数据源时，指定缺省数据源                            | \<property  name="defaultDatasource" value="ds1"> |
| packagesToScan                  | 配置包名，启动时会扫描这些包下的所有实体类并加载。                | \<property  name="packageToScan">\<list>      \<value>org.easyframe.test\</value>      \<value>org.easyframe.entity\</value>   \</list>\</property> |
| annotatedClasses                | 配置类名，启动时会扫描这些类并加载                        | \<property  name="annotatedClasses">\<list>   \<value>org.easyframe.testp.jta.Product\</value>   \<value>org.easyframe.testp.jta.Users\</value>   \</list>\</property> |
| createTable                     | boolean类型。当扫描到实体后，如果数据库中不存在，是否建表         | 默认true，可以关闭  \<property  name="createTable" value="false" /> |
| alterTable                      | boolean类型。当扫描到实体后，如果数据库中存在，是否修改表         | 默认true，可以关闭  \<property  name="alterTable" value="false" /> |
| allowDropColumn                 | boolean类型。当扫描到实体后，如果数据库中存在并且需要修改时，是否可以删除列 | 默认false，可以开启  \<property  name="allowDropColumn" value="true" /> |
| enhancePackages                 | 配置包名，启动时先对指定包下的实体进行一次增强，多个包用逗号分隔。        | 扫描增强只能对目录下的class文件生效，对ear/war/jar包中class无效。由于大部分J2EE容器都支持包方式部署，此功能只建议在单元测试时使用，不建议发布class未增强的包。  \<property  name="enhancePackages" value="org.easyframe.tutorial"  /> |
| dynamicTables                   | 配置数据库表名，启动时扫描这些表，生成动态表模型。表名之间逗号分隔        | 参见动态表相关功能说明。  \<property  name="dynamicTables" value="EF_TABLE1,XX_TABLE2,TABLE3"  /> |
| registeNonMappingTableAsDynamic | 对比当前数据库中存在的表，如果数据库中的表并未被任何实体所映射，那么生成这张表的动态表模型。 | 该功能可以将所有未被映射的表当做动态表，建立对应的动态元模型，参见动态表相关功能说明。默认false  \<property  name="registeNonMappingTableAsDynamic" value="true" /> |

### 11.2.2.  多数据源的配置

前面提到EF-ORM原生支持分库分表，分库分表意味着EF-ORM要能支持多个数据库实例。

最简单的多数据库下的配置如下

~~~xml

	<!-- 配置多个数据源的DataSource，可以配置带连接池的DataSource，也可以配置不带连接池的DataSource
      甚至可以混合使用（没必要的情况下不建议这么做） -->
	<bean id="dataSource-1" class="jef.database.datasource.SimpleDataSource"
		p:url="${db.url}" p:username="${db.user}" p:password="${db.password}" />
	<bean id="dataSource-2" class="com.alibaba.druid.pool.DruidDataSource"
		destroy-method="close" 
		p:driverClassName="${db.driver2}"
		p:url="${db.url2}" 
		p:username="${db.user2}"
		p:password="${db.password2}" 
		p:initialSize=3 
		p:minIdle=1 
		p:maxIdle=20
		p:maxActive=50
		/>
	<bean id="dataSource-3" class="org.apache.commons.dbcp.BasicDataSource"
		destroy-method="close" 
		p:driverClassName="${db.driver3}"
		p:url="${db.url3}" 
		p:username="${db.user3}"
		p:password="${db.password3}" 
		p:initialSize=3 
		p:minIdle=1 
		p:maxIdle=20
		p:maxActive=50 
		/>
	<bean id="routingDS" class="jef.database.datasource.RoutingDataSource">
		<property name="dataSourceLookup">
			<bean class="jef.database.datasource.SpringBeansDataSourceLookup" />
		</property>
	</bean>

	<bean id="entityManagerFactory" class="org.easyframe.enterprise.spring.SessionFactoryBean">
		<property name="dataSource" ref="routingDS" />
	</bean>
	<bean id="commonDao" class="org.easyframe.enterprise.spring.CommonDaoImpl" />
~~~

 						代码 11-2 多数据源的EntityManagerFactory配置

多数据库下，需要声明一个RoutingDataSource对象。而RoutingDataSource中，可以配置一个DataSourceLookup对象。DataSourceLookup对象提供多个真正的数据源，通过配置不同的DataSourceLookup，可以实现从不同的地方读取数据源。

上面的配置方法中，定义了***dataSource-1,dataSource-2,dataSource-3***三个原始数据源，存放在Spring的ApplicationContext中。而对应的DataSourceLookup对象是SpringBeansDataSourceLookup，该对象可以从Spring上下文中查找所有的DataSource对象。

框架提供了多个DataSourceLookup，用于在从不同的地方读取数据源配置。这些数据源获取器包括以下几种。

####* URLJsonDataSourceLookup

使用HTTP访问一个URL，从该URL中获得数据源的配置信息。返回的数据源信息用JSON格式表示。参见下面的例子。

如果我们HTTP GET [http://192.168.0.1/getdb](http://192.168.0.1/getdb)可以返回如下报文

~~~json
[{
        "id": "ds1",
        "url": "jdbc:mysql://localhost:3306/test",
        "user": "root",
        "password": "123456",
        "driverClassName": "org.gjt.mm.mysql.Driver"
   },
  {
        "id": "ds1",
        "url": "jdbc:mysql://localhost:3306/test2",
        "user": "root",
        "password": "123456",
        "driverClassName": "org.gjt.mm.mysql.Driver"
   }
]
~~~

~~~xml
<bean class="jef.database.datasource.URLJsonDataSourceLookup"
 	p:datasourceKeyFieldName="id 
 	p:urlFieldName="url"
 	p:userFieldName="user"
 	p:passwordFieldName="password"
 	p:driverFieldName="driverClassName"
 	p:location="http://192.168.0.1/getdb">
 	 <property name="passwordDecryptor">
 		  <!-- 自定义的数据库口令解密器 -->
		   <bean class="org.googlecode.jef.spring.MyPasswordDecryptor" />
	  </property>
 </bean>	
~~~

使用上述配置，即可在需要数据库连接信息时，通过网络调用去获取数据库连接配置。

#### * DbDataSourceLookup

到数据库里去取数据源的配置信息。以此进行数据源的查找。

如果利用一个配置库来维护其他各种数据库的连接信息，那么系统会到这个数据库中去寻找数据源。数据库中配置数据源的表和其中的列名也可以配置。参见下面的示例。

~~~xml
<bean class="jef.database.datasource.DbDataSourceLookup"
 	p:configDataSource-ref="dataSource" 
 	p:configDbTable="DATASOURCE_CONFIG"
 	p:whereCondition="enable='1'"
 	p:columnOfId="DATABASE_NAME"
 	p:columnOfUrl="JDBC_URL"
 	p:columnOfUser="DB_USER"
 	p:columnOfPassword="DB_PASSWORD"
 	p:columnOfDriver=""
 	p:datasourceIdOfconfigDB="" 
 	p:defaultDsName="" >
 	 <property name="passwordDecryptor">
 		  <!-- 自定义的数据库口令解密器 -->
		   <bean class="org.googlecode.jef.spring.MyPasswordDecryptor" />
	  </property>
 </bean>	
~~~

 使用上述配置，即可在需要数据库连接信息时，通过数据库查找去获取数据库连接配置。

####* JndiDatasourceLookup

  到JNDI上下文去找寻数据源配置。参见下面的示例。

~~~xml
<bean class="jef.database.datasource.JndiDatasourceLookup"></bean>	
~~~

使用上述配置，即可在需要数据库连接信息时，通过JNDI查找去获取数据库连接配置。

####* MapDataSourceLookup

 从一个固定的Map对象中获取已经配置的数据源信息。在我们的一些示例代码中，有些直接就用Map来传入数据源配置。

~~~java
// 准备多个数据源
Map<String, DataSource> datasources = new HashMap<String, DataSource>();
datasources.put("datasource1", new SimpleDataSource("jdbc:derby:./db;create=true", null, null));
datasources.put("datasource2", new SimpleDataSource("jdbc:derby:./db2;create=true", null, null));
datasources.put("datasource3", new SimpleDataSource("jdbc:derby:./db3;create=true", null, null));
MapDataSourceLookup lookup = new MapDataSourceLookup(datasources);
lookup.setDefaultKey("datasource1");// 指定datasource1是默认的操作数据源
// 构造一个带数据路由功能的DbClient
db = new DbClient(new RoutingDataSource(lookup));
~~~

在Spring配置中也可以用Map来传入多个数据源。

####* PropertiesDataSourceLookup

  从一个classpath下的Properties文件中获取数据源配置信息。参见下面的示例。

[示例](undefined)[[季怡1\]](#_msocom_1) 

####* SpringBeansDataSourceLookup

  ​从Spring上下文中获取所有数据源信息。配置如下。

~~~xml
<bean class="jef.database.datasource.SpringBeansDataSourceLookup" />
~~~

上面提到的是EF提供的几种默认的DataSourceLookup，开发者也可以编写自己的DataSourceLookup。 

[示例](undefined)[[季怡2\]](#_msocom_2) 

 ### 11.2.3.  JPA事务配置

大家都知道，Spring有七种事务传播级别。因为标准JPA只能支持其中六种，因此EF-ORM提供了相关的JPA方言以支持第七种。其中nested方式需要JDBC驱动支持SavePoint.

~~~xml
	<bean id="transactionManager" class="org.springframework.orm.jpa.JpaTransactionManager">
		<property name="entityManagerFactory" ref="entityManagerFactory" />
		<property name="jpaDialect">
			<bean class="org.easyframe.enterprise.spring.JefJpaDialect" />
		</property>
	</bean>

	<tx:annotation-driven transaction-manager="transactionManager" proxy-target-class="true" />
	<aop:aspectj-autoproxy />
~~~

                    					代码 11-3 Spring基于注解的声明式事务配置方法

Spring的事务配置有好多种方法，上面这种是纯注解的声明式事务，另一种流行的AOP拦截器配置方法如下

~~~xml
	<bean id="transactionManager" class="org.springframework.orm.jpa.JpaTransactionManager">
		<property name="entityManagerFactory" ref="entityManagerFactory" />
		<property name="jpaDialect">
			<bean class="org.easyframe.enterprise.spring.JefJpaDialect" />
		</property>
	</bean>
	<tx:advice id="tx-advice-default transaction-manager="transactionManager">
		<tx:attributes>
			<tx:method name="*" propagation="REQUIRED" />
		</tx:attributes>
	</tx:advice>
	<tx:advice id="tx-always-new" transaction-manager="transactionManager">
		<tx:attributes>
			<tx:method name="*" propagation="REQUIRES_NEW" />
		</tx:attributes>
	</tx:advice>
    <aop:config>
        <aop:pointcut id="interceptorPointCuts"
            expression="execution(* com.company.product.dao.*.*(..))" />
        <aop:advisor advice-ref="tx-advice-defaul"
            pointcut-ref="interceptorPointCuts" />        
    </aop:config>
~~~

​					代码 11-4  Spring基于AOP拦截器的声明式事务配置方法

Spring事务配置方法还有很多种，但不管哪种配置方法，和ORM框架相关的就只有**“transactionManager”**对象。其他配置都只和Spring自身的事务实现机制有关。

上面的TransactionManager的配置方法和标准的JPA事务管理器配置方法区别之处在于，指定了一个jpaDialect对象，这是因为标准JPA实现因为接口和方法功能较弱，不足以实现Spring事务控制的所有选项。因此Spring提供了一种手段，由ORM提供一个事务控制的方言，Spring根据方言可以精确控制事务。JefJpaDialect的增加，使得EF-ORM能够支持Spring的事务管理的以下特性。（这些特性是标准JPA接口无法支持的)

| Spring配置                 | **在Spring中的作用**                          | **效果**                                   |
| ------------------------ | ---------------------------------------- | ---------------------------------------- |
| **Propagation=“nested”** | Spring的7种事务传播级别之一,NESTED方法是启动一个和父事务相依的子事务，因为不是EJB标准的，因此JPA不支持。 | JPA接口中无SavePoint操作，因此无法支持NESTED传播行为，EF-ORM在JpaDIalect中支持了SavePoint，因此可以使用NESTED传播行为。  再加上JPA本身支持的其他6种传播行为，EF-ORM可以支持全部7种传播行为。 |
| **isolation**            | 定义事务的四种隔离级别。                             | JPA接口不提供对数据库事务隔离级别的动态调整。也就无法支持Spring的事务隔离级别。但EF-ORM可以支持。 |
| **read-only="true"**     | 指定事务为只读。该属性提示ORM框架和JDBC驱动进行优化，比如Hibernate下只读事务可以省去flush缓存操作。Oracle服务器原生支持readonly级别，可以不产生回滚段，不记录重做日志，甚至可以提供可重复读等特性。 | 在只读模式下，EF-ORM将对JDBC Connection进行readOnly进行设置，从而触发数据库和驱动的只读优化。当然并不是所有的数据库都支持只读优化。 |
| **timeout**              | 事务超时时间，事务一旦超时，会被标记为rollbackOnly，抛出异常并终止处理。 | JPA原生接口不提供事务超时控制。EF-ORM可以通过方言支持。         |

### 11.2.4.  编写DAO

通过上面两节，我们在Spring中提供了EntityFactoryManager和事务管理。接下来就是编写自己的Dao对象了。EF-ORM提供了一个泛型DAO实现。

####*  继承GenericDaoSupport

   EF-ORM提供了一个泛型的DAO实现。

* 接口类为org.easyframe.enterprise.spring.GenericDao\<T>


* 实现类为org.easyframe.enterprise.spring.GenericDaoSupport\<T>

开发者的DAO可以直接继承GenericDaoSupport类。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\StudentDao.java

~~~java
/**
 * 这个类实现了GenericDao<T>接口
 */
public class StudentDao extends GenericDaoSupport<Student>{

}
~~~

继承GenericDaoSupport后，该DAO就已经有了各种基本的持久化操作方法。

  ![11.2.4](E:\User\ef-orm\manual\Chapter\images\11.2.4.png)

如果需要自行添加方法，可以这样做

接口orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\StudentDao.java

~~~java
public interface StudentDao extends GenericDao<Student>{
	/**
	 * 批量升级学生
	 * @param ids
	 */
	public void gradeUp(Collection<Integer> ids);
}
~~~

实现类orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\StudentDaoImpl.java

~~~java
public class StudentDaoImpl extends GenericDaoSupport<Student> implements StudentDao{
	public void gradeUp(Collection<Integer> ids) {
		Student st=new Student();
		st.getQuery().addCondition(Student.Field.id, Operator.IN, ids);
		st.prepareUpdate(Student.Field.grade, new JpqlExpression("grade+1"));
		try {
			getSession().update(st);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}
}
~~~

对于自行实现的方法，可以使用继承自BaseDao类的方法获得Session对象。

一般来说，GenericDao中已经包含了绝大多数日常所需的数据库操作。如果没有特殊操作，我们甚至不需要为某个Bean创建DAO，而是使用后文的CommonDao即可。    

####* 继承BaseDao

GenericDao继承了BaseDao。开发者也可以直接继承org.easyframe.enterprise.spring.BaseDao类来编写DAO。在DAO中，开发者可以使用标准的 JPA 方法来实现逻辑，也可以使用EF-ORM的Session对象来实现逻辑。

 orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\MyDao.java

~~~java
public class MyDao extends BaseDao{
	
	/**
	 * 使用标准JPA的方法来实现DAO
	 */
	public Student loadStudent(int id){
		return getEntityManager().find(Student.class, id);
	}
	
	/**
	 * 使用EF-ORM的方法来实现DAO
	 * @param name
	 * @return
	 */
	public List<Student> findStudentByName(String name){
		Student st=new Student();
		st.getQuery().addCondition(QB.matchAny(Student.Field.name, name));
		try {
			return getSession().select(st);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}
}
~~~

BaseDao基类中提供了以下方法

| 方法                 | 作用                                  | 说明                                       |
| ------------------ | ----------------------------------- | ---------------------------------------- |
| getEntityManager() | 得到javax.persistence.EntityManager对象 | EntityManager是JPA中操作持久化对象的主要方式。          |
| getSession()       | 得到jef.database.Session对象            | Session对象是EF-ORM操作数据库的基本类。前面所有例子中的DbClient和Transaction都是其子类。 |
| getDbClient()      | 得到jef.database.DbClient对象           | 不建议使用。DbClient对象是无事务状态的Session。对其进行的任何操作都是直接提交到数据库的，不在Spring事务控制之下。 |

要注意的是 getEntityManager()中得到的JPA对象javax.persistence.EntityManager中，EF-ORM并没有实现其全部方法。其中CriteriaBuilderCriteriaQuery相关的功能都会抛出UnSupportedOperationException.这部分功能请使用EF-ORM自己的Criteria API。

### 11.2.5.  常用API: CommonDao

EF-ORM提供了CommonDao是基于Spring的Dao bean的通用接口，提供了以下方法(此处仅列举，详细请参阅API-DOC)

由于EF-ORM中的Entity可以携带Query对象，表示复杂的where条件和update条件，因此很多看似简单的接口，实际上能传入相当复杂的SQL查询对象，请不要低估其作用。

| **方法**                                   | **备注**                                   |
| ---------------------------------------- | ---------------------------------------- |
| **基础的查询、插入、更新、删除方法**                     |                                          |
| T insert(T entity);                      | 相当于session.insert                        |
| void remove(Object entity);              | 相当于session.delete()  支持单表CriteriaAPI     |
| int update(T entity);                    | 相当于session.update()  支持单表CriteriaAPI     |
| List\<T> find(T data);                   | 相当于session.select()  支持单表CriteriaAPI     |
| T load(T data);                          | 相当于session.load()  支持单表CriteriaAPI       |
| \<T> ResultIterator\<T> iterate(T obj);  | 相当于session.iteratedSelect()  支持单表CriteriaAPI |
| **byProperty/  byKey系列**                 |                                          |
| void removeByProperty(ITableMetadata meta, String propertyName,  List<?> values); | 指定一个字段为条件，批量删除。                          |
| int removeByKey(ITableMetadata meta,String field,Serializable  key); | 指定一个字段为条件，单次删除                           |
| int removeByKey(Class\<T> meta,String field,Serializable  key); | 指定一个字段为条件，单次删除                           |
| T loadByKey(Class\<T> meta,String field,Serializable key); | 指定一个字段为条件，加载单条                           |
| T loadByKey(ITableMetadata meta,String field,Serializable id); | 指定一个字段为条件，加载单条                           |
| List<?> findByKey(ITableMetadata meta, String  propertyName, Object value); | 指定一个字段为条件，加载多条                           |
| **ByExample系列**                          |                                          |
| List\<T> findByExample(T entity,String... properties); | 传入模板bean，可指定字段名，这些字段值作为where条件           |
| int removeByExample(T entity,String... properties); | 传入模板bean，可指定字段名，这些字段值作为where条件           |
| **By  PrimaryKey **                      |                                          |
| T loadByPrimaryKey(ITableMetadata meta, Object id); | 按主键加载                                    |
| T loadByPrimaryKey(Class\<T> entityClass, Object  primaryKey); | 按主键加载                                    |
| **保存方法**                                 |                                          |
| void persist(Object entity);             | 对象存在时更新，不存在时插入                           |
| T merge(T entity);                       | 对象存在时更新，不存在时插入                           |
| **Update方法**                             |                                          |
| int updateByProperty(T entity,String... property); | 可传入多个字段名，这些字段的值作为where条件                 |
| int update(T entity,Map\<String,Object> setValues,String...  property); | 可传入多个字段名，这些字段的值作为where条件。可在map中指定要更新的值。  |
| **Remove方法**                             |                                          |
| void removeAll(ITableMetadata meta);     | 删除全表记录                                   |
| **批量操作系列**                               |                                          |
| int batchUpdate(List\<T> entities);      | 批量（按主键）更新                                |
| int batchUpdate(List\<T> entities,Boolean doGroup); | 批量（按主键）更新                                |
| int batchRemove(List\<T> entities);      | 批量删除                                     |
| int batchRemove(List\<T> entities,Boolean doGroup); | 批量删除                                     |
| int batchInsert(List\<T> entities);      | 批量插入                                     |
| int batchInsert(List\<T> entities,Boolean doGroup); | 批量插入                                     |
| **命名查询NamedQuery**                       |                                          |
| List\<T> findByNq(String nqName, Class\<T>  type,Map\<String, Object> params); | 传入查询名称、返回类型、参数                           |
| List\<T> findByNq(String nqName, ITableMetadata  meta,Map\<String, Object> params); | 传入查询名称、返回类型、参数                           |
| int executeNq(String nqName,Map\<String,Object> params); | 执行命名查询操作，传入查询名称，参数。                      |
| **E-SQL操作系列**                            |                                          |
| List\<T> findByQuery(String sql,Class\<T> retutnType,  Map\<String, Object> params); | 传入E-SQL语句查询结果                            |
| List\<T> findByQuery(String sql,ITableMetadata retutnType,  Map\<String, Object> params); | 传入E-SQL语句，查询结果                           |
| int executeQuery(String sql,Map\<String,Object> param); | 传入E-SQL语句，执行                             |
| \<T> ResultIterator\<T> iterateByQuery(String  sql,Class\<T> returnType,Map\<String,Object> params); | 传入E-SQL语句。查询并以遍历器返回。                     |
| \<T> ResultIterator\<T> iterateByQuery(String sql,  ITableMetadata returnType, Map\<String, Object> params); | 传入E-SQL语句。查询并以遍历器返回。                     |
| **分页查询方法**                               |                                          |
| Page\<T> findAndPage(T data,int start,int limit); | 传入单表Criteria对象。分页查询                      |
| Page\<T> findAndPageByNq(String nqName, Class\<T>  type,Map\<String, Object> params, int start,int limit); | 传入命名查询名称，分页查询                            |
| Page\<T> findAndPageByNq(String nqName, ITableMetadata  meta,Map\<String, Object> params, int start,int limit); | 传入命名查询名称，分页查询                            |
| Page\<T> findAndPageByQuery(String sql,Class\<T>  retutnType, Map\<String, Object> params,int start,int limit); | 传入E-SQL语句，分页查询                           |
| Page\<T> findAndPageByQuery(String sql,ITableMetadata  retutnType, Map\<String, Object> params,int start,int limit); | 传入E-SQL语句，分页查询                           |
| **其他**                                   |                                          |
| Session getSession();                    | 得到的EF-ROM Session对象                      |
| DbClient getNoTransactionSession();      | 得到当前无事务的操作Session                        |

从上面的API可以看出，配置命名查询配置，仅凭CommonDao已经可以完成大部分的数据库DAO操作。

>​	**DAO轻量化实践**
>
>​	*从个人开发实践看，随着ORM**框架封装性的提升，DAO**层越来越趋向轻量化。这里是个人的一点看法和建议。*
>
>​	*轻量化表现在*
>
>​	*1.在无需多种实现的情况下，DAO**无须设计接口类*
>
>​	*2. 无需为每个Entity创建DAO，大多数数据库操作在Service中直接获取CommonDao进行操作即可。*
>
>​	*为什么说，为每个Entity创建一个DAO这种做法过时了呢？*
>
>​	*实践表明，局限单表的操作都可以继承GenericDao自动获得，子类中几乎无需任何编码，甚至泛型都是不需要的，ORM能够根据传入的对象类型绑定到对应的数据表上。所以泛型的DAO仅仅是起到了增加了一些Bean类型校验的作用。为此付出的代价是，开发时还要控制不同DAO的依赖注入、还要控制bean使用对号入座的DAO**进行操作，这些都是多余的工作。*
>
>​	*此外，如果我们为每个Entity创建DAO，那么多表关联的操作应该放在哪个DAO里呢？ 事实上无论放在哪一个DAO中都不是那么合理的。*
>
>​	*所以，结合业务实践，个人建议在使用EF-ORM的时候，可以省略掉大部分Entity的对应的DAO。一些复杂数据库操作（基本上是涉及多表的），可以自行继承CommonDaoImpl，放在公共的DAO中。一般中小规模的应用，最后的DAO个数不会超过10个*

### 11.2.6.  POJO操作支持

CommonDao中的方法还有一个特点，那就是可以支持POJO Bean。我们在最初的1.1.3示例中可以发现，无需继承jef.database.DataObject，我们可以直接使用单纯的POJO对象创建表、删除表、执行CRUD操作。

POJO支持是为了进一步简化ORM使用而在CommonDao中进行的特殊处理。因此CommonDao中所有的泛型T，都无需继承jef.database.DataObject。

 当我们定义POJO时，依然可以使用 @Id @Column @Table等基本的JPA注解。不过由于POJO Bean中不包含Query对象，因此在使用上基本只能按主键实现CRUD操作。

CommonDao中设计了xxxxByProperty、xxxxByKey等系列的方法，也正是考虑到POJO对象中，无法准备的记录用户设置过值的字段，因此提供一个手工指定的补救办法。使用这两个系列的方法，可以更方便的操作POJO对象。例如

我们定义一个POJO Entity

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\entity\Foo.java

~~~java
@Table(name="MY_FOO")
public class Foo {
	@Id
	@GeneratedValue
	private int id;
	private String name;
	private int age;
//Getter Setter省略
}
~~~

便可以对其进行各种操作了

~~~java
	@Test
	public void test1() throws SQLException{
		commonDao.getNoTransactionSession().dropTable(Foo.class);
		commonDao.getNoTransactionSession().createTable(Foo.class);
		{
			Foo foo=new Foo();
			foo.setName("Hello!");
			commonDao.insert(foo);	
		}
		{
			Foo foo=new Foo();
			foo.setAge(3);
			foo.setName("飞");
			//update MY_FOO set age=3 where name='Hello!'
			commonDao.updateByProperty(foo, "name");
		}
		{
			Foo foo=commonDao.loadByPrimaryKey(Foo.class, 1);
			System.out.println(foo.getName());
		}
		{
			//根据ID删除
			commonDao.removeByKey(Foo.class, "id", 1);
		}
	}
~~~

上面演示了对Foo对象进行建表、删表、增删改查操作。

最后，EF-ORM可以在一定程度上识别某H框架的配置文件，当做POJO Bean的注解来使用。这种做法可以在EF-ORM中直接使用某H框架的Bean定义。

比如我们创建不带任何注解的POJO类

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\entity\PojoEntity.java

~~~java
public class PojoEntity {
	private String name;
	private Integer id;
	private String comments;
     //Getter Setter
}
~~~

然后我们配置一个XML文件去定义这个类的元数据

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\entity\hbm\PojoEntity.hbm.xml

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
<hibernate-mapping>
	<class name="org.easyframe.tutorial.lessonb.entity.PojoEntity" table="Jef_pojo_table">
		<id name="id">
			<column name="id" length="5" />
			<generator strategy="IDENTITY" />
		</id>
		<property name="name">
			<column name="name" />
		</property>
		<property name="comments">
			<column name="comments" />
		</property>
	</class>
</hibernate-mapping>
~~~

只要通过指定class和某xml文件存在关联，EF-ORM就能够识别某H框架中的主要标签来读取元数据配置。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\Case1.java

~~~java
@Test
public void test2() throws SQLException{
	//读取指定路径下的某H框架配置文件。 %s表示类的SimpleName。%c表示类的全名。
	ORMConfig.getInstance().setMetadataResourcePattern("hbm/%s.hbm.xml");

	commonDao.getNoTransactionSession().dropTable(PojoEntity.class);
	commonDao.getNoTransactionSession().createTable(PojoEntity.class);
		
	PojoEntity p=new PojoEntity();
	p.setName("fsdfsfs");
		
	commonDao.insert(p);
	System.out.println(p.getId());
	commonDao.insert(p);
	System.out.println(p.getId());
	commonDao.insert(p);
	System.out.println(p.getId());
		
		
	PojoEntity pojo=commonDao.load(p);
	System.out.println(pojo);
		
	pojo.setName("35677");
	commonDao.update(pojo);
		
	System.out.println("===========================");
		
	PojoEntity cond=new PojoEntity();
	cond.setId(12);
	System.out.println(commonDao.find(cond));
	commonDao.remove(cond);
}
~~~

有一项名为MetadataResourcePattern的全局参数配置，用于指定Entity类和某个XML文件之间的关联关系。

例如，所有的XML文件位于class path下的 /hbm目录中，其名称和类的SimpleName一致。此时可以在jef.properties中配置——

~~~properties
metadata.resource.pattern=/hbm/%s.hbm.xml
~~~

其中 %s表示类的SimpleName；%c表示类的全名；

还可以用 %\*表示匹配任意字符，一旦匹配为*，那么EF-ORM会查找所有满足条件的XML，然后根据XML中配置的class属性反向匹配到Entity上。

目前此功能仅支持某H框架中一些基本的单表字段描述，级联等描述目前还不支持。

## 11.3.  多数据源下的事务控制

在数据分库之后。下一个问题就接踵而至，这就是分布式事务的一致性问题。

如果我们依旧使用Spring的JPA事务控制器，正常情况下，如果所有数据库都成功提交，那么事务可以保持一致，如下图所示------

 ![11.3.-1](E:\User\ef-orm\manual\Chapter\images\11.3.-1.png)

### 11.3.1.  JPA事务（多重）

如果考虑到提交可能失败的场景，我们如果继续使用JPA事务管理器，我们将需要承担一定的风险。

**中断提交**

当遭遇提交失败时，有两种行为策略。默认的是“中断提交”

因此，在默认情况下，当一个Spring事务结束时，EF会顺序提交A、B两个数据库的修改。如果发生提交失败，则中断提交任务。

 ![11.3.1.-1](E:\User\ef-orm\manual\Chapter\images\11.3.1.-1.png)

 

从上例看，也就是说，如果先提交 A库失败，那么A、B库都不提交。如果先提交A库成功，B库提交失败，那么A库的修改将会生效，而B库的修改不生效。

**继续提交**

此外，这一策略还可以变更。在jef.properties中配置：

~~~properties
db.jpa.continue.commit.if.error =true
~~~

开启上述配置后，那么在一个库提交失败后，整个提交过程将持续进行下去，直到所有能提交的变更都写入数据库位置。这种策略下，哪个连接先提交哪个后提交将不再产生影响。如下图所示  

 ![11.3.1.-2](E:\User\ef-orm\manual\Chapter\images\11.3.1.-2.png)

这种方式下，简单来说，如果我们的事务中用到了A、B两个数据库，事务提交时A、B数据库的修改单独提交，互不影响。

无论使用上述哪种策略，都有可能会出现某些数据库提交成功、某些数据库提交失败的可能。因此，在没有跨库事务一致性要求的场合，我们依然可以用JPATransactionManager来管理事务，虽然这可能会造成上述两种场景的数据不一致，但如果您的系统业务上本身就没有这种严格的一致性要求时，JPA事务不失为是最简单的使用方法。

在多库上使用JPA事务管理器时，每个数据库上的操作分别位于独立的事务中，相当于将Spring的事务划分为了多个独立的小型JPA事务。我们姑且用 “多重JPA事务”来称呼。

​	如果出现了某些数据库被提交，某些数据库出错或未提交。此时框架将会抛出*jef.database.innerpool.InconsistentCommitException类。*该异常类标识着多个数据库的提交状态出现了不一致。该异常类中，可以获得哪些数据源提交成功，哪位未提交成功的信息。供开发者自行处理。

### 11.3.2.  JTA事务支持

上面的问题是不是无法避免的呢？不是， SpringFramework还支持JTA事务。使用J2EE的JTA规范，我们可以让EF-ORM在多数据库下支持分布式事务。

JTA是JavaEE技术规范之一，JTA允许应用程序执行分布式事务处理——在两个或多个网络计算机资源上访问并且更新数据。EF-ORM可以借助一些数据库JDBC驱动本身的XA功能，或者第三方的开源JTA框架实现分布式事务。

使用JTA事务后，刚才的流程即可变为下图所示，因此任何一个数据库提交错误情况下，都能确保数据库数据一致性。

 ![11.3.2](E:\User\ef-orm\manual\Chapter\images\11.3.2.png)

 目前ef-orm推荐使用atomikos作为JTA的事务支持框架。

关于JTA的介绍，可参见http://www.ibm.com/developerworks/cn/java/j-lo-jta/

关于atomikos的介绍，可参见[http://www.atomikos.com/](http://www.atomikos.com/)

下面我们举例，用Spring + atomikos + EF-ORM实现分布式事务。
首先，我们在pom.xml中，引入atomikos的包以及jta的API包。

~~~xml
	<dependency>
		<groupId>com.atomikos</groupId>
		<artifactId>transactions-jdbc</artifactId>
		<version>3.9.3</version>
	</dependency>
	<dependency>
		<groupId>com.atomikos</groupId>
		<artifactId>transactions-jta</artifactId>
		<version>3.9.3</version>
	</dependency>
	<dependency>
		<groupId>javax.transaction</groupId>
		<artifactId>jta</artifactId>
		<version>1.1</version>
	</dependency>
~~~

由于使用了atomikos，在Spring bean配置中，需要配置XA的数据源

~~~xml
<bean id="ds1" class="com.atomikos.jdbc.AtomikosDataSourceBean"
		init-method="init" destroy-method="close">
		<property name="uniqueResourceName"><value>mysql/ds1</value>	</property>
		<property name="xaDataSourceClassName">
			<value>com.mysql.jdbc.jdbc2.optional.MysqlXADataSource</value>
		</property>
		<property name="xaProperties">
			<props>
				<prop key="URL">jdbc:mysql://localhost:3307/test</prop>
				<prop key="user">root</prop>
				<prop key="password">admin</prop>
			</props>
		</property>
		<property name="poolSize"><value>3</value></property>
		<property name="maxPoolSize"><value>30</value>	</property>
	</bean>
	<bean id="ds2" class="com.atomikos.jdbc.AtomikosDataSourceBean"
		init-method="init" destroy-method="close">
		<property name="uniqueResourceName"><value>oracle/ds2</value></property>
		<property name="xaDataSourceClassName">
			<value>oracle.jdbc.xa.client.OracleXADataSource</value>
		</property>
		<property name="xaProperties">
			<props>
				<prop key="URL">jdbc:oracle:thin:@pc-jiyi:1521:orcl</prop>
				<prop key="user">pomelo</prop>
				<prop key="password">pomelo</prop>
			</props>
		</property>
		<property name="poolSize"><value>3</value></property>
		<property name="maxPoolSize"><value>30</value>	</property>
	</bean>
~~~

上例配置了两个JTA的数据源，一个是Oracle数据库,的一个是MySQL数据库。然后配置EF-ORM的SessionFactory

~~~xml
<bean id="sessionFactory" class="org.easyframe.enterprise.spring.SessionFactoryBean" destroy-method="close">
		<property name="transactionMode" value="jta"></property>
		<property name="dataSources">
			<map>
				<entry key="ds1" value-ref="ds1" />
				<entry key="ds2" value-ref="ds2" />
			</map>
		</property>
		<property name="packagesToScan">
			<list>
				<value>com.github.xuse.easyframe.test </value>
			</list>
		</property>
	</bean>
~~~

配置SessionFactoryBean，和前面的区别在于要将tranactionMode配置为”jta”。

然后配置Spring的声明式事务管理。

~~~xml
<!--事务管理器，需要使用JtaTransactionManager -->
<bean id="transactionManager"
		class="org.springframework.transaction.jta.JtaTransactionManager">
		<property name="userTransaction">
			<bean class="com.atomikos.icatch.jta.UserTransactionImp"
				p:transactionTimeout="300" />
		</property>
		<property name="transactionManager">
			<bean class="com.atomikos.icatch.jta.UserTransactionManager"
				init-method="init" destroy-method="close" p:forceShutdown="true" />
		</property>
</bean>
<!-- 事务AOP切面，和标准的Spring配置方法没有区别 -->
<tx:advice id="advice" transaction-manager="transactionManager">
	<tx:attributes>
		<tx:method name="save*" propagation="REQUIRED" />
		<tx:method name="delete*" propagation="REQUIRED" />
		<tx:method name="*" read-only="false" />
	</tx:attributes>
</tx:advice>
<aop:config>
	<aop:pointcut id="point"
		expression=" execution(* com.github.easyframe.testp.jta.dao.Biz*.*(..))" />
	<aop:advisor advice-ref="advice" pointcut-ref="point" />
</aop:config>
~~~

上述是Spring的事务策略和AOP配置。其中atomikos的连接池，事务超时等控制参数也可以配置，详情可参阅atomikos的官方文档。

使用上述配置后，EF-ORM和Spring基本放弃了对事务控制，单个线程中的所有操作都在一个事务(UserTransaction)中。直到事务结束，连接关闭（被放回JTA连接池）时，所有数据才被提交。凡是位于上述切面中的save*或者delete*方法中，如果操作了多个数据库的数据，框架都会保证其数据一致性。

>**在JTA模式下,EF-ORM作了哪些机制来适应JTA**
>
>​	*在启用JTA后，EF-ORM会禁用一些内部特性来满足JTA的要求，比如禁用内部连接池，禁用Postgres事务保持功能等。也不会在Connection上执行commit 、rollback、setReadOnly等操作。*
>
>​	*此外，启用了JTA后，DDL语句将不能和业务操作在同一个线程中运行，因此凡是涉及到建表、删表、创建Sequence等DDL操作时，EF-ORM都会创建一个新的线程，在独立连接上操作数据库。这些都是JTA事务模式下的特殊处理。*

## 11.4.  共享其他框架的事务

如果您将EF-ORM和其他ORM框架混合使用，那么就会碰到共享事务的问题。我们一般会希望在一个服务(Service)方法中，无论使用哪个框架来操作数据库，这些操作都位于一个事务中。

为了适应这种场景，EF-ORM中存在一个共享事务的模式，一旦启用后，EF-ORM将会放弃自己的事务控制和连接管理，而是到Spring的上下文中去查找其他框架所使用的连接对象，然后在该连接上进行数据库操作，从而保证多个框架操作同一个事务。

目前EF-ORM可以和以下三种框架共享事务。

* Hibernate 
* MyBatis
* Spring JdbcTemplate。

下面具体说明具体的配置方法。

### 11.4.1.  Hibernate

下面例子中配置了事务共享

~~~xml
<!— 配置数据源 -->
<bean id="dataSource"  class="jef.database.test.jdbc.DebugDataSource">
</bean>
<!— 配置Hibenrate 3 Session Factory-->
<bean id="sessionFactory"
		class="org.springframework.orm.hibernate3.LocalSessionFactoryBean">
	<property name="configLocation" value="classpath:hibernate-perftest.cfg.xml" />
	<property name="dataSource" ref="dataSource" />
	<property name="lobHandler" ref="lobHandler" />
</bean>
<bean id="lobHandler" class="org.springframework.jdbc.support.lob.DefaultLobHandler"
		lazy-init="true" />
<!-- 事务管理器配置 -->
<bean id="hibernateTxManager"
	class="org.springframework.orm.hibernate3.HibernateTransactionManager">
	<property name="sessionFactory" ref="sessionFactory" />
	<property name="dataSource" ref="dataSource" />
</bean>

<!-- JDBC TEMPLATE -->
<bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate">
	<property name="dataSource" ref="dataSource" />
</bean>

<!-- EF配置 -->
<bean id="entityManagerFactory" class="org.easyframe.enterprise.spring.SessionFactoryBean" destroy-method="close">
	<property name="dataSource" ref="dataSource" />
	<property name="transactionMode" value="JDBC" />
</bean>

<!— 此处仅介绍事务管理器配置，其他的事务策略、事务拦截器、事务切面等略，请自行百度 -->
~~~

上述配置的要点是

1. 使用Hibernate的事务管理器。注意需要注入DataSource对象。
2. 必须用同一个DataSource对象初始化JdbcTemplate， EF-ORM SessionFactory。
3. EF-ORM的**transactionMode**参数必须设置为jdbc 。

### 11.4.2.  MyBatis / JdbcTemplate

~~~xml
<bean id="dataSource"  class="org.springframework.jdbc.datasource.DriverManagerDataSource">
<!— 此处定义数据源 -->
</bean>
<!— Spring JdbcTempate配置 -->
<bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate">
	<property name="dataSource" ref="dataSource" />
</bean>
<!— MyBatis配置，其他略 -->
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
     <property name="dataSource" ref="dataSource" /> 
 </bean>

<!-- 事务管理器配置 -->
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    <property name="dataSource" ref="dataSource" />
 </bean>
	 
 <!-- EF配置 -->
<bean id="entityManagerFactory" class="org.easyframe.enterprise.spring.SessionFactoryBean" destroy-method="close">
	<property name="dataSource" ref="dataSource" />
	<property name="transactionMode" value="jdbc" />
</bean>

<!— 此处仅介绍事务管理器配置，其他的事务策略、事务拦截器、事务切面等略，请自行百度 -->
~~~

上述配置的要点是

1. 使用**org.springframework.jdbc.datasource.DataSourceTransactionManager**事务管理器。
2. 必须用同一个DataSource对象初始化JdbcTemplate，MyBatis SessionFactory，EF-ORM SessionFactory。
3. EF-ORM的**transactionMode**参数必须设置为jdbc。

按上述要点配置后，即可确保三个框架的操作处于同一个事务中。

>  **扩展阅读：共享事务的原理**
>
>  	*Spring在设计时，考虑到了JdbcTemplate和Hibenrate共享事务的问题，会将Hibernate事务所使用的连接暴露出来，用ThreadLocal保存在一个静态变量中，这就为共享事务提供了可能。*
>  	*对于Spring来说，在Hibernate中暴露出来的连接，和在使用MyBatis/JdbcTemplate时存放的事务连接是相同的，因此Hibernate和JdbcTemplate/MyBatis之间就可以共享事务。*
>
>  ​	*在启用JDBC的事务模式后，EF-ORM会禁用内部连接管理和事务管理。每次操作时，都去寻找Spring事务管理器所暴露出来的当前事务连接进行利用。相当与把自身的事务管理方式改得和 JdbcTemplate一样，因此也就能和上述两个框架一样，共享事务连接。*