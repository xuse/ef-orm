GeeQuery使用手册——Chapter-4  Query API单表操作

[TOC]

# Chapter-4  Query API单表操作

前面的许多查询和更新示例，其实就是在使用Criteria API。这套API涵盖了实体操作。囊括了单表的各种SQL操作和多表下的操作。我们先了解还有哪些单表下的操作用法。

注意：下面的例子都是在某些场景下，需要达到某个目的的API用法。

但这些用法不是一成不变是，是可以随意组合的。此外，列出的也不是实现目的的唯一方法。

请在理解其含义后灵活使用。

## 4.1.  查询

### 4.1.1.  查询条件的用法

我们先从一组示例来理解和使用CreteriaAPI的单表查询操作。

#### 4.1.1.1.  And Or Not

在SQL条件中，我们经常会碰到这三种组合的场景。下面这个案例中组合使用了And Or Not三种条件连接。可以发现，Condition作为一个条件描述，可以互相嵌套，从而描述出And Or Not之间的顺序关系。

代码示例:src/main/java/org/easyframe/tutorial/lesson3/Case1.java

~~~java
@Test
public void testSelect_AndOrNot() throws SQLException {
	Student st = new Student();
		
	//三个Like条件，或
	Condition isLastNam_ZhaoQianSun =QueryBuilder.or(
			QueryBuilder.matchStart(Student.Field.name, "赵"),
			QueryBuilder.matchStart(Student.Field.name, "钱"),
			QueryBuilder.matchStart(Student.Field.name, "孙")
	);
		
	//或条件前面加上 NOT。
	Condition isNot_ZhaoQianSun = QueryBuilder.not(isLastNam_ZhaoQianSun);
		
	//最终条件: 不姓'赵钱孙' 的女生。
	st.getQuery().addCondition(QueryBuilder.and(
			isNot_ZhaoQianSun,
			QueryBuilder.eq(Student.Field.gender, "F")
		)
	);
		
	List<Student> students=db.select(st);
	System.out.println("不姓'赵钱孙'三姓的女生:" + students.size());
		
	assertEquals(db.count(st.getQuery()), students.size());
}
~~~

对应SQL语句为：

~~~sql
select count(*) from STUDENT t
    where (not (t.NAME like ? escape '/'  or t.NAME like ? escape '/'  or t.NAME like ? escape '/' ) and t.GENDER=?) 
~~~

这里要指出的是，当使用Like语句时，为了支持传入的参数中存在 % _等SQL字符的场景，EF-ORM会自动进行转义。同时也能防止注入攻击。

   上面的写法还是稍显累赘，前面已经说过，QueryBuilder这个类可以省略写作QB。

  	此外上例中的最外层的And嵌套可以省略。因为Query对象中可以填入多个Condition，这些Condition互相之间就是And关系。省略最外层的嵌套后，写法为——

~~~java
@Test
public void testSelect_AndOrNot() throws SQLException {
	Student st = new Student();
	Condition isLastNam_ZhaoQianSun =QB.or(
			QB.matchStart(Student.Field.name, "赵"),
			QB.matchStart(Student.Field.name, "钱"),
			QB.matchStart(Student.Field.name, "孙")
	);
	st.getQuery().addCondition(QB.not(isLastNam_ZhaoQianSun));
	st.getQuery().addCondition(QB.eq(Student.Field.gender, "F"));
	List<Student> students=db.select(st);
}
~~~

#### 4.1.1.2.  使用函数或表达式作为条件

使用函数运算作为查询条件，对数据库字段计算函数

下面例子中，

~~~java
@Test
public void testSelect_Function() throws SQLException {
	Student st = new Student();
	st.getQuery().addCondition(new FBIField("concat(lower(gender) , grade)"),"f2");
	List<Student> students=db.select(st);
		
	assertEquals(db.count(st.getQuery()), students.size());
}
~~~

用FBIField可以对数据库中的字段进行函数运算。上例的条件实际执行SQL如下：

~~~sql
select count(*) from STUDENT t where lower(gender)||grade= ‘f2’
~~~

FBIField这个词可能会引起吐槽。其实这个词和美国的FBI没有什么关系。由来是Oracle的Function based index。即函数列索引。许多数据库都不支持将用函数值来创建索引，而Oracle支持并且将这类索引命名为FBI。

由于在where条件中使用函数会造成数据库索引无效，是无法在数据量较大的表中使用的。即便是Oracle上也需要专门为此创建一个函数索引。这里用了FBIField的名称即来源于此。上例的这种用法仅限于数据量小的表使用。

仔细的同学还会发现，concat函数被转化为了lower(gender)||grade这样的表达式。这其实是EF-ORM后文要提到的一个特点——方言转换功能，EF-ORM会将用户填入的JPQL或SQL表达式用当前数据库方言重写，以适应不同数据库的语法差异。这一功能主要针对NativeQuery查询，不过在这里也能生效。

在上面的例子中还要注意，FBIField中的表达式是用java模型的字段名来描述的。这里的gender、grade都是java字段名，不是数据库列名。 EF-ORM在实际查询前会使用真正的数据库列名替换这些Java属性名。这个替换的规则需要FBIFIeld对象绑定到一个Entity上。在单表查询中这不成问题，框架能自动完成绑定，但是如果在多表查询中，可能就无法准确的绑定到特定的对象上，这时候就需要用到FBIField的另两个构造函数。

~~~java
FBIField(String, Query) 
//或者
FBIField(String, IQueryableEntity)
~~~

这两个种构造函数可以让FBIField显式的绑定到一个查询Entity上。确保各个java属性能被正确的转换为SQL语句中的列名。

#### 4.1.1.3.  使用JPQLExpression

在Condition的Value部分，也允许自行编写表达式。EF-ORM提供了JpqlExpression和SqlExpression的类。

