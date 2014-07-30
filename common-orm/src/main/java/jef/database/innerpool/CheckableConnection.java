package jef.database.innerpool;

import java.sql.SQLException;

/**
 * 描述连接可被检查的行为特性
 * @author jiyi
 *
 */
public interface CheckableConnection extends IConnection{
	/**
	 * 标记当前连接失效，在业务发生错误时，或者在检查线程检查出问题时，都可能使用此方法来标记连接失效。
	 */
	public void setInvalid();
	/**
	 * 执行检查，不能抛出任何异常
	 * @param 测试用SQL
	 * @return
	 */
	public boolean checkValid(String testSql)throws SQLException;
	
	/**
	 * 执行检查，原先是isValid方法，但是该方法与JDBC4同名方法一致，如果一个类同时实现了两个接口。javac在编译时会出现委派不确定错误，因此更名为checkValid
	 * @param timeout
	 * @return
	 */
	public boolean checkValid(int timeout)throws SQLException;
}
