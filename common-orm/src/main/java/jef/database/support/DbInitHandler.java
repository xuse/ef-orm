package jef.database.support;

import jef.database.DbClient;

/**
 * 抽象类，用户可以继承这个接口，从而在数据库刚刚初始化时执行某些任务。
 * @author jiyi
 *
 */
public interface DbInitHandler {
	
	/**
	 * 用户自行实现，用来在数据库初始化的时候干一点事。
	 * @param db
	 */
	public abstract void doDatabaseInit(DbClient db);
	

}
