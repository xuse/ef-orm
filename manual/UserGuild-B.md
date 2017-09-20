

# 9.    事务控制与批操作

## 9.1. 编程式事务控制

​	本节描述EF-ORM的原生事务接口，不讲述在Spring下的声明式事务。声明式事务请参阅第11章《与Spring集成》。

 	在EF-ORM中，事务操作是在一个名为Transaction的类上进行的。Transaction和DbClient上都可以操作数据库。它们的关系如下图所示：

​				 ![9-9.1-1](images\9-9.1-1.png)



 							图9-1 DbClient和Transaction的关系

​	所有DML类型的数据库操作，都是由DbClient和Transaction的公共基类Session提供的。Transaction类上不能执行DDL操作，但提供了commit()和rollback()方法，用于提交或回滚事务。

DbClient上没有提交或回滚操作，但允许进行DDL操作。

​	这个设计是基于这样的一种模型：用户可以同时操作多个Session。一些Session是有事务控制的(Transaction)，可以回滚或者提交。一些Session则是非事务的、会auto-commit的 (DbClient)，用户无论执行DML还是DDL，都会被立刻提交。事实上，在EF-ORM内部实现中，这也是通过为用户分配不同的连接来实现的。

​	简单来说，DbClient是没有事务状态的Session(Non-Transactional Session)。Transaction则是有事务状态的Session(Transactional Session)。

​	从DbClient上，开启一个新的事务Session，可以使用 startTransaction()方法。

~~~java
Transaction session = db.startTransaction();    //创建一个新的事务，类似于H框架的openSession()方法
~~~

​	从Transaction对象上，也可以得到初始的非事务Session (即DbClient)。

~~~java
DbClient db=session.getNoTransactionSession();   //得到非事务Session。
~~~

下面的代码清单例子演示了数据库事务操作。

```java
DbClient db=new DbClient()
Transaction transaction=db.startTransaction();   、
try{
	transaction.insert(entity1);
	transaction.insert(entity2);
	transaction.insert(entity3);  
	db.insert(entity4);		//这个插入操作和上面三条记录不一样，是单独提交的
	transaction.commit();       //提交上面三个实体的插入
}catch(SQLException e){
	transaction.rollback();      //回滚
}finally{
   transaction.close();
}
```

​	上面的方式，即相当于操作编程式事务，您可以在一个方法中同时操作多个事务。事务之间可以保持互相隔离。（隔离级别取决于数据库设置）。

 	在现在大部分企业级开发项目中，基于AOP的声明式事务已经代替了编程式事务，EF-ORM也同样提供了JPA的事务接口。利用Spring Framework也提供了对JPA的事务支持可以实现AOP式的声明式事务。

​	因此在实际的企业级开发中事务管理支持事实上都是统一使用Spring Framework的事务模型的，Spring事务管理模型支持的四种数据库隔离级别和七种事务传播行为都可以在EF-ORM上实现。（标准JPA只支持六种传播行为）相关内容请参阅11节《与Spring集成》。



## 9.2. 批操作

​	无论在任何场合下，相同数据结构的多条数据操作性能问题总是会引起人们的关注。解决SQL性能的重要手段就是批量操作，这一操作经常被用来进行大量数据的插入、更新、删除。

​	批操作是改善数据库写入性能最重要的手段。批操作的本质，简单来说就是------*单句SQL，多组参数，一次执行*

​	这三个特定决定了批操作的特点和限制

* 单句SQL：一个批次当中所有数据的写入都必须共用同一句SQL，一个批次当中不能有不同的SQL语句。

* 多组参数：批操作中的每个元素都被转换为和公用SQL语句相匹配的参数。显然，批次要有合适大小，性能才会提高。如果一批请求只有三五组参数，性能优势就不明显了。

* 一次执行：前面的NativeQuery中，一个Query也是能搭配多组参数反复运行的。批操作比普通绑定变量操作更快的原因是，批操作不是逐条执行的，而是一次性发送所有参数到数据库服务器，数据库也一次性的返回结果。通信次数的减少是批操作性能提升的关键。

  当然一次执行也衍生了另一个特点，即一批数据中任意一组参数操作失败，那么整批数据操作都失败。这是要引起注意的。



### 9.2.1. 基本用法

​	Batch模式的最基本用法，是使用Session中的batchXXX系列的方法

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson9\BatchOperate.java

~~~java
@Test
public void testBatchOperates() throws SQLException{
	List<Person> persons=new ArrayList<Person>();
	for(int i=0;i<5;i++){
		Person p=new Person();
		RandomData.fill(p); //填充一些随机值
		persons.add(p);
	}
		
	{ //批量插入
		db.batchInsert(persons);
			
		//批量操作下，从数据库获得的自增键值依然会写回到对象中。
		for(int i=0;i<5;i++){
			assertEquals(Integer.valueOf(i+1), persons.get(i).getId());
		}
	}
  
	{ //批量更新(按主键)
		for(int i=0;i<5;i++){
			persons.get(i).setGender(i % 2==0?'M':'F');
		}
		db.batchUpdate(persons);
	}
  
	{//批量更新 (按模板)
		for(int i=0;i<5;i++){
			Person p=persons.get(i);
			p.getQuery().clearQuery();
			p.getQuery().addCondition(Person.Field.name,p.getName());
			p.setName("第"+(i+1)+"人");
		}
		db.batchUpdate(persons);
	}
  
	{//按主键批量删除记录
		db.batchDeleteByPrimaryKey(persons);
	}
}
~~~

​	上例中，我们首先创建了5个Person对象，用批操作一次性插入到数据表。在单表操作中，我们了解到自增键值会在插入执行后立刻回写到对象中。在批操作中，对于自增主键的生成获取特性仍然有效。

 	然后我们对每个元素进行了修改，执行批量更新操作，可以发现五个元素在数据上都按主键为条件执行了更新操作。这一规则和之前的单表操作一致——在没有任何查询条件时，使用主键作为操作的where条件。

 	显然，一个自由的update语句并不一定是按主键操作的。因此例子接下来的演示了按自定义的条件去执行update。接下来，指定了name作为更新的条件。这样的用法产生的SQL如下------

~~~sql
update T_PERSON set PERSON_NAME = ? where PERSON_NAME=? 
~~~

​	其效果是按“姓名”去更新”姓名”。我们已经知道，条件和对象是在不同的位置上的，因此这样的SQL语句是可以在EF-ORM中使用的。

​	最后我们将全部对象传入，并按主键批量删除所有记录。

​	上述例子中，直接使用了Session对象中一个简单的批操作API。Session对象中，和批操作相关的API有-----

| 方法                                       | 用途                                       |
| ---------------------------------------- | ---------------------------------------- |
| Session.batchDelete(List<T>)             | 按传入的操作，在数据库中进行批量删除。                      |
| Session.batchDelete(List<T>,  Boolean)   | 按传入的操作，在数据库中进行批量删除。指定是否分组（为了支持分库分表）      |
| Session.batchDeleteByPrimaryKey(List<T>) | 按传入的对象的主键，在数据库中进行批量删除。                   |
| Session.batchInsert(List<T>)             | 将传入的实体批量插入到数据库。                          |
| Session.batchInsert(List<T>,Boolean)     | 将传入的实体批量插入到数据库。指定是否分组（为了支持分库分表）          |
| Session.batchInsertDynamic(List<T>,  Boolean) | 将传入的实体批量插入到数据库（dynamic=true）。指定是否分组（为了支持分库分表） |
| Session.batchUpdate(List<T>)             | 按传入的操作，在数据库中进行批量更新。                      |
| Session.batchUpdate(List<T>,  Boolean)   | 按传入的操作，在数据库中进行批量更新。指定是否分组（为了支持分库分表）      |
| Session.startBatchDelete(T,  String)     | 高级接口。获得批操作的Batch对象。Batch对象可以让开发者更多的干预和调整batch操作的参数。 |
| Session.startBatchInsert(T,  String, boolean) | 高级接口。获得批操作的Batch对象。Batch对象可以让开发者更多的干预和调整batch操作的参数。 |
| Session.startBatchUpdate(T,  String)     | 高级接口。获得批操作的Batch对象。Batch对象可以让开发者更多的干预和调整batch操作的参数。 |

 	上述方法中dynamic是指动态插入。简单来说，就是在形成INSERT语句时，那些未赋值的字段不出现在SQL语句中。（从而可以使用数据库的DEFAULT值）。

​	值得解释的另一个参数是”指定是否分组“，大家回想前面的批操作限制，必须是单句SQL。这意味着当使用分库分表后，不同表的插入和更新操作不能合并为一句SQL执行。

为了满足这种场景，EF-ORM采用的办法是，将所有在同一张表上执行的操作合并成一个批。而位于不同表上的操作还是区分开，分次操作。显然，分析每个对象操作所影响的表是复杂的计算，会带来一定的开销。因此，在业务开发人员能保证所有操作请求位于同一张表上时可以传入false，禁止Batch对请求重新分组，从而带来更高效的批量操作。

​	这一示例详见第10章。

​	最后提一下，目前批操作全部都是单表操作。不支持级联特性。

### 9.2.2. 不仅仅是操作实体

​	正如前面单表Criteria API中介绍的，批操作并非只能按主键update/delete数据。事实上在update和delete批操作时，传入的每个Entity都是一个完整的单表操作，其内嵌的Query对象可以描述一个通用的查询，而不是仅仅针对单条记录的。

​	从JDBC角度看，批操作是将一条SQL语句用多组参数执行。因此实际上批操作的每个元素都应该是一个SQL语句，而不仅仅是对一个实体（单条记录）的操作。

我们看下面这个批量update的例子

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson9\BatchOperate.java

~~~java
@Test
public void testBatchUpdate() throws SQLException{
	doInsert(5);
	Person p1=new Person();
	p1.getQuery().addCondition(QB.matchAny(Person.Field.name, "a"));
	p1.prepareUpdate(Person.Field.created,db.func(Func.current_timestamp));
		
	Person p2=QB.create(Person.class).
			addCondition(QB.matchAny(Person.Field.name, "b")).getInstance();
		
	Person p3=QB.create(Person.class).
			addCondition(QB.matchAny(Person.Field.name, "cc")).getInstance();
		
	db.batchUpdate(Arrays.asList(p1,p2,p3));
}
~~~

这段代码将引起数据库执行以下的SQL

~~~sql
update T_PERSON
   set CREATED = current_timestamp
where PERSON_NAME like ? escape '/'
~~~

​	然后有三组参数，使用这条SQL语句来运行。

​	该SQL是由整个Batch的第一个元素决定的。  对于整个Batch来说，第一个操作请求决定了整个Batch要执行什么样的SQL语句。后面的操作对象**仅仅起到了传入参数的作用**。也就是说，在后面的对象中，无论增加什么样的其他条件，都不会影响整个批次的运行SQL。

​	上面的示例运行结果在日志中是这样显示的------

~~~
Update Batch executed:3/on 2 record(s)	 Time cost([ParseSQL]:2199us, [DbAccess]:38ms)
~~~

​	这里清晰的指出了执行效果——有3组执行参数，共计修改了2行记录。

 	因此，如果说9.2.1节中体现的是传统ORM中批量操作实体行为，那么本节中进一步澄清了批操作的本质：

>*以第一个传入的操作请求为模板，形成对应的SQL语句。*
>
>*从所有操作请求中获取该SQL需要的操作参数，整批一起执行。*

​	这种处理概念，使得我们能在Batch中处理多次SQL操作，而不仅仅是操作多个实体。

 	用另一个API来执行批操作可以让我们更明显的看出这一点

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson9\BatchOperate.java

~~~java
@Test
public void testBatchUpdate2() throws SQLException {
	doInsert(5);
	Person query = new Person();
	query.getQuery().addCondition(Person.Field.created, Operator.GREAT, DateUtils.getDate(2000, 1, 1));
		
	List<Person> persons = db.select(query);
	for (Person person : persons) {
		person.setCreated(new Date());
	}
	Batch<Person> batch = db.startBatchUpdate(persons.get(0),null);

	batch.execute(persons);
}
~~~

​	这个例子中，我们查出所有创建日期在2000-1-1日之后的Person对象，将其创建日期设置为今天。然后，startBatchUpdate()方法就是传入操作模板，形成Batch操作任务。之后将所有的person对象作为参数传入并执行。

### 9.2.3. 性能的极限------Extreme模式

​	使用上面所说的Batch模式，并没有达到批量操作的最大性能，为了满足大量数据导入或导出（如ETL）等特殊场景，EF-ORM还提供了Extreme模式，批量插入或更新记录。

​	批量插入记录中，有哪些操作影响了性能的最大化呢？

