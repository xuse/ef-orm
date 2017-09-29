GeeQuery使用手册——Chapter-17  性能调优指南

[TOC]

# Chapter-17  性能调优指南

## 17.1.  性能日志

要了解性能问题所在，首先要能看懂EF-ORM输出的性能日志。

一个查询语句输出的日志可能是这样的

~~~sql
select t.* from LEAF t | [hsqldb:TESTHSQLDB@1]
Result Count:5	 Time cost([ParseSQL]:0ms, [DbAccess]:1ms, [Populate]:0ms) max:0/fetch:0/timeout:60 |[hsqldb:TESTHSQLDB@1]
~~~

在上面这段日志中，第一行打印出了SQL语句，竖线后面的是此时的环境描述。这部分信息包括三部分。

~~~
[hsqldb:TESTHSQLDB@1]
数据库类型：数据库名@线程号
~~~

上面是当SQL语句在无事务情况下执行时的环境，当语句在事务下执行时：     

~~~
[Tx22953873@ORCL@1
事务编号@数据库名@线程
~~~

事务编号中的数字是一个随机编号，用于和日志上下文核对，可以跟踪事务的情况。

而性能相关的统计信息都在这一行中显示

~~~
Result Count:5	 Time cost([ParseSQL]:0ms, [DbAccess]:1ms, [Populate]:0ms) max:0/fetch:0/timeout:60
结果条数          耗时  （生成SQL语句耗时,执行SQL语句耗时，转换查询结果耗时）   性能参数信息
~~~

在上例中，查询出5条结果，耗时1ms，其中生成SQL语句和转换结果0ms，数据库查询1ms。

* ParseSQL:获取连接、生成SQL语句的时间。
* DbAccesss:数据库解析SQL，执行查询的时间。
* Populate: 将数据从ResultSet中逐条读出，形成Java对象的时间。


当然并非所有的数据库操作都有这三个时间记录。比如您自行编写的SQL语句（NativeQuery）中不会有ParseSQL的统计，非select语句不会有Populate的统计。

另外，上例中生成SQL语句和转换结果不可能真的不花费时间，因为统计是到毫秒的，因此500微秒以下的数值就被舍去了。

​最后输出的是查询执行时都是三个性能相关的参数。

Max: 返回结果最大数限制。0表示不限制

​Fetch:取ResultSet的fetch-size大小。该参数会严重影响大量数据返回时的性能。

Timeout: 查询超时时间。单位秒。

上面三个都是用户可以控制的性能相关参数，用来对照进行调优的。

所有查询类语句，都会输出结果条数（COUNT类语句直接输出COUNT结果）。而非查询语句则会显示影响的记录数。看懂上述日志，可以帮助用户统计一笔交易中，数据库操作的耗时情况，帮助用户分析和定位性能故障。

## 17.2.  级联性能

前面已经讲过，EF-ORM在支持级联操作的基础上，还保留了单表操作的方式。此外还能控制单个查询语句中需要查询的字段等。首先我们可以考虑在应用场景上避免不当的数据操作。

此外EF-ORM中有若干参数可以辅助级联性能的调节。有以下两个全局参数。

| db.use.outer.join       | 使用外连接加载多对一和一对一关系。这种情况下，只需要一次查询，就可以将对一关系查询出来。默认开启。如果关闭，那么一对一或多对一级联操作将会通过多次单表查询来实现。 |
| ----------------------- | ---------------------------------------- |
| **db.enable.lazy.load** | **延迟加载开关，即关系数据只有当被使用到的时候才会去查询。由于默认情况下対一关系是一次性直接查出的，所以实际上会被延迟加载的只有一对多和多对多  关系。但如果关闭了外连接加载，那么一对一和多对一关系也会被延迟加载。** |

在查询数据时，我们可以精确控制每个查询是否采用外连接加载，是否要加载X対一关系，是否要加载X对多关系。下面的例子演示了这种用法。

~~~java
Query<TestEntity> query = QB.create(TestEntity.class);
query.getResultTransformer().setLoadVsMany(false); //不加载X对多关系
query.getResultTransformer().setLoadVsOne(true);   //加载X対一关系
query.setCascadeViaOuterJoin(false);             //设置不使用外连接方式获取X対一关系
~~~

因此每个查询语句，都可以控制其级联加载的范围，级联加载的方式。

如果不希望级联操作，还可以这样

~~~java
Query<TestEntity> query = QB.create(TestEntity.class);
query.setCascade(false);		
~~~

这和下面的操作是等效的

~~~java
Query<TestEntity> query = QB.create(TestEntity.class);
query.getResultTransformer().setLoadVsMany(false);
query.getResultTransformer().setLoadVsOne(false);
~~~

两个参数的用法上，延迟加载的开启是较为推荐的。这能有效防止你使用级联操作获取过多的数据。大部分情况下，外连接开启也能有效减少数据库操作的次数，提高性能。同时外连接查询能降低对一级缓存的依赖，因为在一些快速查询中，维护缓存数据也有一定的耗时。如果您关闭了外连接查询，那么推荐您开启一级缓存。因为此时级联操作对一级缓存的依赖性大大增加了。

## 17.3.  一级缓存与二级缓存

EF-ORM设计了一级缓存。一级缓存是以每个事务Session为生命周期维护的缓存，这部分缓存会将您操作过的对象和查询过的数据缓存在内容中。（特别大的数据不会被缓存）一级缓存能有效的减少对相同对象的查询，尤其是在一对多的级联关系查询中。

一级缓存默认不开启，开启一级缓存的方法是在jef,properties中配置

~~~properties
cache.level.1=true
~~~

使用JMX可以在ORMConfigMBean中，通过设置CacheDebug属性为true，从而在日志中输出一级缓存的命中和更新信息，用于细节上的调试和分析。

以下情况下，我们建议开启一级缓存：

* 使用较多的级联操作。
* db.use.outer.join=false时


相反，如果使用级联操作较少，同时也开启了db.use.outer.join的场合下，我们建议关闭一级缓存。因为基于SQL操作业务逻辑中，维护一级缓存反而会增加额外的内存和性能开销。

​EF-ORM没有内置的二级缓存。你可以使用诸如EHCache的第三方缓存框架，并通过Spring AOP等手段集成，此处不再赘述。

## 17.4.  结果集加载调优

### 17.4.1.  Fetch-size

即等同于JDBC中的fetch-size，描述了遍历结果集（ResultSet）时每次从数据库拉取的记录条数。设置为0则使用JDBC驱动默认值。过大则占用过多内存，过小则数据库通信次数很多，populate过程耗时很大。

如果您返回5000条以上数据，建议加大fetch-size。

Fetch-size的全局设置：在jef.properties中

~~~properties
#将全局的fetch-size设置为希望的值
db.fetch.size=0
~~~

针对单个查询设置fetch-size：所有的ConditionQuery对象，包括Query、Join、UnionQuery、NativeQuery都提供了setFetchSize(int)方法。

~~~java
//设置NativeQuery的fetch-size
NativeQuery<Foo> nq=db.createNativeQuery(sql, Foo.class);
nq.setFetchSize(1000);

//设置Query对象的fetch-size
Query<Foo> q = QB.create(Foo.class)
q.setFetchSize(1000);
~~~

### 17.4.2.  max-results

这个参数可以控制一个查询返回的最大结果数。事实上一个限制了最大结果数的查询逻辑上不一定正确，但是这能有效预防超出设计者预期数据规模时引起的OutOfMemory或者其他问题，而后者往往会影响整个系统中的所有交易，甚至引起服务器的故障。

因此全局性的max-result设置往往作为一个数据规模的约束条件来使用，而针对单个查询的max-result设置则可以根据应用场景而灵活控制。

~~~properties
#将全局的max-results设置为希望的值，0表示不限制
db.max.results.limit=0
~~~

~~~java
//设置NativeQuery的max-result
NativeQuery<Foo> nq=db.createNativeQuery(sql, Foo.class);
nq.setMaxResults(200);

//设置Query对象的max-result
Query<Foo> q = QB.create(Foo.class)
q.setMaxResult(200);
~~~

### 17.4.3.  使用CachedRowSet

这个参数目前只支持全局设置。其作用是在查出结果后，先将ResultSet的所有数据放在JDBC的CachedRowSet中，释放连接（仅对非事务操作，因为事务操作下连接被事务专用，在提交/回滚前不会放回连接池），然后再转换为java对象，最后释放CachedRowSet。这种操作方式具有以下特点

* 它不能减少查询结果转换的总时间，因为原先转换结果该进行的操作一步也没有少。
* 在非事务下，连接能更快的被释放。供其他业务使用。
* 它会将从ResultSet中读取数据的时间计入DbAccess阶段，使得Populate阶段的时间仅剩下调用反射操作所耗的时间。此时用户可以更清楚的知道，转换结果操作的真实性能开销。也帮助用户了解在ResultSet上的IO通信是否值得增加fetch-size来优化。

调节是否开启此功能的 方法为，在jef.properties中

~~~properties
#开启结果集缓存
db.cache.resultset=true
~~~

## 17.5.  查询超时控制

查询超时控制可以让一个SQL操作在执行一段时间后，如果无返回则抛出异常。这虽然会造成当前业务的失败，但是可以帮助您从以下几个方面改善程序的性能：

* 避免让个别不佳的SQL语句或超出开发者规模预期的查询拖慢整个系统。
* 避免数据库崩溃
* 发现锁表现象。（个别查询是因为锁表而被卡住，不主动查询数据库往往发现不了）

控制超时时间的参数设置方法为，在jef.properties中

~~~properties
#设置查询操作超时时间（单位：秒）
db.select.timeout=0
#设置更新操作超时时间（单位：秒）
db.update.timeout=0
#设置删除操作超时时间（单位：秒）
db.delete.timeout=0
~~~

目前尚未提供针对单个查询设置timeout的方法，后续版本中会增加相关API。

## 17.6.  自增值获取性能问题

​很多时候插入不够快是因为Sequence自增值获取的性能开销造成，优化方法详见3.1.2.5。