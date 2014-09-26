package jef.database.pooltest;


import java.sql.SQLException;

import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;

import org.junit.Before;
import org.junit.Ignore;

/**
 * case609
 *	测试线程池释放连接的时候是否会产生空指针的问题
 *		此case需要debug场景模拟
 *			1.在AbstractConnectionPool的线程的for循环外设置断点
 *			2.在ConnectionPoolV2的release关闭方法置空之前设置断点
 *		让每个连接拿到线程休眠，确保在release之前确保已经进入checker线程的断点
 *		让release线程跳转到置空的位置先不置空 打印出要release的连接的hashCode
 *		进入checker线程一直跳转到判断为空跳过的下一步
 *		让release线程置空
 *		让checker线程往下走 则会模拟出线程不同步的空指针异常
 *
 *	建议要使测试线程的休眠时间要比checker线程休眠的时间要长 
 *	但是checker线程的休眠时间不能太短 要等到线程池满了之后再测试
 *  因为要保证checker线程拿到迭代器是没有释放的
 *  虽然通过debug手段可以控制线程时间的顺序
 * 
 * @author zhaolong
 *
 */
public class Conn609ReleaseNullTest extends AbstractTestConnection{
	
	
	@Before
	public void prepare()
			throws SQLException {
//		 TODO Auto-generated method stub
		String url = ConnDBConfigUtil.getStringValue("url");
		String uname = ConnDBConfigUtil.getStringValue("uname");
		String pwd = ConnDBConfigUtil.getStringValue("pwd");
		int POOL_SIZE=10;
		super.prepare(url, uname, pwd, POOL_SIZE);
	}
	
	
//	public static void main(String[] args) {
//		MyThread[] threads=new MyThread[10];
//		Iterator<MyThread> its=new ArrayIterator<MyThread>(threads);
//		while(its.hasNext()){
//			MyThread t=its.next();
//			for(int i=0;i<10;i++){
//				if(t==threads[i]){
//					System.out.println("yes");
//					return ;
//				}
//			}
//		}
//		
//	}
	
//	@Test
	@Ignore
	public void testCheckerWork() throws SQLException, InterruptedException{

		IUserManagedPool pool=super.getPool();
		MyThread[] threads=new MyThread[10];
		for(int i=0;i<10;i++){
			threads[i]=new MyThread(pool);
			threads[i].start();
		}
		
		//防止主线程结束
		Thread.sleep(15*60*1000);
	}
	
	
	class MyThread extends Thread {

		private IUserManagedPool pool;

		public MyThread() {
			// TODO Auto-generated constructor stub
		}
		public MyThread(IUserManagedPool pool) {
			// TODO Auto-generated constructor stub
			this.pool = pool;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				IConnection conn=pool.poll();
				//休眠等待断开
				Thread.sleep(10*1000);
				conn.close();
			} catch (Exception e) {
				// TODO Auto-generated catch blockx
				e.printStackTrace();
			}
		}
		
		
		

	}

}
