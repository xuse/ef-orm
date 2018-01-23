GeeQuery使用手册——Chapter-14  其他功能与特性

[TOC]

# Chapter-14  其他功能与特性

这里列举一些较难归类的，或者是框架最近新增的特性。这些特性往往能在针对性的环境下简化开发，解决开发中的疑难问题。

## 14.1.  记录时间戳自动维护

即自动维护每条记录的create-Time字段和update-time字段。

我们可能有这种场景，当数据插入数据库时，希望在某个日期时间型字段中写入当前数据库时间。在每次数据更新时，希望在某个日期时间型字段中写入当前的数据库时间。这其实就是常用的日期时间维护操作。用对象关系映射很难实现这种功能。同时由于需要设置的是数据库时间而不是当前java环境时间，这也给功能的实现造成了一定困难。

EF-ORM 1.9.1.RELEASE开始，原生支持此类操作。简单说来，在日期字段的注解上加上@GeneratedValue，则EF就会自动维护这些日期字段。

~~~java
/**
* 为一个日期列添加@GeneratedValue后，如果在插入时没有指定数据，
* 将使用sysdate / current_timestamp填入(具体函数取决于使用的数据库)
*/
@GeneratedValue(generator="created")
@Column(name="create_time")
private Date created;

/**
* 为一个日期列添加@GeneratedValue后，如果在插入或者更新时没有指定数据
* 将使用sysdate / current_timestamp填入(具体函数取决于使用的数据库)
*/
@GeneratedValue(generator="modified")
@Column(name="last_modified")
private Date modified;
~~~

一旦进行了上述配置，就会发现在insert或update语句中，会出现sysdate或current_timestamp等函数，直接获取数据库时间进行操作。

不过这种方式下，数据库时间不会回写到插入或更新的对象中。您需要再执行一次查询操作，才能得到刚刚写入数据库的记录的时间。

另外一种变通的方法是，在插入之前自动为字段赋值，此时的时间为当前java服务器的时间。这种方式下由于是先赋值再插入的，在操作完成后，可以从对象中获得刚才操作的时间。EF-ORM也支持这种方式。

~~~java
@GeneratedValue(generator="created-sys")  //creatd-sys的意思是插入时取当前系统时间
@Column(name="create_time")
private Date created;

@GeneratedValue(generator="modified-sys") //creatd-sys的意思是插入时或者更新时取当前系统时间
@Column(name="last_modified")
private Date modified;
~~~

 使用以上注解后，数据记录的插入和更新时间就可以交由框架自动维护了。

## 14.2. 数据初始化

数据初始化功能一直都有，但一直到1.12.x才完善并提供使用手册。

### 14.2.1 使用csv格式记录初始化数据

我们首先准备一个Entity定义如下

```java
package sample.geequery.domain;
@Entity
public class City extends DataObject implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private String state;

	private String country;

	public enum Field implements jef.database.Field {
		id, name, state, country
	}
```

然后准备一个CSV文件 (扩展名改为txt) ，**sample.geequery.domain.City.txt**，放在项目的src/main/resource路径下。（即编译后的classpath路径下，文件名要和Entity类的全名一致。扩展名为txt）

举例如下：

```csv
[id],[name],[state],[country]
1,上海,上海,中国
2,杭州,浙江,中国
3,北京,北京,中国
4,绍兴,浙江,中国
5,南京,江苏,中国
6,天津,天津,中国
```

在SessionFactory的启动配置中，配置initData=true（根据启动方式的不同，有好多种配置方法，此处以最新的Spring-boot方式为例），在**application.properties**中加上

```
geequery.initData=true
```

如果是传统的Spring XML配置方式如下

```xml
	<bean id="entityManagerFactory" class="org.easyframe.enterprise.spring.SessionFactoryBean"
		p:dataSource-ref="dataSource" p:packagesToScan="sample.myentities"
		p:initData="true"  	/>
```

在启动并扫描Entity之后，就会自动开始初始化数据。日志中出现——

如果表是首次创建，将会插入所有数据，日志如下所示（举例）

```
2018-01-22 16:03:33,594 main[INFO ] DataInitializer - Table [City] was created just now, begin insert data into database.
...
2018-01-22 16:03:33,751 main[INFO ] DataInitializer - Table [City] dataInit completed. 6 records inserted.
```

如果表早已存在，将会尝试合并数据，日志如下所示（举例）

```
2018-01-22 15:49:32,649 main[INFO ] DataInitializer - Table [City] already exists, begin merge data into database.
...
2018-01-22 15:49:32,778 main[INFO ] DataInitializer - Table [City] dataInit completed. 3 records saved.
```

