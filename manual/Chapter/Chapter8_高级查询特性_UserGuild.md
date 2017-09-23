GeeQuery使用手册——Chapter-8   高级查询特性

[TOC]

# Chapter-8  高级查询特性

## 8.1.  查询结果的转换

ORM框架最被人直观感受的功能就是： JDBC的查询结果ResultSet会被转换为用户需要的Java对象。没有之一。

在前面的例子中，我们已经了解到了一部分查询和其对应返回结果的规律。本章中，我们将全面的罗列这些规律，并介绍一些新的返回结果指定方式。EF-ORM提供了一套统一的查询返回结果定义方式，因此本章中提到的结果返回方式规则，适用于前面讲到的所有查询方式------Criteria API的或者是NativeQuery的。

简单说来，查询结果的返回场景分为以下几种

1. 返回简单对象
2. 返回和查询表匹配的对象
3. 返回任意对象容器
4. 返回Var /Map
5. 多个对象以数组形式返回
6. 多个列以数组形式返回

除了上述几个情况以外，还有两种方式转换结果。

7. 动态表的返回：在查询动态表时返回结果的场景。所谓动态表，就是开发时不为数据库表编写映射类。而是在运行时直接按表结构生成元数据模型（ITableMetadata），用该模型当做映射类来操作数据表的方法。动态表查询将在后面章节描述。
8. 自定义ResultSet到Java对象的映射关系： 用户实现ResultSet到Java对象之间的映射器(Mapper)接口，用自行实现的代码来描述从ResultSet到java类之间的转换逻辑。

场景1~6将在这一章介绍；场景8的用法在8.2节介绍。现在我们分别看一下上述场景的例子——

### 8.1.1.  返回简单对象

查询结果可以返回为一个简单对象，如String、Integer、Long、Boolean、Date这样的基本类型。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultType_returnSimpleType() throws SQLException{
	//返回String
	Query<Person> query=QB.create(Person.class);
	Selects select=QB.selectFrom(query);
	select.column(Person.Field.gender);
	List<String> result=db.selectAs(query,String.class);
	System.out.println(result);
		
	//返回data
	NativeQuery<Date> nq=db.createNativeQuery("select created from t_person",Date.class);
	List<Date> results=nq.getResultList();
	System.out.println(results);
	
	//返回数字
	List<Integer> result2=db.selectBySql("select count(*) from t_person group by gender", Integer.class);
	System.out.println(result2);
}
~~~

返回简单对象时，查询出的列只取第一列。该列的值被转换为需要的简单对象。

### 8.1.2.  返回和查询表匹配的对象

这个是使用最广泛的，简单说来查询什么表，返回什么表的对象。这也是大多数人对框架的理解方式。也只有在这种模式下，级联、延迟加载等特性才能体现出来。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultType_returnTableObject() throws SQLException{
	List<Person> persons=db.select(QB.create(Person.class));
	assertEquals(3,persons.size());
}
~~~

基于Query对象的查询，因为Query在构造时就已经知道其对应的表和对象。因此默认情况下，Query就能返回其对应表的对象。多表查询如Join、还有SQL查询下是不会出现这种场景的。****

### 8.1.3.  返回任意容器对象

用户可以指定任意一个Bean作为返回结果的容器，只要SQL语句中查出的字段和该容器对象中的字段名称一致（忽略大小写），就会将查询结果列值注入。对于不能匹配的字段，查询结果将丢弃。

比如，我们用临时定义的一个类PersonResult来作为查询结果的容器。

 orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultType_otherDataObject1() throws SQLException {
	Query<Person> q1 = QB.create(Person.class);
	List<PersonResult> result=db.selectAs(q1, PersonResult.class);
	PersonResult first=result.get(0);
	//Person表中查出的current_school_id字段无用处，被丢弃。 
	//PersonResult中的字段birthday不存在，不赋值
	System.out.println(ToStringBuilder.reflectionToString(first));
		
	String sql="select t.*,sysdate as birthday from t_person t";
	NativeQuery<PersonResult> q=db.createNativeQuery(sql,PersonResult.class);
	PersonResult p=q.getSingleResult();
	System.out.println(ToStringBuilder.reflectionToString(p));
}
		
public static class PersonResult{
	@Column(name = "person_name")
	private String personName;
	private String id;
	private String gender;
	private Date birthday;
	//Getter Setter方法略。
}	
~~~

