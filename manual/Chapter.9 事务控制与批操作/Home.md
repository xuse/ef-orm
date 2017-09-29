GeeQuery使用手册——Chapter-9  事务控制与批操作

[TOC]

# Chapter-9  事务控制与批操作

## 9.1.  编程式事务控制

本节描述EF-ORM的原生事务接口，不讲述在Spring下的声明式事务。声明式事务请参阅第11章《与Spring集成》。

 在EF-ORM中，事务操作是在一个名为Transaction的类上进行的。Transaction和DbClient上都可以操作数据库。它们的关系如下图所示：

 ![9.1](E:\User\ef-orm\manual\Chapter\images\9.1.png)

图9-1 DbClient和Transaction的关系

所有DML类型的数据库操作，都是由DbClient和Transaction的公共基类Session提供的。Transaction类上不能执行DDL操作，但提供了commit()和rollback()方法，用于提交或回滚事务。

DbClient上没有提交或回滚操作，但允许进行DDL操作。

这个设计是基于这样的一种模型：用户可以同时操作多个Session。一些Session是有事务控制的(Transaction)，可以回滚或者提交。一些Session则是非事务的、会auto-commit的 (DbClient)，用户无论执行DML还是DDL，都会被立刻提交。事实上，在EF-ORM内部实现中，这也是通过为用户分配不同的连接来实现的。

简单来说，DbClient是没有事务状态的Session(Non-Transactional Session)。Transaction则是有事务状态的Session(Transactional Session)。

从DbClient上，开启一个新的事务Session，可以使用 startTransaction()方法。

```java
Transaction session = db.startTransaction();    //创建一个新的事务，类似于H框架的openSession()方法
```

从Transaction对象上，也可以得到初始的非事务Session (即DbClient)。

```java
DbClient db=session.getNoTransactionSession();   //得到非事务Session。
```

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

上面的方式，即相当于操作编程式事务，您可以在一个方法中同时操作多个事务。事务之间可以保持互相隔离。（隔离级别取决于数据库设置）。

 在现在大部分企业级开发项目中，基于AOP的声明式事务已经代替了编程式事务，EF-ORM也同样提供了JPA的事务接口。利用Spring Framework也提供了对JPA的事务支持可以实现AOP式的声明式事务。

因此在实际的企业级开发中事务管理支持事实上都是统一使用Spring Framework的事务模型的，Spring事务管理模型支持的四种数据库隔离级别和七种事务传播行为都可以在EF-ORM上实现。（标准JPA只支持六种传播行为）相关内容请参阅11节《与Spring集成》。

## 9.2.  批操作

无论在任何场合下，相同数据结构的多条数据操作性能问题总是会引起人们的关注。解决SQL性能的重要手段就是批量操作，这一操作经常被用来进行大量数据的插入、更新、删除。

批操作是改善数据库写入性能最重要的手段。批操作的本质，简单来说就是------*单句SQL，多组参数，一次执行*

这三个特定决定了批操作的特点和限制

- 单句SQL：一个批次当中所有数据的写入都必须共用同一句SQL，一个批次当中不能有不同的SQL语句。

- 多组参数：批操作中的每个元素都被转换为和公用SQL语句相匹配的参数。显然，批次要有合适大小，性能才会提高。如果一批请求只有三五组参数，性能优势就不明显了。

- 一次执行：前面的NativeQuery中，一个Query也是能搭配多组参数反复运行的。批操作比普通绑定变量操作更快的原因是，批操作不是逐条执行的，而是一次性发送所有参数到数据库服务器，数据库也一次性的返回结果。通信次数的减少是批操作性能提升的关键。

  当然一次执行也衍生了另一个特点，即一批数据中任意一组参数操作失败，那么整批数据操作都失败。这是要引起注意的。

### 9.2.1.  基本用法

Batch模式的最基本用法，是使用Session中的batchXXX系列的方法

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson9\BatchOperate.java

```java
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
```

上例中，我们首先创建了5个Person对象，用批操作一次性插入到数据表。在单表操作中，我们了解到自增键值会在插入执行后立刻回写到对象中。在批操作中，对于自增主键的生成获取特性仍然有效。

 然后我们对每个元素进行了修改，执行批量更新操作，可以发现五个元素在数据上都按主键为条件执行了更新操作。这一规则和之前的单表操作一致——在没有任何查询条件时，使用主键作为操作的where条件。

