package jef.database.dynamic;

import java.sql.SQLException;

import jef.database.DbClient;
import jef.database.VarObject;
import jef.database.meta.MetaHolder;
import jef.database.meta.TupleMetadata;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
	 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
	 @DataSource(name="derby",url="jdbc:derby:./db;create=true"),
	 @DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	 @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db"),
	@DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class DynamicApplyTest {
	private DbClient db;
	
	@DatabaseInit
	public void test123() throws SQLException{
		int n=0;
		for(String tableName:db.getMetaData(null).getTableNames()){
			System.out.println("数据库表"+tableName+"已经扫描。");
			MetaHolder.initMetadata(db, tableName);
			n++;
			if(n>5)break;
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test2(){
		TupleMetadata meta=MetaHolder.getDynamicMeta("CHILD");
		if(meta!=null){
			VarObject obj=meta.newInstance();
			obj.set("id", "asdfsds"); //数据类型不一致。
		}else{
			throw new IllegalArgumentException();
		}
	}
}
