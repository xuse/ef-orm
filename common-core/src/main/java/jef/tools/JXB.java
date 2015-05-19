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
package jef.tools;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import jef.common.IntList;
import jef.common.log.LogUtil;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.BeanWrapperImpl;
import jef.tools.reflect.ClassEx;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * JXB与JAXB类似，是JEF中为了实现Java对象和XML绑定所编写的工具
 * 
 * @author Jiyi
 */
public class JXB {
	private JXB() {
	};

	/**
	 * 将一个XML节点转化为对象
	 * 
	 * @param e
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static Object elementToObject(Element e) throws InstantiationException, IllegalAccessException {
		return classElementToObject(e, new HashMap<String, Object>());
	}

	/**
	 * 将XML节点内容填充到指定的对象中
	 * 
	 * @param e
	 * @param obj
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static void populateObject(Element e, Object obj) throws InstantiationException, IllegalAccessException {
		populateObject(e, obj, new HashMap<String, Object>());
	}

	/**
	 * 将对象转化为XML
	 * 
	 * @param parent
	 *            对象节点将挂在此节点下
	 * @param obj
	 */
	public static Element objectToXML(Object obj, Node parent) {
		HashMap<Integer, Element> objs = new HashMap<Integer, Element>();
		IntList referd = new IntList();
		Element e = objectToXML(parent, obj, objs, referd,null);
		for (Integer i : objs.keySet()) {
			if (!referd.contains(i)) {// 仅有被引用的对象才需要hashcode,删除不需要hashCode的节点，让XML更清晰一些
				Element e1 = objs.get(i);
				e1.removeAttribute("hashCode");
			}
		}
		return e;
	}

	private static void populateObject(Element e, Object obj, HashMap<String, Object> objects) throws InstantiationException, IllegalAccessException {
		BeanWrapperImpl bean = new BeanWrapperImpl(obj);
		NamedNodeMap attribs = e.getAttributes();
		for (int i = 0; i < attribs.getLength(); i++) {
			Node nd = attribs.item(i);
			String name = dashToUpper(nd.getNodeName(), false);

			String fieldName = bean.findPropertyIgnoreCase(name);
			if (fieldName != null && bean.isWritableProperty(fieldName)) {
				setValue(bean, fieldName, nd, objects);
			}
		}
		for (Element sub : XMLUtils.childElements(e)) {
			String name = dashToUpper(sub.getNodeName(), false);
			String fieldName = bean.findPropertyIgnoreCase(name);
			if (fieldName != null && bean.isWritableProperty(fieldName)) {
				setValue(bean, fieldName, sub, objects);
			}
		}
	}

	/*
	 * @param parent 上节点
	 * 
	 * @param obj 数据
	 * 
	 * @param objects 已经处理对象
	 * 
	 * @param refedHash
	 * 
	 * @return
	 */
	private static Element objectToXML(Node parent, Object obj, Map<Integer, Element> objects, IntList refedHash,Class<?> fieldContainer) {
		if (obj == null) {
			Element root = (parent.getNodeType() == Node.DOCUMENT_NODE) ? ((Document) parent).createElement("null") : parent.getOwnerDocument().createElement("null");
			return root;
		}
		Class<?> c = obj.getClass();
		String classname = (c.isArray()) ? "Array_" + c.getComponentType().getName() : c.getName();
		classname = classname.replace('$', '_');
		
		Element objElement;
		if(fieldContainer==null){
			objElement = (parent.getNodeType() == Node.DOCUMENT_NODE) ? ((Document) parent).createElement(classname) : parent.getOwnerDocument().createElement(classname);
			parent.appendChild(objElement);
		}else{
			objElement=(Element)parent;
		}
		// 开始处理
		if (objects.containsKey(obj.hashCode())) {
			objElement.setAttribute("ref-hashCode", String.valueOf(obj.hashCode()));
			refedHash.add(obj.hashCode());
			return objElement;
		} else {
			objElement.setAttribute("hashCode", String.valueOf(obj.hashCode()));
			objects.put(obj.hashCode(), objElement);// 标记已经处理
		}
		if (processIterator(c, obj, objElement, objects, refedHash)) {// 如果是可枚举的对象
			return objElement;
		} else if (classname.startsWith("java.lang")) {// java基本对象
			objElement.setAttribute("value", String.valueOf(obj));
			return objElement;
		} else {
			BeanWrapperImpl bean = new BeanWrapperImpl(obj);
			for (String fieldName : bean.getRwPropertyNames()) {
				appendValue(bean, fieldName, objElement, objects, refedHash);
			}
		}
		return objElement;
	}

