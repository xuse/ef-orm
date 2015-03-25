package jef.json;

import java.lang.reflect.Type;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;

/**
 * 为了在FastJson中实现一个类似于Google Gson的TypeAdapter这样的东西抽象类，
 * 
 * 
 * 首先要明确fastjson和google gson反序列化最大的不同。
 * <li>fast-json: string -> java bean</li>
 * <li>google gson: string -> json结构 -> java bean</li><br />
 * <p>
 * 在gson中，自定义反序列化器其实是指定义从json结构到java bean的拼装逻辑,而不是字符串解析逻辑。因此gson的自定义反序列化器写法要简单得多。（性能差一点）
 * 但是我认为fast-json主要是通过硬编码来解决反射开销、遍历字段开销。如果用户要扩展json序列化和反序列化逻辑而造成的算法开销是值得付出的，因此做这样的扩展是有价值的。
 * <br/>
 * <p>
 * 由于fastJson的json结构描述不是像gson那样的单根结构，json primitive和json null都是非包装的。 因此一个Json value只能表述为Object。
 * 为了避免用户直接操作Object做反序列化，这里将反序列化逻辑拆为四个方法。
 * <li>{@link #processArray(ParserConfig, JSONArray)}</li>
 * <li>{@link #processObject(ParserConfig, JSONObject)}</li> 
 * <li>{@link #processNull(ParserConfig)}</li>
 * <li>{@link #processPrimitive(ParserConfig, Object)}</li>
 * <p>
 * <code><pre>
 * 
 * </pre></code>
 * 
 * 
 * 
 * @author jiyi
 *
 * @param <T>
 */
public abstract class JsonTypeDeserializer<T> implements ObjectDeserializer{
	@SuppressWarnings("unchecked")
	public final <X> X deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        Object obj=parser.parse();
        ParserConfig config=parser.getConfig();
        return (X)process(config,obj);
	}
	
	/**
	 * 将JSON结构拼装为java bean
	 * @param config
	 * @param jsonStruct
	 * @return
	 */
	public final T process(ParserConfig config,Object jsonStruct){
		if(jsonStruct==null){
        	return processNull(config);
        }else if(jsonStruct instanceof JSONObject){
        	return processObject(config,(JSONObject)jsonStruct);
        }else if(jsonStruct instanceof JSONArray){
        	return processArray(config,(JSONArray)jsonStruct);
        }else{
        	return processPrimitive(config,jsonStruct);
        }
	}
	/**
	 * 根据JSONObject返回java bean
	 * @param config
	 * @param obj
	 * @return
	 */
	public abstract T processObject(ParserConfig config,JSONObject obj) ;
	/**
	 * 根据JSONArray返回java bean
	 * @param config
	 * @param obj
	 * @return
	 */
	public T processArray(ParserConfig config,JSONArray obj) {
		return null;
	}
	/**
	 * 根据json结构中的String number date等基础数值返回java bean
	 * @param config
	 * @param obj
	 * @return
	 */
	public T processPrimitive(ParserConfig config,Object obj){
		return null;
	}
	/**
	 * 根据json结构中的null数据返回java bean
	 * @param config
	 * @return
	 */
	public T processNull(ParserConfig config){
		return null;
	}

	public int getFastMatchToken() {
		return '{';
	}

}
