package jef.orm.postgresql.model;

import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;

import jef.database.DbClient;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.JefJUnit4DatabaseTestRunner;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
		@DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"),
})
public class PgtypeTest {
	private DbClient db;
	
	@Test
	public void testTypes() throws SQLException{
		db.createTable(PgPojo.class);
		db.dropTable(PgPojo.class);
	}
	

}
