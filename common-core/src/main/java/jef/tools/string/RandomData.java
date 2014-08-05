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
package jef.tools.string;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import jef.common.wrapper.IntRange;
import jef.tools.chinese.ChineseCharProvider;
import jef.tools.chinese.ChineseCharProvider.Type;
import jef.tools.reflect.BeanWrapper;
import jef.tools.reflect.ClassUtils;
import jef.tools.reflect.GenericUtils;
import jef.tools.reflect.UnsafeUtils;

/**
 * 生成随机数据，以及由随机数据填充的复杂bean
 * @author jiyi
 *
 */
public class RandomData {
	/**
	 * 复杂bean最大嵌套深度
	 */
	public static int MAX_LEVEL = 3;
	private static Class<? extends Annotation> clz_generatred;
	private static Class<? extends Annotation> clz_lob;
	private static Class<? extends Annotation> clz_column;
	private static Method length;

	static{
		try{
			clz_generatred=Class.forName("javax.persistence.GeneratedValue").asSubclass(Annotation.class);
		}catch(Exception e){
		}
		try{
			clz_lob=Class.forName("javax.persistence.Lob").asSubclass(Annotation.class);
		}catch(Exception e){
		}
		try{
			clz_column=Class.forName("javax.persistence.Column").asSubclass(Annotation.class);
			length=clz_column.getMethod("length");
		}catch(Exception e){
		}
		
	}
	private RandomData() {
	}

	private static Random rnd = new Random();

	public static <T> T newInstance(Class<T> clz) {
		return processBeanType(clz, 0); // 嵌套深度
	}
	
	public static void fill(Object bean) {
		fillValues(BeanWrapper.wrap(bean), 0); // 嵌套深度
	}

	private static <T> T processBeanType(Class<T> clz, int level) {
		if (level > MAX_LEVEL)
			return null;
		// 开始
		T instance = null;
		instance = UnsafeUtils.newInstance(clz);
		BeanWrapper bw = BeanWrapper.wrap(instance, BeanWrapper.NORMAL);
		fillValues(bw,level);
		return instance;
	}

	private static void fillValues(BeanWrapper bw,int level) {
		for (String name : bw.getRwPropertyNames()) {
			if (isGeneratedVal(bw,name))
				continue;
			java.lang.reflect.Type genericType = bw.getPropertyType(name);// 泛型类型
			
			Object value = newInstance(genericType, level, getEtaLength(bw,name));
			if (value != null)
				bw.setPropertyValue(name, value);
		}
	}

