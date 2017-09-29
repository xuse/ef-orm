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

## 14.2.  内置连接池

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
