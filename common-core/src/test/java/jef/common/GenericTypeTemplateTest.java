package jef.common;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.ClassWrapper;
import jef.tools.reflect.FieldEx;
import jef.tools.reflect.GenericUtils;
import jef.tools.reflect.MethodEx;
import jef.tools.reflect.TypeToken;

import org.junit.Test;



public class GenericTypeTemplateTest<T extends Date> {
	public String[][] field1;
	public GenericTypeTemplateTest<java.sql.Date>[][] field2;
	
	
	public <X extends T,V extends java.lang.CharSequence> Entry<Map<Entry<String, ? extends Comparable<V>>,Long[]>,Set<? extends X>>[] test1(){
		return null;
	}
	
	public <X extends T> X test2(){
		return null;
	}
	
	public Set<? extends T> test3(){
		return null;
	}
	
	public Map<String,Object> test4(Map<String,String> map){
		return null;
	}
	
	public static class XType<T extends CharSequence>{
		public int main(T args) {
			return args.length();
		}
	}
	
	@Test
	public void testmain() throws SecurityException, NoSuchMethodException {
		System.out.println("====  testmain ====");
		//TypeToken 通过子类继承来描述出一个泛型类的实际类型。
		TypeToken<GenericTypeTemplateTest<Timestamp>> c=new TypeToken<GenericTypeTemplateTest<Timestamp>>() {};//得到泛型类的实例类型。
		Type type1=GenericUtils.resolve(null, c.getType());
		System.out.println(type1);
	}
	
	@Test
	public void testgeneric2()throws SecurityException, NoSuchMethodException {
		System.out.println("====  testgeneric2 ====");
		Type c=new TypeToken<GenericTypeTemplateTest<java.sql.Date>>() {}.getType();//得到泛型类的实例类型。
		
		Type context=GenericUtils.resolve(null,c);
		
		Method m=GenericUtils.getRawClass(c).getMethod("test1");						//得到方法
		Type methodReturn= GenericUtils.resolve2(c, m.getGenericReturnType());
		System.out.println(methodReturn);
		
		//用非泛型的实例无法计算出类型
		System.out.println("====  testgeneric2b ====");
		methodReturn=GenericUtils.resolve(null,GenericUtils.getRawClass(c));
		System.out.println(methodReturn);
		
	}

	@Test
	public void testgeneric4()throws SecurityException, NoSuchMethodException {
		System.out.println("====  testgeneric4 ====");
		
		Type type=GenericUtils.newArrayType(String.class);
		System.out.println(type.getClass().getName());
		System.out.println("isRawArray:"+GenericUtils.isRawArray(type));
		System.out.println("rawType:"+GenericUtils.getRawClass(type));
		
		Class<?> c=new String[0].getClass();
		System.out.println(c.getClass().getName());
		System.out.println(c.getName());
		System.out.println(c);
		
		System.out.println(type.equals(c));
		System.out.println("====  testgeneric4b ====");
		FieldEx field=BeanUtils.getField(GenericTypeTemplateTest.class, "field1");
		System.out.println(field.getType().equals(c));
		System.out.println(field.getGenericType().equals(c));//当不是泛型时，返回的是class对象
		System.out.println(field.getType());
		System.out.println(field.getGenericType());
		System.out.println(type);
		System.out.println(field.getGenericType().equals(type));
		
		
		System.out.println("====  testgeneric4c ====");
		field=BeanUtils.getField(GenericTypeTemplateTest.class, "field2");
		System.out.println(field.getType());
		System.out.println(field.getGenericType());
		type=field.getGenericType();
		System.out.println("isRawArray:"+GenericUtils.isRawArray(type));
		System.out.println("rawType:"+GenericUtils.getRawClass(type));
		System.out.println(GenericUtils.getRawClass(type).equals(field.getType()));
	}
	
	@Test
	public void testgeneric5()throws SecurityException, NoSuchMethodException {
		System.out.println("====  testgeneric5 ====");
		Type map1=GenericUtils.newMapType(String.class, String.class);
		Type map2=GenericUtils.newMapType(String.class, Object.class);
		Type map3=Map.class;
		Type c=GenericUtils.newGenericType(GenericTypeTemplateTest.class, java.sql.Date.class);
		ClassWrapper cw=new ClassWrapper(c);
		MethodEx method=cw.getFirstMethodByName("test4");
		System.out.println(map1.equals(method.getGenericParameterTypes()[0]));
		System.out.println(map2.equals(method.getGenericReturnType()));
	}
	
	@Test
	public void testgeneric3()throws SecurityException, NoSuchMethodException {
		
		System.out.println("====  testgeneric3 ====");
		Type c1=new TypeToken<GenericTypeTemplateTest<java.sql.Date>>() {}.getType();//得到泛型类的实例类型。
		Type c=GenericUtils.newGenericType(GenericTypeTemplateTest.class, java.sql.Date.class);
		
		ClassWrapper cw=new ClassWrapper(c);
		MethodEx method=cw.getFirstMethodByName("test1");
		Type type=method.getGenericReturnType();
		Type result=BeanUtils.getBoundType(type,cw);
		System.out.println("result:" + result);
		
//		Type type=method.getGenericReturnType();
//		ParameterizedType pType=(ParameterizedType)type;
//		
//		
//		
//		System.out.println(pType.getActualTypeArguments()[0]);
//		WildcardType wt=(WildcardType)pType.getActualTypeArguments()[0];
//		TypeVariable upper=(TypeVariable)wt.getUpperBounds()[0];
//		Type context=cw.getImplType(upper);
//		System.out.println(context);
//		
//		Type result=GenericUtils.newGenericType((Class<?>)pType.getRawType(), context);
//		System.out.println(result);
		
	
		
	
	}

}
