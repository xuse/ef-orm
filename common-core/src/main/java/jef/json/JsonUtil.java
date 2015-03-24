/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.json;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jef.tools.StringUtils;
import jef.tools.XMLFastJsonParser;
import jef.tools.reflect.GenericUtils;

import org.easyframe.fastjson.JSON;
import org.easyframe.fastjson.JSONArray;
import org.easyframe.fastjson.JSONObject;
import org.easyframe.fastjson.serializer.JSONSerializer;
import org.easyframe.fastjson.serializer.ObjectSerializer;
import org.easyframe.fastjson.serializer.SerializeConfig;
import org.easyframe.fastjson.serializer.SerializerFeature;
import org.easyframe.json.ConfigManager;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * 从Json序列化/反序列化的工具封装
 * 
 * @author Administrator
 * 
 */
public class JsonUtil {
	public static final String EMPTY_JSON = "{}";
	public static final String EMPTY_JSON_ARRAY = "[]";

	protected JsonUtil() {
	}

	/**
	 * 转换为String,String的map
	 * 
	 * @param data
	 * @return
	 */
	public static Map<String, String> toStringMap(String data) {
		return JSON.parseObject(data, GenericUtils.MAP_OF_STRING);
	}

	/**
	 * 转换为嵌套的复杂Map
	 * 
	 * @param data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> toMap(String data) {
		return JSON.parseObject(data, Map.class);
	}

	/**
	 * 转换为指定类型的Map
	 * 
	 * @param <T>
	 * @param data
	 * @param elementType
	 * @return
	 */
	public static <K, V> Map<K, V> toMap(String data, Class<K> keyType, Class<V> valueType) {
		return JSON.parseObject(data, GenericUtils.newMapType(keyType, valueType));
	}

	/**
	 * 转换为StringList
	 * 
	 * @param data
	 * @return
	 */
	public static List<String> toStringList(String data) {
		return JSON.parseObject(data, GenericUtils.LIST_STRING);
	}

	/**
	 * 转换为复杂的List
	 * 
	 * @param data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Object> toList(String data) {
		return JSON.parseObject(data, List.class);
	}

	/**
	 * 转换为指定类型的List
	 * 
	 * @param <T>
	 * @param data
	 * @param elementType
	 * @return
	 */
	public static <T> List<T> toList(String data, Class<T> elementType) {
		return JSON.parseObject(data, GenericUtils.newListType(elementType));
	}

	/**
	 * 转换为指定类型的数组
	 * 
	 * @param <T>
	 * @param data
	 * @param elementType
	 * @return
	 */
	public static <T> T[] toArray(String data, Class<T> elementType) {
		return JSON.parseObject(data, GenericUtils.newArrayType(elementType));
	}

	/**
	 * 将json文本转换为Json结构
	 * 
	 * @param data
	 * @return
	 */
	public static JSONObject toJsonObject(String data) {
		return JSON.parseObject(data);
	}

	/**
	 * 将json文本转换为json数组
	 * 
	 * @param data
	 * @return
	 */
	public static JSONArray toJsonArray(String data) {
		return JSON.parseArray(data);
	}

	/**
	 * 转换为指定的类型
	 * 
	 * @param <T>
	 * @param json
	 * @param type
	 * @return
	 */
	public static <T> T toObject(String json, Type type) {
		if (StringUtils.isBlank(json)) {
			return null;
		}
		return JSON.parseObject(json, type);
	}

	/**
	 * 转换为指定的类型
	 * 
	 * @param type
	 * @param json
	 * @return
	 */
	public static <T> T convert(JSON json, Class<T> type) {
		if (json == null)
			return null;
		return (T) JSON.toJavaObject(json, type);
	}

	/**
	 * Convet object to json string.
	 * 
	 * @param object
	 * @return {@code JSON} , {} if object is null
	 */
	public static String toJson(Object object) {
		if (object == null)
			return EMPTY_JSON;
		return JSON.toJSONString(object);
	}

	/**
	 * Convert to json string with dataformat.
	 * 
	 * @param object
	 * @param dateFormat
	 * @param features
	 * @return
	 */
	public static final String toJson(Object object, DateFormat dateFormat, SerializerFeature... features) {
		return toJson(object,SerializeConfig.getGlobalInstance(),dateFormat,features);
	}

