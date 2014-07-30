//package jef.database.meta;
//
//import java.lang.reflect.InvocationHandler;
//import java.lang.reflect.Method;
//import java.lang.reflect.Proxy;
//
//import jef.database.Field;
//import jef.database.IQueryableEntity;
//import jef.database.VarObject;
//
//public abstract class MetadataProxyUtil {
//	public static Field getField(String name,TupleMetadata meta){
//		ClassLoader cl=Thread.currentThread().getContextClassLoader();
//		if(cl==null)cl=MetadataProxyUtil.class.getClassLoader();
//		return (Field)Proxy.newProxyInstance(cl, new Class[]{jef.database.Field.class,Comparable.class}, new FieldHandler(meta,name));
//	}
//	
//	public static class FieldHandler implements InvocationHandler{
//		private TupleMetadata meta;
//		private String name;
//		FieldHandler(TupleMetadata meta,String name){
//			this.meta=meta;
//			this.name=name;
//		}
//		
//		public TupleMetadata getMeta() {
//			return meta;
//		}
//
//		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//			int len=(args==null?0:args.length);
//			switch(len){
//			case 0:
//				return process0(method.getName()); 
//			case 1:
//				return process1(method.getName(),args[0]); 
//			case 2:
//				return process2(method.getName(),args[0],args[1]);
//			default:
//				throw new IllegalArgumentException("Unknon method "+method.getName()+" with "+args.length+" args.");
//			}
//		}
//		
//		private Object process0(String method) {
//			if("toString".equals(method) || "name".equals(method)){
//				return name;
//			}else if("hashCode".equals(method)){
//				return name.hashCode();
//			}else if("ordinal".equals(method)){
//				return 1;
//			}
//			
//			throw new IllegalArgumentException("Unknon method "+method+" with no args.");
//		}
//		private Object process1(String method, Object arg) {
//			if("equals".equals(method)){
//				if(arg instanceof jef.database.Field){
//					return name.equals(arg.toString());
//				}else{
//					return false;
//				}
//			}else if("compareTo".equals(method)){
//				if(arg instanceof jef.database.Field){
//					return name.compareTo(arg.toString());
//				}else{
//					throw new ClassCastException(arg.getClass() +" cast to jef.database.Field.");
//				}
//			}
//			throw new IllegalArgumentException("Unknon method "+method+" with 1 args.");
//		}
//		
//		private Object process2(String method, Object arg1,Object arg2) {
//			throw new IllegalArgumentException("Unknon method "+method+" with 2 args.");
//		}
//		public String getName() {
//			return name;
//		}
//		public Class<? extends IQueryableEntity> getDeClass() {
//			return VarObject.class;
//		}
//	}
//
//}
