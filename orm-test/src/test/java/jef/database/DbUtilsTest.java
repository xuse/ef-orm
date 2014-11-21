package jef.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.database.wrapper.executor.DbTask;
import jef.tools.ThreadUtils;

import org.junit.Test;

public class DbUtilsTest {
	@Test
	public void testFinal() throws SQLException {
		List<DbTask> tasks=new ArrayList<DbTask>();
		for (int i = 0; i < 6; i++) {
			final String s="aaa"+i;
			tasks.add(new DbTask(){
				public void execute() throws SQLException {
					ThreadUtils.doSleep(2000);
					System.out.println(s+" "+Thread.currentThread().getName());
				}
			});
		}
		DbUtils.parallelExecute(tasks);
	}

}
