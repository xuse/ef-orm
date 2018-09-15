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