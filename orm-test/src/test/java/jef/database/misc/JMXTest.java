package jef.database.misc;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.tools.ThreadUtils;

public class JMXTest {
	public static void main(String[] args) {
		DbClient db=new DbClientBuilder().setEnhancePackages("none").build();
		ThreadUtils.doSleep(1000000);
	}
}
