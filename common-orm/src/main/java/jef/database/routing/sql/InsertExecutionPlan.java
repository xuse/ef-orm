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
		String table = site.getAsOneTable();
		session = session.getTarget(site.getDatabase());
		for (Table t : context.modifications) {
			t.setReplace(table);
		}
		session.innerExecuteSql(context.statement.toString(), context.params);
		for (Table t : context.modifications) {
			t.removeReplace();
		}
		return 1;
	}

}
