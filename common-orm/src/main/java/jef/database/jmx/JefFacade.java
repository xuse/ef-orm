package jef.database.jmx;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import jef.database.DbClient;
import jef.database.ORMConfig;
import jef.database.ORMConfigMBean;
import jef.database.jpa.JefEntityManagerFactory;
import jef.tools.jmx.JefMonitorRegister;

/**
 * 用于获得JEF的JMX控制Bean。可以收集到JEF的统计信息，以及修改相关配置
 * @author jiyi
 *
 */
public class JefFacade {
	/**
	 * ORM配置Bean
	 */
	private static ORMConfigMBean ormConfig;
	/**
	 * 所有DB
	 */
	private static final Map<DbClient,DbClientInfo> emfMap=new IdentityHashMap<DbClient, DbClientInfo>(8);
	/**
	 * 记录所有的db和emf
	 * @param db
	 * @param emf
	 */
	public static synchronized void registeEmf(DbClient db,JefEntityManagerFactory emf){
		DbClientInfo stat=emfMap.get(db);
		if(stat==null){
			stat=new DbClientInfo(db);
			stat.setDbClientFactory(emf);
			emfMap.put(db, stat);
			if(JefMonitorRegister.isJmxEnable()){
				JefMonitorRegister.registe("JefDbClient:key=Db@"+Integer.toHexString(db.hashCode()), stat);
			}
		}else{
			stat.setDbClientFactory(emf);
		}
		initOrmConfig();
	}
	
	private static void initOrmConfig() {
		if(ormConfig==null){
			ormConfig=ORMConfig.getInstance();
			JefMonitorRegister.registe("JefOrmConfig:key=global",ormConfig);
		}
	}

	/**
	 * 注销EMF
	 * @param db
	 */
	public static synchronized void unregisteEmf(DbClient db){
		DbClientInfo bean=emfMap.remove(db);
		if(JefMonitorRegister.isJmxEnable()){
			JefMonitorRegister.unregiste("JefDbClient:key=Db@"+Integer.toHexString(db.hashCode()), bean);
		}
	}
	
	/**
	 * 获得所有记录的db和emf
	 * @return
	 */
	public static Map<DbClient,DbClientInfo> getAll(){
		return Collections.unmodifiableMap(emfMap);
	}

	/**
	 * 获得ORM配置 MBean
	 * @return
	 */
	public static ORMConfigMBean getOrmConfig() {
		initOrmConfig();
		return ormConfig;
	}
}
