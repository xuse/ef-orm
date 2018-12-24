package jef.tools.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * 使用Unsafe的字段反射程序
 * 
 * @author Administrator
 *
 */
@SuppressWarnings("restriction")
public abstract class FieldAccessor implements Property{
	protected long offset;
	protected Object staticBase;
	private String name;

	public abstract Object get(Object bean);
	
	public abstract void set(Object bean, Object value);
	
	/**
	 * 获得Bean中的字段内容，并转换为需要的格式
	 * @param bean
	 * @param clz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getObject(Object bean,Class<T> clz) {
		return (T) get(bean);
	};


	@Override
	public String getName() {
		return name;
	}

	@Override
	public Type getGenericType() {
		return getType();
	}

	final static class I extends FieldAccessor implements IntProperty{
		public Object get(Object bean) {
			return Integer.valueOf(UnsafeUtils.unsafe.getInt(staticBase == null ? bean : staticBase, offset));
		}

		public void set(Object bean, Object value) {
			if (value == null)
				return;
			UnsafeUtils.unsafe.putInt(staticBase == null ? bean : staticBase, offset, ((Integer) value).intValue());
		}

		public Class<?> getType() {
			return int.class;
		}

		@Override
		public int getInt(Object bean) {
			return UnsafeUtils.unsafe.getInt(staticBase == null ? bean : staticBase, offset);
		}

		@Override
		public void setInt(Object bean, int value) {
			UnsafeUtils.unsafe.putInt(staticBase == null ? bean : staticBase, offset, value);
		}
	}

	final static class S extends FieldAccessor {
		public Object get(Object bean) {
			return Short.valueOf(UnsafeUtils.unsafe.getShort(staticBase == null ? bean : staticBase, offset));
		}

		public void set(Object bean, Object value) {
			if (value == null)
				return;
			UnsafeUtils.unsafe.putShort(staticBase == null ? bean : staticBase, offset, ((Short) value).shortValue());
		}

		public Class<?> getType() {
			return short.class;
		}
	}

	final static class F extends FieldAccessor {
		public Object get(Object bean) {
			return Float.valueOf(UnsafeUtils.unsafe.getFloat(staticBase == null ? bean : staticBase, offset));
		}

		public void set(Object bean, Object value) {
			if (value == null)
				return;
			UnsafeUtils.unsafe.putFloat(staticBase == null ? bean : staticBase, offset, ((Float) value).floatValue());
		}

		public Class<?> getType() {
			return float.class;
		}
	}

	final static class B extends FieldAccessor {
		public Object get(Object bean) {
			return Byte.valueOf(UnsafeUtils.unsafe.getByte(staticBase == null ? bean : staticBase, offset));
		}

		public void set(Object bean, Object value) {
			if (value == null)
				return;
			UnsafeUtils.unsafe.putByte(staticBase == null ? bean : staticBase, offset, ((Byte) value).byteValue());
		}

		public Class<?> getType() {
			return byte.class;
		}
	}

	final static class Z extends FieldAccessor implements BooleanProperty{
		public Object get(Object bean) {
			return Boolean.valueOf(UnsafeUtils.unsafe.getBoolean(staticBase == null ? bean : staticBase, offset));
		}

		public void set(Object bean, Object value) {
			if (value == null)
				return;
			UnsafeUtils.unsafe.putBoolean(staticBase == null ? bean : staticBase, offset, ((Boolean) value).booleanValue());
		}

		public Class<?> getType() {
			return boolean.class;
		}

		@Override
		public boolean getBoolean(Object bean) {
			return UnsafeUtils.unsafe.getBoolean(staticBase == null ? bean : staticBase, offset);
		}

		@Override
		public void setBoolean(Object bean, boolean value) {
			UnsafeUtils.unsafe.putBoolean(staticBase == null ? bean : staticBase, offset, value);
		}
	}

	final static class J extends FieldAccessor {
		public Object get(Object bean) {
			return Long.valueOf(UnsafeUtils.unsafe.getLong(staticBase == null ? bean : staticBase, offset));
		}

		public void set(Object bean, Object value) {
			if (value == null)
				return;
			UnsafeUtils.unsafe.putLong(staticBase == null ? bean : staticBase, offset, ((Long) value).longValue());
		}

		public Class<?> getType() {
			return long.class;
		}
	}

	final static class D extends FieldAccessor {
		public Object get(Object bean) {
			return Double.valueOf(UnsafeUtils.unsafe.getDouble(staticBase == null ? bean : staticBase, offset));
		}

		public void set(Object bean, Object value) {
			if (value == null)
				return;
			UnsafeUtils.unsafe.putDouble(staticBase == null ? bean : staticBase, offset, ((Double) value).doubleValue());
		}

		public Class<?> getType() {
			return double.class;
		}
	}

	final static class C extends FieldAccessor {
		public Object get(Object bean) {
			return Character.valueOf(UnsafeUtils.unsafe.getChar(staticBase == null ? bean : staticBase, offset));
		}

		public void set(Object bean, Object value) {
			if (value == null)
				return;
			UnsafeUtils.unsafe.putDouble(staticBase == null ? bean : staticBase, offset, ((Character) value).charValue());
		}

		public Class<?> getType() {
			return char.class;
		}
	}

	final static class O extends FieldAccessor {
		private Class<?> type;

		O(Class<?> type) {
			this.type = type;
		}

		public Object get(Object bean) {
			return UnsafeUtils.unsafe.getObject(staticBase == null ? bean : staticBase, offset);
		}

		public void set(Object bean, Object value) {
			UnsafeUtils.unsafe.putObject(staticBase == null ? bean : staticBase, offset, value);
		}

		public Class<?> getType() {
			return type;
		}
	}

	/**
	 * A safe field accessor (but slow) This will work on some JVM which doesn't
	 * support 'sun.misc.Unsafe'.
	 */
	final static class Safe extends FieldAccessor {
		private Field field;

