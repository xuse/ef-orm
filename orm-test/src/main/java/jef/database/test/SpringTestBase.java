package jef.database.test;

import java.lang.reflect.Method;
import java.net.URL;

import jef.common.log.LogUtil;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public abstract class SpringTestBase extends org.junit.Assert {

	protected ApplicationContext initContext() {
		StackTraceElement stacktrace = new Throwable().getStackTrace()[1];
		Method m;
		try {
			Class<?> clz = Class.forName(stacktrace.getClassName());
			m = clz.getDeclaredMethod(stacktrace.getMethodName());
		} catch (Exception e) {
			throw new RuntimeException("请在合适的方法中调用:" + stacktrace + "不是合适的");
		}
		String caseName = m.getName();
		String name = this.getClass().getName().replace('.', '/') + "/" + caseName + ".xml";
		URL url = this.getClass().getClassLoader().getResource(name);
		if (url == null) {
			throw new RuntimeException("File classpath:" + name + " not exist!");
		}

		LogUtil.show("Loading context " + name + "...");
		return new ClassPathXmlApplicationContext(name);
	}

}
