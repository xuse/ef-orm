/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.TemporalType;

import jef.common.wrapper.IntRange;
import jef.database.OperateTarget.TransformerAdapter;
import jef.database.OperateTarget.TransformerIteratrAdapter;
import jef.database.Session.PopulateStrategy;
import jef.database.dialect.type.ResultSetAccessor;
import jef.database.jdbc.GenerateKeyReturnOper;
import jef.database.jdbc.result.IResultSet;
import jef.database.jsqlparser.expression.JpqlDataType;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.query.ParameterProvider;
import jef.database.query.QueryHints;
import jef.database.query.SqlExpression;
import jef.database.routing.sql.ExecuteablePlan;
import jef.database.routing.sql.QueryablePlan;
import jef.database.routing.sql.SimpleExecutionPlan;
import jef.database.routing.sql.SqlAnalyzer;
import jef.database.routing.sql.SqlAndParameter;
import jef.database.wrapper.ResultIterator;
import jef.database.wrapper.populator.AbstractResultSetTransformer;
import jef.database.wrapper.populator.ColumnDescription;
import jef.database.wrapper.populator.Mapper;
import jef.database.wrapper.populator.ResultSetExtractor;
import jef.database.wrapper.populator.Transformer;
import jef.tools.Assert;
import jef.tools.DateUtils;
import jef.tools.PageLimit;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapper;

