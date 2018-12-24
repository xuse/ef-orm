package jef.accelerator.bean;

import static jef.accelerator.asm.ASMUtils.getDesc;
import static jef.accelerator.asm.ASMUtils.getMethodDesc;
import static jef.accelerator.asm.ASMUtils.getType;
import static jef.accelerator.asm.ASMUtils.iconst;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.github.geequery.asm.ClassWriter;
import com.github.geequery.asm.Label;
import com.github.geequery.asm.MethodVisitor;
import com.github.geequery.asm.Opcodes;
import com.github.geequery.asm.Type;

import jef.accelerator.asm.ASMUtils;
import jef.tools.Primitives;
import jef.tools.reflect.ConvertUtils;

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
					mw.visitMethodInsn(INVOKESPECIAL, beanType, "<init>", "()V", false);
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
				Label notnull=new Label();
				Label end=new Label();
				mw.visitInsn(DUP); // S3
				mw.visitJumpInsn(IFNONNULL, notnull);//S2
				mw.visitInsn(POP2);//S0
				mw.visitJumpInsn(GOTO,end);
				mw.visitLabel(notnull);
				Class<?> wrpped=Primitives.toWrapperClass(fi.getRawType());
				mw.visitTypeInsn(CHECKCAST, getType(wrpped));	//S2 value
				ASMUtils.doUnwrap(mw, fi.getRawType(), wrpped);
				this.generateInvokeMethod(mw, fi.getSetter());//S0
				if (fi.getSetter().getReturnType() != void.class) {
					mw.visitInsn(POP);
				}
				mw.visitLabel(end);
			}else {
				mw.visitTypeInsn(CHECKCAST, getType(fi.getRawType()));	//S2 value
				this.generateInvokeMethod(mw, fi.getSetter());
				if (fi.getSetter().getReturnType() != void.class) {
					mw.visitInsn(POP);
				}
			}
		}
		mw.visitVarInsn(ALOAD, 2);
		mw.visitInsn(ARETURN);
		mw.visitMaxs(3,3);
		mw.visitEnd();
		
		
		mw = cw.visitMethod(ACC_PUBLIC, "fromMap2", getMethodDesc(Object.class, Map.class), null, null);
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

			pushGenericType(mw,fi.getRawType(),fi.getName());
			mw.visitMethodInsn(INVOKESTATIC, getType(ConvertUtils.class), "toProperType", "(Ljava/lang/Object;Ljava/lang/reflect/Type;)Ljava/lang/Object;"); //S2 value
			
			if (fi.isPrimitive()) {
				Label notnull=new Label();
				Label end=new Label();
				mw.visitInsn(DUP); // S3
				mw.visitJumpInsn(IFNONNULL, notnull);//S2
				mw.visitInsn(POP2);//S0
				mw.visitJumpInsn(GOTO,end);
				mw.visitLabel(notnull);
				Class<?> wrpped=Primitives.toWrapperClass(fi.getRawType());
				mw.visitTypeInsn(CHECKCAST, getType(wrpped));	//S2 value
				ASMUtils.doUnwrap(mw, fi.getRawType(), wrpped);
				this.generateInvokeMethod(mw, fi.getSetter());//S0
				if (fi.getSetter().getReturnType() != void.class) {
					mw.visitInsn(POP);
				}
				mw.visitLabel(end);
			}else {
				mw.visitTypeInsn(CHECKCAST, getType(fi.getRawType()));	//S2 value
				this.generateInvokeMethod(mw, fi.getSetter());
				if (fi.getSetter().getReturnType() != void.class) {
					mw.visitInsn(POP);
				}
			}
		}
		mw.visitVarInsn(ALOAD, 2);
		mw.visitInsn(ARETURN);
		mw.visitMaxs(4,3);
		mw.visitEnd();
	}

	private void pushGenericType(MethodVisitor mw,Class<?> rawType,String pname) {
		if(rawType.isPrimitive()) {
			String s = rawType.getName();
			// int 226
			// short 215
			// long 221
			// boolean 222
			// float 219
			// double 228
			// char 201
			// byte 237
			switch (s.charAt(1) + s.charAt(2)) {
			case 226:
				mw.visitFieldInsn(GETSTATIC, getType(Integer.class), "TYPE", "Ljava/lang/Class;");
				break;
			case 215:
				mw.visitFieldInsn(GETSTATIC, getType(Short.class), "TYPE", "Ljava/lang/Class;");
				break;
			case 221:
				mw.visitFieldInsn(GETSTATIC, getType(Long.class), "TYPE", "Ljava/lang/Class;");
				break;
			case 222:
				mw.visitFieldInsn(GETSTATIC, getType(Boolean.class), "TYPE", "Ljava/lang/Class;");
				break;
			case 219:
				mw.visitFieldInsn(GETSTATIC, getType(Float.class), "TYPE", "Ljava/lang/Class;");
				break;
			case 228:
				mw.visitFieldInsn(GETSTATIC, getType(Double.class), "TYPE", "Ljava/lang/Class;");
				break;
			case 201:
				mw.visitFieldInsn(GETSTATIC, getType(Character.class), "TYPE", "Ljava/lang/Class;");
				break;
			case 237:
				mw.visitFieldInsn(GETSTATIC, getType(Byte.class), "TYPE", "Ljava/lang/Class;");
				break;
			}
		}else {
			mw.visitVarInsn(ALOAD, 0);
			mw.visitLdcInsn(pname);// S2
			mw.visitMethodInsn(INVOKEVIRTUAL, accessorType, "getGenericType", getMethodDesc(java.lang.reflect.Type.class,String.class));
			
		}
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