上例中，定义了一个返回结果容器类PersonResult。该类的字段和Person表并不完全一致。其中Person表中有的current_school_id字段该类没有，因此该列的值被丢弃。同时PersonResult类中的birthday字段Person表中没有，故该字段的值为null。

我们注意到，t_person表中有一个名为created的日期型字段，能不能把birthday的值设置为这个列的值呢？是可以的，一个办法是在birthday上加上@Column注解——

~~~java
public static class PersonResult{
	@Column(name = "person_name")
	private String personName;
	private String id;
	private String gender;
	@Column(name = "created")  //增加@Column注解后，birthday就会去匹配数据库中的created列。
	private Date birthday;
	//Getter Setter方法略。
}	
~~~

也就是说，无论用于结果容器的类是不是为Entity，其字段上的@Column注解都有效。在ResultSet和结果类的字段进行匹配时，如果没有@Column注解，那么使用java field name和数据库列匹配；反之则用@Column中的name属性和数据库列匹配。均忽略大小写。

我们再看下面这个例子。这一次我们利用另一个数据库表的对象Student，来作为Person表的查询容器。

 orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultType_otherDataObject2() throws SQLException {
	// 用Person表查出Student对象。
	{
		Query<Person> query = QB.create(Person.class);
		Selects select = QB.selectFrom(query);
		select.columns("id, name as name, gender, '3' as grade, created as dateOfBirth");
		List<Student> result = db.selectAs(query, Student.class);
		System.out.println(result);
	}
	// 用Person表查出Student对象。(这种写法虽然啰嗦，但是可以利用java编译器检查字段的正确性)
	{
		Query<Person> query = QB.create(Person.class);
		Selects select = QB.selectFrom(query);
		select.column(Person.Field.id);
		select.column(Person.Field.name).as("name");
		select.column(Person.Field.gender);
		select.sqlExpression("'3'").as("grade");
	 	select.column(Person.Field.created).as("dateOfBirth");
		List<Student> result = db.selectAs(query, Student.class);
		System.out.println(result);
	}
	// 用NativeSQL做到上一点
	{
		String sql = "select id,person_name as name, gender, '3' as grade, created as date_of_birth from t_person";
		NativeQuery<Student> nq = db.createNativeQuery(sql, Student.class);
		List<Student> result = nq.getResultList();
		System.out.println(result);
	}
}
~~~

上例中，采用三种写法，使Person表的数据能被注入到Student对象中去。其中值得注意的是Student对象中有一个名为dateOfBirth，数据库列为"DATE_OF_BIRTH"的字段。

~~~java
    /**
     * 出生日期
     */
    @Column(name="DATE_OF_BIRTH")
    private Date dateOfBirth;
~~~

然而在上面的示例中，前两处用的是java名，在SQL语句中用的是数据库列名。这是因为CriteriaAPI中的column(xxx)实现上有一个特殊规则。当使用as()方法指定列在SQL中的别名是dateOfBirth时，同时还指定了这个列要注入到对象的dateOfBirth字段内。因此书写时可以按java field名称。

>**下划线不会被忽略**
>
>​	*曾经有人询问，为什么不默认认为数据库中的PERSON_NAME对应java中的personName。这过于想当然了。因为下划线在java和在数据端，都是有效的标示符。你可以在同一个类中同时定义——*
>
>​	*personName*
>
>​	*_personName*
>
>​	*person_name*
>
>​	*因此这种过度依赖java编程规范和个人命名习惯的“容错”方式，只会让框架变得越来越混乱。因此，在没有显式指定映射关系的情况下，系统不会认为DATE_OF_BIRTH会和dateofbirth匹配。下划线是名称中不可忽略的要素，为了避免混乱，EF-ORM不会去忽略名称中的下划线。*

使用自定义对象来容纳结果，对于NativeSQL和原生SQL来说是特别方便的。我们可以用任何一个已经存在的Java Bean作为结果返回容器——只要在SQL语句中，将列的别名(alias)写的和java类的中字段名称一样就可以了。

比如，我们找了一个其他库中的类，这个类有 key, value两个属性。我们在编写SQL时候让返回的两个字段名称分别为key,value，查询结果就乖乖的进入到指定的对象里去了。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultType_otherDataObject3() throws SQLException{
	String sql = "select gender as \"key\",count(*) as value from t_person group by gender";
	List<Entry> results=db.selectBySql(sql, jef.common.Entry.class);
	System.out.println(results);
}
~~~

