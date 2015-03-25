/*
 * Copyright 1999-2011 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.druid.benckmark.pool;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import jef.database.innerpool.IPool;
import jef.database.innerpool.PoolService;
import jef.database.innerpool.SimplePooledDatasource;
import jef.tools.ThreadUtils;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Before;
import org.junit.Test;
import org.logicalcobwebs.proxool.ProxoolDataSource;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.admin.jmx.ProxoolJMXHelper;

import com.alibaba.druid.TestUtil;
import com.alibaba.druid.mock.MockConnection;
import com.alibaba.druid.mock.MockDriver;
import com.alibaba.druid.pool.DruidDataSource;
import com.jolbox.bonecp.BoneCPDataSource;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * TestOnBo 类Case1.java的实现描述：TODO 类实现描述
 * 
 * @author admin 2011-5-28 下午03:47:40
 * 
 * 
 * 
 */
public class PoolPerformanceTest {
/**
	主要功能对比

	 Druid BoneCP DBCP C3P0 Proxool JBoss 
	LRU 是 否 是 否 是 是 
	PSCache 是 是 是 是 否 否 
	PSCache-Oracle-Optimized 是 否 否 否 否 否 
	ExceptionSorter 是 否 否 否 否 是 

	LRU
	LRU是一个性能关键指标，特别Oracle，每个Connection对应数据库端的一个进程，如果数据库连接池遵从LRU，有助于数据库服务器优化，这是重要的指标。在测试中，Druid、DBCP、Proxool是遵守LRU的。BoneCP、C3P0则不是。BoneCP在mock环境下性能可能好，但在真实环境中则就不好了。

	PSCache
	PSCache是数据库连接池的关键指标。在Oracle中，类似SELECT NAME FROM USER WHERE ID = ?这样的SQL，启用PSCache和不启用PSCache的性能可能是相差一个数量级的。Proxool是不支持PSCache的数据库连接池，如果你使用Oracle、SQL Server、DB2、Sybase这样支持游标的数据库，那你就完全不用考虑Proxool。

	PSCache-Oracle-Optimized
	Oracle 10系列的Driver，如果开启PSCache，会占用大量的内存，必须做特别的处理，启用内部的EnterImplicitCache等方法优化才能够减少内存的占用。这个功能只有DruidDataSource有。如果你使用的是Oracle Jdbc，你应该毫不犹豫采用DruidDataSource。

	ExceptionSorter
	ExceptionSorter是一个很重要的容错特性，如果一个连接产生了一个不可恢复的错误，必须立刻从连接池中去掉，否则会连续产生大量错误。这个特性，目前只有JBossDataSource和Druid实现。Druid的实现参考自JBossDataSource。
*/
	
	private String jdbcUrl;
	private String user;
	private String password;
	private String driverClass;
	private int initialSize = 10;
	private int minPoolSize = 10;
	private int maxPoolSize = 30;
	private int maxActive = 30;
	private String validationQuery = "SELECT 1";
	private int threadCount = 50;
	private int loopCount = 5;
	final int LOOP_COUNT = 10000 * 50 * 1 / threadCount;

	private static AtomicLong physicalConnStat = new AtomicLong();


	public static class TestDriver extends MockDriver {
		AtomicInteger open = new AtomicInteger();
		AtomicInteger close = new AtomicInteger();
		
		public void reset(){
			System.out.println("Open:" + open+"  Close:"+close);
			open.set(0);
			close.set(0);
		}
		
		public static TestDriver instance = new TestDriver();

		public boolean acceptsURL(String url) throws SQLException {
			if (url.startsWith("jdbc:test:")) {
				return true;
			}
			return super.acceptsURL(url);
		}

		
		
		
		public Connection connect(String url, Properties info) throws SQLException {
			physicalConnStat.incrementAndGet();
			open.incrementAndGet();
			return super.connect("jdbc:mock:case1", info);
		}
		

		@Override
		protected void afterConnectionClose(MockConnection conn) {
			super.afterConnectionClose(conn);
			close.incrementAndGet();
		}
	}

	@Before
	public void setUp() throws Exception {
		DriverManager.registerDriver(TestDriver.instance);

		user = "dragoon25";
		password = "dragoon25";

		// jdbcUrl = "jdbc:h2:mem:";
		// driverClass = "org.h2.Driver";
		jdbcUrl = "jdbc:test:case1:";
		driverClass = "com.alibaba.druid.benckmark.pool.PoolPerformanceTest$TestDriver";

		physicalConnStat.set(0);
	}

	@Test
	public void test_druid() throws Exception {
		DruidDataSource dataSource = new DruidDataSource();

		dataSource.setInitialSize(initialSize);
		dataSource.setMaxActive(maxActive);
		dataSource.setMinIdle(minPoolSize);
		dataSource.setMaxIdle(maxPoolSize);
		dataSource.setPoolPreparedStatements(true);
		dataSource.setDriverClassName(driverClass);
		dataSource.setUrl(jdbcUrl);
		dataSource.setPoolPreparedStatements(true);
		dataSource.setUsername(user);
		dataSource.setPassword(password);
		dataSource.setValidationQuery(validationQuery);
		dataSource.setTestOnBorrow(false);
		System.out.println(dataSource.getClass().getSimpleName());
		for (int i = 0; i < loopCount; ++i) {
			p0(dataSource, "druid", threadCount);
		}
		System.out.println();
		TestDriver.instance.reset();
		dataSource.close();
	}

