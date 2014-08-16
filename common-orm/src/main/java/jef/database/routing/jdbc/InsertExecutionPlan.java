package jef.database.routing.jdbc;

import jef.database.OperateTarget;
import jef.database.annotation.PartitionResult;
import jef.database.jsqlparser.statement.insert.Insert;

public class InsertExecutionPlan extends AbstractExecutionPlan{
	
	private StatementContext<Insert> context;
	
	public InsertExecutionPlan(PartitionResult[] results, StatementContext<Insert> context) {
		super(results);
		this.context=context;
	}

	
	//Insert操作是最简单的因为表名肯定只有一个
	public int processUpdate(PartitionResult site, OperateTarget session) {
		String table=site.getAsOneTable();
		session=session.getTarget(site.getDatabase());
		
				
		
		return 0;
	}

}
