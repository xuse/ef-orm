package jef.database.routing;

import java.sql.SQLException;

import javax.naming.NamingException;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.ORMConfig;
import jef.database.datasource.DbDataSourceLookup;
import jef.database.datasource.JndiDatasourceLookup;
import jef.database.datasource.MapDataSourceLookup;
import jef.database.datasource.PropertiesDataSourceLookup;
import jef.database.datasource.SimpleDataSource;
import jef.database.datasource.SpringBeansDataSourceLookup;
import jef.database.datasource.URLJsonDataSourceLookup;
import jef.database.dialect.ColumnType;
import jef.database.meta.TupleMetadata;
import jef.http.HttpServerEmu;
import jef.http.Response;
import jef.tools.Assert;
import jef.tools.IOUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.base.Function;


/**
 * 单元测试，测试六种DataSourceLookup
 * @author jiyi
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/spring-dslookup-test.xml" })
public class DatasourceLookupTest extends org.junit.Assert implements ApplicationContextAware{
	protected ApplicationContext applicationContext;
	
	@Test
	public void testProperties(){
		PropertiesDataSourceLookup lookup=applicationContext.getBean(PropertiesDataSourceLookup.class);
		Assert.notNull(lookup);
		
		SimpleDataSource ds1=(SimpleDataSource)lookup.getDataSource("ds1");
		assertNotNull(ds1);
		assertEquals("jdbc:mysql://localhost:3306/test", ds1.getUrl());
		assertEquals("root", ds1.getUser());
		assertEquals("root", ds1.getUsername());
		assertEquals("org.gjt.mm.mysql.Driver", ds1.getDriverClass());
		assertEquals(5, lookup.getAvailableKeys().size());
	}
	
	@Test
	public void testJSON(){
		URLJsonDataSourceLookup lookup=applicationContext.getBean(URLJsonDataSourceLookup.class);
		//为了测试，使用HTTP模拟器
		HttpServerEmu server=new HttpServerEmu(9999);
		server.setHandler(new Function<Response, Void>() {
			public Void apply(Response arg0) {
				arg0.returnResource("plain/text;charset=UTF-8",getClass().getResourceAsStream("/spring/datasource_lookup.json"));
				return null;
			}
		}).start();
		SimpleDataSource ds1=(SimpleDataSource)lookup.getDataSource("ds2");
		assertNotNull(ds1);
		assertEquals("jdbc:mysql://localhost:3306/test", ds1.getUrl());
		assertEquals("root", ds1.getUser());
		assertEquals("root", ds1.getUsername());
		assertEquals("org.gjt.mm.mysql.Driver", ds1.getDriverClass());
		assertEquals(5, lookup.getAvailableKeys().size());
		IOUtils.closeQuietly(server);
	}
		
	@Test
	public void testJndi() throws NamingException{
		SimpleNamingContextBuilder jndiEnv = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
		SimpleDataSource ds=new SimpleDataSource("jdbc:mysql://localhost:3306/test","root","admin");
		ds.setDriverClass("com.mysql.jdbc.Driver");
		jndiEnv.bind("ds1", ds);
		jndiEnv.bind("ds2", ds);
		jndiEnv.bind("ds3", ds);
		
		JndiDatasourceLookup lookup=applicationContext.getBean(JndiDatasourceLookup.class);
		SimpleDataSource ds1=(SimpleDataSource)lookup.getDataSource("ds2");
		assertNotNull(ds1);
		assertEquals("jdbc:mysql://localhost:3306/test", ds1.getUrl());
		assertEquals("root", ds1.getUser());
		assertEquals("root", ds1.getUsername());
		assertEquals("com.mysql.jdbc.Driver", ds1.getDriverClass());
		assertEquals(3, lookup.getAvailableKeys().size());
	}
	
	@Test
	public void testDb() throws SQLException{
		mockDatabase();
		
		//开始测试
		DbDataSourceLookup lookup=applicationContext.getBean(DbDataSourceLookup.class);
		SimpleDataSource ds1=(SimpleDataSource)lookup.getDataSource("ds2");
		assertNotNull(ds1);
		assertEquals("jdbc:mysql://localhost:3306/test", ds1.getUrl());
		assertEquals("root", ds1.getUser());
		assertEquals("root", ds1.getUsername());
		assertEquals(3, lookup.getAvailableKeys().size());
	}
	
	@Test
	public void testMap(){
		MapDataSourceLookup lookup=applicationContext.getBean(MapDataSourceLookup.class);
		SimpleDataSource ds1=(SimpleDataSource)lookup.getDataSource("ds2");
		assertEquals("jdbc:derby:./db2;create=true", ds1.getUrl());
		assertEquals(null, ds1.getUser());
		assertEquals(3, lookup.getAvailableKeys().size());
	}
	
	@Test
	public void testSpring(){
		SpringBeansDataSourceLookup lookup=applicationContext.getBean(SpringBeansDataSourceLookup.class);
		SimpleDataSource ds1=(SimpleDataSource)lookup.getDataSource("datasource2");
		assertEquals("jdbc:derby:./db2;create=true", ds1.getUrl());
		assertEquals(null, ds1.getUser());
		assertEquals(3, lookup.getAvailableKeys().size());
	}

	private void mockDatabase() throws SQLException {
		DbClient db=new DbClientBuilder("jdbc:hsqldb:mem:db1","","").setMaxPoolSize(2).build();
		ORMConfig.getInstance().setDebugMode(false);
		TupleMetadata t=new TupleMetadata("DATASOURCE_CONFIG");
		t.addColumn("ENABLE", new ColumnType.Char(1));
		t.addColumn("JDBC_URL", new ColumnType.Varchar(64));
		t.addColumn("DB_USER", new ColumnType.Varchar(64));
		t.addColumn("DB_PASSWORD", new ColumnType.Varchar(64));
		t.addColumn("DATABASE_NAME", new ColumnType.Varchar(64));
		db.createTable(t);
		db.insert(
				t.newInstance().set("ENABLE", "1")
				.set("DATABASE_NAME", "ds1")
				.set("JDBC_URL", "jdbc:mysql://localhost:3306/test")
				.set("DB_USER",  "root")
				.set("DB_PASSWORD",  "d3d43r43")
		);
		db.insert(
				t.newInstance().set("ENABLE", "1")
				.set("DATABASE_NAME", "ds2")
				.set("JDBC_URL", "jdbc:mysql://localhost:3306/test")
				.set("DB_USER",  "root")
				.set("DB_PASSWORD",  "43gdsfsd3")
		);
		db.insert(
				t.newInstance().set("ENABLE", "1")
				.set("DATABASE_NAME", "ds3")
				.set("JDBC_URL", "jdbc:mysql://localhost:3306/test")
				.set("DB_USER",  "root")
				.set("DB_PASSWORD",  "sdfs333csd")
		);
		db.close();
	}

	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		this.applicationContext=arg0;
	}
}
