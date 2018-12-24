package jef.database.query;

import java.util.List;
import java.util.Map;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.Field;
import jef.database.IConditionField;
import jef.database.meta.Reference;

/**
 * 描述最通用的条件容器和查询内容
 * @author Administrator
 * @Date 2011-6-16 
 * @param <T>
 */
public interface Query<T> extends TypedQuery<T>,JoinElement{
	/**
	 * 添加级联对象过滤条件
	 * 在对象中你可以创建{@link javax.persistence.OneToOne OneToOne} {@link javax.persistence.OneToMany OneToMany}
	 * {@link javax.persistence.ManyToOne ManyToOne}
	 * {@link javax.persistence.ManyToMany ManyToMany}等引用关系。
	 * 查询时这会产生级联查询来加载关联的对象。在这里添加的条件，可被用于过滤<strong>级联的对象</strong>。(注意：是级联引用的指向对象，不是当前对象)<br>
	 * <h3>举例来说：</h3>
	 * <pre><code>
	 *    Student student=new Student();
	 *    student.getQuery().addCondition(QB.eq(Student.Field.id, 1));
	 * 	  student.getQuery().addCascadeCondition( QB.eq(Lesson.Field.name, "语文"));
	 *    Student student=session.load(student);
	 *    //在上例中，无论学生是否有语文课成绩记录，都不影响查询ID=1的学生。
	 *    //然而
	 *    student.getLessons(); //此处得到的Lesson中只会有'语文'
	 *    </code></pre>
	 * 总结来说——CascadeCondition只会影响到级联对象的选择，永远不会影响当前对象的选择。
	 * <br>
	 * 
	 * <h3>为何使用</h3>
	 * 一般来说，仅在一对多的时候使用。在上例中，如果一个学生对应的课程记录有数千条，而我们每次只需要统计学生“语数外”的成绩时，
	 * 将其他课程的成绩查出来显然是十分浪费的。因此我们可以用CascadeCondition进一步缩小一对多查询的选择返回，防止因为一对多关系被滥用造成的性能下降
	 * 
	 * <h3>其他</h3>
	 * 这一特性是即时加载还是延迟加载时都会生效。当用于对一级联时，如果不是左外连接，启用此特性将造成原本一次查出（使用外连接）的结果，被拆成两次查询。
	 * 
	 * 
	 * @param conditions 
	 * @return 当前Query对象
	 * @since 1.1
	 * @author Jiyi
	 */
	Query<T> addCascadeCondition(Condition conditions);

	/**
	 * 添加级联对象过滤条件
	 * 在对象中你可以创建{@link javax.persistence.OneToOne OneToOne} {@link javax.persistence.OneToMany OneToMany}
	 * {@link javax.persistence.ManyToOne ManyToOne}
	 * {@link javax.persistence.ManyToMany ManyToMany}等引用关系。
	 * 查询时这会产生级联查询来加载关联的对象。在这里添加的条件，可被用于过滤<strong>级联的对象</strong>。(注意：是级联引用的指向对象，不是当前对象)<br>
	 * <h3>举例来说：</h3>
	 * <pre><code>
	 *    Student student=new Student();
	 *    student.getQuery().addCondition(QB.eq(Student.Field.id, 1));
	 * 	  student.getQuery().addCascadeCondition( QB.eq(Lesson.Field.name, "语文"));
	 *    Student student=session.load(student);
	 *    //在上例中，无论学生是否有语文课成绩记录，都不影响查询ID=1的学生。
	 *    //然而
	 *    student.getLessons(); //此处得到的Lesson中只会有'语文'
	 *    </code></pre>
	 * 总结来说——CascadeCondition只会影响到级联对象的选择，永远不会影响当前对象的选择。
	 * <br>
	 * 
	 * <h3>为何使用</h3>
	 * 一般来说，仅在一对多的时候使用。在上例中，如果一个学生对应的课程记录有数千条，而我们每次只需要统计学生“语数外”的成绩时，
	 * 将其他课程的成绩查出来显然是十分浪费的。因此我们可以用CascadeCondition进一步缩小一对多查询的选择返回，防止因为一对多关系被滥用造成的性能下降
	 * 
	 * <h3>其他</h3>
	 * 这一特性是即时加载还是延迟加载时都会生效。当用于对一级联时，如果不是左外连接，启用此特性将造成原本一次查出（使用外连接）的结果，被拆成两次查询。
	 * @param refName 发生关系的字段名, 比如A -> B有多条路径的引用关系。用这个名称可以区分出是哪一条引用关系，可以指定间接级联关系，如 member.info
	 * @param conditions
	 * @return 当前Query对象
	 * @since 1.1
	 * @author Jiyi
	 */
	Query<T> addCascadeCondition(String refName,Condition... conditions);
	
	
	/**
	 * 添加扩展查询
	 * @param querys
	 * @return 当前Query对象
	 */
	Query<T> addExtendQuery(Query<?> querys);
	
