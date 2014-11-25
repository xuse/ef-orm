package jef.database.routing.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.ORMConfig;
import jef.database.annotation.PartitionResult;
import jef.database.jdbc.GenerateKeyReturnOper;
import jef.database.jdbc.JDBCTarget;
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

	public UpdateReturn processUpdate(GenerateKeyReturnOper oper) throws SQLException {
		long start = System.currentTimeMillis();
		int total = 0;
		if (sites.length >= ORMConfig.getInstance().getParallelSelect()) {
			final AtomicInteger counter = new AtomicInteger();
			List<DbTask> tasks = new ArrayList<DbTask>();
			for (PartitionResult site : getSites()) {
				final List<String> sqls = new ArrayList<String>(site.tableSize());
				final String siteName = site.getDatabase();
				for (String table : site.getTables()) {
					sqls.add(getSql(table));
				}
				tasks.add(new DbTask() {
					public void execute() throws SQLException {
						counter.addAndGet(processUpdate0(siteName, sqls));
					}
				});
			}
			DbUtils.parallelExecute(tasks);
			total = counter.get();
		} else {
			for (PartitionResult site : getSites()) {
				List<String> sqls = new ArrayList<String>(site.tableSize());
				for (String table : site.getTables()) {
					sqls.add(getSql(table));
				}
				total += processUpdate0(site.getDatabase(), sqls);
			}
		}

		if (isMultiDatabase() && ORMConfig.getInstance().isDebugMode()) {
			LogUtil.show(StringUtils.concat("Total Executed:", String.valueOf(total), "\t Time cost([DbAccess]:", String.valueOf(System.currentTimeMillis() - start), "ms) |  @", String.valueOf(Thread.currentThread().getId())));
		}
		return new UpdateReturn(total);
	}

	private int processUpdate0(String site, List<String> sqls) throws SQLException {
		JDBCTarget db = context.db.getTarget(site);
		int count = 0;
		for (String sql : sqls) {
			List<Object> params = context.params;
			count += db.innerExecuteUpdate(sql, params, GenerateKeyReturnOper.NONE).getAffectedRows();
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
