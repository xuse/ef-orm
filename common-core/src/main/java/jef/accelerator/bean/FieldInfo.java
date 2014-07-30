package jef.accelerator.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.IdentityHashMap;

import jef.tools.reflect.GenericUtils;

import org.easyframe.fastjson.util.ASMUtils;

@SuppressWarnings("rawtypes")
public class FieldInfo {
	private String name;
	
	//属性泛型类型
	private Type type;
	//属性非泛型类型
	private Class<?> rawType;
	
	private Method getter;
	
	private Method setter;
	
	private IdentityHashMap<Class,Annotation> annoOnField;
	private IdentityHashMap<Class,Annotation> annoOnGetter;
	private IdentityHashMap<Class,Annotation> annoOnSetter;
	
	public void setGetter(Method getter) {
		this.getter=getter;
	}
	public void setSetter(Method setter) {
		this.setter = setter;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Class getRawType() {
		return rawType;
	}
	public boolean isGeneric(){
		return !(type instanceof Class);
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
		this.rawType=GenericUtils.getRawClass(type);
	}
	public String getTypeDesc(){
		return ASMUtils.getDesc(rawType);
	}
	//转换为在定义中可出现的类型
	public String getTypeCastName() {
		return getTypeString(rawType);
	}
	private static String getTypeString(Type t) {
		if(t instanceof Class){
			Class clz=(Class)t;
			if(clz.isArray()){
				String rawStr=getTypeString(clz.getComponentType());
				return rawStr.concat("[]");
			}else{
				return clz.getName();
			}	
		}else{
			return t.toString();
		}
	}
	
	
	public Method getGetter() {
		return getter;
	}
	public Method getSetter() {
		return setter;
	}
	public IdentityHashMap<Class, Annotation> getAnnoOnField() {
		return annoOnField;
	}
	public void setAnnoOnField(IdentityHashMap<Class, Annotation> annoOnField) {
		this.annoOnField = annoOnField;
	}
	public IdentityHashMap<Class, Annotation> getAnnoOnGetter() {
		return annoOnGetter;
	}
	public void setAnnoOnGetter(IdentityHashMap<Class, Annotation> annoOnGetter) {
		this.annoOnGetter = annoOnGetter;
	}
	public IdentityHashMap<Class, Annotation> getAnnoOnSetter() {
		return annoOnSetter;
	}
	public void setAnnoOnSetter(IdentityHashMap<Class, Annotation> annoOnSetter) {
		this.annoOnSetter = annoOnSetter;
	}
	public boolean isPrimitive() {
		return rawType.isPrimitive();
	}
	@Override
	public String toString() {
		return "{"+this.name+":[\""+this.getter.getName()+"\", \""+this.setter.getName()+"\"], hash:"+ this.name.hashCode()+"}";
	}
}
