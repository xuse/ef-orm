package com.github.geequery.codegen.ast;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.geequery.codegen.ast.IClass.RealClass;
import jef.tools.ArrayUtils;

public class JavaConstructor extends DefaultJavaElement {
	private static final String JAVA_CONSTRUCTOR = "#JAVA_CONSTRUCTOR#";
	private int modifier = 1;
	private Map<String, IClass> inputArgs = new LinkedHashMap<String, IClass>();
	private Map<String, Integer> paramModifier = new HashMap<String, Integer>();
	private List<IClass> throws_ = new ArrayList<IClass>();
	private boolean varArg = false;
	private String[] typeParameters;

	public void addTypeParameter(String typeParameter) {
		this.typeParameters = ArrayUtils.addElement(typeParameters, typeParameter);
	}

	public String[] getTypeParameters() {
		return typeParameters;
	}

	public void setTypeParameters(String[] typeParameters) {
		this.typeParameters = typeParameters;
	}

	public boolean isVarArg() {
		return varArg;
	}

	public void setVarArg(boolean varArg) {
		this.varArg = varArg;
	}

	public void setAbstract(boolean isAbstract) {
		if (isAbstract != isAbstract()) {
			modifier ^= Modifier.ABSTRACT;
		}
	}

	public boolean isAbstract() {
		return Modifier.isAbstract(modifier);
	}

	public JavaConstructor() {
	}

	public void setModifier(int modifier) {
		this.modifier = modifier;
	}

	public void addparam(IClass type, String argname, int modifiers) {
		inputArgs.put(argname, type);
		paramModifier.put(argname, modifiers);
	}

	public void addparam(Class<?> javaType, String argname) {
		addparam(new RealClass(javaType), argname, 0);
	}

	public void addparam(String javaType, String argname) {
		addparam(IClassUtil.getIClass(javaType), argname, 0);
	}

	public void addparam(String javaType, String argname, int modifier) {
		addparam(IClassUtil.getIClass(javaType), argname, modifier);
	}

	public void addThrows(Class<? extends Throwable> t) {
		throws_.add(new RealClass(t));
	}

	public void addThrows(String t) {
		throws_.add(IClassUtil.getIClass(t));
	}

	private String code = null;

	// 当使用toCode方法生成imports时已经太迟了，所以生成要提前
	public void buildImport(JavaUnit javaUnit) {
		super.buildImport(javaUnit);
		this.code = toCode(javaUnit);
	}

	public void clear() {
		this.code = null;
	}

	public String toCode(JavaUnit main) {
		if (code != null)
			return code;
		StringBuilder sb = new StringBuilder();
		// 生成注释
		sb.append(super.generateComments());
		// 生成Annotation
		for (String s : annotations) {
			sb.append(s);
			sb.append("\r\n\t");
		}
		JavaUnit.appendModifier(sb, this.modifier, false);
		if (typeParameters != null && typeParameters.length > 0) {
			sb.append("<");
			for (int n = 0; n < typeParameters.length; n++) {
				if (n > 0)
					sb.append(',');
				sb.append(typeParameters[n]);
			}
			sb.append("> ");
		}
		sb.append(main.getSimpleName());
		sb.append("(");
		if (!inputArgs.isEmpty()) {
			Iterator<String> iter = inputArgs.keySet().iterator();
			String key = iter.next(); // key形参名
			IClass pcls = inputArgs.get(key);
			int mod = paramModifier.containsKey(key) ? paramModifier.get(key) : 0;
			JavaUnit.appendModifier(sb, mod, false);
			if (varArg && !iter.hasNext()) {
				sb.append(main.getJavaClassName(pcls)).append("... ").append(key);
			} else {
				sb.append(main.getJavaClassName(pcls)).append(" ").append(key);
			}
			for (; iter.hasNext();) {
				key = iter.next();
				sb.append(",");
				pcls = inputArgs.get(key);
				mod = paramModifier.containsKey(key) ? paramModifier.get(key) : 0;
				JavaUnit.appendModifier(sb, mod, false);
				if (varArg && !iter.hasNext()) {
					sb.append(main.getJavaClassName(pcls)).append("... ").append(key);
				} else {
					sb.append(main.getJavaClassName(pcls)).append(" ").append(key);
				}
			}
		}

		sb.append(")");
		// 添加异常定义
		if (this.throws_ != null && throws_.size() > 0) {
			sb.append("throws ");
			for (int i = 0; i < throws_.size(); i++) {
				if (i > 0)
					sb.append(',');
				IClass t = throws_.get(i);
				sb.append(main.getJavaClassName(t));
			}
		}
		if (Modifier.isAbstract(this.modifier) || main.isInterface()) {
			sb.append(";");
			return sb.toString();
		}
		sb.append("{\r\n");
		for (String str : super.getContent()) {
			sb.append("\t\t");
			sb.append(str).append("\r\n");
		}
		sb.append("\t}\r\n");
		return sb.toString();
	}

	static String toMethodKey(String name, IClass... inputArgs) {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append('(');
		if (inputArgs.length > 0) {
			for (IClass clz : inputArgs) {
				sb.append(clz.getSimpleName());
				sb.append(',');
			}
			sb.setLength(sb.length() - 1);
		}
		sb.append(')');
		return sb.toString();
	}

	static String toMethodKey(String name, Collection<IClass> inputArgs) {
		return toMethodKey(name, inputArgs.toArray(new IClass[inputArgs.size()]));
	}

	public String getKey() {
		return toConstratorKey(inputArgs.values());
	}

	public static String toConstratorKey(Collection<IClass> keys) {
		return toMethodKey(JAVA_CONSTRUCTOR, keys);
	}
}
