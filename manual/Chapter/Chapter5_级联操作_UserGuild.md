GeeQuery使用手册——Chapter-5 级联操作

[TOC]

# Chapter-5  级联操作

级联操作，是指在多个对象建立固定的关联关系，EF-ORM在维护对象的记录时，会将这些关联表中的记录一起维护起来。 在查询数据的时候，也会将这些关联表的中相关记录一起查询出来。

 当然上面说的是一般情况下，为了防止使用中通过关联查询获得过多的数据影响性能，现代的ORM框架大多都具备延迟加载特性（又名懒加载）。即只有当使用者请求到这部分数据时，才去数据库中查询。

级联操作除了广泛用于查询以外，还可用于插入、修改、删除等场景。

关联关系根据JPA的定义，一般分为以下四种

| 类型   | 注解           | java对象定义                                 |
| ---- | ------------ | ---------------------------------------- |
| 一对一  | @ OneToOne   | T                                        |
| 一对多  | @ OneToMany  | List\<T> / Set\<T>  /Collection\<T>/ T[] |
| 多对一  | @ ManyToOne  | T                                        |
| 多对多  | @ ManyToMany | List\<T> / Set\<T>  /Collection\<T>/ T[] |

在JPA中，这些关系都使用Annotation来标注。除此之外，EF-ORM还扩展了几个标注，用来支持一些常用的数据库操作的场景。

## 5.1.  基本操作

### 5.1.1.  使用注解描述级联关系

我们通过案例来看，首先创建如下几个实体

EF-ORM支持JPA所定义的几个多表关联和级联操作（部分支持），这些定义包括：@OneToOne,@ManyToOne, @OneToMany, @ManyToMany。使用注解，我们可以在类上描绘出级联关系。例如

src/main/java/org/easyframe/tutorial/lesson4/entity/School.java

~~~java
/**
 * 实体:学校
 * 描述一个学校的ID和名称
 */
public class School extends DataObject{
	@Column
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	
	@Column(length=64,nullable=false)
	private String name;
	//元模型
	public enum Field implements jef.database.Field{
		id,name
	}
	public School(){}
	public School(String name){
		setName(name);
	}
	//Getter Setter此处省略
}
~~~

src/main/java/org/easyframe/tutorial/lesson4/entity/Person.java

~~~java
/**
 * 实体:个人
 * 描述一个学校的ID和名称
 */
@Entity
@Table(name = "t_person")
public class Person extends DataObject {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column
	private Integer id;

	@Column(name = "person_name", length = 80, nullable = false)
	@Indexed(definition = "unique")
	private String name;

	/**
	 * 当前学校ID
	 */
	@Column(name="CURRENT_SCHOOL_ID",columnDefinition="integer")
	private int currentSchoolId;

	/**
	 * 将currentSchoolId字段和School表的id字段进行关联。从而扩展出关联对象school。
	 */
	@ManyToOne(targetEntity = School.class)
	@JoinColumn(name = "currentSchoolId", referencedColumnName = "id")
	private School currentSchool;

	public enum Field implements jef.database.Field {
		id, name, currentSchoolId
	}

	public Person() {}
	public Person(int id) {
		this.id=id;
	}
	//Getter Setter此处省略
}
~~~

接下来我们在后面的案例中演示这种配置的效果。

### 5.1.2.  单表操作方式的保留

在使用这个模型进行级联操作之前，我们先看一下，这个模型依然可以进行单表的操作。这意味着前面所看到的基于单表的Criteria API等各种单表用法在这个场景上依然可用。

src/main/java/org/easyframe/tutorial/lesson4/Case1.java

~~~java
/**
 * 级联描述是一种可以后续随时添加删除的扩展描述，原先的单表操作模型保持不变。
 *
 * @throws SQLException
*/
@Test
public void testNonCascade() throws SQLException {
	Person p = new Person();
	{
		p.setName("玄德");
		p.setCurrentSchoolId(1);

		// 虽然Person对象中配置了级联关系。
		// 但在EF-ORM中，级联关系是在保证了单表模型完整可用的基础上,补充上去的一种附属描述
		// 因此非级联操作一样可用.
		db.insert(p);
	}
	//查出记录
	p = db.load(p);

	//单表更新
	p.setCurrentSchoolId(2);
	p.setName("云长");
	db.update(p);
		
	//单表删除
	db.delete(p);
}
~~~

执行上面的代码，可以看到执行的操作和前面的单表几乎一样。这个案例顺序执行了以下的SQL语句

~~~sql
insert into T_PERSON(CURRENT_SCHOOL_ID,ID,PERSON_NAME) values(?,DEFAULT,?)
(1)currentSchoolId:	[1]
(2)name:         	[玄德]