JpqlExpression和SqlExpression都是指表达式。但两者用法上还是有所不同的。这两个表达式的作用都是将制定的文本包装为数据库的原生SQL片段，将这部分SQL嵌入到我们的Criteria API查询中去，可以更为灵活和方便。

下面是JPQLExpression的两个例子

src/main/java/org/easyframe/tutorial/lesson3/Case1.java

~~~java
@Test
public void testSelect_JpqlExpression() throws SQLException {
	Student st = new Student();
	{
		//案例一
		st.getQuery().addCondition(Student.Field.gender, new JpqlExpression("upper(nvl('f','m'))"));
		List<Student> students=db.select(st);
		assertEquals(db.count(st.getQuery()), students.size());	
	}
	st.getQuery().clearQuery(); //清除上一个查询条件
	{
		//案例二: 查出出生日期最晚的学生
		st.getQuery().addCondition(Student.Field.dateOfBirth, new JpqlExpression("(select max(dateOfBirth) from student)",st.getQuery()));
		List<Student> students=db.select(st);
		assertEquals(db.count(st.getQuery()), students.size());	
	}
}
~~~

上面的代码演示了两个例子，第一个例子很简单，使用数据库的upper和nvl函数。

~~~sql
select count(*) from STUDENT t where t.GENDER=upper(coalesce('f','m'))
~~~

Nvl函数是Oracle专用的，Derby不支持，这里EF-ORM将其自动改写为coalesce函数。

第二个例子复杂一些了

~~~sql
select t.* from STUDENT t where t.DATE_OF_BIRTH=(select max(t.DATE_OF_BIRTH) from student)
~~~

​这严格意义上是一个多表查询。不过我们也能看出其特点

* 整个表达式被嵌入到where条件中的值的位置。
* 表达式中的java属性名’dateOfBirth’，被替换为了数据库列名 ‘DATE_OF_BIRTH’。


其实就这是JPQLExpression的特点，和前面介绍的一样，EF-ORM会对JPQLExpression表达式进行解析和改写处理。这包括数据库方言适配和字段名称别名匹配。

从上面例子可以看出，表达式是一个强力的工具，灵活使用表达式能让EF-ORM简单的API发挥出预想意外的强大功能。

#### 4.1.1.4.  使用SqlExpression

上一节的案例二，也可以用SqlExpression来编写，写法上稍有不同

~~~java
@Test
public void testSelect_SqlExpression() throws SQLException {
	Student st = new Student();
	//案例: 查出出生日期最晚的学生
	st.getQuery().addCondition(Student.Field.dateOfBirth, new SqlExpression("(select max(date_of_birth) from student)"));
	List<Student> students=db.select(st);

	assertEquals(db.count(st.getQuery()), students.size());	
}
~~~

生成的SQL语句和上例是一样的。

SqlExpression相比JpqlExpression，是更接近SQL底层的表达式。EF-ORM不会对SqlExpression进行改写。也就是说，SqlExpression对象中的字符被直接输出到SQL语句当中，为此你需要保证其中的内容符合数据库的语法，并且用作参数的是数据库列名，而不是对象的属性名称。这里的列名date_of_birth 必须直接写成数据库里的字段名。如果写成 dataOfBirth那么肯定是要出错的。

SqlExpression可以提供更为灵活的功能。比如SqlExpression可以直接作为Condition使用。

~~~java
@Test
public void testSelect_SqlExpression2() throws SQLException {
	Student st = new Student();
	st.getQuery().addCondition(
			new SqlExpression("{fn timestampdiff(SQL_TSI_DAY,date_of_birth,current_timestamp)} > 100")
		
		);
	List<Student> students=db.select(st);
	
	assertEquals(db.count(st.getQuery()), students.size());	
}
~~~

当然，使用SqlExpression要兼容各种数据库，对开发者的SQL功底有一定的要求。

### 4.1.2.  使用Selects对象

#### 4.1.2.1.  定义查询的列

经常有DBA告诫开发人员说，在Select语句中，不要总是查t.*，而是需要那几个列就查哪几个列。

因此，我们在使用查询时，也可以指定要查的列。

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

~~~java
@Test
public void testSelect_selectFrom() throws SQLException{
	Query<Student> query=QueryBuilder.create(Student.class);
	query.addCondition(QueryBuilder.eq(Student.Field.gender, "F"));
		
	Selects selects=QueryBuilder.selectFrom(query);
	selects.column(Student.Field.id);
	selects.column(Student.Field.name);
		
	List<Student> students=db.select(query); //查询所有女生的学号和名字
}
~~~

当查询指定了若干列时，返回的对象中未指定选出的列都是null。只有选定的几个列的属性是有值的，因此查出的是一组“不完整”对象。

#### 4.1.2.2.  使用Distinct

刚才说到，QueryBuilder可以从一个查询请求中提取出 Selects对象，Selects对象是一个操作SQL语句select部分的工具。它对应到SQL语句的select部分，是select部分的封装。

使用selects工具，我们可以完成各种诸如列选择、distinct、group by、having、为列指定别名等各种操作。例如:

 src/main/java/org/easyframe/tutorial/lesson3/Case2.java

~~~java
@Test
public void testSelect_selectDistinct() throws SQLException{
	Query<Student> query=QueryBuilder.create(Student.class);
	Selects selects=QueryBuilder.selectFrom(query);
	selects.setDistinct(true);
	selects.column(Student.Field.name);
		
	//相当于 select distinct t.NAME from STUDENT t
	List<Student> students=db.select(query);
}
~~~

上例中，通过在selects中设置distinct=true，实现distinct的查询。

#### 4.1.2.3.  使用Group和max/min

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

