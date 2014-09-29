package jef.database.pooltest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import jef.database.DbCfg;
import jef.database.DbUtils;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.database.innerpool.PoolService;
import jef.tools.JefConfiguration;

import org.easyframe.enterprise.spring.TransactionMode;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

/**
 * @author guokui
 *
 */
public class Conn606MinMaxSame {
	
	static org.slf4j.Logger log=LoggerFactory.getLogger(JefConfiguration.class);
	private int maxSize;
	private int minSize;
	private String dbType;
	private String dbHost;
	private String dbPort;
	private String dbName;
	private String dbUser;
	private String dbPasswd;
	private String dbUrl;
	private String dbTypeNoMode;

	public Conn606MinMaxSame() {
		this.maxSize = JefConfiguration.getInt(
				DbCfg.DB_CONNECTION_POOL_MAX, 50);
		this.minSize = JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL,3);
		this.dbType = JefConfiguration.get(DbCfg.DB_TYPE);
		if (dbType.indexOf(':') > 0) {
			this.dbTypeNoMode = dbType.substring(0, dbType.indexOf(':'));
		} else {
			this.dbTypeNoMode = dbType;
		}
		this.dbHost = JefConfiguration.get(DbCfg.DB_HOST);
		this.dbPort = JefConfiguration.get(DbCfg.DB_PORT);
		this.dbName = JefConfiguration.get(DbCfg.DB_NAME);
		this.dbUser = JefConfiguration.get(DbCfg.DB_USER);
		this.dbPasswd = JefConfiguration.get(DbCfg.DB_PASSWORD);
		this.dbUrl = "jdbc" + ":" + dbType + ":@" + dbHost + ":" + dbPort + ":"
				+ dbName;
		log.info("The database url: "+dbUrl);
	}
	
	/*
	 * 测试MIN与MAX的值相同时是否正常运行
	 */
//	@Test
	@Ignore
	public void case606() throws SQLException {
		log.info("Case606:连接池的最小和最大连接数被设置成相同的数值的情况下，能正常工作");
		log.info("The current size of MIN is "+minSize);
		log.info("The current size of MAX is "+maxSize);
		log.info("Oracle 测试用户："+dbUser);
		DataSource ds = DbUtils.createSimpleDataSource(dbUrl, dbUser, dbPasswd);
		final IUserManagedPool pool = PoolService.getPool(ds, maxSize,TransactionMode.JPA);
		// pool.
		for (int i = 0; i < 15; i++) {
			new MyThread(pool).start();
		}
		
		try {
			log.info("主线程需要睡眠5sec,使得其它线程有运行的时间");
			Thread.sleep(5000);
			System.exit(0);
		} catch (InterruptedException e) {
			log.error(e.getMessage());
			System.exit(1);
		}finally{
			System.exit(1);
		}

	}



	static class MyThread extends Thread {
		private IUserManagedPool pool;

		public MyThread(IUserManagedPool cPool) {
			this.pool = cPool;
		}

		@Override
		public void run() {

			IConnection cInPool = null;
			try {
				cInPool = pool.poll();
			} catch (SQLException e1) {
				log.error(e1.getMessage());
				System.exit(1);
			}

				try {
					// lPools.add(pool.getConnection());

					PreparedStatement ps = cInPool.prepareStatement(
									"select * from EMP where ename ='BLAKE'");
					ResultSet rSet = ps.executeQuery();

					while (rSet.next()) {
						log.info("正在执行select语句做查询：  "+rSet.getString("ENAME"));
					}
					// pool.releaseConnection();
					log.info(Thread.currentThread().getName() + "当前连接池数量:"
							+ pool.getStatus());
				} catch (SQLException e) {
					log.error(e.getMessage());
					System.exit(1);
				}
			while(true);
		}
	}

}
