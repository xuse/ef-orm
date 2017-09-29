GeeQuery使用手册——Chapter-10  分库分表与数据路由

[TOC]

# Chapter-10  分库分表与数据路由

## 10.1.  数据分片概述

### 10.1.1.  什么是数据分片

​ 一个生产系统总会经历一个业务量由小变大的过程，可扩展性成为了考量系统高可用性的一个重要衡量指标。在大数据时代，数据规模的支持成为衡量系统能力的重要指标。

在这个时代，几乎data sharding成为每个系统架构时需要考虑的重要问题。实时上，远在互联网和“大数据”概念时代之前，银行和电信业早已在处理数十亿数百亿级的数据，并且采用了传统的分区、分表、分库等手段，因此数据分片并不是什么新鲜概念。Oracle 9以前就提供分区表的功能，并且提供管理了TB以上单表的生产案例。在后来若干年中，支持数据分区已经成为各种商业数据库的标配特性。

数据库软件提供的数据分片方案，一般都是分区方案，根据分区条件，可以让数据存储在不同的表空间中。在DBA配置完分区规则后，整个分区的动作对应用程序是透明的。开发人员可以用普通的SQL语句来操作分区表，无须考虑分区的规则和数据位置。

商业数据库提供的原生数据分区方案，有不可替代的优越性——使用简单，性能较好，而且数据还会提供进一步的优化特性（如分区索引等）。最大的特点是，数据库原生的分区方案基本都能做到**对开发者透明**。

尽管数据库的供应商们已经为我们设计了众多的数据分片sharding方式；尽管在大数据时代各种分布式数据存储系统都提供了海量的存储规模。然而在大量的系统中，基于分库分表的RDBMS仍然占用重要的地位。在对实时性和一致性有高度要求的系统中，分库分表依然是处理海量结构化数据最重要的手段，并且为此也开发出了众多的中间框架。

为什么系统的开发者们不使用数据库软件本身提供分区功能，而要自行开发分库分表功能？一般有以下的理由——

*  能对数据库DBA的人力和技能要求。分区表后期的维护复杂性。
*  分成不同的表，有利于数据的归档、清除、备份等维护性行为。例如对交易记录和日志等，按历史表归档既简单又符合业务需求。


*  需要用多台数据库服务器的场合。RDBMS一般只提供分区功能，无法将分片到不同的实例上。


*  成本因素。不愿意花费购买商业数据库的分区特性或相关服务。


*  还是成本因素。使用了一些不支持分区功能的低成本数据库。


*  希望保留跨数据库的移植性。用户希望系统将来能移植到其他数据库软件上，无论这些软件是否支持分区功能。


*  习惯和守旧。沿用过去的做法，不愿意学习和接受数据库软件的新功能。


对于上面的几种因素，除了最后一种以外，我们都有在应用中自行实现数据分片的必要。分库分表依然是数据库分片中，成本最低，适应性最广，对开发者要求最低的方案。因此这种做法到今天为止也有着广泛的市场。但无论任何用户自行开发的或是框架提供的分表方案，都对操作有着诸多的限制，（例如不支持Join等），因此不能完全做到对开发者透明。

### 10.1.2.  EF-ORM支持的分片功能

EF-ORM的分库分表功能，能够通过配置数据分片规则，在操作时自动定位到数据所在的库和表上去。

EF-ORM支持数据的水平拆分和垂直拆分，在水平拆分维度上能同时支持多库和多表。

>**水平拆分和垂直拆分**
>
>*水平拆分就是将原本一张表的数据，按每条记录的特性分别存储到不同的数据表或数据库中。例如——*
>
>*  *将用户表拆成男用户表和女用户表*
>
>
>*  *将日志表拆成每个月一张表*
>
>
>*  *将某数据备份表拆成周一到周日，七张表并循环使用*
>
>
>*  *按手机尾号的数字将用户拆分到10张表上。*
>
>*水平分表后，拆开的表可以分布在同一个数据库上，也可以分布到不同的数据库上。水平分区在大型网站，大型企业应用中经常采用。目的是出于海量数据分散存储，分散操作，分散查询以便提高数据处理量和整体数据处理性能。*
>
>*垂直拆分，就是将不同的数据表存在到不同的数据库中。起到均衡负载的作用。解决多个表之间的IO竞争问题。但是垂直拆分不解决单表数据库过大引起的查询和修改效率等问题。*

在水平分片的情况下，数据分库分表以后对操作有着巨大的限制——不支持分区表和其他表关联（Join）。因此一旦设置成分区表，这张表上只能执行单表操作了。跨分区查询、排序等特性也是分库分表后的难点。

EF-ORM的功能特色

1. 按需建表——对于按时间分表的场合，由于时间范围是无穷的，因此不可能实现创建所有的数据表。EF-ORM可以在需要用到某个时间点表的时候，为其创建表。
2. 范围分析——水平分区后，如果分区条件完整，那么各种框架都可以简单的定位到需要操作的分区表上。但如果分区条件不完整、或者只是一个区间范围，那么EF能够给予区间运算等逻辑，准确的计算出需要操作的表的范围。并且将操作局限在数量最少的表上。
3. 跨表查询——水平分区后跨表操作的场合下，EF-ORM能够通过调度改写为union语句、count数累加、顺序查询、结果集合并、结果集重排序等一系列操作模块，在内存占用极小的情况下完成对多表、多库查询乃至排序的需求。
4. 跨库查询——水平分区后跨库操作的场合下，可以对多库的不同表同时操作，影响记录数累加，Select语句的场合结果集可自动合并。
5. 双解析引擎。无论是用CriteriaAPI方式操作数据库，还是SQL方式操作数据库，都支持解析Query/SQL语句来分析分表条件，并路由到不同的数据源上进行操作。
6. 全自动。 用户只需要配置分表分库规则。在执行非多表关联操作时，分库分表引擎即自动生效。路由、多库操作、结果集处理其他无需操心。在多表/多库，数据库自增键值不能再使用数据库列自增了。此时包括Sequence/自增键生成规则变更等，也是均自动完成。

众所周知：水平拆分下，数据操作面临相当复杂的挑战——

   1)distinct 

       因为需要遍历所有shard分区，并进行合并判断重复记录。 

   2)order by 

        类似 1) 

   3)aggregation 

        count，sim，avg等聚合操作先分散到分区执行，再进行汇总。

   4)跨分区查询时的多表Join

        分区表Join非分区表。目前大部分分库分表方案都没有解决这个问题

备注：EF-ORM能支持水平拆分下的跨表和跨库操作

| 查询操作       | 支持的操作                                    | 不支持的操作 |
| ---------- | ---------------------------------------- | ------ |
| 跨表情况下(不跨库) | count  Distinct  Order by  Group by  Group by+having  Order by+分页  Group by+having+分页 | Join操作 |
| 跨库情况下      | Count  Order by  Distinct(有数据量限制)  Order by+分页(有数据量限制)  Distinct(有数据量限制)  Group by(有数据量限制)  Group by+having(有数据量限制)  Order by+分页(有数据量限制)  Group by+having+分页(有数据量限制) | Join操作 |

