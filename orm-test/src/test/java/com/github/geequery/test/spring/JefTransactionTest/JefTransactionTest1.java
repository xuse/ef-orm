package com.github.geequery.test.spring.JefTransactionTest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.googlecode.jef.spring.case2.ServiceRequired;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Propagation;

import jef.common.log.LogUtil;
import jef.database.jpa.JefEntityManager;
import jef.database.meta.MetaHolder;
import jef.orm.multitable2.model.Root;
import jef.tools.ArrayUtils;

@ContextConfiguration(locations = { "classpath:JefTransactionTest1.xml" })
public class JefTransactionTest1 extends AbstractJUnit4SpringContextTests{

	/**
	 * 测试常规的事务
	 * 
	 * @throws SQLException
	 */
	@Test
	public void doTest() throws SQLException {
		ServiceRequired tm = applicationContext.getBean(ServiceRequired.class);
		List<DbCall> calls = new ArrayList<DbCall>();
		DbCall call = new DbCall() {
			public void call(JefEntityManager em) {
				try {
					em.getSession().getNoTransactionSession().getMetaData(null)
							.createTable(MetaHolder.getMeta(Root.class), "root");
				} catch (SQLException e) {
					e.printStackTrace();
				}
				LogUtil.show(em.createNativeQuery("select * from root").getSingleResult());
			}
		};
		calls.add(call);
		calls.add(call);
		tm.executeMethod1(ArrayUtils.asList(Propagation.REQUIRES_NEW, Propagation.REQUIRES_NEW), calls);

		tm.executeMethod2();

	}
}
