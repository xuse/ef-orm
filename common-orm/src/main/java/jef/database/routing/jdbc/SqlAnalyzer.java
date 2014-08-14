package jef.database.routing.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.common.PairSO;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.OperateTarget;
import jef.database.annotation.PartitionFunction;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionResult;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.expression.JdbcParameter;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.Parenthesis;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.expression.operators.conditional.AndExpression;
import jef.database.jsqlparser.expression.operators.conditional.OrExpression;
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
import jef.database.jsqlparser.statement.delete.Delete;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.statement.update.Update;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.Notable;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.SqlValue;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetadataAdapter;
import jef.database.query.ComplexDimension;
import jef.database.query.Dimension;
import jef.database.query.RangeDimension;
import jef.database.query.RegexpDimension;
import jef.database.wrapper.clause.GroupByItem;
import jef.database.wrapper.clause.GroupFunctionType;
import jef.database.wrapper.clause.InMemoryGroupByHaving;
import jef.database.wrapper.populator.Mappers;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

public class SqlAnalyzer {

	public static ExecutionPlan getExecutionPlan(Statement sql, List<Object> value, OperateTarget db) {
		TableMetaCollector collector = new TableMetaCollector();
		sql.accept(collector);
		if(collector.get()==null)return null;
		MetadataAdapter meta=collector.get();
		if (meta == null || meta.getPartition() == null) {
			return null;
		}
		Map<Expression, Object> params = reverse(sql, value); // 参数对应关系还原

		if (sql instanceof Insert) {
			return getExePlan(meta, (Insert) sql, params,db,collector.getModificationPoints());
		} else if (sql instanceof Update) {
			return getExePlan(meta, (Update) sql, params,db,collector.getModificationPoints());
		} else if (sql instanceof Delete) {
			return getExePlan(meta, (Delete) sql, params,db,collector.getModificationPoints());
		} else {
			Select select = (Select) sql;
			SelectBody body = select.getSelectBody();
			if (body instanceof PlainSelect) {
				return getExePlan(meta, (PlainSelect) body, params,db,collector.getModificationPoints());
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	// 将顺序的参数重新变为和JpqlParameter对应的map
	static class ParamReverser extends VisitorAdapter {
		ParamReverser(List<Object> raw) {
			this.rawParams = raw.iterator();
		}

		final Map<Expression, Object> params = new IdentityHashMap<Expression, Object>();
		private Iterator<Object> rawParams;

		@Override
		public void visit(JpqlParameter parameter) {
			int res = parameter.resolvedCount();
			if (res == 0) {
				params.put(parameter, rawParams.next());
			} else if (res > 0) {
				Object[] array = new Object[res];
				for (int i = 0; i < res; i++) {
					array[i] = rawParams.next();
				}
				params.put(parameter, array);
			}
		}

		@Override
		public void visit(JdbcParameter jdbcParameter) {
			params.put(jdbcParameter, rawParams.next());
		}

		public Map<Expression, Object> getParams() {
			return params;
		}
	}

	private static Map<Expression, Object> reverse(Statement sql, List<Object> value) {
		ParamReverser p = new ParamReverser(value);
		sql.accept(p);
		return p.params;
	}

	private static ExecutionPlan getExePlan(ITableMetadata meta, Delete sql, Map<Expression, Object> value,OperateTarget db,List<Table> tables) {
		throw new UnsupportedOperationException();
	}

	private static ExecutionPlan getExePlan(ITableMetadata meta, Update sql, Map<Expression, Object> value,OperateTarget db,List<Table> tables) {
		throw new UnsupportedOperationException();
	}

	private static ExecutionPlan getExePlan(ITableMetadata meta, Insert sql, Map<Expression, Object> value,OperateTarget db,List<Table> tables) {
		throw new UnsupportedOperationException();
	}

	// 获得分库分表的执行计划
	private static ExecutionPlan getExePlan(MetadataAdapter meta, PlainSelect sql, Map<Expression, Object> value,OperateTarget db,List<Table> tables) {
		DimensionCollector collector = new DimensionCollector(meta, value);
		Map<String, Dimension> val = getPartitionCondition(sql, collector);
		PartitionResult[] results=DbUtils.partitionUtil.toTableNames(meta, val, db.getPartitionSupport());
		SelectExecutionPlan ex=new SelectExecutionPlan(results,tables,sql);
		return ex;
	}

	private static Map<String, Dimension> getPartitionCondition(PlainSelect sql,DimensionCollector context) {
		Map<String,Dimension> result;
		if (sql.getWhere() != null) {
			result=context.parse(sql.getWhere());	
		}else{
			result=Collections.emptyMap();
		}
		FromItem from = sql.getFromItem();
		if(from instanceof SubSelect){
			Map<String,Dimension> cond=getPartitionCondition(((SubSelect) from).getSelectBody(),context);
			result=mergeAnd(result, cond);
		}
		return result;
	}

	private static Map<String, Dimension> getPartitionCondition(SelectBody selectBody, DimensionCollector context) {
		if (selectBody instanceof PlainSelect) {
			return getPartitionCondition((PlainSelect) selectBody, context);
		} else {
			// Union 暂不支持
			throw new UnsupportedOperationException();
		}
	}

	@SuppressWarnings("rawtypes")
	static class DimensionCollector {
		private Map.Entry<PartitionKey, PartitionFunction>[] keys;
		private Map<Expression, Object> params;
		private final Map<String, PartitionKey> columnToPartitionKey = new HashMap<String, PartitionKey>();

		DimensionCollector(ITableMetadata meta, Map<Expression, Object> params) {
			keys = meta.getEffectPartitionKeys();
			this.params = params;
			for (Map.Entry<PartitionKey, PartitionFunction> key : keys) {
				String field = key.getKey().field();
				Field fld = meta.getField(field);
				if (fld == null) {
					throw new IllegalArgumentException("The partition field [" + field + "] is not a database column.");
				}
				String columnName = meta.getColumnName(fld, Mappers.UPPER_COLUMNS, false);
				columnToPartitionKey.put(columnName, key.getKey());
			}
		}

		public Map<String, Dimension> parse(Expression exp) {
			PairSO<Dimension> dim = null;
			switch (exp.getType()) {
			case and: {
				AndExpression and = (AndExpression) exp;
				Map<String, Dimension> left = parse(and.getLeftExpression());
				Map<String, Dimension> right = parse(and.getRightExpression());
				return mergeAnd(left, right);
			}
			case or: {
				OrExpression or = (OrExpression) exp;
				Map<String, Dimension> left = parse(or.getLeftExpression());
				Map<String, Dimension> right = parse(or.getRightExpression());
				return mergeOr(left, right);
			}
			case parenthesis:
				Parenthesis p = (Parenthesis) exp;
				Map<String, Dimension> in = parse(p.getExpression());
				if (p.isNot()) {
					return mergeNot(in);
				}
				return in;
				// ////////////////////多维度运算结束/////////////
			case between:
				dim = process((Between) exp);
				break;
			case eq:
				dim = process((EqualsTo) exp);
				break;
			case ge:
				dim = process((GreaterThanEquals) exp);
				break;
			case gt:
				dim = process((GreaterThan) exp);
				break;
			case in:
				dim = process((InExpression) exp);
				break;
			case lt:
				dim = process((MinorThan) exp);
				break;
			case le:
				dim = process((MinorThanEquals) exp);
				break;
			case like:
				dim = process((LikeExpression) exp);
				break;
			case ne:
				dim = process((NotEqualsTo) exp);
				break;
			// /////////////////单维度运算结束////////////////
			// 不处理的类型
			case isnull:
			case complex:
			case arithmetic:
			case param:
			case value:
			default:
				break;
			}
			// 处理Not的场景
			if (dim != null && (exp instanceof Notable)) {
				boolean not = ((Notable) exp).isNot();
				if (not) {
					dim.second = dim.second.mergeNot();
				}
				return Collections.singletonMap(dim.first, dim.second);
			}
			return Collections.emptyMap();
		}

		private PairSO<Dimension> process(InExpression exp) {
			if (exp.getLeftExpression().getType() != ExpressionType.column) {
				return null;
			}
			String field = this.getPartitionField((Column) exp.getLeftExpression());
			if (field == null)
				return null;
			List<Object> values = new ArrayList<Object>();
			if (exp.getItemsList() instanceof ExpressionList) {
				for (Expression ex : ((ExpressionList) exp.getItemsList()).getExpressions()) {
					Object v = getAsValue(ex);
					if (v == ObjectUtils.NULL)
						return null;// in条件中有任意一个无法解析的表达式，则整个维度条件无效。
					values.add(v);
				}
			}
			Dimension d = getAsPointsDimension(values);
			return new PairSO<Dimension>(field, d);
		}

		private static Dimension getAsPointsDimension(List<Object> cv) {
			return ComplexDimension.create((Comparable[]) cv.toArray(new Comparable[cv.size()]));
		}

		private PairSO<Dimension> process(EqualsTo exp) {
			PairSO<Object> v = getFromBinaryOperate(exp);
			if (v != null) {
				return v.<Dimension> replaceSecond(RangeDimension.create(v.second, v.second));
			}
			return null;
		}

		private PairSO<Dimension> process(NotEqualsTo exp) {
			PairSO<Object> v = getFromBinaryOperate(exp);
			if (v != null) {
				Dimension d = RangeDimension.create(v.second, v.second);
				return v.replaceSecond(d.mergeNot());
			}
			return null;
		}

		private PairSO<Dimension> process(LikeExpression exp) {
			PairSO<Object> v = getFromBinaryOperate(exp);
			if (v != null) {
				String like = String.valueOf(v.second);
				if (like.endsWith("%") && !like.startsWith("%")) {
					String base = StringUtils.substringBefore(like, "%");
					return v.<Dimension> replaceSecond(new RegexpDimension(base));
				}
			}
			return null;
		}

		private PairSO<Dimension> process(MinorThanEquals exp) {
			PairSO<Object> v = getFromBinaryOperate(exp);
			if (v != null) {
				return v.<Dimension> replaceSecond(RangeDimension.createCL(null, v.second));
			}
			return null;
		}

		private PairSO<Dimension> process(MinorThan exp) {
			PairSO<Object> v = getFromBinaryOperate(exp);
			if (v != null) {
				return v.<Dimension> replaceSecond(RangeDimension.createCC(null, v.second));
			}
			return null;
		}

		private PairSO<Dimension> process(GreaterThan exp) {
			PairSO<Object> v = getFromBinaryOperate(exp);
			if (v != null) {
				return v.<Dimension> replaceSecond(RangeDimension.createCC(v.second, null));
			}
			return null;
		}

		private PairSO<Dimension> process(GreaterThanEquals exp) {
			PairSO<Object> v = getFromBinaryOperate(exp);
			if (v != null) {
				return v.<Dimension> replaceSecond(RangeDimension.createLC(v.second, null));
			}
			return null;
		}

		private PairSO<Object> getFromBinaryOperate(BinaryExpression exp) {
			Column column = null;
			Expression valueExp = null;
			if (exp.getLeftExpression().getType() == ExpressionType.column) {
				column = (Column) exp.getLeftExpression();
				valueExp = exp.getRightExpression();
			}
			if (exp.getRightExpression().getType() == ExpressionType.column) {
				column = (Column) exp.getRightExpression();
				valueExp = exp.getLeftExpression();
			}
			String field = getPartitionField(column);
			if (field != null) {
				Object obj = getAsValue(valueExp);
				if (ObjectUtils.NULL != obj) {
					return new PairSO<Object>(field, obj);
				}
			}
			return null;
		}

		private PairSO<Dimension> process(Between exp) {
			if (exp.getLeftExpression().getType() == ExpressionType.column) {
				String field = getPartitionField((Column) exp.getLeftExpression());
				if (field == null)
					return null;
				Expression start = exp.getBetweenExpressionStart();
				Expression end = exp.getBetweenExpressionEnd();
				Object min = getAsValue(start);
				Object max = getAsValue(end);
				// 无效
				if (min == ObjectUtils.NULL && max == ObjectUtils.NULL) {
					return null;
				}
				if (min == ObjectUtils.NULL)
					min = null;
				if (max == ObjectUtils.NULL)
					max = null;
				return new PairSO<Dimension>(field, RangeDimension.create(min, max));
			}
			return null;
		}

		/**
		 * 返回ObjectUtils.null表示是无效条件
		 * 
		 * @param exp
		 * @return
		 */
		private Object getAsValue(Expression exp) {
			if (exp.getType() == ExpressionType.value) {
				SqlValue value = (SqlValue) exp;
				return value.getValue();
			} else if (exp.getType() == ExpressionType.param) {
				Object value = this.params.get(exp);
				return value;
			}
			return ObjectUtils.NULL;
		}

		private String getPartitionField(Column column) {
			if (column == null)
				return null;
			PartitionKey key = columnToPartitionKey.get(StringUtils.upperCase(column.getColumnName()));
			if (key == null)
				return null;
			return key.field();
		}
	}

	// 对所有维度取非
	private static Map<String, Dimension> mergeNot(Map<String, Dimension> in) {
		Map<String, Dimension> result = new HashMap<String, Dimension>();
		for (Map.Entry<String, Dimension> d : in.entrySet()) {
			result.put(d.getKey(), d.getValue().mergeNot());
		}
		return result;
	}

	// 合并两次的维度，与
	private static Map<String, Dimension> mergeOr(Map<String, Dimension> left, Map<String, Dimension> right) {
		Map<String, Dimension> m = new HashMap<String, Dimension>(left);
		for (Map.Entry<String, Dimension> e : right.entrySet()) {
			Dimension old = m.put(e.getKey(), e.getValue());
			if (old != null) {
				m.put(e.getKey(), e.getValue().mergeOr(old));
			}
		}
		return m;
	}

	// 合并两次的维度，或
	private static Map<String, Dimension> mergeAnd(Map<String, Dimension> left, Map<String, Dimension> right) {
		Map<String, Dimension> m = new HashMap<String, Dimension>(left);
		for (Map.Entry<String, Dimension> e : right.entrySet()) {
			Dimension old = m.put(e.getKey(), e.getValue());
			if (old != null) {
				m.put(e.getKey(), e.getValue().mergeAnd(old));
			}
		}
		return m;
	}

	/**
	 * 只有当确定select语句中使用了groupBy后才走入当前分支，解析出当前的内存分组任务
	 * @param selects
	 * @return
	 */
	public InMemoryGroupByHaving parseSelectFunction(List<SelectItem> selects,List<Expression> groupExps) {
		List<GroupByItem> keys=new ArrayList<GroupByItem>();
		List<GroupByItem> values=new ArrayList<GroupByItem>();
		//解析出SQL修改句柄，当延迟操作group时，必然要将原先的分组函数去除，配合将groupBy去除
		
		Set<String> groups=new HashSet<String>();
		for(Expression exp: groupExps){
			groups.add(exp.toString().toUpperCase());
		}
		for(int i=0;i<selects.size();i++){
			SelectItem e=selects.get(i);
			Expression ex=e.getExpression();
			String alias=e.getAlias();
			if(ex==null)
				continue;//基本上是不可能的，在group的语句中
			
			//TODO 先用简单粗暴的方式做出来再说，性能什么的再说了……
			String sql=ex.toString().toUpperCase();
			if(groups.contains(sql)){
				keys.add(new GroupByItem(i,GroupFunctionType.GROUP,alias));
			}else{
				GroupFunctionType type;
				String exp=sql.toUpperCase();
				if(exp.startsWith("AVG(")){
					type=GroupFunctionType.AVG;
				}else if(exp.startsWith("COUNT(")){
					type=GroupFunctionType.COUNT;
				}else if(exp.startsWith("SUM(")){
					type=GroupFunctionType.SUM;
				}else if(exp.startsWith("MIN(")){
					type=GroupFunctionType.MIN;
				}else if(exp.startsWith("MAX(")){
					type=GroupFunctionType.MAX;
				}else if(exp.startsWith("ARRAY_TO_LIST(")){	
					type=GroupFunctionType.ARRAY_TO_LIST;
				}else{
					type=GroupFunctionType.NORMAL;
				}	
				values.add(new GroupByItem(i,type,alias));
			}
		}
		return new InMemoryGroupByHaving(keys,values);
	}
}
