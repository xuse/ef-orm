package jef.database.covertformdb;

import java.sql.SQLException;

import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.junit.Ignore;
import org.junit.Test;

import jef.database.DbClient;
import jef.database.DbClientBuilder;

public class UserTest {
	
	
	
	@Test
	@Ignore
	public void test() throws SQLException {
		DbClient db = new DbClientBuilder("jdbc:postgresql://pc-jiyi:5432/test","root","admin").build();
		CommonDao dao=new CommonDaoImpl(db);
		
		db.refreshTable(User.class);
		
		db.dropTable(User.class);
		db.createTable(User.class);
		
		User user = new User();
		user.setUsername("spling Test");
		user.setFullname("12345");
		user.setPassword("fddsfdfd");
		user.setFullname("dsdsds");

		
		db.insert(user);
		User loaded=db.load(user);
		
		loaded.setEmail("jiyi@sdcxdf.com");
		db.update(loaded);
		
		db.delete(loaded);
	}

}
