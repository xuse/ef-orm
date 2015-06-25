package jef.accelerator.bean;

import static jef.accelerator.asm.ASMUtils.getDesc;
import static jef.accelerator.asm.ASMUtils.getMethodDesc;
import static jef.accelerator.asm.ASMUtils.getType;
import static jef.accelerator.asm.ASMUtils.iconst;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jef.accelerator.asm.ASMUtils;
import jef.accelerator.asm.ClassWriter;
import jef.accelerator.asm.Label;
import jef.accelerator.asm.MethodVisitor;
import jef.accelerator.asm.Opcodes;
import jef.accelerator.asm.Type;
import jef.tools.reflect.BeanUtils;

public abstract class ClassGenerator implements Opcodes {
	protected Class<?> beanClass;
	protected String beanType;
	protected String accessorName;
	protected String accessorType;
	protected ClassLoader cl;
	protected FieldInfo[] fields;

	protected ClassGenerator(Class<?> javaBean, String accessorName, FieldInfo[] fields, ClassLoader cl) {
		this.beanClass = javaBean;
		this.beanType = getType(beanClass);

		this.accessorName = accessorName;
		this.accessorType = accessorName.replace('.', '/');

		this.fields = fields;
		this.cl = cl;
	}

	abstract byte[] generate();

	public void generatePublicMethods(ClassWriter cw) {
		generateCopy(cw);
		generateConvert(cw);
		generateFromMap(cw);
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "newInstance", getMethodDesc(Object.class), null, null);
			mw.visitTypeInsn(NEW, beanType);
			try {
				// 运行空构造方法
				if (beanClass.getDeclaredConstructor() != null) {
					mw.visitInsn(DUP);
					mw.visitMethodInsn(INVOKESPECIAL, beanType, "<init>", "()V");
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
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "getType", getMethodDesc(Class.class), null, null);
			mw.visitLdcInsn(Type.getType(beanClass));// S1
			mw.visitInsn(ARETURN);
			mw.visitMaxs(1, 1);
			mw.visitEnd();
		}
	}

	private void generateFromMap(ClassWriter cw) {
		MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "fromMap", getMethodDesc(Object.class, Map.class), null, null);
		mw.visitTypeInsn(NEW, beanType);//S1 L2
		try {
			// 运行空构造方法
			if (beanClass.getDeclaredConstructor() != null) {
				mw.visitInsn(DUP);
				mw.visitMethodInsn(INVOKESPECIAL, beanType, "<init>", "()V");
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		mw.visitVarInsn(ASTORE, 2);// L3 S0
		
		for (int i = 0; i < fields.length; i++) {
			FieldInfo fi = fields[i];
			mw.visitVarInsn(ALOAD, 2);// S1 the object
			
			mw.visitVarInsn(ALOAD, 1);// S2 the map
			mw.visitLdcInsn(fi.getName());// S3 key
			mw.visitMethodInsn(INVOKEINTERFACE, getType(Map.class), "get", "(Ljava/lang/Object;)Ljava/lang/Object;"); //S2 value
			
			if (fi.isPrimitive()) {
				Class<?> wrpped=BeanUtils.toWrapperClass(fi.getRawType());
				mw.visitTypeInsn(CHECKCAST, getType(wrpped));	//S2 value
				ASMUtils.doUnwrap(mw, fi.getRawType(), wrpped);
			}else {
				mw.visitTypeInsn(CHECKCAST, getType(fi.getRawType()));	//S2 value
			}
			this.generateInvokeMethod(mw, fi.getSetter());
			if (fi.getSetter().getReturnType() != void.class) {
				mw.visitInsn(POP);
			}
		}
		mw.visitVarInsn(ALOAD, 2);
		mw.visitInsn(ARETURN);
		mw.visitMaxs(3,3);
		mw.visitEnd();
	}

	private void generateConvert(ClassWriter cw) {
		MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "convert", getMethodDesc(Map.class, Object.class), null, null);
		// L2 S0
		Label checkArg = new Label();

		mw.visitVarInsn(ALOAD, 1);// S1
		mw.visitJumpInsn(IFNONNULL, checkArg);
		mw.visitInsn(Opcodes.ACONST_NULL);
		mw.visitInsn(ARETURN);

		mw.visitLabel(checkArg);

		mw.visitVarInsn(ALOAD, 1);// S1
		mw.visitTypeInsn(CHECKCAST, beanType);
		mw.visitVarInsn(ASTORE, 2);// L3 S0

		mw.visitTypeInsn(NEW, getType(HashMap.class));// S1
		mw.visitInsn(DUP); // S2
		iconst(mw, fields.length); // S3
		mw.visitMethodInsn(INVOKESPECIAL, getType(HashMap.class), "<init>", "(I)V");// S1
		mw.visitVarInsn(ASTORE, 3);// L4 S0

		for (int i = 0; i < fields.length; i++) {
			FieldInfo fi = fields[i];
			mw.visitVarInsn(ALOAD, 3);// S1
			mw.visitLdcInsn(fi.getName());// S2

			mw.visitVarInsn(ALOAD, 2);// S3
			Method getter = fi.getGetter();
			generateInvokeMethod(mw, getter);// S3
			if (fi.isPrimitive()) {
				ASMUtils.doWrap(mw, fi.getRawType());
			}

			mw.visitMethodInsn(INVOKEVIRTUAL, getType(HashMap.class), "put", getMethodDesc(Object.class, Object.class, Object.class));
			mw.visitInsn(POP);
		}
		mw.visitVarInsn(ALOAD, 3);
		mw.visitInsn(ARETURN);
		mw.visitMaxs(4, 4);
		mw.visitEnd();
	}

	private void generateCopy(ClassWriter cw) {

		MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "copy", getMethodDesc(Void.TYPE, Object.class, Object.class), null, null);

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
			if (fields[i].getSetter().getReturnType() != void.class) {
				mw.visitInsn(POP);
			}
		}
		mw.visitInsn(RETURN);
		mw.visitMaxs(3, 5);
		mw.visitEnd();

	}

	protected void generateInvokeMethod(MethodVisitor mw, Method m) {
		mw.visitMethodInsn(INVOKEVIRTUAL, getType(m.getDeclaringClass()), m.getName(), getDesc(m));
	}
}