上述“有数据量限制”的特性，是指查询返回的结果数量不能过大。（并非指数据库中的记录数量）。因为上述操作都是不得不进行内存操作的功能，所以不建议在太大的结果集上进行操作。一般来说，OLTP类应用单笔业务影响的记录行数不超过1000条，因此EF-ORM的这种支持方案是能满足大部分需求的。但是，不建议在OLAP类应用中尝试用EF-ORM来解决跨库数据合并的问题。

### 10.1.3.  和具有同类功能的产品比较

这里比较笔者有了解的Hibernate Shards和TDDL两个框架， Ibatis-Sharding没用过，有机会再补充。下面的各种论点均为采集互联网资料了解而得，可能有错漏、陈旧之处，请读者包含。

#### * Hibernate Shards / HiveDB


Hibernate Shards是Google的工程师贡献的开源代码，集成到Hibernate中使用后可以支持数据水平拆分。HiveDB是基于HibernateShard 用来横向切分mysql数据库的开源框架。

这个解决方案目前通过 Criteria 接口的实现对 聚合提供了较好的支持， 因为 Criteria 以API接口指定了 Projection 操作，逻辑相对简单。

EF-ORM中的shards功能和上述框架比较类似，

1. 还有很多hibernate的API没有实现
2. 不支持cross-shard的对象关系，比如A、B之间存在关联关系，而A、B位于不同的shard中。hibernate shards提供了CrossShardRelationshipDetectingInterceptor，以hibernate Interceptor的方式来检测cross-shard的对象关系问题，但这个拦截器有一定的开销（比如需要查映射的元数据、有可能过早的触发延迟加载行为等），可以用于测试环境来检测cross-shard关系的问题
3. hibernate shards本身不支持分布式事务，若要使用分布式事务需要采用其他解决方案
4. hql、criteria存在不少限制，相比于hql，criteria支持的特性更多一些
5. Session或者SessionFactory上面有状态的拦截器，在shard环境下面会存在一些问题。拿session来说，在hibernate中拦截器是对单个session上的多次sql执行事件进行拦截，而在shard情况下hibernate shards的ShardedSession会对应每个shard建立一个session，这时拦截器就是跨多个session了，因此hibernate shards要求有状态的拦截器必须通过实现StatefulInterceptorFactory来提供新的实例。如果拦截器需要使用到目标shard的session，则必须实现hibernate shards的RequiresSession接口 

EF相较于这两个框架，其优势是——

1.    提供了较好的分区范围分析算法。而HibernateShard依赖于用户自行实现分区策略的计算。而当分区条件含糊时，用户很难编写出精确的路由算法。

2.    针对分表和分库的情况加以区分，在同个数据库上的时候能利用SQL操作实现排序和聚合计算，对服务器的CPU和内存压力较小。而HibernateShard不区分这两种情况。

3.    优化的多库排序： 在多库排序时，能分析分表规则，当分表条件和排序条件一致时，直接将各个结果集按排序条件拼合。免去了排序的性能开销。

              在必须重排序时，利用每个库各自的顺序，使用了内存占用较小的排序算法。

4.    EF-ORM中分区操作对用户基本透明，无需移植。而从hienrate移植到HibernateShard时的部分接口类需要调整。

5.    hibernate shards没有对hql进行解析,因此hql中的count、sum、distinct等这样的操作还无法支持。而EF-ORM除了API层面外，对于传入的SQL语句也支持分库分表。

6.    配置更简单，业务侵入性更小，对开发人员透明度更高。EF-ORM中除了Entity上需要加入少量注解外，开发者无需关心任何分库分表相关的工作，也无需编写任何代码。hibernate shards等则需要开发者实现若干策略。

7.    EF-ORM的主键生成算法也做到对用户透明，在支持Sequence下的数据库中，会使用Sequence生成自增序列。其他数据库上会使用具有一定步长的表来生成Sequence。

8.    Hibernate shards不支持按时间或实际使用时建表。




EF-ORM其功能基本包含了Hibernate Shards / HiveDB。但还有一定不足——

1.    没有提供并行查询策略。目前多次查询的场合都是顺序操作。

2.    virtual shards，虚拟分片的作用是将数据分得更细，然后多个虚拟片公用一个实际的表。这样的好处是virtualshards和实际shards之间的映射关系还可以后续调整。

3.    Hibernate shards的诞生和发展周期更长，功能完善程度更高，包括相同维度切分的实体之间的级联关系等也都做了处理。




EF-ORM和上述框架共有的不足之处是——

1.    两者都绑定特定的ORM实现，前者绑定Hibernate，后者是EF-ORM的一部分。

      ​



####* Alibaba TDDL

TDDL（Taobao DistributedData Layer）顾名思义，是淘宝网的分布式数据框架。它和众多的连接池一样，是一个封装为DataSource的中间框架，也能处理SQL语句的分析和路由。

 应该说，TDDL和本框架之间关注内容和所属的层次是不同的，TDDL的处理是更为底层的：

1. 是在JDBC规范下，以DataSource方式进行封装的。TDDL是对SQL语句进行分析和处理的，而不是Criteria对象上进行处理的。这使得TDDL能拦截一切数据库操作，但也使得复杂场景下的分库分表路由支持变得困难。路由条件的传递除了解析SQL之外，TDDL中还为此开了不少“后门”来传递路由条件。
2. 数据路由只是TDDL的一部分功能，TDDL本身还提供了SQL重试、负载均衡等一系列提高可用性的模块。正如官方材料中说的，你可以使用TDDL的分库分表功能，也可以不使用。而仅仅使用其底层封装的高可用性模块。



TDDL目前最大问题是，其“开源程度”不高，所谓的“开源程度”问题，主要是指

1. Github上，该项目只有2012年更新过，仅有4次代码提交。而阿里内部，该软件的版本则一直在升级改进，但并未对外公开。
2. TDDL升级改进的需求几乎均来自阿里内部，其开发团队几乎未对外部用户和开源社区做出什么响应。
3. TDDL中最关键的分库分表层的模块代码一直没有在开源项目中出现。遭到不少网友的疑问。
4. 几乎没有社区支持，官方也没有文档支持。仅有的少量文档都是阿里内部流出，其对应的版本和代码不明。

TDDL 3.3.x的代码最终还是找到了，我也读过了，感觉官方开发团队为了解决JDBC庞大的API支持投注的精力十分可观，因此对分库分表等支持上多少不够专注。

从我的感觉来说，TDDL最实用的地方恰恰不是其分库分表功能，而是——

1. 数据库主备和动态切换； 
2. 带权重的读写分离； 
3. 单线程读重试； 
4. 集中式数据源信息管理和动态变更； 
5. 可分析的日志打印 , 日志流控，动态变更；