	// public void test_jobss() throws Exception {
	// LocalTxDataSourceDO dataSourceDO = new LocalTxDataSourceDO();
	// dataSourceDO.setBlockingTimeoutMillis(1000 * 60);
	// dataSourceDO.setMaxPoolSize(maxPoolSize);
	// dataSourceDO.setMinPoolSize(minPoolSize);
	//
	// dataSourceDO.setDriverClass(driverClass);
	// dataSourceDO.setConnectionURL(jdbcUrl);
	// dataSourceDO.setUserName(user);
	// dataSourceDO.setPassword(password);
	//
	// LocalTxDataSource tx =
	// TaobaoDataSourceFactory.createLocalTxDataSource(dataSourceDO);
	// DataSource dataSource = tx.getDatasource();
	//
	// for (int i = 0; i < loopCount; ++i) {
	// p0(dataSource, "jboss-datasource", threadCount);
	// }
	// System.out.println();
	// }
	@Test
	public void test_dbcp() throws Exception {
		final BasicDataSource dataSource = new BasicDataSource();

		dataSource.setInitialSize(initialSize);
		dataSource.setMaxActive(maxActive);
		dataSource.setMinIdle(minPoolSize);
		dataSource.setMaxIdle(maxPoolSize);
		dataSource.setPoolPreparedStatements(true);
		dataSource.setDriverClassName(driverClass);
		dataSource.setUrl(jdbcUrl);
		dataSource.setPoolPreparedStatements(true);
		dataSource.setUsername(user);
		dataSource.setPassword(password);
		dataSource.setValidationQuery("SELECT 1");
		dataSource.setTestOnBorrow(false);
		System.out.println(dataSource.getClass().getSimpleName());
		for (int i = 0; i < loopCount; ++i) {
			p0(dataSource, "dbcp", threadCount);
		}
		System.out.println();
		dataSource.close();
		TestDriver.instance.reset();
	}

	@Test
	public void test_bonecp() throws Exception {
		BoneCPDataSource dataSource = new BoneCPDataSource();
		// dataSource.(10);
		// dataSource.setMaxActive(50);
		System.out.println(dataSource.getClass().getSimpleName());
		dataSource.setMinConnectionsPerPartition(minPoolSize);
		dataSource.setMaxConnectionsPerPartition(maxPoolSize);

		dataSource.setDriverClass(driverClass);
		dataSource.setJdbcUrl(jdbcUrl);
		dataSource.setStatementsCacheSize(100);
		dataSource.setServiceOrder("LIFO");
		// dataSource.setMaxOpenPreparedStatements(100);
		dataSource.setUsername(user);
		dataSource.setPassword(password);
		// dataSource.setConnectionTestStatement("SELECT 1");
		dataSource.setPartitionCount(1);
		dataSource.setAcquireIncrement(5);
		dataSource.setIdleConnectionTestPeriod(0L);
		
		
		Connection c=dataSource.getConnection();
		ThreadUtils.doSleep(100000);
		c.close();
		// dataSource.setDisableConnectionTracking(true);


		for (int i = 0; i < loopCount; ++i) {
			p0(dataSource, "boneCP", threadCount);
		}
		dataSource.close();
		System.out.println();
	}
	
	@Test
	public void test_c3p0() throws Exception {
		ComboPooledDataSource dataSource = new ComboPooledDataSource();
		// dataSource.(10);
		// dataSource.setMaxActive(50);
		dataSource.setMinPoolSize(minPoolSize);
		dataSource.setMaxPoolSize(maxPoolSize);

		dataSource.setDriverClass(driverClass);
		dataSource.setJdbcUrl(jdbcUrl);
		// dataSource.setPoolPreparedStatements(true);
		// dataSource.setMaxOpenPreparedStatements(100);
		dataSource.setUser(user);
		dataSource.setPassword(password);
		System.out.println(dataSource.getClass().getSimpleName());
		for (int i = 0; i < loopCount; ++i) {
			p0(dataSource, "c3p0", threadCount);
		}
		System.out.println();
		dataSource.close();
		TestDriver.instance.reset();
	}

