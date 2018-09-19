package jef.accelerator.bean;

import static jef.accelerator.asm.ASMUtils.doUnwrap;
import static jef.accelerator.asm.ASMUtils.doWrap;
import static jef.accelerator.asm.ASMUtils.getDesc;
import static jef.accelerator.asm.ASMUtils.getMethodDesc;
import static jef.accelerator.asm.ASMUtils.getPrimitiveType;
import static jef.accelerator.asm.ASMUtils.getType;
import static jef.accelerator.asm.ASMUtils.iconst;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import com.github.geequery.asm.ClassWriter;
import com.github.geequery.asm.FieldVisitor;
import com.github.geequery.asm.Label;
import com.github.geequery.asm.MethodVisitor;
import com.github.geequery.asm.Type;

import jef.tools.reflect.BeanUtils;

final class ASMSwitcherGenerator extends ClassGenerator {

	public ASMSwitcherGenerator(Class<?> beanClass, String accessorName, FieldInfo[] fields,ClassLoader cl) {
		super(beanClass,accessorName,fields,cl);
	}

	public byte[] generate() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER + ACC_FINAL, accessorName,null, "jef/accelerator/bean/SwitchBeanAccessor", new String[] {});
		// field
		{
			FieldVisitor fw = cw.visitField(ACC_PRIVATE, "fields", getDesc(java.util.Set.class), null,null);
			fw.visitEnd();

			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getPropertyNames", "()Ljava/util/Collection;", null,null);
			mw.visitVarInsn(ALOAD, 0);
			mw.visitFieldInsn(GETFIELD, accessorType, "fields", getDesc(java.util.Set.class));
			mw.visitInsn(ARETURN);
			mw.visitMaxs(1, 1);
			mw.visitEnd();
		}
		// //构造器
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null,null);
			mw.visitVarInsn(ALOAD, 0);
			mw.visitMethodInsn(INVOKESPECIAL, "jef/accelerator/bean/SwitchBeanAccessor", "<init>", "()V");
			mw.visitTypeInsn(NEW, getType(HashSet.class));
			mw.visitInsn(DUP);

			mw.visitMethodInsn(INVOKESPECIAL, getType(HashSet.class), "<init>", "()V");
			mw.visitVarInsn(ASTORE, 1);
			mw.visitVarInsn(ALOAD, 0);
			mw.visitVarInsn(ALOAD, 1);
			mw.visitFieldInsn(PUTFIELD, accessorType, "fields", getDesc(java.util.Set.class));

			for (FieldInfo info : fields) {
				mw.visitVarInsn(ALOAD, 1);
				mw.visitLdcInsn(info.getName());
				mw.visitMethodInsn(INVOKEINTERFACE, getType(Set.class), "add", "(Ljava/lang/Object;)Z");
				mw.visitInsn(POP);
			}
			mw.visitInsn(RETURN);
			mw.visitMaxs(2, 2);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getProperty", getMethodDesc(Object.class, Object.class, String.class), null,null);
			mw.visitVarInsn(ALOAD, 1);
			mw.visitTypeInsn(CHECKCAST, beanType);
			mw.visitVarInsn(ASTORE, 3);

			mw.visitVarInsn(ALOAD, 2);
			mw.visitMethodInsn(INVOKEVIRTUAL, getType(String.class), "hashCode", "()I");

			SwitchHelper sw = new SwitchHelper();
			mw.visitLookupSwitchInsn(sw.defaultLabel, sw.hashs, sw.labels);

			for (int i = 0; i < sw.size(); i++) {
				mw.visitLabel(sw.labels[i]);
				mw.visitVarInsn(ALOAD, 3);
				Method getter=sw.f[i].getGetter();
				Class<?> fieldType = getter.getReturnType();
				generateInvokeMethod(mw,getter);
				if (fieldType.isPrimitive()) {
					doWrap(mw, fieldType);
				}
				mw.visitInsn(ARETURN);
			}

			{
				mw.visitLabel(sw.defaultLabel);
				mw.visitTypeInsn(NEW, getType(NoSuchElementException.class));
				mw.visitInsn(DUP);
				mw.visitTypeInsn(NEW, getType(StringBuilder.class));
				mw.visitInsn(DUP);
				mw.visitVarInsn(ALOAD, 2);
				mw.visitMethodInsn(INVOKESPECIAL, getType(StringBuilder.class), "<init>", getMethodDesc(Void.TYPE, String.class));
				mw.visitLdcInsn(" is not exist in " + beanClass.getName());
				mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "append", getMethodDesc(StringBuilder.class, String.class));
				mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "toString", getMethodDesc(String.class));
				mw.visitMethodInsn(INVOKESPECIAL, getType(NoSuchElementException.class), "<init>", getMethodDesc(Void.TYPE, String.class));
				mw.visitInsn(ATHROW);
			}
			mw.visitMaxs(5, 4);
			mw.visitEnd();
		}
		{
			// setProperty
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC , "setProperty", getMethodDesc(Boolean.TYPE, Object.class, String.class, Object.class), null,null);
			mw.visitVarInsn(ALOAD, 1);
			mw.visitTypeInsn(CHECKCAST, beanType);
			mw.visitVarInsn(ASTORE, 4);

			mw.visitVarInsn(ALOAD, 2);
			mw.visitMethodInsn(INVOKEVIRTUAL, getType(String.class), "hashCode", "()I");
			SwitchHelper sw = new SwitchHelper();
			mw.visitLookupSwitchInsn(sw.defaultLabel, sw.hashs, sw.labels);
			Label success = new Label();
			Label ifnull=new Label();
			for (int i = 0; i < sw.size(); i++) {
				mw.visitLabel(sw.labels[i]);
				mw.visitVarInsn(ALOAD, 4);
				mw.visitVarInsn(ALOAD, 3);
				
				Method setter=sw.f[i].getSetter();
				Class<?> type = setter.getParameterTypes()[0];
				if (type.isPrimitive()) {
					mw.visitInsn(DUP);
					mw.visitJumpInsn(IFNULL, ifnull);
					Class<?> wrapped = BeanUtils.toWrapperClass(type);
					mw.visitTypeInsn(CHECKCAST, getType(wrapped));
					doUnwrap(mw, type, wrapped);
				} else {
					mw.visitTypeInsn(CHECKCAST, getType(type));
				}
				generateInvokeMethod(mw, setter);
				if(setter.getReturnType()!=void.class){
					mw.visitInsn(POP); //丢掉
				}
				if (i < sw.size() - 1) {
					mw.visitJumpInsn(GOTO, success);// 最后一个分支不使用goto语句，确保success标签就在之后。
				}
			}
			{//必须在第一位
				mw.visitLabel(success);
				mw.visitInsn(ICONST_1);
				mw.visitInsn(IRETURN);
			}
			{
				mw.visitLabel(sw.defaultLabel);
				mw.visitInsn(ICONST_0);
				mw.visitInsn(IRETURN);
			}
			{
				mw.visitLabel(ifnull);
				mw.visitInsn(POP); //S1
				mw.visitInsn(POP); //S0
				mw.visitInsn(ICONST_1);
				mw.visitInsn(IRETURN);
			}
			mw.visitMaxs(3, 5);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getPropertyType", getMethodDesc(Class.class, String.class), null,null);
			mw.visitVarInsn(ALOAD, 1);
			mw.visitMethodInsn(INVOKEVIRTUAL, getType(String.class), "hashCode", "()I");
			SwitchHelper sw = new SwitchHelper();
			mw.visitLookupSwitchInsn(sw.defaultLabel, sw.hashs, sw.labels);
			for (int i = 0; i < sw.size(); i++) {
				mw.visitLabel(sw.labels[i]);
				if(	fields[i].isPrimitive()){
					getPrimitiveType(mw,fields[i].getRawType());
				}else{
					mw.visitLdcInsn(Type.getType(fields[i].getRawType()));	
				}
				mw.visitInsn(ARETURN);
			}
			{
				mw.visitLabel(sw.defaultLabel);
				mw.visitTypeInsn(NEW, getType(NoSuchElementException.class));
				mw.visitInsn(DUP);
				mw.visitTypeInsn(NEW, getType(StringBuilder.class));
				mw.visitInsn(DUP);
				mw.visitVarInsn(ALOAD, 1);
				mw.visitMethodInsn(INVOKESPECIAL, getType(StringBuilder.class), "<init>", getMethodDesc(Void.TYPE, String.class));
				mw.visitLdcInsn(" is not exist in " + beanClass.getName());
				mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "append", getMethodDesc(StringBuilder.class, String.class));
				mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "toString", getMethodDesc(String.class));
				mw.visitMethodInsn(INVOKESPECIAL, getType(NoSuchElementException.class), "<init>", getMethodDesc(Void.TYPE, String.class));
				mw.visitInsn(ATHROW);
			}
			mw.visitMaxs(5, 2);
			mw.visitEnd();

		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getGenericType", getMethodDesc(java.lang.reflect.Type.class, String.class), null,null);
			mw.visitVarInsn(ALOAD, 1);
			mw.visitMethodInsn(INVOKEVIRTUAL, getType(String.class), "hashCode", "()I");
			SwitchHelper sw = new SwitchHelper();
			mw.visitLookupSwitchInsn(sw.defaultLabel, sw.hashs, sw.labels);
			for (int i = 0; i < sw.size(); i++) {
				mw.visitLabel(sw.labels[i]);
				mw.visitVarInsn(ALOAD, 0);
				mw.visitFieldInsn(GETFIELD, accessorType, "genericType", "[Ljava/lang/reflect/Type;");
				iconst(mw, i);
				mw.visitInsn(AALOAD);
				mw.visitInsn(ARETURN);
			}
			{
				mw.visitLabel(sw.defaultLabel);
				mw.visitTypeInsn(NEW, getType(NoSuchElementException.class));
				mw.visitInsn(DUP);
				mw.visitTypeInsn(NEW, getType(StringBuilder.class));
				mw.visitInsn(DUP);
				mw.visitVarInsn(ALOAD, 1);
				mw.visitMethodInsn(INVOKESPECIAL, getType(StringBuilder.class), "<init>", getMethodDesc(Void.TYPE, String.class));
				mw.visitLdcInsn(" is not exist in " + beanClass.getName());
				mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "append", getMethodDesc(StringBuilder.class, String.class));
				mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "toString", getMethodDesc(String.class));
				mw.visitMethodInsn(INVOKESPECIAL, getType(NoSuchElementException.class), "<init>", getMethodDesc(Void.TYPE, String.class));
				mw.visitInsn(ATHROW);
			}
			mw.visitMaxs(5, 2);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getAnnotationOnField", getMethodDesc(java.util.Map.class, String.class), null,null);
			mw.visitVarInsn(ALOAD, 1);
			mw.visitMethodInsn(INVOKEVIRTUAL, getType(String.class), "hashCode", "()I");
			SwitchHelper sw = new SwitchHelper();
			mw.visitLookupSwitchInsn(sw.defaultLabel, sw.hashs, sw.labels);

			for (int i = 0; i < sw.size(); i++) {
				mw.visitLabel(sw.labels[i]);
				mw.visitVarInsn(ALOAD, 0);
				mw.visitFieldInsn(GETFIELD, accessorType, "fieldAnnoMaps", "[Ljava/util/Map;");
				iconst(mw, i);
				mw.visitInsn(AALOAD);
				mw.visitInsn(ARETURN);
			}
			{
				mw.visitLabel(sw.defaultLabel);
				mw.visitTypeInsn(NEW, getType(NoSuchElementException.class));
				mw.visitInsn(DUP);
				mw.visitTypeInsn(NEW, getType(StringBuilder.class));
				mw.visitInsn(DUP);
				mw.visitVarInsn(ALOAD, 1);
				mw.visitMethodInsn(INVOKESPECIAL, getType(StringBuilder.class), "<init>", getMethodDesc(Void.TYPE, String.class));
				mw.visitLdcInsn(" is not exist in " + beanClass.getName());
				mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "append", getMethodDesc(StringBuilder.class, String.class));
				mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "toString", getMethodDesc(String.class));
				mw.visitMethodInsn(INVOKESPECIAL, getType(NoSuchElementException.class), "<init>", getMethodDesc(Void.TYPE, String.class));
				mw.visitInsn(ATHROW);
			}
			mw.visitMaxs(5, 2);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getAnnotationOnGetter", getMethodDesc(java.util.Map.class, String.class), null,null);
			mw.visitVarInsn(ALOAD, 1);
			mw.visitMethodInsn(INVOKEVIRTUAL, getType(String.class), "hashCode", "()I");
			SwitchHelper sw = new SwitchHelper();
			mw.visitLookupSwitchInsn(sw.defaultLabel, sw.hashs, sw.labels);

			for (int i = 0; i < sw.size(); i++) {
				mw.visitLabel(sw.labels[i]);
				mw.visitVarInsn(ALOAD, 0);
				mw.visitFieldInsn(GETFIELD, accessorType, "getterAnnoMaps", "[Ljava/util/Map;");
				iconst(mw, i);
				mw.visitInsn(AALOAD);
				mw.visitInsn(ARETURN);
			}
			{
				mw.visitLabel(sw.defaultLabel);
				mw.visitTypeInsn(NEW, getType(NoSuchElementException.class));
				mw.visitInsn(DUP);
				mw.visitTypeInsn(NEW, getType(StringBuilder.class));
				mw.visitInsn(DUP);
				mw.visitVarInsn(ALOAD, 1);
				mw.visitMethodInsn(INVOKESPECIAL, getType(StringBuilder.class), "<init>", getMethodDesc(Void.TYPE, String.class));
				mw.visitLdcInsn(" is not exist in " + beanClass.getName());
				mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "append", getMethodDesc(StringBuilder.class, String.class));
				mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "toString", getMethodDesc(String.class));
				mw.visitMethodInsn(INVOKESPECIAL, getType(NoSuchElementException.class), "<init>", getMethodDesc(Void.TYPE, String.class));
				mw.visitInsn(ATHROW);
			}
			mw.visitMaxs(5, 2);
			mw.visitEnd();
		}
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getAnnotationOnSetter", getMethodDesc(java.util.Map.class, String.class), null,null);
			mw.visitVarInsn(ALOAD, 1); //S1
			mw.visitMethodInsn(INVOKEVIRTUAL,getType(String.class), "hashCode",  "()I"); //S1
			SwitchHelper sw = new SwitchHelper();
			mw.visitLookupSwitchInsn(sw.defaultLabel, sw.hashs, sw.labels);

			for (int i = 0; i < sw.size(); i++) {
				mw.visitLabel(sw.labels[i]);
				mw.visitVarInsn(ALOAD, 0);
				mw.visitFieldInsn(GETFIELD, accessorType, "setterAnnoMaps", "[Ljava/util/Map;");
				iconst(mw, i);
				mw.visitInsn(AALOAD);
				mw.visitInsn(ARETURN);
			}
			{
				mw.visitLabel(sw.defaultLabel);
				mw.visitTypeInsn(NEW, getType(NoSuchElementException.class));//S1
				mw.visitInsn(DUP);  //S2
				mw.visitTypeInsn(NEW, getType(StringBuilder.class)); //S3
				mw.visitInsn(DUP); //S4
				mw.visitVarInsn(ALOAD, 1); //S5
				mw.visitMethodInsn(INVOKESPECIAL, getType(StringBuilder.class), "<init>",getMethodDesc(Void.TYPE, String.class));
				mw.visitLdcInsn(" is not exist in " + beanClass.getName());
				mw.visitMethodInsn(INVOKEVIRTUAL,  getType(StringBuilder.class),"append", getMethodDesc(StringBuilder.class, String.class));
				mw.visitMethodInsn(INVOKEVIRTUAL, getType(StringBuilder.class), "toString", getMethodDesc(String.class));
				mw.visitMethodInsn(INVOKESPECIAL,  getType(NoSuchElementException.class),"<init>", getMethodDesc(Void.TYPE, String.class));
				mw.visitInsn(ATHROW);
			}
			mw.visitMaxs(5, 2);
			mw.visitEnd();
		}
		super.generatePublicMethods(cw);
		cw.visitEnd();
		return cw.toByteArray();
	}

	private class SwitchHelper {
		private Label[] labels;
		private int[] hashs;
		private FieldInfo[] f;
		private Label defaultLabel;

		int size() {
			return f.length;
		}

		SwitchHelper() {
			defaultLabel = new Label();
			f = fields;
			labels = new Label[fields.length];
			hashs=new int[fields.length];
			for (int i = 0; i < fields.length; i++) {
				hashs[i] = fields[i].getName().hashCode();
				labels[i] = new Label();
			}
		}
	}
}