这些提高应用可用性、可维护性、伸缩性方面的特性。

由于TDDL的功能和EF-ORM的功能仅仅有少量是重叠的（分库分表），因此我们仅能对重叠部分进行一些功能的比较。

TDDL的开源版本（两年前的版本）分库分表功能相较于目前EF-ORM其问题是——

1. TDDL 需要依赖 diamond 配置中心
2. TDDL专注于支持MySQL等内部需要使用的场景，对其他数据库下的解析和处理支持有限。
3. 不支持从Between……and中解析分库条件，不支持SQL中的NOT语义。
4. TDDL目前还不支持任何种类的注释。TDDL目前还不支持强制指定使用排他锁的方式。
5. 不支持从当前数据库已有的分表中扫描存在的分表作为决策。
6. 不支持按时间或实际使用时建表。
7. TDDL对聚合函数支持较差，如Count,avg,max,min等还不支持，称在以后的版本中会对出现在column列名字段的聚合函数予以支持。但不会对group by 中having后出现的聚合函数加以支持。
8. TDDL不支持 Having以及having内使用的其他聚合函数。
9. 事务的有限支持。支持基于单库的事务，但不支持跨库进行事务。

   ​上述是从目前网上了解到的对开源版本的TDDL的一些对比。从流出的TDDL 3.3.x版本代码来看，上述许多功能应该是是已经支持的。但具体情况因缺少支持文档不明。



## 10.2.  分库分表规则配置

本章，我们开始了解EF-ORM的分库分表功能使用。

### 10.2.1.  水平拆分——分表

首先，我们需要了解分库分表的配置。默认情况下，分库分表规则可以通过Entity类上标注的注解来描述。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\entity\OperateLog.java

~~~java
@Entity
@PartitionTable(key = {
	@PartitionKey(field = "created", function = KeyFunction.YEAR_MONTH)
 })
public class OperateLog extends DataObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @GeneratedValue(generator = "created-sys")
    private Date created;

    @Column(name="message",length=500)
    private String message;

    public enum Field implements jef.database.Field {
        id, created, message
    }
}
~~~

上例中的注解@PartitionTable就是分库分表规则了。

每个@PartitionKey表示一个分表条件字段。上例中的配置表示——取对象的createDate字段，取其年+月（yyyyMM格式），作为表名的后缀。

对于上面这样配置的日志表。2014-1-31日的日志就会存放在 operatelog\_201401表中，2014-2-1日的日志就会存放在operatelog_201402表中。

如果插入前createDate字段没有值，那么意味着无法决定记录要插到哪张表中，此时会抛出异常。

当分库条件不明确时，路由计算器会尝试计算一个范围来覆盖记录所在的表中，因此分库分表的条件含糊可能引起路由返回结果为多张表、甚至多个数据库。

对于几种操作情形，路由返回结果为多张表时，EF-ORM将会采取不同的策略来执行。

| 操作     | 单库单表 | 单库多表                                     | 多库多表                                     |
| ------ | ---- | ---------------------------------------- | ---------------------------------------- |
| insert | 支持   | 抛出异常                                     | 抛出异常                                     |
| update | 支持   | 在所有表上执行update                            | 在所有表上执行update                            |
| delete | 支持   | 在所有表上执行delete                            | 在所有表上执行delete                            |
| select | 支持   | 尝试转换为union all语句。配置一定结果集处理。              | 对每个库上的多表采用 union all。  对多个库的结果集进行混合计算。   |
| count  | 支持   | 如果不涉及distinct、group by则在所有表上执行count查询，并将结果累加。  如果涉及distinct、group by则查询结果并计算将SQL修改为在union all外部嵌套distinct和group by，最后计算总数。 | 如果不涉及distinct、group by则在所有表上执行count查询，并将结果累加。  如果涉及distinct、group by则查询结果并汇总混合。最后得到结果的数量。（数量有限制） |

 

我们再看下面一个例子，有一张表，其中有两个字段name和created，我们希望以这两个字段为关键字进行分表操作。我们可以这样定义。

~~~java
@PartitionTable(key = {
	// 分区关键字1为name字段，取头3个字符
	@PartitionKey(field = "name", length = 3),
	// 分区关键字2为created字段(日期型，取其月份数，长度不足2则补充到2)，
	@PartitionKey(field = "created", function = KeyFunction.MONTH, length = 2) })
@Table(name = "PEOPLE_TABLE")
@Entity
public class PeopleTable extends DataObject {
	private int id;
	private String name;
	private Date created;
}
~~~

对于这种配置，其分表条件有两个。如果用以下代码插入记录到数据库——

~~~java
PeopleTable entity=new PeopleTable();
entity.setId(1);
entity.setCreated(DateUtils.getDate(2011, 2, 1));
entity.setName("ZHANG");
db.insert(entity);
~~~

实际上该记录会写入到表 *PEOPLE_TABLE_ZHA02*中。其中ZHA是第一个分表条件产生的，02是第二个分表条件产生的。多个分表条件之间默认没有任何分隔符，如需要可以用keySeparator参数配置。

当查询数据时：

~~~java
PeopleTable entity=new PeopleTable();
entity.getQuery().addCondition(PeopleTable.Field.name, "WANG");
entity.getQuery().addCondition(PeopleTable.Field.created, new Date());//假设现在是3月份
List<PeopleTable> data= db.select(entity);
~~~

查询时的表名也是根据条件自动计算的，实际上会到一张名为 PEOPLE_TABLE_WAN03的表中查询。

从上面的配置我们可以了解到通过@PartitionTable注解，我们可以配置一些规则，这些规则将在表名后面添加若干后缀，作为一张分表。

@PartitionTable的可配置项包括

| 属性           | 作用和举例                    | 缺省值  |
| ------------ | ------------------------ | ---- |
| appender     | 描述原始表名和分区后缀之间的分隔符        | "_"  |
| keySeparator | 描述多个KEY之间的分隔符            | “”   |
| key          | 多个分区条件。多值@PartitionKey   | 无    |
| dbPrefix     | 如果有配置了数据库名的分区条件时，数据库名的前缀 | “”   |

@PartitionTable的可配置项

@PartitionTable的key属性中，我们可以配置多个@PartitionKey。

@PartitionKey的可配置项包括

