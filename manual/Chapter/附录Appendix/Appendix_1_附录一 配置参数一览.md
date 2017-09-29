GeeQuery使用手册——Chapter-19  附录一 配置参数一览

[TOC]

# Chapter-19  附录一 配置参数一览

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
| db.autoincrement.native        | jpa实现关于自增实现分为  identity: 使用数据库表本身的自增特性   sequence： 使用sequence生成自增  table： 使用数据库表模拟出sequence。   Auto: 根据数据库自动选择 Identity  > Sequence  > Table (由于数据库多少都支持前两个特性，所以实际上Table默认不会被使用     * \</ul>  开启此选项后，即便用户配置为Sequence或IDentity也会被认为采用Auto模式。(Table模式保留不变) | true             |
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