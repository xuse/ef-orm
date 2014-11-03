package jef.database;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import jef.common.wrapper.IntRange;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.meta.ITableMetadata;
import jef.database.query.SqlExpression;
import jef.database.wrapper.ResultIterator;
import jef.database.wrapper.populator.Transformer;

/**
 * SQL操作工具类，这个对象指向单个特定的数据源。并且可以在这个数据源上执行各种SQL / JPQL/存储过程 相关的操作
 * 
 * 
 */
public interface SqlTemplate {
	/**
	 * 得到数据库元数据管理器
	 * @return 元数据管理器
	 * @throws SQLException
	 * @see DbMetaData
	 */
	public DbMetaData getMetaData() throws SQLException;
	
	/**
	 * 创建Native查询
	 * @param sqlString E-SQL
	 * @param clz 返回类型
	 * @return 查询对象(NativeQuery)
	 * @throws SQLException
	 * @see NativeQuery
	 * @see Session#createNativeQuery(String, Class)
	 */
	public <T> NativeQuery<T> createNativeQuery(String sqlString, Class<T> clz) throws SQLException;
	
	/**
	 * 创建Native查询
	 * @param sqlString E-SQL
	 * @param meta 返回结果的元数据类型
	 * @return 查询对象(NativeQuery)
	 * @throws SQLException
	 * @see NativeQuery
	 * @see Session#createNativeQuery(String, ITableMetadata)
	 */
	public <T> NativeQuery<T> createNativeQuery(String sqlString, ITableMetadata meta) throws SQLException;
	
	
	/**
	 * 创建JPQL查询
	 * @param jpql JPQL语句
	 * @param resultClass 返回结果类型
	 * @return  查询对象(NativeQuery)
	 * @throws SQLException
	 * @see NativeQuery
	 * @see Session#createQuery(String, Class)
	 */
	public <T> NativeQuery<T> createQuery(String jpql, Class<T> resultClass) throws SQLException;
	
	/**
	 * 调用存储过程
	 * @param procedureName 存储过程名
	 * @param paramClass 存储过程的入参和出参
	 * @return 存储过程调用（NativeCall）
	 * @throws SQLException
	 * @see NativeCall
	 * @see Session#createNativeCall(String, Type...)
	 */
	public NativeCall createNativeCall(String procedureName, Type... paramClass) throws SQLException;
	
	/**
	 * 创建匿名过程（匿名块）
	 * @param callString 匿名块代码
	 * @param paramClass 存储过程的入参和出参
	 * @return 存储过程调用(NativeCall)
	 * @throws SQLException
	 * @see NativeCall
	 * @see Session#createAnonymousNativeCall(String, Type...)
	 */
	public NativeCall createAnonymousNativeCall(String callString, Type... paramClass) throws SQLException;
	
	/**
	 * 使用原生SQL分页查询。
	 * 
	 * <h3>什么是原生SQL</h3>
	 * 原生SQL和NativeQuery不同。凡是NativeQuery系列的方法都是对SQL进行解析和改写处理的,而原生SQ不作任何解析和改写，直接用于数据库操作。<p>
	 * 
	 * 原生SQL中，绑定变量占位符和E-SQL不同，用一个问号表示——<pre><tt>select * from t_person where id=? and name like ?</tt></pre>
	 * 
	 * 原生SQL适用于不希望进行SQL解析和改写场合，一般情况下用在SQL解析器解析不了的SQL语句上，用作规避手段。<br>
	 * 建议，在需要保证应用的可移植性的场合下，尽可能使用{@link #createNativeQuery(String, Class)}代替。
	 * 
	 * <h3>SQL中不必写分页逻辑</h3>
	 * 在分页时，EF-ORM依然会尝试改写SQL语句去查询count。
	 * 
	 * @param sql 原生SQL语句
	 * @param returnType 查询返回类型
	 * @param pageSize   每页大小
	 * @return
	 * @throws SQLException
	 */
	public <T> PagingIterator<T> pageSelectBySql(String sql, Class<T> returnType, int pageSize) throws SQLException;
	
	/**
	 * 使用原生SQL分页查询。
	 * 
	 * <h3>什么是原生SQL</h3>
	 * 原生SQL和NativeQuery不同。凡是NativeQuery系列的方法都是对SQL进行解析和改写处理的,而原生SQ不作任何解析和改写，直接用于数据库操作。<p>
	 * 
	 * 原生SQL中，绑定变量占位符和E-SQL不同，用一个问号表示——<pre><tt>select * from t_person where id=? and name like ?</tt></pre>
	 * 
	 * 原生SQL适用于不希望进行SQL解析和改写场合，一般情况下用在SQL解析器解析不了的SQL语句上，用作规避手段。<br>
	 * 建议，在需要保证应用的可移植性的场合下，尽可能使用{@link #createNativeQuery(String, Class)}代替。
	 * 
	 * <h3>SQL中不必写分页逻辑</h3>
	 * 在分页时，EF-ORM依然会尝试改写SQL语句去查询count。
	 * 
	 * @param sql 原生SQL语句
	 * @param meta 查询返回类型的元数据
	 * @param pageSize   每页大小
	 * @return
	 * @throws SQLException
	 */
	public <T> PagingIterator<T> pageSelectBySql(String sql, ITableMetadata meta, int pageSize) throws SQLException;
	
