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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

import jef.common.PairSO;
import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.OperateTarget.TransformerAdapter;
import jef.database.Session.PopulateStrategy;
import jef.database.annotation.PartitionResult;
import jef.database.dialect.type.ResultSetAccessor;
import jef.database.jsqlparser.expression.JpqlDataType;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.query.ParameterProvider;
import jef.database.query.QueryHints;
import jef.database.query.SqlExpression;
import jef.database.routing.jdbc.ExecutionPlan;
import jef.database.routing.jdbc.SelectExecutionPlan;
import jef.database.routing.jdbc.SqlAnalyzer;
import jef.database.routing.jdbc.SqlExecutionParam;
import jef.database.wrapper.ResultIterator;
import jef.database.wrapper.populator.ColumnDescription;
import jef.database.wrapper.populator.Mapper;
import jef.database.wrapper.populator.ResultSetTransformer;
import jef.database.wrapper.populator.Transformer;
import jef.database.wrapper.result.IResultSet;
import jef.database.wrapper.result.MultipleResultSet;
import jef.tools.Assert;
import jef.tools.DateUtils;
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
 * @author Administrator
 * @param <X>
 *            返回结果的参数类型
 * @see jef.database.jsqlparser.expression.JpqlDataType
 */
@SuppressWarnings({ "unchecked", "hiding" })
public class NativeQuery<X> implements javax.persistence.TypedQuery<X>, ParameterProvider {
	private OperateTarget db;
	private NamedQueryConfig config; // 查询本体
	private IntRange range; // 额外的范围要求
	// 实例数据
	private Transformer resultTransformer; // 返回类型
	private Map<Object, Object> nameParams = new HashMap<Object, Object>();// 按名参数

	private LockModeType lock = null;
	private FlushModeType flushType = null;
	private final Map<String, Object> hint = new HashMap<String, Object>();
	private int fetchSize = ORMConfig.getInstance().getGlobalFetchSize();
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

