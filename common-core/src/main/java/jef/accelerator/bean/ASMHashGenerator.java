package jef.accelerator.bean;

import static org.easyframe.fastjson.util.ASMUtils.getDesc;
import static org.easyframe.fastjson.util.ASMUtils.getMethodDesc;
import static org.easyframe.fastjson.util.ASMUtils.getType;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jef.accelerator.asm.ClassWriter;
import jef.accelerator.asm.FieldVisitor;
import jef.accelerator.asm.Label;
import jef.accelerator.asm.MethodVisitor;
import jef.accelerator.asm.Opcodes;
import jef.accelerator.asm.Type;
import jef.accelerator.bean.ASMAccessorFactory.ClassGenerator;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.UnsafeUtils;

import org.easyframe.fastjson.util.ASMUtils;

final class ASMHashGenerator implements Opcodes,ClassGenerator {
	private Class<?> beanClass;
	private String beanType;

	private FieldInfo[] fields;
	@SuppressWarnings("rawtypes")
	private Class[] properTyClz;
	private ClassLoader cl;
	private String typeName;
	private String className;

	public ASMHashGenerator(Class<?> javaBean, String clzName, FieldInfo[] fields,ClassLoader cl) {
		this.beanClass = javaBean;
		this.beanType = getType(beanClass);

		this.className = clzName;
		this.typeName = className.replace('.', '/');
		this.fields = fields;
		this.properTyClz = new Class[fields.length];
		this.cl=cl;
	}

	public byte[] generate() {
		generatePropertyClz();
		return generateMain();
	}

	private void generateInvokeMethod(MethodVisitor mw, Method m) {
		mw.visitMethodInsn(INVOKEVIRTUAL, getType(m.getDeclaringClass()), m.getName(), getDesc(m));
	}
	
	private byte[] generateMain() {
		ClassWriter cw = new ClassWriter(0);
		String parentClz = getType(HashBeanAccessor.class);
		String mapType=getType(Map.class);
		
		cw.visit(V1_5, ACC_PUBLIC + ACC_FINAL, className, null,parentClz, new String[] {});
		
		{
			FieldVisitor fw = cw.visitField(ACC_PRIVATE | ACC_FINAL, "fields", getDesc(java.util.Map.class),null, null);
			fw.visitEnd();
		}
		
		// //构造器
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null,null);
			mw.visitVarInsn(ALOAD, 0); 
			mw.visitMethodInsn(INVOKESPECIAL, parentClz, "<init>", "()V");//S0
			
			String hashMapType=getType(HashMap.class);
			mw.visitTypeInsn(NEW, hashMapType);		//S1=map
			mw.visitInsn(DUP);  					//S2=map
 			ASMUtils.iconst(mw, fields.length*4/3+1);     //S3=int
			mw.visitMethodInsn(INVOKESPECIAL, hashMapType, "<init>", "(I)V"); //S1=map
			mw.visitVarInsn(ASTORE, 1); //S0
			
			mw.visitVarInsn(ALOAD, 0); //S1=this
			mw.visitVarInsn(ALOAD, 1); //S1=map
			mw.visitFieldInsn(PUTFIELD, className, "fields", "Ljava/util/Map;");  //s清空
			
