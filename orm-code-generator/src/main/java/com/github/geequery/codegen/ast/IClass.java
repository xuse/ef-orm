package com.github.geequery.codegen.ast;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.GenericUtils;

public interface IClass {
	static final String[] PRIMITIVE_TYPES= {"int","short","long","float","double","boolean","char","byte"};
	static final String[] WRAPPED_TYPES= {"Integer","Short","Long","Float","Double","Boolean","Character","Byte"};
	//得到类名
	String getName();
	//得到简称
	String getSimpleName();
	//是否数组
	boolean isArray();
	//是否原生
	boolean isPrimitive();
	//得到数组的元素类型
	IClass getComponentType();
	
	String toSimpleString();
	
	Type toJavaType()throws ClassNotFoundException;
	
	public static class GenericClass implements IClass {
		IClass raw;
		List<IClass> paramTypes;

		GenericClass(String k, String... vs) {
			this.raw = new VirtualClass(k);
			paramTypes = new ArrayList<IClass>();
			for (String v : vs) {
				paramTypes.add(new VirtualClass(v));
			}
		}
		
		GenericClass(IClass k, IClass... vs) {
			this.raw = k;
			paramTypes = new ArrayList<IClass>();
			paramTypes.addAll(Arrays.asList(vs));
		}

		public void addGenericParamType(IClass type) {
			paramTypes.add(type);
		}

		public String getName() {
			return raw.getName();
		}

		public String getSimpleName() {
			return raw.getSimpleName();
		}

		public boolean isArray() {
			return raw.isArray();
		}

		public IClass getComponentType() {
			return raw.getComponentType();
		}

		public IClass getRawClass() {
			return raw;
		}

		public List<IClass> getParamTypes() {
			return paramTypes;
		}
		public boolean isPrimitive() {
			return raw.isPrimitive();
		}

		@Override
		public String toString() {
			 StringBuilder sb=new StringBuilder();
			 if(raw.isArray()){
				 sb.append(raw.getComponentType().toString());
			 }else{
				 sb.append(raw.toString());
			 }
			 if(paramTypes!=null && !paramTypes.isEmpty()){
				 sb.append('<');
				 int n=0;
				 for(IClass c:paramTypes){
					 if(n>0)sb.append(',');
					 sb.append(c.toString());
					 n++;
				 }
				 sb.append('>');	 
			 }
			 if(raw.isArray()){
				 sb.append("[]");
			 }
			return sb.toString();
		}

		public String toSimpleString() {
			StringBuilder sb=new StringBuilder();
			if(raw.isArray()){
				sb.append(raw.getComponentType().toSimpleString());
			}else{
				sb.append(raw.toSimpleString());
			}
			if(paramTypes!=null && !paramTypes.isEmpty()){
				sb.append('<');
				int n=0;
				for(IClass c:paramTypes){
					if(n>0)sb.append(',');
					sb.append(c.toSimpleString());
					n++;
				}
				sb.append('>');	 
			}
			if(raw.isArray()){
				sb.append("[]");
			}
			return sb.toString();
		}

		public Type toJavaType() throws ClassNotFoundException{
			Class<?> rawType=(Class<?>)raw.toJavaType();
			Type[] types=new Type[this.paramTypes.size()];
			for(int i=0;i<paramTypes.size();i++){
				types[i]=paramTypes.get(i).toJavaType();
			}
			return GenericUtils.newGenericType(rawType, types);
		}
	}

	public static class VirtualClass implements IClass {
		private String name;

		VirtualClass(String c) {
			Assert.isNotEmpty(c);
			this.name = c;
		}

		public String getName() {
			return name;
		}

		public boolean isArray() {
			if (name.endsWith("[]"))
				return true;
			if (name.startsWith("["))
				return true;
			return false;
		}

		public IClass getComponentType() {
			if (name.endsWith("[]")) {
				return new VirtualClass(StringUtils.substringBeforeLast(name, "["));
			} else if ("[I".equals(name)) {
				return new RealClass(new int[0].getClass());
			} else if ("[B".equals(name)) {
				return new RealClass(new byte[0].getClass());
			} else if ("[Z".equals(name)) {
				return new RealClass(new boolean[0].getClass());
			} else if ("[C".equals(name)) {
				return new RealClass(new char[0].getClass());
			} else if ("[D".equals(name)) {
				return new RealClass(new double[0].getClass());
			} else if ("[F".equals(name)) {
				return new RealClass(new float[0].getClass());
			} else if ("[J".equals(name)) {
				return new RealClass(new long[0].getClass());
			} else if ("[S".equals(name)) {
				return new RealClass(new short[0].getClass());
			} else if (name.startsWith("[L")) {
				return new VirtualClass(StringUtils.substringBetween(name, "[L", ";"));
			} else {
				return null;
			}
		}

		public String getSimpleName() {
			String n = name;
			if (name.indexOf('<') > -1) {
				n = StringUtils.substringBefore(name, "<");
			}
			if (n.indexOf('.') > -1)
				return StringUtils.substringAfterLast(n, ".");
			return n;
		}

		@Override
		public int hashCode() {
			return getName().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof IClass) {
				return getName().equals(((IClass) obj).getName());
			} else if (obj instanceof String) {
				return getName().equals((String) obj);
			}
			return false;
		}

		public boolean isPrimitive() {
			return ArrayUtils.contains(PRIMITIVE_TYPES, name);
		}

		@Override
		public String toString() {
			return name;
		}

		public String toSimpleString() {
			return getSimpleName();
		}

		public Type toJavaType() throws ClassNotFoundException {
			if(this.isArray()){
				return GenericUtils.newArrayClass(this.getComponentType().toJavaType());
			}else{
				return Class.forName(name);
			}
		}
	}

	public static class RealClass implements IClass {
		RealClass(Class<?> c) {
			this.cls = c;
		}

		private Class<?> cls;

		public String getName() {
			return cls.getName().replace('$', '.');
		}

		public boolean isArray() {
			return cls.isArray();
		}

		public IClass getComponentType() {
			return new RealClass(cls.getComponentType());
		}

		public String getSimpleName() {
			return cls.getSimpleName();
		}

		@Override
		public int hashCode() {
			return getName().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof IClass) {
				return getName().equals(((IClass) obj).getName());
			} else if (obj instanceof String) {
				return getName().equals((String) obj);
			}
			return false;
		}

		public boolean isPrimitive() {
			return cls.isPrimitive();
		}

		@Override
		public String toString() {
			return cls.getName();
		}

		public Class<?> getCls() {
			return cls;
		}

		public String toSimpleString() {
			return getSimpleName();
		}

		public Type toJavaType() throws ClassNotFoundException {
			return cls;
		}
	}
}
