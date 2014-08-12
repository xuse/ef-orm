package jef.database.dialect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.DbCfg;
import jef.database.DbClient;
import jef.database.DebugUtil;
import jef.database.OperateTarget;
import jef.database.innerpool.IConnection;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.onetable.model.TestEntity;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;
import jef.tools.string.RandomData;
import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * HSQLDB内存模式测试类
 * 
 * @Date 2012-8-6
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	@DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = "") })
public class HsqlDbMemTest extends org.junit.Assert {

	protected DbClient db;
	protected static String queryTable = JefConfiguration.get(DbCfg.DB_QUERY_TABLE_NAME);

	protected OperateTarget getDefaultTarget() {
		return new OperateTarget(db, null);
	}
	
	@DatabaseInit
	public void prepare(){
		try{
			createSchema("ad");
			createTable();
		} catch (Exception e) {
			LogUtil.exception(e);
		}
	}

	@Test
	public void testExistTable() {
		if(StringUtils.isNotEmpty(queryTable)){
			try {
				Assert.assertTrue(db.existTable(queryTable.toUpperCase()));
				Assert.assertTrue(db.existTable(queryTable.toLowerCase()));

				Assert.assertTrue(db.existTable("Test_Entity"));
				Assert.assertTrue(db.existTable("test_entity"));
				Assert.assertTrue(db.existTable("TEST_ENTITY"));

			} catch (SQLException e) {
				Assert.fail(e.getMessage());
			}	
		}
	}

	protected void createSchema(String schema) {
		IConnection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = DebugUtil.getConnection(getDefaultTarget());
			pstmt = conn.prepareStatement("CREATE SCHEMA " + schema);
			pstmt.execute();
		} catch (SQLException e) {
			Assert.fail(e.getMessage());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			try {
				pstmt.close();
				// AfterClass will close conn, so need not call conn.close here.
			} catch (SQLException e) {
				Assert.fail(e.getMessage());
			} catch (Exception e) {
				Assert.fail(e.getMessage());
			}
		}
	}

	protected void createTable() throws SQLException {
		db.createTable(TestEntity.class);
	}

	protected void dropTable(String table) {
		IConnection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = DebugUtil.getConnection(getDefaultTarget());
			pstmt = conn.prepareStatement("DROP TABLE " + table);
			pstmt.execute();
		} catch (SQLException e) {
			Assert.fail(e.getMessage());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			try {
				pstmt.close();
				// AfterClass will close conn, so need not call conn.close here.
			} catch (SQLException e) {
				Assert.fail(e.getMessage());
			} catch (Exception e) {
				Assert.fail(e.getMessage());
			}
		}
	}
	
	@Test
	public void testResultSet() throws SQLException{
		memoryOccupied(10);
		ResultSet rs=db.getResultSet("select field_1,field_2 from test_entity", 0);
		int n=0;
		while(rs.next()){
			System.out.println(rs.getObject(1));
			n++;
		}
		System.out.println(n);
		rs.first();
	}

	/**
	 * 测试内存模式下的内存占用，非十分准确，仅作为参考。
	 */
	@Test
	public void testMemoryOccupied() {
		try {
			memoryOccupied(1000);

//			memoryOccupied(10000);

//			memoryOccupied(100000);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	private void memoryOccupied(int count) throws SQLException {
		Runtime.getRuntime().gc();

		List<TestEntity> batch =new ArrayList<TestEntity>();
		for (int i = 0; i < count; i++) {
			batch.add(RandomData.newInstance(TestEntity.class));
		}

		Runtime.getRuntime().gc();
		long freeMemory = Runtime.getRuntime().freeMemory();

		// 批量提交过程中的内存消耗也被算进去了
		db.batchInsert(batch);

		// 这时运行GC有可能反而会导致内存消耗增加
		// Runtime.getRuntime().gc();

		long occupiedMemory = freeMemory - Runtime.getRuntime().freeMemory();
		System.out.printf("存储 %s 条 %s 占用的内存约: %s KB, 平均约 %s B/条.\r\n", count, "TestEntity", occupiedMemory / 1024, occupiedMemory / count);
	}

}
