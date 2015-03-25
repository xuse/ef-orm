package com.github.geequery.codegen.ast;

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.github.geequery.codegen.ast.IClass.GenericClass;
import com.github.geequery.codegen.ast.IClass.RealClass;
import com.github.geequery.codegen.ast.IClass.VirtualClass;
import jef.common.log.LogUtil;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.GenericUtils;
import jef.tools.string.JefStringReader;

public class IClassUtil {
	static final char[] SEP = new char[] { '<', ',' };
	static final char[] CLOSE = new char[] { '>' };

	public static IClass generic(String rawClz,String... paramTypes){
		IClass raw=getIClass(rawClz);
		return generic(raw,paramTypes);
	}
	

	public static IClass generic(IClass raw,IClass... types){
		if(types.length==0)return raw;
		return new GenericClass(raw, types);
	}
	
	public static IClass generic(String rawClz, IClass... types){
		IClass raw=getIClass(rawClz);
		return generic(raw,types);
	}
	
	public static IClass generic(IClass raw, String... paramTypes){
		List<IClass> types = new ArrayList<IClass>();
		for(String s:paramTypes){
			types.add(getIClass(s));
		}
		return generic(raw,types.toArray(new IClass[types.size()]));
	}
	
	public static IClass getIClass(String name) {
		name=name.trim();
		Assert.isNotEmpty(name);
		int n = name.indexOf('<');
		if (n > -1) {
			String thisname=name.substring(0, n);
			if(name.endsWith("[]")){
				name=name.substring(0,name.length()-2);
				thisname+="[]";
			}
			IClass raw = getIClass(thisname);
			List<IClass> types = new ArrayList<IClass>();
			String paramStr = name.substring(n + 1);
			paramStr = StringUtils.substringBeforeLast(paramStr, ">");
			JefStringReader reader = new JefStringReader(paramStr);
			while (reader.nextChar() != -1) {
				try {
					StringBuilder sb = new StringBuilder();
					sb.append(reader.readUntilCharIs(SEP));
					if (reader.nextChar() == '<') {
						int left=1;
						sb.append((char)reader.read());
						while(left>0){
							int i=reader.read();
							if(i==-1){
								break;
							}
							char c=(char)i;
							if(c=='<'){
								left++;
							}else if(c=='>'){
								left--;
							}
							sb.append(c);
						}
					}else{
						reader.read();
					}
					String clz=StringUtils.trimToNull(sb.toString());
					if(clz!=null){
						types.add(getIClass(clz));	
					}
				} catch (IOException e) {
					LogUtil.exception(e);
				}
			}
			return new GenericClass(raw, types.toArray(new IClass[types.size()]));
		} else {
			return new VirtualClass(name);
		}
	}

	public static IClass getIClass(Type type) {
		type = GenericUtils.resolve(null, type);
		if (type instanceof Class) {
			return new RealClass((Class<?>) type);
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) type;
			IClass raw = new RealClass((Class<?>) pType.getRawType());
			IClass[] params = new IClass[pType.getActualTypeArguments().length];
			for (int n = 0; n < pType.getActualTypeArguments().length; n++) {
				params[n] = getIClass(pType.getActualTypeArguments()[n]);
			}
			return new GenericClass(raw, params);
		} else if (type instanceof GenericArrayType) {
			GenericArrayType array = (GenericArrayType) type;
			return new VirtualClass(array.getGenericComponentType().toString() + "[]");
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * 对于AST框架的getRaw功能
	 * @param clz
	 * @return
	 */
	public static IClass getRaw(IClass clz){
		if(clz instanceof GenericClass){
			return getRaw(((GenericClass)clz).getRawClass());
		}else if(clz instanceof RealClass){
			return clz; 
		}else if(clz instanceof VirtualClass){
			if(clz.getName().indexOf('<')<0)return clz;
			return IClassUtil.getIClass(StringUtils.substringBefore(clz.getName(), "<"));
		}
		throw new RuntimeException();
	}
	
	/**
	 * 为JavaUnit添加一个apache common log的对象
	 */
	public static boolean addCommonsLog(JavaUnit unit){
		JavaField field=unit.addField(Modifier.PROTECTED|Modifier.STATIC,"org.apache.commons.logging.Log","log");
		if(field==null)return false;
		unit.addImport("org.apache.commons.logging.LogFactory");
		field.setInitValue("LogFactory.getLog("+unit.getSimpleName()+".class)");
		return true;
	}
	
	/**
	 * 是否out
	 * @param param
	 * @return
	 */
	public static boolean isOut(JavaParameter param){
		for(String s:param.getAnnotation()){
			if("@Out".equals(s) || "@InOut".equals(s)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 是否In
	 * @param param
	 * @return
	 */
	public static boolean isIn(JavaParameter param){
		for(String s:param.getAnnotation()){
			if("@In".equals(s) || "@InOut".equals(s)){
				return true;
			}
		}
		return false;
	}
	
	public static String newInstance(JavaUnit unit,IClass clz,String... params){
		unit.addImport(clz);
		StringBuilder sb=new StringBuilder("new ").append(clz.toSimpleString());
		sb.append("(");
		for(int n=0;n<params.length;n++){
			sb.append(params[n]);
			if(n<params.length-1)sb.append(',');
		}
		sb.append(")");
		return sb.toString(); 
	}
	
	public static String defaultValue(IClass clz){
		if(!clz.isPrimitive())return "null";
		String name=clz.getName();
		if ("char".equals(name)) {
			return "(char)0";
		} else if ("byte".equals(name)) {
			return "(byte)0";
		} else if ("float".equals(name)) {
			return "0F";
		} else if ("double".equals(name)) {
			return "(double)0";
		} else if ("int".equals(name)) {
			return "0";
		} else if ("short".equals(name)) {
			return "(short)0";
		} else if ("long".equals(name)) {
			return "0L";
		} else if("boolean".equals(name)){
			return "false";
		}
		return "null";
	}
}