select T1.CURRENT_SCHOOL_ID AS T1__CURRENTSCHOOLID,
	T1.ID AS T1__ID,
	T1.PERSON_NAME AS T1__NAME,
	T2.NAME AS T2__NAME,
	T2.ID AS T2__ID
 from T_PERSON T1
 left join SCHOOL T2 ON T1.CURRENT_SCHOOL_ID=T2.ID
 where T1.ID=?
(1)id:           	[1]


update T_PERSON set CURRENT_SCHOOL_ID = ?, PERSON_NAME = ? where ID=?
(1)currentSchoolId:	[2]
(2)name:         	[云长]
(3)id:           	[1]

delete from T_PERSON where ID=?
(1)id:           	[1]
~~~

可以发现，除了在查询对象之外，Insert,update,delete操作都没有去维护级联关系。 而查询对象时默认会尝试用左外连接的方式查出用户所属学校，相当于开启了级联功能。

当然，增删改操作下的级联关系是可以维护的，这在后面会讲到。

 这里我们阐述EF-ORM对级联关系的理解和处理。在EF-ORM中，级联操作途径不会破坏原有单表操作途径。

级联关系是一种可以灵活的修改和变更的**附加关系模型**。之所以说它是“附加的”，是因为EF-ORM中，级联关系是在保证了单表模型完整可用的基础上，补充的一种关系描述。

上例可以看出，补充上去的模型表现为这一段定义。无论类上有没有定义这个关系，都不影响这个这个类的单表操作。

~~~java
/**
* 将currentSchoolId字段和School表的id字段进行关联。从而扩展出关联对象school。
*/
@ManyToOne(targetEntity = School.class)
@JoinColumn(name = "currentSchoolId", referencedColumnName = "id")
private School currentSchool;
~~~

这种设计的补充描述如下：

>**级联不破单表**
>
>​	*这种做法简单来说就是，“级联模型不破单表模型”，而是依附于单表模型。这和某H框架不一样，这类纯O-R映射试图抹去对象的单表设计，用关系属性直接代替数据库表的外键列。*
>
>​	*EF为什么要这样设计？因为级联关系在数据库设计时往往表现为表之间的外键。和数据库设计一样，这种外键关系是后来附加的，在用PowerDesign设计数据库的过程中，这表现为两个Entity的一根连线。在数据库设计中，我们可以在不改动Entity本身的情况下，变更它们之间的关系（连线）。为什么到了java代码中，变更关系就要以修改java字段属性对列的映射的方式来实现？*
>
>​	*这种反传统的设计，目的还是为了还关系数据库以本来面目，正向前面多次提到的那样。为业务操作提供更大的灵活性——即要照顾到级联场合下的高效，也要照顾到一些简单功能时的便捷。*
>
>​	*此外，随着应用规模的扩大，应用设计可能会扩展为使用分区、分表、分库等大数据用法。在分库分表后，级联操作几乎就无法使用了。考虑到适应数据规模伸缩性的因素，作了这样的设计。*

### 5.1.3.  级联操作的效果

刚才说了单表操作方式得以保留的特点，现在来看级联模型下的用法。

src/main/java/org/easyframe/tutorial/lesson4/Case1.java

~~~java
@Test
public void testCascade() throws SQLException{
	School school=db.load(new School("天都中学"));
	int personId;
	{
		Person p = new Person();		
		p.setName("玄德");
		p.setCurrentSchool(school);

		db.insertCascade(p);
		personId=p.getId();
	}
	{
		//查出记录
		Person p=db.load(new Person(personId));
		System.out.println(p.getCurrentSchoolId()+":"+p.getCurrentSchool().getName());	
		//可以看到级联对象：School被查出来了
		assertEquals("天都中学",p.getCurrentSchool().getName());
			
		//更新为另一学校
		p.setCurrentSchool(new School("天都外国语学校"));
		//外国语学校是新增的，在更新语句执行之前会先做插入School表操作。
		db.updateCascade(p);
		System.out.println("天都外国语学校 = "
			               +p.getCurrentSchoolId()+"="+p.getCurrentSchool().getId());

		//再使用单表更新，更新回原来的学校
		p.setCurrentSchoolId(school.getId());
		db.update(p);
			
		//删除该学生
		db.deleteCascade(p);
	}
}
~~~

这一次发现被执行的SQL语句多了不少，由于最后记录被删除了，所以下面解说一下每个步骤对应的SQL操作。

~~~sql
select t.* from SCHOOL t where t.NAME=?
(1)name:         	[天都中学]
查出“天都中学”。对应代码中的: School school=db.load(new School("天都中学"));

select t.* from SCHOOL t where t.ID=?
(1)id:           	[1]
insert into T_PERSON(CURRENT_SCHOOL_ID,ID,PERSON_NAME) values(?,DEFAULT,?)
(1)currentSchoolId:	[1]
(2)name:         	[玄德]
级联插入，上面两句，对应代码中的db.insertCascade(p);，第一个select语句是在级联操作之前先检查“天都中学”是否存在，如果不存在会补充插入。

