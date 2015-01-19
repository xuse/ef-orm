package org.easyframe.tutorial.lessonb;

import java.sql.SQLException;

import jef.common.log.LogUtil;
import jef.database.SqlTemplate;

import org.easyframe.enterprise.spring.CommonDao;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration(locations = { "classpath:spring/spring-test-jta.xml" })

public class CaseJTA extends AbstractJUnit4SpringContextTests {
	@Autowired
	private CommonDao dao;
	
	
	@Before
	public void setup() throws SQLException{
		LogUtil.show(dao.getNoTransactionSession().getMetaData("ds1").getTables());
		LogUtil.show(dao.getNoTransactionSession().getMetaData("ds2").getTables());
	}

	@Transactional
	@Test
	public void ysss1() throws SQLException {
		SqlTemplate ds1=dao.getSession().getSqlTemplate("ds1");
		
		SqlTemplate ds2=dao.getSession().getSqlTemplate("ds2");
		
		
		
	}
}