~~~java
@Test
public void testSelect_selectGroup() throws SQLException{
	Query<Student> query=QueryBuilder.create(Student.class);
	Selects selects=QueryBuilder.selectFrom(query);
		
	selects.column(Student.Field.gender).group(); //按性别分组
	selects.column(Student.Field.id).count();     //统计人数
	selects.column(Student.Field.id).max();       //最大的学号
	selects.column(Student.Field.id).min();       //最小的学号
		
	//上述查询的结果。无法再转换为Student对象返回了，这里将各个列按顺序形成数组返回。
	List<String[]> stat=db.selectAs(query,String[].class); 
	//相当于执行查询select t.GENDER, count(t.ID), max(t.ID), min(t.ID)  
  	//				from STUDENT t  group by t.GENDER
	for(Object[] result : stat){
		System.out.print("M".equals(result[0])?"男生":"女生");
		System.out.print(" 总数:"+result[1]+" 最大学号:" + result[2] + " 最小学号"+ result[3]);
		System.out.println();	
	}
}
~~~

上例是Selects对象的进一步用法。通过Selects对象，可以指定SQL语句的group部分，同时可以执行count、sum、avg、max、min等通用的统计函数。

 上例中还要注意一个问题，即查询对象的返回问题。在这例中，查询对象无法转换为Student对象了，因此我们需要设置一个能存放这些字段的数据类型作为返回记录的容器。最基本的容器当然就是Map和数组了。此处我们用一个String[]作为返回值的容器。

关于如何传入合适的返回结果容器，以及结果转换过程的干预等等，可以参见第8章，高级查询特性。

 有朋友问，Oracle分析函数 partition by能不能用Query对象写出来。这个 是不支持的，因为这样的SQL语句只能在Oracle上使用，且在别的数据库上难以写出替代语句，因此此类语句建议您使用NativeQuery 来写。同时NativeQuery也无法自动将其改写为其他数据库兼容的语句，因此多数据库SQL方言也需要人工编写。

#### 4.1.2.4.  使用Having

上面的例子稍微改写一下，可以产生Having的语句

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

~~~java
@Test
public void testSelect_groupHaving() throws SQLException{
	Query<Student> query=QueryBuilder.create(Student.class);
	Selects selects=QueryBuilder.selectFrom(query);
		
	selects.column(Student.Field.grade).group(); //按年级分组
	//查出人数，同时添加一个Having条件，count(id)>2
	selects.column(Student.Field.grade).count().having(Operator.GREAT, 2);    
		
	List<String[]> stat=db.selectAs(query,String[].class); 
		for(Object[] result : stat){
			System.out.print("年级:"+result[0]+" 人数:"+result[1]);
			System.out.println();	
	}
}
~~~

上面的查询产生的SQL语句如下

~~~sql
select t.GRADE,count(t.GRADE)  from STUDENT t  group by t.GRADE having count(t.GRADE)>2
~~~

在count()后面，还可以用havingOnly。其效果是 count(id)将不作为select的一项出现，仅作为having子句的内容。

#### 4.1.2.5.  count的用法

在Session类当中，还有一个常用的方法 count();

count方法和select方法经常被拿在一起比较，当传入相同的查询请求时，select方法会查出全部数据，而count方法会将请求改写，用数据库的count函数去计算这个查询将返回多少条结果。

比如下面这个例子——

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

~~~java
@Test
public void testSelect_countDistinct() throws SQLException {
	Query<Student> q = QB.create(Student.class);

	Selects items = QB.selectFrom(q);
	items.column(Student.Field.name);
	items.setDistinct(true);
	q.setMaxResult(1);

	long total = db.count(q);// select count(distinct t.NAME) from STUDENT t

	List<String> result=db.selectAs(q,String.class); // select distinct t.NAME from STUDENT t
	System.out.println("总数为:"+ total +" 查出"+ result.size()+"条");
}
~~~

这个案例使用了同一个Query请求去执行count方法和select方法，前者使用了count distinct函数，查询不重复的名称数量，返回15。后者去查询，但只返回了一条结果，因为在请求中要求最多返回一条记录。

从上面的例子来看，count方法总是通过改写一个“查询数据内容”的请求来得到数量。

再来看下面这个有点相似的例子

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

~~~java
@Test
public void testSelect_countDistinct2() throws SQLException {
	Query<Student> q = QB.create(Student.class);

	Selects items = QB.selectFrom(q);
	items.column(Student.Field.name).countDistinct();

	Integer total=db.loadAs(q,Integer.class); // select count(distinct t.NAME) from STUDENT t
	System.out.println("Count:"+  total);
}
~~~

就count而言，两种方法产生的SQL语句是完全一样的，但两种查询的含义是不同的。第二个例子中，count语句是由使用者自行指定，作为一个返回的列上的函数操作出现的。这种方式是可以和group混用的。相当于使用者自定义了一条查count的SQL语句，同时这个Query对象也只能返回数值类型的结果。

因此上面两端代码，虽然都实现了count distinct功能，但是使用的机制是不一样的，请仔细体会。

API说明：将传入的普通查询请求转换为count语句的查询方法

| 方法                                       | 作用                                       |
| ---------------------------------------- | ---------------------------------------- |
| jef.database.Session.count(ConditionQuery) | 将传入的查询请求（单表/多表）改写为count语句，然后求满足查询条件的记录总数。 |
| jef.database.Session.count(IQueryableEntity,  String) | 根据传入的查询请求（单表），求符合条件记录总数。第二个参数可以强制指定表名。一般传入null即可。 |

#### 4.1.2.6.  使用数据库函数

刚才我们已经在group中了解了常见统计函数的用法。我们在另一个案例中稍稍回顾一下

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

