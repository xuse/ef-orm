package jef.orm.custom;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.orm.onetable.model.Foo;

import org.easyframe.fastjson.JSONObject;
import org.junit.Test;

public class DatatypeTest {
	
	DbClient db=new DbClientBuilder().build();

	@Test
	public void test1() throws SQLException{
		db.dropTable(MyFoo.class);
		db.createTable(MyFoo.class);
		System.out.println(db.getMetaData(null).getTable("myfoo"));
		
		MyFoo m=new MyFoo();
		m.setId(1);
		m.setName("test");
		m.setData(Arrays.asList("aas","bgbg","der"));
		
		
		m.setHstoreField(new HashMap<String,String>());
		m.getHstoreField().put("aaa", "bbb");
		m.getHstoreField().put("bbb", "ccc");
		
		
		JSONObject json=new JSONObject();
		json.put("sddsds", "dfcddf");
		json.put("sddsdscdcvd", "dfcddf4545");
		m.setJsonField(json);
		
		db.insert(m);
		
		m.setId(m.getId());
		MyFoo n=db.load(m);
		System.out.println(n.getData());
		System.out.println(n.getHstoreField());
		System.out.println(n.getJsonField());
		
	}
	
	@Test
	public void test2() throws SQLException{
		db.createTable(Foo.class);
	}
	
	
}
