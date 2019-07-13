package jef.database.jsqlparser;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.parser.JpqlParser;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.AllColumns;
import jef.database.jsqlparser.statement.select.AllTableColumns;
import jef.database.jsqlparser.statement.select.Join;
import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.OrderBy;
import jef.database.jsqlparser.statement.select.OrderByElement;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.statement.select.SubJoin;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.SelectItemVisitor;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.tools.Exceptions;

public final class FromCollector implements SelectItemVisitor {
	// 转换缓存
	static Map<String, ITableMetadata> cache = new HashMap<String, ITableMetadata>();
	public final Map<String, ITableMetadata> aliasMap = new HashMap<String, ITableMetadata>();
	private DatabaseDialect dialect;

	public FromCollector(DatabaseDialect dialect) {
		this.dialect = dialect;
	}

	private ITableMetadata parseSimpleName(String simpleEntityName) {
		String key = simpleEntityName.toLowerCase();
		ITableMetadata clz = cache.get(key);
		if (clz == null) {
			clz = findClass(simpleEntityName);
		}
		if (clz == null)
			return null;
		cache.put(key, clz);
		return clz;
	}

	private ITableMetadata findClass(String simpleEntityName) {
		for (ITableMetadata c : MetaHolder.getCachedModels()) {
			if (c.getSimpleName().equalsIgnoreCase(simpleEntityName)) {
				// if(clz!=null){
				// throw new
				// IllegalArgumentException("Duplicate class with same name:"
				// +
				// simpleEntityName+". between"+clz.getName()+" and "+c.getName());
				// }
				return c;
			}
		}
		LogUtil.warn("the " + simpleEntityName + " does't match any known entity class.");
		return null;
	}

	public void visit(Table tableName) {
		String t = tableName.getName();
		ITableMetadata c;
		if (t.indexOf('.') > 0) {
			try {
				Class<?> clz = Class.forName(t);
				c = MetaHolder.getMeta(clz);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		} else {
			c = parseSimpleName(t);
		}
		if (c == null) {
			throw new IllegalArgumentException("Entity not found in SQL: '" + t + "'");
		}
		tableName.setName(c.getTableName(true));
		aliasMap.put(tableName.getAlias(), c);
	}

	public void visit(SubSelect subSelect) {
		subSelect.getSelectBody().accept(new JPQLSelectConvert(dialect));
	}

	public void visit(SubJoin subjoin) {
		subjoin.getLeft().accept(this);
		subjoin.getJoin().accept(this);
	}

	public void visit(JpqlParameter tableClip) {
		String tablename = tableClip.toString();
		if ("?".equals(tablename)) {
			throw new RuntimeException("Not a valid table");
		}
		JpqlParser p = new JpqlParser(new StringReader(tablename));
		try {
			FromItem item = p.FromItem();
			item.accept(this);
		} catch (ParseException e) {
			Exceptions.log(e);
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

	public void visit(Join join) {
		join.getRightItem().accept(this);
	}

	public void visit(Limit limit) {
	}

}