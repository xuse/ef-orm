package jef.database;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.jsqlparser.parser.JpqlParser;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.AllColumns;
import jef.database.jsqlparser.statement.select.AllTableColumns;
import jef.database.jsqlparser.statement.select.OrderBy;
import jef.database.jsqlparser.statement.select.OrderByElement;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.statement.select.SubJoin;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.SelectItemVisitor;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.tools.Assert;

/**
 * 将JPQL的SELECT语句解析为标准SQL
 * 目前其实并不是真正的JPQL支持，只能将查询中出现的字段名和类名更改为对应的表名和列名而已。
 * 因此实际上并不常用。
 * @author Administrator
 * 这个实现好像问题不少,尽量避免再用
 */
public class JPQLSelectConvert extends VisitorAdapter {
	private Map<String, Class<?>> aliasMap = new HashMap<String, Class<?>>();
	private SqlProcessor rProcessor;

	public JPQLSelectConvert(SqlProcessor rPrcessor) {
		this.rProcessor = rPrcessor;
	}
	static Map<String ,Class<?>> cache=new HashMap<String,Class<?>>();

	private SelectItemVisitor fromCollector = new SelectItemVisitor() {
		private Class<?> parseSimpleName(String simpleEntityName) {
			String key=simpleEntityName.toLowerCase();
			Class<?> clz=cache.get(key);
			if(clz==null){
				for (ITableMetadata c : MetaHolder.getCachedModels()) {
					if (c.getSimpleName().equalsIgnoreCase(simpleEntityName)) {
						if(clz!=null){
							throw new IllegalArgumentException("Duplicate class with same name:" + simpleEntityName+". between"+clz.getName()+" and "+c.getName());
						}
						clz=c.getThisType();
					}
				}
				LogUtil.warn("the " + simpleEntityName + " does't match any known entity class.");
			}
			if(clz!=null){
				cache.put(key, clz);
			}
			return clz.asSubclass(IQueryableEntity.class);
		}
		
		public void visit(Table tableName) {
			String t = tableName.getName();
			Class<?> c;
			if (t.indexOf('.') > 0) {
				try {
					c = Class.forName(t);
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException(e.getMessage());
				}
			} else {
				c = parseSimpleName(t);
			}
			Assert.notNull(c, "Entity not found " + t);
			tableName.setName(MetaHolder.getMeta(c.asSubclass(IQueryableEntity.class)).getTableName(true));
			aliasMap.put(tableName.getAlias(), c);
		}

		public void visit(SubSelect subSelect) {
			subSelect.getSelectBody().accept(new JPQLSelectConvert(rProcessor));
		}

		public void visit(SubJoin subjoin) {
			System.out.println(subjoin);
		}

		public void visit(JpqlParameter tableClip) {
			String tablename=tableClip.toString();
			if("?".equals(tablename)){
				throw new RuntimeException("Not a valid table");
			}
			JpqlParser p=new JpqlParser(new StringReader(tablename));
			try {
				FromItem item=p.FromItem();
				item.accept(this);
			} catch (ParseException e) {
				LogUtil.exception(e);
			}
		}

		public void visit(AllColumns allColumns) {
		}

		public void visit(AllTableColumns allTableColumns) {
		}

		public void visit(SelectExpressionItem selectExpressionItem) {
		}

		public void visit(OrderByElement orderBy) {
		}

		public void visit(OrderBy orderBy) {
		}

		public void visit(ExpressionList expressionList) {
		}
	};

	public void visit(PlainSelect plainSelect) {
		plainSelect.getFromItem().accept(fromCollector);
		// 再解析Join
		if (plainSelect.getJoins() != null) {
			for (jef.database.jsqlparser.statement.select.Join jj : plainSelect.getJoins()) {
				jj.getRightItem().accept(fromCollector);
				if (jj.getOnExpression() != null)
					jj.getOnExpression().accept(this);
				if (jj.getUsingColumns() != null) {
					for (Column c : jj.getUsingColumns()) {
						c.accept(this);
					}
				}
			}
		}
		// 先解析From
		for (SelectItem s : plainSelect.getSelectItems()) {
			s.accept(this);
		}
		if (plainSelect.getWhere() != null)
			plainSelect.getWhere().accept(this);
	}

	@Override
	public void visit(Column tableColumn) {
		String tbAlias = tableColumn.getTableAlias();
		String col = tableColumn.getColumnName();
		Class<?> cc = null;
		if (tbAlias == null) {
			for (Class<?> c : aliasMap.values()) {
				ITableMetadata meta = MetaHolder.getMeta(c.asSubclass(IQueryableEntity.class));
				if (meta == null)
					continue;
				Field fld = meta.findField(col);
				if (fld == null)
					continue;
				tableColumn.setColumnName(meta.getColumnName(fld, null, rProcessor.getProfile()));
			}
		} else {
			cc = aliasMap.get(tbAlias);
			if (cc == null)
				return;
			ITableMetadata meta = MetaHolder.getMeta(cc.asSubclass(IQueryableEntity.class));
			Field fld = meta.findField(col);
			if (fld == null)
				return;
			tableColumn.setColumnName(meta.getColumnName(fld, null, rProcessor.getProfile()));
		}
		super.visit(tableColumn);
	}
}
