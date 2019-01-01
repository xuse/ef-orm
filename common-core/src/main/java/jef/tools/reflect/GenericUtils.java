package jef.tools.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jef.common.Entry;
import jef.tools.Assert;
import jef.tools.Primitives;


/**
 * 泛型工具集，用于解析泛型、生成泛型
 * @author Administrator
 * @Date 2011-6-20
 */
public final class GenericUtils{
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
	
	public static Class<?> getRawClass(Type type) {
		if (type instanceof Class<?>) {
			// type is a normal class.
			return (Class<?>) type;

		} else if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;

			// I'm not exactly sure why getRawType() returns Type instead of
			// Class.
			// Neal isn't either but suspects some pathological case related
			// to nested classes exists.
			Type rawType = parameterizedType.getRawType();
			Assert.isTrue(rawType instanceof Class);
			return (Class<?>) rawType;

		} else if (type instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) type).getGenericComponentType();
			return Array.newInstance(getRawClass(componentType), 0).getClass();

		} else if (type instanceof TypeVariable) {
			// we could use the variable's bounds, but that won't work if there
			// are multiple.
			// having a raw type that's more general than necessary is okay
			return Object.class;

		} else if (type instanceof WildcardType) {
			return getRawClass(((WildcardType) type).getUpperBounds()[0]);

		} else {
			String className = type == null ? "null" : type.getClass().getName();
			throw new IllegalArgumentException("Expected a Class, ParameterizedType, or " + "GenericArrayType, but <" + type + "> is of type " + className);
		}
	}
	
	/**
	 * Google编写的泛型解析方法
	 * @param context
	 * @param toResolve
	 * @return
	 */
	public static Type resolve2(Type context,Type toResolve){
		return GqGenericResolver.resolve(context, context==null?null:getRawClass(context), toResolve);
	}
	
	/**
	 * Jiyi编写的泛型解析方法，将所有泛型边界和泛型边界解析为边界的具体类型
	 * @param context
	 * @param toResolve
	 * @return
	 */
	public static Type resolve (Type context,Type toResolve){
		return getBoundType(toResolve, context==null?null:new ClassEx(context));
	}
	
	/**
	 * Jiyi 编写的计算泛型边界，将泛型变量、边界描述、全部按照允许的最左边界进行计算
	 * 
	 * @param type
	 * @param cw
	 * @return
	 */
	public static Type getBoundType(Type type, ClassEx cw) {
		if (type instanceof TypeVariable<?>) {
			TypeVariable<?> tv = (TypeVariable<?>) type;
			Type real = cw.getImplType(tv);
			if (real != null) {
				return getBoundType(real, cw);
			}
			real = tv.getBounds()[0];
			return getBoundType(real, cw);
		} else if (type instanceof WildcardType) {
			WildcardType wild = (WildcardType) type;
			return getBoundType(wild.getUpperBounds()[0], cw);
		}
		if (isImplType(type)) {
			return type;
		}
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type[] types = pt.getActualTypeArguments();
			for (int i = 0; i < types.length; i++) {
				types[i] = getBoundType(types[i], cw);
			}
			Class<?> raw = (Class<?>) getBoundType(pt.getRawType(), cw);
			return GenericUtils.newGenericType(raw, types);
		} else if (type instanceof GenericArrayType) {
			GenericArrayType at = (GenericArrayType) type;
			return GenericUtils.newArrayType(getBoundType(at.getGenericComponentType(), cw));
		}
		return null;
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
	
	// 是否确定类型的泛型常量，还是类型不确定的泛型变量。
	private static boolean isImplType(Type type) {
		if (type instanceof Class<?>)
			return true;
		if (type instanceof GenericArrayType) {
			return isImplType(((GenericArrayType) type).getGenericComponentType());
		} else if (type instanceof ParameterizedType) {
			for (Type sub : ((ParameterizedType) type).getActualTypeArguments()) {
				if (!isImplType(sub)) {
					return false;
				}
			}
			return true;
		} else if (type instanceof TypeVariable<?>) {
			return false;
		} else if (type instanceof WildcardType) {
			return false;
		}
		throw new IllegalArgumentException();
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
		return GqGenericResolver.newParameterizedTypeWithOwner(clz.getEnclosingClass(), clz, valueType);
	}
	
	/**
	 * 生成Map的泛型类型
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public static ParameterizedType newMapType(Type keyType,Type valueType){
		if(keyType instanceof Class<?>){
			keyType=Primitives.toWrapperClass((Class<?>)keyType);
		}
		if(valueType instanceof Class<?>){
			valueType=Primitives.toWrapperClass((Class<?>)valueType);
		}
		return GqGenericResolver.newParameterizedTypeWithOwner(null, Map.class, keyType,valueType);
	}
	

	/**
	 * Returns the generic form of {@code supertype}. For example, if this is
	 * {@code ArrayList<String>}, this returns {@code Iterable<String>} given
	 * the input {@code Iterable.class}.
	 * 
	 * @param supertype
	 *            a superclass of, or interface implemented by, this.
	 */
	public static Type getSuperType(Type context, Class<?> contextRawType, Class<?> supertype) {
		Assert.isTrue(supertype.isAssignableFrom(contextRawType));
		return GqGenericResolver.resolve(context, contextRawType, GqGenericResolver.getGenericSupertype(context, contextRawType, supertype));
	}
	
	/**
	 * 生成List的泛型类型
	 * @param elementType
	 * @return
	 */
	public static ParameterizedType newListType(Type elementType){
		if(elementType instanceof Class<?>){
			elementType=Primitives.toWrapperClass((Class<?>)elementType);
		}
		return GqGenericResolver.newParameterizedTypeWithOwner(null, List.class, elementType);
	}
	
	/**
	 * 生成Set的泛型类型
	 * @param elementType
	 * @return
	 */
	public static ParameterizedType newSetType(Type elementType){
		if(elementType instanceof Class<?>){
			elementType=Primitives.toWrapperClass((Class<?>)elementType);
		}
		return GqGenericResolver.newParameterizedTypeWithOwner(null, Set.class, elementType);
	}
	
	/**
	 * 生成Array的泛型类型
	 * @param elementType
	 * @return 注意：作为java中特殊的类型，所有的Array都有对应的class，
	 * 此处产生的是GenericArrayType，如果确认该类型中没有泛型参数（用isRawArray()检测），
	 * 可以使用getRawClass()得到该类型的class形式。
	 */
	public static GenericArrayType newArrayType(Type elementType){
		return GqGenericResolver.arrayOf(elementType);
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
		return getCollectionElementType(context, Collection.class);
	}
	

	/**
	 * Returns the element type of this collection type.
	 * 
	 * @throws IllegalArgumentException
	 *             if this type is not a collection.
	 */
	public static Type getCollectionElementType(Type context, Class<?> contextRawType) {
		Type collectionType = getSuperType(context, contextRawType, Collection.class);
		return ((ParameterizedType) collectionType).getActualTypeArguments()[0];
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
			Type[] types= getMapKeyAndValueTypes(mapType, Map.class);
			return new Entry<Type,Type>(types[0],types[1]);
		}
	}
	

	/**
	 * Returns a two element array containing this map's key and value types in
	 * positions 0 and 1 respectively.
	 */
	public static Type[] getMapKeyAndValueTypes(Type context, Class<?> contextRawType) {
		/*
		 * Work around a problem with the declaration of java.util.Properties.
		 * That class should extend Hashtable<String, String>, but it's declared
		 * to extend Hashtable<Object, Object>.
		 */
		if (context == Properties.class) {
			return new Type[] { String.class, String.class };
		}

		Type mapType = getSuperType(context, contextRawType, Map.class);
		ParameterizedType mapParameterizedType = (ParameterizedType) mapType;
		return mapParameterizedType.getActualTypeArguments();
	}
	
	/**
	 * 是否数组类型
	 * @param field
	 * @return
	 */
	public static boolean isArray(Type type){
		return GqGenericResolver.isArray(type);
	}

	/**
	 * 批量转换为RawClass
	 * @param types
	 * @return
	 */
	public static Class<?>[] getRawClasses(Type[] types){
		Class<?>[] result=new Class[types.length];
		for(int i=0;i<types.length;i++){
			result[i]=getRawClass(types[i]);
		}
		return result;
	}
	
	/**
	 * 获得数组元素的泛型类型
	 * @param array
	 * @return
	 */
	public static Type getArrayComponentType(Type array) {
		return GqGenericResolver.getArrayComponentType(array);
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