### 8.1.4.  返回Var /Map

在没有合适的容器容纳返回结果时，一种通常的手段是使用Map。在前面的例子中，我们已经很多次的使用了Map容器。我们回过去看一下很久以前的例子——

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
//select min(t.ID) AS MIN_ID,  max(t.ID) AS MAX_ID, sum(t.ID) AS SUB_ID, avg(t.ID) AS AVG_ID from STUDENT t
~~~

一个值得注意是问题是，不同的数据库对列的大小写处理是不同的。比如Oracle中，返回的数据库列名称都是大写的，MySQL正相反。而很不幸的，Map的Key是大小写敏感的，因此当我们从Map中获取数据时，我们要使用大写的列名，还是小写的呢？

为了解决这个问题，EF-ORM返回Map的时候，用的不是JDK中的HashMap或是其他Map。而是自行实现的一个类jef.script.javascript.Var，这个类实现了Map接口，是一个忽略Key大小写的Map。所有的key在被放入时都转为小写。开发者可以任意用大写或小写的方式获取Map中的数值。这种定义也和大部分SQL环境保持一致。

一些时候，不指定返回的结果类型，也会用Map作为默认的数据返回类型。例如NativeQuery的单参数构造、不指定查询返回结果类型的Join查询等。

当需要用Map作为返回类型时，只需指定Var.class/Map.class作为返回类型，无需指定到具体的HashMap等类型。

### 8.1.5.  多个对象以数组形式返回

在Join查询中我们经常需要返回多个对象。我们看一下前面的示例

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson6\Case1.java

~~~java
@Test
public void testMultiTable() throws SQLException {
	Query<Person> p = QB.create(Person.class);
	Query<Item> i = QB.create(Item.class);
	Join join = QB.innerJoin(p, i, QB.on(Person.Field.name, Item.Field.name));
	...
	{
		Object[] objs = db.loadAs(join, Object[].class);
		Person person = (Person) objs[0];
		Item item = (Item) objs[1];

		assertEquals(person.getName(), item.getName());
	}
}
~~~

在使用CriteriaAPI的查询中，我们可以使用Object[].class作为返回结果类型。因为参与查询的每张表，都是以java对象的形式传入的。

那么，在本地化查询中，我们直接编写的SQL语句也能以Object[]分别传回吗？框架默认不提供这种行为。但是可以通过ResultTransformer来定义从ResultSet到Object[]的转换逻辑。这将在下一节介绍。

### 8.1.6.  多个列以数组形式返回

另一种方式下，我们依然可以返回数组。但这种方式返回的数组和返回多个对象不同。这是将查询出的多个列，直接以数组的方式返回。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testSlelectSimpleValueArray() throws SQLException {
	NativeQuery<Object[]> objs = db.createNativeQuery("select 'Asa' as  a ,'B' as b,1+1 as c, current_timestamp as D from student", Object[].class);
	Object[] result = objs.getSingleResult();
	assertTrue(result[1].getClass() == String.class);
	assertTrue(result[2].getClass() == Integer.class);
	assertTrue(result[3].getClass() == Timestamp.class);
}
~~~

复杂一些的查询，如多表查询，也是可以用这种方式返回结果的.

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultType_columnAsArray() throws SQLException {
	String sql = "select t1.id,person_name,gender,t2.name from t_person t1," +
			"school t2 where t1.current_school_id=t2.id";
	{
		NativeQuery<String[]> q1 = db.createNativeQuery(sql, String[].class);
		// 每个列的值被转换为String，每行记录变为一个String[]。
		for (String[] array : q1.getResultList()) {
			System.out.println(Arrays.toString(array));
		}
	}
	{
		NativeQuery<Object[]> q2 = db.createNativeQuery(sql, Object[].class);
		for (Object[] array : q2.getResultList()) {
			// 每个列的值被保留了其原始类型，每行记录变为一个Object[]。
			System.out.println(Arrays.toString(array));
		}
	}
	{
		// 单表查询API也是可以的
		Query<Person> q = QB.create(Person.class);
		Selects sel = QB.selectFrom(q);
		sel.columns(Person.Field.id, Person.Field.name, Person.Field.gender);
		List<Object[]> persons = db.selectAs(q, Object[].class);
		for (Object[] array : persons) {
			System.out.println(Arrays.toString(array));
		}
	}
}
~~~