| 属性                        | 作用和举例                                    | 缺省值   |
| ------------------------- | ---------------------------------------- | ----- |
| field                     | 指定对应的字段(分表分库规则的依据字段)                     | 无     |
| function                  | 指定字段上函数(一般是对日期进行处理或者数值取摸)  函数可以从jef.database.routing.function.KeyFunction类中枚举。  KeyFunction类中定义了各种日期时间函数和数字取模函数。 | RAW   |
| functionClass             | 指定一个自定义的字段函数。  例如，配置为ModulusFunction.class，便表示对指定的分表字段进行取模运算。  此参数经常需要和functionClassConstructorParams注解同时使用，用于提供构造处理函数所需的构造参数。     此注解不可与function注解同时使用。   一个例子是这样的：  @PartitionKey(           field ="amount", length =  2,           functionClass=ModulusFunction.class,           functionClassConstructorParams={"5"}  )  这个例子表示，对amount字段，按5取模，然后补充到两位数后作为分表后缀名。 | 无     |
| functionConstructorParams | 和function或者functionClass同时使用，描述functionClass构造时的构造参数。配置时只能是文本，使用时会自动转换。 | {}    |
| isDbName                  | 当此值设置为true时，当前ParitionKey计算出来的字符串不是作为分表名称的一部分，而是作为一个独立的数据库名。  这种用法用于当应用部署在多个独立的数据库上时，可以实现跨库的数据库操作  每次数据库操作可以通过这个对象得到其他的数据库的连接，从而实现跨库的数据库操作。 | false |
| defaultWhenFieldIsNull    | 当指定字段无值时，使用的缺省值                          | “”    |
| length                    | 指定用作表名后缀的字符串长度，0表示不限制。当长度限定时，如果不足会填充。如果超过会截断 | 0     |
| filler                    | 当表名后缀的字符串长度不足时，在左侧填充的字符。                 | ‘0’   |

@PartitionKey的可配置项

上述配置可以描述对象的分表规则。一旦配置了分表规则后，就可以像操作正常的实体一样进行CRUD操作。框架会自动解析对象中的数值和Query中的条件，来判断需要操作哪些分表。

 ### 10.2.2.  水平拆分——分库

上面的配置除了可以实现分表配置外，还可以配置出水平分库的实体。下面例举了一个既分库又分表的实体。

customer表可以按客户编号(customerNo)，按3取模。然后补到两位作为数据库名。我们可以推测出根据customerNo的不同，记录会分别落到D0, D1,D2这样三个名称的数据库上。

同时，按照createDate取年+月进行分表。

~~~java
@Entity
@PartitionTable(key = {
		@PartitionKey(field = "createDate",function=KeyFunction.YEAR_MONTH),
		@PartitionKey(field = "customerNo",functionClass=ModulusFunction.class,
				  functionConstructorParams="3",isDbName=true,filler='D',length=2)
})
public class Customer extends DataObject {
	/**
	 * 客户编号
	 */
    private int customerNo;
    /**
     * 出生日期
     */
    private Date DOB;
    /**
     * 死亡日期
     */
    private Date DOD;
    /**
     * 名
     */
    private String firstName;
    /**
     * 姓
     */
    private String lastName;
    /**
     * 电子邮件
     */
    private String email;
    /**
     * 记录创建日期
     */
    private Date createDate;

    public enum Field implements jef.database.Field {
        customerNo, DOB, DOD, firstName, lastName, email, createDate
    }
}
~~~

包含分库分表条件的Entity。

根据上面的配置，我们可以推测出——

customerNo=1000，createDate=2014-1-3的记录，将会存放到D1数据库的customer_201401表中。

customerNo=1001，createDate=2014-2-1的记录，将会存放到D2数据库的customer_201402表中。

customerNo=1002，createDate=2014-2-1的记录，将会存放到D0数据库的customer_201402表中。

可能会有这样的疑问：我在开发的时候怎么能知道部署的时候会有几个数据库呢？怎么能确定部署时的数据库叫什么名字呢？在这里配置的数据库名其实也是一种虚拟数据源。实际使用时，可以在jef.properties中配置映射关系。

~~~properties
db.datasource.mapping=D0:mysql01,D1:mysql02,D2:mysql03
~~~

上面的配置，可以将开发时指定的虚拟数据源D0,D1,D2映射到实际部署环境的mysql01,mysql02,mysql03三个数据库上。

 ### 10.2.3.  垂直拆分

数据垂直拆分是一个更简单的做法，也就是某类数据（某些表）被单独放到别的数据源上。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\entity\Person2.java

~~~java
/**
 * 垂直拆分的实体。
 * 所谓数据垂直拆分，意思是将一类表放到不同的数据库上。从而降低负载。
 */
@Entity
@BindDataSource("datasource2")
public class Person2 extends DataObject {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
    private int id;

	@Column(length=64)
    private String name;

	@Column(name="DATA_DESC",length=255)
    private String desc;

    @GeneratedValue(generator="created")
    private Date created;

    @GeneratedValue(generator="modified")
    private Date modified;
}
~~~

一旦进行了上述配置则Person2表将始终保存到数据源datasource2上。前面已经说过，在实际部署环境中，可以通过db.datasource.mapping参数将数据源名称映射为部署环境的真实数据库名。

 ## 10.3.  分库分表后的操作和行为

### 10.3.1.  前提与原则

 分库分表以后，有以下几个最基本的限制和规则

1. 不支持多表查询。所有的单表查询Query API都能支持分库分表。所有的多表查询则都会**忽略**分库分表的配置规则。

       在NativeQuery中运行单表查询的SQL语句，都能支持分库分表规则。而多表的SQL语句则都不支持。

2. 原生SQL操作方式由于对SQL是不作解析的，因此也就不支持分库分表。
3. 目前分库分表功能支持多库多表下的常用聚合函数、Distinct、Order by、Start with... Connect by、分页。 
4. 目前分库分表功能不能支持分析函数、不支持聚合函数中的avg函数。
5. 使用API方式，分库分表前后的数据库操作方式没有任何差异。
6. 使用NativeQuery方式，要启用分库分表功能。需要调用NativeQuery的withRouting()方法(见后文示例)。

在对分库分表的操作有了上述认识后，可以看下面的特性和示例。

### 10.3.2.  按需建表

我们来看分库分表后的实际操作例子。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\Case1.java

~~~java
/**
* 测试在分库后的表中插入一条记录
*/
@Test
public void createTest() throws SQLException {
	OperateLog log = new OperateLog();
	log.setCreated(DateUtils.getDate(2010, 3, 2));
	log.setMessage("测试！！");
	db.insert(log);
}
~~~

在上例中，我们可以发现，实际运行SQL语句为

~~~sql
insert into OPERATELOG_20100302(ID,CREATED,MESSAGE) values(?,?,?)
~~~

但是在插入之前，表OPERATELOG_20100302还根本不存在。此时框架会自动创建这张表，并将记录插入到这张表中。

### 10.3.3.  自增主键生成行为

我们在仔细看上面的例子还会发现更多的东西。比如，在对象中，我们配置了@GeneratedValue(strategy=GenerationType.*IDENTITY*)。正常情况下将会使用数据库表列的自增功能来生成主键。但是多表下问题就来了，不同的表可能会生成重复的ID。因此我们可以看到框架自动将主键生成的行为变更了，在Derby上，采用TABLE的方式来生成自增主键值……打印出的SQL日志如下------

