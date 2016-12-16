package jef.orm.multitable3;

import java.sql.SQLException;
import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import jef.database.DbClient;
import jef.database.QB;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
	 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
	 @DataSource(name="derby",url="jdbc:derby:./db;create=true"),
	 @DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	 @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db?date_string_format=yyyy-MM-dd HH:mm:ss"),
	 @DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
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
		db.createTable(Factor2.class,Role.class,Sub1.class);
	}
	
	@Test
	public void case1() throws SQLException {
		db.select(QB.create(Factor.class));
	}
	
	@Test
	public void addData() throws SQLException {
		Role r1=new Role("Role1");
		Role r2=new Role("Role2");
		{
			Factor2 f=new Factor2();
			f.setRoles(new ArrayList<Role>());
			f.getRoles().add(r1);
			f.getRoles().add(r2);
			f.setSub1(new ArrayList<Sub1>());
			
			Sub1 s1=new Sub1("Sub1-1");
			Sub1 s2=new Sub1("Sub1-2");
			f.getSub1().add(s1);
			f.getSub1().add(s2);
			
			db.insertCascade(f);	
		}
		
		
		System.out.println("=====================================");
		Factor2 f2=db.load(Factor2.class,1);
		f2.setRoles(null);
//		f2.getRoles().clear();
//		f2.getRoles().add(r1);
		db.updateCascade(f2);
		
		
		
		
	}
}