~~~java
@Test
public void testSelect_function1() throws SQLException {
	Query<Student> q = QB.create(Student.class);

	Selects items = QB.selectFrom(q);
	items.column(Student.Field.id).min().as("min_id");
	items.column(Student.Field.id).max().as("max_id");
	items.column(Student.Field.id).sum().as("sub_id");
	items.column(Student.Field.id).avg().as("avg_id");
		
	for(Map<String,Object> result:db.selectAs(q,Map.class)){
		System.out.println(result);
	}
}
//select min(t.ID) AS MIN_ID,  max(t.ID) AS MAX_ID, sum(t.ID) AS SUB_ID, avg(t.ID) AS AVG_ID 
// from STUDENT t
~~~

上例中对统计列添加了别名，同时返回结果改用Map封装。

那么是不是就只能用几个基本的统计函数了呢？再看下面的例子

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

~~~java
@Test
public void testSelect_function2() throws SQLException {
	Query<Student> q = QB.create(Student.class);

	Selects items = QB.selectFrom(q);
	//对姓名统一转大写
	items.column(Student.Field.name).func(Func.upper);
    //性别进行函数转换，decode是Oracle下的函数，注意观察其在Derby下的处理。
    // 有兴趣的可以换成MySQL试一下。
	items.column(Student.Field.gender).func(Func.decode, "?", "'M'" ,"'男'","'F'" ,"'女'");
	//先对日期转文本，然后截取前面的部分
	items.column(Student.Field.dateOfBirth).func(Func.str)
			.func(Func.substring,"?","1","10");
		
	for(String[] result:db.selectAs(q,String[].class)){
		System.out.println(Arrays.toString(result));
	}
}
~~~

在上面这个例子中，使用了三个不同的函数，对结果进行处理。

此外，如果不是EF-ORM内部的标注函数，您可以可以直接输入函数名，比如在mySQL上，您可以这样用

~~~java
items.column(Student.Field.dateOfBirth).func("date_format","%Y-%M-%D");
~~~

 上面的方法都是使用API创建的函数，还有一种用法，直接传入SQL表达式，这也是可以的。

看下面的例子。

#### 4.1.2.7.  在查询项中使用SQL表达式

整个查询部分使用SQL表达式。

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

~~~java
@Test
public void testSelect_sqlExpression() throws SQLException {
	Query<Student> q = QB.create(Student.class);
		
	Selects select = QB.selectFrom(q);
	select.columns("name,decode(gender,'F','女','M','男') as gender");;
	for(Student result:db.select(q)){
		System.out.println(result.getName()+" "+result.getGender());
	}
	//实际执行SQL:
	// select t.NAME,CASE WHEN GENDER = 'F' THEN '女' WHEN GENDER = 'M' THEN '男' END AS GENDER from STUDENT t
}
~~~

上面是最接近SQL的写法。相当于整个select部分都直接用SQL片段表达了。

 还可以这样写——单个字段的函数使用表达式。

src/main/java/org/easyframe/tutorial/lesson3/Case2.java

~~~java
@Test
public void testSelect_sqlExpression2() throws SQLException {
	Query<Student> q = QB.create(Student.class);
		
	Selects select = QB.selectFrom(q);
	select.column(Student.Field.name);
	select.sqlExpression("str(add_months(Date_Of_Birth,24))").as("BIRTH_ADD_24");
	for(String[] result:db.selectAs(q,String[].class)){
		System.out.println(Arrays.toString(result));
	}
}
~~~

单个字段使用表达式，为每个学生的出生日期增加24个月，SQL表达式后面也可以添加列别名等修饰。

上面两个例子中传入的都是SQL表达式。因此注意列按数据库的名称。

上面两例都对传入的SQL表达式进行了解析和改写。某些时候如不希望EF-ORM进行改写，可以使用rawExpression()方法

~~~java
//select.sqlExpression("str(add_months(Date_Of_Birth,24))");
select.rawExpression("str(add_months(Date_Of_Birth,24))"); //不带重写功能的表达式
~~~

### 4.1.3.  分页

一般来说，分页查询包含两方面的内容，一是获取总数，二是查询限定范围的结果数据。为了准确的限定结果范围，排序条件必不可少。

EF-ORM对于分页的两步操作，总体上来将遵循以下建议。

>*用户传入的Query对象或者是SQL语句中不要带有count / limit/ offset操作。可以传入不分页的普通的查询，由EF-ORM进行转化，根据需要自动转变为count语句、限定行范围的查询语句。*
>
>*这样做的原因*
>
>*1.希望应用开发者专注于业务本身，而不是分页细节。*
>
>*2. EF-ORM生成的分页语句能对开发人员屏蔽不同的数据库语法差异。*

关于总数的获取，我们可使用前文介绍的几种count方法（参见 4.1.2.5）。

关于结果范围的限定，我们可以使用EF-ORM提供的几个API （参见4.1.3.1）。

EF-ORM还提供了将上述分页行为封装在一起的操作对象，可以——

* 直接获取总数、总页数等分页信息。
* 可从头到尾进行顺序翻页或跳转，获取当页数据。

该对象名为PagingIterator的类。(参见4.1.3.2)。

#### 4.1.3.1.  限定结果范围

Session类中有以下几个方法，可以传入类型为IntRange的参数，这里的IntRange就可以用来限定结果范围。