1. Sequence获取访问。为了让用户能在完成插入操作后的对象中获得到其Sequence ID值，框架采用先访问一次数据库获得ID值，再执行插入的做法。批操作下，这一动作非常损耗性能。
2. 自增值回写，在一些自带自增列功能的数据库上，框架要将数据库端生成的自增列值写回到对象中。
3. 各个数据库往往会设计一些特殊的语法，来满足高性能写入等场合的需要。

   ​Extreme模式就是针对上述情况所设计的，Extreme模式下，会放弃主键回写等一些不必要的功能，同时会启用数据库方言中的特殊语法，从而实现最大效率的数据插入和更新。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson9\BatchOperate.java

~~~java
private void doExtremeInsert(int max) throws SQLException {
	List<Person> persons = new ArrayList<Person>(max);
	for (int i = 0; i < max; i++) {
		Person p = new Person();
		RandomData.fill(p); // 填充一些随机值
		persons.add(p);
	}
	db.extremeInsert(persons,false);
}
~~~

​	Extreme模式的使用很简单，使用session类中的 extremeInsert 和 extremeUpdate方法即可。

​	对用户来说，就这么简单，但如果您想进一步了解什么是Extreme模式，可以继续看下去。

​	在Oracle上，Extreme模式的数据插入性能改善最为明显。因为在Oracle的Extreme模式下，INSERT语句中会直接引用Sequence。如

~~~sql
INSERT INTO FOO (ID, NAME, REMARK) VALUES (S_FOO.NEXTVAL, ?, ?)
~~~

​	从而免去单独访问Sequence的开销。除此之外，当启用Extreme模式后，在Oracle数据库上会增加SQL Hint，变为

~~~sql
INSERT /*+ APPEND */ INTO FOO (ID, NAME, REMARK) VALUES (S_FOO.NEXTVAL, ?, ?)
~~~

​	目的是进一步提升插入效率。

​	在别的数据库上，Extreme模式的性能差距会小一些，由于省去了ID回写功能，性能还是会有一定改善。extremeUpdate也是如此。后续的其他一些性能优化手段也会继续添加到Extreme模式上。

​	EF-ORM的Extereme模式插入数据的速度是非常快的。实际测试表明：在网络环境较好的Oracle数据库上，针对7~8字段（无LOB）的表。Extreme模式可以达到插入8~10万条/秒。在一般的百兆局域网环境，每秒插入记录数量也可以达到 5~6万/秒。Oracle下这一数值是普通Batch模式的10倍以上；是普通单条插入的千倍以上！

​	在其他数据库上，Extreme模式的性能也会有一定提高。



# 10. 分库分表与数据路由

## 10.1. 数据分片概述

### 10.1.1. 什么是数据分片

​	 一个生产系统总会经历一个业务量由小变大的过程，可扩展性成为了考量系统高可用性的一个重要衡量指标。在大数据时代，数据规模的支持成为衡量系统能力的重要指标。

​	在这个时代，几乎data sharding成为每个系统架构时需要考虑的重要问题。实时上，远在互联网和“大数据”概念时代之前，银行和电信业早已在处理数十亿数百亿级的数据，并且采用了传统的分区、分表、分库等手段，因此数据分片并不是什么新鲜概念。Oracle 9以前就提供分区表的功能，并且提供管理了TB以上单表的生产案例。在后来若干年中，支持数据分区已经成为各种商业数据库的标配特性。

​	数据库软件提供的数据分片方案，一般都是分区方案，根据分区条件，可以让数据存储在不同的表空间中。在DBA配置完分区规则后，整个分区的动作对应用程序是透明的。开发人员可以用普通的SQL语句来操作分区表，无须考虑分区的规则和数据位置。

​	商业数据库提供的原生数据分区方案，有不可替代的优越性——使用简单，性能较好，而且数据还会提供进一步的优化特性（如分区索引等）。最大的特点是，数据库原生的分区方案基本都能做到**对开发者透明**。

​	尽管数据库的供应商们已经为我们设计了众多的数据分片sharding方式；尽管在大数据时代各种分布式数据存储系统都提供了海量的存储规模。然而在大量的系统中，基于分库分表的RDBMS仍然占用重要的地位。在对实时性和一致性有高度要求的系统中，分库分表依然是处理海量结构化数据最重要的手段，并且为此也开发出了众多的中间框架。

​	为什么系统的开发者们不使用数据库软件本身提供分区功能，而要自行开发分库分表功能？一般有以下的理由------

*  使用数据库分区功能对数据库DBA的人力和技能要求。分区表后期的维护复杂性。
*  分成不同的表，有利于数据的归档、清除、备份等维护性行为。例如对交易记录和日志等，按历史表归档既简单又符合业务需求。


*  需要用多台数据库服务器的场合。RDBMS一般只提供分区功能，无法将分片到不同的实例上。


*  成本因素。不愿意花费购买商业数据库的分区特性或相关服务。


*  还是成本因素。使用了一些不支持分区功能的低成本数据库。


*  希望保留跨数据库的移植性。用户希望系统将来能移植到其他数据库软件上，无论这些软件是否支持分区功能。


*  习惯和守旧。沿用过去的做法，不愿意学习和接受数据库软件的新功能。

   ​对于上面的几种因素，除了最后一种以外，我们都有在应用中自行实现数据分片的必要。分库分表依然是数据库分片中，成本最低，适应性最广，对开发者要求最低的方案。因此这种做法到今天为止也有着广泛的市场。但无论任何用户自行开发的或是框架提供的分表方案，都对操作有着诸多的限制，（例如不支持Join等），因此不能完全做到对开发者透明。

### 10.1.2. EF-ORM支持的分片功能

​	EF-ORM的分库分表功能，能够通过配置数据分片规则，在操作时自动定位到数据所在的库和表上去。

​	EF-ORM支持数据的水平拆分和垂直拆分，在水平拆分维度上能同时支持多库和多表。

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

​	在水平分片的情况下，数据分库分表以后对操作有着巨大的限制——不支持分区表和其他表关联（Join）。因此一旦设置成分区表，这张表上只能执行单表操作了。跨分区查询、排序等特性也是分库分表后的难点。

​	EF-ORM的功能特色

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

​	上述“有数据量限制”的特性，是指查询返回的结果数量不能过大。（并非指数据库中的记录数量）。因为上述操作都是不得不进行内存操作的功能，所以不建议在太大的结果集上进行操作。一般来说，OLTP类应用单笔业务影响的记录行数不超过1000条，因此EF-ORM的这种支持方案是能满足大部分需求的。但是，不建议在OLAP类应用中尝试用EF-ORM来解决跨库数据合并的问题。

### 10.1.3. 和具有同类功能的产品比较

​	这里比较笔者有了解的Hibernate Shards和TDDL两个框架， Ibatis-Sharding没用过，有机会再补充。下面的各种论点均为采集互联网资料了解而得，可能有错漏、陈旧之处，请读者包含。

* **Hibernate Shards / HiveDB**


​	Hibernate Shards是Google的工程师贡献的开源代码，集成到Hibernate中使用后可以支持数据水平拆分。HiveDB是基于HibernateShard 用来横向切分mysql数据库的开源框架。

​	这个解决方案目前通过 Criteria 接口的实现对 聚合提供了较好的支持， 因为 Criteria 以API接口指定了 Projection 操作，逻辑相对简单。

​	EF-ORM中的shards功能和上述框架比较类似，

1. 还有很多hibernate的API没有实现
2. 不支持cross-shard的对象关系，比如A、B之间存在关联关系，而A、B位于不同的shard中。hibernate shards提供了CrossShardRelationshipDetectingInterceptor，以hibernate Interceptor的方式来检测cross-shard的对象关系问题，但这个拦截器有一定的开销（比如需要查映射的元数据、有可能过早的触发延迟加载行为等），可以用于测试环境来检测cross-shard关系的问题
3. hibernate shards本身不支持分布式事务，若要使用分布式事务需要采用其他解决方案
4. hql、criteria存在不少限制，相比于hql，criteria支持的特性更多一些
5. Session或者SessionFactory上面有状态的拦截器，在shard环境下面会存在一些问题。拿session来说，在hibernate中拦截器是对单个session上的多次sql执行事件进行拦截，而在shard情况下hibernate shards的ShardedSession会对应每个shard建立一个session，这时拦截器就是跨多个session了，因此hibernate shards要求有状态的拦截器必须通过实现StatefulInterceptorFactory来提供新的实例。如果拦截器需要使用到目标shard的session，则必须实现hibernate shards的RequiresSession接口 

EF相较于这两个框架，其优势是------

1.    提供了较好的分区范围分析算法。而HibernateShard依赖于用户自行实现分区策略的计算。而当分区条件含糊时，用户很难编写出精确的路由算法。

2.    针对分表和分库的情况加以区分，在同个数据库上的时候能利用SQL操作实现排序和聚合计算，对服务器的CPU和内存压力较小。而HibernateShard不区分这两种情况。

3.    
         优化的多库排序： 在多库排序时，能分析分表规则，当分表条件和排序条件一致时，直接将各个结果集按排序条件拼合。免去了排序的性能开销。
         在必须重排序时，利用每个库各自的顺序，使用了内存占用较小的排序算法。

4.    EF-ORM中分区操作对用户基本透明，无需移植。而从hienrate移植到HibernateShard时的部分接口类需要调整。

5.    hibernate shards没有对hql进行解析,因此hql中的count、sum、distinct等这样的操作还无法支持。而EF-ORM除了API层面外，对于传入的SQL语句也支持分库分表。

6.    配置更简单，业务侵入性更小，对开发人员透明度更高。EF-ORM中除了Entity上需要加入少量注解外，开发者无需关心任何分库分表相关的工作，也无需编写任何代码。hibernate shards等则需要开发者实现若干策略。

7.    EF-ORM的主键生成算法也做到对用户透明，在支持Sequence下的数据库中，会使用Sequence生成自增序列。其他数据库上会使用具有一定步长的表来生成Sequence。

8.    Hibernate shards不支持按时间或实际使用时建表。

      ​



EF-ORM其功能基本包含了Hibernate Shards / HiveDB。但还有一定不足------

1.    没有提供并行查询策略。目前多次查询的场合都是顺序操作。

2.    virtual shards，虚拟分片的作用是将数据分得更细，然后多个虚拟片公用一个实际的表。这样的好处是virtualshards和实际shards之间的映射关系还可以后续调整。

3.    Hibernate shards的诞生和发展周期更长，功能完善程度更高，包括相同维度切分的实体之间的级联关系等也都做了处理。

      ​



EF-ORM和上述框架共有的不足之处是------

1.     两者都绑定特定的ORM实现，前者绑定Hibernate，后者是EF-ORM的一部分。



* **Alibaba TDDL**

​	TDDL（Taobao DistributedData Layer）顾名思义，是淘宝网的分布式数据框架。它和众多的连接池一样，是一个封装为DataSource的中间框架，也能处理SQL语句的分析和路由。

​	应该说，TDDL和本框架之间关注内容和所属的层次是不同的，TDDL的处理是更为底层的：

1. 是在JDBC规范下，以DataSource方式进行封装的。TDDL是对SQL语句进行分析和处理的，而不是Criteria对象上进行处理的。这使得TDDL能拦截一切数据库操作，但也使得复杂场景下的分库分表路由支持变得困难。路由条件的传递除了解析SQL之外，TDDL中还为此开了不少“后门”来传递路由条件。
2. 数据路由只是TDDL的一部分功能，TDDL本身还提供了SQL重试、负载均衡等一系列提高可用性的模块。正如官方材料中说的，你可以使用TDDL的分库分表功能，也可以不使用。而仅仅使用其底层封装的高可用性模块。

TDDL目前最大问题是，其“开源程度”不高，所谓的“开源程度”问题，主要是指

1. Github上，该项目只有2012年更新过，仅有4次代码提交。而阿里内部，该软件的版本则一直在升级改进，但并未对外公开。
2. TDDL升级改进的需求几乎均来自阿里内部，其开发团队几乎未对外部用户和开源社区做出什么响应。
3. TDDL中最关键的分库分表层的模块代码一直没有在开源项目中出现。遭到不少网友的疑问。
4. 几乎没有社区支持，官方也没有文档支持。仅有的少量文档都是阿里内部流出，其对应的版本和代码不明。

TDDL 3.3.x的代码最终还是找到了，我也读过了，感觉官方开发团队为了解决JDBC庞大的API支持投注的精力十分可观，因此对分库分表等支持上多少不够专注。

​	从我的感觉来说，TDDL最实用的地方恰恰不是其分库分表功能，而是——

1. 数据库主备和动态切换； 
2. 带权重的读写分离； 
3. 单线程读重试； 
4. 集中式数据源信息管理和动态变更； 
5. 可分析的日志打印 , 日志流控，动态变更；

这些提高应用可用性、可维护性、伸缩性方面的特性。

​	由于TDDL的功能和EF-ORM的功能仅仅有少量是重叠的（分库分表），因此我们仅能对重叠部分进行一些功能的比较。

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