select T1.CURRENT_SCHOOL_ID AS T1__CURRENTSCHOOLID,
	T1.ID AS T1__ID,
	T1.PERSON_NAME AS T1__NAME,
	T2.NAME AS T2__NAME,
	T2.ID AS T2__ID
 from T_PERSON T1
 left join SCHOOL T2 ON T1.CURRENT_SCHOOL_ID=T2.ID
 where T1.ID=?
(1)id:           	[1]
级联查询，对应代码中的Person p=db.load(new Person(personId));

insert into SCHOOL(NAME,ID) values(?,DEFAULT)
(1)name:         	[天都外国语学校]
update T_PERSON set CURRENT_SCHOOL_ID = ? where ID=?
(1)currentSchoolId:	[4]
(2)id:           	[1]
级联更新，对应第一个db.updateCascade(p);//外国语学校是新增的，在更新语句执行之前会先做插入School表操作。

update T_PERSON set CURRENT_SCHOOL_ID = ? where ID=?
(1)currentSchoolId:	[1]
(2)id:           	[1]
非级联更新，对应代码中的db.update(p); 直接将schoolId更新为1.

select t.* from T_PERSON t where t.ID=?
(1)id:           	[1]
delete from T_PERSON where ID=?
(1)id:           	[1]
级联删除，看似除了做了一次不需要的查询以外别的啥也没做。实际上，查询操作的目的是为了得到当前数据库中的Person记录的各个键值。如果这些键值中有需要去删除别的表中记录的键（例如 OneToMany）那么就会去删除别的表中的记录。
本例中，Person到School是ManyToOne关系。这意味着School中的对象可能被其他记录所使用，因此不会去删除School表中的对象。
~~~

上面的例子，演示了级联操作的用法。还包括了混合了一次非级联更新操作。

由于在Person中，同时存在 currentSchoolId和currentSchool两个字段，这两个字段将分别在单表操作和级联操作中使用。（即单表字段和级联描述字段同时存在）。

某些场合下，人为操作可能使Person对象的currentSchoolId和currentSchool可能指向不同的关联记录。为了明确的区分当前用户是使用级联模型操作，还是单表模型操作，EF-ORM将级联插入、更新、删除的API和非级联下的API显式的分离。

|      | 非级联操作    | 级联操作            |
| ---- | -------- | --------------- |
| 插入记录 | insert() | insertCascade() |
| 更新记录 | update() | updateCascade() |
| 删除记录 | delete() | deleteCascade() |

                  					 表5-1 级联和非级联操作API的对比

我们再看例子中的这几句。

~~~java
//更新为另一学校
p.setCurrentSchool(new School("天都外国语学校"));
//外国语学校是新增的，在更新语句执行之前会先做插入School表操作。
db.updateCascade(p);
System.out.println("天都外国语学校 = "+p.getCurrentSchoolId()+"= "+p.getCurrentSchool().getId());
~~~

显然，在设置新的School对象到Person上之后，person.getCurrentSchoolId() 属性依然指向旧的School记录。但是更新操作完成后。除了刚刚被插入数据库的School对象中的id字段更新为数据库主键之外， Person对象中的外键值也被正确的更新了。

也就是说，虽然过程中Person对象短暂的出现指向School对象不一致的问题，但是在级联操作完成后，ID指向将会被正确的维护。

 通过分离的级联操作API，我们应该可以清楚的知道，插入数据库的值来自单表模型还是级联模型。

 级联操作API分离的另外一个好处是，由于级联下不支持分库分表。因此API的分离更容易在分库分表时被掌握和控制。

最后，可能有人会问，select和load操作默认都是级联的。这能关闭吗？看下面这个例子

~~~java
Person query=new Person();
query.setId(personId);
//设置为非级联查询
query.getQuery().setCascade(false);
p= db.load(query);
System.out.println(p.getCurrentSchool());
assertNull(p.getCurrentSchool()); 		//关闭级联开关后不做级联查询，所以School对象得不到了
~~~

因此，级联查询开关是可以关闭的。

## 5.2.  使用注解定义级联行为

这里说的注解即(Annotation)。

### 5.2.1.  仅引用级联对象的单个字段

我们考虑这样一个场景——

Person类中有一个gender的字段，描述用户性别，M表示男性，F表示女性。

另外有一张DATA_DICT表，其中记录了M=男性 F=女性的对应关系。

在查询时，我们希望查出”男”,”女”的属性。而不关心DATA_DICT表中的其他字段。

查询时，很多时候我们希望**只引用目标对象中的个别字段**，并不希望引用整个对象。实际上这种情况在业务中很常见。

@FieldOfTargetEntity注解就是为这种场景设计的。

我们定义的DATA_DICT对象如下：

~~~java
@Entity
@Table(name="data_dict")
public class DataDict extends DataObject {
    @Id
    @GeneratedValue
    private int id;
   
