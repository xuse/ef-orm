package jef.database.innerpool;

import java.util.concurrent.ConcurrentLinkedQueue;

import jef.common.pool.PoolStatus;
import jef.database.DbCfg;
import jef.tools.JefConfiguration;
import jef.tools.ThreadUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 全局唯一的池收缩器.每个
 * @author jiyi
 *
 */
public class PoolReleaseThread extends Thread{
	private static PoolReleaseThread prt=new PoolReleaseThread();
	
	private Logger log=LoggerFactory.getLogger(this.getClass());
	private boolean alive=true;
	private final ConcurrentLinkedQueue<IPool<?>> pools=new ConcurrentLinkedQueue<IPool<?>>();
	
	
	
	private PoolReleaseThread() {
		super();
		setDaemon(true);
	}

	public void addPool(IPool<?> ip){
		pools.add(ip);
		synchronized (this) {
			if(!isAlive() && alive){
				start();	
			}	
		}
	}
	
	public void removePool(IPool<?> ip){
		pools.remove(ip);
		
	}
	
	public static PoolReleaseThread getInstance(){
		return prt;
	}

	public void close(){
		this.alive=false;
	}
	
	@Override
	public void run() {
		long sleep=JefConfiguration.getLong(DbCfg.DB_CONNECTION_LIVE,60000);
		ThreadUtils.doSleep(sleep);
		try{
			while(alive){
				doWork();
				ThreadUtils.doSleep(sleep);
			}
		}catch(Exception e){
			log.error("",e);
		}
	}

	private void doWork() {
		for(IPool<?> pool:pools){
			try{
				pool.closeConnectionTillMin();
			}catch(Exception e){
				log.error("release connecton pool error",e);
			}
		}
	}
	
	/**
	 * this function is using for debug the connection pool.
	 * 
	 * it will check ths poll count and offer count of pools.
	 */
	public void assertPoolCount(){
		for(IPool<?> pool:pools){
			PoolStatus st=pool.getStatus();
			if(st.getPollCount()!=st.getOfferCount()){
				PoolService.logPoolStatic(pool.getClass().getSimpleName(), st.getPollCount(), st.getOfferCount());
				throw new IllegalStateException(st.toString());
			}
			
		}
	}
}
