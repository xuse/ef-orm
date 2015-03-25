package jef.tools;

import java.util.Date;
import java.util.Map.Entry;

import jef.json.JsonUtil;
import jef.json.SerializeConfigEx;
import jef.json.XmlJsonSerializer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.ObjectSerializer;

/**
 * 提供Json和XML的互相转换
 * 
 * @author Administrator
 * 
 */
public abstract class XMLFastJsonParser {
	/**
	 * 缺省转换规则。和json-lib中的转换规则一样
	 * <pre><code>
	 * {"id":576,"desc":"嵇舒门","created":1427266557000,"name":"奚惠","score":38,"modified":1427266815000}
	 * </code></pre>
	 * 将被转换为<code><pre>&lt;o&gt;
    &lt;score type="number"&gt;38.0&lt;/score&gt;
    &lt;id type="number"&gt;576&lt;/id&gt;
    &lt;desc type="string"&gt;嵇舒门&lt;/desc&gt;
    &lt;created type="date"&gt;1427266557000&lt;/created&gt;
    &lt;name type="string"&gt;奚惠&lt;/name&gt;
    &lt;modified type="date"&gt;1427266815000&lt;/modified&gt;
&lt;/o&gt;
	 * </pre></code>
	 * 
	 */
	public static final XMLFastJsonParser DEFAULT = new XMLJsonParserImpl();

	/**
	 * 简易转换规则
	 * {"created":1427266557000,"desc":"嵇舒门","id":576,"modified":1427266815000,"name":"奚惠","score":38}
<?xml version="1.0" encoding="UTF-8"?>
<o created="1427266557000" desc="嵇舒门" id="576" modified="1427266815000" name="奚惠" score="38"/>
	 */
	public static final XMLFastJsonParser SIMPLE = new SimpleXmlJsonImpl();

	public static JSONObject asJsonObject(Object obj) {
		return (JSONObject) obj;
	}

	public static JSONArray asJsonArray(Object obj) {
		return (JSONArray) obj;
	}

	public static JSONObject getAsJSONObject(Object obj) {
		if (obj instanceof JSONObject) {
			return (JSONObject) obj;
		} else {
			return null;
		}
	}

	public static boolean isJsonObject(Object obj) {
		return (obj instanceof JSONObject);
	}

	public static JSONArray getAsJSONArray(Object obj) {
		if (obj instanceof JSONArray) {
			return (JSONArray) obj;
		} else {
			return null;
		}
	}

	public static boolean isJsonArray(Object obj) {
		return (obj instanceof JSONArray);
	}

	public static boolean isJsonNull(Object obj) {
		return obj == null;
	}

	public static boolean isJsonPrimitive(Object obj) {
		if (obj == null)
			return false;
		Class<?> clz = obj.getClass();
		return clz != JSONArray.class && clz != JSONObject.class;
	}

	/**
	 * 将一个JsonObject转换为Document
	 * 
	 * @param json
	 * @return
	 */
	public abstract Document toDocument(JSONObject json);

	/**
	 * 将节点还原为JsonObject
	 * 
	 * @param node
	 * @return
	 */
	public abstract JSONObject toJsonObject(Node node);

	/**
	 * 将节点还原为JsonObject
	 * 
	 * @param node
	 * @return
	 */
	public abstract String toJsonString(Node node);

	static class SimpleXmlJsonImpl extends XMLFastJsonParser {
		private static SerializeConfigEx XML_GSON;
		static {
			XML_GSON = new SerializeConfigEx();
			ObjectSerializer nodeSer=new XmlJsonSerializer();
			XML_GSON.putHierarchy(org.w3c.dom.Node.class, nodeSer);
		}

		public Document toDocument(JSONObject json) {
			Document doc = XMLUtils.newDocument();
			Element root;
			if (json.size() > 1) {
				root = XMLUtils.addElement(doc, "o");
				for (Entry<String, Object> entry : json.entrySet()) {
					processJsonProperty(root, entry.getKey(), entry.getValue());
				}
			} else {
				Entry<String, Object> e = json.entrySet().iterator().next();
				root = XMLUtils.addElement(doc, e.getKey());
				JSONObject rObj = asJsonObject(e.getValue());
				for (Entry<String, Object> entry : rObj.entrySet()) {
					processJsonProperty(root, entry.getKey(), entry.getValue());
				}
			}
			return doc;
		}

		public JSONObject toJsonObject(Node node) {
			String s=toJsonString(node);
//			System.out.println(s);
			return (JSONObject) JSON.parse(s);
		}

		@Override
		public String toJsonString(Node node) {
			return JsonUtil.toJson(node, XML_GSON,null);
		}

		private void processJsonProperty(Element root, String key, Object value) {
			if ("#text".equals(key)) {
				XMLUtils.setText(root, value.toString());
				return;
			}

			if (isJsonObject(value)) {
				Element ele = XMLUtils.addElement(root, key == ARRAY_ELEMENT ? XmlJsonSerializer.NAME_COMPLEX_ARRAY_ELEMENT : key);
				for (Entry<String, Object> entry : asJsonObject(value).entrySet()) {
					processJsonProperty(ele, entry.getKey(), entry.getValue());
				}
			} else if (isJsonNull(value)) {
				if (key == ARRAY_ELEMENT) {
					XMLUtils.addElement(root, XmlJsonSerializer.NAME_PRIMITIVE_ARRAY_ELEMENT, "null");
				} else {
					root.setAttribute(key, "null");
				}
			} else if (isJsonPrimitive(value)) {
				if (key == ARRAY_ELEMENT) {
					XMLUtils.addElement(root, XmlJsonSerializer.NAME_PRIMITIVE_ARRAY_ELEMENT, value.toString());
				} else {
					processJsonPrimitive(root, key, value);
				}
			} else if (isJsonArray(value)) {
				processJsonArray(root, key == ARRAY_ELEMENT ? XmlJsonSerializer.NAME_COMPLEX_ARRAY_ELEMENT : key, asJsonArray(value));
			}
		}

