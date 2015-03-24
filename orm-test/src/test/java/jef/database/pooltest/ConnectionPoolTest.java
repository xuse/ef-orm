package jef.database.pooltest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.DbUtils;
import jef.database.DebugUtil;
import jef.database.NativeQuery;
import jef.database.innerpool.IConnection;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.jre5support.ProcessUtil;
import jef.orm.multitable2.model.Child;
import jef.orm.onetable.model.TestEntity;
import jef.tools.ThreadUtils;
import jef.tools.string.RandomData;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
	 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
	 @DataSource(name="derby",url="jdbc:derby:./db;create=true"),
	 @DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	 @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db"),
	@DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class ConnectionPoolTest {
	private DbClient db;
	
	@DatabaseInit
	public void setup() throws SQLException{
		db.createTable(TestEntity.class,Child.class);
		//使用createNativeQuery后，在MySQL下，会自动为建表语句加上`xx`这样的修饰符
		if (!db.getMetaData(null).existTable("DUAL")) {
			db.createNativeQuery("create table dual(X char(1))").executeUpdate();
		}
		if (db.loadBySql("select count(*) from DUAL",Integer.class) != 1) {
			db.createNativeQuery("delete from DUAL").executeUpdate();
			db.createNativeQuery("insert into DUAL values('X')").executeUpdate();
		}
	}
	/**
	 * 测试连接池自动重连功能,在网线断开造成连接实效后,下次能自动重连。该集成案例需要人工干预，故自动测试不进行
	 * @throws SQLException
	 */
	@Test
	@Ignore
	public void testPoolReconnect() throws SQLException{
		DbClient db=new DbClientBuilder("oracle", "oel1246.hz.asiainfo.com", 1521, "oel1246", "XG", "XG").setMaxPoolSize(1).build();
		NativeQuery<Integer> config=db.createNativeQuery("select count(*) from JEF_NAMED_QUERIES",Integer.class);
		LogUtil.show(config.getSingleResult());
		System.out.println("请拔掉网线。");
		ThreadUtils.doSleep(4000);
		try{
			LogUtil.show(config.getSingleResult());	
		}catch(Exception e){
			System.out.println("查询失败。");	
		}
		System.out.println("请插上网线。");
		ThreadUtils.doSleep(4000);
		LogUtil.show(config.getSingleResult());
	}
	
	private static class Readthread extends Thread{
		private IConnection conn;
		
		@Override
		public void run() {
			try{
				for(int i=0;i<20;i++){
					doReadTasdk();	
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		private void doReadTasdk() throws SQLException {
			System.out.println("[查询线程]操作开始");
			PreparedStatement st=conn.prepareStatement("select * from test_entity");
			ResultSet rs=st.executeQuery();
			int n=0;
			while(rs.next()){
				n++;
				rs.getObject(1);
				rs.getObject(2);
				
				if(n%500==0){
					ThreadUtils.doSleep(50);
					System.out.println("目前结果遍历已经达到"+n+"次");
				}
			}
			DbUtils.close(rs);
			DbUtils.close(st);
			System.out.println("结果遍历完成:共计"+n);
		}
		
	}
	private static class TransactiionThread extends Thread{
		private IConnection conn;
		@Override
		public void run() {
			try{
				doTransactionTasdk();	
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		private void doTransactionTasdk() throws SQLException {
			System.out.println("[事务]操作开始");
			conn.setAutoCommit(false);
//			System.out.println("[事务]===2===");
			for(int i=0;i<100;i++){
				doInsert(i);
				doSelect(i);
				doUpdate(i);
				conn.commit();
				System.out.println("[事务]第"+i+"次事务完成");
//				ThreadUtils.doSleep(400);
			}
			System.out.println("[事务]事务操作全部完成。");
		}

		
		private void doUpdate(int i) {
			//做一次更新操作
			PreparedStatement st=null;
			try{
				st=conn.prepareStatement("update child set parentid=? where id=?");
				st.setInt(1, i);
				st.setInt(2, 1);
				int n=st.executeUpdate();
			}catch(SQLException e){
				e.printStackTrace();
			}finally{
				DbUtils.close(st);
			}
		}

		private void doSelect(int i) {
			PreparedStatement st=null;
			//做一次查询操作
			ResultSet rs=null;
			try{
				st=conn.prepareStatement("select * from child where id=?");
				st.setInt(1, 1);
				rs=st.executeQuery();
				int count=0;
				while(rs.next()){
					count++;
				}
//				if(count!=1){
//					System.out.println("[事务]问题？没有查到记录"+count);//整个处理过程中，id是不变的因此查不到是不正常的
//				}
			}catch(SQLException e){
				e.printStackTrace();
			}finally{
				DbUtils.close(rs);
				DbUtils.close(st);
			}
		}

		private void doInsert(int i) {
			//做一次插入操作
			PreparedStatement st=null;
			try{
				st=conn.prepareStatement("insert into B(ID,NAME) values (?,?)");
				st.setInt(1, 100+i+RandomData.randomInteger(1, 10000));
				st.setString(2, RandomData.randomChineseName());
				int n=st.executeUpdate();
			}catch(SQLException e){
				e.printStackTrace();
			}finally{
				DbUtils.close(st);
			}
		}
		
	}
	
	/**
	 * TODO 测试太耗时
	 * @throws SQLException
	 */
	@Test
	@Ignore
	public void testConnectionPoolMultiThread() throws SQLException{
		System.out.println("进程号:"+ ProcessUtil.getPid());
		
		IConnection conn=DebugUtil.getConnection(db.getSqlTemplate(null));
		db.executeSql("delete from B");
		
		////////////////////////////////////////////
		Readthread t1=new Readthread();
		t1.setName("测试读取线程");
		t1.conn=conn;
		conn.setAutoCommit(true);
		
		TransactiionThread t2=new TransactiionThread();
		
//		TransactiionThread t3=new TransactiionThread();
		
		t2.setName("事务线程1");
		t2.conn=conn;
		t1.start();
		ThreadUtils.doSleep(200);
		t2.start();
//		t3.start();
		
		
		int n=0;
		for(int i=0;i<50;i++){
			Statement st=conn.createStatement();
			ResultSet rs=null;
			try{
				rs=st.executeQuery("select 1 from dual");
			}catch(SQLException e){
				e.printStackTrace();
			}finally{
				DbUtils.close(rs);
				DbUtils.close(st);
			}
			n++;
			if(n%10==0){
				System.out.println("心跳已经执行"+n+"次");
			}
			ThreadUtils.doSleep(100);
		}
		System.out.println("主线程执行完毕,总共"+n+"次心跳检测完成。");
	}
}