~~~sql
SELECT V FROM global_sequences WHERE T='OPERATELOG_SEQ' | [derby:DB@1]
UPDATE global_sequences SET V=? WHERE V=? AND T='OPERATELOG_SEQ'  |[derby:DB@1]
(1):               [60]
(2):               [40]
~~~

显然到TABLE中逐个获取序号，效率是非常低下的，因此EF-ORM采用了以下两个手段来保证效率——

1. 一次获取多个序号，上例中，一次SQL操作直接将序号递增20。即一次获取20个序号。
2. CAS(ComapreAndSwap)操作。上例的UpdateSQL语句是支持并发的，一旦有多个SQL同时操作该表，能保证每个SQL都能正确获取序号值。

EF-ORM中，用于调节Sequence获取行为的参数还有不少。比如TABLE方式，可以每个表一个TABLE，也可以全局公用一个TABLE。TABLE下的Sequence等也都可以调节。详见附录一 配置参数一览。

### 10.3.4.  手动建表

DbClient中提供了若干数据库表维护的API，使用这些API，可以在数据库中创建表。

对于启用了分库分表后的情况，我们可以用下面的测试代码试验一下。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\Case2.java

~~~java
	//现在删表，删表时会自动扫描目前存在的分表。
 db.dropTable(Customer.class, Device.class, OperateLog.class, Person2.class);

 System.err.println("现在开始为Customer对象建表。Customer对象按“年_月”方式分表，并且按customerNo除以3的余数分库");
 db.createTable(Customer.class);
 System.out.println();

System.err.println("现在开始为Device对象建表。Device对象按“IndexCode的头两位数字”分表，当头两位数字介于10~20时分布于ds1；21~32时分布于ds2；33~76时分布于ds3；其他情形时分布于默认数据源");
db.createTable(Device.class);
System.out.println();
			
 System.err.println("现在开始为OperateLog对象建表。OperateLog对象按“年_月_日”方式分表，不分库");
 db.createTable(OperateLog.class);
 System.out.println();
			
 System.err.println("现在开始为Person2对象建表。Person2对象是垂直拆分，因此所有数据都位于datasource2上。不分表");
 db.createTable(Person2.class);
 System.out.println();

System.err.println("======= 建表操作完成，对于分区表只创建了可以预测到的若干表，实际操作中需要用到的表会自动按需创建=========");
~~~

**Drop时，扫描数据库中存在的表**

在不指定特定的分库分表时，dropTable方法会检索数据库中所有存在的分表加以删除。

**Create时，预测需要的表来创建**

而createTable时，框架会尽量预测需要创建的分表来尝试创建。

预测的方法是——

对于取模类函数，将取模可能的值进行遍历。如按10取模，那么会预测到0~9的所有数字。

对于日期类的函数，以当前日期为界，创建前后一段时期内的表（这个时期范围可以通过jef.properties中的全局配置参数），如果配置下面这样，表示选取过去三个月和未来三个月。

~~~properties
partition.date.span=-3,3
~~~

要注意的是，上述参数不仅仅用于建表时表名的推测，也用于执行数据库操作时的分表范围推测。比如用户查询OperateLog数据时，没有指定时间范围，那么框架默认就查询距今前后三个月的表。不会再查询三个月之前的表。

上述方法都是手工创建一个分区表的所有分表。我们也可以手工指定分库分表条件，来创建某张特定的分表。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\Case2.java

~~~java
@Test
public void ddlTest() throws SQLException{
	Customer customer=new Customer();
	customer.setCustomerNo(1234);
	customer.setCreateDate(DateUtils.getDate(2016,12,10));
	db.createTable(customer);
	db.dropTable(customer);
		
		
	Device d=new Device();
	d.setIndexcode("123456");
	db.createTable(d);
	db.dropTable(d);
}
~~~

上述方法中，由于指定了分表条件，因此不会再像上例一样，在所有可能的数据库上创建分表，而是会在特定的datasource上创建和删除CUSTOMER\_201612、 DEVICE_1表。

### 10.3.5.  路由时过滤不存在的表

对于未明确指定分库分表条件的查询，框架会查询所有可能有此记录的表。这种“可能性”分析并不仅仅局限于函数和枚举操作，还包括对数据库现存的表的扫描。因此正常情况下，数据库中存在的表才会被查询到。数据库中不存在的表，将不会被查到。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\Case2.java

~~~java
/**
 * 当分表结果计算后，对比数据库中实际存在的表。不存在的表不会参与查询。
 * 
 * @throws SQLException
 */
@Test
public void testTableFilter() throws SQLException {
	Device d=new Device();
	d.setIndexcode("665565");
	db.createTable(d);
	ThreadUtils.doSleep(1000);
	System.out.println("第一次查询，表Device_6存在，故会查询此表");
	Query<Device> query = QB.create(Device.class);
	List<Device> device=db.select(query);
	
	db.dropTable(d);
	ThreadUtils.doSleep(500);
	System.out.println("第二次查询，由于表Device_6被删除，因此不会查此表");
	device=db.select(query);
}
~~~

前面提到的按需建表功能也好，此处的过滤不存在的表功能也好，都是对现有数据库中表是否存在进行判断。显然这个操作是较为耗时的，不可能每次计算路由时临时计算。因此，数据库中现存的表的情况，是被缓存下来的。

您可以关闭不存在表过滤的功能。通过jef.properties中配置

~~~properties
partition.filter.absent.tables=false   //关闭表过滤功能。
~~~

同时，分表数据缓存的有效期可以设置

~~~properties
db.partition.refresh=3600   //设置分表信息缓存的最大生存时间。
~~~

### 10.3.6.  如果路由为空

如果分表计算发现数据库中不存在符合条件的表，那么实际上就无需查询任何表，像下面这种情况。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\Case2.java

~~~java
/**
* 当分表结果计算后，发现没有需要查询的表的时候，会直接返回
*/
@Test
public void testNoMatchTables() throws SQLException {
	Query<Device> d = QB.create(Device.class);
	//当Device_9表还不存在时，这个查询直接返回空
	if(!db.getMetaData(null).existTable("DEVICE_9")){
		d.addCondition(Device.Field.indexcode, "9999999");
		List<Device> empty=db.select(d);
		assertTrue(empty.isEmpty());
	}
}
~~~

上例中，本来按分表条件应该查询DEVICE_9表。但是该表不存在，因此路由模块会返回空。此时框架直接返回空列表，不作任何查询。

### 10.3.7.  垂直拆分的场景

垂直拆分就是将Entity用@BindDataSource绑定到另外一个数据库上去。实际使用的演示可以参考示例代码。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\Case2.java

