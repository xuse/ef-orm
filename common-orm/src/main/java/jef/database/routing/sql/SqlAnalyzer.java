package jef.database.routing.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.common.Pair;
import jef.common.PairSO;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.ORMConfig;
import jef.database.OperateTarget;
import jef.database.annotation.PartitionFunction;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionResult;
import jef.database.innerpool.PartitionSupport;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.expression.JdbcParameter;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.Parenthesis;
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
import jef.database.jsqlparser.visitor.SqlValue;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetadataAdapter;
import jef.database.query.ComplexDimension;
import jef.database.query.Dimension;
import jef.database.query.RangeDimension;
import jef.database.query.RegexpDimension;
import jef.database.routing.jdbc.ParameterContext;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * 基于SQL语句的分库分表解析。主要逻辑部分
 * @author jiyi
 *
 */
public class SqlAnalyzer {
	/**
	 * 获得select语句的执行计划
	 * @param sql AST of select
	 * @param value 绑定变量值
	 * @param db  数据库Session
	 * @return
	 */
	public static SelectExecutionPlan getSelectExecutionPlan(Select sql,Map<Expression, Object>  params, List<Object> value, OperateTarget db) {
		TableMetaCollector collector = new TableMetaCollector();
		sql.accept(collector);
		if(collector.get()==null)return null;
		MetadataAdapter meta=collector.get();
		if (meta == null) {
			return null;
		}
		if(meta.getPartition() == null){
			if(meta.getBindDsName()!=null && !meta.getBindDsName().equals(db.getDbkey())){
				return new SelectExecutionPlan(meta.getBindDsName());
			}else{
				return null;
			}
		}
		Select select = (Select) sql;
		SelectBody body = select.getSelectBody();
		if (body instanceof PlainSelect) {
			StatementContext<PlainSelect> context=new StatementContext<PlainSelect>((PlainSelect) body,meta,params,value,db,collector.getModificationPoints());
			return getPlainSelectExePlan(context);
		} else {//已经是Union语句的暂不支持
			throw new UnsupportedOperationException();
		}
	}
	
	public static List<Object> asValue(List<ParameterContext> params) {
		List<Object> values=new ArrayList<Object>(params.size());
		for(ParameterContext context:params){
			values.add(context.getValue());
		}
		return values;
	}
	
	
	public static TableMetaCollector getTableMeta(Statement st){
		TableMetaCollector collector = new TableMetaCollector();
		st.accept(collector);
		return collector;
	}
	
	/**
	 * 按每组参数计算路由结果，并按路由结果对Batch中的参数进行分组
	 * @param params
	 * @param st
	 * @param db
	 * @return
	 */
	public static Multimap<String, List<ParameterContext>> doGroup(MetadataAdapter meta,List<List<ParameterContext>> params,Statement st,OperateTarget db) {
		Multimap<String,List<ParameterContext>> result=ArrayListMultimap.create();
		for(List<ParameterContext> param:params){
			List<Object> values=asValue(param);
			Map<Expression, Object> paramMap = reverse(st, values); // 参数对应关系还原
			PartitionResult routing=getPartitionResult(st,meta,paramMap,db.getPartitionSupport());
			String key=routing.getDatabase()+"-"+routing.getAsOneTable();
			result.put(key, param);
		}
		return result;
	}
	
	
	/**
	 * 获得其他操作语句（Insert，Delete，Update语句的执行计划）
	 * @param sql    AST of /Update/Delete/Insert
	 * @param value  绑定变量值
	 * @param db     数据库Session
	 * @return
	 */
	public static ExecutionPlan getExecutionPlan(Statement sql,Map<Expression,Object> params, List<Object> value, OperateTarget db) {
		TableMetaCollector collector = new TableMetaCollector();
		sql.accept(collector);
		MetadataAdapter meta=collector.get();
		if (meta == null) {
			return null;
		}
		if(meta.getPartition() == null){
			if(meta.getBindDsName()!=null && !meta.getBindDsName().equals(db.getDbkey())){
				return new SelectExecutionPlan(meta.getBindDsName());
			}else{
				return null;
			}
		}
		if (sql instanceof Insert) {
			StatementContext<Insert> context=new StatementContext<Insert>((Insert) sql,meta,params,value,db,collector.getModificationPoints());
			return getInsertExePlan(context);
		} else if (sql instanceof Update) {
			StatementContext<Update> context=new StatementContext<Update>((Update) sql,meta,params,value,db,collector.getModificationPoints());
			return getUpdateExePlan(context);
		} else if (sql instanceof Delete) {
			StatementContext<Delete> context=new StatementContext<Delete>((Delete) sql,meta,params,value,db,collector.getModificationPoints());
			return getDeleteExePlan(context);
		}
		return null;
	}

	/*
	 * 将顺序的参数重新变为和JpqlParameter对应的map
	 */
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

