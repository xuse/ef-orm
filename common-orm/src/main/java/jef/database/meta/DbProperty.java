package jef.database.meta;

public enum DbProperty {
	/**
	 * 操作关键字
	 */
	DROP_COLUMN,ADD_COLUMN,MODIFY_COLUMN,
	
	/**
	 * 开销最小的查询SQL语句，用于检测数据库心跳，如果没有这样的语句，返回null
	 * 目前采用了JDBC 4.0中的isValid方法来检查连接心跳，因此这个参数最近没什么用。
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
	GET_IDENTITY_FUNCTION
	
	
	//Derby支持一下函数来获得当前环境
//	CURRENT ISOLATION
//
//	CURRENT SCHEMA
//	
//	CURRENT_USER
	
}