	/**
	 * 使用JPQL进行查询
	 * <br>
	 * EF并未实现JPQL的大部分功能。目前提供的JPQL功能其实只有将Java字段名替换为数据库列名的功能，离JPA规范的JPQL差距较大，而且由于设计理念等差异，要完整支持JPQL基本不可能。
     * 现有若干伪JPQL功能是早期遗留的产物，后来在对SQL的特性作了大量改进后，E-SQL成为EF-ORM主要的查询语言。JPQL方面暂无改进计划，因此不建议使用。
     * 
	 * @param jpql JPQL语句 
	 * @param resultClz 返回结果
	 * @param params  绑定变量参数
	 * @return 查询结果
	 * @throws SQLException
	 */
	public <T> List<T> selectByJPQL(String jpql,Class<T> resultClz,Map<String,Object> params) throws SQLException;
	
	/**
	 * 执行JPQL语句
	 * <br>
	 * EF并未实现JPQL的大部分功能。目前提供的JPQL功能其实只有将Java字段名替换为数据库列名的功能，离JPA规范的JPQL差距较大，而且由于设计理念等差异，要完整支持JPQL基本不可能。
     * 现有若干伪JPQL功能是早期遗留的产物，后来在对SQL的特性作了大量改进后，E-SQL成为EF-ORM主要的查询语言。JPQL方面暂无改进计划，因此不建议使用。
     * 
	 * @param jpql JPQL语句
	 * @param params 绑定变量参数
	 * @return 影响的记录行数
	 * @throws SQLException
	 */
	public int executeJPQL(String jpql,Map<String,Object> params) throws SQLException;

	
	/**
	 * 使用原生SQL查询出一个long型数值
	 * {@linkplain #executeSql(String, Object...) 什么是原生SQL}
	 * 
	 * 相当于
	 * {@linkplain #selectBySql(String, Long,class, Object...)}
	 * @param countSql 查询的语句，应该是一个count语句
	 * @param params  查询参数
	 * @return 查询结果
	 * @throws SQLException
	 */
	public long countBySql(String countSql, Object... params) throws SQLException ;
	

	/**
	 * 执行原生SQL语句
	 * <h3>什么是原生SQL</h3>
	 * 原生SQL和NativeQuery不同。凡是NativeQuery系列的方法都是对SQL进行解析和改写处理的,而原生SQ不作任何解析和改写，直接用于数据库操作。<p>
	 * 
	 * 原生SQL中，绑定变量占位符和E-SQL不同，用一个问号表示——<pre><tt>select * from t_person where id=? and name like ?</tt></pre>
	 * 
	 * 原生SQL适用于不希望进行SQL解析和改写场合，一般情况下用在SQL解析器解析不了的SQL语句上，用作规避手段。<br>
	 * 建议，在需要保证应用的可移植性的场合下，尽可能使用{@link #createNativeQuery(String, Class)}代替。
	 * 
	 * @param sql    SQL语句
	 * @param params 绑定变量
	 * @return 影响的记录条数
	 * @throws SQLException
	 */
	public int executeSql(String sql, Object... params) throws SQLException;
	
	/**
	 * 使用原生SQL语句加载一条记录。
	 * {@linkplain #executeSql(String, Object...) 什么是原生SQL}
	 * 
	 * @param sql SQL语句
	 * @param t   返回结果类型
	 * @param params 绑定变量
	 * @return 查询结果
	 * @throws SQLException 除了查询错误的情况外，如果查询结果不止一条，也会抛出此异常
	 */
	public <T> T loadBySql(String sql,Class<T> t,Object... params) throws SQLException;
	
	/**
	 * 使用原生SQL查询，
	 * 	{@linkplain #executeSql(String, Object...) 什么是原生SQL}
	 * 
	 * @param sql   SQL语句
	 * @param resultClz  需要的返回结果类型
	 * @param params    绑定变量
	 * @return   查询结果
	 * @throws SQLException
	 */
	public <T> List<T> selectBySql(String sql, Class<T> resultClz, Object... params) throws SQLException;
	
	/**
	 * 使用原生SQL查询，
	 * 	 {@linkplain #executeSql(String, Object...) 什么是原生SQL}
	 * @param sql   SQL语句
	 * @param transformer  查询结果转换器
	 * @param range        分页范围(是一个含头含尾的区间对象)
	 * @param params       绑定变量
	 * @return   查询结果
	 * @throws SQLException
	 */
	public <T> List<T> selectBySql(String sql, Transformer transformer, IntRange range, Object... params) throws SQLException;
	
