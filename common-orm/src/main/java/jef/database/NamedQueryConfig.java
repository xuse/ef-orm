package jef.database;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import jef.common.Entry;
import jef.database.annotation.EasyEntity;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.RemovedDelayProcess;
import jef.database.jsqlparser.SelectToCountWrapper;
import jef.database.jsqlparser.SqlFunctionlocalization;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.expression.operators.relational.Between;
import jef.database.jsqlparser.expression.operators.relational.EqualsTo;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.jsqlparser.expression.operators.relational.GreaterThan;
import jef.database.jsqlparser.expression.operators.relational.GreaterThanEquals;
import jef.database.jsqlparser.expression.operators.relational.InExpression;
import jef.database.jsqlparser.expression.operators.relational.LikeExpression;
import jef.database.jsqlparser.expression.operators.relational.MinorThan;
import jef.database.jsqlparser.expression.operators.relational.MinorThanEquals;
import jef.database.jsqlparser.expression.operators.relational.NotEqualsTo;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.meta.MetaHolder;
import jef.database.query.ParameterProvider;
import jef.database.query.ParameterProvider.MapProvider;
import jef.database.query.SqlExpression;
import jef.database.routing.sql.SqlAndParameter;
import jef.tools.ArrayUtils;
import jef.tools.StringUtils;

/**
 * 描述一个命名查询的配置.
 * 
 * <h3>什么是命名查询</h3>
 * 命名查询即Named-Query,在Hibernate和JPA中都有相关的功能定义。简单来说，命名查询就是将查询语句(SQL,HQL,JPQL等)
 * 事先编写好， 然后为其指定一个名称。<br>
 * 在使用ORM框架时，取出事先解析好的查询，向其中填入绑定变量的参数，形成完整的查询。
 * 
 * <h3>EF-ORM的命名查询和上述两种框架定义有什么不同</h3> EF-ORM也支持命名查询，机制和上述框架相似，具体有以下的不同。
 * <ul>
 * <li>命名查询默认定义在配置文件 named-queries.xml中。不支持使用Annotation等方法定义</li>
 * <li>命名查询也可以定义在数据库表中，数据库表的名称可由用户配置</li>
 * <li>命名查询可以支持 {@linkplain jef.database.NativeQuery E-SQL}
 * 和JPQL两种语法（后者特性未全部实现,不推荐）</li>
 * <li>由于支持E-SQL，命名查询可以实现动态SQL语句的功能，配置XML的配置功能，比较近似与IBatis的操作方式</li>
 * </ul>
 * 
 * <h3>使用示例</h3> 在named-queries.xml中配置
 * 
 * <pre>
 * <tt>&lt;query name = "testIn" type="sql" fetch-size="100" &gt;
 *  	&lt;![CDATA[
 *  		   select * from person_table where id in (:names&lt;int&gt;)
 *  	]]&gt;
 * &lt;/query&gt;</tt>
 * </pre>
 * 
 * 上例中,:names就是一个绑定变量占位符。实际使用方式如下：
 * 
 * <pre>
 * <tt>  ...
 *    Session session=getSession();
 *    NativeQuery&lt;Person&gt; query=session.createNamedQuery("testIn",Person.class);
 *    query.setParam("names",new String[]{"张三","李四","王五"});
 *    List&lt;Person&gt; persons=query.getResultList();   //相当于执行了 select * from person_table where id in ('张三','李四','王五') 
 *   ...
 * </tt>
 * </pre>
 * 
 * @author jiyi
 * @see jef.database.NativeQuery
 * 
 */
