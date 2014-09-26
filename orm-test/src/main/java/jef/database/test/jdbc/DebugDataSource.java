package jef.database.test.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import jef.database.datasource.AbstractDataSource;


/**
 * 用来进行测试的数据源，目的是分析在连接上执行的各项操作是否合法。
 * 
 * 结果集关闭，连接关闭等是否正确。
 * 
 * 如果框架操作不正确，就会出现事务未关闭、资源未释放等问题，因此用这样一个驱动来检查
 * 
 * @author jiyi
 *
 */
public class DebugDataSource extends AbstractDataSource{
	private DataSource ds;
	public final Map<OperType,AtomicLong> count=new IdentityHashMap<OperType,AtomicLong>();
	public static final Map<OperType,Boolean> printStackCfg=new HashMap<OperType,Boolean>();
	static{
		printStackCfg.put(OperType.create_connection, true);
		printStackCfg.put(OperType.close_connection, true);
		
		printStackCfg.put(OperType.preapre_statement, true);
		printStackCfg.put(OperType.close_prepared_statement, true);
		
		printStackCfg.put(OperType.create_statement, true);
		printStackCfg.put(OperType.close_statement, true);
		
		printStackCfg.put(OperType.create_resultSet, true);
		printStackCfg.put(OperType.close_resultSet, true);

		printStackCfg.put(OperType.set_autocommit_false, true);
		printStackCfg.put(OperType.commit, true);
		printStackCfg.put(OperType.rollback, true);
		printStackCfg.put(OperType.set_autocommit_true, true);
		
		printStackCfg.put(OperType.set_connection_attr, true);
		printStackCfg.put(OperType.set_statement_attr, false);
		printStackCfg.put(OperType.set_resultset_attr, false);
		
		printStackCfg.put(OperType.execute_batch, true);
		printStackCfg.put(OperType.execute_update, true);
	}
	
	
	/**
	 * 构造
	 * @param ds
	 */
	public DebugDataSource(DataSource ds) {
		this.ds=ds;
		initCounter();
	}
	

	private void initCounter() {
		count.put(OperType.create_connection, new AtomicLong());
		count.put(OperType.close_connection,  new AtomicLong());
		
		count.put(OperType.preapre_statement,  new AtomicLong());
		count.put(OperType.close_prepared_statement, new AtomicLong());
		
		count.put(OperType.create_statement,  new AtomicLong());
		count.put(OperType.close_statement,  new AtomicLong());
		
		count.put(OperType.create_resultSet,  new AtomicLong());
		count.put(OperType.close_resultSet,  new AtomicLong());

		count.put(OperType.set_autocommit_false,  new AtomicLong());
		count.put(OperType.commit,  new AtomicLong());
		count.put(OperType.rollback,  new AtomicLong());
		count.put(OperType.set_autocommit_true,  new AtomicLong());
		
		count.put(OperType.set_connection_attr,  new AtomicLong());
		count.put(OperType.set_statement_attr,  new AtomicLong());
		count.put(OperType.set_resultset_attr,  new AtomicLong());
		
		count.put(OperType.execute_batch,  new AtomicLong());
		count.put(OperType.execute_update,  new AtomicLong());	
	}


	/**
	 * 空构造
	 */
	public DebugDataSource() {
		super();
		initCounter();
	}

	/**
	 * 创建连接
	 */
	@Override
	public Connection getConnection() throws SQLException {
		Connection conn=ds.getConnection();
		event(OperType.create_connection,conn);
		return new DebugConnection(conn,this);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		Connection conn=ds.getConnection(username,password);
		event(OperType.create_connection, conn);
		return new DebugConnection(conn,this);
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return null;
	}

	public DataSource getDs() {
		return ds;
	}

	public void setDs(DataSource ds) {
		this.ds = ds;
	}
	public void event(OperType type,Object message) {
		//计数器累加
		count.get(type).incrementAndGet();
		StackTraceElement[] stacks=new Throwable().getStackTrace();

		System.out.println(">> "+type.name()+"=== "+message+" ===");
		Boolean printStack=printStackCfg.get(type);
		if(printStack==null || printStack){
			System.out.println(">>"+stacks[2]+"\n>>"+stacks[3]);
		}
	}


	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		checkCount();
	}


	private void checkCount() {
		List<String> errorMessages=new ArrayList<String>();
		//检查连接打开和关闭数量
		checkNumber(errorMessages,OperType.create_connection,OperType.close_connection);
		//检查PreparedStatement次数
		checkNumber(errorMessages,OperType.preapre_statement,OperType.close_prepared_statement);
		//检查Statement次数
		checkNumber(errorMessages,OperType.create_statement,OperType.close_statement);
		//检查结果集关闭数量
		checkNumber(errorMessages,OperType.create_resultSet,OperType.close_resultSet);
		//检查事务开启关闭次数
		checkNumber(errorMessages,OperType.set_autocommit_false,OperType.commit,OperType.rollback);
//		count.put(OperType.set_autocommit_true,  new AtomicLong());
//		count.put(OperType.set_connection_attr,  new AtomicLong());
//		count.put(OperType.execute_batch,  new AtomicLong());
//		count.put(OperType.execute_update,  new AtomicLong());	
		if(!errorMessages.isEmpty()){
			System.err.println("********************** ERROR JDBC OPERATE DETLECTED *************************");
			for(String s:errorMessages){
				System.err.println(s);
			}
			
		}
	}


	private void checkNumber(List<String> msgs,OperType t1, OperType t2) {
		long n1=count.get(t1).get();
		long n2=count.get(t2).get();
		if(n1!=n2){
			msgs.add(t1.name()+"["+n1+"]!="+t2.name()+"["+n2+"]");
		}
	}
	private void checkNumber(List<String> msgs,OperType t1, OperType t2,OperType t3) {
		long n1=count.get(t1).get();
		long n2=count.get(t2).get();
		long n3=count.get(t3).get();
		if(n1!=n2+n3){
			msgs.add(t1.name()+"["+n1+"]!="+t2.name()+"["+n2+"]+"+t3.name()+"["+n3+"]");
		}
	}
	
}