	/**
	 * 使用原生SQL查询，返回的遍历器,	 遍历器模式查找一般用于超大结果集的返回。
	 * {@linkplain #executeSql(String, Object...) 什么是原生SQL}<br>
	 * 
	 * <h3>作用</h3> 当结果集超大时，如果用List<T>返回，内存占用很大甚至会溢出。<br>
	 * JDBC设计时考虑到这个问题，因此其返回的ResultSet对象只是查询结果视图的一段，用户向后滚动结果集时，数据库才将需要的数据传到客户端。
	 * 如果客户端不缓存整个结果集，那么前面已经滚动过的结果数据就被释放。<p>
	 * 这种处理方式实际上是一种流式处理模型，iteratedSelect就是这种模型的封装。<br>
	 * iteratedSelect并不会将查询出的所有数据放置到一个List对象中（这常常导致内存溢出）。而是返回一个Iterator对象，用户不停的调用next方法向后滚动，
	 * 同时释放掉之前处理过的结果对象。这就避免了超大结果返回时内存溢出的问题。
	 * <h3>注意事项</h3> 由于 ResultIterator
	 * 对象中有尚未关闭的ResultSet对象，因此必须确保使用完后关闭ResultIteratpr.如下示例
	 * <pre><tt>ResultIterator<TestEntity> iter = db.iteratorBySql(sql,new Transformer(TestEntity.class), 0, 0);
	 * try{
	 * for(; iter.hasNext();) {
	 * 	iter.next();
	 * 	//do something.
	 *  }	
	 * }finally{
	 *  //必须在finally块中关闭。否则一旦业务逻辑抛出异常，则ResultIterator未释放造成游标泄露.
	 *   iter.close(); 
	 * }</tt></pre>如果ResultSet不释放，相当于数据库上打开了一个不关闭的游标，而数据库的游标数是很有限的，耗尽后将不能执行任何数据库操作。<br>
	 * 
	 * @param sql 原生SQL语句
	 * @param transformers 结果转换器
	 * @param maxReturn    最多返回结果数据
	 * @param fetchSize    JDBC获取每批大小。
	 * @param objs         绑定变量参数
	 * @return
	 * @throws SQLException
	 */
	public <T> ResultIterator<T> iteratorBySql(String sql, Transformer transformers,int maxReturn, int fetchSize,Object... objs) throws SQLException;
	
	/**
	 * 指定一条原生SQL语句，批量执行多次。
	 * {@linkplain #executeSql(String, Object...) 什么是原生SQL}
	 * 
	 * @param sql
	 *            要执行的SQL语句
	 * @param params
	 *            绑定变量，每个List<?>是一组数据（相当于SQL执行一次）。可以传入多组数据
	 * @return 查询影响记录行数（总计）
	 * @throws SQLException
	 */
	public int executeSqlBatch(String sql, List<?>... params) throws SQLException;
	
	/**
	 * 得到SQL表达式的值。因为不同的数据库语法不同，例如
	 * <code>
	 * <ul>select [expression] from dual;  //Oracle</ul>
	 * <ul>select [expression];     //MySQL</ul>
	 * <ul>values [expression];     //Derby</ul>
	 * </code>
	 * 使用此方法，只需传入表达式本身，SQL语句会根据当前数据库自动生成
	 * 
	 * @param expression 表达式
	 * @param clz 返回结果类型
	 * @param 绑定变量的值
	 * @return 根据表达式查询出来的结果
	 */
	public <T> T getExpressionValue(String expression,Class<T> clz,Object... params)throws SQLException;
	
	/**
	 * 得到SQL函数的值。因为不同的数据库语法不同，例如
	 * <code>
	 * <ul>select [expression] from dual;  //Oracle</ul>
	 * <ul>select [expression];     //MySQL</ul>
	 * <ul>values [expression];     //Derby</ul>
	 * </code>
	 * 使用此方法，只需传入表达式本身，SQL语句会根据当前数据库自动生成
	 * 
	 * @param func 函数
	 * @param clz 返回结果类型
	 * @param params 函数参数
	 * @return 根据函数查询出来的结果
	 */
	public <T> T getExpressionValue(DbFunction func,Class<T> clz,Object... params)throws SQLException;
	
	/**
	 * 返回数据库函数表达式。
	 * 
	 * @param func
	 *            {@link jef.database.query.Func}
	 * @param params
	 *         参数
	 * @return 函数表达式
	 */
	public SqlExpression func(DbFunction func, Object... params) ;
	
	/**
	 * 获得Sequence
	 * @param mapping Sequence所在列的元数据定义
	 * @return Sequence
	 * @throws SQLException
	 * @see Sequence 
	 */
	public Sequence getSequence(AutoIncrementMapping<?> mapping) throws SQLException;

	/**
	 * 获得Sequence
	 * @param seqName Sequence名称
	 * @param i  最大位数
	 * @return Sequence
	 * @throws SQLException
	 * @see Sequence 
	 */
	public Sequence getSequence(String seqName, int i)throws SQLException;
}