	/*
	 * 将已经顺序排列好的参数和解析后的AST中的参数对象一一对应。
	 */
	public static Map<Expression, Object> reverse(Statement sql, List<Object> value) {
		ParamReverser p = new ParamReverser(value);
		sql.accept(p);
		return p.params;
	}

	/*
	 * 为Delete生成执行计划
	 */
	private static ExecutionPlan getDeleteExePlan(StatementContext<Delete> context) {
		DimensionCollector collector = new DimensionCollector(context.meta, context.paramsMap);
		Map<String, Dimension> val = getPartitionCondition(context.statement, collector);
		val=fill(val,collector);
		PartitionResult[] results=DbUtils.partitionUtil.toTableNames(context.meta, val, context.db.getPartitionSupport(),ORMConfig.getInstance().isFilterAbsentTables());
		DeleteExecutionPlan ex=new DeleteExecutionPlan(results,context);
		return ex;
	}
	
	public static PartitionResult[] getPartitionResultOfSQL(Statement sql,List<Object> values,PartitionSupport support){
		TableMetaCollector collector = new TableMetaCollector();
		sql.accept(collector);
		if(collector.get()==null){
			throw new IllegalArgumentException("The SQL is a known partition table.");
		}
		MetadataAdapter meta=collector.get();
		if (meta == null || meta.getPartition() == null) {
			return null;
		}
		Map<Expression, Object> params = reverse(sql, values); // 参数对应关系还原
		Map<String, Dimension> val=null;
		if (sql instanceof Select) {
			Select select = (Select) sql;
			DimensionCollector dims = new DimensionCollector(meta, params);
			val = getPartitionCondition(select.getSelectBody(), dims);
		} else if(sql instanceof Insert){//已经是Union语句的暂不支持
			DimensionCollector dims = new DimensionCollector(meta, params);
			val = getPartitionCondition((Insert)sql, dims);
		} else if(sql instanceof Delete){//已经是Union语句的暂不支持
			DimensionCollector dims = new DimensionCollector(meta, params);
			val = getPartitionCondition((Delete)sql, dims);
		} else if(sql instanceof Update){//已经是Union语句的暂不支持
			DimensionCollector dims = new DimensionCollector(meta, params);
			val = getPartitionCondition((Update)sql, dims);
		}else{
			throw new UnsupportedOperationException(sql.getClass().toString());
		}
		return DbUtils.partitionUtil.toTableNames(meta, val, support,ORMConfig.getInstance().isFilterAbsentTables());
	}
	


	/*
	 * 为Update生成执行计划
	 */
	private static ExecutionPlan getUpdateExePlan(StatementContext<Update> context) {
		DimensionCollector collector = new DimensionCollector(context.meta, context.paramsMap);
		Map<String, Dimension> val = getPartitionCondition(context.statement, collector);
		val=fill(val,collector);
		PartitionResult[] results=DbUtils.partitionUtil.toTableNames(context.meta, val, context.db.getPartitionSupport(),ORMConfig.getInstance().isFilterAbsentTables());
		UpdateExecutionPlan ex=new UpdateExecutionPlan(results,context);
		return ex;
	}

	/*
	 * 为Select生成执行计划
	 */
	private static SelectExecutionPlan getPlainSelectExePlan(StatementContext<PlainSelect> context) {
		DimensionCollector collector = new DimensionCollector(context.meta, context.paramsMap);
		Map<String, Dimension> val = getPartitionCondition(context.statement, collector);
		val=fill(val,collector);
		PartitionResult[] results=DbUtils.partitionUtil.toTableNames(context.meta, val, context.db.getPartitionSupport(),ORMConfig.getInstance().isFilterAbsentTables());
		SelectExecutionPlan ex=new SelectExecutionPlan(results,context);
		return ex;
	}
	
	
	private static Map<String, Dimension> fill(Map<String, Dimension> result,DimensionCollector collector) {
		Set<String> keys=collector.meta.getMinUnitFuncForEachPartitionKey().keySet();
		if(result.size()<keys.size()){
			result=new HashMap<String,Dimension>(result);
			for(String s: keys){
				if(!result.containsKey(s)){
					result.put(s, RangeDimension.EMPTY_RANGE);
				}
			}
		}
		return result;
	}
	
	public static PartitionResult getPartitionResult(Statement st,MetadataAdapter meta,Map<Expression, Object> paramsMap,PartitionSupport support){
		DimensionCollector collector = new DimensionCollector(meta, paramsMap);
		Map<String, Dimension> val ;
		if(st instanceof Insert){
			val= getPartitionCondition((Insert)st, collector);
		}else if(st instanceof Update){
			val= getPartitionCondition((Update)st, collector);
		}else if(st instanceof Delete){
			val= getPartitionCondition((Delete)st, collector);
		}else{
			throw new UnsupportedOperationException(st.getClass().getSimpleName());
		}
		val=fill(val,collector);
		return DbUtils.partitionUtil.toTableName(meta, val, support);
	}
	