>  数据合并：数据合并是一个较为复杂的操作，其方式为查找旧的相同记录，如果没有则插入新记录。如果找到旧记录，那么会修改数据库中已有的记录。
>
> 查找和修改的依据是表的主键。

一般来说我们推荐一次性初始化，此时会会采用Batch方式写入数据，对一次性初始化大量数据的场景有巨大的性能提升。而合并数据模式下将对每条数据进行比较合并，此时性能会差一些。

### 14.2.2 相关配置

#### 14.2.2.1 扩展名

可能有人会问，既然是CSV格式的文件，扩展名用csv不就好了，为什么要用txt呢？如果您希望扩展名用csv，可以这样配置，设置initDataExtension=csv

如果使用spring-boot进行配置，那么在**application.properties**中配置：

```properties
#初始化文件扩展名，默认值是txt
geequery.initDataExtension=csv
```

如果用传统XML方式配置，那么也是设置该属性——

```xml
<bean id="entityManagerFactory" class="org.easyframe.enterprise.spring.SessionFactoryBean"
		p:dataSource-ref="dataSource" p:packagesToScan="sample.myentities"
		p:initData="true"  p:initDataExtension="csv" />
```

启用上述配置后，上例中的初始化文件名就应该改为——**sample.geequery.domain.City.csv**。扩展名要使用什么，具体看使用者喜好而定。

#### 14.2.2.2 扩展数据字符编码

同上。设置 initDataCharset即可。 eg.

```properties
#初始化文件编码，默认值为UTF8
geequery.initDataCharset=GB18030
```

### 14.2.3 使用数据表来控制初始化行为

在项目上线后，对数据表的更改是一件非常需要小心的事情，甚至有时在开发或测试过程中，如果每次都尝试对表中的数据进行合并，除了影响性能之外，对初始化数据的不当维护还有可能产生脏数据。

有没有办法控制数据初始化的行为，让它只在需要的时候运行呢？

办法是有的，框架支持在数据库中创建一张表作为标记，具体用法如下：设置属性useDataInitTable=true。

如果使用spring-boot进行配置，那么在**application.properties**中配置：

```
#启用初始化控制表
geequery.useDataInitTable=true
```

如果用传统XML方式配置，那么也是设置该属性——

```xml
<bean id="entityManagerFactory" class="org.easyframe.enterprise.spring.SessionFactoryBean"
		p:dataSource-ref="dataSource" p:packagesToScan="sample.myentities"
		p:initData="true"  p:useDataInitTable="true"	/>
```

开启该配置后，系统启动后会自动创建表——**allow_data_initialize**，该表数据可能如下

| 列    | do_init                  | last_data_init_time | last_data_init_user                      | last_data_init_result                 |
| ---- | ------------------------ | ------------------- | ---------------------------------------- | ------------------------------------- |
| 数值   | 0                        | 2018-01-22 16:03:33 | 10440@PC-XXX(10.10.105.105) OS:windows 7 | success. Tables init = 1, records = 6 |
| 含义   | 为1时下次启动自动初始化数据，为0时不进行初始化 | 上次执行初始化的时间          | 上一次初始化执行的服务器主机                           | 上一次初始化执行结果                            |

每次初始化任务执行完成后，都会将do_init从1改为0。
系统首次安装使用时，这张表还不存在，因此肯定会执行数据初始化，但是执行一次后，就不会再执行了。只有**人工将该字段设置为1**，才能让初始化任务再度执行。这样，数据库初始化的行为就受到使用者的严格控制，用户再也不用担心初始化程序随意修改数据了。

### 14.2.4 使用注解定义初始化行为

在Entity类上增加注解 @InitializeData，可以定义该张表的初始化行为，具体功能可以看Java-doc。

#### 1.14.3.1 自增序号问题

自增需要和数据合并有关，正常情况下，如果是新建的表，其自增肯定是从1开始的。但在表合并时，使用表的自增功能不能保证插入的记录的id和初始化文件中的id一致。比如下面的例子中——

```c
[id],[name],[state],[country]
1,上海,上海,中国
2,杭州,浙江,中国
3,北京,北京,中国
4,绍兴,浙江,中国
5,南京,江苏,中国
6,天津,天津,中国
```

实际插入数据库的“上海”这条记录，其id可能不是1，而是20或者其他更大的值。这产生一个问题，当下次执行数据初始化时，GeeQuery试图进行数据合并。

