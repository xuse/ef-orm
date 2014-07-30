package jef.database.query;

import jef.database.Field;


/**
 * 列选择操作器。可以操作SQL查询的select字句当中的部分。
 * @author Administrator
 * @Date 2011-6-17
 */
public interface Selects extends EntityMappingProvider{
	
	/**
	 * 设置带有distinct修饰
	 * @param distinct
	 */
	void setDistinct(boolean distinct);
	
	/**
	 * 是否被distinct修饰
	 * @return
	 */
	boolean isDistinct();
	
	/**
	 * 取该表全部列
	 * @param query  查询实体：对应表 
	 * @return
	 */
	AllTableColumns allColumns(Query<?> query);
	
	/**
	 * 该表不取任何列, 等同于allColumns(query).notSelectAnyColumn();
	 * @param query
	 */
	void noColums(Query<?> query);
	
	/**
	 * 指定查询某个列
	 * @deprecated此方法不能指定使用哪个查询表，因此不建议使用
	 * @param name
	 * @param alias
	 * @return
	 */
	SelectColumn guessColumn(String name);
	
	/**
	 * 根据字符串创建一个Select部分的SQL表达式，比如 函数等<br>
	 * 系统会对SQL语句进行解析并尝试将其中的特定函数翻译为当前数据库支持的语言<p>
	 * <code>
	 * 可以使用 $1 $2来表示参与查询的第一张表和第二张表的别名
	 * use $1, $2 to assign the table alias of all queries.
	 * </code>
	 * 
	 * @param string
	 * @return
	 */
	SelectExpression sqlExpression(String sql);
	
	/**
	 * 根据字符串创建一个Select部分的SQL表达式，比如 函数等<br>
	 * 系统仅检查SQL语法合法性，不会翻译为当前数据库语言<br>
	 * JEF不会对这个方法中输入的SQL进行检查和该写，用户应当自行保证传入SQL兼容当前的数据库。<p>
	 * <code>
	 * 可以使用 $1 $2来表示参与查询的第一张表和第二张表的别名
	 * use $1, $2 to assign the table alias of all queries.
	 * </code>
	 * @param sql
	 * @return
	 */
	SelectExpression rawExpression(String string);
	
	
	/**
	 * 指定要查出这个列
	 * @param name
	 * @return
	 */
	SelectColumn column(Field name);
	
	/**
	 * 指定选择某个列(因为一个join查询中一张表可能出现多次，因此通过指定Query来确定是从那张表中选取)
	 * @param query  : 查询实体：对应表
	 * @param name   ：实体成员变量
	 * @return
	 */
	SelectColumn column(Query<?> query, Field name);
	
	/**
	 *指定选择某个列
	 * @param query: 查询实体：对应表
	 * @param name   实体成员变量名称
	 * @return
	 */
	SelectColumn column(Query<?> query, String name);
	
	/**
	 * 指定在某个表中的多个查询列
	 * @param t2 查询表实例
	 * @param string 列名或表达式，可以有多个列，并且可以用符合SQL习惯的方式指定别名，如 name as personname,id as rootId
	 */
	void columns(Query<?> t2, String string);
	
	/**
	 * 指定多个查询列和别名
	 * @param string
	 */
	void columns(String string);
	
	/**
	 * 指定多个查询列和别名
	 * @param string
	 */
	void columns(Field... fields);
	
	/**
	 * 清除所有的Select选择设置，清除以后将使用系统默认的选择列和拼装方式
	 *
	 */
	void clearSelectItems();

}