~~~java
/**
* 当采用垂直拆分时， Person2的所有操作都集中在DB2上。而不是在默认的DB上操作
*/
@Test
public void test1() throws SQLException {
	// 插入
	Person2 p2 = new Person2();
	p2.setName("Jhon smith");
	db.insert(p2);

	// 查询
	Person2 loaded = db.load(Person2.class, p2.getId());

	// 更新
	loaded.setName("Kingstone");
	assertEquals(1, db.update(loaded));

	// 查询数量
	assertEquals(1, db.count(QB.create(Person2.class)));

	// 删除
	assertEquals(1, db.delete(loaded));
	// 删除后
	assertEquals(0, db.count(QB.create(Person2.class)));
		
	System.out.println("=============== In Native Query ============");
	{
		String sql="insert into person2(name,data_desc,created,modified) values(:name,:data,sysdate,sysdate)";
		NativeQuery query=db.createNativeQuery(sql, Person2.class).withRouting();	
		query.setParameter("name", "测试111");
		query.setParameter("data", "备注信息");
		query.executeUpdate();
	}
	{
		String sql="select * from person2";
		NativeQuery<Person2> query=db.createNativeQuery(sql, Person2.class).withRouting();
		query.getResultCount();
			
		query.getResultList();	
	}
	{
		String sql="update person2 set name=:name where id=:id";
		NativeQuery query=db.createNativeQuery(sql).withRouting();
		query.setParameter("id", 1);
		query.setParameter("name", "测试222");
		query.executeUpdate();
	}
	{
		String sql="delete person2  where id=:id";
		NativeQuery query=db.createNativeQuery(sql).withRouting();
		query.setParameter("id", 2);
		query.executeUpdate();
	}
}
~~~

上例分为两部分。第一部分是基于API的数据库操作。第二部分是基于SQL语句的数据库操作。

运行上面的案例，可以发现基于垂直分区的配置，所有的数据库操作都在DB2上运行，而不是在默认的数据库上运行。

可以发现，在垂直拆分的情况下，API方式操作和以前没有任何变化。而使用SQL语句NativeQuery方式的时候，需要调用一下withRouting()方法。

### 10.3.8.  在分库分表条件基本确定的场景下

我们以customer表为例，看一下在customer表上执行增删改查操作的情况。Customer对象按customerNo除以3的余数，分配到三个数据库上。同时customer还按照记录的创建时间进行了分表。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\Case2.java

~~~java
/**
 * Customer对象按创建记录created字段时的“年_月”方式分表，并且按customerNo除以3的余数分库
 */
@Test
public void testCustomer() throws SQLException {
	// 插入
	Customer c = new Customer();
	c.setLastName("Hession");
	c.setFirstName("Obama");
	c.setDOB(DateUtils.getDate(1904, 3, 4));
	c.setDOD(DateUtils.getDate(1991, 7, 12));
	c.setEmail("obama@hotmail.com");
	db.insert(c);
	/*
	 * 这个案例中，用作分库分表的customerNo和created这两个字段都没赋值，但是记录却成功的插入了。
	 * 因为customerNo是自增值，created是系统自动维护的当前时间值。系统自动维护了这两个字段，
	 *从而使得分库分表的路由条件充分了。
	 * 此外，注意自增值的生成方式变化了。在本例中，使用TABLE来实现自增值。因为Derby不支持Sequence，
	 *所以采用TABLE方式生成自增值。(实际上支持，但兼容性有些问题，故关闭了)
	 */

	Customer c2 = new Customer();
	c2.setLastName("Joe");
	c2.setFirstName("Lei");
	c2.setDOB(DateUtils.getDate(1981, 3, 4));
	c2.setEmail("joe@hotmail.com");
	c2.setCreateDate(DateUtils.getDate(2013, 5, 1));
	System.out.println("====将会按需建表====");
	db.insert(c2);
	/*
	 * 这里指定记录创建时间为2013-5-1，也就是说该条记录需要插到表 CUSTOMER_201305这张表中。
	 * 这张表虽然现在不存在，但是EF-ORM会在需要用到时，自动创建这张表。
	 */

	// 路由维度充分时的查询
	System.out.println("当分表条件充分时，只需要查一张表即可...");
	Customer loaded1 = db.load(c);
	
	// 查询。
	System.out.println("当分表条件不完整时，需要查询同一个库上的好几张表。");
	Customer loaded = db.load(Customer.class, c.getCustomerNo());

	// 更新
	loaded.setLastName("King");
	assertEquals(1, db.update(loaded));

	// 查询总数，由于有没分库分表条件，意味着要查询所有库上，一定时间范围内的所有表
	assertEquals(1, db.count(QB.create(Customer.class)));

	// 删除
	assertEquals(1, db.delete(loaded));
	// 删除后
	assertEquals(0, db.count(QB.create(Customer.class)));
}
~~~

上述代码中，展现了对customer对象的基本操作，包括插入，加载、更新、删除、计算总数等。对照日志中输出的SQL语句，可以发现当对象中createDate和customerNo两个字段均有值时，操作会准确的定位到一个库的一张表上。

但是在按customerNo查询时，由于缺少createDate这个分表条件，框架不得不在前后几个月的表中就近进行查询。而当不带任何条件查询总数时，框架在所有库上的多张表中进行了查询。

### 10.3.9.  Batch模式和分库分表

分库分表对Batch模式也有一定的影响。由于分库分表后，不同库和表上的数据操作SQL语句是不一样的，因此框架必须对Batch中的对象进行分组，将同一张表里的记录划为一组，然后再对每组进行操作。

[Case2.java](../../orm-tutorial/src/main/java/org/easyframe/tutorial/lessona/Case2.java)

~~~java
/**
* 演示Batch操作在分库分表后发生的变化
*/
@Test
public void testCustomerBatch() throws SQLException {
	List<Customer> list = new ArrayList<Customer>();
	// 批量插入
	Customer c = new Customer();
	c.setLastName("Hession");
	c.setFirstName("Obama");
	c.setDOB(DateUtils.getDate(1904, 3, 4));
	c.setDOD(DateUtils.getDate(1991, 7, 12));
	c.setEmail("obama@hotmail.com");
	list.add(c);

	c = new Customer();
	c.setLastName("Joe");
	c.setFirstName("Lei");
	c.setDOB(DateUtils.getDate(1981, 3, 4));
	c.setEmail("joe@hotmail.com");
	c.setCreateDate(DateUtils.getDate(2013, 5, 1));
	list.add(c);

	c = new Customer();
	c.setCustomerNo(4);
	c.setLastName("Yang");
	c.setFirstName("Fei");
	c.setDOB(DateUtils.getDate(1976, 12, 15));
	c.setEmail("fei@hotmail.com");
	c.setCreateDate(DateUtils.getDate(2013, 5, 1));
	list.add(c);
		
	System.out.println("当Batch操作遇到分库分表——\n所有记录会重新分组后，再进行批量插入。");
	db.batchInsert(list);

	// 跨表跨库的搜索
	Query<Customer> query = QB.create(Customer.class);
	query.addCondition(QB.between(Customer.Field.createDate, DateUtils.getDate(2013, 4, 30), DateUtils.getDate(2014, 12, 31)));

	// 由于条件只有一个时间范围，因此会搜索所有可能出现记录的表。包括——三个数据库上所有时段在13年4月到14年12月的表。理论上所有可能的组合。
	// 但由于大部分表不存在。EF-ORM只会查找实际存在的表，实际上不存在的表不会被查找。
	list = db.select(query);
	assertEquals(3, list.size());

	// 批量更新
	for (int i = 0; i < list.size(); i++) {
		list.get(i).setEmail("mail" + i + "@hotmail.com");
	}
	// 虽然是Batch更新，但实际上所有记录分散在不同的库的不同的表中，重新分组后，每张表构成一个批来更新
	db.batchUpdate(list);

	// 批量删除
	db.batchDelete(list);
}
~~~

