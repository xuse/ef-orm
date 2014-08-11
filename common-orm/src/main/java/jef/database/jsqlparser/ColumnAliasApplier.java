package jef.database.jsqlparser;

import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.query.SqlContext;
import jef.tools.StringUtils;

public class ColumnAliasApplier extends VisitorAdapter{
	@SuppressWarnings("unused")
	private DatabaseDialect profile;
	private SqlContext context;
	
	public ColumnAliasApplier(String alias, DatabaseDialect profile,SqlContext context) {
		this.context = context;
		this.profile = profile;
	}

	public void visit(Column tableColumn) {
		String name=tableColumn.getTableAlias();
		if (name != null && name.charAt(0)=='$') {
			int i=StringUtils.toInt(name.substring(1), 0);
			if(i>0 && i<=context.size()){
				String alias=context.getAliasOf(i-1);
				tableColumn.setTableAlias(alias);
			}
		}
	}
}