	private static int getEtaLength(BeanWrapper bw, String name) {
		if(clz_lob!=null){
			if(bw.getAnnotationOnField(name, clz_lob)!=null){
				return 85;
			}
			Object column=bw.getAnnotationOnField(name, clz_column);
			if(column!=null){
				try {
					Integer result=(Integer)length.invoke(column);
					return result==null?0:result.intValue()/2;
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			
		}
		return 0;
	}

	
	private static boolean isGeneratedVal(BeanWrapper bw, String name) {
		if(clz_generatred!=null){
			return bw.getAnnotationOnField(name, clz_generatred)!=null;
		}
		return false;
	}

	private static Object newInstance(java.lang.reflect.Type type, int level, int etaColumnLength) {
		if (type == Integer.class || type == Integer.TYPE) {
			return randomInteger(1, 1000);
		} else if (type == Short.class || type == Short.TYPE) {
			return (short) randomInteger(1, 1000);
		} else if (type == Long.class || type == Long.TYPE) {
			return randomLong(1, 9999999999L);
		} else if (type == Boolean.class || type == Boolean.TYPE) {
			return randomInteger(0, 2) > 0;
		} else if (type == Character.class || type == Character.TYPE) {
			return (char) randomInteger(1, 128);
		} else if (type == Byte.class || type == Byte.TYPE) {
			return randomByte();
		} else if (type == Double.class || type == Double.TYPE) {
			return randomDouble(0, 100);
		} else if (type == Float.class || type == Float.TYPE) {
			return randomFloat(0, 100);
		} else if (type == String.class) {
			if (etaColumnLength > 199) {
				return randomString(ChineseCharProvider.getInstance().get(Type.CHINESE_LAST_NAME), new IntRange(50, 200));
			} else if (etaColumnLength == 0) {
				return randomChineseName();
			} else {
				return randomString(CharUtils.ALPHA_NUM_UNDERLINE, new IntRange(1, etaColumnLength));
			}
		} else if (type == Date.class) {
			long cur = System.currentTimeMillis();
			return randomDate(new Date(cur - 300000), new Date(cur + 1000000));
		}
		Class<?> raw = GenericUtils.getRawClass(type);
		if (raw.isArray()) {
			return processArrayType(raw.getComponentType(), level + 1);
		} else if (List.class.isAssignableFrom(raw)) {
			return processListTypes(GenericUtils.getCollectionType(type), level + 1);
		} else if (Map.class.isAssignableFrom(raw)) {
			return processMapTypes(GenericUtils.getMapKeyAndValueTypes(type, Map.class), level + 1);
		} else if (Set.class.isAssignableFrom(raw)) {
			return processSetTypes(GenericUtils.getCollectionType(type), level + 1);
		} else if (ClassUtils.hasConstructor(raw)) {// 有空构造的对象就构造
			return processBeanType(raw, level + 1);
		}
		return null;
	}

	private static Object processArrayType(Class<?> componentType, int level) {
		if (componentType == byte.class) {
			return randomByteArray(1024);
		} else {
			Object obj = newInstance(componentType, level,0);
			if (obj == null)
				return null;
			Object array = Array.newInstance(componentType, 1);
			Array.set(array, 0, obj);
			return array;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object processSetTypes(java.lang.reflect.Type collectionType, int level) {
		Object obj = newInstance(collectionType, level,0);
		if (obj == null)
			return null;
		Set result = new HashSet();
		result.add(obj);
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object processMapTypes(java.lang.reflect.Type[] mapKeyAndValueTypes, int level) {
		Object key = newInstance(mapKeyAndValueTypes[0], level,0);
		Object value = newInstance(mapKeyAndValueTypes[1], level,0);
		if (key == null || value == null)
			return null;
		Map result = new HashMap();
		result.put(key, value);
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object processListTypes(java.lang.reflect.Type collectionType, int level) {
		Object obj = newInstance(collectionType, level,0);
		if (obj == null)
			return null;
		List result = new ArrayList();
		result.add(obj);
		return result;
	}

	// 生成随机的日期
	public static Date randomDate(Date startDate, Date endDate) {
		long start = startDate.getTime()/1000;
		long end = endDate.getTime()/1000;
		return new Date(randomLong(start, end)*1000);
	}

	// 生成随机的整数
	public static int randomInteger(int start, int end) {
		return start + rnd.nextInt(end - start);
	}

	public static long randomLong(long start, long end) {
		int i = (int) (end - start);
		if(start+i!=end){
			i=Integer.MAX_VALUE;
		}else if (i < 0)
			i = -i;
		return start + rnd.nextInt(i);
	}

	public static float randomFloat(float start, float end) {
		int n = (int) (end - start);
		return start + rnd.nextFloat() + n > 0 ? rnd.nextInt(n) : 0;
	}

	public static double randomDouble(double start, double end) {
		int n = (int) (end - start);
		return start + rnd.nextFloat() + n > 0 ? rnd.nextInt(n) : 0;
	}

	// 生成中文数据
	public static String randomChineseName(int min, int max) {
		return randomString(ChineseCharProvider.getInstance().get(Type.CHINESE_LAST_NAME), new IntRange(min, max));
	}

	// 生成中文名称
	public static String randomChineseName() {
		return randomChineseName(2, 3);
	}

	public static String randomString(int maxLen) {
		IntRange range=new IntRange(1, maxLen);
		return randomString(CharUtils.ALPHA_NUM_UNDERLINE, range);
	}
	
	public static String randomString(int min,int maxLen) {
		IntRange range=new IntRange(min, maxLen);
		return randomString(CharUtils.ALPHA_NUM_UNDERLINE, range);
	}
	
	
	/**
	 * 生成随机的字符串
	 * 
	 * @param range
	 * @param length
	 * @return
	 */
	public static String randomString(String range, IntRange length) {
		return randomString(range.toCharArray(), length);
	}

	// 生成随机的字符串
	public static String randomString(char[] range, IntRange length) {
		StringBuilder sb = new StringBuilder();
		int width = range.length;
		int realLen = rnd.nextInt(length.getEnd() - length.getStart() + 1) + length.getLeastValue();
		for (int n = 0; n <= realLen - 1; n++) {
			sb.append(range[rnd.nextInt(width)]);
		}
		return sb.toString();
	}

	/**
	 * 生成随机的一个char
	 * 
	 * @param length
	 * @return
	 */
	public static char randomChar(int length) {
		return (char) randomInteger(1, 65536);
	}

	/**
	 * 生成随机的一个字节
	 * 
	 * @return
	 */
	public static byte randomByte() {
		return (byte) (rnd.nextInt(256) - 128);
	}

	/**
	 * 生成随机的字节组
	 * 
	 * @param length
	 * @return
	 */
	public static byte[] randomByteArray(int length) {
		byte[] b = new byte[length];
		rnd.nextBytes(b);
		return b;
	}
	
	public static String[] randomStringArray(int length,int stringLegth){
		String[] ss=new String[length];
		for(int i=0;i<length;i++){
			ss[i]=randomString(stringLegth);
		}
		return ss;
	}

	// 生成枚举类的项目
	public static <T extends Enum<T>> T randomEnum(Class<T> c) {
		T[] enums = c.getEnumConstants();
		int n = rnd.nextInt(enums.length);
		return enums[n];
	}

	// 返回枚局的元素
	public static <T> T randomElement(T[] enums) {
		int n = rnd.nextInt(enums.length);
		return enums[n];
	}

	/**
	 * 创建多个实例
	 * 
	 * @param <T>
	 * @param class1
	 * @param i
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] newArrayInstance(Class<T> class1, int i) {
		T[] array = (T[]) Array.newInstance(class1, i);
		for (int n = 0; n < i; n++) {
			array[n] = newInstance(class1);
		}
		return array;
	}

	public static boolean randomBoolean() {
		return rnd.nextBoolean();

	}
}