/**
 * JEF的NativeQuery实现(对应JPA的TypedQuery)。 <h2>概览</h2>
 * 让用户能根据临时拼凑的或者预先写好的SQL语句进行数据库查询，查询结果将被转换为用户需要的类型。<br>
 * NativeQuery支持哪些功能？
 * <ul>
 * <li>支持绑定变量，允许在SQL中用占位符来描述变量。</li>
 * <li>一个NativeQuery可携带不同的绑定变量参数值，反复使用</li>
 * <li>可以指定{@code fetch-size} , {@code max-result} 等参数，进行性能调优</li>
 * <li>可以自定义查询结果到返回对象之间的映射关系，根据自定义映射转换结果</li>
 * <li>支持{@code E-SQL}，即对传统SQL进行解析和改写以支持一些高级功能，参见下文《什么是E-SQL》节</li>
 * </ul>
 * <h2>什么是E-SQL</h2>
 * EF-ORM会对用户输入的SQL进行解析，改写，从而使得SQL语句的使用更加方便，EF-ORM将不同数据库DBMS下的SQL语句写法进行了兼容处理。
 * 并且提供给上层统一的SQL写法，为此我们将其称为 E-SQL (Enhanced SQL). E-SQL可以让用户使用以下特性：
 * <ul>
 * <li>Schema重定向</li>
 * <li>数据库方言——语法格式整理</li>
 * <li>数据库方言——函数转换</li>
 * <li>增强的绑定变量占位符表示功能</li>
 * <li>绑定变量占位符中可以指定变量数据类型</li>
 * <li>动态SQL语句——表达式忽略</li>
 * </ul>
 * 
 * <h3>示例</h3> 面我们逐一举例这些特性 <h4>Schema重定向</h4>
 * 在Oracle,PG等数据库下，我们可以跨Schema操作。Oracle数据库会为每个用户启用独立Schema，
 * 例如USERA用户下和USERB用户下都有一张名为TT的表。 我们可以在一个SQL语句中访问两个用户下的表，
 * 
 * <pre>
 * <tt>select * from usera.tt union all select * from userb.tt </tt>
 * </pre>
 * 
 * 但是这样就带来一个问题，在某些场合，实际部署的数据库用户是未定的，在编程时开发人员无法确定今后系统将会以什么用户部署。因此EF-
 * ORM设计了Schema重定向功能。<br>
 * 在开发时，用户根据设计中的虚拟用户名编写代码，而在实际部署时，可以配置文件jef.properties指定虚拟schema对应到部署中的实际schema上
 * 。<br>
 * 例如，上面的SQL语句，如果在jef.properties中配置
 * 
 * <pre>
 * <tt>schema.mapping=USERA:ZHANG, USERB:WANG</tt>
 * </pre>
 * 
 * 那么SQL语句在实际执行时，就变为
 * 
 * <pre>
 * <tt>select * from zhang.tt union all select * from wang.tt //实际被执行的SQL</tt>
 * </pre>
 * 
 * 用schema重定向功能，可以解决开发和部署的 schema耦合问题，为测试、部署等带来更大的灵活性。
 * 
 * <h4>数据库方言——语法格式整理</h4> 根据不同的数据库语法，EF-ORM会在执行SQL语句前根据本地方言对SQL进行修改，以适应当前数据库的需要。<br>
 * <strong>例1：</strong>
 * 
 * <pre>
 * <tt>select t.id||t.name as u from t</tt>
 * </pre>
 * 
 * 在本例中{@code ||}表示字符串相连，这在大部分数据库上执行都没有问题，但是如果在MySQL上执行就不行了，MySQL中{@code ||}
 * 表示或关系，不表示字符串相加。 因此，EF-ORM在MySQL上执行上述E-SQL语句时，实际在数据库上执行的语句变为<br>
 * 
 * <pre>
 * <tt> select concat(t.id, t.name) as u from t</tt>
 * </pre>
 * 
 * <br>
 * 这保证了SQL语句按大多数人的习惯在MYSQL上正常使用。
 * <p>
 * <strong>例2：</strong>
 * 
 * <pre>
 * <tt>select count(*) total from t</tt>
 * </pre>
 * 
 * 这句SQL语句在Oracle上是能正常运行的，但是在postgresql上就不行了。因为postgresql要求每个列的别名前都有as关键字。
 * 对于这种情况EF-ORM会自动为这样的SQL语句加上缺少的as关键字，从而保证SQL语句在Postgres上也能正常执行。
 * 
 * <pre>
 * <tt>select count(*) as total from t</tt>
 * </pre>
 * <p>
 * 这些功能提高了SQL语句的兼容性，能对用户屏蔽数据库方言的差异，避免操作者因为使用了SQL而遇到数据库难以迁移的情况。
 * <p>
 * 注意：并不是所有情况都能实现自动改写SQL，比如有些Oracle的使用者喜欢用+号来表示外连接，写成
 * {@code select t1.*,t2.* from t1,t2 where t1.id=t2.id(+) } 这样，但在其他数据库上不支持。
 * 目前EF-ORM还<strong>不支持</strong>将这种SQL语句改写为其他数据库支持的语法(今后可能会支持)。
 * 因此如果要编写能跨数据库的SQL语句，还是要使用‘OUTER JOIN’这样标准的SQL语法。
 * <p>
 * 
 * <h4>数据库方言——函数转换</h4>
 * EF-ORM能够自动识别SQL语句中的函数，并将其转换为在当前数据库上能够使用的函数。<br>
 * <strong>例1：</strong>
 * 
 * <pre>
 * <tt>select replace(person_name,'张','王') person_name,decode(nvl(gender,'M'),'M','男','女') gender from t_person</tt>
 * </pre>
 * 
 * 这个语句如果在postgresql上执行，就会发现问题，因为postgres不支持nvl和decode函数。 但实际上，框架会将这句SQL修改为
 * 
 * <pre>
 * <tt>select replace(person_name, '张', '王') AS person_name,
 *        CASE
 *          WHEN coalesce(gender, 'M') = 'M' 
 *          THEN '男'
 *          ELSE '女'
 *        END AS gender
 *   from t_person</tt>
 * </pre>
 * 
 * 从而在Postgresql上实现相同的功能。
 * <h4>绑定变量改进</h4>
 * E-SQL中表示参数变量有两种方式 :
 * <ul>
 * <li>:param-name　　(:id :name，用名称表示参数)</li>
 * <li>?param-index　(如 ?1 ?2，用序号表示参数)。</li>
 * 上述绑定变量占位符是和JPA规范完全一致的。<br>
 * 
 * E-SQL中，绑定变量可以声明其参数类型，也可以不声明。比如<br>
 * 
 * <pre>
 * <tt>select count(*) from Person_table where id in (:ids<int>)</tt>
 * </pre>
 * 
 * 也可以写作
 * 
 * <pre>
 * <tt>select count(*) from Person_table where id in (:ids)</tt>
 * </pre>
 * 
 * 类型成名不区分大小写。如果不声明类型，那么传入的参数如果为List&lt;String&gt;，
 * 那么数据库是否能正常执行这个SQL语句取决于JDBC驱动能否支持。（因为数据库里的id字段是number类型而传入了string）。
 * <p>
 * <br>
 * {@linkplain jef.database.jsqlparser.expression.JpqlDataType 各种支持的参数类型和作用}<br>
 * 
 * 
 * <h4>动态SQL语句——表达式忽略</h4>
 * EF-ORM可以根据未传入的参数，动态的省略某些SQL片段。这个特性往往用于某些参数不传场合下的动态条件，避免写大量的SQL。
 * 有点类似于IBatis的动态SQL功能。 我们先来看一个例子
 * 
 * <pre>
 * <code>//SQL语句中写了四个查询条件
 * String sql="select * from t_person where id=:id " +
 * 		"and person_name like :person_name&lt;$string$&gt; " +
 * 		"and currentSchoolId=:schoolId " +
 * 		"and gender=:gender";
 * NativeQuery&lt;Person&gt; query=db.createNativeQuery(sql,Person.class);
 * {
 * 	System.out.println("== 按ID查询 ==");
 * 	query.setParameter("id", 1);
 * 	Person p=query.getSingleResult();  //只传入ID时，其他三个条件消失
 * 	System.out.println(p.getId());
 * 	System.out.println(p);	
 * }
 * {
 * 	System.out.println("== 由于参数'ID'并未清除，所以变为 ID + NAME查询 ==");
 * 	query.setParameter("person_name", "张"); //传入ID和NAME时，其他两个条件消失
 * 	System.out.println(query.getResultList());
 * }
 * {
 * 	System.out.println("== 参数清除后，只传入NAME，按NAME查询 ==");
 * 	query.clearParameters();
 * 	query.setParameter("person_name", "张"); //只传入NAME时，其他三个条件消失
 * 	System.out.println(query.getResultList());
 * }
 * {
 * 	System.out.println("== 按NAME+GENDER查询 ==");
 * 	query.setParameter("gender", "F");  //传入GENDER和NAME时，其他两个条件消失
 * System.out.println(query.getResultList());
 * }
 * {
 * 	query.clearParameters();    //一个条件都不传入时，整个where子句全部消失
 * 	System.out.println(query.getResultList());
 * }</code>
 * </pre>
 * 
 * 上面列举了五种场合，每种场合都没有完整的传递四个WHERE条件。
 * 这种常见需求一般发生在按条件查询中，比较典型的一个例子是用户Web界面上的搜索工具栏，当用户输入条件时
 * ，按条件搜索。当用户未输入条件时，该字段不作为搜索条件
 * 。使用动态SQL功能后，一个固定的SQL语句就能满足整个视图的所有查询场景，极大的简化了视图查询的业务操作。
 *
 * <h3>注意</h3> 不支持多线程。每次使用前需要createNamedQuery或者createNativeQuery.
 * 
 * 
 * 
 * @author Administrator
 * @param <X>
 *            返回结果的参数类型
 * @see jef.database.jsqlparser.expression.JpqlDataType
 */
