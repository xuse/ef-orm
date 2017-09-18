package jef.database;

import jef.common.Configuration.ConfigItem;

/**
 * 数据库配置的枚举
 * @author Jiyi
 *
 */
public enum DbCfg implements ConfigItem {
	/**
	 * 数据库文本编码，当将String转为BLOB时，或者在计算字符串插入数据库后的长度时使用（java默认用unicode，中文英文都只占1个字符）
	 */
	DB_ENCODING,
	/**
	 * 在日志中，当文本过长的时候是不完全输出的，此时要输出文本的长度。可能影响性能
	 */
	DB_ENCODING_SHOWLENGTH,
	/**
	 * 数据库查询最大条数限制，默认0，表示不限制
	 */
	DB_MAX_RESULTS_LIMIT,
	/**
	 * 数据库查询超时，单位秒，默认60
	 */
	DB_SELECT_TIMEOUT,	
	/**
	 * 数据库查询超时，单位秒，默认60<br/>
	 * 实际操作中各种存储过程和自定义的SQL语句也大多使用此超时判断
	 */
	DB_UPDATE_TIMEOUT,
	/**
	 * 数据库删除超时,单位秒，默认60<br/>
	 */
	DB_DELETE_TIMEOUT,
	/**
	 * schema重定向功能（特色）
	 */
	SCHEMA_MAPPING,
	/**
	 * 多数据源支持:数据源名称重定向表（特色）
	 */
	DB_DATASOURCE_MAPPING,
	/**
	 * 取消支持多数据源，原先配置的数据源绑定等功能都映射到当前数据源上
	 * 默认false
	 */
	DB_SINGLE_DATASOURCE,
	/**
	 * 分表和路由规则加载器类.默认会使用基于class的annotation加载器，也可以使用资源文件<br/>
	 * 默认实现类：jef.database.meta.PartitionStrategyLoader$DefaultLoader<br/>
	 * 使用者可以实现jef.database.meta.PartitionStrategyLoader编写自己的分表规则加载器。
	 * 甚至可以使用外部资源乃至数据库数据。
	 */
	PARTITION_STRATEGY_LOADER,
	
	/**
	 * 是否根据事务参数去设置事务的隔离级别。
	 * 默认开启，可以禁用。当碰到某些不支持设置事务隔离级别的驱动时，可以关闭此开关。
	 */
	DB_SET_ISOLATION,
	
	/**
	 * <h3>扩展点，配置自定义的元数据加载器</h3>
	 * 元数据默认现在都是配置在类的注解中的，使用自定义的读取器可以从其他更多的地方读取元数据配置。
	 * 可以自行编写一个类实现jef.database.meta.AnnotationProvider，从而实现自定义的元数据加载方式
	 */
	CUSTOM_METADATA_LOADER,
	
	/**
	 * <h3>内建的METADATA加载器可以从外部资源文件XML中读取元数据配置</h3>
	 * 如果此选项不配置任何内容，那么就取消外部元数据资源加载功能。
	 * 
	 * 可以配置为 /hbm/%s.hbm.xml 
	 * 上面的%s表示类的简单名 %c表示类的全名 %*表示搜索全部符合条件的资源并根据其中的 class属性自动匹配。
	 */
	METADATA_RESOURCE_PATTERN,
	
	/**
	 * true禁用分表功能
	 * 默认false
	 * @deprecated 尚未使用
	 */
	PARTITION_DISABLED,
	/**
	 * 将resultset的数据先放入缓存，再慢慢解析(Iterated操作除外)。这一配置会增加内存开销，但使得每次查询占用连接的时间更少。
	 */
	DB_CACHE_RESULTSET,	
	/**
	 * 在batch操作时，为效率计，不会打印出全部项的参数。
	 * 在批量操作时日志中打印出的最多的参数组，默认5
	 */
	DB_MAX_BATCH_LOG,
	/**
	 * 自动转换表名(为旧版本保留，如果用户没有通过JPA配置对象与表名的关系，那么开启此选项后， userId -> USER_ID， 否则userId -> USERID
	 */
	TABLE_NAME_TRANSLATE, 
	
