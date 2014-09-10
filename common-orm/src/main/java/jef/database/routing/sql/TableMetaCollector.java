package jef.database.routing.sql;

import java.util.ArrayList;
import java.util.List;

import jef.common.wrapper.Holder;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.create.CreateTable;
import jef.database.jsqlparser.statement.delete.Delete;
import jef.database.jsqlparser.statement.drop.Drop;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.replace.Replace;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.SubJoin;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.statement.truncate.Truncate;
import jef.database.jsqlparser.statement.update.Update;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.StatementVisitor;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.MetadataAdapter;

final class TableMetaCollector extends Holder<MetadataAdapter> implements StatementVisitor {
	private boolean breakProcess;
	private final List<Table> modificationPoints=new ArrayList<Table>();

	public void visit(Select select) {
		SelectBody body = select.getSelectBody();
		process(body);
	}

	private void process(SelectBody body) {
		if (body instanceof PlainSelect) {
			FromItem item = ((PlainSelect) body).getFromItem();
			processFromItem(item);
		} else if (body instanceof Union) {
			Union union = (Union) body;
			for (PlainSelect sel : union.getPlainSelects()) {
				processFromItem(sel.getFromItem());
				if (breakProcess)
					break;
			}
		}
	}

	private void processFromItem(FromItem item) {
		if (item instanceof SubJoin) {
			breakProcess = true;
		} else if (item instanceof JpqlParameter) {
			breakProcess = true;
		} else if (item instanceof SubSelect) {
			process(((SubSelect) item).getSelectBody());
		} else if (item instanceof Table) {
			process((Table) item);
		}
	}

	private void process(Table table) {
		String schema = table.getSchemaName();
		String name = table.getName();
		ITableMetadata meta = MetaHolder.lookup(schema, name);
		if (this.get() == null) {
			modificationPoints.add(table);
			this.set((MetadataAdapter)meta);
		} else if (this.get() != meta) {//出现多张表,匹配失败
			this.set(null);
			breakProcess = true;
		}else{
			modificationPoints.add(table);          //单表，修改点+1
		}
	}

	public void visit(Delete delete) {
		processFromItem(delete.getTable());
	}

	public void visit(Update update) {
		processFromItem(update.getTable());
	}

	public void visit(Insert insert) {
		processFromItem(insert.getTable());
	}

	public void visit(Replace replace) {
		// 不支持
	}

	public void visit(Drop drop) {
		// 不支持
	}

	public void visit(Truncate truncate) {
		// 不支持
	}

	public void visit(CreateTable createTable) {
		// 不支持
	}

	public List<Table> getModificationPoints() {
		return modificationPoints;
	}
	
}