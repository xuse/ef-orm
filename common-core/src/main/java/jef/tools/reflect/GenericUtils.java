package jef.tools.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.common.Entry;
import jef.tools.Assert;


/**
 * 泛型工具集，用于解析泛型、生成泛型
 * @Company: Asiainfo-Linkage Technologies(China),Inc.  Hangzhou
 * @author Administrator
 * @Date 2011-6-20
 */
public final class GenericUtils extends JefGson{
	/**
	 * 泛型类型常量 Map&lt;String,String&gt;
	 */
	public static final Type MAP_OF_STRING=newMapType(String.class,String.class);
	/**
	 * 泛型类型常量 Map&lt;String,String[]&gt;
	 */
	public static final Type MAP_STRING_SARRAY=newMapType(String.class,String[].class);
	/**
	 *  泛型类型常量 Map&lt;String,Object&gt;
	 */
	public static final Type MAP_STRING_OBJECT=newMapType(String.class,Object.class);
	/**
	 * 泛型类型常量 List&lt;String&gt;
	 */
	public static final Type LIST_STRING=newListType(String.class);
	/**
	 * 泛型类型常量 List&lt;Object&gt;
	 */
	public static final Type LIST_OBJECT=newListType(Object.class);
	
	
	/**
	 * Google编写的泛型解析方法
	 * @param context
	 * @param toResolve
	 * @return
	 */
	public static Type resolve2(Type context,Type toResolve){
		return $Gson$Types.resolve(context, context==null?null:getRawClass(context), toResolve);
	}
	
	/**
	 * Jiyi编写的泛型解析方法，将所有泛型边界和泛型边界解析为边界的具体类型
	 * @param context
	 * @param toResolve
	 * @return
	 */
	public static Type resolve (Type context,Type toResolve){
		return BeanUtils.getBoundType(toResolve, context==null?null:new ClassEx(context));
	}
	
	/**
	 * 解析子类继承父类时所设置的泛型参数(只返回第一个)
	 * @param subclass
	 * @return
	 * @deprecated 不能指定是接口还是类继承，容易出错，用getTypeParameters(Class,Class)代替
	 */
	public static Type getFirstTypeParameters(Class<?> subclass) {
		Type superclass = subclass.getGenericSuperclass();
		if (superclass instanceof Class<?>) {
			throw new RuntimeException("Missing type parameter.");
		}
		return ((ParameterizedType) superclass).getActualTypeArguments()[0];
	}
	
	/**
	 * 得到继承上级接口、父类所指定的泛型类型
	 * @param subclass
	 * @param superclass
	 * @return
	 * @deprecated getTypeParameters is a more strong implement.
	 */
	public static Class<?>[] getInterfaceTypeParameter(Class<?> subclass,Class<?> superclass) {
		List<Class<?>> cls=new ArrayList<Class<?>>();
		for(Type superClz: subclass.getGenericInterfaces()){
			if (superClz instanceof Class<?>) {
				continue;
			}
			ParameterizedType superType=((ParameterizedType) superClz);
			if(superclass==null || superclass==superType.getRawType()){
				for(Type type:superType.getActualTypeArguments()){
					if(type instanceof Class){
						cls.add((Class<?>) type);
					}else if(type instanceof GenericArrayType){
						Type t=((GenericArrayType) type).getGenericComponentType();
						if(t instanceof Class){
							cls.add(Array.newInstance((Class<?>)t, 0).getClass());	
						}
					}
				}
				return cls.toArray(new Class[cls.size()]);
			}
		}	
		throw new RuntimeException("the "+subclass.getName()+" doesn't implements " + superclass.getName());
	}
	
	/**
	 * 创建一个泛型类型
	 * @param clz
	 * @param valueType
	 * @return
	 */
	public static ParameterizedType newGenericType(Class<?> clz,Type... valueType){
		if(!isGenericType(clz)){
			throw new IllegalArgumentException();
		}
		return $Gson$Types.newParameterizedTypeWithOwner(clz.getEnclosingClass(), clz, valueType);
	}
	
	/**
	 * 生成Map的泛型类型
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public static ParameterizedType newMapType(Type keyType,Type valueType){
		if(keyType instanceof Class<?>){
			keyType=BeanUtils.toWrapperClass((Class<?>)keyType);
		}
		if(valueType instanceof Class<?>){
			valueType=BeanUtils.toWrapperClass((Class<?>)valueType);
		}
		return $Gson$Types.newParameterizedTypeWithOwner(null, Map.class, keyType,valueType);
	}
	
	/**
	 * 生成List的泛型类型
	 * @param elementType
	 * @return
	 */
	public static ParameterizedType newListType(Type elementType){
		if(elementType instanceof Class<?>){
			elementType=BeanUtils.toWrapperClass((Class<?>)elementType);
		}
		return $Gson$Types.newParameterizedTypeWithOwner(null, List.class, elementType);
	}
	