上例中三次查询，指定的返回类型都是数组。查询条结果就是按照select语句中各列的出现顺序形成数组。

* 如果指定为Object[]，那么每列的值将保留为默认的数据类型，如String、Integer等。
* 如果指定为String[]，那么每列的值都会被强转为Striung。
* 如果指定为Integer[]也会执行转换，不过如果有列不能转换为数字就会抛出异常。
* 其他类型同

 从上面可以看到，无论用NativeQuery，或是单表查询的CriteriaAPI查询，都可以将结果列拼成数组返回。

有同学可能会有疑问，目前对于8.1.5和本节中，数组返回的指定方式是一样的，但结果集转换逻辑是有差异的。这是不是会造成混淆呢？请参见8.2章。 

## 8.2.  Transformer的使用

在上面的例子中，我们都使用slectAs() loadAs()等方法，在传入Query对象的同时指定了要返回的class类型。实际上，即便不使用selectAs和loadAs方法，也一样能指定返回的结果类型。

我们看一下Session类中的loadAs的源代码：

代码清单：EF-ORM中Session类的源代码, loadAs方法

~~~java
public <T> T loadAs(ConditionQuery queryObj,Class<T> resultClz) throws SQLException {
	queryObj.getResultTransformer().setResultType(resultClz);
	return load(queryObj);
}
~~~

我们可以发现，其实数据返回类型就是直接设置在Query对象内的。

我们前面看到过的三种查询对象——Join / Query<T>/UnionQuery，其实都提供了一个方法。

~~~java
getResultTransformer()
~~~

使用这个方法就可以得到Transformer对象，这个对象就是描述结果转换的各种行为的对象。除了控制结果转换的类型以外，还提供了更多的相关参数。我们举几个典型的例子来观察其作用。

### 8.2.1.  直接指定返回类型

首先，前面例子中的一部分使用selectAsloadAs的方法，我们可以直接用select、load等方法等效的来实现。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
/**
* 使用ResultTransformer来指定返回类型
* @throws SQLException
*/
@Test
public void testResult_transformer1() throws SQLException {
	{
		Query<Person> q = QB.create(Person.class);
		Selects sel = QB.selectFrom(q);
		sel.columns(Person.Field.id, Person.Field.name, Person.Field.gender);
		q.getResultTransformer().setResultType(String[].class); 	//指定返回类型
		String[] result=db.load(q);
		System.out.println(Arrays.toString(result));	
	}
	{
		Query<Person> query = QB.create(Person.class);
		Selects select = QB.selectFrom(query);
		select.column(Person.Field.id);
		select.column(Person.Field.name).as("name");
		select.column(Person.Field.gender);
		select.sqlExpression("'3'").as("grade");
		select.column(Person.Field.created).as("dateOfBirth");
		query.getResultTransformer().setResultType(Student.class); //指定返回类型
		Student st=db.load(query);
	}
}
~~~

因此loadAs和selectAs方法不是必需的。不过这两类方法存在一是可以简化API，让人更容易理解和使用。二是让传入的Class泛型等同于传出的泛型，使用这两个方法可以利用java语法的校验，减少开发者搞错返回类型的机会。因此，一般在条件允许的情况下，您无需使用Transformer来指定返回结果。

### 8.2.2.  区分两种返回数据的规则

在上一节，介绍了查询时返回数组的两种转换规则。

* 多表查询时每张表对应一个java bean，多个bean构成数组
* 查询的每个列的值对应到一个java值，多列的java值构成数组

大部分情况下，EF-ORM都能判断出用户传入数组的实际意图，并根据这个意图使用合适的转换规则。

但是也有一些特殊情况，用户传入的查询会具有二义性。比如前面的8.1.5 中Join查询场景，Join查询的Object[]返回格式被认为是“多个对象形成数组”返回，而不是“多列形成数组”返回。

