package jef.database.innerpool;

import java.util.concurrent.ConcurrentLinkedQueue;

import jef.common.log.LogUtil;
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

	/**
	 * 将连接池添加到心跳线程任务队列中
	 * 
	 * @param pool
	 */
	public void addPool(CheckablePool pool) {
		pools.add(pool);
		if (ORMConfig.getInstance().isDebugMode()) {
			LogUtil.show("The [" + pool.toString() + "] was added into PoolCheck task queue.");
		}
		synchronized (this) {
			if (alive && !isAlive()) {
				try {
					start();
				} catch (IllegalStateException e) {
					LogUtil.warn("Start check thread error.", e);
				}
			}
		}
	}

	/**
	 * 将连接池从心跳线程任务队列中移除
	 * 
	 * @param pool
	 * @return
	 */
	public boolean removePool(IPool<?> pool) {
		return pools.remove(pool);
	}

	/**
	 * 获得连接池心跳任务实例
	 * 
	 * @return 连接池心跳任务实例
	 */
	public static PoolCheckThread getInstance() {
		if (prt == null || prt.getState() == State.TERMINATED) {
			replaceInstance();
		}
		return prt;
	}

	private synchronized static void replaceInstance() {
		if (prt == null || prt.getState() == State.TERMINATED) {
			prt = new PoolCheckThread();
		}
	}

	/**
	 * 关闭心跳任务
	 */
	public void close() {
		this.alive = false;
	}

	@Override
	public void run() {
		ThreadUtils.doSleep(12000);
		try {
			while (alive) {
				long sleep = ORMConfig.getInstance().getHeartBeatSleep();
				if (sleep <= 0) {
					sleep = 120000; // 不作心跳，一分钟后再行动
				} else {
					for (CheckablePool pool : pools) {
						synchronized (pool) {
							pool.doCheck();
						}
					}
				}
				ThreadUtils.doSleep(sleep);
			}
			log.info("Thread {} was terminated.", getName());
		} catch (Exception e) {
			log.error("", e);
		}
	}
}
