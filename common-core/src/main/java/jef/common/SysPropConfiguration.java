package jef.common;

public class SysPropConfiguration extends Cfg{
	@Override
	protected String get(String key, String string) {
		String s=System.getProperty(key);
		if(s==null)return string;
		return s;
	}
}
