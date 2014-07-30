package jef.database.misc;

import jef.database.DbClient;
import jef.tools.ThreadUtils;

public class JMXTest {
	public static void main(String[] args) {
		DbClient db=new DbClient();
		ThreadUtils.doSleep(1000000);
	}
}