	/**
	 * 生成Set的泛型类型
	 * @param elementType
	 * @return
	 */
	public static ParameterizedType newSetType(Type elementType){
		if(elementType instanceof Class<?>){
			elementType=BeanUtils.toWrapperClass((Class<?>)elementType);
		}
		return $Gson$Types.newParameterizedTypeWithOwner(null, Set.class, elementType);
	}
	
	/**
	 * 生成Array的泛型类型
	 * @param elementType
	 * @return 注意：作为java中特殊的类型，所有的Array都有对应的class，
	 * 此处产生的是GenericArrayType，如果确认该类型中没有泛型参数（用isRawArray()检测），
	 * 可以使用getRawClass()得到该类型的class形式。
	 */
	public static GenericArrayType newArrayType(Type elementType){
		return $Gson$Types.arrayOf(elementType);
	}
	
	/**
	 * 生成非泛型的数组class
	 * @param elementType
	 * @return
	 */
	public static Class<?> newArrayClass(Type componentType){
		return Array.newInstance(getRawClass(componentType), 0).getClass();
	}
	
	/**
	 * 判断指定的类型是否为一个没有泛型的数组类型
	 * 每个数组都有两种表示方式，基于class的和基于GenericArrayType的。前者可以表示不带泛型参数的类型，
	 * 后者可以表示带有泛型参数的类型。
	 * 如将后者转换前者 getRawClass()，可能会丢失信息，此方法判断为true的情况下，可以转换为class而不丢失数据类型。
	 * @param type  要检测的Type
	 * @return
	 * @Throws RuntimeException 输入类型必须是一个泛型Array或class Array,如果输入类型不是一个数组类型，抛出RuntimeException。
	 */
	public static boolean isRawArray(Type type){
		if(type instanceof Class<?>){
			Class<?> clz=(Class<?>)type;
			Assert.isTrue(clz.isArray(),"the input type "+ type +" is not a array type!");
			return true;
		}
		if(type instanceof GenericArrayType){
			Type subType=((GenericArrayType)type).getGenericComponentType();
			if(isArray(subType)){
				return isRawArray(subType);	
			}else{
				return subType instanceof Class<?>;
			}
		}
		return false;
	}
	
	/**
	 * 计算Collection的泛型参数
	 * @param context
	 * @param contextRawType
	 * @return
	 */
	public static Type getCollectionType(Type context) {
		return $Gson$Types.getCollectionElementType(context, Collection.class);
	}
	
	/**
	 * 获取泛型Map的参数类型
	 * @param mapType
	 * @return
	 */
	public static Entry<Type,Type> getMapTypes(Type mapType){
		if(mapType instanceof Class){
			return new Entry<Type,Type>(Object.class,Object.class);
		}else{
			Type[] types= $Gson$Types.getMapKeyAndValueTypes(mapType, Map.class);
			return new Entry<Type,Type>(types[0],types[1]);
		}
		
	}
	
	/**
	 * 是否数组类型
	 * @param field
	 * @return
	 */
	public static boolean isArray(Type type){
		return $Gson$Types.isArray(type);
	}
	
	/**
	 * 得到原始类型
	 * @param type
	 */
	public static Class<?> getRawClass(Type type){
		if(type==null)return null;
		return $Gson$Types.getRawType(type);
	}
	
	/**
	 * 批量转换为RawClass
	 * @param types
	 * @return
	 */
	public static Class<?>[] getRawClasses(Type[] types){
		Class<?>[] result=new Class[types.length];
		for(int i=0;i<types.length;i++){
			result[i]=$Gson$Types.getRawType(types[i]);
		}
		return result;
	}
	
	/**
	 * 获得数组元素的泛型类型
	 * @param array
	 * @return
	 */
	public static Type getArrayComponentType(Type array) {
		return $Gson$Types.getArrayComponentType(array);
	}
	
	public static Type[] getMapKeyAndValueTypes(Type context, Class<?> contextRawType) {
		return $Gson$Types.getMapKeyAndValueTypes(context,contextRawType);
	}
	
	/**
	 * 判断指定对象是否定义了泛型
	 * @param container
	 * @return
	 */
	public static boolean isGenericType(GenericDeclaration container){
		return container.getTypeParameters().length>0;
	}
}
