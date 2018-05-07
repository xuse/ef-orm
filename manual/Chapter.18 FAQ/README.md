GeeQuery使用手册——Chapter-18  常见问题 (FAQ)

[TOC]

# Chapter-18  常见问题 (FAQ)

### 18.1.1.   JDBC驱动问题

EF-ORM在很多DDL处理和函数改写等特性上，使用了JDBC4的一些功能，因此也要求尽可能使用支持JDBC4的数据库驱动，比如Oracle驱动请使用ojdbc5或者ojdbc6。其他驱动选择也请尽可能用最新的。

### 18.1.2.  JDK7编译后的ASM兼容

在1.8.0版本中，如果使用JDK7的编译版本编译了Entity，那么类在增强后无法在Java 7的版本下使用。这是因为JDK7使用了新版本的编译和类加载机制，将类型推断移到编译时进行，此时ASM修改后的类由于缺少类型推断信息故不能被JDK7加载。解决这个问题的办法是，在虚拟机启动参数中加上

~~~
-XX:-UseSplitVerifier
~~~

EF-ORM 1.8.8以后的版本，在实体增强时会将类版本降低到50(对应JDK6)，因此不会发生此错误。

### 18.1.3.  数据库存储的口令加密

可以配置加密后的数据库口令作为密码，EF-ORM在查询数据源时，会尝试解密。默认解密采用3DES算法，密钥可以在jef.properties中配置或者通过虚拟机启动参数指定。

### 18.1.4.  Oracle RAC环境下的数据库连接

默认在Oracle RAC下，使用Oracle驱动的FCF或者OCI驱动的TAF方式进行连接失效转移。（不明白什么是FCF和TAF的请自行百度）。这两种方式将会使用Oracle驱动的连接池，因此可以手工关闭EF-ORM的内建连接池。

~~~properties
Db.no.pool=true
~~~

实际上，使用JTA事务等场景下，也要关闭内置连接池，不过此时关闭内建连接池是自动进行的。

### 18.1.5.  某些正确的SQL语句解析错误怎么办

目前的词法分析器经过大量SQL语句的测试，都能正确解析。一个已知的问题是，JPQL参数名称不能取SQL关键字作为名称，比如”select * from t where id=:top” 、“update t set name=:desc”等，这些语句都会解析失败，因为top，desc等是SQL关键字。因此，如果碰到解析错误的SQL，尝试先改变一下JPQL参数的名称，是不是用到了SQL关键字。任何复合词都不是SQL关键字。

如果确实有解析不了的SQL语句，请用7.5节的方法，直接使用原生SQL语句。当然原生SQL语句不具备分库分表等高级功能，您需要自行处理。

### 18.1.6.  使用Jackjson做JSON序列化怎么办

Jackson是常见的JSON序列化工具，Jackson默认的序列化策略中会将DataObject中的Query，UpdateValueMap取出来进行序列化，这往往不是我们需要的。

为此，我们可以使用以下两种方法来避免出现这种情况——

方法一：只有Getter和Setter齐全的字段才被序列化。为ObjectMapper增加配置即可——

 Jackson 1.x

~~~json
objectMapper.configure(Feature.REQUIRE_SETTERS_FOR_GETTERS, true);
~~~

 Jackson 2.x

~~~
objectMapper.configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true);
~~~

方法二：忽略掉不希望被序列化的字段

在Entity类上增加注解

~~~
@JsonIgnoreProperties({"query","updateValueMap"})
public class MyClass{
  …
}
~~~

这两个方法任选其一，可以在Jackson下正常序列化Entity。

### 18.1.7 SQLite下JDBC驱动的日期时间字符串格式问题

当使用JDBC连接SQLite数据库时，可能会碰到如下错误。

````
Caused by: java.text.ParseException: Unparseable date: "2018-05-04 09:35:38" does not match (\p{Nd}++)\Q-\E(\p{Nd}++)\Q-\E(\p{Nd}++)\Q \E(\p{Nd}++)\Q:\E(\p{Nd}++)\Q:\E(\p{Nd}++)\Q.\E(\p{Nd}++)
        at org.sqlite.date.FastDateParser.parse(FastDateParser.java:299) ~[sqlite-jdbc-3.21.0.1.jar:na]
        at org.sqlite.date.FastDateFormat.parse(FastDateFormat.java:490) ~[sqlite-jdbc-3.21.0.1.jar:na]
        at org.sqlite.jdbc3.JDBC3ResultSet.getTimestamp(JDBC3ResultSet.java:529) ~[sqlite-jdbc-3.21.0.1.jar:na]
        ... 45 common frames omitted Caused by: java.text.ParseException: Unparseable date: "2018-05-04 09:35:38" does not match (\p{Nd}++)\Q-\E(\p{Nd}++)\Q-\E(\p{Nd}++)\Q \E(\p{Nd}++)\Q:\E(\p{Nd}++)\Q:\E(\p{Nd}++)\Q.\E(\p{Nd}++)
        at org.sqlite.date.FastDateParser.parse(FastDateParser.java:299) ~[sqlite-jdbc-3.21.0.1.jar:na]
        at org.sqlite.date.FastDateFormat.parse(FastDateFormat.java:490) ~[sqlite-jdbc-3.21.0.1.jar:na]
        at org.sqlite.jdbc3.JDBC3ResultSet.getTimestamp(JDBC3ResultSet.java:529) ~[sqlite-jdbc-3.21.0.1.jar:na]
        ... 45 common frames omitted 

````

这是因为SQLite本身没有严格的日期时间数据类型，为了满足JDBC中对日期时间的框架使用的日期时间格式的要求，JDBC驱动按自己的默认格式对日期时间进行了解析后，转化为数字存储到数据库中的，因此我们需要将日期格式设置为符合我们习惯的方式。具体方法是在配置JDBC连接串(URL)的时候，加上日期格式的参数。如下所示——

```
jdbc:sqlite:xxx.db?date_string_format=yyyy-MM-dd HH:mm:ss
```

即可消除上述错误。