@SuppressWarnings({ "unchecked", "hiding" })
public class NativeQuery<X> implements javax.persistence.TypedQuery<X>, ParameterProvider {
	private NamedQueryConfig config; // 查询本体、本身线程安全

	private OperateTarget db;
	private LockModeType lock = null;
	private FlushModeType flushType = null;
	private PageLimit range; // 额外的范围要求
	// 实例数据
	private Map<Object, Object> nameParams = new HashMap<Object, Object>();// 按名参数
	private Transformer resultTransformer; // 返回类型

	private final Map<String, Object> hint = new HashMap<String, Object>();
	private int fetchSize = ORMConfig.getInstance().getGlobalFetchSize();

	/**
	 * 是否支持数据路由
	 */
	private boolean routing;

	/**
	 * 是否启用ＳＱＬ语句路由功能
	 * 
	 * @return ＳＱＬ语句路由
	 */
	public boolean isRouting() {
		return routing;
	}

	/**
	 * 设置是否启用ＳＱＬ语句路由功能
	 * 
	 * @param routing
	 *            ＳＱＬ语句路由
	 */
	public NativeQuery<X> setRouting(boolean routing) {
		this.routing = routing;
		return this;
	}

	/**
	 * 设置启用ＳＱＬ语句路由功能
	 * 
	 * @return 当前NativeQuery本身
	 */
	public NativeQuery<X> withRouting() {
		this.routing = true;
		return this;
	}

	/*
	 * 从SQL语句加上返回类型 构造
	 * 
	 * @param db 目标数据库
	 * 
	 * @param sql SQL语句
	 * 
	 * @param resultClass 查询转换器
	 */
	NativeQuery(OperateTarget db, String sql, Transformer t) {
		if (StringUtils.isEmpty(sql)) {
			throw new IllegalArgumentException("Please don't input an empty SQL.");
		}
		this.db = db;
		this.resultTransformer = t;
		this.config = new NamedQueryConfig("", sql, null, 0);
		resultTransformer.addStrategy(PopulateStrategy.PLAIN_MODE);
	}

	/*
	 * 构造，从NamedQueryConfig构造出来
	 */
	NativeQuery(OperateTarget db, NamedQueryConfig config, Transformer t) {
		this.db = db;
		this.resultTransformer = t;
		this.config = config;
		resultTransformer.addStrategy(PopulateStrategy.PLAIN_MODE);
	}