    @Column(name="dict_type")
    private String type;

    @Column(name="value")
    private String value;

    @Column(name="text")
    private String text;
    
	public enum Field implements jef.database.Field {
        id, type, value, text
     }
//Getter setter方法略
}
~~~

然后我们在Person对象中。定义一个“单字段”的引用字段。

~~~java
/**
 * 性别的显示名称“男”“女”
*/
@ManyToOne(targetEntity=DataDict.class)
@JoinColumn(name="gender",referencedColumnName="value")
@FieldOfTargetEntity("text")
private String genderName;
~~~

上例中，我们指定了“只引用目标对象中的 text字段”。

每次查询时，就像是访问Person表本身的属性一样。可以查出存储在另一张表中的用户性别的“男”，“女”这样的字样。

这种方式具有以下特点

1. 由于引用的字段较少，性能会比引用整个对象高很多。
2. 因为引用描述的不是一个完整对象。因此这种引用方式只对查询生效，对插入、更新、删除不会产生影响。

本节总结如下

@FieldOfTargetEntity

用于指定单字段引用。

| 属性    | 作用                                |
| ----- | --------------------------------- |
| value | String类型。字段名，表示仅引用目标entity中的指定字段。 |

 我们建议在EF-ORM中使用单字段引用，不再引用整个对象，意味着查询效率的提高。此外，如果相同的引用关系（比如引用目标对象的两个字段），EF-ORM会合并处理（在SQL语句中指定引用的两个字段），因此不会造成多余的数据库查询。

### 5.2.2.  @JoinDescription、@OrderBy

#### 5.2.2.1.  定义与作用

但是事实情况要比理想中的更为复杂，我们不会就为了存储一个 {M=男, F=女}这样的对应关系去设计一张表，现实中，往往会是这种情况——

DATA_DICT表中另外有一个type字段，当type=’USER.GENDER’时，才表示性别的对应关系。当 type等于别的值时，这些记录表示别的对应关系。（比如 0=’在线’ 1=’离线’，这样的关系）。

所以在orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4\Case1.java中，示例要更复杂一些。对genderName的定义是这样的——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4/ntity/Person.java

~~~java
/**
* 性别的显示名称“男”“女”
 */
@ManyToOne(targetEntity=DataDict.class)
@JoinColumn(name="gender",referencedColumnName="value")
@JoinDescription(filterCondition="type='USER.GENDER'")  //在引用时还要增加过滤条件	@FieldOfTargetEntity("text")
private String genderName;
~~~

通过增加@JoinDescription这样的注解，为SQL中的Join关系增加了一个过滤条件。

实际上查询的SQL语句变为(示意)

~~~sql
select  person.*, data_dict.text 
from   person 
       left join  data_dict on person.gender = data_dict.value and type=’USER.GENDER’
where
        ….
~~~

这样就起到了过滤其他类型的对应关系的效果。

实际示例代码如下

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4\Case1.java

~~~java
public void testGetFieldFromManyToOne() throws SQLException{
	//准备数据
	Person p1=new Person();
	p1.setName("孟德");
	p1.setGender('M'); 
	Person p2=new Person();
	p2.setName("貂蝉");
	p2.setGender('F');
	db.insert(p1);
	db.insert(p2);
		
	//查出数据
	Query<Person> query=QB.create(Person.class);
	query.addCondition(QB.notNull(Person.Field.gender));
	query.orderByAsc(Person.Field.gender);
	List<Person> p=db.select(query);
	assertEquals("女人", p.get(0).getGenderName());
	assertEquals("男人", p.get(1).getGenderName());
}
~~~

上面介绍了@JoinDescription组合的用法。@JoinDescription用来描述多表关联关系时一些额外的特征与特性。

虽然上面的例子中同时使用了@FieldOfTargetEntity和@JoinDescription，但两者各有各的作用，并不一定要组合使用。@JoinDescription和@FieldOfTargetEntity使用上没有必然联系。

@ JoinDescription

| 属性              | 作用                                       |
| --------------- | ---------------------------------------- |
| type            | 枚举常量jef.database.annotation.JoinType。  LEFT   左外连接  RIGHT 右外连接  INNER 内连接  FULL  全外连接 |
| filterCondition | JPQL表达式，表示Join时额外的条件。表达式中可以包含参数变量。       |
| maxRows         | 当对多连接时，限制结果记录数最大值。                       |

#### 5.2.2.2.  控制级联对象的排序和数量

上表中提到了JoinDescription中的另外属性，我们举例说明

