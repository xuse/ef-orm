package jef.database.wrapper.executor;

import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import jef.tools.Assert;

public abstract class DbTask implements Runnable{
	private volatile Queue<SQLException> exceptions;
	private volatile Queue<Throwable> throwables;
	private volatile CountDownLatch latch;
	
	@Override
	public final void run() {
		try{
			execute();
		}catch(SQLException ex){
			exceptions.add(ex);
		}catch(Throwable t){
			throwables.add(t);
		}finally{
			latch.countDown();
			latch=null;
		}
	}

	public abstract void execute() throws SQLException;

	public final void prepare(CountDownLatch latch,Queue<SQLException> exceptions,Queue<Throwable> t) {
		Assert.isNull(this.latch);
		this.latch=latch;
		this.exceptions=exceptions;
		this.throwables=t;
	}

	public final Queue<SQLException> getExceptions() {
		return exceptions;
	}

	public final Queue<Throwable> getThrowables() {
		return throwables;
	}
}
