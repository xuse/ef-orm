package jef.database.jsqlparser;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.meta.ITableMetadata;

/**
 * 将JPQL的SELECT语句解析为标准SQL 目前其实并不是真正的JPQL支持，只能将查询中出现的字段名和类名更改为对应的表名和列名而已。
 * 因此实际上并不常用。
 * 
 * @author Administrator 这个实现好像问题不少,尽量避免再用
 */
public final class JPQLSelectConvert extends VisitorAdapter {

	private DatabaseDialect profile;

	public JPQLSelectConvert(DatabaseDialect profile) {
		this.profile = profile;
	}
	
	private final FromCollector fromCollector = new FromCollector(profile)	;

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
		ITableMetadata cc = null;
		if (tbAlias == null) {
			for (ITableMetadata meta : fromCollector.aliasMap.values()) {
				if (meta == null)
					continue;
				ColumnMapping fld = meta.findField(col);
				if (fld == null)
					continue;
				tableColumn.setColumnName(fld.getColumnName(profile, true));
			}
		} else {
			cc = fromCollector.aliasMap.get(tbAlias);
			if (cc == null)
				return;
			ColumnMapping fld = cc.findField(col);
			if (fld == null)
				return;
			tableColumn.setColumnName(fld.getColumnName(profile, true));
		}
		super.visit(tableColumn);
	}
}
