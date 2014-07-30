package jef.tools.management;

/**
 * SUN JDK下的Term信号量处理器
 * @author Administrator
 *
 */
public class SunJdkTERMHandler extends AbstractSunJdkSignalHandler implements TermHandler{
	private int exitStatus = 0;

	public void setDoNotExit() {
		exitStatus = -1;
	}

	public int getExitStatus() {
		return exitStatus;
	}

	public void setExitStatus(int exitStatus) {
		this.exitStatus = exitStatus;
	}

	@Override
	protected void processAfter() {
		if(exitStatus>-1){
			System.exit(exitStatus);
		}
	}

	public void activate() {
		regist("TERM");
	}
}