显然，一个自由的update语句并不一定是按主键操作的。因此例子接下来的演示了按自定义的条件去执行update。接下来，指定了name作为更新的条件。这样的用法产生的SQL如下——

```sql
update T_PERSON set PERSON_NAME = ? where PERSON_NAME=? 
```

其效果是按“姓名”去更新”姓名”。我们已经知道，条件和对象是在不同的位置上的，因此这样的SQL语句是可以在EF-ORM中使用的。

最后我们将全部对象传入，并按主键批量删除所有记录。

上述例子中，直接使用了Session对象中一个简单的批操作API。Session对象中，和批操作相关的API有——

| 方法                                       | 用途                                       |
| ---------------------------------------- | ---------------------------------------- |
| Session.batchDelete(List\<T>)            | 按传入的操作，在数据库中进行批量删除。                      |
| Session.batchDelete(List\<T>,  Boolean)  | 按传入的操作，在数据库中进行批量删除。指定是否分组（为了支持分库分表）      |
| Session.batchDeleteByPrimaryKey(List\<T>) | 按传入的对象的主键，在数据库中进行批量删除。                   |
| Session.batchInsert(List\<T>)            | 将传入的实体批量插入到数据库。                          |
| Session.batchInsert(List\<T>,Boolean)    | 将传入的实体批量插入到数据库。指定是否分组（为了支持分库分表）          |
| Session.batchInsertDynamic(List\<T>,  Boolean) | 将传入的实体批量插入到数据库（dynamic=true）。指定是否分组（为了支持分库分表） |
| Session.batchUpdate(List\<T>)            | 按传入的操作，在数据库中进行批量更新。                      |
| Session.batchUpdate(List\<T>,  Boolean)  | 按传入的操作，在数据库中进行批量更新。指定是否分组（为了支持分库分表）      |
| Session.startBatchDelete(T,  String)     | 高级接口。获得批操作的Batch对象。Batch对象可以让开发者更多的干预和调整batch操作的参数。 |
| Session.startBatchInsert(T,  String, boolean) | 高级接口。获得批操作的Batch对象。Batch对象可以让开发者更多的干预和调整batch操作的参数。 |
| Session.startBatchUpdate(T,  String)     | 高级接口。获得批操作的Batch对象。Batch对象可以让开发者更多的干预和调整batch操作的参数。 |

 上述方法中dynamic是指动态插入。简单来说，就是在形成INSERT语句时，那些未赋值的字段不出现在SQL语句中。（从而可以使用数据库的DEFAULT值）。

值得解释的另一个参数是”指定是否分组“，大家回想前面的批操作限制，必须是单句SQL。这意味着当使用分库分表后，不同表的插入和更新操作不能合并为一句SQL执行。

为了满足这种场景，EF-ORM采用的办法是，将所有在同一张表上执行的操作合并成一个批。而位于不同表上的操作还是区分开，分次操作。显然，分析每个对象操作所影响的表是复杂的计算，会带来一定的开销。因此，在业务开发人员能保证所有操作请求位于同一张表上时可以传入false，禁止Batch对请求重新分组，从而带来更高效的批量操作。

这一示例详见第10章。

最后提一下，目前批操作全部都是单表操作。不支持级联特性。

### 9.2.2.  不仅仅是操作实体

正如前面单表Criteria API中介绍的，批操作并非只能按主键update/delete数据。事实上在update和delete批操作时，传入的每个Entity都是一个完整的单表操作，其内嵌的Query对象可以描述一个通用的查询，而不是仅仅针对单条记录的。

从JDBC角度看，批操作是将一条SQL语句用多组参数执行。因此实际上批操作的每个元素都应该是一个SQL语句，而不仅仅是对一个实体（单条记录）的操作。

我们看下面这个批量update的例子

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson9\BatchOperate.java

```java
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
```

这段代码将引起数据库执行以下的SQL

```sql
update T_PERSON
   set CREATED = current_timestamp
where PERSON_NAME like ? escape '/'
```

然后有三组参数，使用这条SQL语句来运行。

