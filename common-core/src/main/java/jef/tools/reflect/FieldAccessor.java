package jef.tools.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 使用Unsafe的字段反射程序
 * @author Administrator
 *
 */
@SuppressWarnings("restriction")
public abstract class FieldAccessor {
	public abstract Object getObject(Object bean);
	public abstract void set(Object bean, Object value);
	protected abstract Class<?> getType();
	protected long offset;
	protected Object staticBase;
	
	final static class I extends FieldAccessor{
		public Object getObject(Object bean) {
			return Integer.valueOf(UnsafeUtils.unsafe.getInt(staticBase==null?bean:staticBase,offset));
		}
		public void set(Object bean, Object value) {
			if(value==null)return;
			UnsafeUtils.unsafe.putInt(staticBase==null?bean:staticBase,offset, ((Integer)value).intValue());
		}
		protected Class<?> getType() {
			return int.class;
		}
	}
	
	final static class S extends FieldAccessor{
		public Object getObject(Object bean) {
			return Short.valueOf(UnsafeUtils.unsafe.getShort(staticBase==null?bean:staticBase,offset));
		}
		public void set(Object bean, Object value) {
			if(value==null)return;
			UnsafeUtils.unsafe.putShort(staticBase==null?bean:staticBase,offset, ((Short)value).shortValue());
		}
		protected Class<?> getType() {
			return short.class;
		}
	}
	
	final static class F extends FieldAccessor{
		public Object getObject(Object bean) {
			return Float.valueOf(UnsafeUtils.unsafe.getFloat(staticBase==null?bean:staticBase,offset));
		}
		public void set(Object bean, Object value) {
			if(value==null)return;
			UnsafeUtils.unsafe.putFloat(staticBase==null?bean:staticBase,offset, ((Float)value).floatValue());
		}
		protected Class<?> getType() {
			return float.class;
		}
	}
	
	final static class B extends FieldAccessor{
		public Object getObject(Object bean) {
			return Byte.valueOf(UnsafeUtils.unsafe.getByte(staticBase==null?bean:staticBase,offset));
		}
		public void set(Object bean, Object value) {
			if(value==null)return;
			UnsafeUtils.unsafe.putByte(staticBase==null?bean:staticBase,offset, ((Byte)value).byteValue());
		}
		protected Class<?> getType() {
			return byte.class;
		}
	}
	
	final static class Z extends FieldAccessor{
		public Object getObject(Object bean) {
			return Boolean.valueOf(UnsafeUtils.unsafe.getBoolean(staticBase==null?bean:staticBase,offset));
		}
		public void set(Object bean, Object value) {
			if(value==null)return;
			UnsafeUtils.unsafe.putBoolean(staticBase==null?bean:staticBase,offset, ((Boolean)value).booleanValue());
		}
		protected Class<?> getType() {
			return boolean.class;
		}
	}
	
	final static class J extends FieldAccessor{
		public Object getObject(Object bean) {
			return Long.valueOf(UnsafeUtils.unsafe.getLong(staticBase==null?bean:staticBase,offset));
		}
		public void set(Object bean, Object value) {
			if(value==null)return;
			UnsafeUtils.unsafe.putLong(staticBase==null?bean:staticBase,offset, ((Long)value).longValue());
		}
		protected Class<?> getType() {
			return long.class;
		}
	}
	
	final static class D extends FieldAccessor{
		public Object getObject(Object bean) {
			return Double.valueOf(UnsafeUtils.unsafe.getDouble(staticBase==null?bean:staticBase,offset));
		}
		public void set(Object bean, Object value) {
			if(value==null)return;
			UnsafeUtils.unsafe.putDouble(staticBase==null?bean:staticBase,offset, ((Double)value).doubleValue());
		}
		protected Class<?> getType() {
			return double.class;
		}
	}
	
	final static class C extends FieldAccessor{
		public Object getObject(Object bean) {
			return Character.valueOf(UnsafeUtils.unsafe.getChar(staticBase==null?bean:staticBase,offset));
		}
		public void set(Object bean, Object value) {
			if(value==null)return;
			UnsafeUtils.unsafe.putDouble(staticBase==null?bean:staticBase,offset, ((Character)value).charValue());
		}
		protected Class<?> getType() {
			return char.class;
		}
	}
	
	final static class O extends FieldAccessor{
		private Class<?> type;
		O(Class<?> type){
			this.type=type;
		}
		public Object getObject(Object bean) {
			return UnsafeUtils.unsafe.getObject(staticBase==null?bean:staticBase,offset);
		}
		public void set(Object bean, Object value) {
			UnsafeUtils.unsafe.putObject(staticBase==null?bean:staticBase, offset, value);
		}
		protected Class<?> getType() {
			return type;
		}
	}
	/**
	 * A safe field accessor (but slow)
	 * This will work on some JVM which doesn't support 'sun.misc.Unsafe'. 
	 */
	final static class Safe extends FieldAccessor{
		private Field field;
		Safe(Field field){
			this.field=field;
			field.setAccessible(true);
		}
		public Object getObject(Object bean) {
			try {
				return field.get(bean);
			} catch (Exception e) {
				throw new IllegalArgumentException(field.getName()+" get error.",e);
			}
		}
		public void set(Object bean, Object value) {
			try {
				field.set(bean, value);
			} catch (Exception e) {
				throw new IllegalArgumentException(field.getName()+" set error.",e);
			}
		}
		protected Class<?> getType() {
			return field.getType();
		}
	}
	
	public static FieldAccessor generateAccessor(Field field) {
		if(!UnsafeUtils.enable){
			return new Safe(field);
		}
		FieldAccessor accessor;
		Class<?> c=field.getType();
		if(c==int.class){
			accessor=new I();
		}else if(c==long.class){
			accessor=new J();
		}else if(c==short.class){
			accessor=new S();
		}else if(c==float.class){
			accessor=new F();
		}else if(c==double.class){
			accessor=new D();
		}else if(c==char.class){
			accessor=new C();
		}else if(c==boolean.class){
			accessor=new Z();
		}else if(c==byte.class){
			accessor=new B();
		}else{
			accessor=new O(field.getType());
		}
		try{
			if(Modifier.isStatic(field.getModifiers())){
				accessor.staticBase=UnsafeUtils.getUnsafe().staticFieldBase(field);
				accessor.offset=UnsafeUtils.getUnsafe().staticFieldOffset(field);
			}else{
				accessor.offset=UnsafeUtils.getUnsafe().objectFieldOffset(field);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return accessor;
	}
}
