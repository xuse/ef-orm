package jef.database.routing.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.ORMConfig;
import jef.database.OperateTarget;
import jef.database.annotation.PartitionResult;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.update.Update;
import jef.database.routing.jdbc.UpdateReturn;
import jef.database.wrapper.executor.DbTask;
import jef.tools.StringUtils;

public class UpdateExecutionPlan extends AbstractExecutionPlan implements ExecuteablePlan {
	private StatementContext<Update> context;

	public UpdateExecutionPlan(PartitionResult[] results, StatementContext<Update> context) {
		super(results);
		this.context = context;
	}

	public UpdateReturn processUpdate(int generateKeys, int[] returnIndex, String[] returnColumns) throws SQLException {
		long start = System.currentTimeMillis();
		int total = 0;
		if(sites.length>=ORMConfig.getInstance().getParallelSelect()){
			final AtomicInteger counter=new AtomicInteger();
			List<DbTask> tasks=new ArrayList<DbTask>();
			for (final PartitionResult site : getSites()) {
				tasks.add(new DbTask(){
					public void execute() throws SQLException {
						counter.addAndGet(processUpdate0(site));
					}
				});
			}
			DbUtils.parallelExecute(tasks);
			total=counter.get();
		}else{
			for (PartitionResult site : getSites()) {
				total += processUpdate0(site);
			}	
		}
		
		if (isMultiDatabase() && ORMConfig.getInstance().isDebugMode()) {
			LogUtil.show(StringUtils.concat("Total Executed:", String.valueOf(total), "\t Time cost([DbAccess]:", String.valueOf(System.currentTimeMillis() - start), "ms) |  @", String.valueOf(Thread.currentThread().getId())));
		}
		return new UpdateReturn(total);
	}

	private int processUpdate0(PartitionResult site) throws SQLException {
		OperateTarget db = context.db.getTarget(site.getDatabase());
		int count = 0;
		for (String table : site.getTables()) {
			String sql = getSql(table);
			List<Object> params = context.params;
			count += db.innerExecuteSql(sql, params);
		}
		return count;
	}

	public String getSql(String table) {
		for (Table t : context.modifications) {
			t.setReplace(table);
		}
		String s = context.statement.toString();
		for (Table t : context.modifications) {
			t.removeReplace();
		}
		return s;
	}
}