```SQL
--数据库中已有一条记录
-- id=20, name='上海'， state='上海'  country='中国'
--GQ先查询数据库中有无id为1的记录
select * from city where id = 1;
--如果没有该记录
insert into city (name,state,country) value ('上海','上海','中国');
--结果产生了一条 id=21的记录，“上海”。
--这种情况下，每次合并记录都会不停的产生重复的数据。
```

为了解决这个问题，我们可以要求自增值固定。也就是说插入的记录的id和数据初始化文件中的一致。

我们可以这样做——

```java
@Entity
@InitializeData(manualSequence = true)
public class City extends jef.database.DataObject implements Serializable {
	@Id
	@GeneratedValue
	private Long id;
```

manualSequence = true的意思就是手动指定自增序号（数据库本身要支持）。此时每次插入的记录的自增ID会严格的按照初始化文件中的数值来定义。

#### 1.14.3.2 按业务键进行数据合并

在上面的问题中，除了固定ID以外，还有一个方法，就是按业务主键作为合并用的主键。在上面的例子中，对于City表，id只是一个物理主键，我们假定每个省只有一座同名的城市。因此 (NAME, STATE) 两个列可以构成一个联合主键，这个主键就是该表的业务主键。

```java
@Entity
@InitializeData(mergeKeys={"name","state"})
public class City extends jef.database.DataObject implements Serializable {
```

作了上述配置后，数据合并的规则就不再按照物理主键来进行，而是按照业务主键来进行数据合并了。

#### 1.14.3.3 其他配置

@InitializeData中还有多项其他配置，详见Java-Doc

| 属性               | 类型       | 用途                                       |
| ---------------- | -------- | ---------------------------------------- |
| value            | String   | 配置一个资源文件名，记录了所有的初始化记录值 如果不指定，默认使用 classpath:/{classname}+全局扩展名 例如：配置为 |
| enable           | boolean  | 启用该表的自动初始化，可配置成false禁用                   |
| mergeKeys        | String[] | 如果合并不是按主键进行，这里填写表的业务主键（name of java field） |
| manualSequence   | boolean  | 自增键使用记录中的值，不使用数据库的自增编号。                  |
| charset          | String   | 数据文件的字符集                                 |
| ensureFileExists | boolean  | 开启后会检查初始化数据文件必需存在，如果不存在将抛出异常             |
| sqlFile          | sqlFile  | 指定一个SQL脚本文件，执行该SQL来更新数据库。 一旦使用该功能，value()定义的CSV初始化功能将失效。 |

## 14.3.  内置连接池

EF-ORM内置了一个稳定高效的连接池（但功能较弱）。正常情况下，EF-ORM会检测用户的DataSource是否使用了连接池。如果用户已经使用了连接池，那么EF-ORM不会启用内置连接池。如果检测到用户没有使用连接池，EF-ORM就会启用内置连接池。

内置连接池可以在额定连接数到最大连接数之间变化。定期关闭那些闲置的过多的连接。内置连接池可以定期检查空闲连接的可用性。在取出的连接使用中，如果捕捉到SQLException，并且数据库方言判定这个Exception是由于网络断线等IO错误造成的，会强行触发连接池立刻检查，清除无效连接。并且连接池会不断尝试重新连接，试图从故障中恢复。

EF-ORM启动时，都有一条info日志，如果启用内置连接池，日志如下

~~~
There is NO Connection-Pool detected in data source class {DataSource类名}, EF-Inner Pool was enabled.
~~~

如果没有启用连接池，日志如下

~~~
There is Connection-Pool in data source {DataSource类名}, EF-Inner Pool was disabled.
~~~

大部分情况下，不需要您关心内置连接池是否开启。自动判断连接池能准确的识别DBCP, C3p0,proxool, Driuid, Tomcat cp, BoneCp等众多的连接池。如果您使用了较为冷僻的连接池造成两个连接池都被启用，您可以使用jef.properties参数（见附录一）强行关闭EF的内置连接池。

内置连接池相关参数配置可见附录一的“内置连接池相关”部分。

​EF内置连接池采用了无阻塞算法，并发下安全，存取非常高效，有定期心跳，拿取检查，脏连接丢弃等连接池的基本功能，但没有PreparedStament缓存等功能。也曾在某大型电信中稳定使用，但现在是连接池层出不穷的年代，c3p0和proxool等老牌连接池都在druid、jboss等连接池面前相形见绌。而内置连接池不是EF-ORM今后发展的目标，您在小型项目中或者快速原型的项目用用没什么问题，大型商业项目建议您还是用tomcatcp或者druid吧。
