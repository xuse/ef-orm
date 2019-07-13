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
package com.github.geequery.codegen.ast;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.geequery.codegen.ast.IClass.RealClass;

import jef.tools.ArrayUtils;
import jef.tools.StringUtils;
import jef.tools.string.JefStringReader;

public class JavaMethod extends DefaultJavaElement implements JavaElement {
	private String name;
	private int modifier = 1;
	private IClass returnType;
	private Map<String, JavaParameter> inputArgs = new LinkedHashMap<String, JavaParameter>();
	private List<IClass> throws_ = new ArrayList<IClass>();
	private boolean varArg = false;
	private boolean checkReturn = true;
	private String[] typeParameters;

	public IClass getReturnType() {
		return returnType;
	}

	public void clearInputArgs() {
		inputArgs.clear();
	}

	/**
	 * 按序号获取，从0开始
	 * 
	 * @param index
	 * @return
	 */
	public JavaParameter getParameter(int index) {
		String key = inputArgs.keySet().toArray(new String[0])[index];
		return getParameter(key);
	}

	/**
	 * 按名称获取
	 * 
	 * @param name
	 * @return
	 */
	public JavaParameter getParameter(String name) {
		return inputArgs.get(name);
	}

	/**
	 * 获取所有参数的类型
	 * 
	 * @return
	 */
	public IClass[] getParameterTypes() {
		List<IClass> types = new ArrayList<IClass>();
		for (JavaParameter p : inputArgs.values()) {
			types.add(p.getType());
		}
		return types.toArray(new IClass[inputArgs.size()]);
	}

	/**
	 * 增加一个泛型定义
	 * 
	 * @param typeParameter
	 */
	public void addTypeParameter(String typeParameter) {
		this.typeParameters = ArrayUtils.addElement(typeParameters, typeParameter);
	}

	public String[] getTypeParameters() {
		return typeParameters;
	}

	public void setTypeParameters(String[] typeParameters) {
		this.typeParameters = typeParameters;
	}

	public boolean isCheckReturn() {
		return checkReturn;
	}

	public void setCheckReturn(boolean checkReturn) {
		this.checkReturn = checkReturn;
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

	public void setStatic(boolean isStatic) {
		if (isStatic != isStatic()) {
			modifier ^= Modifier.STATIC;
		}
	}

	public boolean isStatic() {
		return Modifier.isStatic(modifier);
	}

	public JavaMethod(String name) {
		this.name = name;
	}

	public void setModifier(int modifier) {
		this.modifier = modifier;
	}

	public void setReturnType(IClass type) {
		this.returnType = type;
	}

	public void setReturnType(String javaType) {
		if ("void".equals(javaType)) {
			this.returnType = null;
		} else {
			this.returnType = StringUtils.isEmpty(javaType) ? null : IClassUtil.parse(javaType);
		}
	}

	public void setReturnType(Class<?> javaType) {
		this.returnType = (javaType == null || javaType == Void.class) ? null : new RealClass(javaType);
	}

	public JavaParameter addparam(IClass type, String argname, int modifiers) {
		JavaParameter p = new JavaParameter(this, argname, type, modifiers);
		inputArgs.put(argname, p);
		return p;
	}

	public JavaParameter addparam(Class<?> javaType, String argname) {
		return addparam(new RealClass(javaType), argname, 0);
	}

	public JavaParameter addparam(String javaType, String argname) {
		return addparam(IClassUtil.parse(javaType), argname, 0);
	}

	public JavaParameter addparam(String javaType, String argname, int modifier) {
		return addparam(IClassUtil.parse(javaType), argname, modifier);
	}

	public void addThrows(Class<? extends Throwable> t) {
		throws_.add(new RealClass(t));
	}

	public void addThrows(String t) {
		throws_.add(IClassUtil.parse(t));
	}

	// private String code = null;

	public List<IClass> getThrows() {
		return throws_;
	}

	// 当使用toCode方法生成imports时已经太迟了，所以生成要提前
	public void buildImport(JavaUnit javaUnit) {
		super.buildImport(javaUnit);
		toCode(javaUnit);
	}

	// public void clear() {
	// this.code = null;
	// }

	public String toCode(JavaUnit main) {
		// if (code != null)
		// return code;
		StringBuilder sb = new StringBuilder();
		// 生成注释
		sb.append(super.generateComments());
		// 生成Annotation
		for (String s : annotations) {
			sb.append(s);
			sb.append("\r\n\t");
			// line++;
		}
		JavaUnit.appendModifier(sb, this.modifier, main.isInterface());
		if (typeParameters != null && typeParameters.length > 0) {
			sb.append("<");
			for (int n = 0; n < typeParameters.length; n++) {
				if (n > 0)
					sb.append(',');
				sb.append(typeParameters[n]);
			}
			sb.append(">");
		}
		if (returnType == null) {
			sb.append("void ");
		} else {
			sb.append(main.getJavaClassName(returnType)).append(" ");
		}
		sb.append(name);
		sb.append("(");
		if (!inputArgs.isEmpty()) {
			Iterator<String> iter = inputArgs.keySet().iterator();
			String key = iter.next(); // key形参名
			JavaParameter param = inputArgs.get(key);
			param.genetateCode(this, main, sb, varArg && !iter.hasNext());
			for (; iter.hasNext();) {
				key = iter.next();
				sb.append(",");
				param = inputArgs.get(key);
				if (param == null)
					continue;
				param.genetateCode(this, main, sb, varArg && !iter.hasNext());
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
		boolean hasReturn = false;
		for (String str : super.getContent()) {
			sb.append("\t\t");
			sb.append(str).append("\r\n");
			if (checkReturn && str.startsWith("return ")) {
				hasReturn = true;
			}
		}
		if (checkReturn && returnType != null && !hasReturn) {
			sb.append("\t\treturn " + IClassUtil.defaultValue(returnType) + ";\n");
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
		String key = sb.toString();
		return key;
	}

	static String toMethodKey(String name, List<IClass> inputArgs) {
		return toMethodKey(name, inputArgs.toArray(new IClass[inputArgs.size()]));
	}

	public String getKey() {
		List<IClass> set = new ArrayList<IClass>();
		for (JavaParameter jp : inputArgs.values()) {
			set.add(jp.getType());
		}
		return toMethodKey(name, set);
	}

	public String getName() {
		return name;
	}

	public int getModifier() {
		return modifier;
	}

	public String appendCode(String code) {
		try (JefStringReader reader = new JefStringReader(code)){
			;
			StringBuilder sb = new StringBuilder();
			int c;
			while ((c = reader.read()) > -1) {
				char ch = (char) c;
				if (ch == '\'') {
					sb.append("\"");
				} else {
					sb.append(ch);
				}
			}
			String str = sb.toString();
			super.addContent(str);
			return str;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
