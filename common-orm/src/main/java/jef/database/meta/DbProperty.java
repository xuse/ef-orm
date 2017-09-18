package jef.database.meta;

public enum DbProperty {
	/**
	 * 操作关键字
	 */
	DROP_COLUMN,ADD_COLUMN,MODIFY_COLUMN,ADD_CONSTRAINT,
	
	/**
	 * 开销最小的查询SQL语句，用于检测数据库心跳，如果没有这样的语句，返回null
	 * {@code
	 * 目前采用了JDBC 4.0中的isValid方法来检查连接心跳，因此这个参数最近没什么用。
	 * }
	 */
	CHECK_SQL,
	/**
	 * 嵌入式数据库大多需要特别的命令来关闭数据库
	 * 这个功能最近似乎支持得不太好。因为除了HSQL这类内存数据库其他大多数数据库都不需要。
	 * @deprecated 目前无效
	 * TODO 保留在HSQLdb支持用
	 */
	SHUTDOWN_COMMAND,
	/**
	 * 无关联表的表达式获取，比如获取当前时间的SQL语法。
	 * 将表达式作为参数，通过 String.format(template,expression)的方式得到SQL语句
	 */
	SELECT_EXPRESSION,
	
	/**
	 * 当使用关键字作为表名或列名时的处理
	 * 大部分数据库用 "
	 * MYSQL用`来包围表名和列名
	 */
	WRAP_FOR_KEYWORD,
	
//	/**
//	 * Oracle Sequence可以用nocache作为关键字
//	 */
//	NO_CACHE,
	/**
	 * 用于获取某列下一个Sequence值的SQL语句模板
	 */
	SEQUENCE_FETCH,
	
	/**
	 * GBASE特性，在Index结尾需要指定USING HASH
	 * GBase特性，非BITMAP索引需要使用USING HASH进行定义
	 */
	INDEX_USING_HASH,
	/**
	 * 用于返回数据库刚刚生成的自增键的函数
	 */
	GET_IDENTITY_FUNCTION,
	
	/**
	 * 返回若干用于查询数据库基本信息的SQL语句，如果配置了这些SQL语句，那么启动时在输出数据库版本信息的时候就会
	 * 将这些SQL的执行结果也作为版本信息一起输出。
	 * 如果有多句SQL,用';'分隔。
	 */
	OTHER_VERSION_SQL,
	
	/**
	 * 索引的最大长度
	 */
	INDEX_LENGTH_LIMIT,
	/**
	 * 索引长度超过时，需要修复的长度（MYSQL）
	 */
	INDEX_LENGTH_LIMIT_FIX,
	/**
	 * 修复的关键字（MYSQL）
	 */
	INDEX_LENGTH_CHARESET_FIX,
	/**
	 * 像Oracle,DB2,PG，drop index xxx 即可。
	 * 但是像SQLServer，需要drop index table.index。
	 * 像MySQL，需要 drop index xxx on tablexxx.
	 */
	DROP_INDEX_TABLE_PATTERN,
	
	/*
	 * Sequence的最大长度
	 */
	MAX_SEQUENCE_VALUE,
	
	/**
	 * Drop Foreign key的语句模板
	 */
	DROP_FK_PATTERN
	
	//Derby支持一下函数来获得当前环境
//	CURRENT ISOLATION
//
//	CURRENT SCHEMA
//	
//	CURRENT_USER
	
}