	/**
	 * String，配置Blob的返回类型,支持 string/byte/stream/file 四种，默认为stream
	 */
	DB_BLOB_RETURN_TYPE,
	
	//////////////////内嵌连接池相关设置/////////////////
	
	/**
	 * JEF内嵌连接池额定连接数,数字，默认值3
	 * 
	 * 自1.12开始内嵌连接池默认不再使用，仅当以非DataSource方式启动才会使用
	 */
	DB_CONNECTION_POOL,
	/**
	 * JEF内嵌连接池最大连接数,数字，默认50
	 */
	DB_CONNECTION_POOL_MAX,
	/**
	 * JEF内嵌连接池调试开关，默认false，开启后输出连接池相关日志
	 */
	DB_POOL_DEBUG,
	/**
	 * JEF内嵌连接池心跳时间，按此间隔对连接进行扫描检查，单位毫秒。默认120秒
	 */
	DB_HEARTBEAT,
	/**
	 * 每个连接最小生存时间
	 */
	DB_CONNECTION_LIVE,	
	
	
	/////////////////默认连接的数据库配置////////////////
	/**
	 * 当使用无参数构造时，所连接的数据库类型
	 */
	DB_TYPE,
	/**
	 * 当使用无参数构造时，所连接的数据库路径（本地的文件数据库，如derby hsql sqlite等）
	 */
	DB_FILEPATH,				
	/**
	 * 当使用无参数构造时，所连接的数据库地址
	 */
	DB_HOST, 					//数据局地址
	/**
	 * 当使用无参数构造时，所连接的数据库端口
	 */
	DB_PORT, 					//数据库端口
	/**
	 * 当使用无参数构造时，所连接的数据库名称（服务名/SID）
	 */
	DB_NAME, 					//数据库名(服务名)
	/**
	 * 当使用无参数构造时，所连接的数据库登录
	 */
	DB_USER,					//登录用户
	/**
	 * 当使用无参数构造时，所连接的数据库登录密码
	 */
	DB_PASSWORD, 				//登录密码
	
	///////////////////其他数据库选项////////////////
	/**
	 * 当用户将Oracle的 start... with... connect by这样的语句在其他不支持此特性的数据库上运行时，允许删除这部分语句。（默认true）
	 */
	ALLOW_REMOVE_START_WITH,
	
	/**
	 * 启用Oracle rowid支持。默认true
	 */
	DB_ENABLE_ROWID,
	/**
	 * SQL语句输出格式。默认情况下面向开发期间的输出格式，会详细列出语句、参数，分行显示。
	 * 但在linux的命令行环境下，或者系统试运行期间，如果要用程序统计SQL日志，那么分行显示就加大了统计的难度。
	 * 为此可以配置为default / no_wrap，no_wrap格式下，一个SQL语句只占用一行输出。
	 */
	DB_LOG_FORMAT,
	