	public static String dashToUpper(String name, boolean capitalize) {
		if (name.indexOf('-') < 0)
			return name;
		char[] r = new char[name.length()];
		int n = 0;
		boolean nextUpper = capitalize;
		for (char c : name.toCharArray()) {
			if (c == '-') {
				nextUpper = true;
			} else {
				if (nextUpper) {
					r[n] = Character.toUpperCase(c);
					nextUpper = false;
				} else {
					r[n] = Character.toLowerCase(c);
				}
				n++;
			}
		}
		return new String(r, 0, n);
	}

	/**
	 * 将以类名为节点名的节点转换为对象
	 */
	private static Object classElementToObject(Element e, HashMap<String, Object> objects) throws InstantiationException, IllegalAccessException {
		String nodeName = dashToUpper(e.getNodeName(), false);
		String hashId = XMLUtils.attrib(e, "hashCode");
		String ref_hashId = XMLUtils.attrib(e, "ref-hashCode");
		Object obj;
		if (nodeName.startsWith("Array_")) {
			try {
				obj = processIteratorElement(nodeName, e, objects);
			} catch (ClassNotFoundException e1) {
				throw new IllegalAccessException("ClassNotFound:" + e1.getMessage());
			}
			return obj;
		}
		if (nodeName.indexOf(".") > -1) {// 肯定是类名
			String s = XMLUtils.attrib(e, "value");
			if (s == null)
				s = XMLUtils.nodeText(e);
			if ("java.lang.String".equals(nodeName)) {
				obj = s;
			} else if ("java.lang.Long".equals(nodeName)) {
				if ("null".equals(s) || s == null)
					return null;
				obj = Long.valueOf(s);
			} else if ("java.lang.Integer".equals(nodeName)) {
				if ("null".equals(s) || s == null)
					return null;
				obj = Integer.valueOf(s);
			} else if ("java.lang.Short".equals(nodeName)) {
				if ("null".equals(s) || s == null)
					return null;
				obj = Short.valueOf(s);
			} else if ("java.lang.Float".equals(nodeName)) {
				if ("null".equals(s) || s == null)
					return null;
				obj = Float.valueOf(s);
			} else if ("java.lang.Double".equals(nodeName)) {
				if ("null".equals(s) || s == null)
					return null;
				obj = Double.valueOf(s);
			} else if ("java.lang.Byte".equals(nodeName)) {
				if ("null".equals(s) || s == null)
					return null;
				obj = Byte.valueOf(s);
			} else if ("java.lang.Boolean".equals(nodeName)) {
				if ("null".equals(s) || s == null)
					return null;
				obj = Boolean.valueOf(s);
			} else if ("java.lang.Character".equals(nodeName)) {
				if ("null".equals(s) || s == null)
					return null;
				obj = XMLUtils.nodeText(e).charAt(0);
			} else {
				if (ref_hashId != null) {
					obj = objects.get(ref_hashId);
					Assert.notNull(obj);
				} else {
					try {
						nodeName = nodeName.replace('_', '$');
						obj = processIteratorElement(nodeName, e, objects);
						if (obj == null) {
							Class<?> c = Class.forName(nodeName);
							obj = c.newInstance();
							if (hashId != null) {
								objects.put(hashId, obj);
							}
							populateObject(e, obj, objects);
						}
					} catch (ClassNotFoundException e1) {
						throw new IllegalAccessException("ClassNotFound:" + e1.getMessage());
					}
				}
				return obj;
			}
		} else {// 可能是无包名的类，也可能是某个未开放的字段名，无法处理
			throw new IllegalArgumentException("Unknown element class:" + nodeName);
		}
		if (hashId != null) {
			objects.put(hashId, obj);
		}
		return obj;
	}

