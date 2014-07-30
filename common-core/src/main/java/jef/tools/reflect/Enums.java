package jef.tools.reflect;


/**
 * 自己写的Enum工具，Guava的实现也看了，总觉他搞复杂了，0.3us~0.8us 的一次转换操作被他多搞出一个Optional对象来，
 * 
 * Guava：
 * <code>Item i=Enums.getIfPresent(Item.class, "HTTP_TIMEOUT").orNull();</code>
 * Consume 962ns 
 * Jef:
 * <code>Item i=jef.tools.Enums.valueOf(Item.class, "HTTP_TIMEOUT", null);</code>
 * Consume 321ns
 * 
 * 感觉Guava为了让编程符合自然语言习惯已经有点走火入魔了。 
 * @author jiyi
 *
 */
public final class Enums {
	private Enums(){}
	
	/**
	 * get the enum value. or return the defaultValue if absent.
	 * @param <T>
	 * @param clz
	 * @param value
	 * @param defaultValue
	 * @return the enum value. or return the defaultValue if absent.
	 */
	public static <T extends Enum<T>> T valueOf(Class<T> clz,String value,T defaultValue){
		try{
			return Enum.valueOf(clz, value);
		}catch(IllegalArgumentException e){
			return defaultValue;
		}
	}
	
	/**
	 *  get the enum value. or throw exception if the name not exist.
	 * @param <T>
	 * @param clz
	 * @param value
	 * @param exceptionMessage 异常消息模板，用 %s来标记传入的value
	 * @return
	 */
	public static <T extends Enum<T>> T valueOf(Class<T> clz,String value,String exceptionMessage,Object... params){
		try{
			return Enum.valueOf(clz, value);
		}catch(IllegalArgumentException e){
			throw new IllegalArgumentException(String.format(exceptionMessage, params));
		}
	}

}