@EasyEntity(checkEnhanced = false)
@Entity
@javax.persistence.Table(name = "NAMED_QUERIES")
public class NamedQueryConfig extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;
	public static final int TYPE_SQL = 0;
	public static final int TYPE_JPQL = 1;

	@Id
	@Column(name = "NAME")
	private String name;

	@Column(name = "SQL_TEXT", length = 4000)
	private String rawsql;

	/**
	 * 设置该命名查询的类型，是SQL，还是JPQL(TYPE_JPQL/TYPE_SQL)
	 */
	@Column(name = "TYPE", precision = 1)
	private int type;
	/**
	 * 标记
	 */
	@Column(name = "TAG")
	private String tag;

	/**
	 * 备注信息
	 */
	@Column(name = "REMARK")
	private String remark;

	/**
	 * fetchSize of Result. 
	 */
	@Column(name = "FETCH_SIZE", precision = 6)
	private int fetchSize;

	private boolean fromDb = false;

	private Map<DatabaseDialect, DialectCase> datas = new IdentityHashMap<DatabaseDialect, DialectCase>();;

	public boolean isFromDb() {
		return fromDb;
	}

	public void setFromDb(boolean fromDb) {
		this.fromDb = fromDb;
	}

	private static class DialectCase {
		Statement statement;
		jef.database.jsqlparser.statement.select.Select count;
		Map<Object, JpqlParameter> params;
		RemovedDelayProcess delays;
		public Limit countLimit;
	}

	/*
	 * 解析SQL语句，改写
	 */
	private static DialectCase analy(String sql, int type, OperateTarget db) throws SQLException {
		final DatabaseDialect profile = db.getProfile();
		try {
			Statement st = DbUtils.parseStatement(sql);
			final Map<Object, JpqlParameter> params = new HashMap<Object, JpqlParameter>();
			// Schema重定向处理：将SQL语句中的schema替换为映射后的schema
			st.accept(new VisitorAdapter() {
				@Override
				public void visit(JpqlParameter param) {
					params.put(param.getKey(), param);
				}

				@Override
				public void visit(Table table) {
					String schema = table.getSchemaName();
					if (schema != null) {
						String newSchema = MetaHolder.getMappingSchema(schema);
						if (newSchema != schema) {
							table.setSchemaName(newSchema);
						}
					}
					if (profile.containKeyword(table.getName())) {
						table.setName(DbUtils.escapeColumn(profile, table.getName()));
					}
				}

				@Override
				public void visit(jef.database.jsqlparser.expression.Column c) {
					String schema = c.getSchema();
					if (schema != null) {
						String newSchema = MetaHolder.getMappingSchema(schema);
						if (newSchema != schema) {
							c.setSchema(newSchema);
						}
					}
					if (profile.containKeyword(c.getColumnName())) {
						c.setColumnName(DbUtils.escapeColumn(profile, c.getColumnName()));
					}
				}

			});
			// 进行本地语言转化
			SqlFunctionlocalization localization = new SqlFunctionlocalization(profile, db);
			st.accept(localization);

			if (type == TYPE_JPQL)
				st.accept(new JPQLSelectConvert(profile));

			DialectCase result = new DialectCase();
			result.statement = st;
			result.params = params;
			if(localization.delayLimit!=null || localization.delayStartWith!=null){
				result.delays=new RemovedDelayProcess(localization.delayLimit,localization.delayStartWith);
			}
			return result;
		} catch (ParseException e) {
			String message = e.getMessage();
			int n = message.indexOf("Was expecting");
			if (n > -1) {
				message = message.substring(0, n);
			}
			throw new SQLException(StringUtils.concat("Parse error:", sql, "\n", message));
		}
	}

	public NamedQueryConfig() {
	};

	public NamedQueryConfig(String name, String sql, String type, int fetchSize) {
		stopUpdate();
		this.rawsql = sql;
		this.name = name;
		this.fetchSize = fetchSize;
		if ("jpql".equalsIgnoreCase(type)) {
			this.type = TYPE_JPQL;
		} else {
			this.type = TYPE_SQL;
		}
	}

	/**
	 * 获得SQL语句中所有的参数和定义
	 * 
	 * @param db
	 * @return
	 * @throws SQLException
	 */
	public Map<Object, JpqlParameter> getParams(OperateTarget db) {
		DialectCase dc;
		try {
			dc = getDialectCase(db);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
		return dc.params;
	}

	/**
	 * 得到SQL和绑定参数
	 * 
	 * @param db
	 * @param prov
	 * @return 要执行的语句和绑定变量列表
	 * @throws SQLException
	 */
	public SqlAndParameter getSqlAndParams(OperateTarget db, ParameterProvider prov) throws SQLException {
		DialectCase dc = getDialectCase(db);
		SqlAndParameter result= applyParam(dc.statement, prov);
		result.setInMemoryClause(dc.delays);
		return result;
	}

	private DialectCase getDialectCase(OperateTarget db) throws SQLException {
		DatabaseDialect profile = db.getProfile();
		if(datas==null){
			//当使用testNamedQueryConfigedInDb案例时，由于使用Unsafe方式构造对象，故构造器方法未运行造成datas为null;
			 datas= new IdentityHashMap<DatabaseDialect, DialectCase>();
		}
		DialectCase dc = datas.get(profile);
		if (dc == null) {
			synchronized (datas) {
				if((dc=datas.get(profile))==null){
					dc = analy(this.rawsql, this.type, db);
					datas.put(profile, dc);
				}
			}
		}
		return dc;
	}

	/**
	 * 得到修改后的count语句和绑定参数 注意只有select语句能修改成count语句
	 * 
	 * @param db
	 * @param prov
	 * @return
	 * @throws SQLException
	 */
	public SqlAndParameter getCountSqlAndParams(OperateTarget db, ParameterProvider prov) throws SQLException {
		DialectCase dc = getDialectCase(db);
		if (dc.count == null) {
			if (dc.statement instanceof jef.database.jsqlparser.statement.select.Select) {
				SelectBody oldBody = ((jef.database.jsqlparser.statement.select.Select) dc.statement).getSelectBody();
				SelectToCountWrapper body = null;
				if (oldBody instanceof PlainSelect) {
					body = new SelectToCountWrapper((PlainSelect) oldBody, db.getProfile());
				} else if (oldBody instanceof Union) {
					body = new SelectToCountWrapper((Union) oldBody);
				}
				if (body == null) {
					throw new SQLException("Can not generate count SQL statement for " + dc.statement.getClass().getName());
				}
				jef.database.jsqlparser.statement.select.Select ctst = new jef.database.jsqlparser.statement.select.Select();
				ctst.setSelectBody(body);
				dc.count = ctst;
				if(dc.delays!=null && dc.delays.limit!=null){
					dc.countLimit=dc.delays.limit;
				}else{
					dc.countLimit=body.getRemovedLimit();
				}
			} else {
				throw new IllegalArgumentException();
			}
		}
		SqlAndParameter result= applyParam(dc.count, prov);
		result.setInMemoryClause(dc.delays);
		result.setLimit(dc.countLimit);
		return result;
	}

	private final static class ParamApplier extends VisitorAdapter {
		private ParameterProvider prov;
		private List<Object> params;

		public ParamApplier(ParameterProvider prov, List<Object> params) {
			this.prov = prov;
			this.params = params;
		}

		// 进行绑定变量匹配
		@Override
		public void visit(JpqlParameter param) {
			Object value = null;
			boolean contains;
			if (param.isIndexParam()) {
				value = prov.getIndexedParam(param.getIndex());
				contains = prov.containsParam(param.getIndex());
			} else {
				value = prov.getNamedParam(param.getName());
				contains = prov.containsParam(param.getName());
			}

			if (value instanceof SqlExpression) {
				param.setResolved(((SqlExpression) value).getText());
			} else if (value != null) {
				if (value.getClass().isArray()) {
					int size = Array.getLength(value);
					if (value.getClass().getComponentType().isPrimitive()) {
						value = ArrayUtils.toObject(value);
					}
					for (Object v : (Object[]) value) {
						params.add(v);
					}
					param.setResolved(size);
				} else if (value instanceof Collection) {
					int size = ((Collection<?>) value).size();
					for (Object v : (Collection<?>) value) {
						params.add(v);
					}
					param.setResolved(size);
				} else {
					params.add(value);
					param.setResolved(0);
				}
			} else if (contains) {
				params.add(value);
				param.setResolved(0);
			} else {
				param.setNotUsed();
			}
		}

		@Override
		public void visit(NotEqualsTo notEqualsTo) {
			super.visit(notEqualsTo);
			notEqualsTo.checkEmpty();
		}

		@Override
		public void visit(InExpression inExpression) {
			super.visit(inExpression);
			inExpression.setEmpty(Boolean.FALSE);
			if (inExpression.getItemsList() instanceof ExpressionList) {
				ExpressionList list0 = (ExpressionList) inExpression.getItemsList();
				List<Expression> list = list0.getExpressions();
				if (list.size() == 1 && (list.get(0) instanceof JpqlParameter)) {
					JpqlParameter p = (JpqlParameter) list.get(0);
					if (p.resolvedCount() == -1) {
						inExpression.setEmpty(Boolean.TRUE);
					}
				}
			}
		}

		@Override
		public void visit(Between between) {
			super.visit(between);
			if (between.getBetweenExpressionStart() instanceof JpqlParameter) {
				JpqlParameter p = (JpqlParameter) between.getBetweenExpressionStart();
				if (p.resolvedCount() == -1) {
					between.setEmpty(Boolean.TRUE);
					return;
				}
			}
			if (between.getBetweenExpressionEnd() instanceof JpqlParameter) {
				JpqlParameter p = (JpqlParameter) between.getBetweenExpressionEnd();
				if (p.resolvedCount() == -1) {
					between.setEmpty(Boolean.TRUE);
					return;
				}
			}
			between.setEmpty(Boolean.FALSE);
		}

		@Override
		public void visit(EqualsTo equalsTo) {
			super.visit(equalsTo);
			equalsTo.checkEmpty();
		}

		@Override
		public void visit(MinorThan minorThan) {
			super.visit(minorThan);
			minorThan.checkEmpty();
		}

		@Override
		public void visit(MinorThanEquals minorThanEquals) {
			super.visit(minorThanEquals);
			minorThanEquals.checkEmpty();
		}

		@Override
		public void visit(GreaterThan greaterThan) {
			super.visit(greaterThan);
			greaterThan.checkEmpty();
		}

		@Override
		public void visit(GreaterThanEquals greaterThanEquals) {
			super.visit(greaterThanEquals);
			greaterThanEquals.checkEmpty();
		}

		@Override
		public void visit(LikeExpression likeExpression) {
			super.visit(likeExpression);
			likeExpression.checkEmpty();
		}
	}

	/**
	 * 在指定的SQL表达式中应用参数
	 * 
	 * @param ex
	 * @param prov
	 * @return
	 */
	public static Entry<String, List<Object>> applyParam(Expression ex, MapProvider prov) {
		final List<Object> params = new ArrayList<Object>();
		ex.accept(new ParamApplier(prov, params));
		return new Entry<String, List<Object>>(ex.toString(), params);
	}

	/*
	 * 返回应用参数后的查询
	 */
	public static SqlAndParameter applyParam(Statement st, final ParameterProvider prov) {
		final List<Object> params = new ArrayList<Object>();
		st.accept(new ParamApplier(prov, params));
		return new SqlAndParameter(st, params,prov);
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String toString() {
		if (this.datas.isEmpty()) {
			return rawsql;
		} else {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<DatabaseDialect, DialectCase> e : datas.entrySet()) {
				DialectCase dc = e.getValue();
				sb.append(e.getKey().getName()).append(":");
				sb.append(dc.statement).append("\n");
			}
			return sb.toString();
		}
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getRawsql() {
		return rawsql;
	}

	public void setRawsql(String rawsql) {
		this.rawsql = rawsql;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getFetchSize() {
		return fetchSize;
	}

	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	public enum Field implements jef.database.Field {
		rawsql, name, type, tag, remark, fetchSize
	}
}