| **方法**                                   | **用途说明**                                 |
| ---------------------------------------- | ---------------------------------------- |
| **Session.select(T,  IntRange)**         | 传入Entity形态的查询(单表/级联)，限定返回条数在IntRange区间范围内。 |
| **Session.select(ConditionQuery,  IntRange)** | 传入Query形态的查询(单表、Union、Join均可)，限定返回条数在IntRange区间范围内。 |
| **Session.select(ConditionQuery,  Class\<T>, IntRange)** | 传入Query形态的查询(单表、Union、Join均可)，结果转换为指定类型，限定返回条数在IntRange区间范围内。 |
| **Session.selectForUpdate(Query\<T>,  IntRange)** | 传入Query形态的单表查询，可在结果集上直接更新记录。             |
| **Session.iteratedSelect(T,  IntRange)** | 传入Entity形态的查询(单表/级联)，限定返回条数在IntRange区间范围内。将游标封装为返回结果遍历器。 |
| **Session.iteratedSelect(TypedQuery\<T>,  IntRange)** | 传入Query形态的查询(单表/级联/Union)，限定返回条数在IntRange区间范围内。将游标封装为返回结果遍历器。 |
| **Session.iteratedSelect(ConditionQuery,  IntRange)** | 传入Query形态的查询(单表、Union、Join均可)，限定返回条数在IntRange区间范围内。将游标封装为返回结果遍历器。 |
| **Session.iteratedSelect(ConditionQuery\<T>,  Class, IntRange)** | 传入Query形态的查询(单表、Union、Join均可)，限定返回条数在IntRange区间范围内。将游标封装为返回结果遍历器。 |

可以发现，EF-ORM查询接口高度集中。主要分为几个系列： select系列是查询出List结果。iteratedSelect是查询出游标待遍历。还有一个load系列的方法是查出单条记录的。

IntRange表示的是一个含头含尾的区间(闭区间)，和Java Collection中常见的前闭后开区间有所不同。比如表示第 1到10条记录。不是用new IntRange(0, 10)，而是用new IntRange(1, 10)。来表示，更为接近我们日常的口头语法。其实Java用习惯的人会更偏好前闭后开区间，以后可能会再考虑向下兼容的前提下支持。

示例，查询返回11~20条记录

src/main/java/org/easyframe/tutorial/lesson3/Case3.java

~~~java
@Test
public void test_IntRange() throws SQLException{
	Query<Student> q = QB.create(Student.class);
	List<Student> results=db.select(q,new IntRange(11, 20));//查询，返回第11到20条

	int count=db.count(q);
	assertEquals(count-10, results.size());
}
~~~

使用上述方法后，在不同的数据库下，框架会生成不同的分页语句。实现数据库分页。

#### 4.1.3.2.  使用PagingIterator

Session的API方法中，除了select系列、iteratedSelect系列、load系列外，还有一套pageSelect系列的方法。

| **方法**                                   | **作用**                                   |
| ---------------------------------------- | ---------------------------------------- |
| **Session.pageSelect(T,  int)**          | 传入Entity形态的查询(单表/级联) 和 分页大小              |
| **Session.pageSelect(ConditionQuery,  int)** | 传入Query形态的查询(单表、Union、Join均可)            |
| **Session.pageSelect(ConditionQuery\<T>,  Class, int)** | 传入Query形态的查询(单表、Union、Join均可)， 并指定返回结果类型 |
| **Session.pageSelect(String,  Class\<T>, int)** | 传入NativeQuery形态的查询，并指定返回结果类型。            |
| **Session.pageSelect(String,  ITableMetadata, int)** | 传入NativeQuery形态的查询，并指定返回结果类型元数据（一般用来描述动态表的模型）。 |
| **Session.pageSelect(NativeQuery\<T>,  int)** | 传入NativeQuery形态的查询，查询结果类型已经在NativeQuery中指定。一般为传入命名查询（NamedQuery）. |

上面的后三个方法涉及了NativeQuery和NamedQuery，可参见第7章。

另外，还有pageSelectBySQL的方法，但该方法支持的是原生SQL，即EF-ORM不作任何解析和改写处理的SQL。此处先不介绍。

pageSelect系列API的特点是——

* 都返回PagingIterator对象，其中封装了分页逻辑
* 最后一个int参数表示分页的大小
* 都只需要传入查询数据的请求，框架会自动改写出count语句来查总数，当然传入 SQL或Criteria Query的对象的改写实现是不同的，不过这被封装在框架内部。

当得到PagingIterator后，我们可以通过以下的API来实现分页操作

| **方法**                                   | **作用**                                   |
| ---------------------------------------- | ---------------------------------------- |
| **jef.database.PagingIterator.getTotalAsInt()** | 获得总记录数（int）                              |
| **jef.database.PagingIterator.getTotal()** | 获得总记录数 (long)                            |
| **jef.database.PagingIterator.getTotalPage()** | 获得总页数。无记录时为0。例如每页10条，总记录数11时算2页。         |
| **jef.database.PagingIterator.setOffset(int)** | 设置跳过的记录数。例如如果从第1条记录开始返回，传入0；要从第11条记录开始返回，传入10即可。 |
| **jef.database.PagingIterator.getRowsPerPage()** | 得到每页的记录数                                 |
| **jef.database.PagingIterator.getPageData()** | 得到Page对象，其中存放了当前页中的数据和总记录数。              |
| **jef.database.PagingIterator.recalculatePages()** | 强制重新计算页数。正常情况下，只会查询一次总数。基于数据库的不可重复读特性（在处理过程中有新的数据插入或被删除），如需刷新总数需要调用此方法。 |
| **jef.database.PagingIterator.hasNext()** | 当前页后面是否还有数据。（实现Iterator接口）               |
| **jef.database.PagingIterator.next()**   | 返回当前页数据，同时页码向后翻一页。（实现Iterator接口）         |

我们用举例来看分页查询的用法

src/main/java/org/easyframe/tutorial/lesson3/Case3.java

~~~java
@Test
public void test_PageSelect() throws SQLException{
	Student st=new Student();
	st.getQuery().setAllRecordsCondition();
	st.getQuery().orderByAsc(Student.Field.id);
	int start=20;
	int limit=10;
		
	//10是每页大小，20是记录偏移。记录偏移从0开始。下面的语句相当于查询21~30条记录
	Page<Student> pagedata=	db.pageSelect(st, limit).setOffset(start).getPageData();
}
~~~

