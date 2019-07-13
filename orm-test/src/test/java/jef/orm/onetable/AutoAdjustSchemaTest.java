package jef.orm.onetable;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jef.database.DbClient;
import jef.database.ORMConfig;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.multitable2.model.EnumationTable;
import jef.orm.multitable2.model.Root;
import jef.tools.string.RandomData;

/**
 * Schema重定向单元测试类
 * 
 * @Date 2012-10-17
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	@DataSource(name = "hsqldb", url = "${hsqldb.url}", user = "sa", password = ""),
		@DataSource(name = "oracle", url = "${oracle.url}", user = "${oracle.user}", password = "${oracle.password}"),
		@DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
		@DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"),
		@DataSource(name = "derby", url = "${derby.url}"),
		@DataSource(name = "sqlite", url = "${sqlite.url}"),
		@DataSource(name = "sqlserver", url = "${sqlserver.url}", user = "${sqlserver.user}", password = "${sqlserver.password}") 
	})
public class AutoAdjustSchemaTest {

	private static final String TABLE_NAME = "root_cus";

	private DbClient db;

	@DatabaseInit
	public void init() {
//		ORMConfig.getInstance().setSchemaMapping("AILK2:,JIYI:pomelo");
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
		db.createTable(Root.class, TABLE_NAME, null);
		db.createTable(EnumationTable.class);
		ORMConfig.getInstance().cacheDebug = true;
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testCRUD() throws SQLException {
		
		Assert.assertTrue(db.existsTable(TABLE_NAME));

		Root root = RandomData.newInstance(Root.class);
		db.insert(root, TABLE_NAME);

		//当发生多表外连接级联时，CUSTOMTABLE无效。
		root.getQuery().setCustomTableName(TABLE_NAME);
		Assert.assertEquals(1, db.select(root.getQuery(), null).size());

		root.startUpdate();
		String name = "name" + RandomStringUtils.randomNumeric(6);
		root.setName(name);
		db.update(root, TABLE_NAME);

		root.getQuery().setCustomTableName(TABLE_NAME);
		List roots = db.select(root.getQuery(), null);
		
		//此前查询是自动关联为多表查询的，此时缓存也是按多表进行的。然后更新了单表数据，此时多表缓存并未更新，造成多表查询缓存再次命中
		//得到了更新前的数据
		//BUG: 更新单表数据时，没有刷新引用到这张表的多表查询的缓存。
		Assert.assertEquals(name, ((Root) roots.get(0)).getName().trim());

		root.getQuery().setCustomTableName(TABLE_NAME);
		db.delete(root);

		root.getQuery().setCustomTableName(TABLE_NAME);
		Assert.assertEquals(0, db.select(root.getQuery(), null).size());

	}

}