	/**
	 * 将JavaBean序列化为JsonElement结构
	 * 
	 * @param src
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static JSON toJsonTree(Object src) {
		return (JSON) JSON.toJSON(src);
	}

	/**
	 * 序列化对象，输出到流
	 * 
	 * @param src
	 * @param writer
	 */
	public static void toJson(Object src, Writer writer) {
		if (src == null)
			try {
				writer.append(EMPTY_JSON);
				return;
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
		JSON.writeJSONStringTo(src, writer);
	}

	/**
	 * 对Json结构进行排版输出
	 * 
	 * @param json
	 * @return
	 */
	public static String format(JSON json) {
		return format(json, 0);
	}

	/**
	 * 将Json文字转换为XML结构。
	 * 
	 * @param data
	 * @return
	 */
	public static Document jsonToXML(String data) {
		JSONObject obj = toObject(data, JSONObject.class);
		return XMLFastJsonParser.SIMPLE.toDocument(obj);
	}

	public static Document jsonToXML(JSONObject obj) {
		return XMLFastJsonParser.SIMPLE.toDocument(obj);
	}

	/**
	 * 将XML转换为json字符串
	 * 
	 * @param node
	 * @return
	 */
	public static JSONObject xmlToJson(Node node) {
		return XMLFastJsonParser.SIMPLE.toJsonObject(node);
	}

	public static String toJson(Object object, SerializeConfig config, DateFormat dataFormat, SerializerFeature... features) {
		if (object == null)
			return EMPTY_JSON;
		
		JSONSerializer serializer = new JSONSerializer(config);
		serializer.setDateFormat(dataFormat);
		try {
			for (SerializerFeature f : features) {
				serializer.config(f, true);
			}
			if (dataFormat != null) {
				serializer.config(SerializerFeature.WriteDateUseDateFormat, true);
				serializer.setDateFormat(dataFormat);
			}
			serializer.write(object);
			return serializer.toString();
		} finally {
			serializer.close();
		}
	}

	/**
	 * 转换为JSON文本，字段名不加引号
	 * 
	 * @param src
	 * @return
	 */
	public static String toJsonWithoutQuot(Object src) {
		JSONSerializer serializer = new JSONSerializer();
		try {
			serializer.config(SerializerFeature.QuoteFieldNames, false);
			serializer.write(src);
			return serializer.toString();
		} finally {
			serializer.close();
		}
	}

	/**
	 * 转换为JSON文本，字段名不加引号，支持JsFunction对象的转换
	 * 
	 * @param src
	 * @return
	 */
	public static String toJsonScriptCode(Object src) {
		JSONSerializer serializer = new JSONSerializer(JSCFG);
		try {
			serializer.config(SerializerFeature.QuoteFieldNames, false);
			serializer.config(SerializerFeature.PrettyFormat, true);
			serializer.write(src);
			return serializer.toString();
		} finally {
			serializer.close();
		}
	}

	private static String format(Object json, int quote) {
		StringBuilder sb = new StringBuilder();
		if (json instanceof JSONObject) {
			sb.append("{\n");
			Iterator<Map.Entry<String, Object>> ije = ((JSONObject) json).entrySet().iterator();
			if (ije.hasNext()) {
				Map.Entry<String, Object> je = ije.next();
				for (int i = 0; i < quote; i++) {// 添加缩进
					sb.append("\t");
				}
				sb.append(je.getKey()).append(':').append(format(je.getValue(), quote + 1));
				while (ije.hasNext()) {
					sb.append(",\n");// 换行
					je = ije.next();
					for (int i = 0; i < quote; i++) {// 添加缩进
						sb.append("\t");
					}
					sb.append(je.getKey()).append(':').append(format(je.getValue(), quote + 1));
				}
				sb.append('\n');
			}
			for (int i = 0; i < quote - 1; i++) {// 添加缩进
				sb.append("\t");
			}
			sb.append('}');
		} else if (json instanceof JSONArray) {
			sb.append('[');
			Iterator<Object> ji = ((JSONArray) json).iterator();
			if (ji.hasNext()) {
				sb.append(format(ji.next(), quote + 1));
			}
			while (ji.hasNext()) {
				sb.append(',');
				sb.append(format(ji.next(), quote + 1));
			}
			sb.append(']');
		} else {
			sb.append(json.toString());
		}
		return sb.toString();
	}

	private static SerializeConfig JSCFG;
	static {
		JSCFG = ConfigManager.get("EXT");
		JSCFG.putHierarchy(JScriptExpression.class, new ObjectSerializer() {
			public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType,int features) throws IOException {
				serializer.getWriter().write(String.valueOf(object));
			}
		});
	}

}