	// 将数组或集合类型的对象转化为XML
	private static boolean processIterator(Class<?> c, Object obj, Element root, Map<Integer, Element> objects, IntList list) {
		if (c.isArray()) {
			if (c.getComponentType().isPrimitive()) {// 是原生类型数组
				Object[] objArray = ArrayUtils.toObject(obj);// 强制将原生类型转换为对象类型(装箱)
				for (Object ele : objArray) {
					objectToXML(root, ele, objects, list,null);
				}
			} else {// 对象数组
				for (Object ele : (Object[]) obj) {
					objectToXML(root, ele, objects, list,null);
				}
			}
			return true;
		} else if (List.class.isAssignableFrom(c)) {
			for (Object ele : (List<?>) obj) {
				objectToXML(root, ele, objects, list,null);
			}
			return true;
		} else if (Set.class.isAssignableFrom(c)) {
			for (Object ele : (Set<?>) obj) {
				objectToXML(root, ele, objects, list,null);
			}
			return true;
		} else if (Map.class.isAssignableFrom(c)) {
			Map<?, ?> map = (Map<?, ?>) obj;
			for (Object ele : map.keySet()) {
				Element entry = root.getOwnerDocument().createElement("Entry");
				root.appendChild(entry);
				Element key = objectToXML(entry, ele, objects, list,null);
				key.setAttribute("entry_type", "key");
				Element value = objectToXML(entry, map.get(ele), objects, list,null);
				value.setAttribute("entry_type", "value");
			}
			return true;
		} else if (Queue.class.isAssignableFrom(c)) {
			for (Object ele : (Set<?>) obj) {
				objectToXML(root, ele, objects, list, null);
			}
			return true;
		}
		return false;
	}

	// 将对象的字段中的值写入XML节点上
	private static void appendValue(BeanWrapperImpl bean, String fieldName, Element current, Map<Integer, Element> objects, IntList list) {
		Object value = bean.getPropertyValue(fieldName);
		if (value == null)
			return;
		// ?bean.getFieldType(fieldName):
		Class<?> type = BeanUtils.toWrapperClass(bean.getPropertyRawType(fieldName));
		Class<?> c = value.getClass();
		Element element = null;
		if (String.class == c) {
			element = XMLUtils.addElement(current, fieldName, (String) value);
		} else if (Date.class == c) {
			element = XMLUtils.addElement(current, fieldName, DateUtils.formatDateTime((Date) value));
		} else if (Integer.class == c || Integer.TYPE == c) {
			element = XMLUtils.addElement(current, fieldName, String.valueOf(value));
		} else if (Boolean.class == c || Boolean.TYPE == c) {
			element = XMLUtils.addElement(current, fieldName, String.valueOf(value));
		} else if (Double.class == c || Double.TYPE == c) {
			element = XMLUtils.addElement(current, fieldName, String.valueOf(value));
		} else if (Float.class == c || Float.TYPE == c) {
			element = XMLUtils.addElement(current, fieldName, String.valueOf(value));
		} else if (Byte.class == c || Byte.TYPE == c) {
			element = XMLUtils.addElement(current, fieldName, String.valueOf(value));
		} else if (Character.class == c || Character.TYPE == c) {
			element = XMLUtils.addElement(current, fieldName, String.valueOf(value));
		} else if (Long.class == c || Long.TYPE == c) {
			element = XMLUtils.addElement(current, fieldName, String.valueOf(value));
		} else if (Short.class == c || Short.TYPE == c) {
			element = XMLUtils.addElement(current, fieldName, String.valueOf(value));
		} else if (c.isEnum()) {
			element = XMLUtils.addElement(current, fieldName, String.valueOf(value));
		} else if (c.getName().startsWith("java.lang")) {// java基本对象
			element = XMLUtils.addElement(current, fieldName, String.valueOf(value));
		} else {
			Element field = XMLUtils.addElement(current, fieldName);
			objectToXML(field, value, objects, list, type);
		}
		if (type != c && element!=null) {
			element.setAttribute("class", c.getSimpleName());
		}
	}

