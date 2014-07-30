package jef.tools.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class JefGson {
	/**
	 * 将子类解析为带泛型参数的父类类型
	 * @param context
	 * @param subclass
	 * @param superclass
	 * @return
	 */
	public static Type getSuperType(Type context,Class<?> subclass,Class<?> superclass) {
		return $Gson$Types.getSupertype(context, subclass,superclass);
	}
	

	/**
	 * 得到继承上级所指定的泛型类型
	 * @param subclass
	 * @param superclass
	 * @return
	 */
	public static Type[] getTypeParameters(Class<?> subclass,Class<?> superclass) {
		if(superclass==null){//在没有指定父类的情况下，默认选择第一个接口
			if(subclass.getSuperclass()==Object.class && subclass.getInterfaces().length>0){
				superclass=subclass.getInterfaces()[0];	
			}else{
				superclass=subclass.getSuperclass();	
			}
		}
		Type type= $Gson$Types.getSupertype(null, subclass,superclass);
		if(type instanceof ParameterizedType){
			return ((ParameterizedType) type).getActualTypeArguments();
		}
		throw new RuntimeException("Can not get the generic param type for class:" + subclass.getName());
	}
}
