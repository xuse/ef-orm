package jef.database.dialect;

import java.sql.SQLException;
import java.util.Date;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.ddl.TableForTest;
import jef.database.query.Func;
import jef.orm.onetable.model.Keyword;
import jef.tools.ThreadUtils;
import jef.tools.string.RandomData;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class SqlLiteTest {
	
	
	@Test
	public void sqliteLock() throws SQLException {
		DbClient db=new DbClientBuilder("jdbc:sqlite:c:/aa.db?busy_timeout=5000",null,null).build();
		ThreadUtils.doSleep(5000);
		db.createTable(Keyword.class);
		for(int i=0;i<5000;i++) {
			db.insert(RandomData.newInstance(Keyword.class));
		}
		db.shutdown();
	}

	@Test
	public void sqliteLock2() throws SQLException {
		DbClient db=new DbClientBuilder("jdbc:sqlite:c:/aa.db?busy_timeout=5000",null,null).build();
//		db.createTable(Person.class);
//		for(int i=0;i<5000;i++) {
//			db.insert(RandomData.newInstance(Person.class));
//		}
//		ThreadUtils.doSleep(30000);
		db.dropTable(TableForTest.class);
		db.createTable(TableForTest.class);
		Date date=db.getExpressionValue(Func.current_timestamp, Date.class);
		System.out.println(date);
		System.out.println(new Date());
		db.shutdown();
	}
	
	
	
	
	
}
