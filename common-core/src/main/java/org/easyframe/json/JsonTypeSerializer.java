package org.easyframe.json;

import java.io.IOException;
import java.lang.reflect.Type;

import org.easyframe.fastjson.serializer.JSONSerializer;
import org.easyframe.fastjson.serializer.ObjectSerializer;
import org.easyframe.fastjson.serializer.SerialContext;
import org.easyframe.fastjson.serializer.SerializeWriter;

/**
 * 为了在FastJson中实现一个类似于Google Gson的TypeAdapter这样的东西抽象类，
 * 和过滤器类似，让用户自行编码将要序列化的对象编码成一个其他的东西，然后序列化。最常见的办法是返回一个JSONObject或者JSONArray(和gson一样的处理办法)。
 * 这个工具类可以方便将gson的逻辑移植到fastjson上来。
 * 
 * 
 * 
 * 
 * 
 * 遗留问题：全序列化场合下，记录的classname不对。这个问题暂不考虑。需要的时候抄一下JavaBeanSerializer的代码再该写一下就可以了。
 * @author jiyi
 *
 * @param <T>
 */
public abstract class JsonTypeSerializer<T> implements ObjectSerializer{
	@SuppressWarnings("unchecked")
	public final void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType,int features) throws IOException {
		SerializeWriter out=serializer.getWriter();
		if (object == null) {
			out.writeNull();
			return;
		}

		if (serializer.containsReference(object)) {
			serializer.writeReference(object);
			return;
		}

        SerialContext parent = serializer.getContext();
        serializer.setContext(parent, object, fieldName,0);
        serializer.write(processToJson((T)object));
	}

	protected abstract Object processToJson(T t);
}