​	上述是从目前网上了解到的对开源版本的TDDL的一些对比。从流出的TDDL 3.3.x版本代码来看，上述许多功能应该是是已经支持的。但具体情况因缺少支持文档不明。

 

## 10.2. 分库分表规则配置

​	本章，我们开始了解EF-ORM的分库分表功能使用。

### 10.2.1. 水平拆分------分表

​	首先，我们需要了解分库分表的配置。默认情况下，分库分表规则可以通过Entity类上标注的注解来描述。

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

​	每个@PartitionKey表示一个分表条件字段。上例中的配置表示——取对象的createDate字段，取其年+月（yyyyMM格式），作为表名的后缀。

​	对于上面这样配置的日志表。2014-1-31日的日志就会存放在 operatelog\_201401表中，2014-2-1日的日志就会存放在operatelog_201402表中。

​	如果插入前createDate字段没有值，那么意味着无法决定记录要插到哪张表中，此时会抛出异常。

​	当分库条件不明确时，路由计算器会尝试计算一个范围来覆盖记录所在的表中，因此分库分表的条件含糊可能引起路由返回结果为多张表、甚至多个数据库。

​	对于几种操作情形，路由返回结果为多张表时，EF-ORM将会采取不同的策略来执行。

| 操作     | 单库单表 | 单库多表                                     | 多库多表                                     |
| ------ | ---- | ---------------------------------------- | ---------------------------------------- |
| insert | 支持   | 抛出异常                                     | 抛出异常                                     |
| update | 支持   | 在所有表上执行update                            | 在所有表上执行update                            |
| delete | 支持   | 在所有表上执行delete                            | 在所有表上执行delete                            |
| select | 支持   | 尝试转换为union all语句。配置一定结果集处理。              | 对每个库上的多表采用 union all。  对多个库的结果集进行混合计算。   |
| count  | 支持   | 如果不涉及distinct、group by则在所有表上执行count查询，并将结果累加。  如果涉及distinct、group by则查询结果并计算将SQL修改为在union all外部嵌套distinct和group by，最后计算总数。 | 如果不涉及distinct、group by则在所有表上执行count查询，并将结果累加。  如果涉及distinct、group by则查询结果并汇总混合。最后得到结果的数量。（数量有限制） |

 

​	我们再看下面一个例子，有一张表，其中有两个字段name和created，我们希望以这两个字段为关键字进行分表操作。我们可以这样定义。

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

对于这种配置，其分表条件有两个。如果用以下代码插入记录到数据库------

~~~java
PeopleTable entity=new PeopleTable();
entity.setId(1);
entity.setCreated(DateUtils.getDate(2011, 2, 1));
entity.setName("ZHANG");
db.insert(entity);
~~~

​	实际上该记录会写入到表 *PEOPLE_TABLE_ZHA02*中。其中ZHA是第一个分表条件产生的，02是第二个分表条件产生的。多个分表条件之间默认没有任何分隔符，如需要可以用keySeparator参数配置。

​	当查询数据时：

~~~java
PeopleTable entity=new PeopleTable();
entity.getQuery().addCondition(PeopleTable.Field.name, "WANG");
entity.getQuery().addCondition(PeopleTable.Field.created, new Date());//假设现在是3月份
List<PeopleTable> data= db.select(entity);
~~~

​	查询时的表名也是根据条件自动计算的，实际上会到一张名为 PEOPLE_TABLE_WAN03的表中查询。

​	从上面的配置我们可以了解到通过@PartitionTable注解，我们可以配置一些规则，这些规则将在表名后面添加若干后缀，作为一张分表。

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

​	上述配置可以描述对象的分表规则。一旦配置了分表规则后，就可以像操作正常的实体一样进行CRUD操作。框架会自动解析对象中的数值和Query中的条件，来判断需要操作哪些分表。

 ### 10.2.2. 水平拆分------分库

​	上面的配置除了可以实现分表配置外，还可以配置出水平分库的实体。下面例举了一个既分库又分表的实体。

​	customer表可以按客户编号(customerNo)，按3取模。然后补到两位作为数据库名。我们可以推测出根据customerNo的不同，记录会分别落到D0, D1,D2这样三个名称的数据库上。

​	同时，按照createDate取年+月进行分表。

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

​	可能会有这样的疑问：我在开发的时候怎么能知道部署的时候会有几个数据库呢？怎么能确定部署时的数据库叫什么名字呢？在这里配置的数据库名其实也是一种虚拟数据源。实际使用时，可以在jef.properties中配置映射关系。

~~~properties
db.datasource.mapping=D0:mysql01,D1:mysql02,D2:mysql03
~~~

​	上面的配置，可以将开发时指定的虚拟数据源D0,D1,D2映射到实际部署环境的mysql01,mysql02,mysql03三个数据库上。

 ### 10.2.3. 垂直拆分

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

​	一旦进行了上述配置则Person2表将始终保存到数据源datasource2上。前面已经说过，在实际部署环境中，可以通过db.datasource.mapping参数将数据源名称映射为部署环境的真实数据库名。



 ## 10.3. 分库分表后的操作和行为

### 10.3.1. 前提与原则

 分库分表以后，有以下几个最基本的限制和规则

1. 不支持多表查询。所有的单表查询Query API都能支持分库分表。所有的多表查询则都会**忽略**分库分表的配置规则。

       在NativeQuery中运行单表查询的SQL语句，都能支持分库分表规则。而多表的SQL语句则都不支持。

2. 原生SQL操作方式由于对SQL是不作解析的，因此也就不支持分库分表。
3. 目前分库分表功能支持多库多表下的常用聚合函数、Distinct、Order by、Start with... Connect by、分页。 
4. 目前分库分表功能不能支持分析函数、不支持聚合函数中的avg函数。
5. 使用API方式，分库分表前后的数据库操作方式没有任何差异。
6. 使用NativeQuery方式，要启用分库分表功能。需要调用NativeQuery的withRouting()方法(见后文示例)。

在对分库分表的操作有了上述认识后，可以看下面的特性和示例。

### 10.3.2. 按需建表

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

​	但是在插入之前，表OPERATELOG_20100302还根本不存在。此时框架会自动创建这张表，并将记录插入到这张表中。

### 10.3.3. 自增主键生成行为

​	我们在仔细看上面的例子还会发现更多的东西。比如，在对象中，我们配置了@GeneratedValue(strategy=GenerationType.*IDENTITY*)。正常情况下将会使用数据库表列的自增功能来生成主键。但是多表下问题就来了，不同的表可能会生成重复的ID。因此我们可以看到框架自动将主键生成的行为变更了，在Derby上，采用TABLE的方式来生成自增主键值……打印出的SQL日志如下------

~~~sql
SELECT V FROM global_sequences WHERE T='OPERATELOG_SEQ' | [derby:DB@1]
UPDATE global_sequences SET V=? WHERE V=? AND T='OPERATELOG_SEQ'  |[derby:DB@1]
(1):               [60]
(2):               [40]
~~~

​	显然到TABLE中逐个获取序号，效率是非常低下的，因此EF-ORM采用了以下两个手段来保证效率——

1. 一次获取多个序号，上例中，一次SQL操作直接将序号递增20。即一次获取20个序号。
2. CAS(ComapreAndSwap)操作。上例的UpdateSQL语句是支持并发的，一旦有多个SQL同时操作该表，能保证每个SQL都能正确获取序号值。

​	EF-ORM中，用于调节Sequence获取行为的参数还有不少。比如TABLE方式，可以每个表一个TABLE，也可以全局公用一个TABLE。TABLE下的Sequence等也都可以调节。详见附录一 配置参数一览。

### 10.3.4. 手动建表

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

预测的方法是------

对于取模类函数，将取模可能的值进行遍历。如按10取模，那么会预测到0~9的所有数字。

对于日期类的函数，以当前日期为界，创建前后一段时期内的表（这个时期范围可以通过jef.properties中的全局配置参数），如果配置下面这样，表示选取过去三个月和未来三个月。

~~~properties
partition.date.span=-3,3
~~~

​	要注意的是，上述参数不仅仅用于建表时表名的推测，也用于执行数据库操作时的分表范围推测。比如用户查询OperateLog数据时，没有指定时间范围，那么框架默认就查询距今前后三个月的表。不会再查询三个月之前的表。

​	上述方法都是手工创建一个分区表的所有分表。我们也可以手工指定分库分表条件，来创建某张特定的分表。

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

​	上述方法中，由于指定了分表条件，因此不会再像上例一样，在所有可能的数据库上创建分表，而是会在特定的datasource上创建和删除CUSTOMER\_201612、 DEVICE_1表。

### 10.3.5. 路由时过滤不存在的表

​	对于未明确指定分库分表条件的查询，框架会查询所有可能有此记录的表。这种“可能性”分析并不仅仅局限于函数和枚举操作，还包括对数据库现存的表的扫描。因此正常情况下，数据库中存在的表才会被查询到。数据库中不存在的表，将不会被查到。

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

​	前面提到的按需建表功能也好，此处的过滤不存在的表功能也好，都是对现有数据库中表是否存在进行判断。显然这个操作是较为耗时的，不可能每次计算路由时临时计算。因此，数据库中现存的表的情况，是被缓存下来的。

​	您可以关闭不存在表过滤的功能。通过jef.properties中配置

~~~properties
partition.filter.absent.tables=false   //关闭表过滤功能。
~~~

​	同时，分表数据缓存的有效期可以设置

~~~properties
db.partition.refresh=3600   //设置分表信息缓存的最大生存时间。
~~~

### 10.3.3. 如果路由为空

​	如果分表计算发现数据库中不存在符合条件的表，那么实际上就无需查询任何表，像下面这种情况。

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

​	上例中，本来按分表条件应该查询DEVICE_9表。但是该表不存在，因此路由模块会返回空。此时框架直接返回空列表，不作任何查询。

### 10.3.7. 垂直拆分的场景

​	垂直拆分就是将Entity用@BindDataSource绑定到另外一个数据库上去。实际使用的演示可以参考示例代码。

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

​	上例分为两部分。第一部分是基于API的数据库操作。第二部分是基于SQL语句的数据库操作。

​	运行上面的案例，可以发现基于垂直分区的配置，所有的数据库操作都在DB2上运行，而不是在默认的数据库上运行。

​	可以发现，在垂直拆分的情况下，API方式操作和以前没有任何变化。而使用SQL语句NativeQuery方式的时候，需要调用一下withRouting()方法。

### 10.3.8. 在分库分表条件基本确定的场景下

​	我们以customer表为例，看一下在customer表上执行增删改查操作的情况。Customer对象按customerNo除以3的余数，分配到三个数据库上。同时customer还按照记录的创建时间进行了分表。

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

​	上述代码中，展现了对customer对象的基本操作，包括插入，加载、更新、删除、计算总数等。对照日志中输出的SQL语句，可以发现当对象中createDate和customerNo两个字段均有值时，操作会准确的定位到一个库的一张表上。

​	但是在按customerNo查询时，由于缺少createDate这个分表条件，框架不得不在前后几个月的表中就近进行查询。而当不带任何条件查询总数时，框架在所有库上的多张表中进行了查询。

### 10.3.9. Batch模式和分库分表

​	分库分表对Batch模式也有一定的影响。由于分库分表后，不同库和表上的数据操作SQL语句是不一样的，因此框架必须对Batch中的对象进行分组，将同一张表里的记录划为一组，然后再对每组进行操作。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\Case2.java

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

​	上面的测试案例，演示了分库分表后的Batch操作。一般来说，用户也无需关心分库分表后Batch操作上的变化。框架会自动按照重新分组的方式进行batch操作。

 	但是也有特殊情况。分库分表路由计算毕竟是个一定耗时的操作。某些场景下，用户能在自己的业务中保证batch中的所有数据都是在一个表上的，此时对每条记录进行路由计算就显得多余了。这时就可以使用session中另一个batch操作的方法，即传入一个Boolean值，表示是否需要按路由重新分组。

~~~java
db.batchDelete(list,true); //需要对每个传入数据进行路由计算并重新分组

db.batchInsert(list,false); //不需要对每个传入数据进行路由计算

db.extremeInsert(list, true);//需要对每个传入数据进行路由计算并重新分组

db.extremeInsert(list, false);//不需要对每个传入数据进行路由计算
~~~

### 10.3.10.  当路由结果为多库多表时

​	那么，现在我们来看一看分库分表查询中最为复杂的情况。即路由条件不足，造成多库多表查询的情况。该案例较为复杂，也非常长，因此代码不全部贴出了。可以参见示例工程下的java代码。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessona\Case2.java

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
~~~

该案例归纳情况如下——

1. 无论有多复杂，在查询结果数量不大的情况下（OLTP系统的特点）。开发人员都无须关注分库分表做了什么。一切都由框架完成了，除非实在无法支持的情况框架会抛出异常。
2. 框架可以识别利用 >、<、 between、in、not in、like等一切有关的条件来缩小查询范围。
3. 框架可以对查询语句中的group、distinct、having、记录范围等因素识别并进行相应的处理。

