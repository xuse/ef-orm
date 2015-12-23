package jef.orm.multitable3;

import java.sql.SQLException;

import jef.database.DbClient;
import jef.database.QB;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
//	 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
//	 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
//	 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
//	 @DataSource(name="derby",url="jdbc:derby:./db;create=true"),
	 @DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
//	 @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db"),
//	 @DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class TestCase1 extends org.junit.Assert{
	
private DbClient db;
	
	@BeforeClass
	public static void setUp() throws SQLException{
//		EntityEnhancer en=new EntityEnhancer();
//		en.enhance("jef.orm.multitable2.model");
	}
	
	@DatabaseInit
	public void prepareData() throws SQLException {
		db.createTable(Factor.class,
				Names.class);
	}
	
	@Test
	public void case1() throws SQLException {
		db.select(QB.create(Factor.class));
	}
}
