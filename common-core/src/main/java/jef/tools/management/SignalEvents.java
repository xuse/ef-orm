package jef.tools.management;

import java.util.ArrayList;
import java.util.List;

import jef.common.Callback;

/**
 * 信号量操作基类，需要根据不同版本的JDK便写不同的实现
 * @author Administrator
 *
 */
abstract class SignalEvents{
	 protected List<Callback<Integer,Exception>> events=new ArrayList<Callback<Integer,Exception>>();
	
	public void addEvent(Callback<Integer,Exception> event){
		events.add(event);
	}
}