​	总的来说，对于分库分表下的查询操作，开发人员应当自行确定一些操作限制。因为SQL语句的写法是无穷的，变化也是无穷的，而框架能支持的操作场景是有限的。这是所有具备分库分表功能的框架都面临的问题。

​	复杂到框架无法支持的多库查询，一般的SQL开发人员自己也很难编写出来正确的查询来。反过来说，一个熟练的DBA做不到的SQL查询，框架也很难做出来。多库多表下的查询功能都是在不停的完善的，对任何一个框架都是如此。如果有什么需要支持的合理场景，也请联系作者，可以考虑将您的想法改进到这个框架当中。

### 10.3.11. 使用SQL语句插入、更新和删除时的路由 

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

​	上述代码也在教程工程中，可以运行一下。归纳特性如下：

1. 在NativeQuery中使用路由功能，开发者也基本无需关心路由实现细节，框架基本都自动完成。
2. 框架可以识别利用 >、<、 between、in、not in、like等一切有关的条件来缩小查询范围。

### 10.3.12.  使用SQL语句进行查询时的路由

​	和上文一样，在使用SQL语句进行查询时，如何路由也无需开发者关心。还是那句话，复杂的逻辑都封在框架内，用户像平常那样操作数据库就好。

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

​	需要指出的是，跨库场景下如果使用了group语句，那么count数将不能直接从数据库得出，而是需要在内存中合并计算后才能得出。从性能上讲多少得不偿失，因此这种操作场景并不建议出现。

 

## 10.4. 分库规则加载器

​	EF-ORM中，分表分库规则默认是用Annotation配置在类上的。这可能引起一些不爱用Annotation开发者的不适。EF-ORM中其实默认还支持从json文件中读取分表分库规则。除此之外，您还可以自行编写分库分表规则的加载器，覆盖框架默认的分库分表规则加载器。

​	如果要使用json方式配置分库分表规则，可以在src/main/resources/目录下添加json文件，文件名为类的全限定名。后缀为json。例如------

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

​	即前面提到的@PartitionTable和@PartitionKey的各个字段值。使用json方式定义的分库分表规则将优先于类的Annotation加载。因此当两边都配置了分库分表规则时，json文件中定义的分库分表规则将会覆盖位于类Annotation上的分库分表规则。

 	如果您需要使用自行编写的分库分表规则的加载器。您需要编写一个类，实现jef.database.meta.PartitionStrategyLoader接口。然后在jef,properties中配置

~~~properties
partition.strategy.loader=自定义加载器类名
~~~

​	使用上述方法后，启动框架时就会从开发者指定的加载器中加载分库分表规则。



# 11.  与Spring集成

​	EF-ORM主要通过实现部分JPA接口和Spring集成。EF-ORM将被Spring认为是一个JPA实现一样提供支持。



## [11.1.    典型配置（快速入门）](undefined)

对于只想在自己的项目中快速使用EF-ORM，看本节就够了。后面的章节可以跳过。

本节内容适合于熟悉Spring事务管理机制的同学，在面对日常单个数据库连接时，可以直接使用下面的典型配置并自行修改。

 

上面这段配置，数据源、SessionFactory、TransactionManager、基于注解的事务声明都有了。然后在编写一个自己的DAO——

在Dao中，使用super.getSession()方法可以得到当前事务中的Session对象进行数据库操作。

在Service中，使用@Transactional注解进行事务控制。（请参阅Spring官方文档）

 

## 11.2.     配置和使用

### [11.2.1.   SessionFactory](undefined)的配置

前面的所有例子中，EF的核心对象是一个DbClient对象。DbClient里封装了所有的数据库连接和对应的ORM操作逻辑。

在JPA中，起到类似作用的对象是javax.persistence.EntityManagerFactory类，其地位就和某H框架的SessionFactory一样。

 代码 11-1 单数据源的EntityManagerFactory配置

 

上面的配置中，用Spring的FactoryBean创建EntityManagerFactory对象。EntityManagerFactory是JPA规范中的数据操作句柄。其类似于某H框架和某batis的SessionFactory。

上面的配置，除了定义了EntityManagerFactory对象以外，还定义一个CommonDao对象，该对象实现了org.easyframe.enterprise.spring.CommonDao接口。其作用是仿照传统Dao习惯，提供一个能完成基本数据库操作的对象。具体用法参见11.1.4节。

 

 

org.easyframe.enterprise.spring.SessionFactoryBean这个Bean支持以下的配置参数：

| 参数                              | 用途                                       | 备注或示例                                    |
| ------------------------------- | ---------------------------------------- | ---------------------------------------- |
| dataSource                      | 指定一个数据源。                                 | javax.sql.DataSource的子类即可。               |
| dataSources                     | map类型，指定多个数据源                            | <property  name="dataSources">           <map>                     <entry  key="ds1" value-ref="ds1" />                     <entry  key="ds2" value-ref="ds2" />           </map>  </property> |
| defaultDatasource               | 多数据源时，指定缺省数据源                            | <property  name="defaultDatasource" value="ds1"> |
| packagesToScan                  | 配置包名，启动时会扫描这些包下的所有实体类并加载。                | <property  name="packageToScan"><list>      <value>org.easyframe.test</value>      <value>org.easyframe.entity</value>   </list></property> |
| annotatedClasses                | 配置类名，启动时会扫描这些类并加载                        | <property  name="annotatedClasses"><list>   <value>org.easyframe.testp.jta.Product</value>   <value>org.easyframe.testp.jta.Users</value>   </list></property> |
| createTable                     | boolean类型。当扫描到实体后，如果数据库中不存在，是否建表         | 默认true，可以关闭  <property  name="createTable" value="false" /> |
| alterTable                      | boolean类型。当扫描到实体后，如果数据库中存在，是否修改表         | 默认true，可以关闭  <property  name="alterTable" value="false" /> |
| allowDropColumn                 | boolean类型。当扫描到实体后，如果数据库中存在并且需要修改时，是否可以删除列 | 默认false，可以开启  <property  name="allowDropColumn" value="true" /> |
| enhancePackages                 | 配置包名，启动时先对指定包下的实体进行一次增强，多个包用逗号分隔。        | 扫描增强只能对目录下的class文件生效，对ear/war/jar包中class无效。由于大部分J2EE容器都支持包方式部署，此功能只建议在单元测试时使用，不建议发布class未增强的包。  <property  name="enhancePackages" value="org.easyframe.tutorial"  /> |
| dynamicTables                   | 配置数据库表名，启动时扫描这些表，生成动态表模型。表名之间逗号分隔        | 参见动态表相关功能说明。  <property  name="dynamicTables" value="EF_TABLE1,XX_TABLE2,TABLE3"  /> |
| registeNonMappingTableAsDynamic | 对比当前数据库中存在的表，如果数据库中的表并未被任何实体所映射，那么生成这张表的动态表模型。 | 该功能可以将所有未被映射的表当做动态表，建立对应的动态元模型，参见动态表相关功能说明。默认false  <property  name="registeNonMappingTableAsDynamic" value="true" /> |

 

### 11.2.2.  多数据源的配置

    前面提到EF-ORM原生支持分库分表，分库分表意味着EF-ORM要能支持多个数据库实例。

最简单的多数据库下的配置如下

 代码 11-2 多数据源的EntityManagerFactory配置

 

多数据库下，需要声明一个RoutingDataSource对象。而RoutingDataSource中，可以配置一个DataSourceLookup对象。DataSourceLookup对象提供多个真正的数据源，通过配置不同的DataSourceLookup，可以实现从不同的地方读取数据源。

上面的配置方法中，定义了*dataSource-1,dataSource-2,dataSource-3*三个原始数据源，存放在Spring的ApplicationContext中。而对应的DataSourceLookup对象是SpringBeansDataSourceLookup，该对象可以从Spring上下文中查找所有的DataSource对象。

框架提供了多个DataSourceLookup，用于在从不同的地方读取数据源配置。这些数据源获取器包括以下几种。

#### URLJsonDataSourceLookup

使用HTTP访问一个URL，从该URL中获得数据源的配置信息。返回的数据源信息用JSON格式表示。参见下面的例子。

