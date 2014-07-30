package jef.database;

import java.sql.SQLException;

/**
 * A Sequence to implements Auto Increament
 * @author jiyi
 *
 */
public interface Sequence {
	/**
	 * 将一个用过的Sequence序列归还到缓存中
	 * @param key
	 */
	public void pushBack(long key);

	/**
	 * 获得下一个数据库的Sequence
	 * @param conn
	 * @return
	 */
	public long next();

	/**
	 * 清除Sequence中的缓存
	 */
	public void clear();
	
	/**
	 * 如果是用Table模拟的Sequence，返回true
	 * @return
	 */
	public boolean isTable();
	
	/**
	 * 如果是数据库原生Sequence,并且没有启用hilo、步长也为1的时候。
	 * @return
	 */
	boolean isRawNative();
	
	
	/**
	 * 返回表的名称或Sequence名称
	 * @return
	 */
	public String getName();
	
	/**
	 * 检查Sequence中的下一个值大于表中的最大值
	 * @param table
	 * @param columnName
	 */
	boolean checkSequenceValue(String table,String columnName) throws SQLException;
}
