package jef.database.routing.jdbc;

import jef.database.OperateTarget;
import jef.database.annotation.PartitionResult;
import jef.database.jsqlparser.statement.update.Update;

public class UpdateExecutionPlan extends AbstractExecutionPlan{
	private StatementContext<Update>  context;
	
	public UpdateExecutionPlan(PartitionResult[] results, StatementContext<Update> context) {
		super(results);
		this.context=context;
	}
	public int processUpdate(PartitionResult site, OperateTarget session) {
		// TODO Auto-generated method stub
		return 0;
	}

}
