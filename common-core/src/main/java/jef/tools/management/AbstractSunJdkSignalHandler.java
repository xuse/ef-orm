package jef.tools.management;

import jef.common.Callback;
import jef.common.log.LogUtil;
import jef.tools.Exceptions;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * 专门处理SUN JDK下的信号量的类，只能在SUN JDK下编译通过
 * 
 * @author Administrator
 *
 */
abstract class AbstractSunJdkSignalHandler extends SignalEvents implements SignalHandler {
	public void handle(Signal arg0) {
		for (Callback<Integer, Exception> c : events) {
			try {
				c.call(arg0.getNumber());
			} catch (Exception e) {
				Exceptions.log(e);
			}
		}
		processAfter();
	}

	protected void processAfter() {
	}

	protected void regist(String signalName) {
		Signal sig = new Signal(signalName);
		try {
			Signal.handle(sig, this);
			LogUtil.info("Registe {} signal success!", signalName);
		} catch (Throwable t) {
			Exceptions.illegalState("Registe " + signalName + " Signal error!", t);
		}
	}
}