		Safe(Field field) {
			this.field = field;
			field.setAccessible(true);
		}

		public Object get(Object bean) {
			try {
				return field.get(bean);
			} catch (Exception e) {
				throw new IllegalArgumentException(field.getName() + " get error.", e);
			}
		}

		public void set(Object bean, Object value) {
			try {
				field.set(bean, value);
			} catch (Exception e) {
				throw new IllegalArgumentException(field.getName() + " set error.", e);
			}
		}

		public Class<?> getType() {
			return field.getType();
		}

		public boolean getBoolean(Object bean) {
			try {
				return field.getBoolean(bean);
			} catch (Exception e) {
				throw new IllegalArgumentException(field.getName() + " get error.", e);
			}
		}

		public void setBoolean(Object bean, boolean value) {
			try {
				field.setBoolean(bean, value);
			} catch (Exception e) {
				throw new IllegalArgumentException(field.getName() + " get error.", e);
			}
		}

		public int getInt(Object bean) {
			try {
				return field.getInt(bean);
			} catch (Exception e) {
				throw new IllegalArgumentException(field.getName() + " get error.", e);
			}
		}

		public void setInt(Object bean, int value) {
			try {
				field.setInt(bean, value);
			} catch (Exception e) {
				throw new IllegalArgumentException(field.getName() + " get error.", e);
			}
		}
	}

	public static FieldAccessor generateAccessor(Field field) {
		if (!UnsafeUtils.enable) {
			return new Safe(field);
		}
		FieldAccessor accessor;
		Class<?> c = field.getType();
		if (c == int.class) {
			accessor = new I();
		} else if (c == long.class) {
			accessor = new J();
		} else if (c == short.class) {
			accessor = new S();
		} else if (c == float.class) {
			accessor = new F();
		} else if (c == double.class) {
			accessor = new D();
		} else if (c == char.class) {
			accessor = new C();
		} else if (c == boolean.class) {
			accessor = new Z();
		} else if (c == byte.class) {
			accessor = new B();
		} else {
			accessor = new O(field.getType());
		}
		accessor.name=field.getName();
		try {
			if (Modifier.isStatic(field.getModifiers())) {
				accessor.staticBase = UnsafeUtils.getUnsafe().staticFieldBase(field);
				accessor.offset = UnsafeUtils.getUnsafe().staticFieldOffset(field);
			} else {
				accessor.offset = UnsafeUtils.getUnsafe().objectFieldOffset(field);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return accessor;
	}
}
