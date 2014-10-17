package jef.orm;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbClientFactory;
import jef.database.DbMetaData;
import jef.database.NativeQuery;

import org.junit.BeforeClass;
import org.junit.Test;

public class SqlServerTest {

	private static DbClient db;

	@BeforeClass
	public static void setup() throws SQLException {
		db = DbClientFactory.getDbClient("sqlserver", "10.17.48.103", 1433, "jiyi2", "sa", "hik12345+");
	}

	@Test
	public void test1234() throws SQLException {

		DbMetaData meta = db.getMetaData(null);

		System.out.println("============================");

		LogUtil.show("catalogs:" + Arrays.toString(meta.getCatalogs())); // 返回所有库，当前库位于第一位
		LogUtil.show("current_db:" + meta.getDbName());
		LogUtil.show("schemas:" + Arrays.toString(meta.getSchemas())); // 返回所有库，当前库位于第一位
		LogUtil.show("current:" + meta.getCurrentSchema());
		LogUtil.show(meta.getUserName());
		LogUtil.show(meta.getJdbcVersion());
		LogUtil.show(meta.getCurrentTime());
		System.out.println("============================");
		System.out.println("getAllBuildInFunctions:");
		LogUtil.show(meta.getAllBuildInFunctions()); // 范明辉库内的schema
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

	@Test
	public void testTimestamp() throws SQLException {
		String s = db.createNativeQuery("select datediff(current_timestamp,current_timestamp+1)", String.class).getSingleResult();
		System.out.println(s);

		System.out.println(db.createNativeQuery("select current_date", String.class).getSingleResult());

		System.out.println(db.createNativeQuery("select current_time", String.class).getSingleResult());

		System.out.println(db.createNativeQuery("select current_timestamp", String.class).getSingleResult());

		System.out.println(db.createNativeQuery("select sysdate", String.class).getSingleResult());

	}

	@Test
	public void testAddAndAs() {
		System.out.println(db.createNativeQuery("select 'A' || 'B' ", String.class).getSingleResult());

		System.out.println(db.createNativeQuery("select 'A' || 'B' c", String.class).getSingleResult());
	}

	@Test
	public void testDecodeTranslate() throws SQLException {
		List<String> s = db.createNativeQuery("select decode(X,1,'一',2,'二',3,'三','其他') from dual_int", String.class).getResultList();
		System.out.println(s);

		List<String> s1 = db.createNativeQuery("select translate('abcd1234','cd','=+') from dual", String.class).getResultList();
		System.out.println(s1);
	}

	@Test
	public void testPad() {
		System.out.println(db.createNativeQuery("select lpad(X,5,'o') pad_left, rpad(X,5,'o') as pad_right from dual", Map.class).getResultList());

	}

	@Test
	public void testAddMonth() {
		System.out.println(db.createNativeQuery("select add_months(now(), 11) from dual", Map.class).getResultList());
	}

	@Test
	public void testPage() {
		NativeQuery nq=db.createNativeQuery("SELECT distinct person_name,gender FROM person_table order by gender");
		nq.setFirstResult(1);
		nq.setMaxResults(3);
		System.out.println(nq.getResultList());

	}

}