这种情况下，也有办法，我们可以显式的提示EF-ORM，要采用后一种方式返回结果。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultType_columnAsArray2() throws SQLException {
	//多表查询的时候,Object[]的返回类型默认是一张表的多个列拼成的对象作为数组元素，
	//而不是每个列作为数组元素……
	Query<Person> q = QB.create(Person.class);
	Join join=QB.innerJoin(q, QB.create(School.class));
	List<Object[]> persons=db.selectAs(join,Object[].class);
	for (Object[] array : persons) {
		System.out.println("["+array[0].getClass()+","+array[1].getClass()+"]");
        //该查询每条记录都转换为一个Person对象和一个School对象。
	}
	//可以这样写
	join.getResultTransformer().setStrategy(PopulateStrategy.COLUMN_TO_ARRAY);
	persons=db.selectAs(join,Object[].class);
	for (Object[] array : persons) {
		System.out.println(Arrays.toString(array));
	}		
	//这样,多表查询也可以以String[]形式返回值了
	join.getResultTransformer().setStrategy(PopulateStrategy.COLUMN_TO_ARRAY);
	List<String[]> stringColumns=db.selectAs(join,String[].class);
	for (String[] array : stringColumns) {
		System.out.println(Arrays.toString(array));
	}		
}
~~~

上例中join.getResultTransformer().setStrategy(PopulateStrategy.*COLUMN_TO_ARRAY*);就是给查询设置了一个结果转换策略。显式的指定了采用COLUMN_TO_ARRAY的方式转换结果。

### 8.2.3.  忽略@Column注解

8.1.3节中介绍过，可以用任意java bean作为返回数据的容器。同时我们也提到对于自行编写的sql语句，我们只要将SQL语句中列的别名和java bean的属性名对应上，就可以简单的将数据库查询结果注入到这个Bean中。

那么我们可能写出这样一段代码

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultType_ignoreColumnAnnotation() throws SQLException {
	String sql = "select id as id, name as name, gender as gender from student";
	//将student表中查出的数据映射为Person对象。
	NativeQuery<Person> nq = db.createNativeQuery(sql, Person.class);
	Person person=nq.getSingleResult();
	assertNotNull(person.getName());
}
~~~

上面的代码看起来没什么问题，Person对象中也有id、name、gender三个属性。数据库中查出的三个列正好对应到这三个属性上。

但实际运行后，我们发现，Person对象中的name属性并没有被赋值。原因很简单，因为Person类中的name属性上有@Column注解，指定这个属性是和数据库列”person_name”映射，而不是和”name”映射。

~~~java
@Column(name = "person_name", length = 80, nullable = false)
@Indexed(definition = "unique")
private String name;
~~~

因此，当开发者希望查询结果直接和对象中的属性名发生映射，不受@Column注解影响时，可以这样

代码清单：修改后的查询写法:orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultType_ignoreColumnAnnotation() throws SQLException {
	String sql = "select id as id, name as name, gender as gender from student";
	//将student表中查出的数据映射为Person对象。
	NativeQuery<Person> nq = db.createNativeQuery(sql, Person.class);
	nq.getResultTransformer().setStrategy(PopulateStrategy.SKIP_COLUMN_ANNOTATION);
		
	Person person=nq.getSingleResult();
	assertNotNull(person.getName());
}
~~~

上例中setStrategy(PopulateStrategy.*SKIP_COLUMN_ANNOTATION*); 给了结果转换器一个提示，使其忽略@Column注解。

### 8.2.4.  自定义返回结果转换

这个可能是Transformer最为复杂也最为强大用法了。在本节中，您可以用自行实现的逻辑去转换ResultSet中返回值的处理。

我们还是分几种情况来介绍相关API和用法。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultMapper_1() throws SQLException{
	Query<Student> q=QB.create(Student.class);
	q.getResultTransformer().addMapper(new Mapper<PersonResult>(){
		@Override
		protected void transform(PersonResult obj, IResultSet rs) throws SQLException {
			obj.setBirthday(rs.getDate("DATE_OF_BIRTH"));
			obj.setPersonName(rs.getString("NAME"));
			if(obj.getBirthday()!=null){
				//计算并设置年龄
				int year=DateUtils.getYear(new Date());
				obj.setAge(year-DateUtils.getYear(obj.getBirthday()));
			}
		}
	});
	PersonResult result = db.loadAs(q,PersonResult.class);
	System.out.println(result.getPersonName()+"出生于"+result.getBirthday()+" 今年"+result.getAge()+"岁");
}
~~~

上例中，为Student表中的数据转换到PersonResult对象提供了自定义的规则实现。

 上面的例子中，自定义ResultSet到java Bean的转换。如果一个Java Bean的属性非常多，那么代码也会很繁琐。因此，考虑到很多类都是已经通过Entity类定义了数据库字段和对象关系的类，我们是不是可以利用这些Entity中固有的和数据库的映射关系呢？

