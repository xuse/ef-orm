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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import jef.common.wrapper.IntRange;
import jef.tools.ArrayUtils;
import jef.tools.DateFormats;
import jef.tools.StringUtils;
import jef.tools.chinese.ChineseCharProvider;
import jef.tools.chinese.ChineseCharProvider.Type;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.BeanWrapper;
import jef.tools.reflect.ClassUtils;
import jef.tools.reflect.GenericUtils;
import jef.tools.reflect.UnsafeUtils;

/**
 * 生成随机数据，以及由随机数据填充的复杂bean
 * 
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

	static {
		try {
			clz_generatred = Class.forName("javax.persistence.GeneratedValue").asSubclass(Annotation.class);
		} catch (Exception e) {
		}
		try {
			clz_lob = Class.forName("javax.persistence.Lob").asSubclass(Annotation.class);
		} catch (Exception e) {
		}
		try {
			clz_column = Class.forName("javax.persistence.Column").asSubclass(Annotation.class);
			length = clz_column.getMethod("length");
		} catch (Exception e) {
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
		fillValues(bw, level);
		return instance;
	}

	private static void fillValues(BeanWrapper bw, int level) {
		for (String name : bw.getRwPropertyNames()) {
			if (isGeneratedVal(bw, name) || isIgnore(bw, name))
				continue;
			java.lang.reflect.Type genericType = bw.getPropertyType(name);// 泛型类型

			try {
				Object value = newInstance(genericType, level, asFieldGenerator(bw, name));
				if (value != null)
					bw.setPropertyValue(name, value);
			} catch (Exception e) {
				throw new IllegalArgumentException("random value of field [" + bw.getClassName() + "." + name + "] error!", e);
			}

		}
	}

	private static RandomValue asFieldGenerator(BeanWrapper bw, String name) {
		RandomValue g = bw.getAnnotationOnField(name, RandomValue.class);
		if (g != null) {
			return g;
		}
		if (clz_lob != null) {
			if (bw.getAnnotationOnField(name, clz_lob) != null) {
				return BeanUtils.asAnnotation(RandomValue.class, "length", 85);
			}
			Object column = bw.getAnnotationOnField(name, clz_column);
			if (column != null) {
				try {
					Integer result = (Integer) length.invoke(column);
					return result == null ? null : BeanUtils.asAnnotation(RandomValue.class, "length", (result.intValue() / 2) + 1);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		return DEFAULT;
	}

	private static final RandomValue DEFAULT = new GeneratorImpl();

	static class GeneratorImpl implements RandomValue {
		private long cur = System.currentTimeMillis();

		public Class<? extends Annotation> annotationType() {
			return RandomValue.class;
		}

		public boolean ignore() {
			return false;
		}

		public int length() {
			return 32;
		}

		public ValueType value() {
			return ValueType.AUTO;
		}

		public long numberMin() {
			return 0;
		}

		public long numberMax() {
			return 1000;
		}

		public String dateMin() {
			return DateFormats.DATE_TIME_CS.format(new Date(cur - 300000));
		}

		@Override
		public String dateMax() {
			return DateFormats.DATE_TIME_CS.format(new Date(cur + 1000000));
		}

		@Override
		public String[] options() {
			return ArrayUtils.EMPTY_STRING_ARRAY;
		}

		@Override
		public int count() {
			return 1;
		}

		@Override
		public String characters() {
			return "";
		}

		@Override
		public int minLength() {
			return 1;
		}

		@Override
		public String suffix() {
			return "";
		}

		@Override
		public String prefix() {
			return "";
		}
	}

	private static boolean isGeneratedVal(BeanWrapper bw, String name) {
		if (clz_generatred != null) {
			return bw.getAnnotationOnField(name, clz_generatred) != null;
		}
		return false;
	}

	private static boolean isIgnore(BeanWrapper bw, String name) {
		RandomValue anno = bw.getAnnotationOnField(name, RandomValue.class);
		return anno != null && anno.ignore();
	}

	private static Object newInstance(java.lang.reflect.Type type, int level, RandomValue anno) {
		int len = anno.length() < 0 ? 32 : anno.length();
		int minLen = anno.minLength() < 0 ? len : Math.min(len, anno.minLength());
		long nMin = anno.numberMin();
		long nMax = anno.numberMax();
		if (nMax - nMin == 0) {
			nMin = 0L;
			nMax = 1000L;
		}
		long cur = System.currentTimeMillis();
		Date dMin = new Date(cur + nMin * 1000);
		Date dMax = new Date(cur + nMax * 1000);
		// 如果没有配置dateMin或dateMax，就用numberMin或numberMax来作为日期的偏移量（单位秒）
		dMin = StringUtils.isEmpty(anno.dateMin()) ? dMin : DateFormats.DATE_TIME_CS.parse(anno.dateMin(), dMin);
		dMax = StringUtils.isEmpty(anno.dateMax()) ? dMax : DateFormats.DATE_TIME_CS.parse(anno.dateMax(), dMax);

		if (type == Integer.class || type == Integer.TYPE) {
			return randomInteger((int) nMin, (int) nMax);
		} else if (type == Short.class || type == Short.TYPE) {
			return (short) randomInteger((int) nMin, (int) nMax);
		} else if (type == Long.class || type == Long.TYPE) {
			return randomLong(nMin, nMax);
		} else if (type == Boolean.class || type == Boolean.TYPE) {
			return randomInteger(0, 2) > 0;
		} else if (type == Character.class || type == Character.TYPE) {
			return (char) randomInteger(1, 128);
		} else if (type == Byte.class || type == Byte.TYPE) {
			return randomByte();
		} else if (type == Double.class || type == Double.TYPE) {
			return randomDouble(nMin, nMax);
		} else if (type == Float.class || type == Float.TYPE) {
			return randomFloat(nMin, nMax);
		} else if (type == String.class) {
			switch (anno.value()) {
			case EMAIL:
				return prefix(randomEmail(), anno);
			case OPTIONS:
				return prefix(randomOption(anno.options()), anno);
			case PHONE:
				return prefix(randomPhone(), anno);
			case AUTO:
				return prefix(randomString(ChineseCharProvider.getInstance().get(Type.CHINESE_LAST_NAME), new IntRange(minLen, len)), anno);
			case NAME:
				return randomChineseName();
			case NUMBER:
				return prefix(String.valueOf(newInstance(Long.TYPE, level, anno)), anno);
			case ENGLISH_LOWER:
				return prefix(randomString(CharUtils.ALPHA_LOWERS, new IntRange(1, len)), anno);
			case ENGLISH_MIXED:
				return prefix(randomString(CharUtils.ALPHA_NUM_UNDERLINE, new IntRange(1, len)), anno);
			case ENGLISH_UPPER:
				return prefix(randomString(CharUtils.ALPHA_UPPERS, new IntRange(1, len)), anno);
			case GUID:
				return prefix(StringUtils.generateGuid(), anno);
			case RANGED_STRING:
				return prefix(
						anno.characters().isEmpty() ? randomString(ChineseCharProvider.getInstance().get(Type.CHINESE_LAST_NAME), new IntRange(len / 2, len))
								: randomString(anno.characters(), new IntRange(1, len)),
						anno);
			}
		} else if (type == Date.class) {
			return randomDate(dMin, dMax);
		} else if (type == LocalTime.class) {
			int time = randomInteger(0, 86400);
			return LocalTime.ofSecondOfDay(time);
		} else if (type == LocalDate.class) {
			return LocalDate.ofEpochDay(randomInteger(-300, 1000) + LocalDate.now().toEpochDay());
		} else if (type == LocalDateTime.class) {
			return LocalDateTime.ofInstant(randomDate(dMin, dMax).toInstant(), ZoneId.systemDefault());
		} else if (type == Instant.class) {
			return randomDate(dMin, dMax).toInstant();
		} else if (type == YearMonth.class) {
			return YearMonth.from(LocalDate.ofEpochDay(randomInteger(-300, 1000) + LocalDate.now().toEpochDay()));
		} else if (type == BigDecimal.class) {
			return BigDecimal.valueOf(randomLong(nMin, nMax));
		} else if (type == MonthDay.class) {
			return MonthDay.from(LocalDate.ofEpochDay(randomInteger(-300, 1000) + LocalDate.now().toEpochDay()));
		}
		Class<?> raw = GenericUtils.getRawClass(type);
		if (raw.isArray()) {
			return processArrayType(raw.getComponentType(), level + 1, anno);
		} else if (List.class.isAssignableFrom(raw)) {
			return processListTypes(GenericUtils.getCollectionType(type), level + 1, anno);
		} else if (Map.class.isAssignableFrom(raw)) {
			return processMapTypes(GenericUtils.getMapKeyAndValueTypes(type, Map.class), level + 1, anno);
		} else if (Set.class.isAssignableFrom(raw)) {
			return processSetTypes(GenericUtils.getCollectionType(type), level + 1, anno);
		} else if (ClassUtils.hasConstructor(raw)) {// 有空构造的对象就构造
			return processBeanType(raw, level + 1);
		}
		return null;
	}

	private static String prefix(String str, RandomValue anno) {
		if (anno.prefix().length() == 0 && anno.suffix().length() == 0) {
			return str;
		} else {
			return anno.prefix() + str + anno.suffix();
		}
	}

	private static String randomPhone() {
		return "13" + randomInteger(100000000, 999999999);
	}

	private static String randomOption(String[] options) {
		if (options == null || options.length == 0) {
			return null;
		}
		return options[rnd.nextInt(options.length)];
	}

	private static final String[] MAIL_DOMAIN = new String[] { ".com", ".org", ".cc", ".com.cn", ".gov", ".cn", ".com", ".net", ".com" };

	private static String randomEmail() {
		String s = randomString(CharUtils.ALPHA_NUM_UNDERLINE, new IntRange(4, 11)) + "@" + randomString(CharUtils.ALPHA_NUM_UNDERLINE, new IntRange(4, 11))
				+ randomOption(MAIL_DOMAIN);
		return s.toLowerCase();
	}

	private static Object processArrayType(Class<?> componentType, int level, RandomValue generator) {
		if (componentType == byte.class) {
			return randomByteArray(1024);
		} else {
			Object obj = newInstance(componentType, level, generator);
			if (obj == null)
				return null;
			Object array = Array.newInstance(componentType, generator.count());
			Array.set(array, 0, obj);
			for (int i = 1; i < generator.count(); i++) {
				Array.set(array, i, newInstance(componentType, level, generator));
			}
			return array;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object processSetTypes(java.lang.reflect.Type collectionType, int level, RandomValue generator) {
		Object obj = newInstance(collectionType, level, generator);
		if (obj == null)
			return null;
		Set result = new HashSet();
		for (int i = 0; i < generator.count(); i++) {
			result.add(obj);
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object processMapTypes(java.lang.reflect.Type[] mapKeyAndValueTypes, int level, RandomValue generator) {
		Object key = newInstance(mapKeyAndValueTypes[0], level, generator);
		Object value = newInstance(mapKeyAndValueTypes[1], level, generator);
		if (key == null || value == null)
			return null;
		Map result = new HashMap();
		result.put(key, value);
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object processListTypes(java.lang.reflect.Type collectionType, int level, RandomValue generator) {
		Object obj = newInstance(collectionType, level, generator);
		if (obj == null)
			return null;
		List result = new ArrayList();
		result.add(obj);
		return result;
	}

	// 生成随机的日期
	public static Date randomDate(Date startDate, Date endDate) {
		long start = startDate.getTime() / 1000;
		long end = endDate.getTime() / 1000;
		return new Date(randomLong(start, end) * 1000);
	}

	// 生成随机的整数
	public static int randomInteger(int start, int end) {
		return start + rnd.nextInt(end - start);
	}

	public static long randomLong(long start, long end) {
		int i = (int) (end - start);
		if (start + i != end) {
			i = Integer.MAX_VALUE;
		} else if (i < 0)
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
		IntRange range = new IntRange(1, maxLen);
		return randomString(CharUtils.ALPHA_NUM_UNDERLINE, range);
	}

	public static String randomString(int min, int maxLen) {
		IntRange range = new IntRange(min, maxLen);
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

	public static String[] randomStringArray(int length, int stringLegth) {
		String[] ss = new String[length];
		for (int i = 0; i < length; i++) {
			ss[i] = randomString(stringLegth);
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
	 * @param        <T>
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
