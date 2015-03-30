package jef.orm.custom;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.orm.onetable.model.Foo;

import org.junit.Test;

import com.alibaba.fastjson.JSONObject;

public class DatatypeTest {

	DbClient db = new DbClientBuilder().build();

	@Test
	public void test1() throws SQLException {
		db.dropTable(MyFoo.class);
		db.createTable(MyFoo.class);
		System.out.println(db.getMetaData(null).getTable("myfoo"));

		MyFoo m = new MyFoo();
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
		MyFoo n = db.load(m);
		System.out.println(n.getJsonbField());
		System.out.println(n.getData());
		System.out.println(n.getHstoreField());
		System.out.println(n.getJsonField());

	}

	@Test
	public void test2() throws SQLException {
//		db.dropTable(Foo.class);
		db.createTable(Foo.class);
		Foo foo = new Foo();
		foo.setName("dfdff");
		db.insert(foo);

		foo = new Foo();
		foo.setName("dfdff2");
		db.insert(foo);
	}

}