正是基于这种考虑，EF-ORM中还提供了一个Mappers工具类,可以用Mappers工具直接生成Mapper映射器。

比如这个例子，用自行编写的SQL语句返回Person和school对象。其中school对象将被赋值到Person中的currentSchool字段里。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultMapper_2() throws SQLException{
	String sql="select t1.* , t2.* from t_person t1,school t2 " +
			"where t1.current_school_id=t2.id ";
	NativeQuery<Person> query = db.createNativeQuery(sql,Person.class);

	//Mappers可以提供一些默认的映射器。大部分情况下，field到column的映射都是正确的，
	query.getResultTransformer().addMapper(Mappers.toResultProperty("currentSchool", School.class));
	List<Person> result=query.getResultList();
	for(Person person: result){
		System.out.println(person.toString()+" ->"+person.getCurrentSchool());
	}
}
~~~

在上例中，使用Mappers.*aliasToProperty*方法，框架提供了一个默认的映射行为。该行为将属于School对象的所有字段组成一个Schoold对象，然后赋值到目标的currentSchool这个属性中去。

正常操作下，只有框架的级联查询才能产生Bean嵌套的结构作为结果返回。当然用上面的例子手工编码也可以做出同样的效果。但是，既然Person和School都是我们已经建模的Entity对象，为什么不能少些一点代码呢。所以Mappers工具里提供了若干方法，作用是生成一个和已知Enrtity进行映射的Mapper对象。

并且Mappers工具里也包含这样的动作，即生成的映射可以将已知的Entity注入到返回结果的属性中去，产生类似级联操作的嵌套结构。

~~~java
query.getResultTransformer().addMapper(
      Mappers.toResultProperty("currentSchool", School.class));
~~~

这里的*toResultProperty*的含义就是，由数据库列转换而成的对象，注入到目标的”currentSchool”属性中去。

非常不幸的，上面的案例在Derby虽然可以运行，但是隐藏了一个重大的问题。因为t_person表和school表中都有同名列id，因此查询结果中只能得到一个名为id的列，最终返回的数据是错误的。（在某些数据库上这样的SQL语句会直接报错）。

为此，如果我们要想将id的问题解决，我们可以把方法改成下面这样——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultMapper_2_1() throws SQLException{
	//由于两张表都有id列，这里必须重命名 
	String sql="select t1.* , t2.id as schoolid, t2.name from t_person t1,school t2 " +
			"where t1.current_school_id=t2.id ";
	NativeQuery<Person> query = db.createNativeQuery(sql,Person.class);
	
    //不一致的field，可以使用adjust方法调整。
	query.getResultTransformer().addMapper(
			Mappers.toResultProperty("currentSchool", School.class).adjust("id", "schoolid"));

	List<Person> result=query.getResultList();
}
~~~

Mappers工具返回的BeanMapper对象中，提供了adjust方法，可以指定个别属性的列映射。第一个参数是java属性名，第二个参数是数据库列名。这样就能解决实际使用时，一个Entity大部分字段都和数据库列对应、而个别不对应的问题了。

Mappers工具类提供的方法如下

| 方法                                       | 效果                                       |
| ---------------------------------------- | ---------------------------------------- |
| Mappers.toResultProperty(String,  Class\<T>) | 将查询结果中的列转换为Class所指定的对象后，注入到结果类的属性中。      |
| Mappers.toResultProperty(String,  Class\<T>, String) | 将查询结果中的列转换为Class所指定的对象后，注入到结果类的属性中。  可以指定列的命名空间。（参见后文注解） |
| Mappers.toResultProperty(String,  ITableMetadata, String) | 将查询结果中的列转换为ITableMetadata所指定的对象后，注入到结果类的属性中。  可以指定列的命名空间。（参见后文注解） |
| Mappers.toResultBean(Class\<T>)          | 将查询结果中的列按指定类型Entity映射关系，直接注入到结果类中。（比如DO对象中配置映射关系，VO对象字段几乎一致但未配置映射关系，此时可以按DO对象的规则将属性注入到VO对象中） |
| Mappers.toResultBean(Class\<T>,  String) | 将查询结果中的列按指定类型Entity映射关系，直接注入到结果类中。  可以指定列的命名空间。（参见后文注解） |
| Mappers.toResultBean(ITableMetadata,  String) | 将查询结果中的列按指定类型ITableMetadata映射关系，直接注入到结果类中。  可以指定列的命名空间。（参见后文注解） |
| Mappers.toArrayElement(int,  Class\<T>)  | 将查询结果中的列转换为Class所指定的对象后，注入到结果数组的指定位置上。（要求查询返回结果为数组） |
| Mappers.toArrayElement(int,  Class\<T>, String) | 将查询结果中的列转换为Class所指定的对象后，注入到结果数组的指定位置上。（要求查询返回结果为数组）  可以指定列的命名空间。（参见后文注解） |
| Mappers.toArrayElement(int,  ITableMetadata, String) | 将查询结果中的列转换为Class所指定的对象后，注入到结果数组的指定位置上。（要求查询返回结果为数组）  可以指定列的命名空间。（参见后文注解） |