	@Test
	public void test_proxool() throws Exception {
		
		ProxoolDataSource dataSource2 = new ProxoolDataSource();
		
		ProxoolDataSource dataSource = new ProxoolDataSource();
		// dataSource.(10);
		// dataSource.setMaxActive(50);
		dataSource.setMinimumConnectionCount(minPoolSize);
		dataSource.setMaximumConnectionCount(maxPoolSize);

		dataSource.setDriver(driverClass);
		dataSource.setDriverUrl(jdbcUrl);
		// dataSource.setPoolPreparedStatements(true);
		// dataSource.setMaxOpenPreparedStatements(100);
		dataSource.setUser(user);
		dataSource.setPassword(password);
		dataSource.setJmx(true);
		dataSource.setJmxAgentId("123");
		

		
		
		Connection co=dataSource.getConnection();
		co.close();
		Object o=ProxoolJMXHelper.getObjectName(dataSource.getJmxAgentId());
		
		System.out.println(o);
		ProxoolFacade.getConnectionPoolDefinition(null);
		
		System.out.println(dataSource.getClass().getSimpleName());
		for (int i = 0; i < loopCount; ++i) {
			p0(dataSource, "proxool", threadCount);
		}
		System.out.println();
//		dataSource.
		TestDriver.instance.reset();
	}

	@Test
	public void test_easyframe() throws Exception {
		// dataSource.(10);
		// dataSource.setMaxActive(50);
		// dataSource.setPoolPreparedStatements(true);
		// dataSource.setMaxOpenPreparedStatements(100);

		DataSource dataSource = PoolService.getPooledDatasource(jdbcUrl, driverClass, user, password, minPoolSize, maxPoolSize);

		for (int i = 0; i < loopCount; ++i) {
			p0(dataSource, "easyframe", threadCount);
		}
		System.out.println();
		((IPool<DataSource>)dataSource).close();
		TestDriver.instance.reset();
	}

	@Test
    public void test_easyframe2() throws Exception {
		SimplePooledDatasource dataSource=new SimplePooledDatasource();
		dataSource.setUrl(jdbcUrl);
		dataSource.setDriverClass(driverClass);
		dataSource.setUsername(user);
		dataSource.setPassword(password);
		dataSource.setMax(maxPoolSize);
		dataSource.setMin(minPoolSize);
		
		for (int i = 0; i < loopCount; ++i) {
			p0(dataSource, "easyframe2", threadCount);
		}
		System.out.println();
		dataSource.close();
		TestDriver.instance.reset();
    }

	@Test
	public void test_tomcat_jdbc() throws Exception {
		org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
		// dataSource.(10);
		dataSource.setMaxIdle(maxPoolSize);
		dataSource.setMinIdle(minPoolSize);
		dataSource.setMaxActive(maxPoolSize);

		dataSource.setDriverClassName(driverClass);
		dataSource.setUrl(jdbcUrl);
		// dataSource.setPoolPreparedStatements(true);
		// dataSource.setMaxOpenPreparedStatements(100);
		dataSource.setUsername(user);
		dataSource.setPassword(password);
		System.out.println(dataSource.getClass().getSimpleName());
		for (int i = 0; i < loopCount; ++i) {
			p0(dataSource, "tomcat-jdbc", threadCount);
		}
		dataSource.close();
		System.out.println();
	}

	private void p0(final DataSource dataSource, String name, int threadCount) throws Exception {
		//ThreadUtils.doSleep(600000);
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch endLatch = new CountDownLatch(threadCount);
		final CountDownLatch dumpLatch = new CountDownLatch(1);

		Thread[] threads = new Thread[threadCount];
		for (int i = 0; i < threadCount; ++i) {
			Thread thread = new Thread() {

				public void run() {
					try {
						startLatch.await();

						for (int i = 0; i < LOOP_COUNT; ++i) {
							Connection conn = dataSource.getConnection();
							conn.close();
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					endLatch.countDown();

					try {
						dumpLatch.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			threads[i] = thread;
			thread.start();
		}
		long startMillis = System.currentTimeMillis();
		long startYGC = TestUtil.getYoungGC();
		long startFullGC = TestUtil.getFullGC();
		startLatch.countDown();
		endLatch.await();

		long[] threadIdArray = new long[threads.length];
		for (int i = 0; i < threads.length; ++i) {
			threadIdArray[i] = threads[i].getId();
		}
		ThreadInfo[] threadInfoArray = ManagementFactory.getThreadMXBean().getThreadInfo(threadIdArray);
		dumpLatch.countDown();

		long blockedCount = 0;
		long waitedCount = 0;
		for (int i = 0; i < threadInfoArray.length; ++i) {
			ThreadInfo threadInfo = threadInfoArray[i];
			blockedCount += threadInfo.getBlockedCount();
			waitedCount += threadInfo.getWaitedCount();
		}

		long millis = System.currentTimeMillis() - startMillis;
		long ygc = TestUtil.getYoungGC() - startYGC;
		long fullGC = TestUtil.getFullGC() - startFullGC;
		System.out.println("thread " + threadCount + " " + name + " millis : " + NumberFormat.getInstance().format(millis) + "; YGC " + ygc + " FGC " + fullGC + " blocked " + NumberFormat.getInstance().format(blockedCount) //
				+ " waited " + NumberFormat.getInstance().format(waitedCount) + " physicalConn " + physicalConnStat.get());

	}
}
