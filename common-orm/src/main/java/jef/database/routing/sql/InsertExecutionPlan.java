package jef.database.routing.sql;

import java.sql.SQLException;

import jef.database.OperateTarget;
import jef.database.annotation.PartitionResult;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.insert.Insert;

public class InsertExecutionPlan extends AbstractExecutionPlan {

	private StatementContext<Insert> context;

	public InsertExecutionPlan(PartitionResult[] results, StatementContext<Insert> context) {
		super(results);
		this.context = context;
	}

	// Insert操作是最简单的因为表名肯定只有一个
	public int processUpdate(PartitionResult site, OperateTarget session) throws SQLException {
		session = session.getTarget(site.getDatabase());
		String s=getSql(site.getAsOneTable());
		session.innerExecuteSql(s, context.params);
		return 1;
	}

	@Override
	public String getSql(String table) {
		for (Table t : context.modifications) {
			t.setReplace(table);
		}
		String s=context.statement.toString();
		for (Table t : context.modifications) {
			t.removeReplace();
		}
		return s;
	}

}
