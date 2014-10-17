package jef.orm.onetable;

import java.sql.SQLException;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.ORMConfig;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.multitable2.model.EnumationTable;
import jef.orm.multitable2.model.Root;
import jef.tools.string.RandomData;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Schema重定向单元测试类
 * @Date 2012-10-17
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({ 
	@DataSource(name = "oracle", url = "${oracle.url}", user = "${oracle.user}", password = "${oracle.password}"), 
	@DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	@DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"), 
	@DataSource(name = "derby", url = "jdbc:derby:./db;create=true"),
	@DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	@DataSource(name = "sqlite", url = "jdbc:sqlite:test.db"),
	@DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class AutoAdjustSchemaTest {

	private static final String TABLE_NAME = "ailk2.root";

	private DbClient db;

	@DatabaseInit
	public void init() {
		EntityEnhancer en = new EntityEnhancer();
		en.enhance("jef.orm");
		ORMConfig.getInstance().setSchemaMapping("AILK2:,JIYI:pomelo");
		try {
			dropTable();
			createtable();
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	private void dropTable() throws SQLException {
		db.dropTable(TABLE_NAME);
	}

	private void createtable() throws SQLException {
		db.createTable(Root.class, TABLE_NAME,null);
		db.createTable(EnumationTable.class);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testCRUD() throws SQLException {
			Assert.assertTrue(db.existTable(TABLE_NAME));

			Root root = RandomData.newInstance(Root.class);
			db.insert(root, TABLE_NAME);
			
			
			root.startUpdate();
			root.getQuery().setCustomTableName(TABLE_NAME);
			Assert.assertEquals(1,
					db.select(root.getQuery(), null).size());

			String name = "name" + RandomStringUtils.randomNumeric(6);
			root.setName(name);
			db.update(root, TABLE_NAME);
			root.getQuery().setCustomTableName(TABLE_NAME);
			List roots = db.select(root.getQuery(), null);
			Assert.assertEquals(name, ((Root) roots.get(0)).getName().trim());
			root.getQuery().setCustomTableName(TABLE_NAME);
			db.delete(root);
			root.getQuery().setCustomTableName(TABLE_NAME);
			Assert.assertEquals(0,
					db.select(root.getQuery(), null).size());
		
	}

}