~~~java
@OneToMany(targetEntity = ExecutionLog.class, fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
@JoinColumn(name = "id", referencedColumnName = "taskId")
@JoinDescription(maxRows = 10)
@OrderBy("executeTime desc")

private List<ExecutionLog> lastExecutionLog; //最后10条Execution Log
~~~

上例表示一个指向ExecutionLog的一对多引用。但是对应的ExecutionLog有很多条，我们只取**执行时间最近的10条**。之前的记录不会查出来。

JPA注解@OrderBy可以用于控制级联关系的排序。@OrderBy("execute_time desc")表示对于executeTime字段进行倒序排序。

如果maxRow=1，那么这个映射将只对应最后一条ExecutionLog。那么数据类型可以进一步简化——

~~~java
@OneToMany(targetEntity = ExecutionLog.class, fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
@JoinColumn(name = "id", referencedColumnName = "taskId")
@JoinDescription(maxRows = 1)
@OrderBy("executeTime desc")
private ExecutionLog lastExecutionLog;   //最后一条Execution Log，不使用集合类型。
~~~

最后一条ExecutionLog是单值的，可以不使用集合类型。

#### 5.2.2.3.  在FilterCondition中使用变量

某些时候，我们希望FilterCondition中的表达式中出现的不是固定的常量，而是运行时得到的变量。

比如前面那个 “M=男 F=女”的转换例子，如果数据字段中的映射名称并不总是” USER.GENDER”，而是一个变化的值。那该怎么办呢？

~~~java
/**
* 性别的显示名称“男”“女”
 */
@ManyToOne(targetEntity=DataDict.class)
@JoinColumn(name="gender",referencedColumnName="value")
@JoinDescription(filterCondition="type=:dictType")  //将条件设置为变量
@FieldOfTargetEntity("text")
private String genderName;
~~~

在执行查询时——

~~~java
Query<Person> query=QB.create(Person.class);
query.addCondition(QB.notNull(Person.Field.gender));
query.orderByAsc(Person.Field.gender);
query.setAttribute("dictType", "USER.GENDER");  //为FileterCondition中的变量赋值
List<Person> p=db.select(query);
~~~

在运行时就会将其作为绑定变量处理。

这种用法适用于以下两种场景——

* Join过滤条件中存在不确定的值时
* SQL语句中必须使用绑定变量时

### 5.2.3.  其他JPA注解的支持

这一节，我们不介绍用法，而是针对级联时的操作行为进行一些分析

#### 5.2.3.1.  延迟加载	

我们修改一下Person.java，将Person.java中的currentSchool定义改成下面这样。

~~~java
/**
* 学校映射
*/
@ManyToOne(targetEntity = School.class,fetch=FetchType.LAZY)  //显式指定该字段为延迟加载的。
@JoinColumn(name = "currentSchoolId", referencedColumnName = "id")
private School currentSchool;
~~~

再次运行下面的测试案例

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4\Case1.java

~~~java
@Test
public void testLazyLoad() throws SQLException{
	Person query=new Person();
	query.setId(firstId);
	Person p=db.load(query);
	System.out.println("接下来观察调用get方法后，才会输出加载School的SQL语句。");
	p.getCurrentSchool();
	//请观察输出的SQL语句，
}
~~~

通过输出可以观察到，启用了Lazy的Fetch方法之后，Person对象从数据库中查出时，School对象是未赋值的。只有当调用了getCurrentSchool()之后，才会去数据库中加载School对象。

在JPA定义中，fetch属性用于指定级联加载的行为。

| fetch属性取值       | 效果                                      |
| --------------- | --------------------------------------- |
| FetchType.EAGER | 饥渴的。查询时立刻加载级联对象                         |
| FetchType.LAZY  | 懒惰的。只有当属性被使用时（调用get方法），才会去加载级联对象。（延迟加载） |

JPA的四种级联关系中，默认的加载行为是不同的。

| 类型   | 注解           | 缺省的fetch行为 |
| ---- | ------------ | ---------- |
| 一对一  | @ OneToOne   | EAGER      |
| 一对多  | @ OneToMany  | LAZY       |
| 多对一  | @ ManyToOne  | EAGER      |
| 多对多  | @ ManyToMany | LAZY       |

如果不使用延迟加载，EF-ORM也会使用“外连接获取”的方式来减少数据库操作次数。见下一节。

#### 5.2.3.2.  外连接获取

从5.1.3的例子中，我们可以从日志观察到：当Person对象中有两个多对一的引用时，在查询Person时，实际SQL语句如下

~~~sql
select T1.ID			 as T1__ID,
	  T1.PERSON_NAME	 as T1__NAME,
	  T1.CURRENT_SCHOOL_ID as T1__CURRENTSCHOOLID,
	  T1.GENDER			 as T1__GENDER,
	  T2.TEXT			 as T2__TEXT,
	  T3.NAME			 as T3__NAME,
	  T3.ID				 as T3__ID
 from T_PERSON T1
   left join DATA_DICT T2 ON T1.GENDER=T2.VALUE and T2.DICT_TYPE='USER.GENDER'
   left join SCHOOL T3 ON T1.CURRENT_SCHOOL_ID=T3.ID
 where T1.ID=?  
~~~

也就是说，在查询Person表中的记录时，将对应的School对象和性别“男/女”的显示问题都一次性的查了出来。最后形成的是并不是一个简单的单表查询语句，而是一个多表联合的Join语句。为了防止Join无法连接的记录（比如未对应School的Person记录）被过滤，因此使用了左外连接(Left Join)。

一般来说，关联查询可以通过多次数据库查询完成，也可以用上面那样较为复杂的SQL语句一次性从数据库中查询得到，这种操作方式被称为外连接查询。

这样想，如果查询出10个Person。如果不使用外连接，那么我们还需要执行10次SQL语句去获得每个Person对象的School。但使用外连接后，一次数据库操作就能代替原先11次操作。

外连接查询只能应用与ManyToOne和OneToOne的级联关系中，并能有效的减少数据库访问次数，在某些场合下具有较好的性能。由于JPA注解中OneToOne和ManyToOne的缺省FetchType都是EAGER，即不使用延迟加载。此时单次SQL操作性能更好，因此这也是EF-ORM的默认实现方式。

如果你希望沿用原先执行多次单表查询的方式，可以使用setCascadeViaOuterJoin(false)方法。如下——

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4\Case1.java

~~~java
@Test
public void testNonOuterJoin() throws SQLException{
	Person query=new Person();
	query.setId(firstId);
	query.getQuery().setCascadeViaOuterJoin(false); //改变默认行为，不使用外连接。
	Person p=db.load(query);
	p.getCurrentSchool();
	p.getGenderName();
	//请观察输出的SQL语句，
}
~~~

除此之外，从5.2.3.1的内容也可以知道，使用JPA注解显式声明为LAZY，也可以使得级联关系从外连接转变为多次加载。

因此我们总结如下

​ 1. 当FetchType标记为LAZY时，所有级联 关系都通过多次的单表查询来实现。

​ 2. 当FetchType为EAGER时，OneToOne和ManyToOne可以优化为使用外连接单次查询。OneToMany和ManyToMany无法优化。

​ 3. 如果调用setCascadeViaOuterJoin(**false**)方法，或者配置全局参数 db.use.outer.join=false（见附录一），那么EF-ORM将放弃使用外连接优化。此时性能问题依赖于一级缓存和延迟加载来解决。

#### 5.2.3.3.  级联方向

EF-ORM中，级联关系是针对单个对象的辅助描述，因此所有的级联关系维护都是单向的。上面的例子中，如果我们删除 School表中的数据，不会引起Person数据的任何变化。

如果我们想要在删除School时，级联删除该School的所有学生，那么需要修改School.java这个类。

修改后的School类如下

~~~java
/**
 * 实体:学校
 * 描述一个学校的ID和名称
 */
public class School extends DataObject{
	@Column
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	
	@Column(length=64,nullable=false)
	private String name;

	public enum Field implements jef.database.Field{
		id,name
	}
	@OneToMany
	@JoinColumn(name="id",referencedColumnName="currentSchoolId")
	private List<Person> persons;
	//getter setter省略
}	
~~~

当配置了从School到Person的关系后，如果级联删除School对象，则也会删除对应的Person对象。

上面的配置中，@JoinColumn注解可以省略。如果未配置Join的路径，EF-ORM会到目标对象中去查找反向关系。

因此最简的配法是

~~~java
@OneToMany
private List<Person> persons;
~~~

### 5.2.4.  定制级联行为

#### 5.2.4.1.  四种级联关系的行为

我们来了解EF-ORM在使用级联方法 (insertCascade()、 updateCascade() deleteCascade()等方法 )操作时，实际执行了什么动作。

在EF-ORM中，所有的四种级联关系，都是两表间的关系，不会出现第三张表，包括多对多(@ManyToMany )。事实上这也使ManyToMany的作用和OneToMany其实差不多，因此实用性不大。

这四种级联都对应一些简单的操作步骤，而且并没有提供太多的定制配置。对熟悉H框架的人来说，EF-ORM的级联行为和某H框架并不完全一致。EF-ORM希望设计出一些一刀切的简单规则来处理级联问题，从而降低ORM框架的使用难度。

一个典型的示例就是mappedby属性在EF-ORM中是无效的，EF-ORM也不会为OneToMany生成一张所谓中间表这样的设计。此外，下面的示例在某H框架中是不能删除子表对象的，但在EF-ORM中可以做到。

父表的定义

 orm-tutorial/src/main/java/org/easyframe/tutorial/lesson5/entity/Catalogy.java

~~~java
@Entity
public class Catalogy extends DataObject {
    @Id
    @GeneratedValue
    private int id;

    private String name;

    public enum Field implements jef.database.Field {
        id, name
    }
    
	@OneToMany(mappedBy="catalogyId")
    //@JoinColumn(name="id",referencedColumnName="catalogyId")
	private List<Item> items;

	//getter setter省略
}
~~~

这里顺便提一下——当使用主键和其他实体关联时，可以将@JoinColumn(name="id",referencedColumnName="catalogyId")简化为mappedBy="catalogyId"。两者是等效的。

 子表的定义

orm-tutorial/src/main/java/org/easyframe/tutorial/lesson5/entity/Item.java

~~~java
@Entity()
public class Item extends DataObject {
    @Id
    @GeneratedValue
    private int id;
    private String name;
    private int catalogyId;
 
   public enum Field implements jef.database.Field {
        id, name, catalogyId
	}
	//getter setter省略
}
~~~

运行下面的代码——

orm-tutorial/src/main/java/org/easyframe/tutorial/lesson5/Case1.java

~~~java
public void testCascade() throws SQLException{
	Catalogy c=new Catalogy(); //Catalogy表是父表
	c.setId(1);
	c=db.load(c);

	assertNotNull(c);
	assertEquals(4, c.getItems().size()); //Item表是子表

	c.setItems(null);
	System.out.println("设置为null，调用级联更新，会删除子表记录");
	Transaction tx=db.startTransaction();
	tx.updateCascade(c);
	tx.rollback();
}
~~~

在EF-ORM中，四种级联关系在增、删、改的时候执行的动作是固定的，下表列出了四种不同级联情况下框架执行的操作。

其中——当前对象指配置了级联关系的对象（如本例的Catalogy对象），关联对象指被引用的对象（本例的Item对象）。

| 关系类型       | insertCascade                            | updateCascade                            | deleteCascade        |
| ---------- | ---------------------------------------- | ---------------------------------------- | -------------------- |
| OneToOne   | 1 插入当前对象。  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。 | 1 更新当前对象  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。  如为不再使用则删除 | 1 删除当前对象  2 再删除关联对象。 |
| OneToMany  | 1 插入当前对象。  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。 | 1 更新当前对象  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。  如为不再使用则删除 | 1 删除当前对象  2 再删除关联对象。 |
| ManyToOne  | 1 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新;  2 插入当前对象。 | 1 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新  不做级联删除  2 再更新当前对象 | 1 删除当前对象  不做级联删除。    |
| ManyToMany | 1 插入当前对象。  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。 | 1 更新当前对象  2 检查关联对象——  如为新对象则插入  如为旧对象且变化则更新。  不做级联删除 | 1 删除当前对象  不做级联删除。    |

上表列出了四种不同的映射关系下的行为，可以发现基本上各个行为都只分为维护对象本身和维护级联关系两种。

上表中不同的行为特点是根据大量业务实际情况推导出的。例如：多对一这种关系不做级联删除，这也是考虑到大多数多对一的场景下，被引用的对象一般是公用的。

另外要注意，级联操作是递归的。比如——

A对象@OneToMany到B对象， B对象OneToOne到C对象，在对A对象级联操作的同时，会按上述规则一直维护到C对象为止。如果是更多的对象参与级联，那么以此类推。

上述行为中ManyToMany和某H框架差别较大，正常情况下，我们维护多对多关系总是需要一张中间表。因此某H框架提供了隐含的中间表实现。即两个实体、一个关系，总共三张表。EF-ORM早期几个版本也是这样设计的，但是后续版本中去除了这种设计。

>*因为我们发现，自动维护的中间表不容易扩展。比如：多个考生参加多项课程的考试，获得分数*
>
>*这样一个典型的多对多关系。*
>
>*很显然，在关系表上我们需要增加 分数列、然后我们需要描述这个关系的创建时间——考试日期等等。*
>
>*从业务实践看，单纯的只有两个实体的主键构成的关系实用性很低，而且不利于后续的扩展。*
>
>*早期EF-ORM的设计还不是很清晰的时候，ManyToMany这种表面上是两表关系，实际上是三表关系的设计，极大的增加了框架的复杂度。为此，在权衡复杂度和实用性之后，EF-ORM在0.9版本中去除了这种设计。*

那么，如果我们碰到有类似于E-R-E这样的多对多关系时，该怎么做呢？更倾向于设计三个实体，（例如 学生、课程、考试）分别维持以下关系——

~~~java
class 学生{
	@OneToMany(target = 考试)
}

class 考试{
	@ManyToOne(target= 课程)
	@ManyToOne(target= 学生)
}

class 课程{
	@OneToMany(target=考试)
}
~~~

也就是说，多对多关系被拆分为两个一对多关系。

这是不是说EF-ORM中的@ManyToMany就没有用了呢？不是这样的，再举一个例子。

某学校有10个班，每个班可以有多个学生和多个任课老师。

**教师任课表**

| 教师ID | 班级ID | 课程ID |
| ---- | ---- | ---- |
|      |      |      |

**学生表**

| 学生ID | 班级ID | 姓名   | 性别   | 年龄   |
| ---- | ---- | ---- | ---- | ---- |
|      |      |      |      |      |

在这两个实体中，我们就可以建立双向@ManyToMany的关系。从学生记录中，获得该学生所在班级的所有课程和任课教师。从教师担任的课程的记录中，获得该班级所有的学生记录。

我们怎么区分什么时候该使用@OneToMany什么时候使用@ManyToMany呢？可以遵循这样的原则：

* 当目标实体依赖于当前实体（强关联）时使用OneToMany；
* 当目标实体相对独立     （弱关联）时使用ManyToMany。

弱关联情况下，级联操作将会使用一种较为保守的策略，来避免出现删除关联对象之类的误操作。

关于这个ManyToMany的模型，可以参阅orm-tutorial/src/main/java/org/easyframe/tutorial/lesson5/

Case1.java中的testManyToMany方法。本文不再赘述。

#### 5.2.4.2.  使用注解限制级联关系的使用场合

上面提到了，级联关系是递归维护的。而且要注意，一个对象中可能存在多种级联关系，而其级联对象中又可能有多种级联关系。在递归情况下，情形可能会变得相当复杂。如果没有很好的设计你的E-R关系，你可能会发现，一个简单的数据库操作可能会衍生出大量的级联操作语句。

因此，JPA中可以限制级联关系的使用范围。使用JPA注解，使级联关系只在插入/更新/删除等一种或几种行为中生效。简而言之，我们可以让配置的级联关系只用于查询操作、或者只用于插入操作。

在@OneToOne @OneToMany @ManyToOne @ManyToMany中，可以设置cascade属性如下

~~~java
@ManyToOne(targetEntity = School.class,cascade={CascadeType.MERGE,CascadeType.REFRESH})
~~~

cascade是一个多值属性，配置了以后，则表示此类操作要进行级联，否则就不作级联操作。例如上例配置该字段仅用于级联查询和级联插入更新，不用于级联删除。

| **CascadeType**     | **对应**    |
| ------------------- | --------- |
| CascadeType.PERSIST | 允许级联插入    |
| CascadeType.MERGE   | 允许级联插入或更新 |
| CascadeType.REMOVE  | 允许级联删除    |
| CascadeType.REFERSH | 允许级联查询    |
| CascadeType.ALL     | 允许所有级联操作  |

### 5.2.5.  级联条件CascadeCondition

在级联查询中，我们还能在级联查询的对象上添加条件。请看下面这个例子：

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson4\Case1.java

~~~java
@Test
public void testCascadeCondition() throws SQLException{
	Query<Person> query=QB.create(Person.class);
	query.addCondition(QB.eq(Person.Field.id,firstId));
	query.addCascadeCondition(QB.matchAny(School.Field.name, "清华"));
	db.select(query); 
}
~~~

在这个例子中，使用addCascadeCondition()方法，在查询中增加了一个专门针对级联对象的过滤条件。在查询级联时中，只有带有”清华“字样的学校才会被查出。

另一个例子是在多对多时

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson5\Case1.java

~~~java
@Test
public void testCascadeCondition() throws SQLException {
	{//无级联条件时
		Student s1 = db.load(Student.class,1);
		for(TeacherLesson t:s1.getLessons()){
			System.out.println(t);
		}	
	}
	{
		Student st=new Student();
		st.setId(1);
		st.getQuery().addCascadeCondition(QB.in(TeacherLesson.Field.lessonName
										, new String[]{"语文","化学"}));
		for(TeacherLesson t:db.load(st).getLessons()){
				System.out.println(t);
		}	
	}
		
}
~~~

可以发现，即使是在延迟加载的场景下，级联条件依旧会在查询时生效。

​由于级联是递归的，即级联对象中我们还可以获得其他级联对象，因此过滤条件也不仅仅针对当前对象的，而是可以灵活的指定。我们再看复杂一点的例子

orm-tutorial\src\main\java\org\easyframe\tutorial\lesson5\Case1.java

~~~java
/**
 * 级联过滤条件也可以用于间接的引用中，
 * 本例中， Cacalogy引用Item、Item引用ItemExtendIndo，通过指定引用字段，可以精确控制过滤条件要用于那个对象上。
 */
@Test
public void testCascadeCondition2() throws SQLException {
	Query<Catalogy> q = QB.create(Catalogy.class);
	q.addCondition(QB.eq(Catalogy.Field.id, 1));
     //指定过滤条件作用于 Catalogy的一个间接级联关系上
	q.addCascadeCondition("items.itemExtInfos", QB.eq(ItemExtendInfo.Field.key, "拍摄地点"));
	Catalogy c = db.load(q);
	for (Item item : c.getItems()) {
		System.out.print(item.getItemExtInfos());
	}
   //注意观察SQL语句
}
~~~

Cascade条件一旦设置到一个查询中，无论级联操作是单次操作还是多次操作，都会生效。
