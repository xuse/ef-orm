package jef.database.query;

import java.util.List;

import jef.database.QueryAlias;
import jef.database.SqlProcessor;
import jef.database.meta.JoinKey;
import jef.database.meta.Reference;

/**
 * JoinQuery.
 * 由多个表Join后的查询
 * @author jiyi
 *
 */
public interface Join extends JoinElement{

	/**
	 * 将当前Join对象和一个新的查询进行左外连接。
	 * @param right join右侧的查询对象
	 * @param keys　　join的on条件。可以通过 {@link QueryBuilder#on(jef.database.Field, jef.database.Field)}等方法生成
	 * @return Join的查询对象
	 * @see QueryBuilder#on(jef.database.Field, jef.database.Field)
	 * @see QueryBuilder#on(jef.database.Field, Number)
	 * @see QueryBuilder#on(jef.database.Field, String)
	 * @see QueryBuilder#on(Query, jef.database.Field, Query, jef.database.Field)
	 * @see QueryBuilder#on(jef.database.Field, jef.database.Condition.Operator, Object)
	 */
	public Join leftJoin(Query<?> right,JoinKey... keys);
	
	/**
	 * 将当前Join对象和一个新的查询进行右外连接。
	 * @param right join右侧的查询对象
	 * @param keys　　join的on条件。可以通过 {@link QueryBuilder#on(jef.database.Field, jef.database.Field)}等方法生成
	 * @return Join的查询对象
	 * @see QueryBuilder#on(jef.database.Field, jef.database.Field)
	 * @see QueryBuilder#on(jef.database.Field, Number)
	 * @see QueryBuilder#on(jef.database.Field, String)
	 * @see QueryBuilder#on(Query, jef.database.Field, Query, jef.database.Field)
	 * @see QueryBuilder#on(jef.database.Field, jef.database.Condition.Operator, Object)
	 */
	public Join rightJoin(Query<?> right,JoinKey... keys);
	
	/**
	 * 将当前Join对象和一个新的查询进行内连接。
	 * @param right join右侧的查询对象
	 * @param keys　　join的on条件。可以通过 {@link QueryBuilder#on(jef.database.Field, jef.database.Field)}等方法生成
	 * @return Join的查询对象
	 * @see QueryBuilder#on(jef.database.Field, jef.database.Field)
	 * @see QueryBuilder#on(jef.database.Field, Number)
	 * @see QueryBuilder#on(jef.database.Field, String)
	 * @see QueryBuilder#on(Query, jef.database.Field, Query, jef.database.Field)
	 * @see QueryBuilder#on(jef.database.Field, jef.database.Condition.Operator, Object)
	 */
	public Join innerJoin(Query<?> right,JoinKey... keys);
	
	
	/**
	 * 将当前Join对象和一个新的查询进行全外连接。
	 * @param right join右侧的查询对象
	 * @param keys　　join的on条件。可以通过 {@link QueryBuilder#on(jef.database.Field, jef.database.Field)}等方法生成
	 * @return Join的查询对象
	 * @see QueryBuilder#on(jef.database.Field, jef.database.Field)
	 * @see QueryBuilder#on(jef.database.Field, Number)
	 * @see QueryBuilder#on(jef.database.Field, String)
	 * @see QueryBuilder#on(Query, jef.database.Field, Query, jef.database.Field)
	 * @see QueryBuilder#on(jef.database.Field, jef.database.Condition.Operator, Object)
	 */
	public Join fullJoin(Query<?> right,JoinKey... keys);
	
	
	/**
	 * 框架内部使用<p>
	 * 将当前的Join转化为SQL语句中的表定义部分
	 * @param processor
	 * @param context
	 * @return
	 */
	String toTableDefinitionSql(SqlProcessor processor, SqlContext context);

	
	/**
	 * 框架内部使用<p>
	 * 获得当前Join对象当中所有参与的表查询
	 * @return
	 */
	List<Query<?>> elements();
	
	/**
	 * 框架内部使用<p>
	 * 获得当前Join对象当中所有参与的表查询（含每个查询的别名）
	 * @return
	 */
	List<QueryAlias> allElements();
	
	/**
	 * 框架内部使用<p>
	 * 
	 * 如果该连接对象是由一个级联的单表操作形成，那么返回以下值
	 * 
	 * empty List:表示该级联操作所有关联都使用延迟加载。
	 * 非空List，表示级联操作中，部分关联已经处理过了，后续无需处理。返回这些已经处理过的关联。
	 * 
	 * 如果该连接对象不是级联形成的，返回null;
	 * 
	 * @return
	 */
	List<Reference> getIncludedCascadeOuterJoin();
}