上面的测试案例，演示了分库分表后的Batch操作。一般来说，用户也无需关心分库分表后Batch操作上的变化。框架会自动按照重新分组的方式进行batch操作。

 但是也有特殊情况。分库分表路由计算毕竟是个一定耗时的操作。某些场景下，用户能在自己的业务中保证batch中的所有数据都是在一个表上的，此时对每条记录进行路由计算就显得多余了。这时就可以使用session中另一个batch操作的方法，即传入一个Boolean值，表示是否需要按路由重新分组。

~~~java
db.batchDelete(list,true); //需要对每个传入数据进行路由计算并重新分组

db.batchInsert(list,false); //不需要对每个传入数据进行路由计算

db.extremeInsert(list, true);//需要对每个传入数据进行路由计算并重新分组

db.extremeInsert(list, false);//不需要对每个传入数据进行路由计算
~~~

### 10.3.10.  当路由结果为多库多表时

那么，现在我们来看一看分库分表查询中最为复杂的情况。即路由条件不足，造成多库多表查询的情况。该案例较为复杂，也非常长，因此代码不全部贴出了。可以参见示例工程下的java代码。

[Case2.java](../../orm-tutorial/src/main/java/org/easyframe/tutorial/lessona/Case2.java)

~~~java
/**
* 演示若干复杂的跨库Cirteria操作。这些操作虽然会涉及多个数据库的多张表，但在框架封装下，几乎对开发者完全透明。
* 
* 案例说明：<br>
* 1、用批模式插入50条记录，这些记录将会分布在三个数据库的10张表中。由于分库维度和分表维度一致，因此实际情况为，表1,8,9位于DB，表2,3位于DB2，表4,5,6,7位于DB3。
* 2、跨库查询总数                        (看点:跨库记录数汇总)
* 3、查询index 为4开头的记录              （看点:条件解析与精确路由）
* 4、查询indexcode含0的记录，并按创建日期排序 (看点: 跨库排序)
* 5、查询indexcode含0的记录，并按创建日期排序，每页10条，显示第二页(看点: 跨库排序+分页)
* 6、所有记录,按type字段group by，并计算总数。（看点：跨库+聚合计算）
* 7、所有记录,按type字段group by，并计算总数。并按总数排序（看点：跨库+聚合计算+排序）
* 8、查询所有记录的type字段，并Distinct。  (看点：跨库 distinct)
* 9、跨数据库查询所有记录，并排序分页     (看点：跨库+分页)
* 10、跨数据库查询所有记录,按type分组后计算总数，通过having条件过滤掉总数在9以上的记录，最后排序 (看点：跨库+聚合+Having+排序)
* 11、跨数据库查询所有记录,按type分组后计算总数，通过having条件过滤掉总数在9以上的记录，最后排序，取结果的第3~5条。 (看点：跨库+聚合+Having+排序+分页)
*/
@Test
public void testDeviceSelect() throws SQLException {}
...
  下略
...
~~~

该案例归纳情况如下——

1. 无论有多复杂，在查询结果数量不大的情况下（OLTP系统的特点）。开发人员都无须关注分库分表做了什么。一切都由框架完成了，除非实在无法支持的情况框架会抛出异常。
2. 框架可以识别利用 >、<、 between、in、not in、like等一切有关的条件来缩小查询范围。
3. 框架可以对查询语句中的group、distinct、having、记录范围等因素识别并进行相应的处理。


总的来说，对于分库分表下的查询操作，开发人员应当自行确定一些操作限制。因为SQL语句的写法是无穷的，变化也是无穷的，而框架能支持的操作场景是有限的。这是所有具备分库分表功能的框架都面临的问题。

​复杂到框架无法支持的多库查询，一般的SQL开发人员自己也很难编写出来正确的查询来。反过来说，一个熟练的DBA做不到的SQL查询，框架也很难做出来。多库多表下的查询功能都是在不停的完善的，对任何一个框架都是如此。如果有什么需要支持的合理场景，也请联系作者，可以考虑将您的想法改进到这个框架当中。

### 10.3.11.  使用SQL语句插入、更新和删除时的路由 

和上面的复杂查询一样，我们例举了使用SQL语句进行分库分表操作的场景。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\Case2.java

~~~java
	@Test
	public void testNativeQuery() throws SQLException{
		List<Device> list = generateDevice(50);
		ORMConfig.getInstance().setDebugMode(false);
		db.batchInsert(list);
		ORMConfig.getInstance().setDebugMode(true);
		System.err.println("=====数据准备：插入50条记录完成=====");
		/**
		 * 使用SQL语句插入记录，根据传入的indexcode每次操作都路由到不同数据库上
		 */
		{
			NativeQuery nq=db.createNativeQuery("insert into DeVice(indexcode,name,type,createDate) values(:indexcode, :name, :type, sysdate)").withRouting();;
			nq.setParameter("indexcode", "122346");
			nq.setParameter("name", "测试插入数据");
			nq.setParameter("type", "办公用品");
			nq.executeUpdate();
			
			nq.setParameter("indexcode", "7822346");
			nq.setParameter("name", "非官方的得到");
			nq.setParameter("type", "大家电");
			nq.executeUpdate();
			
			nq.setParameter("indexcode", "452346");
			nq.setParameter("name", "萨菲是方式飞");
			nq.setParameter("type", "日用品");
			nq.executeUpdate();			
		}
		/**
		 * 使用SQL语句更新记录，使用Between条件，这意味着indexcode在1000xxx到6000xxx段之间的所有表会参与更新操作。
		 */
		{
			System.out.println("===Between条件中携带的路由条件,正确操作表: 1,2,3,4,5,6");
			NativeQuery nq=db.createNativeQuery("update DeVice xx set xx.name='ID:'||indexcode,createDate=sysdate where indexcode between :s1 and :s2").withRouting();;
			nq.setParameter("s1", "1000");
			nq.setParameter("s2", "6000");
			nq.executeUpdate();
		}
		
		/**
		 * 使用SQL语句更新记录，使用In条件，这将精确定位到这些记录所在的表上。
		 */
		{
			System.out.println("===用IN条件更新 ，正确操作表5,6和另外两个==");
			NativeQuery nq=db.createNativeQuery("update Device set createDate=sysdate, name=:name where indexcode in (:codes)").withRouting();;
			nq.setParameter("name", "Updated value");
			nq.setParameter("codes", new String[]{"6000123","567232",list.get(0).getIndexcode(),list.get(1).getIndexcode(),list.get(2).getIndexcode()});
			nq.executeUpdate();
		}
		/**
		 * 使用SQL语句更新记录。由于where条件中没有任何用来缩小记录范围的条件，因此所有的表上都将执行更新操作
		 */
		{
			System.out.println("===正确操作表： 2==");
			NativeQuery nq=db.createNativeQuery("update Device set createDate=sysdate where type='办公用品' or indexcode='2002345'").withRouting();;
			nq.executeUpdate();
		}
		
		/**
		 * 删除记录。根据indexcode可以准确定位到三张表上。
		 */
		{
			System.out.println("===正确操作表： 1,6,5==");
			NativeQuery nq=db.createNativeQuery("delete Device where indexcode in (:codes)").withRouting();;
			nq.setParameter("codes", new String[]{"6000123","567232","110000"});
			nq.executeUpdate();
		}
		/**
		 * 删除记录，indexCode上的大于和小于条件以及OR关系勾勒出了这次查询的表,将会是DEVICE_2,DEVICE_3,DEVICE_4、DEVICE_7、DEVICE_8。(5张表)
		 */
		{
			System.out.println("===正确操作表： 2,3,4,5,7,8==");
			NativeQuery nq=db.createNativeQuery("delete Device where indexcode >'200000' and indexcode<'5' or indexcode >'700000' and indexcode <'8'").withRouting();;
			nq.executeUpdate();
		}
    }
