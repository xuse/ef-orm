package jef.common;

import jef.common.Configuration.ConfigItem;
import jef.common.log.LogUtil;
import jef.tools.StringUtils;

/**
 * 所有配置类的基类
 * @author Administrator
 *
 */
public abstract class Cfg {
	static class Config implements ConfigItem{
		String key;
		public Config(String key){
			this.key=key;
		}
		@Override
		public String toString() {
			return key;
		}
		public String name() {
			return key;
		}
	}
	
	/**
	 * 返回一个指定键值的ConfigItem
	 * @param key
	 * @return
	 */
	public static ConfigItem valueOf(String key){
		return new Config(key);
	}
	
	/**
	 * 得到布尔值
	 * @param itemkey
	 * @param defaultValue
	 * @return
	 */
	public boolean getBoolean(ConfigItem itemkey, boolean defaultValue) {
		String s = get(itemkey);
		return StringUtils.toBoolean(s, defaultValue);
	}
	/**
	 * 得到double值
	 * @param itemkey
	 * @param defaultValue
	 * @return
	 */
	public double getDouble(ConfigItem itemkey, double defaultValue) {
		String s = get(itemkey);
		try {
			double n = Double.parseDouble(s);
			return n;
		} catch (Exception e) {
			LogUtil.warn("the jef config ["+itemkey+"] has invalid value:"+ s);
			return defaultValue;
		}
	}


	/**
	 * 得到int值
	 * @param itemkey
	 * @param defaultValue
	 * @return
	 */
	public int getInt(ConfigItem itemkey, int defaultValue) {
		String s = get(itemkey);
		try {
			int n = Integer.parseInt(s);
			return n;
		} catch (Exception e) {
			LogUtil.warn("the jef config ["+itemkey+"] has invalid value:"+ s);
			return defaultValue;
		}
	}
	
	/**
	 * 得到String，如果没有返回""
	 * @param itemkey
	 * @return
	 */
	public String get(ConfigItem itemkey) {
		String key;
		if(itemkey instanceof Enum){
			key=((Enum<?>) itemkey).name();
			key = StringUtils.replaceChars(key, "_$", ".-").toLowerCase();
		}else{
			key=itemkey.toString();
		}
		return get(key, "");
	}
	
	public String get(ConfigItem itemkey, String defaultValue) {
		String key = getKey(itemkey);
		return get(key,defaultValue);
	}
	
	static public String getKey(ConfigItem itemkey){
		return StringUtils.replaceChars(itemkey.toString(), "_$", ".-").toLowerCase();
	}

	/**
	 * 
	 * @param key
	 * @param string
	 * @return
	 */
	protected abstract String get(String key, String string);
}
