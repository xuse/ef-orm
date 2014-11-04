package jef.tools.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import jef.tools.ArrayUtils;

/**
 * JDK的反射机制进行泛型计算有诸多不便，<BR>
 * <BR>
 * 然后我们看下面这个场景
 * <P>
 * <code>
 * public abstract class A&lt;K&gt;{<BR>
 * 　　　public K method1();<BR>
 * }<BR>
 * <BR>
 * public class B extends A&lt;String&gt;{<BR>
 * }<BR>
 * </code>
 * </P>
 * 当我们通过以下代码尝试获得方法的返回参数类型时：<BR>
 * <P>
 * <code>
 *    Method method=B.class.getMethod("method1");<BR>
 *    method.getReturnType(); //期望得到String.class，实际得到Object.class<BR>
 *    method.getGenericReturnType();//即使用这个方法也一样，不会得到String.class<BR>
 * </code>
 * </P>
 * <BR>
 * Field也有类似的问题。<BR>
 * 即任意一个Method实例中，由于丢失了实际所在的class(子类)信息，只保留了DeclearingClass(父类)， 从而也就丢失了泛型的提供者，
 * 因此永远不可能计算出泛型的最小边界。 在泛型的场合下计算边界，三个泛型提供者Class/Methid/Field缺一不可。
 * 为了弥补JDK的这个问题，提供了增强的ClassEx、MethodEx、FieldEx来代替JDK原生的Class、Method、Field进行带泛型的反射操作
 * 
 * @see FieldEx
 * @see ClassEx
 * 
 */
public class MethodEx {
	private java.lang.reflect.Method method;
	private ClassEx instanceClass;
	/**
	 * The BridgeMethod is the method which extends from super class which
	 * generic type was as same as superclass and also is is Volatile. The only
	 * way to fetch the method who was overrided is by bridge method.
	 */
	private java.lang.reflect.Method bridgeMethod;

	public MethodEx(java.lang.reflect.Method method) {
		this(method, (Class<?>) null);
	}

	MethodEx(Method method, ClassEx clz) {
		this.method = method;
		this.instanceClass = clz;
	}

	public MethodEx(Method method, Class<?> clz) {
		this.method = method;
		this.instanceClass = new ClassEx(clz);
	}

	public java.lang.reflect.Method getJavaMethod() {
		return method;
	}

	public Class<?> getInstanceClass() {
		return instanceClass.getWrappered();
	}

	public int getModifiers() {
		return method.getModifiers();
	}

	public void setAccessible(boolean flag) throws SecurityException {
		method.setAccessible(flag);
	}

	public Object invoke(Object obj, Object... args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return method.invoke(obj, args);
	}

	@Override
	public int hashCode() {
		return method.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MethodEx) {
			return this.method.equals(((MethodEx) obj).method);
		}
		return false;
	}

	/**
	 * 得到方法上的Annotation
	 * 
	 * @param class1
	 * @return
	 */
	public <T extends Annotation> T getAnnotation(Class<T> class1) {
		T anno= method.getAnnotation(class1);
		if(anno!=null)return anno;
		if (bridgeMethod != null) {
			Method me = BridgeMethodResolver.findGenericDeclaration(bridgeMethod);
			anno= me.getAnnotation(class1);
		}
		if(anno!=null)return anno;
		Class<?>[] params=(bridgeMethod==null)?method.getParameterTypes():bridgeMethod.getParameterTypes();
		Class<?> c=method.getDeclaringClass().getSuperclass();
		while(c!=null && c!=Object.class){
			Method m=null;
			try {
				m=c.getMethod(method.getName(), params);
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			}
			if(m!=null){
				anno=m.getAnnotation(class1);
				if(anno!=null)return anno;
			}
			c=c.getSuperclass();
		}
		
		for(Class<?> intf:method.getDeclaringClass().getInterfaces()){
			Method m=null;
			try {
				m=intf.getMethod(method.getName(), params);
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			}
			if(m!=null){
				anno=m.getAnnotation(class1);
				if(anno!=null)return anno;
			}
		}
		return anno;
	}