注意PagingIterator对象是一个重量级的懒加载对象，其数据只有在被请求时才会去数据库查询。因此不适合作为DTO传递数据。要获得其实际信息，可用jef.database.PagingIterator.getPageData()方法。得到的Page对象较为适合作为DTO传输。

上面的例子中，我们直接从pageSelect返回的PagingIterator对象中得到了Page这个对象。

Page是一个适合于传输的POJO对象，其中只有两个属性

* 总数
* 当页的记录内容

事实上，分页经常用在Web界面显示上，需要从数据库获得的也就是这两个信息。

使用PagingIterator一个特点是，其对于范围的限定和目前大多数Web前端框架一致，都是从0开始的。

 PagingIterator对象要重量得多，其中封装了很多分页逻辑等。但最后发现Web服务一般是无状态服务，不可能持有PagingIterator很久 ，所以从实际业务看PagingIterator对象使用频率不高。需要了解PagingIterator还提供了哪些功能的，可以自行阅读API-DOC。

### 4.1.4.  小结

总而言之，Criteria API各种组合下，用法十分灵活，在上面这些案例中，请体会每个方法的用法。更多的API，请参阅API-DOC

单表的Criteria API，能将一般项目中90%以上的常见SQL语句都表达出来。在多表情况下大约75%左右的SQL语句也都能表达出来。更复杂的SQL语句可能就要使用EF-ORM中的另一操作体系，NativeQuery了。

要设计一套易于使用，并且含义明确的查询API是相当困难的。JPA 标准Criteria API集合了众多专家的智慧才能这样严谨。EF-ORM中的这套API比较随意，很大程度上来自于众多用户的意见和想法而设计。所以——

如果有什么用法，你不确定是否可以支持，可联系作者或尝试阅读源码。

如果有什么用法，你决定应该支持但却没能很支持，请联系作者。

## 4.2.  更新

### 4.2.1.  基本操作

前面我们已经讲过，update请求用到了Entity-Query这对对象。Entity中的值描述更新后的值，Query描述更新的Where条件。

在3.2.2中，我们甚至更新了对象的主键列。那么我们先回顾一下基本的更新操作用法。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

~~~java
public void testUpdate_Basic() throws SQLException{
   {
      //查出一条对象,更新该条记录.
      Student st=	db.load(Student.class, 1);
      st.setGender("M");
      int updated=db.update(st);
      //SQL: update STUDENT set GENDER = 'M' where ID=1
	}
	{
     	//按主键随意更新一条记录
      	Student st=new Student();
      	st.setId(3);
      	st.setGrade("M");
      	int updated=db.update(st);
      	//SQL: update STUDENT set GRADE = 'M' where ID=3
	}
	{
		//按条件更新多条记录
		Student st=new Student();
		st.getQuery().addCondition(Student.Field.name,Operator.MATCH_ANY,"张");
		st.setGender("M");
		int updated=db.update(st);
		//SQL: update STUDENT set GENDER = 'M' where NAME like '%张%' escape '/'
	}
}
~~~

上面列出了三种更新的场景。第一种，先从数据库读出再更新。第二种，指定主键后直接更新指定的记录。第三种，按条件更新记录。

这里比较有用的是每次update方法都返回一个数值，表示update操作影响到的记录数。如果为0，那么就没有记录被更新。

### 4.2.2.  更新操作Query的构成

EF的更新操作有点“神奇”的地方是，它总是知道哪些字段被设过值、哪些字段没有设值。EF是如何做到这一点的呢，我们来看这段代码。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

~~~java
@Test
public void testUpdate_QueryAndUpdateMap() throws SQLException{
	Student st=new Student();
	st.setId(1);
	st.setGender("M");
	
	Map<Field,Object> updateMap=st.getUpdateValueMap();
	System.out.println(updateMap);
	//updateMap.clear();
	int updated=db.update(st);
} 
~~~

运行代码后，输出

~~~
{gender=M, id=1}
~~~

这就是实现的机制。

每个实体都隐式扩充一个UpdateValueMap,用来存放需要更新的字段。每当我们调用方法startUpdate后，我们对实体的每个set操作都会被记录下来，放入到updateMap中去，当我们调用DbClient.update(T)方法时，只有这些修改过的字段才会被更新到数据库中去。

当我们调用set方法， updateMap中记录下当前设置的值。由此也可以判断出哪些字段被赋过值。

EF在处理时，如果某个字段（主键）被挪作Where条件使用，那么它就被从updateMap中去除。因此你就不会看到这种SQL语句了——

~~~sql
update student set id=1, gender=’M’ where id=1
~~~

我们来试一下，如果将这个Map清除会怎么样，我们将上面代码中注释掉的 updateMap.clear()语句重新恢复。然后运行，发现日志中输出下面的文字，然后什么也没发生。

~~~
Student Update canceled...
~~~

这就说明，由于updateMap中没有值，因此这就成了一个无效的update请求，框架自动忽略了这个请求。所以事实上，框架所做的——

不是在将数据库中的值更新为Entity中的值，而是将其更新为updateMap中的记录的值。

利用这一点，我们可以写出更多灵活的更新语句来。

### 4.2.3.  更多的更新用法

#### 4.2.3.1.  并发环境下原子操作的更新

这是一种基于数据库频繁操作的业务更新用法。

业务是这样的，有一张用户账户余额表，用户可以消费从余额中扣款，也可以向其中充值。这些操作可能同时发生。

我们在充值时，可以这样做。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

~~~java
@Test
public void testUpdate_concurrent_error() throws SQLException {		// 准备
	UserBalance u = new UserBalance();
	u.setAmout(100); // 一开始用户账上有100元钱
	db.insert(u);
		
	//开始
	UserBalance ub=db.load(UserBalance.class,1);//第一步,先查出用户账上有多少钱
	//... do some thing.
	ub.setAmout(ub.getAmout()-50);//扣款50元
	db.update(ub);//将数值更新为扣款后的数值。
}
~~~

