package jef.database.meta;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import jef.database.DbCfg;
import jef.database.annotation.EasyEntity;
import jef.tools.JefConfiguration;
import jef.tools.ResourceUtils;
import jef.tools.StringUtils;
import jef.tools.XMLUtils;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.Enums;
import jef.tools.resource.IResource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * 默认的元模型加载工具
 * @author jiyi
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DefaultMetaLoader implements MetadataConfiguration {

	private Map<String, URL> urlmaps;

	private static final Id ID = BeanUtils.asAnnotation(Id.class, Collections.EMPTY_MAP);
	private static final Entity ENTITY = BeanUtils.asAnnotation(Entity.class, new HashMap<String, Object>(2));
	private String pattern;
	private int patternType = 0;
	
	static DefaultMetaLoader instance;

	public DefaultMetaLoader() {
		String pattern=StringUtils.trimToNull(JefConfiguration.get(DbCfg.METADATA_RESOURCE_PATTERN));
		setPattern(pattern);
		instance=this;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		if(StringUtils.equals(pattern, this.pattern)){
			return;
		}
		if (pattern != null) {
			if (pattern.indexOf("%s") > -1) {
				patternType = 1;
			} else if (pattern.indexOf("%c") > -1) {
				patternType = 2;
			} else if (pattern.indexOf("%*") > -1) {
				patternType = 3;
			}
		}
		this.pattern=pattern;
		urlmaps=null;
	}

	private Container loadFromResourceUrl(URL url, Class<?> cls) throws SAXException, IOException {
		Document doc = XMLUtils.loadDocument(url);
		Element entityNode = findEntityNode(doc, cls);
		if (entityNode == null) {
			return null;
			// throw new
			// IllegalArgumentException("No definition found for class [" + cls
			// + "] in " + url);
		}
		Container c = new Container(cls);

		Map t = XMLUtils.getAttributesMap(entityNode);
		if ("class".equals(entityNode.getNodeName())) {
			t.put("class", t.get("name"));
			t.put("name", t.get("table"));
		}
		c.table = BeanUtils.asAnnotation(Table.class, t);
		if (t.get("refersh") != null || t.get("checkEnhanced") != null) {
			adjustType("refersh", t, true);
			adjustType("checkEnhanced", t, true);
			c.easy = BeanUtils.asAnnotation(EasyEntity.class, t);
		}

		for (Element e : XMLUtils.childElements(entityNode, "property", "id")) {
			String name = e.getAttribute("name");
			if (StringUtils.isEmpty(name)) {
				continue;
			}
			Map<Class<?>, Annotation> cas = new HashMap<Class<?>, Annotation>();
			if ("id".equals(e.getNodeName())) {
				cas.put(Id.class, ID);
			}
			Element column = XMLUtils.first(e, "column");
			if (column != null) {
				Map map = XMLUtils.getAttributesMap(column, true);
				adjustType("unique", map, false);
				adjustType("nullable", map, true);
				adjustType("insertable", map, true);
				adjustType("updatable", map, true);
				adjustType("length", map, 255);
				adjustType("precision", map, 0);
				adjustType("scale", map, 0);
				adjustType("name",map,"");
				adjustType("columnDefinition",map,"");
				adjustType("table",map,"");
				cas.put(javax.persistence.Column.class, BeanUtils.asAnnotation(javax.persistence.Column.class, map));
			}

			Element generator = XMLUtils.first(e, "generator");
			if (generator != null) {
				Map map = XMLUtils.getAttributesMap(generator);
				String gClass = (String) map.get("class");
				if (StringUtils.isNotEmpty(gClass)) {
					if ("identity".equalsIgnoreCase(gClass)) {
						map.put("strategy", GenerationType.IDENTITY);
					} else if ("sequence".equalsIgnoreCase(gClass)) {
						map.put("strategy", GenerationType.SEQUENCE);
					}
				}
				String sequence = getChildParamText(generator, "sequence");
				if (StringUtils.isNotEmpty(sequence)) {
					Map<String, Object> newMap = new HashMap<String, Object>();
					newMap.put("name", sequence);
					newMap.put("sequenceName", sequence);
					cas.put(SequenceGenerator.class, BeanUtils.asAnnotation(SequenceGenerator.class, newMap));
				}
				adjustType("strategy", map, GenerationType.AUTO);
				cas.put(GeneratedValue.class, BeanUtils.asAnnotation(GeneratedValue.class, map));
			}
			c.fieldColumns.put(name, cas);
		}
		return c;
	}

	private String getChildParamText(Element generator, String key) {
		for (Element x1 : XMLUtils.childElements(generator, "param")) {
			String tmpKey = x1.getAttribute("name");
			if (tmpKey.equals(key)) {
				return XMLUtils.nodeText(x1);
			}
		}
		return null;
	}

	private Element findEntityNode(Document doc, Class<?> cls) {
		Element root = doc.getDocumentElement();
		String rootNodeName = root.getNodeName();
		if ("class".equals(rootNodeName) || "table".equals(rootNodeName)) {
			return root;
		}

		List<Element> children = XMLUtils.childElements(root, "table", "class");
		for (Element ele : children) {
			if ("class".equals(ele.getNodeName())) {
				String clz = ele.getAttribute("name");
				if (StringUtils.equals(cls.getName(), clz)) {
					return ele;
				}
			} else {
				if (children.size() == 1) {
					return ele;
				}
				String clz = ele.getAttribute("class");
				if (StringUtils.equals(cls.getName(), clz)) {
					return ele;
				}
			}
		}
		return null;
	}

	private <T extends Enum<T>> void adjustType(String string, Map map, Enum<T> enumValue) {
		Object value = map.get(string);
		if (value instanceof String) {
			String text = (String) value;
			value = null;
			if (StringUtils.isEmpty(text)) {
				value = enumValue;
			} else {
				value = Enums.valueOf(enumValue.getDeclaringClass(), text.toUpperCase(), (T) enumValue);
			}
			map.put(string, value);
		}else if(value==null){
			map.put(string, enumValue);
		}
	}
	

	private void adjustType(String string, Map map,String value) {
		Object v = map.get(string);
		if(v ==null ){
			map.put(string, value);
		}
	}

	private void adjustType(String string, Map map, int i) {
		Object value = map.get(string);
		if (value instanceof String) {
			String text = (String) value;
			value = null;
			if (StringUtils.isEmpty(text)) {
				value = i;
			} else {
				value = StringUtils.toInt(text, i);
			}
			map.put(string, value);
		}else if(value==null){
			map.put(string, i);
		}
	}

	private void adjustType(String string, Map map, boolean b) {
		Object value = map.get(string);
		if (value instanceof String) {
			String text = (String) value;
			value = null;
			if (StringUtils.isEmpty(text)) {
				value = b;
			} else {
				value = StringUtils.toBoolean(text, b);
			}
			map.put(string, value);
		}else if(value==null){
			map.put(string, b);
		}
	}

	protected URL getResource(Class<?> clz) {
		if (pattern == null) {
			return null;
		}
		switch (patternType) {
		case 0:
			return clz.getResource(pattern);
		case 1:
			return clz.getResource(pattern.replace("%s", clz.getSimpleName()));
		case 2:
			return clz.getResource(pattern.replace("%c", clz.getName()));
		case 3:
			if (urlmaps == null) {
				initUrlMap();
			}
			return urlmaps.get(clz.getName());
		}
		return null;
	}

	private void initUrlMap() {
		try {
			urlmaps = new HashMap<String, URL>();
			IResource[] resources = ResourceUtils.findResources(pattern);
			for (IResource res : resources) {
				parseXmlClass(res);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	private synchronized void parseXmlClass(IResource res) throws SAXException, IOException {
		Document doc = XMLUtils.loadDocument(res.getURL());
		Element root = doc.getDocumentElement();
		for (Element ele : XMLUtils.childElements(root, "class")) {
			String name = ele.getAttribute("name");
			if (StringUtils.isNotEmpty(name)) {
				urlmaps.put(name, res.getURL());
			}
		}
		for (Element ele : XMLUtils.childElements(root, "table")) {
			String name = ele.getAttribute("class");
			if (StringUtils.isNotEmpty(name)) {
				urlmaps.put(name, res.getURL());
			}
		}
	}

	public AnnotationProvider getAnnotations(Class clz) {
		URL url = getResource(clz);
		if (url != null) {
			try {
				AnnotationProvider ano = loadFromResourceUrl(url, clz);
				if (ano != null)
					return ano;
			} catch (SAXException e) {
				throw new IllegalArgumentException(e);
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return new AnnoImpl(clz);
	}

	static class AnnoImpl implements AnnotationProvider {
		private Class<?> clz;

		public AnnoImpl(Class clz) {
			this.clz = clz;
		}

		public String getType() {
			return clz.getName();
		}

		public <T extends Annotation> T getAnnotation(Class<T> type) {
			return clz.getAnnotation(type);
		}

		public <T extends Annotation> T getFieldAnnotation(Field fieldname, Class<T> type) {
			return fieldname.getAnnotation(type);
		}
	}

	private static class Container implements AnnotationProvider {
		private Class<?> clz;
		private EasyEntity easy;
		private String type;
		private Table table;
		private Map<String, Map<Class<?>, Annotation>> fieldColumns = new HashMap<String, Map<Class<?>, Annotation>>();

		public Container(Class<?> cls) {
			clz = cls;
		}

		public <T extends Annotation> T getFieldAnnotation(Field fieldName, Class<T> type) {
			Map<Class<?>, Annotation> anns = fieldColumns.get(fieldName.getName());
			if (anns == null)
				return null;
			T t = (T) anns.get(type);
			if (t == null) {
				return fieldName.getAnnotation(type);
			} else {
				return t;
			}
		}

		public String getType() {
			return type;
		}

		public <T extends Annotation> T getAnnotation(Class<T> type) {
			T t = null;
			if (type == Table.class) {
				t = (T) table;
			} else if (type == EasyEntity.class) {
				t = (T) easy;
			} else if (type == Entity.class) {
				return (T) ENTITY;
			}
			return t == null ? clz.getAnnotation(type) : t;
		}
	}
}
