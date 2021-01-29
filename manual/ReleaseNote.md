

### v1.12.10.RELEASE

2021-01-29

1. 修复当字段超过127后ASM操作溢出的问题。
2. 修复SQLServer2016在使用中方言被判定为SQLServer 2012造成错误的问题。

### v1.12.9.RELEASE

2019-12-17 

1. 修复JTA事务下提交会报错的问题。
2. 支持Postgresql中ilike表达式。（SQL解析器修改）
3. 默认的named-query.xml路径改为classpath*:named-query.xml

### v1.12.8.RELEASE

2019-04-06

1. 升级到Spring-data v2.1.5。更改Spring-data变更的API
2. 修复Join注解中部分自定义属性无效问题。
3. alaterTable属性现在可以控制index和约束的创建
4. 修复解析器中数字前的负号被处理两遍的问题

### v.1.12.7.FINAL

2019-05-09

1. 修复update场合下SQL日志中未显示出字段名的问题。
2. 修复关联查询时在Query中设置的attribute无效的问题。
3. 修复Sql词法解析器会将整数前面的负号解析两次造成SQL失效的问题。(该问题在v 1.12.4.RELEASE的第11项改动点中引入)

### v1.12.7.2

2019-03-26

1. 修复计算表名并发场景下一处问题。

### v1.12.7.1

2019-03-19

1. 修改命名查询资源文件获取方式，支持使用通配符方式查找命名查询文件。（classpath*:resourcename）
2. Oracle兼容问题修复

### v1.12.7.RELEASE

2019-03-14

1. 增强：将内置ASM升级到7，从而支持对Java11的类进行解析和生成。

### v1.12.6.1

2019-03-14

1. 修复： 当使用全局的表来代替Sequence时，存在Sequence获取时的并发状态问题。

### v1.12.6.RELEASE

2019-03-12

修复数据库结构维护语句相关的问题

1. 忽略的column.unique()方法
2. 未对null null, default value进行处理的问题
3. 约束比较的逻辑优化
4. 约束删除语句修改。

### v1.12.5.3

2019-02-21

1.  增加判断，防止在自定义映射器的情况下，没有一个字段对应上的场景中误报“无字段对应上”的错误。
2. 解决在特殊使用场景下发生的并发问题。
3. 修正命名查询加载中的兼容性问题。

### v 1.12.4.RELEASE

2018-09-15

1. 修复当未开始事务时执行读，连接过早关闭的问题（重要：1.12引入，1.11无此问题。）
2. 改进了查询API，现在所有的Field字段支持直接使用`Person.Field.gender.isNotNull()`等格式形成条件，无需使用QueryBuilder类帮助。
3. 提升MySQL5.7兼容，增加对MySQL 8.x的适配
4. 原Maven插件 jef-maven-plugin被废弃。新的maven插件为geequery-maven-plugin。支持数据库表导出、数据库初始化数据导出、spring-data仓库生成、Spring-boot初始化等。取代原来的Eclipse插件，参见[geequery-maven-plugin.md](geequery-maven-plugin.md)
5. 配合另一个项目[geequery-spring-boot](https://github.com/xuse/geequery-spring-boot)。可以实现零配置集成Spring-boot。增加了相关文档。参见[quickdemo-spring-boot.md](./quickdemo-spring-boot.md)。 此外升级spring-boot版本到2.0.4.RELEASE，相应spring库升级。
6. 支持配置initDataRoot，初始化数据现在可以通过配置存放在src/main/resources/下的任何其他路径下.
7. 修复生成代码以及数据库元数据获取时，复合主键的顺序总是按照字段出现先后顺序排列的问题。
8. 除了Oracle以外，数据库元数据现在可以使用 default ''用空字符串作为默认值。
9. 从数据库导出实体时，使用@Temporal注解描述时间精度。
10. 修复从数据库导出实体时，缺省值小写被转为大写的问题。
11. 修复词法分析器，不支持建表语句中int default -1（负数常量解析失败）的问题。
12. 为Stream增加一个CloseHandler，确保当使用Strem流式获取结果时，流在关闭能同时关闭JDBC ResultSet.

### v 1.12.2.RELEASE

2018-07-20

1. 支持将GeeQuery的模型生成动态的QueryDSL path，因此可以使用Query语法来编写复杂的查询，甚至直接将GeeQuery的实体在QueryDSL中使用。@see com.github.geequery.extension.querydsl.QueryDSLTables
2. GeeQuery的Spring-data Repository总是实现QuerydslPredicateExecutor，因此可以将任意QueryDSL的Predicate查询条件直接传入Repository进行查询，配合功能1使用。

### v 1.12.0.RELEASE

2018-02-22

1. 支持更多的JPA注解，如@TableGenerator等。支持对数据库索引和各种constraint的获取
2. fixed https://github.com/GeeQuery/ef-orm/issues/102
3. 反射框架中的并发问题修复
4. Spring-data仓库增加更多的方法
5. 增加@FindBy注解，简化Spring-data查询操作。