如果我们HTTP GET [http://192.168.0.1/getdb](http://192.168.0.1/getdb)可以返回如下报文

| [{          "id":  "ds1",          "url":  "jdbc:mysql://localhost:3306/test",          "user":  "root",          "password":  "123456",           "driverClassName": "org.gjt.mm.mysql.Driver"     },    {          "id":  "ds1",          "url":  "jdbc:mysql://localhost:3306/test2",          "user":  "root",          "password":  "123456",           "driverClassName": "org.gjt.mm.mysql.Driver"     }  ] |
| ---------------------------------------- |
| <bean class=*"jef.database.datasource.URLJsonDataSourceLookup"*       p:datasourceKeyFieldName=*"id*         p:urlFieldName=*"url"*       p:userFieldName=*"user"*       p:passwordFieldName=*"password"*       p:driverFieldName=*"driverClassName"*       p:location=*"http://192.168.0.1/getdb"*>        <property name=*"passwordDecryptor"*>             <!-- 自定义的数据库口令解密器 -->              <bean class=*"org.googlecode.jef.spring.MyPasswordDecryptor"*  />         </property>   </bean> |

使用上述配置，即可在需要数据库连接信息时，通过网络调用去获取数据库连接配置。

#### DbDataSourceLookup

到数据库里去取数据源的配置信息。以此进行数据源的查找。

如果利用一个配置库来维护其他各种数据库的连接信息，那么系统会到这个数据库中去寻找数据源。数据库中配置数据源的表和其中的列名也可以配置。参见下面的示例。

    使用上述配置，即可在需要数据库连接信息时，通过数据库查找去获取数据库连接配置。

#### JndiDatasourceLookup

到JNDI上下文去找寻数据源配置。参见下面的示例。

使用上述配置，即可在需要数据库连接信息时，通过JNDI查找去获取数据库连接配置。

#### MapDataSourceLookup

从一个固定的Map对象中获取已经配置的数据源信息。在我们的一些示例代码中，有些直接就用Map来传入数据源配置。

    在Spring配置中也可以用Map来传入多个数据源。

#### PropertiesDataSourceLookup

从一个classpath下的Properties文件中获取数据源配置信息。参见下面的示例。

 

[示例](undefined)[[季怡1\]](#_msocom_1) 

 

 

 

 

 

#### SpringBeansDataSourceLookup

    从Spring上下文中获取所有数据源信息。配置如下。

 

 

上面提到的是EF提供的几种默认的DataSourceLookup，开发者也可以编写自己的DataSourceLookup。

 

 

 

[示例](undefined)[[季怡2\]](#_msocom_2) 

 

 

 

 

 

### [11.2.3.   JPA](undefined)事务配置

大家都知道，Spring有七种事务传播级别。因为标准JPA只能支持其中六种，因此EF-ORM提供了相关的JPA方言以支持第七种。其中nested方式需要JDBC驱动支持SavePoint.

                    代码 11-3 Spring基于注解的声明式事务配置方法

 

 

Spring的事务配置有好多种方法，上面这种是纯注解的声明式事务，另一种流行的AOP拦截器配置方法如下

代码 11-4  Spring基于AOP拦截器的声明式事务配置方法

 

Spring事务配置方法还有很多种，但不管哪种配置方法，和ORM框架相关的就只有“*transactionManager**”*对象。其他配置都只和Spring自身的事务实现机制有关。

上面的TransactionManager的配置方法和标准的JPA事务管理器配置方法区别之处在于，指定了一个jpaDialect对象，这是因为标准JPA实现因为接口和方法功能较弱，不足以实现Spring事务控制的所有选项。因此Spring提供了一种手段，由ORM提供一个事务控制的方言，Spring根据方言可以精确控制事务。JefJpaDialect的增加，使得EF-ORM能够支持Spring的事务管理的以下特性。（这些特性是标准JPA接口无法支持的)

| **Spring****配置**                     | **在Spring****中的作用**                      | **效果**                                   |
| ------------------------------------ | ---------------------------------------- | ---------------------------------------- |
| **Propagation=****“****nested****”** | Spring的7种事务传播级别之一,NESTED方法是启动一个和父事务相依的子事务，因为不是EJB标准的，因此JPA不支持。 | JPA接口中无SavePoint操作，因此无法支持NESTED传播行为，EF-ORM在JpaDIalect中支持了SavePoint，因此可以使用NESTED传播行为。  再加上JPA本身支持的其他6种传播行为，EF-ORM可以支持全部7种传播行为。 |
| **isolation**                        | 定义事务的四种隔离级别。                             | JPA接口不提供对数据库事务隔离级别的动态调整。也就无法支持Spring的事务隔离级别。但EF-ORM可以支持。 |
| **read-only****=****"true"******     | 指定事务为只读。该属性提示ORM框架和JDBC驱动进行优化，比如Hibernate下只读事务可以省去flush缓存操作。Oracle服务器原生支持readonly级别，可以不产生回滚段，不记录重做日志，甚至可以提供可重复读等特性。 | 在只读模式下，EF-ORM将对JDBC Connection进行readOnly进行设置，从而触发数据库和驱动的只读优化。当然并不是所有的数据库都支持只读优化。 |
| **timeout**                          | 事务超时时间，事务一旦超时，会被标记为rollbackOnly，抛出异常并终止处理。 | JPA原生接口不提供事务超时控制。EF-ORM可以通过方言支持。         |

### [11.2.4.   编写DAO](undefined)

    通过上面两节，我们在Spring中提供了EntityFactoryManager和事务管理。接下来就是编写自己的Dao对象了。EF-ORM提供了一个泛型DAO实现。

#### [继承GenericDaoSupport](undefined)

EF-ORM提供了一个泛型的DAO实现。

l  接口类为org.easyframe.enterprise.spring.GenericDao<T>

l  实现类为org.easyframe.enterprise.spring.GenericDaoSupport<T>

开发者的DAO可以直接继承GenericDaoSupport类。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\StudentDao.java

继承GenericDaoSupport后，该DAO就已经有了各种基本的持久化操作方法。

 

 

如果需要自行添加方法，可以这样做

接口orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\StudentDao.java

实现类orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\StudentDaoImpl.java

 

对于自行实现的方法，可以使用继承自BaseDao类的方法获得Session对象。

一般来说，GenericDao中已经包含了绝大多数日常所需的数据库操作。如果没有特殊操作，我们甚至不需要为某个Bean创建DAO，而是使用后文的CommonDao即可。    

#### [继承BaseDao](undefined)

   GenericDao继承了BaseDao。开发者也可以直接继承org.easyframe.enterprise.spring.BaseDao类来编写DAO。在DAO中，开发者可以使用标准的 JPA 方法来实现逻辑，也可以使用EF-ORM的Session对象来实现逻辑。

 

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\MyDao.java

BaseDao基类中提供了以下方法

| 方法                 | 作用                                  | 说明                                       |
| ------------------ | ----------------------------------- | ---------------------------------------- |
| getEntityManager() | 得到javax.persistence.EntityManager对象 | EntityManager是JPA中操作持久化对象的主要方式。          |
| getSession()       | 得到jef.database.Session对象            | Session对象是EF-ORM操作数据库的基本类。前面所有例子中的DbClient和Transaction都是其子类。 |
| getDbClient()      | 得到jef.database.DbClient对象           | 不建议使用。DbClient对象是无事务状态的Session。对其进行的任何操作都是直接提交到数据库的，不在Spring事务控制之下。 |

要注意的是 getEntityManager()中得到的JPA对象javax.persistence.EntityManager中，EF-ORM并没有实现其全部方法。其中CriteriaBuilderCriteriaQuery相关的功能都会抛出UnSupportedOperationException.这部分功能请使用EF-ORM自己的Criteria API。

### [11.2.5.   常用API](undefined)：CommonDao

EF-ORM提供了CommonDao是基于Spring的Dao bean的通用接口，提供了以下方法(此处仅列举，详细请参阅API-DOC)

由于EF-ORM中的Entity可以携带Query对象，表示复杂的where条件和update条件，因此很多看似简单的接口，实际上能传入相当复杂的SQL查询对象，请不要低估其作用。

| **方法**                                   | **备注**                                   |      |      |
| ---------------------------------------- | ---------------------------------------- | ---- | ---- |
| **基础的查询、插入、更新、删除方法******                 |                                          |      |      |
| T insert(T entity);                      | 相当于session.insert                        |      |      |
| void remove(Object entity);              | 相当于session.delete()  支持单表CriteriaAPI     |      |      |
| int update(T entity);                    | 相当于session.update()  支持单表CriteriaAPI     |      |      |
| List<T> find(T data);                    | 相当于session.select()  支持单表CriteriaAPI     |      |      |
| T load(T data);                          | 相当于session.load()  支持单表CriteriaAPI       |      |      |
| <T> ResultIterator<T> iterate(T obj);    | 相当于session.iteratedSelect()  支持单表CriteriaAPI |      |      |
| **byProperty/  byKey****系列******         |                                          |      |      |
| void removeByProperty(ITableMetadata meta, String propertyName,  List<?> values); | 指定一个字段为条件，批量删除。                          |      |      |
| int removeByKey(ITableMetadata meta,String field,Serializable  key); | 指定一个字段为条件，单次删除                           |      |      |
| int removeByKey(Class<T> meta,String field,Serializable  key); | 指定一个字段为条件，单次删除                           |      |      |
| T loadByKey(Class<T> meta,String field,Serializable key); | 指定一个字段为条件，加载单条                           |      |      |
| T loadByKey(ITableMetadata meta,String field,Serializable id); | 指定一个字段为条件，加载单条                           |      |      |
| List<?> findByKey(ITableMetadata meta, String  propertyName, Object value); | 指定一个字段为条件，加载多条                           |      |      |
| **ByExample****系列******                  |                                          |      |      |
| List<T> findByExample(T entity,String... properties); | 传入模板bean，可指定字段名，这些字段值作为where条件           |      |      |
| int removeByExample(T entity,String... properties); | 传入模板bean，可指定字段名，这些字段值作为where条件           |      |      |
| **By  PrimaryKey ******                  |                                          |      |      |
| T loadByPrimaryKey(ITableMetadata meta, Object id); | 按主键加载                                    |      |      |
| T loadByPrimaryKey(Class<T> entityClass, Object  primaryKey); | 按主键加载                                    |      |      |
| **保存方法******                             |                                          |      |      |
| void persist(Object entity);             | 对象存在时更新，不存在时插入                           |      |      |
| T merge(T entity);                       | 对象存在时更新，不存在时插入                           |      |      |
| **Update****方法******                     |                                          |      |      |
| int updateByProperty(T entity,String... property); | 可传入多个字段名，这些字段的值作为where条件                 |      |      |
| int update(T entity,Map<String,Object> setValues,String...  property); | 可传入多个字段名，这些字段的值作为where条件。可在map中指定要更新的值。  |      |      |
| **Remove****方法**                         |                                          |      |      |
| void removeAll(ITableMetadata meta);     | 删除全表记录                                   |      |      |
| **批量操作系列**                               |                                          |      |      |
| int batchUpdate(List<T> entities);       | 批量（按主键）更新                                |      |      |
| int batchUpdate(List<T> entities,Boolean doGroup); | 批量（按主键）更新                                |      |      |
| int batchRemove(List<T> entities);       | 批量删除                                     |      |      |
| int batchRemove(List<T> entities,Boolean doGroup); | 批量删除                                     |      |      |
| int batchInsert(List<T> entities);       | 批量插入                                     |      |      |
| int batchInsert(List<T> entities,Boolean doGroup); | 批量插入                                     |      |      |
| **命名查询NamedQuery******                   |                                          |      |      |
| List<T> findByNq(String nqName, Class<T>  type,Map<String, Object> params); | 传入查询名称、返回类型、参数                           |      |      |
| List<T> findByNq(String nqName, ITableMetadata  meta,Map<String, Object> params); | 传入查询名称、返回类型、参数                           |      |      |
| int executeNq(String nqName,Map<String,Object> params); | 执行命名查询操作，传入查询名称，参数。                      |      |      |
| **E-SQL****操作系列**                        |                                          |      |      |
| List<T> findByQuery(String sql,Class<T> retutnType,  Map<String, Object> params); | 传入E-SQL语句查询结果                            |      |      |
| List<T> findByQuery(String sql,ITableMetadata retutnType,  Map<String, Object> params); | 传入E-SQL语句，查询结果                           |      |      |
| int executeQuery(String sql,Map<String,Object> param); | 传入E-SQL语句，执行                             |      |      |
| <T> ResultIterator<T> iterateByQuery(String  sql,Class<T> returnType,Map<String,Object> params); | 传入E-SQL语句。查询并以遍历器返回。                     |      |      |
| <T> ResultIterator<T> iterateByQuery(String sql,  ITableMetadata returnType, Map<String, Object> params); | 传入E-SQL语句。查询并以遍历器返回。                     |      |      |
| **分页查询方法**                               |                                          |      |      |
| Page<T> findAndPage(T data,int start,int limit); | 传入单表Criteria对象。分页查询                      |      |      |
| Page<T> findAndPageByNq(String nqName, Class<T>  type,Map<String, Object> params, int start,int limit); | 传入命名查询名称，分页查询                            |      |      |
| Page<T> findAndPageByNq(String nqName, ITableMetadata  meta,Map<String, Object> params, int start,int limit); | 传入命名查询名称，分页查询                            |      |      |
| Page<T> findAndPageByQuery(String sql,Class<T>  retutnType, Map<String, Object> params,int start,int limit); | 传入E-SQL语句，分页查询                           |      |      |
| Page<T> findAndPageByQuery(String sql,ITableMetadata  retutnType, Map<String, Object> params,int start,int limit); | 传入E-SQL语句，分页查询                           |      |      |
| **其他**                                   |                                          |      |      |
| Session getSession();                    | 得到的EF-ROM Session对象                      |      |      |
| DbClient getNoTransactionSession();      | 得到当前无事务的操作Session                        |      |      |
|                                          |                                          |      |      |

从上面的API可以看出，配置命名查询配置，仅凭CommonDao已经可以完成大部分的数据库DAO操作。

 

### [11.2.6.   POJO](undefined)操作支持

CommonDao中的方法还有一个特点，那就是可以支持POJO Bean。我们在最初的1.1.3示例中可以发现，无需继承jef.database.DataObject，我们可以直接使用单纯的POJO对象创建表、删除表、执行CRUD操作。

   POJO支持是为了进一步简化ORM使用而在CommonDao中进行的特殊处理。因此CommonDao中所有的泛型T，都无需继承jef.database.DataObject。

 

当我们定义POJO时，依然可以使用 @Id @Column @Table等基本的JPA注解。不过由于POJO Bean中不包含Query对象，因此在使用上基本只能按主键实现CRUD操作。

CommonDao中设计了xxxxByProperty、xxxxByKey等系列的方法，也正是考虑到POJO对象中，无法准备的记录用户设置过值的字段，因此提供一个手工指定的补救办法。使用这两个系列的方法，可以更方便的操作POJO对象。例如

我们定义一个POJO Entity

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\entity\Foo.java

便可以对其进行各种操作了

上面演示了对Foo对象进行建表、删表、增删改查操作。

 

最后，EF-ORM可以在一定程度上识别某H框架的配置文件，当做POJO Bean的注解来使用。这种做法可以在EF-ORM中直接使用某H框架的Bean定义。

 

比如我们创建不带任何注解的POJO类

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\entity\PojoEntity.java

然后我们配置一个XML文件去定义这个类的元数据

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\entity\hbm\PojoEntity.hbm.xml

 

只要通过指定class和某xml文件存在关联，EF-ORM就能够识别某H框架中的主要标签来读取元数据配置。

orm-tutorial\src\main\java\org\easyframe\tutorial\lessonb\Case1.java

 

有一项名为MetadataResourcePattern的全局参数配置，用于指定Entity类和某个XML文件之间的关联关系。

例如，所有的XML文件位于class path下的 /hbm目录中，其名称和类的SimpleName一致。此时可以在jef.properties中配置——

其中 %s表示类的SimpleName；%c表示类的全名；

还可以用 %*表示匹配任意字符，一旦匹配为*，那么EF-ORM会查找所有满足条件的XML，然后根据XML中配置的class属性反向匹配到Entity上。

目前此功能仅支持某H框架中一些基本的单表字段描述，级联等描述目前还不支持。

 

## [11.3.    多数据源下的事务控制](undefined)

在数据分库之后。下一个问题就接踵而至，这就是分布式事务的一致性问题。

如果我们依旧使用Spring的JPA事务控制器，正常情况下，如果所有数据库都成功提交，那么事务可以保持一致，如下图所示——

 

### [11.3.1.   JPA](undefined)事务（多重）

如果考虑到提交可能失败的场景，我们如果继续使用JPA事务管理器，我们将需要承担一定的风险。

**中断提交**

当遭遇提交失败时，有两种行为策略。默认的是“中断提交”

因此，在默认情况下，当一个Spring事务结束时，EF会顺序提交A、B两个数据库的修改。如果发生提交失败，则中断提交任务。

 

从上例看，也就是说，如果先提交 A库失败，那么A、B库都不提交。如果先提交A库成功，B库提交失败，那么A库的修改将会生效，而B库的修改不生效。

 

**继续提交**

此外，这一策略还可以变更。在jef.properties中配置：

开启上述配置后，那么在一个库提交失败后，整个提交过程将持续进行下去，直到所有能提交的变更都写入数据库位置。这种策略下，哪个连接先提交哪个后提交将不再产生影响。如下图所示

 

这种方式下，简单来说，如果我们的事务中用到了A、B两个数据库，事务提交时A、B数据库的修改单独提交，互不影响。

 

无论使用上述哪种策略，都有可能会出现某些数据库提交成功、某些数据库提交失败的可能。因此，在没有跨库事务一致性要求的场合，我们依然可以用JPATransactionManager来管理事务，虽然这可能会造成上述两种场景的数据不一致，但如果您的系统业务上本身就没有这种严格的一致性要求时，JPA事务不失为是最简单的使用方法。

在多库上使用JPA事务管理器时，每个数据库上的操作分别位于独立的事务中，相当于将Spring的事务划分为了多个独立的小型JPA事务。我们姑且用 “多重JPA事务”来称呼。

 

如果出现了某些数据库被提交，某些数据库出错或未提交。此时框架将会抛出*jef.database.innerpool.InconsistentCommitException**类。*该异常类标识着多个数据库的提交状态出现了不一致。该异常类中，可以获得哪些数据源提交成功，哪位未提交成功的信息。供开发者自行处理。

### [11.3.2.   JTA](undefined)事务支持

上面的问题是不是无法避免的呢？不是， SpringFramework还支持JTA事务。使用J2EE的JTA规范，我们可以让EF-ORM在多数据库下支持分布式事务。

JTA是JavaEE技术规范之一，JTA允许应用程序执行分布式事务处理——在两个或多个网络计算机资源上访问并且更新数据。EF-ORM可以借助一些数据库JDBC驱动本身的XA功能，或者第三方的开源JTA框架实现分布式事务。

使用JTA事务后，刚才的流程即可变为下图所示，因此任何一个数据库提交错误情况下，都能确保数据库数据一致性。

 

目前ef-orm推荐使用atomikos作为JTA的事务支持框架。
关于JTA的介绍，可参见http://www.ibm.com/developerworks/cn/java/j-lo-jta/

关于atomikos的介绍，可参见[http://www.atomikos.com/](http://www.atomikos.com/)

 

下面我们举例，用Spring + atomikos + EF-ORM实现分布式事务。
首先，我们在pom.xml中，引入atomikos的包以及jta的API包。

由于使用了atomikos，在Spring bean配置中，需要配置XA的数据源

上例配置了两个JTA的数据源，一个是Oracle数据库,的一个是MySQL数据库。然后配置EF-ORM的SessionFactory

配置SessionFactoryBean，和前面的区别在于要将tranactionMode配置为”jta”。

然后配置Spring的声明式事务管理。

上述是Spring的事务策略和AOP配置。其中atomikos的连接池，事务超时等控制参数也可以配置，详情可参阅atomikos的官方文档。

使用上述配置后，EF-ORM和Spring基本放弃了对事务控制，单个线程中的所有操作都在一个事务(UserTransaction)中。直到事务结束，连接关闭（被放回JTA连接池）时，所有数据才被提交。凡是位于上述切面中的save*或者delete*方法中，如果操作了多个数据库的数据，框架都会保证其数据一致性。

 

 

## 11.4.     共享其他框架的事务

 

如果您将EF-ORM和其他ORM框架混合使用，那么就会碰到共享事务的问题。我们一般会希望在一个服务(Service)方法中，无论使用哪个框架来操作数据库，这些操作都位于一个事务中。

为了适应这种场景，EF-ORM中存在一个共享事务的模式，一旦启用后，EF-ORM将会放弃自己的事务控制和连接管理，而是到Spring的上下文中去查找其他框架所使用的连接对象，然后在该连接上进行数据库操作，从而保证多个框架操作同一个事务。

目前EF-ORM可以和以下三种框架共享事务。

Hibernate 

MyBatis

Spring JdbcTemplate。

下面具体说明具体的配置方法。

### 11.4.1.  Hibernate

下面例子中配置了事务共享

 

上述配置的要点是

1、 使用Hibernate的事务管理器。注意需要注入DataSource对象。

2、 必须用同一个DataSource对象初始化JdbcTemplate， EF-ORM SessionFactory。

3、 EF-ORM的*transactionMode*参数必须设置为jdbc 。

 

### 11.4.2.  MyBatis / JdbcTemplate

 

上述配置的要点是

1、 使用*org.springframework.jdbc.datasource.DataSourceTransactionManager*事务管理器。

2、 必须用同一个DataSource对象初始化JdbcTemplate，MyBatis SessionFactory，EF-ORM SessionFactory。

3、 EF-ORM的*transactionMode*参数必须设置为jdbc。

按上述要点配置后，即可确保三个框架的操作处于同一个事务中。

 

 

 

# [12.        动态表支持](undefined)

## [12.1.     动态表支持](undefined)

### [12.1.1.  动态表的返回（特殊）](undefined)

 

本节介绍不同数据返回的规则。下节介绍操作这些映射规则的API

 

 

API介绍

 

 

 

 

 

 

 

动态表返回对象VartObject

 

1.8.0以上版本支持

## [12.2.     扫描现存的数据库表作为动态表操作](undefined)

1.8.2以上版本支持。

 

其他功能

内置连接池

# 13.        DB元数据与DDL操作

DB元数据，即数据库中的schema、table、view、function、procudure等数据库内建的各种数据结构和代码。

在EF-ORM中，提供了名为DbMetadata的API类来操作数据库的结构信息，初步了解可参见下文的例子。详情请参阅API文档。

使用API来操作数据库结构的主要优点是，一般无需考虑不同的RDBMS的兼容性，因此可以使语句在各种数据库上通用。（但依然要受数据库本身支持的数据类型等限制）

JDBC中，提供了名为DatabaseMetadata的元数据访问接口类。通过这个接口类可以获得大部分的数据库元数据信息。元数据访问接口类中的很多方法，都是在JDBC3或JDBC4中添加的，要想访问这些方法，请尽量使用满足JDBC4接口的驱动。

 

## [13.1.    访问数据库](undefined)结构

### 13.1.1.  获取RDBMS版本号等基本信息

下面的例子演示了如何获取数据库的名称、版本号、JDBC驱动名和版本号；还有数据库支持的函数、数据类型等信息。

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

前面已经讲过EF-ORM中，一个session对象中可以封装多个数据源，因此在获取DbMetaData时，首先要指定数据源。如果传入数据源名称为null，那么就会获得第一个（默认的）数据源。

### [13.1.2.   获得表和字段结构信息](undefined)

下面的例子演示了如何获得数据库中有哪些表和视图，然后可以打印出一张表的各个列和每个列的类型信息。还有相关的索引、主键等信息。

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

运行上述案例，可以查看到数据库中第一张表的列、数据类型、索引、主键等详细信息。

### 13.1.3.  删表和建表

前面的代码中，您可以已经看到了关于删除表和创建表的例子。在DbMetaData上也有相应的删表和建表方法。

       删除表时会主动删除相关的所有主外键、索引、约束。

       建表时会同时创建表上定义的索引(使用@Index 或者 @Indexed定义)，但不会创建外键。

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

注意——

在删表时，如果表存在并成功删除则返回true；如果表不存在不会抛出异常，而是返回false。

在建表时，如果表不存在并成功创建返回true；如果表已存在不会抛出异常，而是返回false。

### 13.1.4.  操作索引

下面的例子演示了查询索引、创建索引、删除索引。

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

### [13.1.5.   操作外键](undefined)

我们可以访问数据库中表和表之间的外键，并且删除它们或者创建它们。下面的例子演示了创建外键、查询外键，删除外键等操作。

 

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

 

### 13.1.6.  访问自定义函数和存储过程

我们可以访问数据库中的自定义函数和存储过程。不过DbMetaData没有提供方法去修改这些对象。

下面的例子演示了如何查询数据库中的自定义函数和存储过程。（只能看到基本信息，无法看到具体的SQL代码）。

 

src\main\java\org\easyframe\tutorial\lessonc\CaseDatabaseMetadata.java

要注意的是，查询存储过程和自定义函数是较高版本的JDBC驱动才能提供的方法。在一些不够主流的数据库上，getFunctions操作可能不支持。如果您使用的是主流数据库，请注意使用较高版本的JDBC驱动。

### [13.1.7.   修改已存在的表(Alter table)](undefined)

修改表——ALTER TABLE语句，可能是不同的RDBMS之间差别最大的SQL语句了。对于修改表，框架提供的接口也会稍微复杂一些。

修改表的功能被统一为refreshTable()方法，这个方法的作用的将数据库中的表结构和传入的表模型进行对比，并让数据库中的表和模型保持一致。

src\main\java\org\easyframe\tutorial\lessonc\CaseAlterTable.java

 

  那么，如果Student表已经存在的情况下，就需要使用ALTER TABLE的方式来维护数据库结构了。再看下面这个例子。

src\main\java\org\easyframe\tutorial\lessonc\CaseAlterTable.java

  运行上述代码，就可以看到，框架会使用ALTER TABLE语句来修改已经存在的表，使其和模型保持一致。

### 13.1.8.  监听和控制表的修改过程

在修改表的过程中，我们可以传入一个事件监听器。事件监听器并不仅仅用于了解刷新表的过程，还可以直接控制这一过程。我们可以先尝试运行位于orm-tutorial中的例子，来观察这一事件。

 

src\main\java\org\easyframe\tutorial\lessonc\CaseAlterTable.java

运行上述案例，使用者可以看到有一个桌面进度条弹出，进度条中显示了当前的工作进度，包括正在执行的比较工作或者数据库SQL语句。

 

 

|                       |                 |      |
| --------------------- | --------------- | ---- |
| 变更表之前                 | （可以控制是否继续刷新操作）  |      |
| 如果表不存在，               | 创建表之前（可以控制是否建表） |      |
| 如果表存在，在比较表之前          | （可以控制是否继续刷新操作）  |      |
| 数据库中的表和元数据比较完成后       |                 |      |
| 比较完成并生成SQL后，每句SQL执行之前 |                 |      |
| 在每句SQL执行成功后           |                 |      |
| 在任何一句SQL执行失败后         |                 |      |
| 在所有SQL都执行完成后          |                 |      |

 

按事件的时间顺序如下 

  

  

 

### 13.1.9.  使用动态表模型来维护数据库结构

在EF-ORM中支持了动态表模型，因此可以用编程接口来构造一个表模型，并且用这个表模型来维护数据库。

src\main\java\org\easyframe\tutorial\lessonc\CaseAlterTable.java

 

## [13.2.    其他功能](undefined)

### [13.2.1.   执行sql](undefined)脚本文件

 

# [14.        其他功能与特性](undefined)

这里列举一些较难归类的，或者是框架最近新增的特性。这些特性往往能在针对性的环境下简化开发，解决开发中的疑难问题。

## [14.1.    记录时间戳自动维护](undefined)

   即自动维护每条记录的create-Time字段和update-time字段。

我们可能有这种场景，当数据插入数据库时，希望在某个日期时间型字段中写入当前数据库时间。在每次数据更新时，希望在某个日期时间型字段中写入当前的数据库时间。这其实就是常用的日期时间维护操作。用对象关系映射很难实现这种功能。同时由于需要设置的是数据库时间而不是当前java环境时间，这也给功能的实现造成了一定困难。

EF-ORM 1.9.1.RELEASE开始，原生支持此类操作。简单说来，在日期字段的注解上加上@GeneratedValue，则EF就会自动维护这些日期字段。

一旦进行了上述配置，就会发现在insert或update语句中，会出现sysdate或current_timestamp等函数，直接获取数据库时间进行操作。

不过这种方式下，数据库时间不会回写到插入或更新的对象中。您需要再执行一次查询操作，才能得到刚刚写入数据库的记录的时间。

另外一种变通的方法是，在插入之前自动为字段赋值，此时的时间为当前java服务器的时间。这种方式下由于是先赋值再插入的，在操作完成后，可以从对象中获得刚才操作的时间。EF-ORM也支持这种方式。

  使用以上注解后，数据记录的插入和更新时间就可以交由框架自动维护了。

 

## [14.2.    内置连接池](undefined)

EF-ORM内置了一个稳定高效的连接池（但功能较弱）。正常情况下，EF-ORM会检测用户的DataSource是否使用了连接池。如果用户已经使用了连接池，那么EF-ORM不会启用内置连接池。如果检测到用户没有使用连接池，EF-ORM就会启用内置连接池。

内置连接池可以在额定连接数到最大连接数之间变化。定期关闭那些闲置的过多的连接。内置连接池可以定期检查空闲连接的可用性。在取出的连接使用中，如果捕捉到SQLException，并且数据库方言判定这个Exception是由于网络断线等IO错误造成的，会强行触发连接池立刻检查，清除无效连接。并且连接池会不断尝试重新连接，试图从故障中恢复。

EF-ORM启动时，都有一条info日志，如果启用内置连接池，日志如下

如果没有启用连接池，日志如下

 

大部分情况下，不需要您关心内置连接池是否开启。自动判断连接池能准确的识别DBCP, C3p0,proxool,Driuid,Tomcat cp, BoneCp等众多的连接池。如果您使用了较为冷僻的连接池造成两个连接池都被启用，您可以使用jef.properties参数（见附录一）强行关闭EF的内置连接池。

内置连接池相关参数配置可见附录一的“内置连接池相关”部分。

EF内置连接池采用了无阻塞算法，并发下安全，存取非常高效，有定期心跳，拿取检查，脏连接丢弃等连接池的基本功能，但没有PreparedStament缓存等功能。也曾在某大型电信中稳定使用，但现在是连接池层出不穷的年代，c3p0和proxool等老牌连接池都在druid、jboss等连接池面前相形见绌。而内置连接池不是EF-ORM今后发展的目标，您在小型项目中或者快速原型的项目用用没什么问题，大型商业项目建议您还是用tomcatcp或者druid吧。

# [15.        JMX监控](undefined)

EF_ORM支持JMX监控。

目前EF-ORM提供两个JMX监控Bean，分别是DbClientInfo和ORMConfig。路径如下图。

 

## [15.1.    DbClientInfo](undefined)

 

 

每个DbClient对象对应一个DbClientInfo的监控Bean。在一个进程中，如果有多个DbClient对象，那么也会有多个DbClientInfo的MXBean。

DbClientInfo的五个属性都是只读属性。记录了当前的一些运行情况信息。

| 属性                      | 含义                                       |
| ----------------------- | ---------------------------------------- |
| Connected               | 是否连接中                                    |
| RoutingDbClient         | 是否为多数据源DbClient                          |
| DataSourceNames         | 如果是多数据源的DbClient，会列出所有已知数据源的名称           |
| EmfName                 | EntityManagerFactory的名称，如果有多个EntityManagerFactory可用于区分。 |
| InnerConnectionPoolInfo | 内置连接池信息。    Max：最大连接数  Min：最小连接数      Current:当前连接数  Used：使用中的连接数 Free:空闲连接数  Poll：连接取用累计次数 Offer:连接归还累计数 |

 

 

## [15.2.    ORMConfig](undefined)

 

ORMConfig记录框架的各项配置信息，每个进程中仅有一个ORMConfig的MXBean。

 

**支持动态参数调整**

ORMConfig中的属性都是可读写的属性，即可以在运行过程中调整ORM的各项参数。包括调试日志、连接池大小等。这些参数大多数都和jef.properties中的参数对应。因此可以查看《附录一配置参数一览》或者API-DOC。

# [16.        保持数据库移植性的实践](undefined)

本章内容暂缺，为保证章节号一致性，故保留本章标题。

# [17.        性能调优指南](undefined)

## [17.1.    性能日志](undefined)

要了解性能问题所在，首先要能看懂EF-ORM输出的性能日志。

 

一个查询语句输出的日志可能是这样的

 

在上面这段日志中，第一行打印出了SQL语句，竖线后面的是此时的环境描述。这部分信息包括三部分。               

上面是当SQL语句在无事务情况下执行时的环境，当语句在事务下执行时：            

事务编号中的数字是一个随机编号，用于和日志上下文核对，可以跟踪事务的情况。

而性能相关的统计信息都在这一行中显示

在上例中，查询出5条结果，耗时1ms，其中生成SQL语句和转换结果0ms，数据库查询1ms。

l   ParseSQL:获取连接、生成SQL语句的时间。

l   DbAccesss:数据库解析SQL，执行查询的时间。

l Populate: 将数据从ResultSet中逐条读出，形成Java对象的时间。

当然并非所有的数据库操作都有这三个时间记录。比如您自行编写的SQL语句（NativeQuery）中不会有ParseSQL的统计，非select语句不会有Populate的统计。

    另外，上例中生成SQL语句和转换结果不可能真的不花费时间，因为统计是到毫秒的，因此500微秒以下的数值就被舍去了。

最后输出的是查询执行时都是三个性能相关的参数。

Max: 返回结果最大数限制。0表示不限制

Fetch:取ResultSet的fetch-size大小。该参数会严重影响大量数据返回时的性能。

Timeout: 查询超时时间。单位秒。

上面三个都是用户可以控制的性能相关参数，用来对照进行调优的。

 

所有查询类语句，都会输出结果条数（COUNT类语句直接输出COUNT结果）。而非查询语句则会显示影响的记录数。看懂上述日志，可以帮助用户统计一笔交易中，数据库操作的耗时情况，帮助用户分析和定位性能故障。

## [17.2.    级联性能](undefined)

前面已经讲过，EF-ORM在支持级联操作的基础上，还保留了单表操作的方式。此外还能控制单个查询语句中需要查询的字段等。首先我们可以考虑在应用场景上避免不当的数据操作。

 

此外EF-ORM中有若干参数可以辅助级联性能的调节。有以下两个全局参数。

| db.use.outer.join   | 使用外连接加载多对一和一对一关系。这种情况下，只需要一次查询，就可以将对一关系查询出来。默认开启。如果关闭，那么一对一或多对一级联操作将会通过多次单表查询来实现。 |
| ------------------- | ---------------------------------------- |
| db.enable.lazy.load | 延迟加载开关，即关系数据只有当被使用到的时候才会去查询。由于默认情况下対一关系是一次性直接查出的，所以实际上会被延迟加载的只有一对多和多对多  关系。但如果关闭了外连接加载，那么一对一和多对一关系也会被延迟加载。 |

在查询数据时，我们可以精确控制每个查询是否采用外连接加载，是否要加载X対一关系，是否要加载X对多关系。下面的例子演示了这种用法。

因此每个查询语句，都可以控制其级联加载的范围，级联加载的方式。

如果不希望级联操作，还可以这样

这和下面的操作是等效的

 

两个参数的用法上，延迟加载的开启是较为推荐的。这能有效防止你使用级联操作获取过多的数据。大部分情况下，外连接开启也能有效减少数据库操作的次数，提高性能。同时外连接查询能降低对一级缓存的依赖，因为在一些快速查询中，维护缓存数据也有一定的耗时。如果您关闭了外连接查询，那么推荐您开启一级缓存。因为此时级联操作对一级缓存的依赖性大大增加了。

## [17.3.    一级缓存与二级缓存](undefined)

EF-ORM设计了一级缓存。一级缓存是以每个事务Session为生命周期维护的缓存，这部分缓存会将您操作过的对象和查询过的数据缓存在内容中。（特别大的数据不会被缓存）一级缓存能有效的减少对相同对象的查询，尤其是在一对多的级联关系查询中。

一级缓存默认不开启，开启一级缓存的方法是在jef,properties中配置

使用JMX可以在ORMConfigMBean中，通过设置CacheDebug属性为true，从而在日志中输出一级缓存的命中和更新信息，用于细节上的调试和分析。

以下情况下，我们建议开启一级缓存：

l  使用较多的级联操作。

l  db.use.outer.join=false时

相反，如果使用级联操作较少，同时也开启了db.use.outer.join的场合下，我们建议关闭一级缓存。因为基于SQL操作业务逻辑中，维护一级缓存反而会增加额外的内存和性能开销。

 

EF-ORM没有内置的二级缓存。你可以使用诸如EHCache的第三方缓存框架，并通过Spring AOP等手段集成，此处不再赘述。

## [17.4.    结果集加载调优](undefined)

### [17.4.1.   Fetch-size](undefined)

即等同于JDBC中的fetch-size，描述了遍历结果集（ResultSet）时每次从数据库拉取的记录条数。设置为0则使用JDBC驱动默认值。过大则占用过多内存，过小则数据库通信次数很多，populate过程耗时很大。

如果您返回5000条以上数据，建议加大fetch-size。

Fetch-size的全局设置：在jef.properties中

 

针对单个查询设置fetch-size：所有的ConditionQuery对象，包括Query、Join、UnionQuery、NativeQuery都提供了setFetchSize(int)方法。

 

### [17.4.2.   max-results](undefined)

这个参数可以控制一个查询返回的最大结果数。事实上一个限制了最大结果数的查询逻辑上不一定正确，但是这能有效预防超出设计者预期数据规模时引起的OutOfMemory或者其他问题，而后者往往会影响整个系统中的所有交易，甚至引起服务器的故障。

因此全局性的max-result设置往往作为一个数据规模的约束条件来使用，而针对单个查询的max-result设置则可以根据应用场景而灵活控制。

 

### [17.4.3.   使用CachedRowSet](undefined)

这个参数目前只支持全局设置。其作用是在查出结果后，先将ResultSet的所有数据放在JDBC的CachedRowSet中，释放连接（仅对非事务操作，因为事务操作下连接被事务专用，在提交/回滚前不会放回连接池），然后再转换为java对象，最后释放CachedRowSet。这种操作方式具有以下特点

l 它不能减少查询结果转换的总时间，因为原先转换结果该进行的操作一步也没有少。

l 在非事务下，连接能更快的被释放。供其他业务使用。

l 它会将从ResultSet中读取数据的时间计入DbAccess阶段，使得Populate阶段的时间仅剩下调用反射操作所耗的时间。此时用户可以更清楚的知道，转换结果操作的真实性能开销。也帮助用户了解在ResultSet上的IO通信是否值得增加fetch-size来优化。

调节是否开启此功能的 方法为，在jef.properties中

 

## [17.5.    查询超时控制](undefined)

查询超时控制可以让一个SQL操作在执行一段时间后，如果无返回则抛出异常。这虽然会造成当前业务的失败，但是可以帮助您从以下几个方面改善程序的性能：

l 避免让个别不佳的SQL语句或超出开发者规模预期的查询拖慢整个系统。

l  避免数据库崩溃

l 发现锁表现象。（个别查询是因为锁表而被卡住，不主动查询数据库往往发现不了）

控制超时时间的参数设置方法为，在jef.properties中

 

目前尚未提供针对单个查询设置timeout的方法，后续版本中会增加相关API。

## [17.6.    自增值获取性能问题](undefined)

 

    很多时候插入不够快是因为Sequence自增值获取的性能开销造成，优化方法详见3.1.2.5。

# [18.        常见问题 (FAQ)](undefined)

### [18.1.1.   JDBC](undefined)驱动问题

EF-ORM在很多DDL处理和函数改写等特性上，使用了JDBC4的一些功能，因此也要求尽可能使用支持JDBC4的数据库驱动，比如Oracle驱动请使用ojdbc5或者ojdbc6。其他驱动选择也请尽可能用最新的。

### [18.1.2.   JDK7](undefined)编译后的ASM兼容

在1.8.0版本中，如果使用JDK7的编译版本编译了Entity，那么类在增强后无法在Java 7的版本下使用。这是因为JDK7使用了新版本的编译和类加载机制，将类型推断移到编译时进行，此时ASM修改后的类由于缺少类型推断信息故不能被JDK7加载。解决这个问题的办法是，在虚拟机启动参数中加上

EF-ORM 1.8.8以后的版本，在实体增强时会将类版本降低到50(对应JDK6)，因此不会发生此错误。

### [18.1.3.   数据库存储的口令加密](undefined)

可以配置加密后的数据库口令作为密码，EF-ORM在查询数据源时，会尝试解密。默认解密采用3DES算法，密钥可以在jef.properties中配置或者通过虚拟机启动参数指定。

### [18.1.4.   Oracle RAC](undefined)环境下的数据库连接

默认在Oracle RAC下，使用Oracle驱动的FCF或者OCI驱动的TAF方式进行连接失效转移。（不明白什么是FCF和TAF的请自行百度）。这两种方式将会使用Oracle驱动的连接池，因此可以手工关闭EF-ORM的内建连接池。

实际上，使用JTA事务等场景下，也要关闭内置连接池，不过此时关闭内建连接池是自动进行的。

### [18.1.5.   某些正确的SQL](undefined)语句解析错误怎么办

目前的词法分析器经过大量SQL语句的测试，都能正确解析。一个已知的问题是，JPQL参数名称不能取SQL关键字作为名称，比如”select * from t where id=:top” 、“update t set name=:desc”等，这些语句都会解析失败，因为top，desc等是SQL关键字。因此，如果碰到解析错误的SQL，尝试先改变一下JPQL参数的名称，是不是用到了SQL关键字。任何复合词都不是SQL关键字。

如果确实有解析不了的SQL语句，请用7.5节的方法，直接使用原生SQL语句。当然原生SQL语句不具备分库分表等高级功能，您需要自行处理。

### 18.1.6.  使用Jackjson做JSON序列化怎么办

Jackson是常见的JSON序列化工具，Jackson默认的序列化策略中会将DataObject中的Query，UpdateValueMap取出来进行序列化，这往往不是我们需要的。

为此，我们可以使用以下两种方法来避免出现这种情况——

方法一：只有Getter和Setter齐全的字段才被序列化。为ObjectMapper增加配置即可——

 Jackson 1.x

 

 Jackson 2.x

 

 

方法二：忽略掉不希望被序列化的字段

       在Entity类上增加注解

 

 

这两个方法任选其一，可以在Jackson下正常序列化Entity。

 

# [19.        附录一 配置参数一览](undefined)

jef.properties中可配置的参数说明。

| **参数名**                        | **用途**                                   | **默认值**          |
| ------------------------------ | ---------------------------------------- | ---------------- |
| **各项数据库操作相关**                  |                                          |                  |
| allow.empty.query              | 允许为空的查询条件                                | false            |
| allow.remove.start.with        | 当用户将oracle的  start... with... connect by这样的语句在其他不支持此特性的数据库上运行时，允许改写这部分语句以兼容其他数据库。  如果关闭此特性，此时会抛出异常，提示当前数据库无法支持 start with  这样的语法。 | false            |
| db.dynamic.insert              | 全局动态插入方式，不设值的字段不插入。                      | false            |
| db.dynamic.update              | 全局动态更新方式，不设值的字段不更新。                      | true             |
| db.encoding                    | 数据库文本编码，当将string转为blob时，或者在计算字符串插入数据库后的长度时使用（java默认用unicode，中文英文都只占1个字符） | 系统defaultcharset |
| schema.mapping                 | 多Schema下：schema重定向映射表。                   | -                |
| db.datasource.mapping          | 多数据源下:数据源名称重定向映射表。                       | -                |
| db.tables                      | 配置类名，数据库启动时扫描并创建这些类对应的表。                 | -                |
| db.init.static                 | 数据库启动时默认加载的类，用于加载一些全局配置                  | -                |
| db.force.enhancement           | 检查每个加载的entity都必须增强，否则抛出异常。               | true             |
| db.specify.allcolumn.name      | 当选择t.*时，是否指定所有的列名。有一种观点认为用t.*的时候性能较差。    | false            |
| db.password.encrypted          | 配置为true，则对数据库密码做解密处理                     | false            |
| db.encryption.key              | 数据库密码解密的密钥。会按3DES算法解密                    | -                |
| db.blob.return.type            | string，配置blob的返回类型,支持  string/byte/stream/file 四种。当使用Object类型映射Blob，又或者在动态表中使用BLOB时，查询出的blob字段将被转换为指定类型进行处理。 | stream           |
| db.enable.rowid                | 启用oracle rowid支持。 目前支持不太好，请勿使用。          | false            |
| db.keep.tx.for.postgresql      | 通过记录savepoint的方式，为postgresql进行事务的保持。     | true             |
| **分库分表功能相关**                   |                                          |                  |
| db.single.datasource           | 取消支持多数据源，原先配置的数据源绑定等功能都映射到当前数据源上         | false            |
| partition.create.table.inneed  | 按需建表开关                                   | true             |
| partition_filter_absent_tables | 过滤不存在的表开关                                | true             |
| partition_inmemory_maxrows     | 当分库操作时，不得不进行内存排序和聚合计算时，限制最大操作的行数，防止内存溢出。一旦达到最大行数，该次操作将抛出异常。0表示不限制。 | 0                |
| partition.date.span            | 当日期类型的分表分库条件缺失时，默认计算当前时间前后一段时间内的分表。      | -6,3             |
| db.partition.refresh           | 当前数据库中存在哪些分表会被缓存下来。设置缓存的最大失效时间(单位秒)，默认3600秒，即一小时。 | 3600             |
| **开发和调试相关**                    |                                          |                  |
| db.format.sql                  | 格式化sql语句，打印日志的时候使其更美观                    | true             |
| db.named.query.update          | 是否自动更新namedqueries查询配置，开启此选项后，会频繁检查本地配置的（或数据库）中named-query配置是否发生变化，并加载这些变化。这是为了开发的时候能随时加载到最新的配置变动。 | 和db.debug一致      |
| db.log.format                  | sql语句输出格式。默认情况下面向开发期间的输出格式，会详细列出语句、参数，分行显示。但在linux的命令行环境下，或者系统试运行期间，如果要用程序统计SQL日志，那么分行显示就加大了统计的难度。为此可以配置为default / no_wrap，no_wrap格式下，一个SQL语句只占用一行输出。 | default          |
| db.encoding.showlength         | 在日志中，当文本过长的时候是不完全输出的，此时要输出文本的长度。         | false            |
| db.max.batch.log               | 在batch操作时，为效率计，不会打印出全部项的参数。 只会打印前几个对象的参数。配置为5的时候就打印前五个对象。 | 5                |
| db.query.table.name            | 配置一张表名，启用数据表存放namedquery功能。一般配置为jef_named_queries | -                |
| **自增主键和Sequence操作相关**          |                                          |                  |
| auto.sequence.offset           | 在自动创建sequence (或模拟用的table)时，sequence的校准值。-1表示表示根据表中现存的记录自动校准(最大值+1)。当配置为正数时，为每张表的初始值+配置值。 | -1               |
| auto.sequence.creation         | 允许自动创建数据库sequence (或模拟用的table)，一般用在开发时和一些小型项目中，不适用于对用户权限有严格规范的专业项目中。 | true             |
| db.autoincrement.hilo          | 如果自增实现实际使用了sequence或table作为自增策略，那么开启hilo优化模式。即sequence或table生成的值作为主键的高位，再根据当前时间等自动填充低位。这样做的好处是一次操作数据库可以生成好几个值，减少了数据库操作的次数，提高了性能。   如果实际使用的是Identity模式，那么此项配置无效。  false时仅有@HiloGeneration(always=true)的配置会使用hilo。  一旦设置为true，那么所有配置了@HiloGeneration()的自增键都会使用hilo. | false            |
| db.autoincrement.native        | jpa实现关于自增实现分为  identity: 使用数据库表本身的自增特性   sequence： 使用sequence生成自增  table： 使用数据库表模拟出sequence。   Auto: 根据数据库自动选择 Identity  > Sequence  > Table (由于数据库多少都支持前两个特性，所以实际上Table默认不会被使用     * </ul>  开启此选项后，即便用户配置为Sequence或IDentity也会被认为采用Auto模式。(Table模式保留不变) | true             |
| sequence.name.pattern          | 全局的sequence名称模板。如果本身配置了sequence的名称，则会覆盖此项全局配置。 | %_SEQ            |
| sequence.batch.size            | 如果自增实现实际使用了sequence或table作为自增策略，那么每次访问数据库时会取多个sequence缓存在内存中，这里配置缓存的大小。 | 50               |
| db.support.manual.generate     | 是否允许手工指定的值代替自动生成的值                       | false            |
| db.sequence.step               | 如果自增实现中实际使用了sequence作为自增策略，那么可以step优化模式。即sequence生成的值每次递增超过1，然后ORM会补齐被跳过的编号。  (对identity,table模式无效)当配置为  0: Sequence：对Oracle自动检测，其他数据库为1  -1:Sequence模式下：对所有数据库均自动检测。在非Oracle数据库上会消耗一次Sequence。  1: Sequence模式下：相当于该功能关闭，Sequence拿到多少就是多少  >1的数值:Sequence模式下：数据库不再自动检测，按配置值作为Sequence步长（如果和实际Sequence步长不一致可能出错） | 0                |
| db.public.sequence.table       | 配置一个表名作为公共的sequence table名称。             | -                |
| **默认数据库连接（空构造DbClient时连接数据）**  |                                          |                  |
| db.type                        | 当使用无参数构造时，所连接的数据库类型                      | -                |
| db.host                        | 当使用无参数构造时，所连接的数据库地址                      | -                |
| db.port                        | 当使用无参数构造时，所连接的数据库端口                      | -                |
| db.name                        | 当使用无参数构造时，所连接的数据库名称（服务名/sid）             | -                |
| db.user                        | 当使用无参数构造时，所连接的数据库登录                      | -                |
| db.password                    | 当使用无参数构造时，所连接的数据库登录密码                    | -                |
| db.filepath                    | 当使用无参数构造时，所连接的数据库路径（本地的文件数据库，如derby  hsql sqlite等） | -                |
| **数据库性能相关参数**                  |                                          |                  |
| cache.level.1                  | 是否启用一级缓存。当启用了db.use.outer.join参数后，一级缓存的作用和对性能的影响其实没有那么大了。但是如果关闭db.use.outer.join后，还是建议要开启一级缓存。 | false            |
| db.cache.resultset             | 将resultset的数据先放入缓存，再慢慢解析(iterated操作除外)。 使用这个操作可以尽快的释放连接，适用于当连接数不够用的情况。 | false            |
| db.no.remark.connection        | true后禁止创建带remark标记的oracle数据库连接。 带remark标记的oracle连接中可以获得metadata中的remarks，但是连接上的所有操作都变得非常慢。 | false            |
| db.fetch.size                  | 数据库每次获取的大小。 0表示按JDBC驱动默认。                | 0                |
| db.max.results.limit           | 数据库查询最大条数限制，0表示不限制。                      | 0                |
| db.delete.timeout              | 数据库删除超时,单位秒                              | 60               |
| db.update.timeout              | 数据库查询超时，单位秒。实际操作中各种存储过程和自定义的sql语句也大多使用此超时判断 | 60               |
| db.select.timeout              | 数据库查询超时，单位秒。                             | 60               |
| db.use.outer.join              | 使用外连接的方式一次性从数据库查出所有对单关联的目标值。             | true             |
| db.enable.lazy.load            | 级联延迟加载特性，默认启用。由于默认情况下开启了db.use.outer.join特性，因此实际上会使用延迟加载的都是一对多和多对多。 | true             |
| db.lob.lazy.load               | LOB延迟加载。现还不够稳定，请谨慎使用                     | false            |
| **内置连接池相关**                    |                                          |                  |
| db.no.pool                     | 禁用内嵌的连接池，当datasource自带连接池功能的时候使用此配置。默认情况下，EF-ORM能识别常见的dbcp,druid,c3p0,proxool,bonecp,tomcatcp等连接池。一旦发现使用了这些连接时就会自动禁用内嵌连接池。因此一般此参数无需修改。 | false            |
| db.connection.live             | 每个连接最小生存时间，单位毫秒。                         | 60000            |
| db.connection.pool             | jef内嵌连接池最小连接数,数字                         | 3                |
| db.connection.pool.max         | jef内嵌连接池最大连接数,数字                         | 50               |
| db.heartbeat                   | jef内嵌连接池心跳时间，按此间隔对空闲的连接进行扫描检查，单位毫秒。      | 120000           |
| db.pool.debug                  | jef内嵌连接池调试开关，开启后输出连接池相关日志                | false            |
| **其他行为**                       |                                          |                  |
| table.name.translate           | 自动转换表名(为旧版本保留，如果用户没有通过jpa配置对象与表名的关系，那么开启此选项后， userid ->  user.id， 否则userid -> userid | False            |
| custom.metadata.loader         | 扩展点，配置自定义的元数据加载器 元数据默认现在都是配置在类的注解中的，使用自定义的读取器可以从其他更多的地方读取元数据配置。 | -                |
| db.operator.listener           | 配置一个类名，这个类需要实现jef.database.support.dboperatorlistener接口。 |                  |
| partition.strategy.loader      | 分表和路由规则加载器类.默认会使用基于class的annotation加载器，也可以使用资源文件  默认实现类：jef.database.meta.partitionstrategyloader$defaultloader 使用者可以实现jef.database.meta.partitionstrategyloader编写自己的分表规则加载器。 |                  |
| partition.disabled             | 尚未使用                                     |                  |
| metadata.resource.pattern      | 内建的metadata加载器可以从外部资源文件xml中读取元数据配置 如果此选项不配置任何内容，那么就取消外部元数据资源加载功能。 |                  |
| db.dialect.config              | 可以配置一个properties文件，文件中指定要覆盖哪些数据库方言和方言类的名称。通过此功能可以实现数据库方言覆盖。 |                  |
| db.check.sql.functions         | 默认在执行NativeQuery会对SQL语句中所有的function进行检查，如果认为数据库不支持则将抛出异常。设为false可以关闭此检查。 | true             |

 

注一：所有EF-ORM中的参数，都可以在启动虚拟机时用虚拟机参数覆盖，如-Ddb.debug=true，将会覆盖jef.properties中的db.debug=false的配置。

# [20.        附录二 数据库兼容性说明](undefined)

完全支持以下数据库——

l Oracle 9.0以上版本、

l MySQL 5.0以上版本

l MariaDB 5.5测试通过。 10.x版本尚未测试。

l Postgresql 8.2以上版本

l Microsoft SQL Server 2003或以上

l HSQLDB 2.0以上版本

l Apache Derby 10.5以上版本

l SQL ite（部分DDL该DBMS无法支持）

部分支持——

l Gbase

计划支持——因时间或工作量因素，Dialect尚未编写的

l IBM DB2

l Sybase

l MongoDB -JDBC

------

 [[季怡1\]](#_msoanchor_1)

 [[季怡2\]](#_msoanchor_2)