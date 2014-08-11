package jef.database.innerpool;

import java.util.concurrent.ConcurrentLinkedQueue;

import jef.database.ORMConfig;
import jef.tools.ThreadUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局唯一的连接检查器
 * 
 * @author jiyi
 * 
 */
final class PoolCheckThread extends Thread {
	private static PoolCheckThread prt = new PoolCheckThread();
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private boolean alive = true;
	private final ConcurrentLinkedQueue<CheckablePool> pools = new ConcurrentLinkedQueue<CheckablePool>();

	private PoolCheckThread() {
		super();
		setName("thread-JEFPoolChecker");
		setDaemon(true);
	}

	public void addPool(CheckablePool ip) {
		pools.add(ip);
		if (alive && !isAlive() && getState() != State.TERMINATED) {
			start();
		}
	}

	public void removePool(IPool<?> ip) {
		pools.remove(ip);
	}

	public static PoolCheckThread getInstance() {
		return prt;
	}

	public void close() {
		this.alive = false;
	}

	@Override
	public void run() {
		ThreadUtils.doSleep(12000);
		try {
			while (alive) {
				long sleep=ORMConfig.getInstance().getHeartBeatSleep();
				if(sleep==0){
					alive=false;
				}else if(sleep>0){
					for (CheckablePool pool : pools) {
						pool.doCheck();
					}	
				}else{
					sleep=12000; //禁用空闲检测功能，2分钟后再次检查是否启用
				}
				ThreadUtils.doSleep(sleep);
			}
			log.info("Thread {} was terminated.", getName());
		} catch (Exception e) {
			log.error("", e);
		}
	}
}