	// 将XML节点上的值写入到对象中
	@SuppressWarnings({ "unchecked" })
	private static void setValue(BeanWrapperImpl bean, String fieldName, Node sub, HashMap<String, Object> objects) throws InstantiationException, IllegalAccessException {
		ClassEx c = new ClassEx(bean.getProperty(fieldName).getType());
		String value = null;
		if (sub.getNodeType() == Node.ATTRIBUTE_NODE) {
			value = sub.getNodeValue();
		} else if (sub.getNodeType() == Node.ELEMENT_NODE) {
			value = XMLUtils.nodeText((Element) sub);
		} else {
			return;
		}
		// 根据目标对象中的字段属性来赋值
		if (String.class == c.getWrappered()) {
			bean.setPropertyValue(fieldName, value);
		} else if (Date.class == c.getWrappered()) {
			try {
				Date d = DateUtils.parseDateTime(value);
				bean.setPropertyValue(fieldName, d);
			} catch (ParseException ex) {
				LogUtil.exception(ex);
			}
		} else if (c.getWrappered() == Integer.class || "int".equals(c.getName())) {
			bean.setPropertyValue(fieldName, StringUtils.toInt(value, 0));
		} else if (Boolean.class == c.getWrappered() || "boolean".equals(c.getName())) {
			bean.setPropertyValue(fieldName, StringUtils.toBoolean(value, false));
		} else if (Double.class == c.getWrappered() || "double".equals(c.getName())) {
			bean.setPropertyValue(fieldName, StringUtils.toDouble(value, 0.0));
		} else if (Float.class == c.getWrappered() || "float".equals(c.getName())) {
			bean.setPropertyValue(fieldName, StringUtils.toFloat(value, 0f));
		} else if (Byte.class == c.getWrappered() || "byte".equals(c.getName())) {
			bean.setPropertyValue(fieldName, (byte) StringUtils.toInt(value, 0));
		} else if (Character.class == c.getWrappered() || "char".equals(c.getName())) {
			if (value.length() > 0) {
				bean.setPropertyValue(fieldName, value.substring(0, 1));
			}
		} else if (Long.class == c.getWrappered() || "long".equals(c.getName())) {
			bean.setPropertyValue(fieldName, StringUtils.toLong(value, 0L));
		} else if (Short.class == c.getWrappered() || "short".equals(c.getName())) {
			bean.setPropertyValue(fieldName, StringUtils.toInt(value, 0));
		} else if (c.isEnum()) {
			bean.setPropertyValue(fieldName, Enum.valueOf(c.getWrappered().asSubclass(Enum.class), value));
		} else {
			Element detailNode = (Element) XMLUtils.first(sub, Node.ELEMENT_NODE);
			if (detailNode != null) {
				Object obj = classElementToObject(detailNode, objects);
				if (obj != null) {
					bean.setPropertyValue(fieldName, obj);
				}
			} else if (sub.getNodeType() == Node.ATTRIBUTE_NODE || value != null) {
				bean.setPropertyValue(fieldName, value);
			}
		}
	}

