package jef.database.routing.sql;

import java.sql.SQLException;

import jef.database.routing.jdbc.UpdateReturn;

public interface ExecuteablePlan extends ExecutionPlan{
	/**
	 * 执行操作
	 * @param site
	 * @param session
	 * @return
	 */
	UpdateReturn processUpdate(int generateKeys, int[] returnIndex, String[] returnColumns) throws SQLException;
}