~~~

上述代码也在教程工程中，可以运行一下。归纳特性如下：

1. 在NativeQuery中使用路由功能，开发者也基本无需关心路由实现细节，框架基本都自动完成。
2. 框架可以识别利用 >、<、 between、in、not in、like等一切有关的条件来缩小查询范围。

### 10.3.12.  使用SQL语句进行查询时的路由

和上文一样，在使用SQL语句进行查询时，如何路由也无需开发者关心。还是那句话，复杂的逻辑都封在框架内，用户像平常那样操作数据库就好。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\Case2.java

~~~java
		/**
		 * 查询，将查询所有表，跨库排序
		 */
		{
			System.out.println("查询，所有表，跨库排序");
			String sql="select t.* from device t where createDate is not null order by createDate";
			NativeQuery<Device> query=db.createNativeQuery(sql,Device.class).withRouting();;
			long total=query.getResultCount();
			System.out.println("预计查询结果总数Count:"+ total);
			List<Device> devices=query.getResultList();
			assertEquals(total, devices.size());
			int n=0;
			for(Device d: devices){
				System.out.println(d);
				if(n++>10)break;
			}
		}
		/**
		 * 查询两个条件时，
		 */
		{
			System.out.println("======查询，Like条件，后一个条件无参数被省略，正确操作表 3==");
			String sql="select t.* from device t where createDate is not null and (t.indexcode like '3%' or t.indexcode in (:codes)) order by createDate";
			NativeQuery<Device> query=db.createNativeQuery(sql,Device.class).withRouting();;
			long total=query.getResultCount();
			System.out.println("预期查询总数Count:"+ total);
			int count=0;
			//使用迭代器方式查询
			ResultIterator<Device> devices=query.getResultIterator();
			for (;devices.hasNext();) {
				System.out.println(devices.next());
				count++;
			}
			devices.close();
			assertEquals(count, total);
			
			query.setParameter("codes", new String[]{"123456","8823478","98765"});
			System.out.println("======查询，Like条件 OR IN条件，正确操作表:1,3,8,9==");
			total=query.getResultCount();
			System.out.println("预期查询总数Count:"+ total);
			List<Device> rst=query.getResultList();
			assertEquals(total, rst.size());
			int n=0;
			for(Device d: rst){
				System.out.println(d);
				if(n++>10)break;
			}
		}
		
		/**
		 * 查询记录、如果SQL语句中带有Group条件...
		 * 跨库条件下的Group实现尤为复杂
		 */
		{
			System.out.println("查询——分组查询，正确操作表：4,1,6");
			String sql="select type,count(*) as count,max(indexcode) max_id from Device tx where indexcode like '4%' or indexcode like '1123%' or indexcode like '6%' group by type ";
			NativeQuery<Map> query=db.createNativeQuery(sql,Map.class).withRouting();;
			System.out.println("预期查询总数Count:"+ query.getResultCount());
			List<Map> devices=query.getResultList();
			for (Map ss : devices) {
				System.out.println(ss);
			}
		}
		
		/**
		 * 查询,SQL语句中带有distinct条件
		 */
		{
			String sql="select distinct type from device";
			NativeQuery<String> query=db.createNativeQuery(sql,String.class).withRouting();;
			long total=query.getResultCount();
			System.out.println("预期查询总数Count:"+ total);
			List<String> devices=query.getResultList();
			for (String ss : devices) {
				System.out.println(ss);
			}
			assertEquals(6, total);
			assertEquals(6, devices.size());
			
		}

~~~

需要指出的是，跨库场景下如果使用了group语句，那么count数将不能直接从数据库得出，而是需要在内存中合并计算后才能得出。从性能上讲多少得不偿失，因此这种操作场景并不建议出现。

## 10.4.  分库规则加载器

EF-ORM中，分表分库规则默认是用Annotation配置在类上的。这可能引起一些不爱用Annotation开发者的不适。EF-ORM中其实默认还支持从json文件中读取分表分库规则。除此之外，您还可以自行编写分库分表规则的加载器，覆盖框架默认的分库分表规则加载器。

如果要使用json方式配置分库分表规则，可以在src/main/resources/目录下添加json文件，文件名为类的全限定名。后缀为json。例如——

org.easyframe.tutorial.lessona.entity.OperateLog.json

~~~json
{"key" : [{
		"field" : "dateField",
		"length" : 2,
		"function" : "MONTH"
	}, {
		"field" : "dbkey",
		"length" : 0,
		"isDbName" : true
	}]
}
~~~

即前面提到的@PartitionTable和@PartitionKey的各个字段值。使用json方式定义的分库分表规则将优先于类的Annotation加载。因此当两边都配置了分库分表规则时，json文件中定义的分库分表规则将会覆盖位于类Annotation上的分库分表规则。

 如果您需要使用自行编写的分库分表规则的加载器。您需要编写一个类，实现jef.database.meta.PartitionStrategyLoader接口。然后在jef,properties中配置

~~~properties
partition.strategy.loader=自定义加载器类名
~~~

使用上述方法后，启动框架时就会从开发者指定的加载器中加载分库分表规则。
