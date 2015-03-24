package org.googlecode.jef.spring;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.QB;
import jef.database.datasource.DataSourceInfoImpl;
import jef.database.jpa.JefEntityManager;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.meta.MetaHolder;
import jef.database.test.SpringTestBase;
import jef.orm.multitable2.model.Root;
import jef.tools.ArrayUtils;

import org.googlecode.jef.spring.case2.ServiceRequired;
import org.googlecode.jef.spring.entity.BindEntity1;
import org.googlecode.jef.spring.entity.BindEntity2;
import org.googlecode.jef.spring.entity.BindEntity3;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Propagation;

public class JefTransactionTest extends SpringTestBase {
	@BeforeClass
	public static void enhanceEntity() {
		EntityEnhancer en = new EntityEnhancer();
		en.enhance("org.googlecode.jef.spring");
		
		
	}

	public void testCreateTables(ApplicationContext ctx) throws SQLException {
		JefEntityManagerFactory factory = ctx.getBean(JefEntityManagerFactory.class);
		DbClient client = factory.getDefault();
		client.dropTable(BindEntity1.class, BindEntity2.class, BindEntity3.class);
		client.createTable(BindEntity1.class, BindEntity2.class, BindEntity3.class);
	}

	@Test
	public void testDbDatasourceLookup() throws SQLException {
		ensureLocalConfig();
		ApplicationContext ctx = super.initContext();
		testCreateTables(ctx);
	}

	private void ensureLocalConfig() throws SQLException {
		DbClient db=new DbClientBuilder("jdbc:derby:./db;create=true","pomelo","pomelo",1).build();
		db.createTable(DataSourceInfoImpl.class);
		db.delete(QB.create(DataSourceInfoImpl.class));
		
		DataSourceInfoImpl info=new DataSourceInfoImpl();
		info.setDbKey("dataSource");
		info.setUrl("jdbc:mysql://localhost:3307/test");
		info.setUser("root");
		info.setPassword("admin");
		db.insert(info);
		
		info=new DataSourceInfoImpl();
		info.setDbKey("dataSource2");
		info.setUrl("jdbc:mysql://localhost:3307/test2");
		info.setUser("root");
		info.setPassword("admin");
		db.insert(info);
		
		info=new DataSourceInfoImpl();
		info.setDbKey("dataSource3");
		info.setUrl("jdbc:mysql://localhost:3307/test3");
		info.setUser("root");
		info.setPassword("admin");
		db.insert(info);
		
		db.close();
	}

	public interface DbCall {
		void call(JefEntityManager em);
	}

	/**
	 * 测试常规的事务
	 * 
	 * @throws SQLException
	 */
	@Test
	public void JefTransactionTest1() throws SQLException {
		ApplicationContext ctx = super.initContext();
		ServiceRequired tm = ctx.getBean(ServiceRequired.class);
		List<DbCall> calls = new ArrayList<DbCall>();
		DbCall call = new DbCall() {
			public void call(JefEntityManager em) {
				try {
					em.getSession().getNoTransactionSession().getMetaData(null).createTable(MetaHolder.getMeta(Root.class), "root");
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

	/**
	 * 多数据源，绑定不同的库
	 * 
	 * @throws SQLException
	 */
	@Test
	public void JefTransactionTest2() throws SQLException {
		ApplicationContext ctx = super.initContext();
		testCreateTables(ctx);
	}
}
