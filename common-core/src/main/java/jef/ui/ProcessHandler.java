package jef.ui;

/**
 * 进程操作句柄
 * @author jiyi
 *
 */
public interface ProcessHandler {
	/**
	 * 向进程控制台发送命令
	 * @param command
	 */
	void sendCommand(String command);

	/**
	 * 等待进程正常退出
	 * @return
	 */
	int waitfor();

//	/**
//	 * 判断进程是否还在执行
//	 * @return
//	 */
//	boolean isAlive();

	/**
	 * 杀掉这个进程
	 */
	void kill();

}