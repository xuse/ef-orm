package jef.accelerator.bean;

import java.lang.reflect.Type;
import java.util.NoSuchElementException;


/**
 * 基于ASM实现的属性反射操作。
 * 
 * 目前针对单个属性进行get/set有三种实现：
 * ASM-HASH   ASM-SWITCH  UNSAFE-FieldAccessor
 * 目前统计性能如下：（单位ns,测试机i7920，大约比i5 4740延迟多一倍）
 * 
 * Hash get= 49~80  set 68~75
 * Switch get 109      set 140~149
 * FieldAccessor     get 99~101   set 234~253
 * 
 * 从上面可以看出Hash算法下每个Property都是动态类，实质是通过静态代码 非反射直接调用get set方法，速度接近实际get set.
 * switch方法由于要做一次基于Hash的Switch，因此要慢。但在分支较少的情况下速度也慢不到哪里去，考虑到字段多的情况，也到不了200ns。
 * Unsafe方法实质是反射，无非是跳过了JDK的一些校验，因此比JDK反射要快。但是从实现来看，读的速度挺快接近真实get方法，但是写的速度慢了很多。
 * @author Administrator
 *
 */
final class FastProperty extends AbstractFastProperty{
	private String name;
	private BeanAccessor accessor;
	
	FastProperty(BeanAccessor accessor,String name,Type type,Class<?> raw){
		this.name=name;
		this.accessor=accessor;
		this.rawType=raw;
		this.genericType=type;
	}
	
	public String getName() {
		return name;
	}

	public Object get(Object obj) {
		return accessor.getProperty(obj, name);
	}

	public void set(Object obj, Object value) {
		boolean flag=accessor.setProperty(obj, name, value);
		if(flag==false){
			throw new NoSuchElementException("There's no accessable field "+ name +" in bean.");
		}
	}
}
