package jef.database.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jef.database.DbUtils;
import jef.database.Field;
import jef.database.QueryAlias;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.meta.FBIField;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.tools.Assert;

/**
 * SelectItems的实现类，供选取列,用于在Criteria API中定制查询
 * 
 * @author Jiyi
 * 
 */
public class SelectsImpl extends AbstractEntityMappingProvider implements Selects {
	private static final long serialVersionUID = -7074691304983975019L;
	Set<QueryAlias> touchQueries=new HashSet<QueryAlias>();

	public SelectsImpl() {
	}

//	private void add(QueryAlias config,IReferenceColumn field){
//		if(!touchQueries.contains(config)){
//			if(config.getReferenceObj()!=null){
//				noColums(config.getTableDef());
//			}
//		}
//		config.addField(field);
//		touchQueries.add(config);
//	}
//	
//	private void add(QueryAlias config,IReferenceAllTable field){
//		config.addField(field);
//		touchQueries.add(config);
//	}
	
	public SelectsImpl(List<QueryAlias> context) {
		this.queries = context;
	}

	public AllTableColumns allTableColumns(Query<?> query) {
		return allColumns(query);
	}

	public AllTableColumns allColumns(Query<?> q) {
		return allTableColumns(null, q);
	}

	public SelectColumn column(Field field) {
		ITableMetadata meta = DbUtils.getTableMeta(field);
		QueryAlias qa = findQuery(meta, null);
		Query<?> found;
		if (qa == null) {
			throw new IllegalArgumentException("the " + meta.getThisType() + " not found in the query tables.");
		}else{
			found=qa.getTableDef();
		}
		return column(found, field, null, field.name());
	}

	public void columns(Field... fields) {
		for (Field f : fields) {
			column(f);
		}
	}

