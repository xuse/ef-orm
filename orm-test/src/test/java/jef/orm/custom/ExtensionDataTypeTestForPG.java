package jef.orm.custom;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alibaba.fastjson.JSONObject;

import jef.database.DbClient;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.onetable.model.Foo;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
		@DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"), })
public class ExtensionDataTypeTestForPG {

	protected DbClient db;

	@Test
	@Ignore
	public void test1() throws SQLException {
		db.dropTable(EntWithHStoreAndJsonb.class);
		db.createTable(EntWithHStoreAndJsonb.class);
		System.out.println(db.getMetaData(null).getTable("myfoo"));

		EntWithHStoreAndJsonb m = new EntWithHStoreAndJsonb();
		m.setId(1);
		m.setName("test");
		m.setData(Arrays.asList("aas", "bgbg", "der"));

		m.setHstoreField(new HashMap<String, String>());
		m.getHstoreField().put("aaa", "bbb");
		m.getHstoreField().put("bbb", "ccc");

		JSONObject json = new JSONObject();
		json.put("sddsds", "dfcddf");
		json.put("sddsdscdcvd", "dfcddf4545");
		m.setJsonField(json);

		m.setJsonbField(new HashMap<String, Object>());
		m.getJsonbField().put("test00", "dfdfd");
		m.getJsonbField().put("test01", 1000L);
		m.getJsonbField().put("test02", 1000D);

		db.insert(m);

		m.setId(m.getId());
		EntWithHStoreAndJsonb n = db.load(m);
		System.out.println(n.getJsonbField());
		System.out.println(n.getData());
		System.out.println(n.getHstoreField());
		System.out.println(n.getJsonField());

	}

	@Test
	public void test2() throws SQLException {
		// db.dropTable(Foo.class);
		db.createTable(Foo.class);
		Foo foo = new Foo();
		foo.setName("dfdff");
		db.insert(foo);

		foo = new Foo();
		foo.setName("dfdff2");
		db.insert(foo);
	}

}
