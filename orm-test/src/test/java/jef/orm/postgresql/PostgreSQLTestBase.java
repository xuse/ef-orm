package jef.orm.postgresql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.DbCfg;
import jef.database.DbClient;
import jef.database.DbClientFactory;
import jef.database.DebugUtil;
import jef.database.OperateTarget;
import jef.database.innerpool.IConnection;
import jef.tools.IOUtils;
import jef.tools.JefConfiguration;
import jef.tools.ResourceUtils;
import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * PostgreSQL测试基类
 * 
 * @Company Asiainfo-Linkage Technologies (China), Inc.
 * @author luolp@asiainfo-linkage.com
 * @Date 2012-7-20
 */
public class PostgreSQLTestBase {

	protected static DbClient db;
	protected static boolean isEQ90version = false;
	protected static boolean isEQ84version = false;
	protected static boolean isEQ83version = false;
	protected static boolean isLE82version = false;
	protected static String queryTable;

	@BeforeClass
	public static void init() throws SQLException {
		try{
			db = DbClientFactory.getDbClient(
					"jdbc:postgresql://localhost:5432/mydb", "test", "test");
	
			String dbVersion = db.getMetaData(null).getDatabaseVersion();
			String dvVersionMain = StringUtils.substringBeforeLast(dbVersion, ".");
			isEQ90version = StringUtils.equals(dvVersionMain, "9.0");
			isEQ84version = StringUtils.equals(dvVersionMain, "8.4");
			isEQ83version = StringUtils.equals(dvVersionMain, "8.3");
			isLE82version = Double.valueOf(dvVersionMain).doubleValue() <= 8.2;
	
			queryTable = JefConfiguration.get(DbCfg.DB_QUERY_TABLE_NAME);
	
			enhanceEntities();
		} catch (Exception e) {
			LogUtil.exception(e);
		}
	}

	protected static void enhanceEntities() {
		new EntityEnhancer().enhance("jef.orm.postgresql.model");
	}

	@AfterClass
	public static void close() throws SQLException {
		System.out.println("Closing database connections...");
		if (StringUtils.isNotBlank(queryTable)) {
			dropTable(queryTable);
		}
		db.close();
	}

	private static void dropTable(String table) {
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
				conn.closePhysical();
			} catch (SQLException e) {
				Assert.fail(e.getMessage());
			} catch (Exception e) {
				Assert.fail(e.getMessage());
			}
		}
	}
	protected static OperateTarget getDefaultTarget(){
		return new OperateTarget(db,null);
	}

	protected void prepareDbByNativeSqls(String sqlFilename) throws Exception {
		String sqls = IOUtils.asString(ResourceUtils.getResource(sqlFilename),
				"UTF-8");

		IConnection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = DebugUtil.getConnection(getDefaultTarget());
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(sqls);
			pstmt.execute();
			conn.commit();
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

}
