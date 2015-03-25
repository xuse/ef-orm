package com.github.geequery.codegen;

import java.util.Iterator;
import java.util.Map;

import jef.common.SimpleMap;
import jef.common.log.LogUtil;

import org.junit.Test;

public class QcTest {
//	public static void main(String[] args) throws CannotCompileException, NotFoundException, ReflectionException {
//		ClassPool pool=ClassPool.getDefault();
//		CtClassWrapper clz=CtClassWrapper.newClass(pool,"com.jef.du1.Maked");
//		clz.addFieldWithGetterAndSetter(String.class, "testField");
//		
//		JavaMethod method=new JavaMethod("helloMe");
//		method.addparam(Integer.class, "arg1");
//		method.addparam(String.class, "arg2");
//		method.addContent("System.out.println($1+$2+testField);");
//		clz.addMethod(method);
//
//		ClassWrapper clazz=clz.toClass();
//		System.out.println(clazz.getName());
//		Object myObj=clazz.newInstance();
//		BeanUtils.setFieldValue(myObj, "testField", "csdsdfsdfsfs");
//		BeanUtils.invokeMethod(myObj, "helloMe", Integer.valueOf(999),"xfire");
//	}
	
	@Test
	public void test2(){
		Map<String,Integer> map=new SimpleMap<String,Integer>();
		map.put("1", 1);
		map.put("2", 2);
		map.put("3", 3);
		map.put("4", 4);
		map.put("5", 5);
		map.put("6", 6);
		map.put("7", 7);
		map.put("4", 3333);
		LogUtil.show(map);
		System.out.println(map.size());
		for(Iterator<String> iter=map.keySet().iterator();iter.hasNext();){
			String key=iter.next();
			if("3".equals(key)){
				iter.remove();
			}
		}
		LogUtil.show("=====================");
		LogUtil.show(map);
		map.remove("5");
		LogUtil.show("=====================");
		LogUtil.show(map);
		
	}
	
	
	
}
