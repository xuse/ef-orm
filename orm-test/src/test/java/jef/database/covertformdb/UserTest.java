package jef.database.covertformdb;

import java.sql.SQLException;

import javax.sql.DataSource;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.test.covertfromdb.User;
import jef.tools.reflect.BeanUtils;

import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
public class UserTest {
	
	@Autowired
	private DataSource dataSource;
	
	@BeforeClass
	public static void enc() {
		new EntityEnhancer().enhance("jef.database.test.covertfromdb"); 
	}
	
	@Test
	public void test() {
		DbClient db = new DbClient(dataSource);
		
		JefEntityManagerFactory emf=new JefEntityManagerFactory(db);
		CommonDao dao=new CommonDaoImpl();
		 //模拟Spring自动注入
		BeanUtils.setFieldValue(dao, "entityManagerFactory", emf);
		
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
