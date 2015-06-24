package jef.accelerator.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import jef.tools.reflect.Property;

/**
 * 使用switcher算法的抽象类
 * @author Administrator
 *
 */
public abstract class SwitchBeanAccessor extends BeanAccessor{
	private Collection<? extends Property> properties;
	protected Map<Class<?>, Annotation>[] fieldAnnoMaps;
	protected Map<Class<?>, Annotation>[] setterAnnoMaps;
	protected Map<Class<?>, Annotation>[] getterAnnoMaps;
	//泛型类型很难用代码描述，因此这里将凡是泛型的类型变量保存下来
	protected Type[] genericType;
	
	public Property getProperty(String name) {
		try{
			Type t=this.getGenericType(name);
			Class<?> c=this.getPropertyType(name);
			//如果上面两步属性不存在，已经抛出NoSuchElemenetException
			return new FastProperty(this,name,t,c);
		}catch(NoSuchElementException e){
			return null;
		}
	}
	
	public Collection<? extends Property> getProperties() {
		if(properties==null){
			List<FastProperty> pps=new ArrayList<FastProperty>(getPropertyNames().size());
			for(String s:getPropertyNames()){
				Type t=this.getGenericType(s);
				Class<?> c=this.getPropertyType(s);
				pps.add(new FastProperty(this,s,t,c));
			}
			properties=pps;
			return properties;
		}
		return properties;
	}
	
	public void initAnnotations(Map<Class<?>,Annotation>[] field,Map<Class<?>,Annotation>[] getter,Map<Class<?>,Annotation>[] setter){
		this.fieldAnnoMaps=field;
		this.getterAnnoMaps=getter;
		this.setterAnnoMaps=setter;
	}
	
	public void initNthGenericType(int index,Class<?> raw,Type type,int total,String fieldName){
		if(genericType==null){
			genericType=new Type[total];
		}
		genericType[index]=type;
	}
	
}
