package jef.database.innerpool;

import java.sql.SQLException;

public class ReconnectException extends SQLException{
	private static final long serialVersionUID = 1L;

	/**
	 * 阻止填充异常堆栈。减少日志处理信息
	 */
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
}
