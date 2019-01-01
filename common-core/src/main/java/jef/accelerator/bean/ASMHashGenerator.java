package jef.accelerator.bean;

import static jef.accelerator.asm.ASMUtils.getDesc;
import static jef.accelerator.asm.ASMUtils.getMethodDesc;
import static jef.accelerator.asm.ASMUtils.getType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.github.geequery.asm.ClassWriter;
import com.github.geequery.asm.FieldVisitor;
import com.github.geequery.asm.Label;
import com.github.geequery.asm.MethodVisitor;

import jef.accelerator.asm.ASMUtils;
import jef.tools.Primitives;

final class ASMHashGenerator extends ClassGenerator {
	@SuppressWarnings("rawtypes")
	private Class[] properTyClz;
	
	public ASMHashGenerator(Class<?> javaBean, String accessorName, FieldInfo[] fields,ClassLoaderAccessor cl) {
		super(javaBean,accessorName,fields,cl);
		this.properTyClz = new Class[fields.length];
	}

	public byte[] generate() {
		generatePropertyClz();
		return generateMain();
	}


	
	private byte[] generateMain() {
		ClassWriter cw = new ClassWriter(0);
		String parentClz = getType(HashBeanAccessor.class);
		String mapType=getType(Map.class);
		
		cw.visit(V1_5, ACC_PUBLIC + ACC_FINAL, accessorName, null,parentClz, new String[] {});
		
		{
			FieldVisitor fw = cw.visitField(ACC_PRIVATE | ACC_FINAL, "fields", getDesc(java.util.Map.class),null, null);
			fw.visitEnd();
			
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getPropertyNames", "()Ljava/util/Collection;",null, null);
			mw.visitVarInsn(ALOAD, 0);
			mw.visitFieldInsn(GETFIELD, accessorType, "fields", "Ljava/util/Map;");
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "keySet", "()Ljava/util/Set;");
			mw.visitInsn(ARETURN);
			mw.visitMaxs(1, 1);
			mw.visitEnd();
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
			mw.visitFieldInsn(PUTFIELD, accessorName, "fields", "Ljava/util/Map;");  //s清空
			
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
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getProperties", "()"+getDesc(Collection.class), null,null);
			mw.visitVarInsn(ALOAD, 0);
			mw.visitFieldInsn(GETFIELD, accessorType, "fields", "Ljava/util/Map;");
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "values", "()Ljava/util/Set;");
			mw.visitInsn(ARETURN);
			mw.visitMaxs(1, 1);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getProperty", ASMUtils.getMethodDesc(jef.tools.reflect.Property.class, String.class), null,null);
			mw.visitVarInsn(ALOAD, 0);
			mw.visitFieldInsn(GETFIELD, accessorType, "fields", "Ljava/util/Map;");
			mw.visitVarInsn(ALOAD, 1);
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");	
			mw.visitInsn(ARETURN);
			mw.visitMaxs(2,2);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getAnnotationOnField", getMethodDesc(java.util.Map.class, String.class),null, null);
			mw.visitVarInsn(ALOAD, 0);  //S1
			mw.visitFieldInsn(GETFIELD, accessorType, "fields", "Ljava/util/Map;");		//S1
			mw.visitVarInsn(ALOAD, 1);		//S2
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "get", "(Ljava/lang/Object;)Ljava/lang/Object;"); //S1
			
			mw.visitInsn(DUP);  //S2
			Label ifnull=new Label();
			mw.visitJumpInsn(IFNULL, ifnull);
			mw.visitTypeInsn(CHECKCAST, getType(AbstractFastProperty.class));
			mw.visitVarInsn(ASTORE, 2);		//S0
			
			mw.visitVarInsn(ALOAD, 0);			//S1`
			mw.visitFieldInsn(GETFIELD, accessorType, "fieldAnnoMaps", "[Ljava/util/Map;"); //S1
			
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
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getAnnotationOnGetter", getMethodDesc(java.util.Map.class, String.class),null, null);
			
			mw.visitVarInsn(ALOAD, 0);  //S1
			mw.visitFieldInsn(GETFIELD, accessorType, "fields", "Ljava/util/Map;");		//S1
			mw.visitVarInsn(ALOAD, 1);		//S2
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "get", "(Ljava/lang/Object;)Ljava/lang/Object;"); //S1
			
			
			mw.visitInsn(DUP);  //S2
			Label ifnull=new Label();
			mw.visitJumpInsn(IFNULL, ifnull);//S1
			mw.visitTypeInsn(CHECKCAST, getType(AbstractFastProperty.class));
			mw.visitVarInsn(ASTORE, 2);		//S0
			
			mw.visitVarInsn(ALOAD, 0);			//S1
			mw.visitFieldInsn(GETFIELD, accessorType, "getterAnnoMaps", "[Ljava/util/Map;"); //S1
			
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
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getAnnotationOnSetter", getMethodDesc(java.util.Map.class, String.class),null, null);
			mw.visitVarInsn(ALOAD, 0);  //S1
			mw.visitFieldInsn(GETFIELD, accessorType, "fields", "Ljava/util/Map;");		//S1
			mw.visitVarInsn(ALOAD, 1);		//S2
			mw.visitMethodInsn(INVOKEINTERFACE, mapType, "get", "(Ljava/lang/Object;)Ljava/lang/Object;"); //S1
			
			mw.visitInsn(DUP);  //S2 AbstractFastProperty
			Label ifnull=new Label();
			mw.visitJumpInsn(IFNULL, ifnull); //S1  //如果属性不存在，返回null
			mw.visitTypeInsn(CHECKCAST, getType(AbstractFastProperty.class));
			mw.visitVarInsn(ASTORE, 2);		//S0
			
			mw.visitVarInsn(ALOAD, 0);			//S1 this
			mw.visitFieldInsn(GETFIELD, accessorType, "setterAnnoMaps", "[Ljava/util/Map;"); //S1
			
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
		super.generatePublicMethods(cw);
		
	
		cw.visitEnd();
		return cw.toByteArray();
	}

	private void generatePropertyClz() {
		for (int i = 0; i < fields.length; i++) {
			FieldInfo fi=fields[i];
			String pname = this.accessorName + "$" + fi.getName();
			byte[] data=generateProperty(i, fi,pname);
			//DEBUG
//			ASMAccessorFactory.saveClass(data, pname);
			Class<?> clz= cl.defineClz(pname, data);
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
			mw.visitIntInsn(ALOAD,1);//S1
			mw.visitTypeInsn(CHECKCAST, beanType);
			mw.visitIntInsn(ASTORE,3);//S0
			
			mw.visitIntInsn(ALOAD,3);
			mw.visitIntInsn(ALOAD,2);	//S2
			
			if(fi.isPrimitive()){
				Class<?> wrpped=Primitives.toWrapperClass(fi.getRawType());
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
			mw.visitIntInsn(ALOAD,1);
			mw.visitTypeInsn(CHECKCAST, beanType);
			mw.visitIntInsn(ASTORE,2);
			
			mw.visitIntInsn(ALOAD,0);
			mw.visitIntInsn(ALOAD,2);
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