	/**
	 * 格式化SQL语句
	 */
	DB_FORMAT_SQL,
	/**
	 * 检查每个加载的Entity都必须增强，否则报错。默认true。
	 */
	DB_FORCE_ENHANCEMENT,		//
	/**
	 * 数据库每次获取的大小。默认0，表示使用JDBC驱动默认的值
	 */
	DB_FETCH_SIZE,				//
	/**
	 * 	配置一张表名，启用数据表存放NamedQuery功能
	 */
	DB_QUERY_TABLE_NAME,
	/**
	 * 是否自动更新NamedQueries查询配置，开启此选项后，会频繁检查本地配置的（或数据库）中named-query配置是否发生变化，并加载这些变化。
	 * 目的是让开发时不用重启系统可以进行调试，默认和db.debug保持一致
	 */
	DB_NAMED_QUERY_UPDATE,
	/**
	 * 用于存放明明查询的资源文件名，默认名称为named-queries.xml
	 */
	NAMED_QUERY_RESOURCE_NAME,
	/**
	 * 当选择t.*时，是否要将全部列名指出，默认 false,为true时全部列名显示
	 */
	DB_SPECIFY_ALLCOLUMN_NAME,
	/**
	 * 动态更新方式(更新的时候对于没有设值的字段不写入)，默认true。
	 * 如果关闭则更新时，对除了主键以外的字段全部做更新。
	 */
	DB_DYNAMIC_UPDATE,
	/**
	 * 在使用对比等手段更新数据时,防止将null等有未赋值等嫌疑的数据更新到数据库中。
	 * 默认关闭
	 */
	DB_SAFE_MERGE,
	/**
	 * 动态插入方式，不设值的字段不插入（也可以以表为单位进行配置）
	 */
	DB_DYNAMIC_INSERT,			//
	/**
	 * 数据库启动时默认创建表
	 */
	DB_TABLES,
	/**
	 * 数据库启动时默认加载的类，用于加载一些全局配置
	 */
	DB_INIT_STATIC,
	/**
	 * true后禁止创建带remark标记的Oracle数据库连接。对于oracle而言，使用remark的连接性能很差。但要读取元数据注解必须使用此特性。<br>
	 * 1.8.1.RELEASE以后，如果使用不使用内建连接池、或者使用内建连接池并且连接数大于3个，那么系统认为是在生产环境使用的大型应用，会自动关闭REMARK特性。
	 */
	DB_NO_REMARK_CONNECTION,	//
	
	/**
	 * 使用外连接的方式一次性从数据库查出所有对单关联的目标值。<br/>
	 * 
	 * 
	 * EF-ORM从1.0到1.8只支持单关联使用外连接。1.8版本以后变为可配参数，默认true。可以关闭。
	 */
	DB_USE_OUTER_JOIN,
	/**
	 * 通过记录SavePoint的方式，为Postgresql进行事务的保持。<p>
	 * Postgresql在事务状态出现SQL执行错误，那么下一句操作必须执行commit或rollback，即不再允许执行其他SQL语句。
	 * 这一点和其他数据库很不一样。虽然大多数时候在SQL错误之后我们都会立即回滚事务，但是还是有不少场景需要在该事务上继续执行SQL语句的，
	 * 为了支持这一点，能实现的办法就是在每次执行SQL前记录一个恢复点，如果执行失败则恢复到执行前的状态。如果执行成功则释放恢复点。<br>
	 * 
	 * 这种方法虽然会稍微影响一点性能，但是却是保证跨数据库兼容性的手段。
	 * 
	 * 默认开启，设置成以下可以关闭。
	 * <code>db.keep.tx.for.postgresql=false</code>
	 */
	DB_KEEP_TX_FOR_POSTGRESQL,
	
	/**
	 * 默认情况下，JPA事务提交时为顺序提交，一旦出错就终止提交并回滚。
	 * 开启此选项后，可以让各个连接相对独立。仅回滚提交出错的连接。
	 * 注意：此选项并不表示各个连接的事务独立。如果是程序运行时抛出异常，所有连接还是会一起回滚的。
	 */
	DB_JPA_CONTINUE_COMMIT_IF_ERROR,
	
	/////////////数据库Sequence生成相关配置////////////////
	/**
	 * JPA实现关于自增实现分为
	 * <ul> 
	 * <li>Identity: 使用数据库表本身的自增特性</li>
	 * <li>Sequence： 使用Sequence生成自增</li>
	 * <li>Table： 使用数据库表模拟出Sequence。</li>
	 * <li>Auto: 根据数据库自动选择 Identity  > Sequence > Table <br />(由于数据库多少都支持前两个特性，所以实际上Table默认不会被使用。（分库分表下会被使用）</li>
	 * </ul>
	 * <p /> 
	 * 
	 * true/false。 开启此选项后，即便用户配置为Sequence或IDentity也会被认为采用Auto模式。(Table模式保留不变)
	 * 默认false
	 */
	DB_AUTOINCREMENT_NATIVE,
	
