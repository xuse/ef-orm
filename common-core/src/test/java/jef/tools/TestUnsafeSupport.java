package jef.tools;

import java.lang.reflect.Field;

import junit.framework.TestCase;

@SuppressWarnings("restriction")
public class TestUnsafeSupport extends TestCase {

	private static sun.misc.Unsafe unsafe;
	private static Person person;

	long ageOffset;
	long nameOffset;
	long dateOffset;
	long arrayOffset;

	protected void setUp() throws Exception {
		Field field = null;
		try {
			field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			unsafe = (sun.misc.Unsafe) field.get(null);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		person = new Person();
		dateOffset = unsafe.objectFieldOffset(Person.class.getDeclaredField("date"));
		ageOffset = unsafe.objectFieldOffset(Person.class.getDeclaredField("age"));
		nameOffset = unsafe.objectFieldOffset(Person.class.getDeclaredField("name"));
		arrayOffset = unsafe.objectFieldOffset(Person.class.getDeclaredField("array"));
	}

	/**
	 * 一般Reflection的操作java.lang.reflect.*
	 */
	public void testOrdinaryReflection() {
		try {
			Class<?> personClazz = Person.class;
			Field __ageField = personClazz.getDeclaredField("age");
			assertEquals(0, __ageField.getInt(person));

			__ageField.set(person, 5);
			assertEquals(5, __ageField.getInt(person));
		} catch (Exception ex) {
			fail(ex.getLocalizedMessage());
		}
	}

	/**
	 * 测试sun.mic.Unsafe获取类Reflection以及初始化、修改对象状态等操作
	 * 
	 * 
	 */
	public void testUnsafeReflection() {
		try {
			// 获取field在class定义中的位移位置

			// getInt获取初始值
			// 需要查看一下unsafe.getInt(long)，确认这个long参数具体代表什么含义
			// 经测试offset为参数的话，JVM异常
			System.out.println(unsafe.getInt(person, ageOffset));
			System.out.println(unsafe.getObject(person, ageOffset));
			assertEquals(0, unsafe.getInt(person, ageOffset));
			// cas
			// public native boolean compareAndSwapInt(Object obj, long offset,
			// int expect, int update);
			unsafe.compareAndSwapInt(person, ageOffset, 0, 25);
			unsafe.compareAndSwapInt(person, ageOffset, 12, 36);// 不等的情况下不会更新

			assertEquals(25, unsafe.getInt(person, ageOffset));

			unsafe.putInt(person, ageOffset, 0);

			assertEquals(0, unsafe.getInt(person, ageOffset));

			// unsafe.putIntVolatile();
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getLocalizedMessage());
		}
	}
}