		private static final String ARRAY_ELEMENT = "#e";

		private void processJsonArray(Element ele, String key, JSONArray jsonArray) {
			Element arrNode = XMLUtils.addElement(ele, key);
			arrNode.setAttribute("_type", "array");
			for (Object json : jsonArray) {
				processJsonProperty(arrNode, ARRAY_ELEMENT, json);
			}
		}

		private void processJsonPrimitive(Element ele, String key, Object p) {
			if(p instanceof Date){
				ele.setAttribute(key, String.valueOf(((Date)p).getTime()));
			}else if(p instanceof Number){
				String floatText= p.toString();
				if (floatText.endsWith(".0")) {
					floatText = floatText.substring(0, floatText.length() - 2);
				}
				ele.setAttribute(key, floatText);
			}else{
				ele.setAttribute(key, p.toString());
			}
		}
	}

	static class XMLJsonParserImpl extends XMLFastJsonParser {
		public Document toDocument(JSONObject json) {
			Document doc = XMLUtils.newDocument();
			Element root;
			root = XMLUtils.addElement(doc, "o");
			for (Entry<String, Object> entry : json.entrySet()) {
				processJsonProperty(root, entry.getKey(), entry.getValue());
			}
			return doc;
		}

		/**
		 * 将XML节点转换为Json,XML节点格式必须符合 <o> </o>
		 * 
		 * @param doc
		 * @return
		 */
		public JSONObject toJsonObject(Node doc) {
			if (doc.getNodeType() == Node.DOCUMENT_NODE) {
				doc = ((Document) doc).getDocumentElement();
			}
			JSONObject obj = new JSONObject();
			for (Element e : XMLUtils.childElements(doc)) {
				String name = e.getTagName();
				if (e.hasAttribute("null") && e.getAttribute("null").equalsIgnoreCase("true")) {
					obj.put(name, null);
				} else if (e.hasAttribute("type")) {
					obj.put(name, processJsonType(e, e.getAttribute("type")));
				} else if (e.hasAttribute("class")) {
					obj.put(name, processJsonClass(e, e.getAttribute("class")));
				} else {
					throw new IllegalArgumentException("The xml node '" + e.getNodeName() + "' do not have any type/class attribute.");
				}
			}
			return obj;
		}

		private void processJsonProperty(Element root, String key, Object value) {
			Element ele = XMLUtils.addElement(root, key);
			if (isJsonObject(value)) {
				ele.setAttribute("class", "object");
				for (Entry<String, Object> entry : asJsonObject(value).entrySet()) {
					processJsonProperty(ele, entry.getKey(), entry.getValue());
				}
			} else if (isJsonNull(value)) {
				ele.setAttribute("class", "object");
				ele.setAttribute("null", "true");
			} else if (isJsonPrimitive(value)) {
				processJsonPrimitive(ele, value);
			} else if (isJsonArray(value)) {
				processJsonArray(ele, asJsonArray(value));
			}
		}

		private void processJsonArray(Element ele, JSONArray jsonArray) {
			ele.setAttribute("class", "array");
			for (Object json : jsonArray) {
				processJsonProperty(ele, "e", json);
			}
		}

		private void processJsonPrimitive(Element ele, Object p) {
			if (p instanceof Boolean) {
				ele.setAttribute("type", "boolean");
				XMLUtils.setText(ele, String.valueOf(p));
			} else if (p instanceof Number) {
				ele.setAttribute("type", "number");
				XMLUtils.setText(ele, String.valueOf(p));
			} else if (p instanceof String) {
				ele.setAttribute("type", "string");
				XMLUtils.setText(ele, (String) p);
			} else if (p instanceof Date) {
				ele.setAttribute("type", "date");
				Date d = (Date) p;
				XMLUtils.setText(ele, String.valueOf((d.getTime())));
			} else {
				throw new IllegalArgumentException(p.getClass().getName());
			}
		}

		private Object processJsonClass(Element ele, String class_) {
			if ("object".equals(class_)) {
				return toJsonObject(ele);
			} else if ("array".equals(class_)) {
				JSONArray array = new JSONArray();
				for (Element e : XMLUtils.childElements(ele, "e")) {
					if (e.hasAttribute("type")) {
						array.add(processJsonType(e, e.getAttribute("type")));
					} else if (e.hasAttribute("class")) {
						array.add(processJsonClass(e, e.getAttribute("class")));
					} else {
						throw new IllegalArgumentException("Unknown type:" + class_);
					}
				}
				return array;
			} else {
				throw new IllegalArgumentException();
			}
		}

		private Object processJsonType(Element ele, String type) {
			if ("boolean".equals(type)) {
				return StringUtils.toBoolean(XMLUtils.nodeText(ele), false);
			} else if ("number".equals(type)) {

				String x = XMLUtils.nodeText(ele);
				if (x.indexOf('.') > -1) {
					return StringUtils.toDouble(x, 0d);
				} else {
					return StringUtils.toLong(x, 0L);
				}
			} else if ("date".equals(type)) {
				String x = XMLUtils.nodeText(ele);
				if (StringUtils.isEmpty(x))
					return null;
				return new Date(StringUtils.toLong(x, 0L));
			} else {
				String s = XMLUtils.nodeText(ele);
				return s;
			}
		}

		@Override
		public String toJsonString(Node node) {
			return toJsonObject(node).toJSONString();
		}
	}
}