	/**
	 * 允许手工指定的值代替自动生成的值，默认false。
	 * 正常情况下，自增主键的字段是无法由用户设值的，开启此选项后，如果用户设值了自增主键的值，则将尝试将此值代替自增主键值。
	 * <br>
	 * <strong>
	 * WARN: 在MSSQL和SQLite上开启后此特性也不能使用。如果要兼容所有数据库，请勿使用此功能。
	 * </strong>
	 */
	DB_SUPPORT_MANUAL_GENERATE,
	
	/**
	 * 如果自增实现<strong>实际使用</strong>了Sequence或Table作为自增策略，那么开启hilo优化模式。即sequence或table生成的值作为主键的高位，再根据当前时间等自动填充低位。
	 * 这样做的好处是一次操作数据库可以生成好几个值，减少了数据库操作的次数，提高了性能。
	 * <p>
	 * 如果实际使用的是Identity模式(即数据库自行生成自增值)，那么此项配置无效。
	 * <p>
	 * true/false。默认false。仅有&#64;HiloGeneration(always=true)的配置会使用hilo。
	 * 一旦设置为true，那么所有配置了&#64;HiloGeneration()的自增键都会使用hilo.
	 */
	DB_AUTOINCREMENT_HILO,
	
	/**
	 * 如果自增实现中<strong>实际使用</strong>了Sequence作为自增策略，那么可以使用该参数适配不同步长的sequence以优化性能。即sequence生成的值每次递增超过1，然后ORM会补齐被跳过的编号。
	 * 对identity,table模式无效。
	 * 当配置为
	 * <ul>
	 * <li>0 : Oracle自动检测，其他数据库为1 (默认)</li>
	 * <li>-1: 对所有数据库均自动检测。在非Oracle数据库上会消耗一次Sequence。</li>
	 * <li>1 :  Sequence步长固定为1.相当于该功能关闭，Sequence拿到多少就是多少。</li>
	 * <li>&gt;1的数值: 数据库不再自动检测，按配置值作为Sequence步长（不推荐。如果和实际Sequence步长不一致可能出错）</li>
	 * </ul>
	 * 默认为0，<br>
	 * 注意：此参数不会修改数据库中已存在的Sequence的步长。也不影响由ef-orm自动创建的sequence步长。
	 * 
	 * 这项配置一般不需要配置，如果你不是很清楚的了解其作用，就不要配这个参数。
	 */
	DB_SEQUENCE_STEP,
	
	/**
	 * 如果自增实现实际使用了Sequence或Table作为自增策略，那么每次访问数据库时会取多个Sequence缓存在内存中，这里配置缓存的大小。
	 * 如果在TABLE模式下，会一次数据库操作直接加上批次的值，从而一次获取满缓存的Sequence
	 * 
	 * 默认为50;
	 */
	SEQUENCE_BATCH_SIZE,
	/**
	 * 开启此选项后，允许自动创建数据库SEQUENCE (或模拟用的TABLE)。
	 * 一般用在开发时和一些小型项目中，不适用于对用户权限有严格规范的专业项目中。
	 */
	AUTO_SEQUENCE_CREATION, //
	/**
	 * 在自动创建SEQUENCE (或模拟用的TABLE)时，Sequence的初始值。
	 * 
	 * 默认-1，表示根据表中现存的记录自动校准(最大值+1)。
	 * 当配置为正数时，为每张表的初始值+配置值。
	 */
	AUTO_SEQUENCE_OFFSET,
	/**
	 * Sequence名称模板<br>
	 * 可以被@SequenceGenerator当中的Sequence名称所覆盖。<br>

	 * 当需要使用Sequence的时候就会被使用。
	 * 
	 * 默认值s_%s S_表名
	 */
	SEQUENCE_NAME_PATTERN,
	
	/**
	 * 配置一个表名作为公共的SequenceTable名称
	 * String
	 * 
	 * 默认空白
	 */
	DB_GLOBAL_SEQUENCE_TABLE,
	
	////////////////////其他数据库相关设置////////////////////////
	/**
	 * 启用一级缓存，默认false
	 */
	CACHE_LEVEL_1,
	