	/**
	 * 获取结果数量<br />
	 * 
	 * <h3>注意</h3> 该方法不是将语句执行后获得全部返回结果，而是尝试将sql语句重写为count语句，然后查询出结果。<br/>
	 * 因此，count返回的数值不受结果集限制的影响。<br />
	 * 如果通过{@link #setMaxResults(int)}或者{@link #setFirstResult(int)}或者
	 * {@link #setRange(IntRange)}设置结果集限制，将不会对COUNT结果产生影响。
	 * 
	 * @return count结果
	 * @see #setFirstResult(int)
	 * @see #setMaxResults(int)
	 * @see #setRange(IntRange)
	 */
	public long getResultCount() {
		try {
			SqlAndParameter paramHolder = config.getCountSqlAndParams(db, this);
			QueryablePlan plan = null;
			if (routing) {
				plan = SqlAnalyzer.getSelectExecutionPlan((Select) paramHolder.statement, paramHolder.getParamsMap(), paramHolder.params, db);
			} else {
				plan = new SimpleExecutionPlan(paramHolder.statement, paramHolder.params, null, db);
			}
			long maxSize = paramHolder.getLimitSpan(); // 查询分页条件，count结果不可能大于分页的最大结果
			paramHolder.setNewLimit(null);
			if (plan.mustGetAllResultsToCount()) {
				// 路由，并且必须遍历结果集场合
				// 很麻烦的场景——由于分库后启用了group聚合，造成必须先将结果集在内存中混合后才能得到正确的count数……
				long total = doQuery(AbstractResultSetTransformer.countResultSet(fetchSize), true); // 全量查询，去除排序。排序是不必要的
				return (maxSize > 0 && maxSize < total) ? maxSize : total;
			} else {
				return plan.getCount(paramHolder, (int) maxSize, fetchSize);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/**
	 * 返回fetchSize
	 * 
	 * @return 每次游标获取的缓存大小
	 */
	public int getFetchSize() {
		if (fetchSize > 0)
			return fetchSize;
		return config.getFetchSize();
	}

	/**
	 * 设置fetchSize
	 * 
	 * @param size
	 *            设置每次获取的缓冲大小
	 */
	public void setFetchSize(int size) {
		this.fetchSize = size;
	}

	/**
	 * 以迭代器模式返回查询结果
	 * 
	 * @return 结果迭代器
	 * @see ResultIterator
	 */
	public ResultIterator<X> getResultIterator() {
		TransformerIteratrAdapter<X> r = new TransformerIteratrAdapter<X>(resultTransformer, this.db);
		r.setFetchSize(fetchSize);
		try {
			return doQuery(r, false);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/**
	 * 执行查询语句，返回结果
	 * 
	 * @return 返回List类型的结果
	 */
	public List<X> getResultList() {
		try {
			TransformerAdapter<X> rst = new TransformerAdapter<X>(resultTransformer, db);
			rst.setFetchSize(fetchSize);
			return doQuery(rst, false);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/**
	 * 真正的执行查询
	 * 
	 * @param extractor
	 *            结果集转换器，查询参数
	 * @param forCount
	 *            如果是count场合，那么返回true。count场合可以去除排序并执行一些SQL变化以优化查询速度。
	 * @return 返回结果
	 * @throws SQLException
	 *             数据库异常
	 */
	private <T> T doQuery(ResultSetExtractor<T> extractor, boolean forCount) throws SQLException {
		SqlAndParameter sqlContext = config.getSqlAndParams(db, this);
		QueryablePlan plan = null;
		if (routing) {
			plan = SqlAnalyzer.getSelectExecutionPlan((Select) sqlContext.statement, sqlContext.getParamsMap(), sqlContext.params, db);
		} else {
			plan = new SimpleExecutionPlan(sqlContext.statement, sqlContext.params, null, db);
		}
		return plan.doQuery(sqlContext, extractor, forCount, range);
	}

	/**
	 * 设置查询结果的条数限制，即分页 包含了{@link #setMaxResults(int)}和
	 * {@link #setFirstResult(int)}的功能<br>
	 * 这一设置会影响 {@link #getResultList()} {@link #getResultIterator()} 的结果。 <br>
	 * 这一设置不影响 {@link #getResultCount()}的结果
	 * 
	 * @param range
	 *            结果集范围。一个含头含尾的区间。
	 * @return this
	 * @see IntRange
	 * @see #getResultList()
	 * @see #getResultIterator()
	 * @see #getResultCount()
	 * @deprecated use {@link #setRange(long, int)}
	 */
	public void setRange(IntRange range) {
		this.range = PageLimit.parse(range);
	}
	
	/**
	 * 设置查询结果的条数限制，即分页 包含了{@link #setMaxResults(int)}和
	 * {@link #setFirstResult(int)}的功能<br>
	 * 这一设置会影响 {@link #getResultList()} {@link #getResultIterator()} 的结果。 <br>
	 * 这一设置不影响 {@link #getResultCount()}的结果
	 * 
	 * @param range
	 *            结果集范围。一个含头含尾的区间。
	 * @return this
	 * @see range
	 * @see #getResultList()
	 * @see #getResultIterator()
	 * @see #getResultCount()
	 */
	public void setRange(PageLimit range) {
		this.range = range;
	}

	/**
	 * 设置查询结果的条数限制，即分页 包含了{@link #setMaxResults(int)}和
	 * {@link #setFirstResult(int)}的功能<br>
	 * 这一设置会影响 {@link #getResultList()} {@link #getResultIterator()} 的结果。 <br>
	 * 这一设置不影响 {@link #getResultCount()}的结果
	 * 
	 * @param start
	 *            结果集范围。相当于SQL中的offset(从0开始)
	 * @param limit
	 *            限定结果条数。相当于SQL中的limit。
	 */
	public void setRange(long start, int limit) {
		this.range = new PageLimit(start, limit);
	}

	/**
	 * 当确认返回结果只有一条时，使用此方法得到结果。 如果查询条数>1，不会抛出异常，而是返回第一条结果。
	 * 
	 * @return 如果查询结果条数是0，返回null
	 */
	public X getSingleResult() {
		TransformerAdapter<X> rst = new TransformerAdapter<X>(resultTransformer, db);
		rst.setMaxRows(2);
		try {
			List<X> list = doQuery(rst, false);
			if (list.isEmpty())
				return null;
			return list.get(0);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " [SQL:" + e.getSQLState() + "]", e);
		}
	}

	/**
	 * 当确认返回结果只有一条时，使用此方法得到结果。 如果查询条数>1，会抛出异常
	 * 
	 * @return 查询结果
	 * @throws NoSuchElementException
	 *             如果查询结果超过1条，抛出
	 */
	public X getSingleOnlyResult() throws NoSuchElementException {
		try {
			List<X> list = doQuery(new TransformerAdapter<X>(resultTransformer, db).setMaxRows(2), false);
			if (list.isEmpty())
				return null;
			if (list.size() > 1) {
				throw new NoSuchElementException("Too many results found.");
			}
			return list.get(0);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " [SQL:" + e.getSQLState() + "]", e);
		}
	}

	/**
	 * 对于各种DDL、insert、update、delete等语句，不需要返回结果的，调用此方法来执行
	 * 
	 * @return 返回影响到的记录条数（针对update\delete）语句
	 */
	public int executeUpdate() {
		try {
			SqlAndParameter parse = config.getSqlAndParams(db, this);
			Statement sql = parse.statement;
			ExecuteablePlan plan = null;
			if (routing) {
				plan = SqlAnalyzer.getExecutionPlan(sql, parse.getParamsMap(), parse.params, db);
			} else {
				plan = new SimpleExecutionPlan(sql, parse.params, null, db);
			}
			return plan.processUpdate(GenerateKeyReturnOper.NONE).getAffectedRows();
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/**
	 * 限制返回的最大结果数<br>
	 * 这一设置会影响 {@link #getResultList()} {@link #getResultIterator()} 的结果。 <br>
	 * 这一设置不影响 {@link #getResultCount()}的结果
	 * 
	 * @param maxResult
	 *            最多返回的结果条数
	 * @return this
	 * @see #getResultList()
	 * @see #getResultIterator()
	 * @see #getResultCount()
	 */
	public NativeQuery<X> setMaxResults(int maxResult) {
		if (range == null) {
			range = new PageLimit(0, maxResult);
		} else {
			range.setLimit(maxResult);
		}
		return this;
	}

	/**
	 * 获取当前的结果拼装策略
	 * 
	 * @return populate strategy
	 * @see PopulateStrategy
	 */
	public PopulateStrategy[] getStrategies() {
		return resultTransformer.getStrategy();
	}

	/**
	 * 设置结果拼装策略，可以同时使用多个选项策略
	 * 
	 * @see {@link jef.database.Session.PopulateStrategy}
	 * @param strategies
	 *            拼装策略
	 */
	public void setStrategies(PopulateStrategy... strategies) {
		resultTransformer.setStrategy(strategies);
	}

	/**
	 * 获取当前设置的最大结果设置
	 * 
	 * @return 查询最大返回结果集
	 */
	public int getMaxResults() {
		if (range != null)
			return range.getLimit();
		return 0;
	}

	/**
	 * 设置结果的开始偏移（即分页时要跳过的记录数）。从0开始。<br>
	 * 这一设置会影响 {@link #getResultList()} {@link #getResultIterator()} 的结果。 <br>
	 * 这一设置不影响 {@link #getResultCount()}的结果
	 * 
	 * @param startPosition
	 *            offset of the result.
	 * @return this (The nativeQuery)
	 * @see #getResultIterator()
	 * @see #getResultList()
	 * @see #getResultCount()
	 */
	public NativeQuery<X> setFirstResult(int startPosition) {
		if (range == null) {
			range = new PageLimit(startPosition, 5000000 - startPosition);
		} else {
			range.setStart(startPosition);
			;
		}
		return this;
	}

	/**
	 * JPA Method<br/>
	 * 得到目前的开始偏移（即分页时要跳过的记录数）。从0开始
	 * 
	 * @return offset值
	 * 
	 */
	public int getFirstResult() {
		if (range == null)
			return 0;
		return range.getStartAsInt();
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 */
	public NativeQuery<X> setHint(String hintName, Object value) {
		hint.put(hintName, value);
		if (QueryHints.START_LIMIT.equals(hintName)) {
			int[] startLimit = StringUtils.toIntArray(String.valueOf(value), ',');
			setRange(startLimit[0], startLimit[1]);
		} else if (QueryHints.FETCH_SIZE.equals(hintName)) {
			this.setFetchSize(StringUtils.toInt(String.valueOf(value), 0));
		}
		return this;
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 */
	public Map<String, Object> getHints() {
		return Collections.unmodifiableMap(hint);
	}

	/**
	 * 目前不支持的JPA方法 抛出异常
	 * 
	 * @deprecated throws UnsupportedOperationException
	 */
	public <X> X unwrap(Class<X> cls) {
		throw new UnsupportedOperationException();
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 */
	public <T> NativeQuery<X> setParameter(Parameter<T> param, T value) {
		if (param.getPosition() != null) {
			setParameter(param.getPosition(), value);
		} else if (StringUtils.isNotEmpty(param.getName())) {
			setParameter(param.getName(), value);
		}
		return this;
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 */
	public NativeQuery<X> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		setParameter(param, value);
		return this;
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 */
	public NativeQuery<X> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		setParameter(param, value);
		return this;
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 */
	public NativeQuery<X> setParameter(String name, Calendar value, TemporalType temporalType) {
		return setParameter(name, value);
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 */
	public NativeQuery<X> setParameter(String name, Date value, TemporalType temporalType) {
		return setParameter(name, value);
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 * <p>
	 * 设置查询的绑定变量参数
	 * 
	 * @param name
	 *            参数名
	 * @param value
	 *            参数值
	 */
	public NativeQuery<X> setParameter(String name, Object value) {
		if (StringUtils.isNotEmpty(name)) {
			JpqlParameter p = config.getParams(db).get(name);
			if (p == null) {
				throw new IllegalArgumentException("the parameter [" + name + "] doesn't exist in the query:" + config.getName());
			}
			value = processValue(p, value);
			this.nameParams.put(name, value);
		}
		return this;
	}

	/**
	 * 设置查询的绑定变量参数
	 * 
	 * @param position
	 *            位置(序号)
	 * @param value
	 *            参数值
	 * @return 当前NativeQuery本身
	 */
	public NativeQuery<X> setParameter(int position, Object value) {
		JpqlParameter p = config.getParams(db).get(Integer.valueOf(position));
		if (p == null) {
			throw new IllegalArgumentException("the parameter [" + position + "] doesn't exist in the named query:" + config.getName());
		}
		value = processValue(p, value);
		nameParams.put(Integer.valueOf(position), value);
		return this;
	}

	/**
	 * 设置参数的值，传入的参数类型为String，
	 * 
	 * @param name
	 *            参数名
	 * @param value
	 *            参数值
	 * @return 当前NativeQuery本身
	 */
	public NativeQuery<X> setParameterByString(String name, String value) {
		if (StringUtils.isNotEmpty(name)) {
			JpqlParameter p = config.getParams(db).get(name);
			if (p == null) {
				throw new IllegalArgumentException("the parameter [" + name + "] doesn't exist in the named query.");
			}
			Object v = value;
			if (p.getDataType() != null) {
				v = toProperType(p.getDataType(), value);
			}
			this.nameParams.put(name, v);
		}
		return this;
	}

	/**
	 * 设置参数的值，传入的参数类型为String[]
	 * 
	 * @param name
	 *            参数名
	 * @param value
	 *            参数值
	 * @return 当前NativeQuery本身
	 */
	public NativeQuery<X> setParameterByString(String name, String[] value) {
		if (StringUtils.isNotEmpty(name)) {
			JpqlParameter p = config.getParams(db).get(name);
			if (p == null) {
				throw new IllegalArgumentException("the parameter [" + name + "] doesn't exist in the named query.");
			}
			Object v = value;
			if (p.getDataType() != null) {
				v = toProperType(p.getDataType(), value);
			}
			this.nameParams.put(name, v);
		}
		return this;
	}

	/**
	 * 设置参数，如果被设置的值为空字符串或null，则不设置
	 * 
	 * @param name
	 *            参数名
	 * @param value
	 *            参数值
	 * @return 当前NativeQuery本身
	 */
	public NativeQuery<X> setNotEmptyParameter(String name, String value) {
		if (!StringUtils.isEmpty(value)) {
			setParameterByString(name, value);
		}
		return this;
	}

	/**
	 * 设置参数，如果被设置的对象为null，则不设置
	 * 
	 * @param name
	 *            参数名
	 * @param value
	 *            参数值
	 * @return 当前NativeQuery本身
	 */
	public NativeQuery<X> setNotNullParameter(String name, Object value) {
		if (value != null) {
			setParameter(name, value);
		}
		return this;
	}

	/**
	 * 设置参数的值，传入的参数类型为String，
	 * 
	 * @param name
	 *            参数名
	 * @param value
	 *            参数值
	 * @return 当前NativeQuery本身
	 */
	public NativeQuery<X> setParameterByString(int position, String value) {
		JpqlParameter p = config.getParams(db).get(position);
		if (p == null) {
			throw new IllegalArgumentException("the parameter [" + position + "] doesn't exist in the named query.");
		}
		Object v = value;
		if (p.getDataType() != null) {
			v = toProperType(p.getDataType(), value);
		}
		nameParams.put(Integer.valueOf(position), v);
		return this;
	}

	/**
	 * 设置参数的值，传入的参数类型为String[]
	 * 
	 * @param name
	 *            参数名
	 * @param value
	 *            参数值
	 * @return
	 */
	public NativeQuery<X> setParameterByString(int position, String[] value) {
		JpqlParameter p = config.getParams(db).get(position);
		if (p == null) {
			throw new IllegalArgumentException("the parameter [" + position + "] doesn't exist in the named query.");
		}
		Object v = value;
		if (p.getDataType() != null) {
			v = toProperType(p.getDataType(), value);
		}
		nameParams.put(Integer.valueOf(position), v);
		return this;
	}

	/**
	 * 设置参数的值
	 */
	public NativeQuery<X> setParameter(int position, Calendar value, TemporalType temporalType) {
		return setParameter(position, fixTemporal(value.getTime(), temporalType));
	}

	/*
	 * 将参数按照命名查询中的类型提示转换为合适的类型
	 */
	private Object toProperType(JpqlDataType type, String[] value) {
		// 如果是动态SQL片段类型，则将数组转换成1个String值。
		if (JpqlDataType.SQL.equals(type)) {
			return new SqlExpression(StringUtils.join(value));
		}

		Object[] result = new Object[value.length];
		for (int i = 0; i < value.length; i++) {
			result[i] = toProperType(type, value[i]);
		}
		return result;
	}

	/**
	 * 以Map形式设置参数的值
	 * 
	 * @param params
	 *            所有参数的Map
	 * @return this
	 */
	public NativeQuery<X> setParameterMap(Map<String, Object> params) {
		if (params == null)
			return this;
		for (String key : params.keySet()) {
			setParameter(key, params.get(key));
		}
		return this;
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 */
	public NativeQuery<X> setParameter(int position, Date value, TemporalType temporalType) {
		return setParameter(position, value);
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 */
	public Set<Parameter<?>> getParameters() {
		Set<Parameter<?>> result = new HashSet<Parameter<?>>();
		for (JpqlParameter jp : config.getParams(this.db).values()) {
			result.add(jp);
		}
		return result;
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 */
	public Parameter<?> getParameter(String name) {
		JpqlParameter param = config.getParams(db).get(name);
		if (param == null) {
			throw new NoSuchElementException(name);
		}
		return param;
	}

	/**
	 * JPA规范方法 {@inheritDoc}
	 */
	public <X> Parameter<X> getParameter(String name, Class<X> type) {
		JpqlParameter param = config.getParams(db).get(name);
		if (param == null || param.getParameterType() != type) {
			throw new NoSuchElementException(name);
		}
		return param;
	}

	/**
	 * JPA规范方法，获得指定的参数 {@inheritDoc}
	 */
	public Parameter<?> getParameter(int position) {
		JpqlParameter param = config.getParams(db).get(position);
		if (param == null) {
			throw new NoSuchElementException(String.valueOf(position));
		}
		return param;
	}

	/**
	 * JPA规范方法，获得指定的参数 {@inheritDoc}
	 */
	public <X> Parameter<X> getParameter(int position, Class<X> type) {
		JpqlParameter param = config.getParams(db).get(position);
		if (param == null || param.getParameterType() != type) {
			throw new NoSuchElementException(String.valueOf(position));
		}
		return param;
	}

	/**
	 * JPA规范方法，目前相关特性未实现，总是返回false {@inheritDoc}
	 */
	public boolean isBound(Parameter<?> param) {
		return false;
	}

	/**
	 * JPA规范方法，得到参数的值 {@inheritDoc}
	 */
	public Object getParameterValue(String name) {
		return nameParams.get(name);
	}

	/**
	 * JPA规范方法，得到参数的值 {@inheritDoc}
	 */
	public Object getParameterValue(int position) {
		return nameParams.get(position);
	}

	/**
	 * JPA规范方法，得到参数的值 {@inheritDoc}
	 */
	public <T> T getParameterValue(Parameter<T> param) {
		if (param.getPosition() != null && param.getPosition() > -1) {
			return (T) getParameterValue(param.getPosition());
		} else {
			return (T) getParameterValue(param.getName());
		}
	}

	/**
	 * JPA规范方法。 {@inheritDoc} 设置FlushType 目前JEF未实现相关特性，该方法可以调用，但对数据库操作无实际影响
	 * 
	 */
	public javax.persistence.TypedQuery<X> setFlushMode(FlushModeType flushMode) {
		this.flushType = flushMode;
		return this;
	}

	/**
	 * JPA规范方法。 {@inheritDoc} 返回FlushMode 目前JEF未实现相关特性，该方法可以调用，但对数据库操作无实际影响
	 */
	public FlushModeType getFlushMode() {
		return flushType;
	}

	/**
	 * JPA规范方法。 {@inheritDoc} 设置lockMode 目前JEF未实现相关特性，该方法可以调用，但对数据库操作无实际影响
	 */
	public javax.persistence.TypedQuery<X> setLockMode(LockModeType lockMode) {
		this.lock = lockMode;
		return this;
	}

	/**
	 * JPA规范方法。 {@inheritDoc} 返回LockMode 目前JEF未实现相关特性，该方法可以调用，但对数据库操作无实际影响
	 */
	public LockModeType getLockMode() {
		return lock;
	}

	/**
	 * 设置是否为Native查询，
	 * 
	 * @param isNative
	 *            SQL即为Native,JPQL则不是
	 */
	public void setIsNative(boolean isNative) {
		this.config.setType(isNative ? NamedQueryConfig.TYPE_SQL : NamedQueryConfig.TYPE_JPQL);
	}

	/**
	 * JPA规范方法。对于以名称为key的参数，获取其参数值 {@inheritDoc}
	 * 
	 * @param 参数名
	 * @return 参数值
	 */
	public Object getNamedParam(String name) {
		if (this.nameParams == null)
			return null;
		return nameParams.get(name);
	}

	/**
	 * JPA规范方法。对于以序号排列的参数，获取其第index个参数的值 {@inheritDoc}
	 * 
	 * @param index
	 *            参数序号
	 * @return 参数值
	 */
	public Object getIndexedParam(int index) {
		if (this.nameParams == null)
			return null;
		return nameParams.get(index);
	}

	/**
	 * 对于命名查询，获取其tag
	 * 
	 * @return Tag
	 */
	public String getTag() {
		return config.getTag();
	}

	/**
	 * 得到查询所在的数据库操作对象
	 * 
	 * @return 数据库操作对象
	 */
	public OperateTarget getDb() {
		return db;
	}

	/**
	 * JPA规范方法。查询指定的参数是否已经设置过值
	 * 
	 * @param key
	 *            检查某个位置是否已经设置过参数
	 * 
	 */
	public boolean containsParam(Object key) {
		return nameParams.containsKey(key);
	}

	/**
	 * 设置一个字段的列值转换器。 当查询返回对象是Var/VarObject/Map等不确定类型的容器时，字段的值将使用JDBC驱动默认返回的对象类型。<br>
	 * 这种情况下，可能不能准确预期返回的数据类型。（比如数值在某些数据库上是Integer,某些数据库上是BigDecimal，
	 * Boolean类型的返回结果就更为不确定了。）<br>
	 * 使用这个接口可以明确指定特定列返回的数据类型。 <h3>举例</h3> <code><pre>
	 * NativeQuery<Var> query=db.createNativeQuery("select 1 as bool_column from dual",Var.class);
	 * 
	 * //指定列bool_column以Boolean格式读取。如果不指定，那么该列值将被转换为Integer.
	 * query.setColumnAccessor("bool_column", ColumnMappings.BOOLEAN);
	 * Boolean flag=query.getSingleResult().get("bool_column");
	 * </pre></code>
	 * 
	 * 
	 * @param name
	 *            列名
	 * @param accessor
	 *            结果转换器
	 * @deprecated 使用难度大，后续考虑用别的形式封装
	 */
	public void setColumnAccessor(String name, ResultSetAccessor accessor) {
		resultTransformer.ignoreColumn(name);
		resultTransformer.addMapper(new DefaultMapperAccessorAdapter(name, accessor));
	}

	/**
	 * 获得结果集的转换配置
	 * 
	 * @return
	 */
	public Transformer getResultTransformer() {
		return resultTransformer;
	}

	/**
	 * 清除之前设置过的所有参数。 <br>
	 * 此方法当一个NativeQuery被重复使用时十分有用。
	 */
	public void clearParameters() {
		nameParams.clear();
		hint.clear();
	}

	/**
	 * 清除指定的参数
	 * 
	 * @param name
	 *            参数名
	 */
	public void clearParameter(String name) {
		nameParams.remove(name);
	}

	/**
	 * 清除指定的参数
	 * 
	 * @param index
	 *            序号
	 */
	public void clearParameter(int index) {
		nameParams.remove(index);
	}

	/**
	 * 得到所有的参数名称
	 * 
	 * @return 所有参数的名称
	 */
	public List<String> getParameterNames() {
		List<String> result = new ArrayList<String>();
		for (Object o : config.getParams(db).keySet()) {
			result.add(String.valueOf(o));
		}
		return result;
	}

	@Override
	public String toString() {
		return config.toString();
	}

	/*
	 * 转换String为合适的参数类型
	 * 
	 * @param type
	 * 
	 * @param value
	 * 
	 * @return
	 */
	private Object toProperType(JpqlDataType type, String value) {
		switch (type) {
		case DATE:
			return DateUtils.toSqlDate(DateUtils.autoParse(value));
		case BOOLEAN:
			return StringUtils.toBoolean(value, null);
		case DOUBLE:
			return StringUtils.toDouble(value, 0.0);
		case FLOAT:
			return StringUtils.toFloat(value, 0.0f);
		case INT:
			return StringUtils.toInt(value, 0);
		case LONG:
			return StringUtils.toLong(value, 0L);
		case SHORT:
			return (short) StringUtils.toInt(value, 0);
		case TIMESTAMP:
			return DateUtils.toSqlTimeStamp(DateUtils.autoParse(value));
		case SQL:
			return new SqlExpression(value);
		case $STRING:
			return "%".concat(value);
		case STRING$:
			return value.concat("%");
		case $STRING$:
			StringBuilder sb = new StringBuilder(value.length() + 2);
			return sb.append('%').append(value).append('%').toString();
		default:
			return value;
		}
	}

	private Object processValue(JpqlParameter p, Object value) {
		JpqlDataType type = p.getDataType();
		if (value instanceof String) {
			if (type != null) {
				value = toProperType(type, (String) value);
			}
		} else if ((value instanceof java.util.Date)) {
			Class<?> clz = value.getClass();
			if (clz == java.sql.Time.class || clz == java.sql.Timestamp.class || clz == java.sql.Time.class) {
				// do nothing
			} else if (type == JpqlDataType.TIMESTAMP) {
				value = new java.sql.Timestamp(((java.util.Date) value).getTime());
			} else {
				value = new java.sql.Date(((java.util.Date) value).getTime());
			}
		}
		// 如果是动态SQL片段类型且参数值是数组类型，则将数组转换成1个String值。
		else if (JpqlDataType.SQL.equals(type) && value instanceof Object[]) {
			value = new SqlExpression(StringUtils.join((Object[]) value));
		} else if (value != null) {
			if (type == JpqlDataType.STRING) {
				value = String.valueOf(value);
			}
		}
		return value;
	}

	private Object fixTemporal(Date time, TemporalType temporalType) {
		if (time == null)
			return time;
		switch (temporalType) {
		case DATE:
			if (time instanceof java.sql.Date)
				return time;
			return new java.sql.Date(time.getTime());
		case TIME:
			if (time instanceof java.sql.Time)
				return time;
			return new java.sql.Time(time.getTime());
		case TIMESTAMP:
			if (time instanceof java.sql.Timestamp)
				return time;
			return new java.sql.Timestamp(time.getTime());
		default:
			return time;
		}
	}

	private static final class DefaultMapperAccessorAdapter extends Mapper<Object> {
		private int n;
		private String name;
		private ResultSetAccessor accessor;

		public DefaultMapperAccessorAdapter(String name, ResultSetAccessor accessor) {
			this.accessor = accessor;
			this.name = name;
		}

		public void process(BeanWrapper wrapper, IResultSet rs) throws SQLException {
			wrapper.setPropertyValue(name, accessor.jdbcGet(rs, n));
		}

		protected void transform(Object wrapped, IResultSet rs) {
		}

		public void prepare(Map<String, ColumnDescription> nameIndex) {
			ColumnDescription columnDesc = nameIndex.get(name.toUpperCase());
			Assert.notNull(columnDesc);
			this.n = columnDesc.getN();
		}
	}

	/**
	 * 使用当前的Query配置创建一个新的NativeQuery对象
	 * @param target
	 * @return
	 */
	public NativeQuery<X> clone(Session session,String dbKey) {
		OperateTarget target = this.db;
		if(session!=null){
			target=session.selectTarget(dbKey==null? this.db.getDbkey(): dbKey);
		}else if(dbKey!=null){
			target=this.db.getTarget(dbKey);
		}
		return new NativeQuery<X>(target, this.config, this.resultTransformer);
	}
}
