package jef.tools.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * JDK的反射机制有一个遗憾，<BR>我们看下面这个场景
 * <pre><code>
 *  public abstract class A&lt;K&gt;{
 * 　　　public K method1();
 * }

 * public class B extends A&lt;String&gt;{
 * }
 * </code></pre></P>
 * 当我们通过以下代码尝试获得方法的返回参数类型时：<BR>
 * <P><code>
 *    Method method=B.class.getMethod("method1");//看上去这个method是从B中得到的<BR>
 *    method.getReturnType(); //期望得到String.class，实际得到Object.class<BR>
 *    method.getGenericReturnType();//即使用这个方法也一样，不会得到String.class<BR>
 * </code></P>
 * <BR>
 * 上述问题其实说明了，任何一个泛型计算都需要有一个上下文。只有提供A<String>这个上下文，才能正确得到A当中的方法method1的返回类型。
 * 而Java反射接口的设计中，这个上下文被丢弃了。从一个class实例中得到Method对象中只有一个DeclearingClass，当泛型的子类继承父类时这个DeclearingClass并非之前的那个class实例，
 * 而之前的class实例实际上不存在于Method当中。<BR>
 * Field也有类似的问题。<BR>
 *  <BR>
 * 即任意一个Field实例中，由于丢失了实际所在的class(子类)信息，只保留了DeclearingClass(父类)，
 * 从而也就丢失了泛型的提供者， 因此永远不可能计算出泛型的最小边界。
 * 在泛型的场合下计算边界，三个泛型提供者Class/Methid/Field缺一不可。 
 * 为了弥补JDK的这个缺陷，提供了{@link FieldEx},{@link MethodEx}和{@link ClassEx}类提供method对象的包装。
 * @see ClassEx
 * @see MethodEx
 *
 */
public class FieldEx {
	private java.lang.reflect.Field field;
	ClassEx instanceClass;
	private Type genericType;
	private FieldAccessor accessor;
	
	public FieldEx(java.lang.reflect.Field method){
		this(method,(Class<?>)null);
	}

	FieldEx(Field field, ClassEx clz) {
		this.field=field;
		this.instanceClass=clz;
		this.genericType=clz.getFieldGenericType(field);
		accessor=FieldAccessor.generateAccessor(field);
	}

	public FieldEx(Field field, Class<?> clz) {
		this.field=field;
		this.instanceClass=new ClassEx(clz);
	}

	public java.lang.reflect.Field getJavaField() {
		return field;
	}
	public Class<?> getInstanceClass() {
		return instanceClass.getWrappered();
	}

	public String getName() {
		return field.getName();
	}

	public Object get(Object obj) {
		return accessor.getObject(obj);
	}

	@Override
	public int hashCode() {
		return field.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FieldEx){
			return this.field.equals(((FieldEx) obj).field);
		}
		return field.equals(obj);
	}

	public <T extends Annotation> T getAnnotation(Class<T> class1) {
		return field.getAnnotation(class1);
	}
	
	public Class<?> getType() {
		return field.getType();
	}
	
	public Type getGenericType(){
		return genericType;
	}

	public void set(Object bean, Object value){
		accessor.set(bean, value);
	}

	/**
	 * 得到 field所在的定义类
	 * @return
	 */
	public Class<?> getDeclaringClass() {
		return field.getDeclaringClass();
	}

	/**
	 * 得到field在定义类中的类型边界
	 * @return
	 */
	public Class<?> getDeclaringType() {
		return field.getType();
	}

	@Override
	public String toString() {
		int mod = field.getModifiers();
		StringBuilder sb=new StringBuilder(80);
		if(mod!=0)sb.append(Modifier.toString(mod)).append(' ');
		Type type=this.getGenericType();
		sb.append((type instanceof Class)?((Class<?>)type).getName():type.toString()).append(' ');
		sb.append(getInstanceClass().getName()).append('.');
		sb.append(getName());
		return sb.toString();
	}

	public int getModifiers() {
		return field.getModifiers();
	}

	public void setAccessible(boolean flag) {
		field.setAccessible(flag);
	}
	
	public FieldAccessor getAccessor(){
		return accessor;
	}

	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return field.isAnnotationPresent(annotationType);
	}

	public Annotation[] getAnnotations() {
		return field.getAnnotations();
	}
}
