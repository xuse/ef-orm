package jef.database.jsqlparser.test.tablesfinder;

import java.util.ArrayList;
import java.util.List;

import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.visitor.VisitorAdapter;

public class TablesNamesFinder extends VisitorAdapter {
	private List<String> tables;

	public List<String> getTableList(Select select) {
		tables = new ArrayList<String>();
		select.getSelectBody().accept(this);
		return tables;
	}
	public void visit(Table tableName) {
		String tableWholeName = tableName.getWholeTableName();
		tables.add(tableWholeName);
	}
}