上面列举了9个生成BeanMapper对象的方法。

首先，需要解释“指定列别名的前缀。”究竟是什么意思。

注意观察的同学可能已经发现，EF-ORM在使用Criteria API进行多表查询时，对列名的默认处理方式是这样的——

~~~sql
select T1.CURRENT_SCHOOL_ID AS T1__CURRENTSCHOOLID,
       T1.ID                AS T1__ID,
       T1.PERSON_NAME       AS T1__NAME,
       T2.NAME              AS T2__NAME,
       T2.ID                AS T2__ID
from T_PERSON T1
left join SCHOOL T2 ON T1.CURRENT_SCHOOL_ID = T2.ID
~~~

也就是说，每个列的别名是通过增加了表别名的前缀来完成的，整个前缀共4个字符，其中两位是分隔符。实际上，就相当于将原来的ID、NAME等列放到了不同的命名空间下。上面这句SQL中，PERSON表的字段都在T1的命名空间下，SCHOOL表的字段都在T2命名空间下。

因此，当我们使用框架的Criteria API查询数据时，使用“命名空间”可以快速的区分出属于一张表的所有列。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
@Test
public void testResultMapper_3() throws SQLException{
	Query<Person> t1 = QB.create(Person.class);
	Query<Student> t2 = QB.create(Student.class);

	Join join = QB.innerJoin(t1, t2, QB.on(Person.Field.name, Student.Field.name));
	Transformer transformer=join.getResultTransformer();
		
	//因为是两表查询，默认返回的数组长度为2，为了增加一个返回对象需要将数组长度调整为3
	transformer.setResultTypeAsObjectArray(3);
	transformer.addMapper(Mappers.toArrayElement(0, Student.class, "T2"));
	transformer.addMapper(Mappers.toArrayElement(1, Person.class, "T1"));
		
	//增加一个自定义的映射。
	transformer.addMapper(new Mapper<Object[]>() {
		@Override
		protected void transform(Object[] obj, IResultSet rs) throws SQLException {
			PersonResult result=new PersonResult();
			result.setPersonName(rs.getString("T1__NAME"));
			result.setBirthday(rs.getDate("T1__CREATED"));
			obj[2]=result;
		}
	});
	List<Object[]> result = db.select(join, null);
	assertNotNull(result.get(0)[2]);
}
~~~

上面的映射中，将Join查询默认返回两个对象的行为，修改成了返回3个对象，同时还调整了School和Person对象在结果中的位置。这看似和默认的返回行为差不多，但整个过程是充分定制的。并且这种定制是可以随心所欲的——包括T1和T2的映射对象可以变为别的Java Bean。

最后，ResultTransformer中提供了若干ignore系列的方法。这是由addMapper的动作衍生而来的。在一些查询中，addMapper并不会清除掉框架默认的结果转换规则，而是会并存。而某些时候我们并不希望发生框架默认的转换行为，因此可以用ignoreXXX方法，要求框架不处理返回结果中的指定列。

我们直接在上面的方法结束前增加一些代码——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case1.java

~~~java
//第二段案例: 清除映射器、忽略默认的映射规则
	{
		Transformer t=join.getResultTransformer();
		t.setResultType(Holder.class);
			
		//清除之前定义的映射器，因为之前已经在Transformer中添加了映射器。
		t.clearMapper();
		//忽略默认的映射规则
		t.ignoreAll(); 
		t.addMapper(new Mapper<Holder<PersonResult>>(){
			@Override
			protected void transform(Holder<PersonResult> obj, IResultSet rs) throws SQLException {
				PersonResult result=new PersonResult();
				result.setPersonName(rs.getString("T1__NAME"));
				result.setBirthday(rs.getDate("T1__CREATED"));
				obj.set(result);
			}
		});
		List<Holder<PersonResult>> holders=db.select(join,null);
		for(Holder<PersonResult> h: holders){
			System.out.println(h.get());
		}
	}
