package jef.database.routing.sql;

import java.sql.SQLException;
import java.util.List;

import jef.database.OperateTarget;
import jef.database.annotation.PartitionResult;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.delete.Delete;

public class DeleteExecutionPlan extends AbstractExecutionPlan{
	private StatementContext<Delete> context;

	public DeleteExecutionPlan(PartitionResult[] results, StatementContext<Delete> context) {
		super(results);
		this.context=context;
	}

	public int processUpdate(PartitionResult site, OperateTarget session) throws SQLException {
		OperateTarget db=session.getTarget(site.getDatabase());
		int count=0;
		for(String table: site.getTables()){
			String sql=getSql(table);
			List<Object> params=context.params;
			count+=db.innerExecuteSql(sql, params);
		}
		return count;
	}

	private String getSql(String table) {
		for(Table t: context.modifications){
			t.setReplace(table);
		}
		String s=context.statement.toString();
		for(Table t: context.modifications){
			t.removeReplace();
		}
		return s;
	}

}
