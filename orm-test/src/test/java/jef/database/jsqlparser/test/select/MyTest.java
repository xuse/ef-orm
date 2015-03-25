package jef.database.jsqlparser.test.select;

import java.sql.SQLException;
import java.util.Map;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.NativeQuery;
import jef.database.jsqlparser.parser.ParseException;

import org.junit.Test;

public class MyTest {
	@Test(expected=RuntimeException.class)
	public void main2() throws ParseException, SQLException {
		DbClient db=new DbClientBuilder().setEnhancePackages("none").build();
		
		String source="select t.* from :tablename<sql> t";
		
		NativeQuery<?> nq=db.createNativeQuery(source);
		nq.setParameter("tablename", "RootA");
		nq.getResultList();
		db.close();
	}
	
	
	@Test(expected=Exception.class)
	public void main3() throws ParseException, SQLException {
		DbClient db=new DbClientBuilder().setEnhancePackages("none").build();
		NativeQuery<?> nq=db.createNamedQuery("myTest", Map.class);
		nq.getResultList();
		db.close();
	}
}
