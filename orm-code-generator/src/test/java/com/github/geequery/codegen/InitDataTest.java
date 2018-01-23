package com.github.geequery.codegen;

import java.sql.SQLException;

import jef.database.DbClient;

import org.easyframe.enterprise.spring.SessionFactoryBean;
import org.junit.Test;

import com.sun.jna.platform.win32.Netapi32Util.User;

public class InitDataTest {
	@Test
	public void testInitData() throws SQLException {

		SessionFactoryBean bean = new SessionFactoryBean();
		bean.setDataSource("jdbc:mysql://localhost:3306/test", "root", "admin");
//		bean.setAnnotatedClasses(new String[] { Interface.class.getName(), Project.class.getName(), User.class.getName()});
		bean.setInitData(true);
		DbClient sf=bean.build();
//		sf.dropTable(Interface.class);
//		sf.dropTable(Project.class);
//		sf.dropTable(User.class.getName());
		sf.shutdown();
	}
}
