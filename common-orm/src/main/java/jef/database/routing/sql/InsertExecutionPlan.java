package jef.database.routing.sql;

import java.sql.SQLException;

import jef.database.annotation.PartitionResult;
import jef.database.jdbc.GenerateKeyReturnOper;
import jef.database.jdbc.JDBCTarget;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.routing.jdbc.UpdateReturn;

public class InsertExecutionPlan extends AbstractExecutionPlan implements ExecuteablePlan{

	private StatementContext<Insert> context;

	public InsertExecutionPlan(PartitionResult[] results, StatementContext<Insert> context) {
		super(results);
		this.context = context;
	}

	// Insert操作是最简单的因为表名肯定只有一个
	public UpdateReturn processUpdate(GenerateKeyReturnOper generateKeys) throws SQLException {
		PartitionResult site = this.sites[0];
		JDBCTarget session=context.db.getTarget(site.getDatabase());
		String s=getSql(site.getAsOneTable());
		return session.innerExecuteUpdate(s, context.params, generateKeys);
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