			for(int i=0;i<fields.length;i++){
				mw.visitVarInsn(ALOAD, 1); //S1=map
				
				FieldInfo fi=fields[i];
				mw.visitLdcInsn(fi.getName());				//S2=string
				String pType=getType(properTyClz[i]);		 
				mw.visitTypeInsn(NEW, pType);  //S3=property  
				mw.visitInsn(DUP);   			//S4=Property
				mw.visitMethodInsn(INVOKESPECIAL, pType, "<init>", "()V");   //S3=Property
				mw.visitMethodInsn(INVOKEINTERFACE, mapType, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");//S1 S2,S3消耗 S1=return
				mw.visitInsn(POP);//S1移除，留下S0
			}
			mw.visitInsn(RETURN);
			mw.visitMaxs(4, 2);
			mw.visitEnd();
		}
		{//Simple methods x2
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getPropertyNames", "()"+getDesc(Collection.class),null, null);
			mw.visitVarInsn(ALOAD, 0);
			mw.visitFieldInsn(GETFIELD, className, "fields", "Ljava/util/Map;");
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "keySet", "()Ljava/util/Set;");
			mw.visitInsn(ARETURN);
			mw.visitMaxs(1, 1);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getProperties", "()"+getDesc(Collection.class), null,null);
			mw.visitVarInsn(ALOAD, 0);
			mw.visitFieldInsn(GETFIELD, className, "fields", "Ljava/util/Map;");
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "values", "()Ljava/util/Set;");
			mw.visitInsn(ARETURN);
			mw.visitMaxs(1, 1);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getProperty", ASMUtils.getMethodDesc(jef.tools.reflect.Property.class, String.class), null,null);
			mw.visitVarInsn(ALOAD, 0);
			mw.visitFieldInsn(GETFIELD, className, "fields", "Ljava/util/Map;");
			mw.visitVarInsn(ALOAD, 1);
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");	
			mw.visitInsn(ARETURN);
			mw.visitMaxs(2,2);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getAnnotationOnField", getMethodDesc(java.util.IdentityHashMap.class, String.class),null, null);
			mw.visitVarInsn(ALOAD, 0);  //S1
			mw.visitFieldInsn(GETFIELD, className, "fields", "Ljava/util/Map;");		//S1
			mw.visitVarInsn(ALOAD, 1);		//S2
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "get", "(Ljava/lang/Object;)Ljava/lang/Object;"); //S1
			
			mw.visitInsn(DUP);  //S2
			Label ifnull=new Label();
			mw.visitJumpInsn(IFNULL, ifnull);
			mw.visitTypeInsn(CHECKCAST, getType(AbstractFastProperty.class));
			mw.visitVarInsn(ASTORE, 2);		//S0
			
			mw.visitVarInsn(ALOAD, 0);			//S1`
			mw.visitFieldInsn(GETFIELD, typeName, "fieldAnnoMaps", "[Ljava/util/IdentityHashMap;"); //S1
			
			mw.visitVarInsn(ALOAD, 2);			//S2
			mw.visitFieldInsn(GETFIELD, getType(jef.accelerator.bean.AbstractFastProperty.class), "n", "I"); //S2==int
			
			mw.visitInsn(AALOAD);
			mw.visitInsn(ARETURN);
			
			mw.visitLabel(ifnull);
			mw.visitInsn(POP); //S0
			mw.visitTypeInsn(NEW, getType(NoSuchElementException.class));
			mw.visitInsn(DUP); //S2
			mw.visitTypeInsn(NEW, getType(StringBuilder.class)); //S3
			mw.visitInsn(DUP); //S4
			mw.visitVarInsn(ALOAD, 1);  //S5
			mw.visitMethodInsn(INVOKESPECIAL, getType(StringBuilder.class), "<init>",getMethodDesc(Void.TYPE, String.class)); //S3=StringBuilder;
			mw.visitLdcInsn(" is not exist in " + beanClass.getName());  //S4 String
			mw.visitMethodInsn(INVOKEVIRTUAL,  getType(StringBuilder.class),"append", getMethodDesc(StringBuilder.class, String.class)); //S3=StringBuilder;
			mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "toString", getMethodDesc(String.class));  //S3=String
			mw.visitMethodInsn(INVOKESPECIAL,  getType(NoSuchElementException.class),"<init>", getMethodDesc(Void.TYPE, String.class)); //S1=NoSuchElementException
			mw.visitInsn(ATHROW);
			
			mw.visitMaxs(5, 3);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getAnnotationOnGetter", getMethodDesc(java.util.IdentityHashMap.class, String.class),null, null);
			
			mw.visitVarInsn(ALOAD, 0);  //S1
			mw.visitFieldInsn(GETFIELD, className, "fields", "Ljava/util/Map;");		//S1
			mw.visitVarInsn(ALOAD, 1);		//S2
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "get", "(Ljava/lang/Object;)Ljava/lang/Object;"); //S1
			
			
			mw.visitInsn(DUP);  //S2
			Label ifnull=new Label();
			mw.visitJumpInsn(IFNULL, ifnull);//S1
			mw.visitTypeInsn(CHECKCAST, getType(AbstractFastProperty.class));
			mw.visitVarInsn(ASTORE, 2);		//S0
			
			mw.visitVarInsn(ALOAD, 0);			//S1
			mw.visitFieldInsn(GETFIELD, typeName, "getterAnnoMaps", "[Ljava/util/IdentityHashMap;"); //S1
			
			mw.visitVarInsn(ALOAD, 2);			//S2
			mw.visitFieldInsn(GETFIELD, getType(jef.accelerator.bean.AbstractFastProperty.class), "n", "I"); //S2==int
			
			mw.visitInsn(AALOAD);
			mw.visitInsn(ARETURN);
			
			mw.visitLabel(ifnull);
			mw.visitInsn(POP); //S0
			mw.visitTypeInsn(NEW, getType(NoSuchElementException.class));
			mw.visitInsn(DUP); //S2
			mw.visitTypeInsn(NEW, getType(StringBuilder.class)); //S3
			mw.visitInsn(DUP); //S4
			mw.visitVarInsn(ALOAD, 1);  //S5
			mw.visitMethodInsn(INVOKESPECIAL, getType(StringBuilder.class), "<init>",getMethodDesc(Void.TYPE, String.class)); //S3=StringBuilder;
			mw.visitLdcInsn(" is not exist in " + beanClass.getName());  //S4 String
			mw.visitMethodInsn(INVOKEVIRTUAL,  getType(StringBuilder.class),"append", getMethodDesc(StringBuilder.class, String.class)); //S3=StringBuilder;
			mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "toString", getMethodDesc(String.class));  //S3=String
			mw.visitMethodInsn(INVOKESPECIAL,  getType(NoSuchElementException.class),"<init>", getMethodDesc(Void.TYPE, String.class)); //S1=NoSuchElementException
			mw.visitInsn(ATHROW);
			
			mw.visitMaxs(5, 3);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getAnnotationOnSetter", getMethodDesc(java.util.IdentityHashMap.class, String.class),null, null);
			mw.visitVarInsn(ALOAD, 0);  //S1
			mw.visitFieldInsn(GETFIELD, className, "fields", "Ljava/util/Map;");		//S1
			mw.visitVarInsn(ALOAD, 1);		//S2
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "get", "(Ljava/lang/Object;)Ljava/lang/Object;"); //S1
			
			mw.visitInsn(DUP);  //S2 AbstractFastProperty
			Label ifnull=new Label();
			mw.visitJumpInsn(IFNULL, ifnull); //S1  //如果属性不存在，返回null
			mw.visitTypeInsn(CHECKCAST, getType(AbstractFastProperty.class));
			mw.visitVarInsn(ASTORE, 2);		//S0
			
			mw.visitVarInsn(ALOAD, 0);			//S1 this
			mw.visitFieldInsn(GETFIELD, typeName, "setterAnnoMaps", "[Ljava/util/IdentityHashMap;"); //S1
			
			mw.visitVarInsn(ALOAD, 2);			//S2 AbstractFastProperty
			mw.visitFieldInsn(GETFIELD, getType(jef.accelerator.bean.AbstractFastProperty.class), "n", "I"); //S2==int 获取序号
			
			mw.visitInsn(AALOAD);   //setterAnnoMaps是数组，按序号获取
			mw.visitInsn(ARETURN);
			
			mw.visitLabel(ifnull);
			mw.visitInsn(POP); //S0
			mw.visitTypeInsn(NEW, getType(NoSuchElementException.class));
			mw.visitInsn(DUP); //S2
			mw.visitTypeInsn(NEW, getType(StringBuilder.class)); //S3
			mw.visitInsn(DUP); //S4
			mw.visitVarInsn(ALOAD, 1);  //S5
			mw.visitMethodInsn(INVOKESPECIAL, getType(StringBuilder.class), "<init>",getMethodDesc(Void.TYPE, String.class)); //S3=StringBuilder;
			mw.visitLdcInsn(" is not exist in " + beanClass.getName());  //S4 String
			mw.visitMethodInsn(INVOKEVIRTUAL,  getType(StringBuilder.class),"append", getMethodDesc(StringBuilder.class, String.class)); //S3=StringBuilder;
			mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "toString", getMethodDesc(String.class));  //S3=String
			mw.visitMethodInsn(INVOKESPECIAL,  getType(NoSuchElementException.class),"<init>", getMethodDesc(Void.TYPE, String.class)); //S1=NoSuchElementException
			mw.visitInsn(ATHROW);
			
			mw.visitMaxs(5, 3);
			mw.visitEnd();
		}
		{

			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "copy", getMethodDesc(Void.TYPE, Object.class, Object.class), null,null);

			Label checkArg2 = new Label();
			Label checkOver = new Label();

			mw.visitVarInsn(ALOAD, 1);
			mw.visitJumpInsn(IFNONNULL, checkArg2);
			mw.visitInsn(RETURN);

			mw.visitLabel(checkArg2);
			mw.visitVarInsn(ALOAD, 2);
			mw.visitJumpInsn(IFNONNULL, checkOver);
			mw.visitInsn(RETURN);

			mw.visitLabel(checkOver);
			mw.visitVarInsn(ALOAD, 1);
			mw.visitTypeInsn(CHECKCAST, beanType);
			mw.visitVarInsn(ASTORE, 3);
			mw.visitVarInsn(ALOAD, 2);
			mw.visitTypeInsn(CHECKCAST, beanType);
			mw.visitVarInsn(ASTORE, 4);

			for (int i = 0; i < fields.length; i++) {
				mw.visitVarInsn(ALOAD, 4);
				mw.visitVarInsn(ALOAD, 3);
				
				this.generateInvokeMethod(mw, fields[i].getGetter());
				this.generateInvokeMethod(mw, fields[i].getSetter());
				if(fields[i].getSetter().getReturnType()!=void.class){
					mw.visitInsn(POP);
				}
			}
			mw.visitInsn(RETURN);
			mw.visitMaxs(3, 5);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "newInstance", getMethodDesc(Object.class), null,null);
			mw.visitTypeInsn(NEW, beanType);
			try {
				//运行空构造方法
				if(beanClass.getDeclaredConstructor()!=null){
					mw.visitInsn(DUP);
					mw.visitMethodInsn(INVOKESPECIAL,beanType, "<init>", "()V");
				}
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
			mw.visitInsn(ARETURN);
			mw.visitMaxs(2, 1);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getType", getMethodDesc(Class.class), null,null);
			mw.visitLdcInsn(Type.getType(beanClass));//S1
			mw.visitInsn(ARETURN);
			mw.visitMaxs(1, 1);
			mw.visitEnd();
		}
		cw.visitEnd();
		return cw.toByteArray();
	}

	private void generatePropertyClz() {
		for (int i = 0; i < fields.length; i++) {
			FieldInfo fi=fields[i];
			String pname = this.className + "$" + fi.getName();
			byte[] data=generateProperty(i, fi,pname);
			//DEBUG
//			ASMAccessorFactory.saveClass(data, pname);
			Class<?> clz= UnsafeUtils.defineClass(pname, data, 0, data.length, cl);
			this.properTyClz[i]=clz;
		}
	}

	private byte[] generateProperty(int i, FieldInfo fi,String pname) {
		ClassWriter cw = new ClassWriter(0);
		String parentClz = getType(AbstractFastProperty.class);
		cw.visit(V1_5, ACC_PUBLIC + ACC_FINAL, pname,null, parentClz, new String[] {});
		// //构造器
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null,null);
			mw.visitVarInsn(ALOAD, 0);
			mw.visitMethodInsn(INVOKESPECIAL, parentClz, "<init>", "()V");
			mw.visitInsn(RETURN);
			mw.visitMaxs(1, 1);
			mw.visitEnd();
		}
		// getName
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getName", "()Ljava/lang/String;", null,null);
			mw.visitLdcInsn(fi.getName());
			mw.visitInsn(ARETURN);
			mw.visitMaxs(1, 1);
			mw.visitEnd();
		}
		// SET
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", null,null);
			mw.visitInsn(ALOAD_1);//S1
			mw.visitTypeInsn(CHECKCAST, beanType);
			mw.visitInsn(ASTORE_3);//S0
			
			mw.visitInsn(ALOAD_3);
			mw.visitInsn(ALOAD_2);	//S2
			
			if(fi.isPrimitive()){
				Class<?> wrpped=BeanUtils.toWrapperClass(fi.getRawType());
				mw.visitTypeInsn(CHECKCAST, getType(wrpped));	//S2
				ASMUtils.doUnwrap(mw, fi.getRawType(), wrpped);	//S2
			}else{
				mw.visitTypeInsn(CHECKCAST, getType(fi.getRawType()));	//S2
			}
			this.generateInvokeMethod(mw, fi.getSetter());
			
			mw.visitInsn(RETURN);
			if(fi.isPrimitive()){
				mw.visitMaxs(3, 4);
			}else{
				mw.visitMaxs(2, 4);
			}
			mw.visitEnd();
		}
		//GET
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "get", "(Ljava/lang/Object;)Ljava/lang/Object;", null,null);
			mw.visitInsn(ALOAD_1);
			mw.visitTypeInsn(CHECKCAST, beanType);
			mw.visitInsn(ASTORE_2);
			
			mw.visitInsn(ALOAD_0);
			mw.visitInsn(ALOAD_2);
			generateInvokeMethod(mw, fi.getGetter());
			if(fi.isPrimitive()){//inbox
				ASMUtils.doWrap(mw, fi.getRawType());
			}
			mw.visitInsn(ARETURN);
			mw.visitMaxs(3, 3);
			mw.visitEnd();
		}
		cw.visitEnd();
		return cw.toByteArray();
	}
}