该SQL是由整个Batch的第一个元素决定的。  对于整个Batch来说，第一个操作请求决定了整个Batch要执行什么样的SQL语句。后面的操作对象**仅仅起到了传入参数的作用**。也就是说，在后面的对象中，无论增加什么样的其他条件，都不会影响整个批次的运行SQL。

上面的示例运行结果在日志中是这样显示的——

```
Update Batch executed:3/on 2 record(s)	 Time cost([ParseSQL]:2199us, [DbAccess]:38ms)
```

这里清晰的指出了执行效果——有3组执行参数，共计修改了2行记录。

 因此，如果说9.2.1节中体现的是传统ORM中批量操作实体行为，那么本节中进一步澄清了批操作的本质：

> *以第一个传入的操作请求为模板，形成对应的SQL语句。*
>
> *从所有操作请求中获取该SQL需要的操作参数，整批一起执行。*

这种处理概念，使得我们能在Batch中处理多次SQL操作，而不仅仅是操作多个实体。

 用另一个API来执行批操作可以让我们更明显的看出这一点

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson9\BatchOperate.java

```java
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
```

这个例子中，我们查出所有创建日期在2000-1-1日之后的Person对象，将其创建日期设置为今天。然后，startBatchUpdate()方法就是传入操作模板，形成Batch操作任务。之后将所有的person对象作为参数传入并执行。

### 9.2.3.  性能的极限——Extreme模式

使用上面所说的Batch模式，并没有达到批量操作的最大性能，为了满足大量数据导入或导出（如ETL）等特殊场景，EF-ORM还提供了Extreme模式，批量插入或更新记录。

批量插入记录中，有哪些操作影响了性能的最大化呢？

1. Sequence获取访问。为了让用户能在完成插入操作后的对象中获得到其Sequence ID值，框架采用先访问一次数据库获得ID值，再执行插入的做法。批操作下，这一动作非常损耗性能。
2. 自增值回写，在一些自带自增列功能的数据库上，框架要将数据库端生成的自增列值写回到对象中。
3. 各个数据库往往会设计一些特殊的语法，来满足高性能写入等场合的需要。

Extreme模式就是针对上述情况所设计的，Extreme模式下，会放弃主键回写等一些不必要的功能，同时会启用数据库方言中的特殊语法，从而实现最大效率的数据插入和更新。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson9\BatchOperate.java

```java
private void doExtremeInsert(int max) throws SQLException {
	List<Person> persons = new ArrayList<Person>(max);
	for (int i = 0; i < max; i++) {
		Person p = new Person();
		RandomData.fill(p); // 填充一些随机值
		persons.add(p);
	}
	db.extremeInsert(persons,false);
}
```

Extreme模式的使用很简单，使用session类中的 extremeInsert 和 extremeUpdate方法即可。

对用户来说，就这么简单，但如果您想进一步了解什么是Extreme模式，可以继续看下去。

在Oracle上，Extreme模式的数据插入性能改善最为明显。因为在Oracle的Extreme模式下，INSERT语句中会直接引用Sequence。如

```sql
INSERT INTO FOO (ID, NAME, REMARK) VALUES (S_FOO.NEXTVAL, ?, ?)
```

从而免去单独访问Sequence的开销。除此之外，当启用Extreme模式后，在Oracle数据库上会增加SQL Hint，变为

```sql
INSERT /*+ APPEND */ INTO FOO (ID, NAME, REMARK) VALUES (S_FOO.NEXTVAL, ?, ?)
```

目的是进一步提升插入效率。

在别的数据库上，Extreme模式的性能差距会小一些，由于省去了ID回写功能，性能还是会有一定改善。extremeUpdate也是如此。后续的其他一些性能优化手段也会继续添加到Extreme模式上。

EF-ORM的Extereme模式插入数据的速度是非常快的。实际测试表明：在网络环境较好的Oracle数据库上，针对7~8字段（无LOB）的表。Extreme模式可以达到插入8~10万条/秒。在一般的百兆局域网环境，每秒插入记录数量也可以达到 5~6万/秒。Oracle下这一数值是普通Batch模式的10倍以上；是普通单条插入的千倍以上！

​在其他数据库上，Extreme模式的性能也会有一定提高。