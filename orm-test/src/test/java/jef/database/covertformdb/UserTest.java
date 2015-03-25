package jef.database.covertformdb;

import java.sql.SQLException;

import jef.database.DbClient;
import jef.database.DbClientBuilder;

import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.junit.Test;

public class UserTest {
	
	
	
	@Test
	public void test() {
		DbClient db = new DbClientBuilder("jdbc:postgresql://pc-jiyi:5432/test","root","admin").build();
		CommonDao dao=new CommonDaoImpl(db);
		
		User user = new User();
		user.setId(2);
		user.setUsername("spling Test");
		user.setFullname("12345");
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