	/**
	 * 清除所有其他条件，设置为全纪录查询条件
	 * @return 当前Query对象
	 */
	Query<T> setAllRecordsCondition();
	
	/**
	 * 添加一个条件,运算符为equals
	 * @param field
	 * @param value
	 * @return 当前Query对象
	 */
	Query<T> addCondition(Field field,Object value);
	
	/**
	 * 添加一个条件
	 * @param field 字段
	 * @param oper 运算符
	 * @param value 值
	 * @return 当前Query对象
	 */
	Query<T> addCondition(Field field, Operator oper,Object value);
	
	/**
	 * 添加一个条件，如Or Not And Exists NotExists等复杂条件
	 * @param condition 条件
	 * @return 当前Query对象
	 * @see IConditionField
	 * @see IConditionField.And
	 * @see IConditionField.Or
	 * @see IConditionField.Not
	 * @see IConditionField.Exists
	 * @see IConditionField.NotExists
	 * @since 1.1
	 * @author Jiyi
	 * @see IConditionField
	 */
	Query<T> addCondition(IConditionField condition);
	
	/**
	 * 添加一个条件，与之前的条件为And关系
	 * @param condition 条件
	 * @return Query对象本身
	 */
	Query<T> addCondition(Condition condition);
	
	/**
	 * 得到子查询过滤条件
	 * @return 所有的子查询过滤条件
	 */
	Map<Reference, List<Condition>> getFilterCondition();
	
	/**
	 * 目前的设计趋势是逐步将DataObject相分离，用IQuery来代替原来DataObject的查询接口。
	 * @return
	 */
	T getInstance();
	
	/**
	 * 得到Query的其他查询提供者, 
	 * @return 其他外连接查询
	 */
	Query<?>[] getOtherQueryProvider();
	
	/**
	 * 设置自定义的查询表名
	 * @param name
	 */
	void setCustomTableName(String name);
	
	/**
	 * 获取：是否尝试用外连接的方式，一次性查出级联关系
	 * 
	 * @return 如果开启自动外连接功能，返回true
	 * @see jef.database.DbCfg#DB_USE_OUTER_JOIN
	 */
	public boolean isCascadeViaOuterJoin();

	/**
	 * 设置：是否尝试用外连接的方式，一次性查出级联关系
	 * @param cascadeViaOuterJoin 如果为true，将尽量合并数据库查询来减少操作次数。
	 */
	public void setCascadeViaOuterJoin(boolean cascadeViaOuterJoin);
	
	/**
	 * 设置是否要使用级联查询。调用这个方法的效果等同于同时调用下面两个方法<code><pre>
	 * .getResultTransformer().setLoadVsMany(false);
	 * .getResultTransformer().setLoadVsOne(false);
	 * </pre></code>
	 * @param cascade 是否要级联查询
	 * @see jef.database.wrapper.populator.Transformer#setLoadVsMany(boolean)
	 * @see jef.database.wrapper.populator.Transformer#setLoadVsOne(boolean)
	 */
	public void setCascade(boolean cascade);
	

	/**
	 * 是否查询全部记录
	 * @return
	 */
	boolean isAll();

	Map<Field, Object> getUpdateValueMap();

	boolean needUpdate();

	void clearUpdateMap();
	
	void prepareUpdate(Field field, Object newValue);
	
	/**
	 * 查询条件生成器
	 * <strong>试验性功能。<strong>
	 * @return Terms
	 * @see Terms
	 */
	Terms terms();
}
