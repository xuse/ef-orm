package jef.database.jsqlparser;

import java.util.HashMap;
import java.util.Map;

import jef.common.Pair;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.update.Update;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.meta.ITableMetadata;

import org.springframework.util.Assert;

/**
 * 将JPQL的SELECT语句解析为标准SQL 目前其实并不是真正的JPQL支持，只能将查询中出现的字段名和类名更改为对应的表名和列名而已。
 * 因此实际上并不常用。
 * 
 * @author Administrator 这个实现好像问题不少,尽量避免再用
 */
public final class JPQLConvert extends VisitorAdapter {
	private DatabaseDialect dialect;

	private ITableMetadata meta;

	private FromCollector from = new FromCollector(dialect);
	
	public JPQLConvert(DatabaseDialect profile) {
		this.dialect = profile;
	}

	static Map<String, Class<?>> cache = new HashMap<String, Class<?>>();

	public void visit(Insert insert) {
		insert.getTable().accept(from);
		meta = from.aliasMap.get(insert.getTable().getAlias());
		Assert.notNull(meta);
		for (Column column : insert.getColumns()) {
			ColumnMapping fld = meta.findField(column.getColumnName());
			if (meta == null)
				continue;
			column.setColumnName(fld.getColumnName(dialect, true));
		}
	}

	public void visit(Update update) {
		update.getTable().accept(from);
		meta = from.aliasMap.get(update.getTable().getAlias());
		Assert.notNull(meta);
		for (Pair<Column, Expression> pair : update.getSets()) {
			Column col = pair.getFirst();
			ColumnMapping fld = meta.findField(col.getColumnName());
			if (meta == null)
				continue;
			col.setColumnName(fld.getColumnName(dialect, true));
		}
		update.getWhere().accept(this);
	}

	@Override
	public void visit(Column tableColumn) {
		String tbAlias = tableColumn.getTableAlias();
		String col = tableColumn.getColumnName();
		ITableMetadata cc = null;
		if (tbAlias == null) {
			for (ITableMetadata meta : from.aliasMap.values()) {
				if (meta == null)
					continue;
				ColumnMapping fld = meta.findField(col);
				if (fld == null)
					continue;
				tableColumn.setColumnName(fld.getColumnName(dialect, true));
			}
		} else {
			cc = from.aliasMap.get(tbAlias);
			if (cc == null)
				return;
			ColumnMapping fld = cc.findField(col);
			if (fld == null)
				return;
			tableColumn.setColumnName(fld.getColumnName(dialect, true));
		}
		super.visit(tableColumn);
	}
}
