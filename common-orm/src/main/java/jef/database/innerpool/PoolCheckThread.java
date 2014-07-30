package jef.database.innerpool;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

import jef.common.log.LogUtil;
import jef.database.ORMConfig;
import jef.tools.ThreadUtils;

import org.apache.commons.lang.StringUtils;
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
						doCheck(pool);
					}	
				}else{
					sleep=60000; //禁用空闲检测功能，一分钟后再次检查是否启用
				}
				ThreadUtils.doSleep(sleep);
			}
			log.info("Thread {} was terminated.", getName());
		} catch (Exception e) {
			log.error("", e);
		}
	}

	/**
	 * 立刻检查
	 * 
	 * @param pool
	 */
	public void doCheck(CheckablePool pool) {
		int total = 0;
		int invalid = 0;
		String testSql = pool.getTestSQL();
		boolean useJDbcValidation = false;
		if (StringUtils.isBlank(testSql) || "jdbc4".equals(testSql)) {
			useJDbcValidation = true;
		}else if("false".equalsIgnoreCase(testSql) || "disable".equalsIgnoreCase(testSql)){
			return;
		}
		synchronized (pool) {
			for (CheckableConnection conn : pool.getConnectionsToCheck()) {
				total++;
				if (!conn.isUsed()) {// 仅对空闲连接进行检查
					boolean flag = false;
					try {
						if (useJDbcValidation) {
							try{
								flag = conn.checkValid(5);
							}catch(AbstractMethodError e){ //JDBC未实现此方法
								LogUtil.exception(e);
								LogUtil.warn("The Connection Check was disabled since the JDBC Driver doesn't support 'isValid(I)Z'");
								pool.setTestSQL("false");
								return;
							}
						} else {
							flag = conn.checkValid(testSql);
						}
					} catch (SQLException e) {
						LogUtil.exception(e);
					}
					if (!flag) {
						conn.setInvalid();
						invalid++;
					}
				}
			}
			LogUtil.info("Checked [{}]. total:{},  invalid:{}", pool, total, invalid);
		}
	}
}
