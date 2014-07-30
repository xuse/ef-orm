package org.easyframe.json;

import java.util.HashMap;
import java.util.Map;

import org.easyframe.fastjson.serializer.SerializeConfig;

/**
 * 每个{@link SerializeConfig}持有着动态类的classloader，因此每个config管理着若干序列化和反序列化器。
 * 如果要对一个bean实现不同的反序列化策略，可以考虑使用不同的SerializeConfig。
 * 但是自定义bean的缺省ASM序列化器不能重用，这也不好。每次新建的配置都会重用目前已经注册的位于全局config中的序列化反序列化器
 * 
 * 
 * @author jiyi
 *
 */
public final class ConfigManager {
	static Map<String,SerializeConfig> typeKeys=new HashMap<String,SerializeConfig>();
	
	public static SerializeConfig getGlobal(){
		return SerializeConfig.getGlobalInstance();
	}
	
	public static SerializeConfig get(String key){
		SerializeConfig config=typeKeys.get(key);
		if(config==null){
			synchronized (typeKeys) {
				Map<String,SerializeConfig> c=new HashMap<String, SerializeConfig>(typeKeys);
				config=new SerializeConfig(SerializeConfig.getGlobalInstance());
				config.setTypeKey(key);
				c.put(key, config);
				typeKeys=c;	
			}
		}
		return config;
	}
	
	public static void clear(){
		typeKeys.clear();
	}
	
	public static void remove(String key){
		typeKeys.remove(key);
	}
}