//	public Method[] getSuperMethod(){
//		List<Method> result=new ArrayList<Method>();
//		Class current=this.method.getDeclaringClass();
//		Class superClz=current.getSuperclass();
//		String name=method.getName();
////		Class[] paramType=bridgeMethod!=null
////		while(superClz!=null && superClz!=Object.class){
////			Method m=
////		}
//		return result.toArray(new Method[result.size()]);
//	}
	/**
	 * 返回方法指定序号的参数上的Annotation
	 * 
	 * @param i   参数序号
	 * @param annotationCls Annotation类型
	 * @return 注解(Annotation)对象，如果没有指定类型注解返回ull。
	 * @throws IllegalArgumentException 如果序号超过参数
	 */
	public <T extends Annotation> T getParamAnnotation(int i, Class<T> annotationCls) {
		return getMethodParamAnnotation(this, i, annotationCls);
	}
	
	/**
	 * 获得位于方法上的Annotation，会到父类和父接口查找
	 * @param method
	 * @param i
	 * @param annotationCls
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Annotation> T getMethodParamAnnotation(MethodEx method, int i, Class<T> annotationCls) {
		Annotation[][] annos = method.getParameterAnnotations();
		if (i >= annos.length) {
			throw new IllegalArgumentException("the param max index is:" + annos.length + ". the input index " + i + " is out of bound.");
		}
		Annotation[] anns = annos[i];
		if (anns == null || anns.length == 0) {
			return null;
		}
		for (Annotation a : anns) {
			if (ArrayUtils.contains(a.getClass().getInterfaces(), annotationCls)) {
				return (T) a;
			}
		}
		return null;
	}

	public java.lang.reflect.Method getBridgeMethod() {
		return bridgeMethod;
	}

	public void setBridgeMethod(java.lang.reflect.Method bridgeMethod) {
		this.bridgeMethod = bridgeMethod;
	}

	/**
	 * 获取所有的Annotation
	 * 
	 * @return
	 */
	public Annotation[] getAnnotations() {
		Annotation[] annos = method.getAnnotations();
		if (annos.length == 0 && bridgeMethod != null) {
			Method me = BridgeMethodResolver.findGenericDeclaration(bridgeMethod);
			me.getAnnotations();
		}
		return annos;
	}

	/**
	 * 得到方法的泛型返回值类型
	 * 
	 * @return
	 */
	public Type getGenericReturnType() {
		return instanceClass.getMethodReturnType(method);
	}

	/**
	 * 得到方法的非泛型返回值类型
	 * 
	 * @return
	 */
	public Class<?> getReturnType() {
		return GenericUtils.getRawClass(getGenericReturnType());
	}

	/**
	 * 得到方法的泛型参数
	 * 
	 * @return
	 */
	public Type[] getGenericParameterTypes() {
		return instanceClass.getMethodParamTypes(method);
	}

	/**
	 * 得到方法的非泛型参数
	 * 
	 * @return
	 */
	public Class<?>[] getParameterTypes() {
		Type[] types = getGenericParameterTypes();
		Class<?>[] result = new Class<?>[types.length];
		for (int i = 0; i < types.length; i++) {
			result[i] = GenericUtils.getRawClass(types[i]);
		}
		return result;
	}

	/**
	 * 获取方法名称
	 * 
	 * @return
	 */
	public String getName() {
		return method.getName();
	}

	@Override
	public String toString() {
		return method.toString();
	}

	public Class<?> getDeclaringClass() {
		return method.getDeclaringClass();
	}

	public boolean isBridge() {
		return method.isBridge();
	}

	public Annotation[][] getParameterAnnotations() {
		Annotation[][] annos= method.getParameterAnnotations();
		int n=0;
		for(Annotation[] ann:annos){
			n+=ann.length;
		}
		if(n==0 && bridgeMethod!=null){
			Method me=BridgeMethodResolver.findGenericDeclaration(bridgeMethod);
			return me.getParameterAnnotations();
		}
		return annos;
	}

	public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return method.isAnnotationPresent(annotationClass);
	}
}