	public SelectColumn column(Query<?> query, Field name) {
		return column(query, name, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.SelectItems#column(jef.database.query.Query,
	 * java.lang.String)
	 */
	public SelectColumn column(Query<?> query, String name) {
		QueryAlias config = findQuery(query); // 目前其实只有QueryAlias一个实现
		Assert.notNull(config, "the query is not contain in the join tables.");
		ITableMetadata meta = MetaHolder.getMeta(query.getInstance());
		Field field = meta.getField(name);
		if (field == null)
			field = new FBIField(name, query);
		return column(query, field, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.SelectItems#guessColumn(java.lang.String,
	 * java.lang.String)
	 */
	public SelectColumn guessColumn(String name) {
		Field field = null;
		Query<?> matched = null;
		if (name.indexOf('(') > -1 || name.indexOf('|') > -1) {// 无法支持FBIField?
			if (queries.size() > 0) {
				matched = queries.get(0).getTableDef();
				field = new FBIField(name, matched);
				return column(matched, field, null, null);
			}
		}
		for (ISelectItemProvider qa : this.queries) {
			ITableMetadata meta = MetaHolder.getMeta(qa.getTableDef().getInstance());
			Field find = meta.getField(name);
			if (find != null && field != null) {
				System.err.println("There are duplicate field named '" + name + "' in multi table columns");
			} else if (find != null) {
				field = find;
				matched = qa.getTableDef();
			}
		}
		if (field == null)
			return null;
		return column(matched, field, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.SelectItems#clearSelectItems()
	 */
	public void clearSelectItems() {
		for (ISelectItemProvider i : this.queries) {
			i.setFields(null);
		}
	}

	public void columns(String string) {
		if (queries.size() == 1) {
			QueryAlias q = (QueryAlias) queries.get(0);
			columns(q.getQuery(), string);
		} else {
			throw new IllegalArgumentException("This method allowed only for one table query.");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.SelectItems#columns(jef.database.query.Query,
	 * java.lang.String)
	 */
	public void columns(Query<?> q, String columns) {
		try {
			List<SelectItem> items = DbUtils.parseSelectItems(columns);
			ITableMetadata meta =q.getMeta();
			for (SelectItem item : items) {
				if (item instanceof SelectExpressionItem) {
					SelectExpressionItem expression = (SelectExpressionItem) item;
					Expression column=expression.getExpression();
					String exp = column.toString();
					Field field = (column instanceof Column)?meta.findField(exp):null;
					if (field == null)
						field = new FBIField(exp, q);
					this.column(q, field, expression.getAlias(), null);
				} else if (item instanceof jef.database.jsqlparser.statement.select.AllTableColumns) {
					jef.database.jsqlparser.statement.select.AllTableColumns at = (jef.database.jsqlparser.statement.select.AllTableColumns) item;
					this.allTableColumns(at.getTable().getAlias(), q);
				}
			}
		} catch (ParseException e) {
			throw new IllegalArgumentException(columns);
		}
	}

	public void merge(AbstractEntityMappingProvider selectItems) {
		this.distinct=selectItems.distinct;
		for (ISelectItemProvider os : this.getReference()) { // 新的
			boolean found=false;
			for (ISelectItemProvider is : selectItems.getReference()) { // 原先的
				if (is.getTableDef() == os.getTableDef()) {
					os.setFields(is.getReferenceObj(), is.getReferenceCol());
					found=true;
					break;
				}
			}
			if(!found){
				AllTableColumns ac = new AllTableColumns(os.getTableDef());
				ac.notSelectAnyColumn();
				ac.setName(null);
				os.setFields(ac);
			}
		}
	}

	/**
	 * 创建查询表达式，支持方言改写。
	 * {@inheritDoc}
	 * <p>
	 * 如果在表达式中需要引用第一个表的别名，使用$1,$2...以此类推
	 */
	public SelectExpression sqlExpression(String sql) {
		List<SelectItem> items;
		try {
			items = DbUtils.parseSelectItems(sql);
		} catch (ParseException e) {
			throw new IllegalArgumentException("unsupported expression: " + sql);
		}
		SelectExpression2 ac = null;
		for (SelectItem item : items) {
			if (item instanceof SelectExpressionItem) {
				SelectExpressionItem i = (SelectExpressionItem) item;
				ac = new SelectExpression2(i.getExpression());
				ac.as(i.getAlias());
				ISelectItemProvider config = this.queries.get(0);
				config.addField(ac);
			} else {
				throw new IllegalArgumentException("unsupported expression: " + sql);
			}
		}
		return ac;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * 如果在表达式中需要引用第一个表的别名，使用$1,$2...以此类推
	 */
	public SelectExpression rawExpression(String sql) {
		SelectExpression ac = new SelectExpression(sql);
		ISelectItemProvider config = this.queries.get(0);
		config.addField(ac);
		return ac;
	}

	/**
	 * 指定一个选择列和别名
	 * 
	 * @param fld
	 *            列
	 * @param alias
	 *            列的别名(在select中使用)
	 * @param query
	 *            对应的查询实例
	 * @param populateTo
	 *            结果最终拼装时的字段名
	 */
	private SelectColumn column(Query<?> query, Field fld, String alias, String populateTo) {
		if (fld instanceof LazyQueryBindField) {
			LazyQueryBindField qb = (LazyQueryBindField) fld;
			if (!qb.isBind()) {
				qb.setBind(query);
			}
		}
		QueryAlias config = findQuery(query); // 目前其实只有QueryAlias一个实现
		Assert.notNull(config, "the query is not contain in the join tables.");
		SelectColumn ac = new SelectColumn(fld, populateTo);
		ac.as(alias);
		config.addField(ac);
		return ac;
	}

	private AllTableColumns allTableColumns(String toField, Query<?> q) {
		QueryAlias config = findQuery(q); // 目前其实只有QueryAlias一个实现
		Assert.notNull(config, "the query is not contain in the join tables.");
		AllTableColumns ac = new AllTableColumns(q);
		ac.setName(toField);
		config.addField(ac);
		return ac;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.SelectItems#noColums(jef.database.query.Query)
	 */
	public void noColums(Query<?> query) {
		allColumns(query).notSelectAnyColumn();
	}
}