	// 将XML中列表元素转换为Array,List,Map,Set,Queue等对象，如果不是上述对象则返回Null
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object processIteratorElement(String className, Element arrayNode, HashMap<String, Object> objects) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
		String nodeName = arrayNode.getNodeName();
		String hashId = XMLUtils.attrib(arrayNode, "hashCode");
		String ref_hashId = XMLUtils.attrib(arrayNode, "ref-hashCode");
		Object result = null;
		if (ref_hashId != null) {
			Assert.isTrue(objects.containsKey(ref_hashId));
			return objects.get(ref_hashId);
		}
		if (className.startsWith("Array_")) {// 必须先知道数组有多少个元素
			String compName = StringUtils.substringAfter(className, "_");
			String objName = primitiveNameToObjName(compName);
			boolean isPrimitive = (compName != objName);
			List<Element> elements = XMLUtils.childElements(arrayNode);
			try {
				Object[] objs = (Object[]) Array.newInstance(Class.forName(primitiveNameToObjName(compName)), elements.size());
				for (int i = 0; i < elements.size(); i++) {
					Element eleNode = elements.get(i);
					objs[i] = classElementToObject(eleNode, objects);
					if (isPrimitive && objs[i] == null) {// 原生类型不可以为NULL
						throw new RuntimeException("primitive type must not be NULL");
					}
				}
				if (isPrimitive) {// 是基本类型的数组
					result = ArrayUtils.toPrimitive(objs);
				} else {
					result = objs;
				}
			} catch (NegativeArraySizeException e) {
				throw new IllegalAccessException(e.getMessage());
			} catch (ClassNotFoundException e) {
				throw new IllegalAccessException(e.getMessage());
			}
		}
		if (result == null) {
			Class c = Class.forName(className);
			if (Map.class.isAssignableFrom(c)) {
				Class impl = Class.forName(nodeName);
				Map<Object, Object> objs = (Map<Object, Object>) impl.newInstance();
				List<Element> elements = XMLUtils.childElements(arrayNode);
				for (Element entry : elements) {
					Element keyE = null;
					Element valueE = null;
					for (Element e : XMLUtils.childElements(entry)) {
						String eeType = e.getAttribute("entry_type");
						if ("key".equals(eeType)) {
							keyE = e;
						} else if ("value".equals(eeType)) {
							valueE = e;
						}
					}
					Assert.notNull(keyE);
					Assert.notNull(valueE);
					Object key = classElementToObject(keyE, objects);
					Object value = classElementToObject(valueE, objects);
					objs.put(key, value);
				}
				result = objs;
			} else if (List.class.isAssignableFrom(c)) {
				Class impl = Class.forName(nodeName);
				List<Object> objs = (List<Object>) impl.newInstance();
				List<Element> elements = XMLUtils.childElements(arrayNode);
				for (Element eleNode : elements) {
					objs.add(classElementToObject(eleNode, objects));
				}
				result = objs;
			} else if (Set.class.isAssignableFrom(c)) {
				Class impl = Class.forName(nodeName);
				Set<Object> objs = (Set<Object>) impl.newInstance();
				List<Element> elements = XMLUtils.childElements(arrayNode);
				for (Element eleNode : elements) {
					objs.add(classElementToObject(eleNode, objects));
				}
				result = objs;
			} else if (Queue.class.isAssignableFrom(c)) {
				Class impl = Class.forName(nodeName);
				Queue<Object> objs = (Queue<Object>) impl.newInstance();
				List<Element> elements = XMLUtils.childElements(arrayNode);
				for (Element eleNode : elements) {
					objs.add(classElementToObject(eleNode, objects));
				}
				result = objs;
			}
		}
		if (hashId != null && result != null) {
			objects.put(hashId, result);
		}
		return result;
	}

	// 从原生名称得到对象名称
	private static String primitiveNameToObjName(String compName) {
		if ("int".equals(compName)) {
			return Integer.class.getName();
		} else if ("boolean".equals(compName)) {
			return Boolean.class.getName();
		} else if ("long".equals(compName)) {
			return Long.class.getName();
		} else if ("short".equals(compName)) {
			return Short.class.getName();
		} else if ("float".equals(compName)) {
			return Float.class.getName();
		} else if ("byte".equals(compName)) {
			return Byte.class.getName();
		} else if ("char".equals(compName)) {
			return Character.class.getName();
		} else if ("double".equals(compName)) {
			return Double.class.getName();
		} else {
			return compName;
		}
	}

	/**
	 * 将对象用JXB序列化为XML保存
	 * 
	 * @param obj
	 * @param file
	 * @return
	 */
	public static boolean saveObjectToXML(Object obj, File file) {
		try {
			Document doc = XMLUtils.newDocument();
			JXB.objectToXML(obj, doc);
			XMLUtils.saveDocument(doc, file, "UTF-8");
			return true;
		} catch (Exception e) {
			LogUtil.exception(e);
			return false;
		}
	}

	/**
	 * 将JXB保存的对象还原
	 * 
	 * @param file
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static Object loadObjectFromXML(File file) throws SAXException, IOException, InstantiationException, IllegalAccessException {
		Document doc = XMLUtils.loadDocument(file);
		Object obj = JXB.elementToObject(doc.getDocumentElement());
		return obj;
	}

}