这看上去没什么问题。但我们想一想，如果在load出数据，到update开始之间，用户同时充值了100元并提交了。这会发生什么事？很显然，用户会杯具地发现，他充值的100元消失了。因为我们在load记录时没有锁表，update的时候数据被更新为充值前再扣款50元。基于性能考虑，即便在事务中，一般也不会在一个普通的select语句中锁表。

何况锁表并不是解决问题的最好方法，在业务繁忙的系统中，我们应当尽可能提高系统的并行度，让充值和扣款能同时发生显然并不是一个坏主意。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

~~~java
@Test
public void testUpdate_Cas1() throws SQLException {
	// 准备
	UserBalance u = new UserBalance();
	u.setAmout(100); // 一开始用户账上有100元钱
	db.insert(u);
		
	//开始
	int updated;
	do{
		UserBalance ub=db.load(UserBalance.class,1);//第一步,先查出用户账上有多少钱
		ub.getQuery().addCondition(UserBalance.Field.id,ub.getId());
		ub.getQuery().addCondition(UserBalance.Field.amout,ub.getAmout());
		ub.setAmout(ub.getAmout()-50);//扣款50元
		updated=db.update(ub);
	}while(updated==0);
}
~~~

基于较小的调整，我们可以将更新请求改成这样。显然，只有当扣款前的数值和预期的一样的时候，扣款才会发生，否则会再到数据库中查询出最新的值，重新扣款。

这种算法实际上是基于Compare And Swap(CAS)的一种乐观锁实现。但是很少人会在实际项目中这么写。何况我们码农处理多变的业务规则已经够烦心的了，还要考虑在开发时考虑设计乐观锁来?

他们有更简单的理由。他们说，用一个很简单的SQL语句就能准确的扣款50元。

~~~sql
 update userbalance set amount = amount - 50 where id=1
~~~

没有比这更简单高效的用法了，只要操作一次数据库就可以完成任务。但是这给ORM提出了挑战，怎么样，能够用对象模型完成么？

我们可以这样完成一个扣款的原子操作。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

~~~java
@Test
public void testUpdate_atom() throws SQLException {
	UserBalance ub = new UserBalance();
	ub.setId(1);
	ub.prepareUpdate(UserBalance.Field.amout, new JpqlExpression("amout-50"));
	int updated = db.update(ub);
}
~~~

上例中，prepareUpdate()方法的作用，就是向updateValueMap中标记一个更新的表达式。而updateValueMap的存在，使得我们的Update请求可不仅仅将数据更新为一些字面常量，还可以更新为更多的SQL函数和表达式。

这种写法产生的SQL语句中的参数“50”没有使用绑定变量。对SQL语句绑定变量有严格要求的同学肯定会有意见。对于这些同学，可以这样写——

~~~java
@Test
public void testUpdate_atom() throws SQLException {
	UserBalance ub = new UserBalance();
	ub.setId(1);
	//扣款额为绑定变量
	ub.prepareUpdate(UserBalance.Field.amout, new JpqlExpression("amout-:cost")); 
	ub.getQuery().setAttribute("cost", 50);
	int updated = db.update(ub);
}
~~~

这里我们可以看到，表达式中可以使用绑定变量占位符，这个也是EF-ORM查询语言的特点（参见7 本地化查询）。绑定变量的具体值可以通过Query对象中的Attribute属性传入。

#### 4.2.3.2.  使用prepareUpdate方法

prepareUpdate还可以产生更多的灵活用法。比如，将一个字段的值更新为另外一个字段，或者一个已知的数据库函数。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

~~~java
@Test
public void testUpdate_MoreValues() throws SQLException {
	UserBalance ub = new UserBalance();
	ub.getQuery().setAllRecordsCondition();

	// 将一个字段更新为另一个字段的值
	ub.prepareUpdate(UserBalance.Field.todayAmount, UserBalance.Field.amout);
	// 将updateTime更新为现在的值
	ub.prepareUpdate(UserBalance.Field.updateTime, db.func(Func.now));
	// 更新为另外两个字段相加
	ub.prepareUpdate(UserBalance.Field.totalAmount, new JpqlExpression("todayAmount + amout"));
		db.update(ub);
} 
//SQL: update USERBALANCE set TODAYAMOUNT = AMOUT, TOTALAMOUNT = TODAYAMOUNT + AMOUT, UPDATETIME //= current_timestamp
~~~

上例中，我们将todayAmount更新为表的另一个字段值相加，并将updateTime更新为数据库当前的时间。这体现了prepareUpdate的灵活用法。

当然，update中也可以使用JpqlExpression和SqlExpression。这两个对象在前面的Select中已经演示过了。

update的Query部分则可以用前面select中演示过的大部分语法，包括And or等复杂条件的组合。

最终，使用上述办法Entity能够表达出大部分的Update SQL语句的，体现了这个框架的目标——更简单的操作SQL，而不是用对象关系去代替数据库的E-R关系。

### 4.2.4.  UpdateValueMap的一些特性

#### 4.2.4.1.  回写

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

~~~java
@Test
public void testUpdate_Writeback() throws SQLException{
	Student st=db.load(Student.class,1);
	st.prepareUpdate(Student.Field.id, 199);
	int oldId=st.getId();
	db.update(st);
	System.out.println("Student的id从"+ oldId +" 更新为" + st.getId());
}
~~~

上例代码运行后，出现信息

~~~
Student的id从1 更新为199
~~~

这说明，在执行update操作的时候，更新的值会回写到对象中，覆盖旧值。这也使的对象具备同时描述更新前与更新后的值的能力。

 同时上述案例也提供了另一种更新主键的操作方式。即使用对象中的主键值作为where条件，使用updateValueMap作为更新的语句。