	/**
	 * 从SQL语句加上返回类型构造
	 * 
	 * @param db
	 * @param sql
	 * @param resultClass
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

	NativeQuery(OperateTarget db, NamedQueryConfig config, Transformer t) {
		this.db = db;
		this.resultTransformer = t;
		this.config = config;
		resultTransformer.addStrategy(PopulateStrategy.PLAIN_MODE);
	}

	/**
	 * 获取结果数量<br />
	 * 注意:该方法不是将语句执行后获得全部返回结果，而是尝试将sql语句重写为count语句，然后查询出结果
	 * 
	 * @return count结果
	 */
	public long getResultCount() {
		try {
			SqlExecutionParam parse = config.getCountSqlAndParams(db, this);
			SelectExecutionPlan plan = null;
			if (routing) {
				plan = (SelectExecutionPlan) SqlAnalyzer.getSelectExecutionPlan((Select) parse.statement, parse.params, db);
			}
			boolean debug = ORMConfig.getInstance().debugMode;
			if (plan == null) {
				String sql = parse.statement.toString();
				long start = System.currentTimeMillis();
				Long num = db.innerSelectBySql(sql, ResultSetTransformer.GET_FIRST_LONG, 1, 0, parse.params);
				if (debug) {
					long dbAccess = System.currentTimeMillis();
					LogUtil.show(StringUtils.concat("Count:", String.valueOf(num), "\t [DbAccess]:", String.valueOf(dbAccess - start), "ms) |", db.getTransactionId()));
				}
				return num;
			}else if(plan.isChangeDatasource()!=null){
				OperateTarget db=this.db.getTarget(plan.isChangeDatasource());
				String sql = parse.statement.toString();
				long start = System.currentTimeMillis();
				Long num = db.innerSelectBySql(sql, ResultSetTransformer.GET_FIRST_LONG, 1, 0, parse.params);
				if (debug) {
					long dbAccess = System.currentTimeMillis();
					LogUtil.show(StringUtils.concat("Count:", String.valueOf(num), "\t [DbAccess]:", String.valueOf(dbAccess - start), "ms) |", db.getTransactionId()));
				}
				return num;
			} else if (plan.mustGetAllResultsToCount()) {// 很麻烦的场景——由于分库后启用了group聚合，造成必须先将结果集在内存中混合后才能得到正确的count数……
				int count = doQuery(0, fetchSize, true).size(); // 全量查询，去除排序。排序是不必要的
				LogUtil.warn(StringUtils.concat("The count value was get from InMemory grouping arithmetic, since the 'group by' on multiple databases. |  @", String.valueOf(Thread.currentThread().getId())));
				return count;
			} else {
				long total = 0;
				long start = System.currentTimeMillis();
				for (PartitionResult site : plan.getSites()) {
					total += plan.getCount(site, db);
				}
				LogUtil.show(StringUtils.concat("Count:", String.valueOf(total), "\t [DbAccess]:", String.valueOf(System.currentTimeMillis() - start), "ms) |  @", String.valueOf(Thread.currentThread().getId())));
				return total;
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
	 * @return
	 */
	public ResultIterator<X> getResultIterator() {
		try {
			SqlExecutionParam parse = config.getSqlAndParams(db, this);
			Statement sql = parse.statement;
			String s = sql.toString();

			ORMConfig config = ORMConfig.getInstance();
			boolean debug = config.debugMode;

			SelectExecutionPlan plan = null;
			if (routing) {
				plan = (SelectExecutionPlan) SqlAnalyzer.getSelectExecutionPlan((Select) sql, parse.params, db);
			}
			if (plan == null) {// 普通查询
				if (range != null)
					s = toPageSql(sql, s);
				return db.innerIteratorBySql(s, resultTransformer, 0, fetchSize, parse.params);
			} else if(plan.isChangeDatasource()!=null){// 分表分库查询
				OperateTarget db=this.db.getTarget(plan.isChangeDatasource());
				if (range != null)
					s = toPageSql(sql, s);
				return db.innerIteratorBySql(s, resultTransformer, 0, fetchSize, parse.params);
			} else {// 分表分库查询
				if (plan.isMultiDatabase()) {// 多库
					MultipleResultSet mrs = new MultipleResultSet(config.isCacheResultset(), debug);
					for (PartitionResult site : plan.getSites()) {
						processQuery(db.getTarget(site.getDatabase()), plan.getSql(site, false), 0, mrs);
					}
					plan.parepareInMemoryProcess(range, mrs);
					IResultSet irs = mrs.toSimple(null, this.resultTransformer.getStrategy());
					@SuppressWarnings("rawtypes")
					ResultIterator<X> iter = new ResultIterator.Impl(db.getSession().iterateResultSet(irs, null, resultTransformer), irs);
					return iter;
				} else { // 单库多表，基于Union的查询. 可以使用数据库分页
					PartitionResult site = plan.getSites()[0];
					PairSO<List<Object>> result = plan.getSql(plan.getSites()[0], false);
					s = result.first;
					if (range != null) {
						s = toPageSql(sql, s);
					}
					return db.getTarget(site.getDatabase()).innerIteratorBySql(s, resultTransformer, 0, fetchSize, result.second);
				}
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	private String toPageSql(Statement sql, String s) {
		if (sql instanceof Select) {
			boolean isUnion = ((Select) sql).getSelectBody() instanceof Union;
			s = db.getProfile().toPageSQL(s, range, isUnion);
		}
		return s;
	}

	/**
	 * 执行查询语句，返回结果
	 * 
	 * @return 返回List类型的结果
	 */
	public List<X> getResultList() {
		try {
			return doQuery(0, fetchSize, false);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	private List<X> doQuery(int max, int fetchSize, boolean noOrder) throws SQLException {
		long start = System.currentTimeMillis();
		SqlExecutionParam parse = config.getSqlAndParams(db, this);
		Statement sql = parse.statement;
		SelectExecutionPlan plan = null;
		if (routing) {
			plan = SqlAnalyzer.getSelectExecutionPlan((Select) sql, parse.params, db);
		}
		String s = sql.toString();
		
		long dbAccess;
		List<X> list;
		if (plan == null) {// 普通查询
			if (range != null) {
				s = toPageSql(sql, s);
			}
			TransformerAdapter<X> rst = new TransformerAdapter<X>(resultTransformer,db);
			rst.setOthers(parse);
			list = db.innerSelectBySql(s, rst, max, fetchSize, parse.params);
			dbAccess=rst.dbAccess;
		} else if(plan.isChangeDatasource()!=null){
			this.db=this.db.getTarget(plan.isChangeDatasource());
			if (range != null) {
				s = toPageSql(sql, s);
			}
			TransformerAdapter<X> rst = new TransformerAdapter<X>(resultTransformer,db);
			rst.setOthers(parse);
			list = db.innerSelectBySql(s, rst, max, fetchSize, parse.params);
			dbAccess=rst.dbAccess;
		} else if (plan.isMultiDatabase()) {// 多库
			ORMConfig config = ORMConfig.getInstance();
			boolean debug = config.debugMode;
			MultipleResultSet mrs = new MultipleResultSet(config.isCacheResultset(), debug);
			for (PartitionResult site : plan.getSites()) {
				processQuery(db.getTarget(site.getDatabase()), plan.getSql(site, noOrder), max, mrs);
			}
			dbAccess = System.currentTimeMillis();
			try {
				plan.parepareInMemoryProcess(range, mrs);
				if (noOrder) { // 去除内存排序
					mrs.setInMemoryOrder(null);
				}
				mrs.setInMemoryConnectBy(parse.parseStartWith(mrs.getColumns()));
				IResultSet rsw = mrs.toSimple(null, resultTransformer.getStrategy());
				list = db.populateResultSet(rsw, null, resultTransformer);
			} finally {
				mrs.close();
			}

		} else { // 单库多表，基于Union的查询. 可以使用数据库分页
			PartitionResult pr = plan.getSites()[0];
			PairSO<List<Object>> result = plan.getSql(pr, false);
			s = result.first;
			if (range != null) {
				s = toPageSql(sql, s);
			}
			OperateTarget db = this.db.getTarget(pr.getDatabase());
			TransformerAdapter<X> rst = new TransformerAdapter<X>(resultTransformer,db);
			rst.setOthers(parse);
			list = db.innerSelectBySql(s, rst, max, fetchSize, result.second);
			dbAccess=rst.dbAccess;
		}
		if (ORMConfig.getInstance().isDebugMode()) {
			LogUtil.show(StringUtils.concat("Result Count:", String.valueOf(list.size()), "\t Time cost([DbAccess]:", String.valueOf(dbAccess - start), "ms, [Populate]:", String.valueOf(System.currentTimeMillis() - dbAccess), "ms) |", db.getTransactionId()));
		}
		return list;
	}

	/*
	 * 执行查询动作，将查询结果放入mrs
	 */
	private void processQuery(OperateTarget db, PairSO<List<Object>> sql, int max, MultipleResultSet mrs) throws SQLException {
		StringBuilder sb = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		if (mrs.isDebug())
			sb = new StringBuilder(sql.first.length() + 150).append(sql.first).append(" | ").append(db.getTransactionId());
		try {
			psmt = db.prepareStatement(sql.first, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			BindVariableContext context = new BindVariableContext(psmt, db, sb);
			BindVariableTool.setVariables(context, sql.second);
			if (fetchSize > 0) {
				psmt.setFetchSize(fetchSize);
			}
			if (max > 0) {
				psmt.setMaxRows(max);
			}
			rs = psmt.executeQuery();
			mrs.add(rs, psmt, db);
		} finally {
			if (mrs.isDebug())
				LogUtil.show(sb);
		}
	}

	/**
	 * 设置查询结果的条数限制，即分页 包含了setMaxResult和setFirstResult的功能
	 * 
	 * @param range
	 */
	public void setRange(IntRange range) {
		this.range = range;
	}

	/**
	 * 当确认返回结果只有一条时，使用此方法得到结果。 如果查询条数>1，不会抛出异常，而是返回第一条结果。
	 * 
	 * @return 如果查询结果条数是0，返回null
	 */
	public X getSingleResult() {
		try {
			List<X> list = doQuery(2, 0, false);
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
			List<X> list = doQuery(2, 0, false);
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
			SqlExecutionParam parse = config.getSqlAndParams(db, this);
			Statement sql = parse.statement;
			ExecutionPlan plan = null;
			if (routing) {
				plan = SqlAnalyzer.getExecutionPlan(sql, parse.params, db);
			}
			if (plan == null) {
				return db.innerExecuteSql(parse.statement.toString(), parse.params);
			} else if(plan.isChangeDatasource()!=null){
				return db.getTarget(plan.isChangeDatasource()).innerExecuteSql(parse.statement.toString(), parse.params);
			} else {
				long start = System.currentTimeMillis();
				int total = 0;
				for (PartitionResult site : plan.getSites()) {
					total += plan.processUpdate(site, db);
				}
				if (plan.isMultiDatabase() && ORMConfig.getInstance().debugMode) {
					LogUtil.show(StringUtils.concat("Total Executed:", String.valueOf(total), "\t Time cost([DbAccess]:", String.valueOf(System.currentTimeMillis() - start), "ms) |  @", String.valueOf(Thread.currentThread().getId())));
				}
				return total;
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/**
	 * 限制返回的最大结果数
	 */
	public NativeQuery<X> setMaxResults(int maxResult) {
		if (range == null) {
			range = new IntRange(1, maxResult);
		} else {
			range = new IntRange(range.getStart(), range.getStart() + maxResult - 1);
		}
		return this;
	}

	/**
	 * 获取当前的结果拼装策略
	 * 
	 * @return
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
	 */
	public int getMaxResults() {
		if (range != null)
			return range.getGreatestValue();
		return 0;
	}

	/**
	 * 设置结果的开始偏移（即分页时要跳过的记录数）。从0开始。
	 */
	public NativeQuery<X> setFirstResult(int startPosition) {
		if (range == null) {
			range = new IntRange(startPosition + 1, 5000000);
		} else {
			range = new IntRange(startPosition + 1, range.size() + startPosition);
		}
		return this;
	}

	/**
	 * 得到目前的开始偏移（即分页时要跳过的记录数）。从0开始
	 */
	public int getFirstResult() {
		if (range == null)
			return 0;
		return range.getStart() - 1;
	}

	public NativeQuery<X> setHint(String hintName, Object value) {
		hint.put(hintName, value);
		if (QueryHints.START_LIMIT.equals(hintName)) {
			int[] startLimit = StringUtils.toIntArray(String.valueOf(value), ',');
			int start = startLimit[0] + 1;
			setRange(new IntRange(start, start + startLimit[1] - 1));
		} else if (QueryHints.FETCH_SIZE.equals(hintName)) {
			this.setFetchSize(StringUtils.toInt(String.valueOf(value), 0));
		}
		return this;
	}

	public Map<String, Object> getHints() {
		return Collections.unmodifiableMap(hint);
	}

	/**
	 * 目前不支持的JPA方法 抛出异常
	 * 
	 * @deprecated
	 */
	public <X> X unwrap(Class<X> cls) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 设置查询的绑定变量参数
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
	 * 设置查询的绑定变量参数
	 */
	public NativeQuery<X> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		setParameter(param, value);
		return this;
	}

	/**
	 * 设置查询的绑定变量参数
	 */
	public NativeQuery<X> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		setParameter(param, value);
		return this;
	}

	/**
	 * 设置查询的绑定变量参数
	 */
	public NativeQuery<X> setParameter(String name, Calendar value, TemporalType temporalType) {
		return setParameter(name, value);
	}

	/**
	 * 设置查询的绑定变量参数
	 */
	public NativeQuery<X> setParameter(String name, Date value, TemporalType temporalType) {
		return setParameter(name, value);
	}

	/**
	 * 设置查询的绑定变量参数
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

	/*
	 * 
	 */
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

	/**
	 * 设置参数的值，传入的参数类型为String，
	 * 
	 * @param name
	 * @param value
	 * @return
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
	 * @param value
	 * @return
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
	 * 设置参数的值，传入的参数类型为String，
	 * 
	 * @param name
	 * @param value
	 * @return
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
	 * @param value
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

	/**
	 * 设置参数的值
	 */
	public NativeQuery<X> setParameter(int position, Calendar value, TemporalType temporalType) {
		return setParameter(position, value);
	}

	/**
	 * 以Map形式设置参数的值
	 * 
	 * @param params
	 * @return
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
	 * 设置参数的值
	 */
	public NativeQuery<X> setParameter(int position, Date value, TemporalType temporalType) {
		return setParameter(position, value);
	}

	public Set<Parameter<?>> getParameters() {
		Set<Parameter<?>> result = new HashSet<Parameter<?>>();
		for (JpqlParameter jp : config.getParams(this.db).values()) {
			result.add(jp);
		}
		return result;
	}

	public Parameter<?> getParameter(String name) {
		JpqlParameter param = config.getParams(db).get(name);
		if (param == null) {
			throw new NoSuchElementException(name);
		}
		return param;
	}

	public <X> Parameter<X> getParameter(String name, Class<X> type) {
		JpqlParameter param = config.getParams(db).get(name);
		if (param == null || param.getParameterType() != type) {
			throw new NoSuchElementException(name);
		}
		return param;
	}

	public Parameter<?> getParameter(int position) {
		JpqlParameter param = config.getParams(db).get(position);
		if (param == null) {
			throw new NoSuchElementException(String.valueOf(position));
		}
		return param;
	}

	public <X> Parameter<X> getParameter(int position, Class<X> type) {
		JpqlParameter param = config.getParams(db).get(position);
		if (param == null || param.getParameterType() != type) {
			throw new NoSuchElementException(String.valueOf(position));
		}
		return param;
	}

	/**
	 * JPA接口，目前相关特性未实现，总是返回false
	 */
	public boolean isBound(Parameter<?> param) {
		return false;
	}

	/**
	 * 得到参数的值
	 */
	public Object getParameterValue(String name) {
		return nameParams.get(name);
	}

	/**
	 * 得到参数的值
	 */
	public Object getParameterValue(int position) {
		return nameParams.get(position);
	}

	/**
	 * 得到参数的值
	 */
	public <T> T getParameterValue(Parameter<T> param) {
		if (param.getPosition() != null && param.getPosition() > -1) {
			return (T) getParameterValue(param.getPosition());
		} else {
			return (T) getParameterValue(param.getName());
		}
	}

	/**
	 * 设置FlushType 目前JEF未实现相关特性，该方法可以调用，但对数据库操作无实际影响
	 */
	public javax.persistence.TypedQuery<X> setFlushMode(FlushModeType flushMode) {
		this.flushType = flushMode;
		return this;
	}

	/**
	 * 返回FlushMode 目前JEF未实现相关特性，该方法可以调用，但对数据库操作无实际影响
	 */
	public FlushModeType getFlushMode() {
		return flushType;
	}

	/**
	 * 设置lockMode 目前JEF未实现相关特性，该方法可以调用，但对数据库操作无实际影响
	 */
	public javax.persistence.TypedQuery<X> setLockMode(LockModeType lockMode) {
		this.lock = lockMode;
		return this;
	}

	/**
	 * 返回LockMode 目前JEF未实现相关特性，该方法可以调用，但对数据库操作无实际影响
	 */
	public LockModeType getLockMode() {
		return lock;
	}

	/**
	 * 设置是否为Native查询， SQL即为Native,JPQL则不是
	 * 
	 * @param isNative
	 */
	public void setIsNative(boolean isNative) {
		this.config.setType(isNative ? NamedQueryConfig.TYPE_SQL : NamedQueryConfig.TYPE_JPQL);
	}

	/**
	 * 对于以名称为key的参数，获取其参数值
	 */
	public Object getNamedParam(String name) {
		if (this.nameParams == null)
			return null;
		return nameParams.get(name);
	}

	/**
	 * 对于以序号排列的参数，获取其第index个参数的值
	 */
	public Object getIndexedParam(int index) {
		if (this.nameParams == null)
			return null;
		return nameParams.get(index);
	}

	/**
	 * 对于命名查询，获取其tag
	 * 
	 * @return
	 */
	public String getTag() {
		return config.getTag();
	}

	/**
	 * 得到查询所在的dbclient对象
	 * 
	 * @return
	 */
	public OperateTarget getDb() {
		return db;
	}

	/**
	 * 查询指定的参数是否已经设置过值
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
	 * @param accessor
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

	@Override
	public String toString() {
		return config.toString();
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
			wrapper.setPropertyValue(name, accessor.getProperObject(rs, n));
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
	 * 清除之前设置过的所有参数。 此方法当一个NativeQuery被重复使用时十分有用。
	 */
	public void clearParameters() {
		nameParams.clear();
		hint.clear();
	}

	/**
	 * 清除指定的参数
	 * 
	 * @param name
	 */
	public void clearParameter(String name) {
		nameParams.remove(name);
	}

	/**
	 * 清除指定的参数
	 * 
	 * @param index
	 */
	public void clearParameter(int index) {
		nameParams.remove(index);
	}

	/**
	 * 得到所有的参数名称
	 * 
	 * @return
	 */
	public List<String> getParameterNames() {
		List<String> result = new ArrayList<String>();
		for (Object o : config.getParams(db).keySet()) {
			result.add(String.valueOf(o));
		}
		return result;
	}
}
