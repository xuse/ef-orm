package jef.accelerator.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jef.tools.reflect.Property;

/**
 * bean快速访问接口
 * @author Administrator
 *
 * @param <T>
 */
@SuppressWarnings("rawtypes")
public abstract class BeanAccessor {
	/**
	 * 得到bean的全部属性
	 * @return 全部属性名
	 */
	public abstract Collection<String> getPropertyNames();
	/**
	 * 得到bean的某个属性类型
	 * @param name 属性名
	 * @return
	 * @throws NoSuchElementException 如果该属性不存在，抛出NoSuchElementException。
	 */
	public abstract Class<?> getPropertyType(String name);
	
	/**
	 * 得到bean的属性的泛型类型
	 * @param name 属性名
	 * @return
	 * @throws NoSuchElementException 如果该属性不存在，抛出NoSuchElementException。
	 */
	public abstract Type getGenericType(String name);
	
	/**
	 * 得到bean的属性值
	 * @param bean
	 * @param name
	 * @return
	 * @throws NoSuchElementException 如果该属性不存在，抛出NoSuchElementException。
	 */
	public abstract Object getProperty(Object bean,String name);
	/**
	 * 设置bean属性值
	 * @param bean
	 * @param name
	 * @param v
	 * @return 返回true设置成功，false设置失败(注意此时不会抛出异常)
	 */
	public abstract boolean setProperty(Object bean,String name,Object v);
	/**
	 * 在两个bean间快速拷贝
	 * @param o1
	 * @param o2
	 */
	public abstract void copy(Object o1,Object o2);
	
	/**
	 * 得到某个指定ProperyHolder
	 * @param name
	 * @return 如果Property不存在，返回null(注意此时不会抛出异常)
	 */
	public abstract Property getProperty(String name);
	
	/**
	 * 得到全部属性
	 * @return
	 */
	public abstract Collection<? extends Property> getProperties();
	/**
	 * 得到位于field上的annotation
	 * @param name
	 * @return 包含各种Annotation的Map
	 * @throws NoSuchElementException 如果该属性不存在，抛出NoSuchElementException。
	 */
	public abstract IdentityHashMap<Class,Annotation> getAnnotationOnField(String name);
	
	/**
	 * 得到位于getter上的annotation
	 * @param name
	 * @return 包含各种Annotation的Map
	 * @throws NoSuchElementException 如果该属性不存在，抛出NoSuchElementException。
	 */
	public abstract IdentityHashMap<Class,Annotation> getAnnotationOnGetter(String name);
	/**
	 * 得到位于属性setter上的annotation
	 * @param name
	 * @return 包含各种Annotation的Map
	 * @throws NoSuchElementException 如果该属性不存在，抛出NoSuchElementException。
	 */
	public abstract IdentityHashMap<Class,Annotation> getAnnotationOnSetter(String name);

	/**
	 * 构造一个实例，注意类必须有空构造方法
	 * @return 类的实例
	 */
	public abstract Object newInstance();
	
	
	public abstract Class<?> getType(); 
	
	/*
	 * 框架内部使用，初始化所有的AnnotationMap
	 */
	public abstract void initAnnotations(IdentityHashMap<Class,Annotation>[] field,IdentityHashMap<Class,Annotation>[] getter,IdentityHashMap<Class,Annotation>[] setter);
	public abstract void initNthGenericType(int index,Class raw,Type type,int total,String fieldName);
	
	
	/**
	 * 得到指定Field上的Annotation
	 * @param field
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getFieldAnnotation(String field,Class<T> type){
		Map<Class,Annotation> anns=getAnnotationOnField(field);
		if(anns==null)return null;
		return (T)anns.get(type);
	}
	
	
}