	/*
	 * 为Insert生成执行计划
	 */
	private static ExecutionPlan getInsertExePlan(StatementContext<Insert> context) {
		DimensionCollector collector = new DimensionCollector(context.meta, context.paramsMap);
		Map<String, Dimension> val = getPartitionCondition(context.statement, collector);
		val=fill(val,collector);
		PartitionResult results=DbUtils.partitionUtil.toTableName(context.meta, val, context.db.getPartitionSupport());
		InsertExecutionPlan ex=new InsertExecutionPlan(new PartitionResult[]{results},context);
		return ex;
	}

	/*
	 * 收集路由维度，从Insert语句
	 */
	private static Map<String, Dimension> getPartitionCondition(Insert statement, DimensionCollector collector) {
		List<Column> cols=statement.getColumns();
		if(cols==null){
			throw new UnsupportedOperationException("the SQL must assign column names.");
		}
		if(statement.getItemsList() instanceof SubSelect){
			throw new UnsupportedOperationException("Can not support a subselect");
		}
		ExpressionList exp=(ExpressionList)statement.getItemsList();
		Map<String,Dimension> result=new HashMap<String,Dimension>();
		for(int i=0;i<exp.size();i++){
			Column c=cols.get(i);
			String field=collector.getPartitionField(c);
			if(field==null)continue;
			Object obj=collector.getAsValue(exp.get(i));
			if(obj==ObjectUtils.NULL){
				continue;
			}
			result.put(field, (Dimension)RangeDimension.create(obj, obj));
		}
		return result;
	}


	/*
	 * 收集路由维度（从Delete语句）
	 */
	private static Map<String, Dimension> getPartitionCondition(Delete statement, DimensionCollector collector) {
		if (statement.getWhere() != null) {
			return collector.parse(statement.getWhere());	
		}else{
			return Collections.emptyMap();
		}
	}
	
	/*
	 * 收集路由维度 (从Update语句)
	 * @param statement
	 * @param collector
	 * @return
	 */
	private static Map<String, Dimension> getPartitionCondition(Update sql, DimensionCollector collector) {
		Map<String,Dimension> result;
		if (sql.getWhere() != null) {
			result=collector.parse(sql.getWhere());	
		}else{
			result=Collections.emptyMap();
		}
		for(Pair<Column,Expression> set:sql.getSets()){
			String field=collector.getPartitionField(set.first);
			if(field==null)continue;
			if(result.get(field)!=null){
				continue;
			}
			Object value=collector.getAsValue(set.second);
			if(ObjectUtils.NULL!=value){
				result.put(field, RangeDimension.create(value, value));
			}
		}
		return result;
	}
	
	
	/*
	 * 递归实现——收集路由维度 
	 */
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

	/*
	 * 递归实现——收集路由维度 
	 */
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
		private Map<Expression, Object> params;
		private final Map<String, String> columnToPartitionKey = new HashMap<String, String>();
		private ITableMetadata meta;

		DimensionCollector(ITableMetadata meta, Map<Expression, Object> params) {
			this.params = params;
			this.meta=meta;
			for (Map.Entry<PartitionKey, PartitionFunction> key : meta.getEffectPartitionKeys()) {
				String field = key.getKey().field();
				Field fld = meta.getField(field);
				if (fld == null) {
					throw new IllegalArgumentException("The partition field [" + field + "] is not a database column.");
				}
				String columnName = meta.getColumnDef(fld).upperColumnName();
				columnToPartitionKey.put(columnName, key.getKey().field());
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
					if(v instanceof Object[]){
						values.addAll(Arrays.asList((Object[])v));
					}else{
						values.add(v);
					}
				}
			}
			Dimension d = ComplexDimension.create((Comparable[]) values.toArray(new Comparable[values.size()]));
			return new PairSO<Dimension>(field, d);
		}


		private PairSO<Dimension> process(EqualsTo exp) {
			PairSO<Object> v = getFromBinaryOperate(exp);
			if (v != null) {
				return v.<Dimension>replaceSecond(RangeDimension.create(v.second, v.second));
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


		private String getPartitionField(Column column) {
			if (column == null)
				return null;
			String key = columnToPartitionKey.get(StringUtils.upperCase(column.getColumnName()));
			if (key == null)
				return null;
			return key;
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
				Object value = params.get(exp);
				return value;
			}
			return ObjectUtils.NULL;
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
	
	public static <T> List<T> repeat(List<T> source,int count){
		if(count==1){
			return source;
		}
		List<T> result=new ArrayList<T>(source.size()*count);
		for(int i=0;i<count;i++){
			result.addAll(source);
		}
		return result;
	}
}
