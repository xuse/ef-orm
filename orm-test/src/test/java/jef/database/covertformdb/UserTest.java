package jef.database.covertformdb;

import java.sql.SQLException;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.test.covertfromdb.User;
import jef.tools.reflect.BeanUtils;

import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.junit.BeforeClass;
import org.junit.Test;

public class UserTest {
	
	
	
	@Test
	public void test() {
		DbClient db = new DbClientBuilder("jdbc:postgresql://pc-jiyi:5432/test","root","admin").build();
		CommonDao dao=new CommonDaoImpl(db);
		
		User user = new User();
		user.setIId(2);
		user.setSUsername("spling Test");
		user.setSFullname("12345");
//		user.setUsername("spling");
//		user.setPassword("xxxxdfds");
		
		try {
			db.insert(user);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
