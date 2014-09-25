package jef.database.pooltest;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.tools.ThreadUtils;

import org.junit.Before;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

/**
 * 测试是否存在连接泄露
 * @author zhaolong
 *
 */
public class Conn6xxLeakTest extends AbstractTestConnection{

	private final int CONN_LOOPS =10;
	private final AtomicInteger count = new AtomicInteger(0);
	private org.slf4j.Logger log=LoggerFactory.getLogger(Conn6xxLeakTest.class);
	
	
	@Before
	public void prepare()
			throws SQLException {
		String url = ConnDBConfigUtil.getStringValue("url");
		String uname = ConnDBConfigUtil.getStringValue("uname");
		String pwd = ConnDBConfigUtil.getStringValue("pwd");
		int POOL_SIZE=ConnDBConfigUtil.getIntValue("pool.size");
		super.prepare(url, uname, pwd, POOL_SIZE);
	}
	

	//测试多线程的获取释放连接是否会产生连接泄露
//	@Test
	@Ignore
	public void testConnLeak() throws Exception {
		// check num of conn is less than expected
		count.getAndSet(0);
		IUserManagedPool pool=super.getPool();
		MyThread[] threads=new MyThread[CONN_LOOPS];
		for (int j = 0; j < CONN_LOOPS; j++) {
			threads[j] = new MyThread(pool, j, this,true);
		}
		for(int j=0;j<CONN_LOOPS;j++){
			Thread.sleep(100);
			threads[j].start();
		}
		
		//当全部线程执行完毕 输出连接池的属性 查看是否存在连接泄露
		while (count.get() < CONN_LOOPS) {
			ThreadUtils.doWait(pool);
		}
	
		String msg="当前连接池数量:" + pool.getStatus();
		ConnPrintOutUtil.print(log, "info", msg);
		pool.close();
	}


	class MyThread extends Thread {

		private IUserManagedPool<IConnection> pool;
		private int index;
		private Conn6xxLeakTest connLeakTest;
		private boolean isRelease = true;

		public MyThread() {
			// TODO Auto-generated constructor stub
		}

		public MyThread(IUserManagedPool pool, int index,
				Conn6xxLeakTest connLeakTest) {
			// TODO Auto-generated constructor stub
			this.pool = pool;
			this.index = index;
			this.connLeakTest = connLeakTest;
		}

		public MyThread(IUserManagedPool pool, int index,
				Conn6xxLeakTest connLeakTest, boolean isRelease) {
			// TODO Auto-generated constructor stub
			this(pool,index,connLeakTest);
			this.isRelease = isRelease;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				IConnection conn=pool.poll();
				Thread.sleep(20*1000);
				if (isRelease) {
					conn.close();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch blockx
				String msg="thread " + index + " has exception";
				ConnPrintOutUtil.print(log, ConnPrintOutUtil.ERROR, msg);
				connLeakTest.count.incrementAndGet();
				ThreadUtils.doNotify(pool);
			}
			connLeakTest.count.incrementAndGet();
			ThreadUtils.doNotify(pool);
		}

	}

}