#### 4.2.4.2.  自动清空

在更新操作完成后，updateValueMap将被清空，对象又重新开始记录赋值操作。

#### 4.2.4.3.  stopUpdate和startUpdate

在每个实体的基类DataObject当中，提供了两个方法

| **方法**            | **用途说明**                                 |
| ----------------- | ---------------------------------------- |
| **startUpdate()** | 让对象开始记录每次的赋值操作。调用此方法后，向对象赋值会被记录到updateValueMap中。 |
| **stopUpdate()**  | 让对象停止记录每次的赋值操作。调用此方法后，向对象赋值不会更新到updateValueMap中。 |

要提到的是，对于从数据库中查询出来出来的实体，默认都已经执行了startUpdate’方法，此时对其做任何set操作都会被记录。因此我们可以直接在查出的数据上修改字段，然后直接更新到数据库。新构建的对象，也是出于startUpdate阶段的。因此大部分时候我们无需调用上述两个方法。

#### 4.2.4.4.  通过对比形成updateValueMap

ER-ORM提供了一些其他的方法来生成更新请求。首先、update之前可以和数据库中的旧对象进行比较来生成updateValueMap。

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

~~~java
@Test
public void testUpdate_Compare1() throws SQLException{
	//获得新的st对象
	Student newSt=new Student();
	newSt.setId(1);
	newSt.setDateOfBirth(new Date());
	newSt.setGender("M");
	newSt.setName("王五");
	//从数据库中获得旧的st对象
	Student oldSt=db.load(Student.class,1);
		
	//把修改过的值记录到oldSt的updateValueMap中,相等的值不记入
	DbUtils.compareToUpdateMap(newSt, oldSt);
		
	//如果需要记录字段修改记录，可以直接获取oldSt.getUpdateValueMap()来记录。
	db.update(oldSt); //只有数值不同的字段被更新。
}
~~~

通过DbUtils.*compareToUpdateMap()*方法可以比较两个对象的修改内容。另外有一个DbUtils.compareToNewUpdateMap() 方法。其区别是，前者是将对比后的updateValueMap生成在old对象中，后者生成在new对象中。生成在new对象中的update操作更为适合在级联场景下，将多个表的数据都update成新的状态。

此外，当得到一个对象时，还可以将除了主键之外的全部数据都主动放置到updateValueMap中去。例如

src/main/java/org/easyframe/tutorial/lesson3/CaseUpdate.java

~~~java
@Test
public void testUpdate_fillValues() throws SQLException {
	Student newSt = new Student();
	newSt.stopUpdate();//不记录赋值操作
	newSt.setId(1);
	newSt.setDateOfBirth(new Date());
	newSt.setGender("F");
	newSt.setName("张三");
	db.update(newSt);//由于未记录赋值变更，此处update操作无效。
		
	DbUtils.fillUpdateMap(newSt);//将主键以外所有字段都记录为变更。
	db.update(newSt);//update有效
}
~~~

某些时候，对象中的updateValueMap是空的。原因可能有——

* 对象刚刚被执行过update操作，updateValueMap被清空
* 直接从数据库中查出的对象，updateValueMap是空的。
* 类不增强，失去记录赋值变更功能。
* 调用过了stopUpdate()方法。

如同上例演示一般，第一次调用update方法是无效的。

对于updateValueMap信息缺失的情况，可以用一个DbUtils.*fillUpdateMap()*方法，将对象中除主键外所有属性都标记到updateValueMap中去。因此，第二个update语句相当与更新除主键外的所有字段，就成功操作了。

## 4.3.  删除记录

### 4.3.1.  概述

删除记录和查询记录是基本一样的。都是将Entity-Query当中的数据作为条件。因此几乎所有在单表select中的Query用法，都可以直接在delete查询中使用。反过来，delete中的用法也几乎都可以在select中使用。

区别是select查询返回数据本身，delete操作则返回被删除的记录条数。

### 4.3.2.  用法示例

#### 4.3.2.1.  基本操作

在学习了Select的用法以后，我们可以轻易的写出这样的delete用法。

代码： src/main/java/org/easyframe/tutorial/lesson3/CaseDelete.java

~~~java
@Test
public void testDelete_Basic() throws SQLException {
	{// Case1. 删除从数据库加载出来的对象
		Student st = db.load(Student.class, 1);
		db.delete(st);
	}

	{ // Case2. 删除所有女生
		Student st = new Student();
		st.setGender("F");
		db.delete(st);
	}
	{//Case3. 删除所有1980年以前出生的学生
		Student st = new Student();
	    st.getQuery().addCondition(Student.Field               .dateOfBirth,Operator.LESS,DateUtils.getDate(1980, 1, 1));
		db.delete(st);
	}
}
~~~

#### 4.3.2.2.  使用Query对象

在查询时，对Query对象的赋值操作对应于select语句中的where条件。以此类推，在删除时同样可以通过Query对象传递where条件。

~~~java
public void testDelete_Basic2() throws SQLException {
	{// Case1. Between条件,删除账户余额amout在-100到0之间的所有记录。
		UserBalance ub=new UserBalance();
		ub.getQuery().addCondition(QB.between(UserBalance.Field.amout, -100, 0));
		db.delete(ub);
	}
	{ // Case2. 两个字段比较，删除todayAmount和 totalAmout相等的记录
		UserBalance ub=new UserBalance();
		ub.getQuery().addCondition(UserBalance.Field.todayAmount,UserBalance.Field.totalAmount);
		db.delete(ub);
	}
	{//Case3. 删除按表达式条件删除
		UserBalance ub=new UserBalance();
		ub.getQuery().addCondition(new SqlExpression("todayAmount + 100< totalAmount"));
		db.delete(ub);
	}
}  
~~~

上面例子都是将where条件写入到Query对象中，可对比等效的SQL语句。
