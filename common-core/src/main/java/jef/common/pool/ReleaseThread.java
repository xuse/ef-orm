package jef.common.pool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jef.tools.Exceptions;

/**
 * 对注册进来的池进行定期释放的线程
 * @author jiyi
 *
 */
public class ReleaseThread extends Thread {
	private static Logger log = LoggerFactory.getLogger(ReleaseThread.class);
	
	private static ReleaseThread prt;
	
	private static final Map<Long,ReleaseThread> threads=new HashMap<Long,ReleaseThread>();
	
	private boolean alive = true;

	private long sleep;

	private final ConcurrentLinkedQueue<Releasable> pools = new ConcurrentLinkedQueue<Releasable>();
	
	ReleaseThread(long interval){
		super();
		setDaemon(true);
		setName("Pool Release Thread");
		this.sleep=interval;
	}

	public void addPool(Releasable ip) {
		pools.add(ip);
		if (!isAlive() && alive) {
			start();
		}
	}

	public void removePool(Releasable ip) {
		logPoolStatic(ip);
		pools.remove(ip);
	}


	public static ReleaseThread getInstance(long interval) {
		ReleaseThread r=threads.get(interval);
		if(r==null){
			r=create(interval);
		}
		return r;
	}

	public void close() {
		this.alive = false;
	}

	@Override
	public void run() {
		while (alive) {
			work();
			try{
				Thread.sleep(sleep);
			}catch(InterruptedException e){
				Exceptions.log(e);
			}
		}
	}

	private void work() {
		for (Releasable pool : pools) {
			try {
				pool.releaseTillMinSize();
			} catch (Exception e) {
				log.error("release connecton pool {} error", pool, e);
			}
		}
	}
	
	private synchronized static ReleaseThread create(long interval) {
		ReleaseThread r=threads.get(interval);
		if(r==null){
			r=new ReleaseThread(interval);
			if(prt==null){
				prt=r;
			}
		}
		return r;
	}

	public static ReleaseThread getInstance() {
		if(prt==null){
			return getInstance(3000);
		}
		return prt;
	}
	
	
	/**
	 * 正常情況下，關閉時兩者應該相等
	 * @param pollCount
	 * @param offerCount
	 */
	static void logPoolStatic(Releasable re){
		PoolStatus status=re.getStatus();
		log.info("The pool {} poll-count:{} offer-count:{}",re,status.getPollCount(),status.getOfferCount());
	}
}
