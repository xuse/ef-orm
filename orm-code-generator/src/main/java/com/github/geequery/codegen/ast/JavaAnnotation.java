package com.github.geequery.codegen.ast;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class JavaAnnotation implements JavaElement {
	private final String name;
	private final Map<String, Object> properties = new HashMap<String, Object>();

	public JavaAnnotation(Class<? extends Annotation> clz) {
		this.name = clz.getName();
	}

	// public void addCheckImports(Collection<Class<?>> clzs) {
	// for (Class<?> clz : clzs) {
	// checkImport.add(clz.getName());
	// }
	// }
	//
	// public void addCheckImport(Class<?> clz) {
	// checkImport.add(clz.getName());
	// }

	public JavaAnnotation(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public JavaAnnotation put(String key, Object value) {
		properties.put(key, value);
		return this;
	}

	public JavaAnnotation putValue(Object value) {
		put("value", value);
		return this;
	}

	public String toCode(JavaUnit main) {
		// for (String importClass : this.checkImport) {
		// main.addImport(importClass);
		// }
		StringBuilder sb = new StringBuilder();
		sb.append("@").append(main.getJavaClassName(name));
		boolean isSingle = properties.size() == 1;
		if (properties.size() > 0) {
			sb.append("(");
			int n = 0;
			for (String key : properties.keySet()) {
				Object v = properties.get(key);
				if (v == null)
					continue;
				if (isSingle && "value".equals(key)) {
					appendValue(sb, v, main);
				} else {
					if (n > 0)
						sb.append(", ");
					sb.append(key).append(" = ");
					appendValue(sb, v, main);
				}
				n++;
			}
			sb.append(")");
		}
		return sb.toString();
	}

	private static final String[] FROM = { "\r\n", "\n", "\"" };
	private static final String[] TO = { " ", " ", "\\\"" };

	private void appendValue(StringBuilder sb, Object v, JavaUnit main) {
		if (v instanceof Collection) {
			Iterator<?> iter = ((Collection<?>) v).iterator();
			sb.append('{');
			if (iter.hasNext()) {
				Object o = iter.next();
				appendValue(sb, o, main);
			}
			for (; iter.hasNext();) {
				Object o = iter.next();
				sb.append(", \r\n\t");
				appendValue(sb, o, main);
			}
			sb.append('}');
		} else if(v instanceof Class) {
			String name=((Class<?>) v).getName();
			sb.append(main.getJavaClassName(name)+".class");
		} else if(v instanceof IClass) {
			String name=((IClass) v).getName();
			sb.append(main.getJavaClassName(name)+".class");
		} else if (v instanceof JavaAnnotation) {
			sb.append(((JavaAnnotation) v).toCode(main));
		} else if (v instanceof CharSequence) {
			String s = String.valueOf(v);
			s = StringUtils.replaceEach(s, FROM, TO);
			sb.append('"').append(s).append('"');
		} else if (v instanceof Enum) {
			Enum<?> e = (Enum<?>) v;
			String clzName = main.getJavaClassName(e.getDeclaringClass().getName());
			sb.append(clzName).append('.').append(e.name());
		} else {
			sb.append(String.valueOf(v));
		}
	}

	public void buildImport(JavaUnit javaUnit) {
	}

	@Override
	public String toString() {
		return "@" + name + properties;
	}

}
