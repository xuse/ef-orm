package jef.database.routing.jdbc;

import jef.database.Session;
import jef.database.annotation.PartitionResult;
import jef.database.jsqlparser.statement.update.Update;

public class UpdateExecutionPlan implements ExecutionPlan{

	public UpdateExecutionPlan(PartitionResult[] results, StatementContext<Update> context) {
		// TODO Auto-generated constructor stub
	}

	public boolean isMultiDatabase() {
		// TODO Auto-generated method stub
		return false;
	}

	public PartitionResult[] getSites() {
		// TODO Auto-generated method stub
		return null;
	}

	public int processUpdate(PartitionResult site, Session session) {
		// TODO Auto-generated method stub
		return 0;
	}

}