	/**
	 * 默认为0。当为0时，表示不启用二级缓存。
	 * 如果设置为>0的数值表示启用二级缓存，每条缓存记录的最大生存时间等于指定的秒数。
	 */
	CACHE_GLOBAL_EXPIRE_TIME,
	/**
	 * 允许为空的查询条件，默认false
	 */
	ALLOW_EMPTY_QUERY, 
	/**
	 * 配置一个类名，这个类需要实现jef.database.support.DbOperatorListener接口。监听器可以监听数据库的各项基本操作。
	 */
	DB_OPERATOR_LISTENER,
	/**
	 * 配置为true，则对数据库密码做解密处理
	 */
	DB_PASSWORD_ENCRYPTED, 	//true/false描述数据库密码是否加密
	/**
	 * 密码加密的密钥
	 */
	DB_ENCRYPTION_KEY,
	/**
	 * 扫描分表间隔时间(单位秒)，默认3600秒，即一小时。
	 * JEF扫描基于基础表的所有存在分表。这一结果会被缓存，但是考虑到生产环境中会动态的建表，因此缓存可以设置有效期
	 */
	DB_PARTITION_REFRESH,
	/**
	 * ORM初始化后是否显示JDBC和数据库的版本信息，true/false。默认true
	 */
	DB_SHOW_JDBC_VERSION,
	/**
	 * 延迟加载特性，默认启用(true)。关闭后可禁用延迟加载。这项配置对所有的级联操作生效
	 */
	DB_ENABLE_LAZY_LOAD,
	/**
	 * 延迟加载LOB特性，默认关闭(false)。关闭后可禁用延迟加载LOB。这项配置对对象中的BLOB和CLOB字段生效
	 * FIXME 待完善
	 * @deprecated 现尚不稳定，请勿使用
	 */
	DB_LOB_LAZY_LOAD,
	/**
	 * 用来定义数据库和方言类的映射关系的配置文件。
	 * 默认使用 META-INF/dialect-mapping.properties
	 */
	DB_DIALECT_CONFIG,
	/**
	 * 检查SQL语句中的function，默认true，设置为false后，ORM不再检查函数是否可用。
	 */
	DB_CHECK_SQL_FUNCTIONS,
	/**
	 * 当分表维度缺失时，默认时间宽度。是一个表达式，例如 -3,1表示从当天算起时间维度为向前三个月到向后一个月
	 * 分库分表场景下，如果使用时间作为分表维度，而查询数据而又未指定明确的时间时，将只查询该时段范围内的数据。
	 * 
	 */
	PARTITION_DATE_SPAN,
	/**
	 * 当需要查询的数据库数量大于等于指定值时，启用多线程并行查询。(默认值3，即3个数据库或以上启用并行查询)
	 */
	PARTITION_PARALLEL,
	/**
	 * 当分库操作时，不得不进行内存排序和聚合计算时，该参数用于限制最大操作的行数，防止内存溢出。<br>
	 * 一旦达到最大行数，该次操作将抛出异常。
	 * 默认0，表示不限制。
	 */
	PARTITION_INMEMORY_MAXROWS,
	/**
	 * 按需建表功能开关，默认开。开启按需建表功能后，在分库分表下执行插入操作时，会自动创建需要的表。
	 */
	PARTITION_CREATE_TABLE_INNEED,
	/**
	 * 在分库分表下执行查询、删除、更新操作时过滤掉哪些数据库中不存在的表。默认开启。
	 */
	PARTITION_FILTER_ABSENT_TABLES,
	
	/**
	 * 当使用HashMod1024Mapping对字段进行分表分库处理时的计算规则。
	 * 配置方法为
	 * {@code 0-255:DB1,256-767:DB2,768-1023:DB3}
	 * 表示当对字符串取hash后，除以1024的余数为0~255范围时
	 * 
	 */
	PARTITION_BUCKET_RANGE,
	
	
	/**
	 * 在一次in条件中允许出现的最多参数数量。缺省500。有部分批量查询接口，可以自动将传入的条件转为 in (?,?,?,..)这样的语句，该参数可用于控制in条件最大的元素个数。
	 */
	DB_MAX_IN_CONDITIONS;
}
