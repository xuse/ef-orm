package jef.database.wrapper.executor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.DebugUtil;
import jef.database.ORMConfig;
import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.database.jdbc.result.ResultSetWrapper;
import jef.database.support.SqlLog;
import jef.database.wrapper.processor.BindVariableContext;
import jef.database.wrapper.processor.BindVariableTool;
import jef.tools.StringUtils;
import jef.tools.ThreadUtils;

/**
 * 在新的线程中运行若干DDL语句的执行器。 之所以要用新线程，是为了方式被JTA事务所包含。
 * 
 * 注意！一定要在finally中关闭 DDLExecutor executor=createExecutor(); try{ //work()
 * xxxxxxxx }finally{ executor.close(); }
 * 
 * @author jiyi
 * 
 */
public class ExecutorJTAImpl implements Runnable, StatementExecutor {

	IConnection conn;
	Statement st;

	private IUserManagedPool parent;
	private String dbkey;
	private String txId;
	private DatabaseDialect profile;

	/**
	 * 构造并在新线程中创建连接和Statement
	 * 
	 * @param parent
	 * @param dbkey
	 * @param txId
	 */
	public ExecutorJTAImpl(IUserManagedPool parent, String dbkey, String txId) {
		LogUtil.debug("The sqlExecutor {} was starting.",this);
		this.parent = parent;
		this.dbkey = dbkey;
		this.txId = txId;
		this.cl = new CountDownLatch(1); // 初始化检测器
		this.profile = parent.getProfile(dbkey);
		DbUtils.es.execute(this);
		ThreadUtils.await(cl);// 等待连接在新线程中初始化完成后，构造方法才退出。
		cl = null;
		// 构造方法退出后，可以用isReady检测Executor是否处于可用状态
		if (exception != null) {
			// 如果在初始化过程中失败，则构造方法直接抛出异常。
			throw new PersistenceException(exception);
		}
		if (!isReady()) {
			throw new IllegalStateException("Not ready");
		}
	}

	/**
	 * 当前的SQL任务
	 */
	private final BlockingQueue<String> sqltask = new ArrayBlockingQueue<String>(10);
	private volatile CountDownLatch cl;

	/**
	 * 记录当前出现的异常
	 */
	private SQLException exception;
	/**
	 * 记录当前执行器是否关闭
	 */
	private volatile boolean close;

	public boolean isReady() {
		return st != null;
	}

	@Override
	public void run() {
		try {
			if (init()) {
				try {
					execute();
				} finally {
					DbUtils.close(st);
					DbUtils.closeConnection(conn);
					LogUtil.debug("The sqlExecutor {} was finished. connection was released.",this);
				}
			}
		} catch (Throwable e) {
			LogUtil.exception(e);
		}
	}

	private boolean init() {
		try {
			conn = parent.poll();
			conn.setKey(dbkey);
//			if(!conn.get AutoCommit()){
//				conn.set AutoCommit(true);
//			}
			st = conn.createStatement();
			return true;
		} catch (SQLException e) {
			exception = e;
			DbUtils.closeConnection(conn);// If error at create statement
											// then close connection.
			conn = null;
			return false;
		} finally {
			if (cl != null) {
				cl.countDown();
			}

		}
	}

	private void execute() {
		while (!close) {
			try {
				String sql = sqltask.take();
				if(StringUtils.isNotEmpty(sql)){
					doSql(st, txId, sql);
				}
			} catch (InterruptedException e) {
				LogUtil.exception(e);
			}finally{
				if(cl!=null){
					cl.countDown();
				}
			}
		}
	}

	private void doSql(Statement st, String txId, String sql) {
		this.exception = null;// 清理掉上次的异常记录，以免本次运行误判
		SqlLog log=ORMConfig.getInstance().newLogger(sql.length()+60);
		try {
			long start=System.currentTimeMillis();
			log.append(sql).append(" | ",txId);
			st.executeUpdate(sql);
			log.append("\nExecuted: ",System.currentTimeMillis()-start).append("ms");
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, sql);
			this.exception = e;
		}finally{
			log.output();
		}
	}

	/**
	 * 执行的DDL语句 同步方式
	 * 
	 * @param ddls
	 * @throws SQLException
	 */
	public void executeSql(String... ddls) throws SQLException {
		if (this.cl != null) {
			throw new IllegalStateException("There's sql executing now...");
		}
		this.cl = new CountDownLatch(ddls.length);
		for(String s:ddls){
			sqltask.add(s);
		}
		boolean flag = ThreadUtils.await(cl, 60000);
		cl=null;//取消占用
		if (!flag) { // 运行超时就抛出异常
			throw new IllegalStateException("the countdownlatch not return until timeout." + Arrays.toString(ddls));
		}
		if (exception != null) {
			throw exception;
		}
	}

	/**
	 * 执行执行的DDL语句 同步方式
	 * 
	 * @param ddls
	 * @throws SQLException
	 */
	public void executeSql(List<String> ddls) throws SQLException {
		executeSql(ddls.toArray(new String[ddls.size()]));
	}


	public SQLException getException() {
		return exception;
	}

	/**
	 * 关闭运行器
	 */
	public void close() {
		if (!sqltask.isEmpty() ) {
			throw new IllegalStateException("There's sql executing now...");
		}
		this.close = true;
		this.sqltask.add("");
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		st.setQueryTimeout(seconds);
	}

	@Override
	public ResultSet executeQuery(String sql, Object... params) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(sql);
		for (int i = 0; i < params.length; i++) {
			ps.setObject(i + 1, params[i]);
		}
		ResultSet rs = ps.executeQuery();
		return new ResultSetWrapper(null, ps, rs);
	}

	public int executeUpdate(String sql, Object... params) throws SQLException {
		SqlLog sb = ORMConfig.getInstance().newLogger().append(sql).append("\t|",txId);
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			if (params.length > 0) {
				BindVariableContext context = new BindVariableContext(ps, profile, sb);
				BindVariableTool.setVariables(context, Arrays.asList(params));
			}
			int total = ps.executeUpdate();
			sb.append("\nExecuted:", total).append("\t |", txId);
			return total;
		} finally {
			sb.output();
			DbUtils.close(ps);
		}
	}
}
