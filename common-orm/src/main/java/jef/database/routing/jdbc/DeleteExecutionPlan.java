package jef.database.routing.jdbc;

import jef.database.OperateTarget;
import jef.database.annotation.PartitionResult;
import jef.database.jsqlparser.statement.delete.Delete;

public class DeleteExecutionPlan extends AbstractExecutionPlan{
	private StatementContext<Delete> context;

	public DeleteExecutionPlan(PartitionResult[] results, StatementContext<Delete> context) {
		super(results);
		this.context=context;
	}

	public int processUpdate(PartitionResult site, OperateTarget session) {
		// TODO Auto-generated method stub
		return 0;
	}

}