~~~

可以看到，在这个部分做了两个至关重要的操作------  

* t.clearMapper();

  清除Join中之前设置的映射器，之前的自定义映射器无法针对本次查询的返回类型操作，如果不清除而任由这些映射器生效，必然造成程序异常。

* t.ignoreAll(); 

  使框架默认的映射规则忽略所有列。即禁用默认的映射规则。因为默认的映射规则完全不能识别这次的返回类型，因此如不清除，一样会抛出异常。

## 8.3.  流式操作接口

当我们从数据库需要读取数百万记录时（比如报表和导出功能），常常会面临内存不够用的问题。因为如果一个用户就在内存中缓存百万的数据，那么整个系统都没有内存进行其他正常的工作了。

许多人会采用分页的方法，例如每次到数据库读取5000条。但是会受数据库的幻读问题困扰，因为多次分页期间，系统可能并发的删除或添加某些记录。因此在“不可重复读”的环境下，用分页实现高可靠的数据导出是困难的，某些记录可能被丢失，而某些记录可能会被查出两遍。

一个可行的办法，是使用JDBC的流式处理接口。这点尤其在Oracle上特别有用，因为Oracle的驱动实现得较好——可以返回一批数据到JDBC驱动中，随着一边处理一边将结果向后滚动，批量的将后续的数据传入到客户端来。

这种ResultSet实现使得数据库的查询响应变快，同时用户可以合理的调整每批加载的记录数（fetch-size），使得整体性能达到最优。MySQL 5.0以上Server和5.0以上JDBC驱动，多少也实现了这种流式加载的方法。这能有效的避免客户端出现OutOfMemoryError的信息。

到目前为止，并非所有的数据库JDBC驱动都支持流式操作模型。（也有些数据库是一次性拉取全部结果的），但不管怎么说支持流式操作确实是RDBMS在大数据时代面临的和必须解决的一个问题。

但是我们再来看之前使用数据库查询接口

~~~java
public <T extends IQueryableEntity> List<T> select(T obj) throws SQLException;
~~~

很显然，流式处理模型在这个方法上是走不通的，List<T>中会缓存整个结果集的全部数据。内存占用不是一般的大。

为此，EF-ORM中提供了支持流式操作模型的接口。

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson8\Case2.java

~~~java
@Test
public void  testIteratedSelect() throws SQLException{
	Query<Person> p=QB.create(Person.class);
	p.setFetchSize(100);
	ResultIterator<Person> results=db.iteratedSelect(p, null);
	try{
		for(;results.hasNext();){
			Person person=results.next();
			//执行业务处理
			System.out.println(person);
		}	
	}finally{
		results.close();
	}
}
~~~

在Session对象中，有iteratedSelect的几个方法，这个系列的方法都可以返回一个ResultIterator<T>对象，这个对象提供了流式处理的行为。按照上例的逻辑，每处理一个对象，就可以在内存中释放掉该对象（在取消了结果集缓存的场合下）。结果集向后滚动到底，内存中也只有最近处理的对象。

对于NativeQuery。前面已经提到过，NativeQuery提供了相同功能的查询方法------

~~~java
public ResultIterator<X> NativeQuery.getResultIterator();
~~~

那么如果原生SQL，想要查询大量数据并用流式模型操作呢？在SqlTemplate类中也提供了类似方法------

~~~java
public final <T> ResultIterator<T> iteratorBySql(String sql, 
      Transformer transformers, int maxReturn, int fetchSize, Object... objs) throws SQLException 
~~~

这个方法的参数有些复杂，详情请参阅API-DOC。

**注意事项**

1. 请注意ResultIterator的关闭问题。正常情况下，当遍历完成后ResultIterator会自动关闭。但是我们希望在编程时，必须在finally块中显式关闭ResultIterator对象。因为这个对象不关闭，意味着JDBC ResultSet不关闭，数据库资源会很快耗尽的。
2. 另外。在Session中有一个方法Session.getResultSet(String, int, Object...)。这个方法返回ResultSet对象，可能有人会误以为这就是JDBC驱动的原生结果集。可以实现流式操作。但实际上这个方法是对原生的Result的完整缓存。因此并不能用在超大结果集的返回上。

