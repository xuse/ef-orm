package jef.tools.management;

import jef.common.Callback;

/**
 * 捕捉和处理TERM信号量的句柄
 * @author Administrator
 *
 */
public interface TermHandler {
	public void setDoNotExit();

	public int getExitStatus();

	public void setExitStatus(int exitStatus);
	
	public void addEvent(Callback<Integer,Exception> event);
	
	public void activate();
}
