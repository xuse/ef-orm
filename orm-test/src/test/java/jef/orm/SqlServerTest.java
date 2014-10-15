package jef.orm;

import java.sql.SQLException;
import java.util.Arrays;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbClientFactory;
import jef.database.DbMetaData;

import org.junit.Test;

public class SqlServerTest {
	@Test
	public void test1234() throws SQLException {
		DbClient db = DbClientFactory.getDbClient("sqlserver", "10.17.48.103", 1433, "jiyi", "sa", "hik12345+");
		DbMetaData meta=db.getMetaData(null);
		
		
		System.out.println("============================");
		
		LogUtil.show("catalogs:"+Arrays.toString(meta.getCatalogs())); //返回所有库，当前库位于第一位
		LogUtil.show("current_db:"+meta.getDbName());
		LogUtil.show("schemas:"+Arrays.toString(meta.getSchemas())); //返回所有库，当前库位于第一位
		LogUtil.show("current:"+meta.getCurrentSchema());
		LogUtil.show(meta.getUserName());
		LogUtil.show(meta.getJdbcVersion());
		LogUtil.show(meta.getCurrentTime());
		System.out.println("============================");
		System.out.println("getAllBuildInFunctions:");
		LogUtil.show(meta.getAllBuildInFunctions()); //范明辉库内的schema
		System.out.println("getNumericFunctions:");
		LogUtil.show(meta.getNumericFunctions());
		System.out.println("getStringFunctions:");
		LogUtil.show(meta.getStringFunctions());
		System.out.println("getTimeDateFunctions:");
		LogUtil.show(meta.getTimeDateFunctions());
		System.out.println("getSystemFunctions:");
		LogUtil.show(meta.getSystemFunctions());
		System.out.println("============================");
		LogUtil.show(meta.getTableTypes());
		System.out.println("getTables:");
		LogUtil.show(meta.getTables());
		System.out.println("getFunctions:");
		LogUtil.show(meta.getFunctions(null));
		System.out.println("getProcedures:");
		LogUtil.show(meta.getProcedures(null));
		System.out.println("getViews:");
		LogUtil.show(meta.getViews(false));
		System.out.println("============================");
		LogUtil.show(meta.getSupportDataType());
		System.out.println("============================");
		LogUtil.show(meta.getSQLKeywords());
	}
}

