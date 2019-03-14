package jef.codegen;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import javax.persistence.Entity;

import org.apache.commons.lang.ArrayUtils;

import com.github.geequery.asm.ClassReader;
import com.github.geequery.asm.ClassVisitor;
import com.github.geequery.asm.ClassWriter;
import com.github.geequery.asm.FieldVisitor;
import com.github.geequery.asm.Label;
import com.github.geequery.asm.MethodVisitor;
import com.github.geequery.asm.Opcodes;
import com.github.geequery.asm.Type;
import com.github.geequery.entity.Entities;

import jef.accelerator.asm.ASMUtils;
import jef.accelerator.asm.commons.AnnotationFetcher;
import jef.common.log.LogUtil;
import jef.database.annotation.EasyEntity;
import jef.tools.Assert;
import jef.tools.IOUtils;
import jef.tools.resource.ResourceLoader;

public class EnhanceTaskASM {
	private ResourceLoader root;

	public EnhanceTaskASM(ResourceLoader root) {
		super();
		this.root = root;
	}

	public EnhanceTaskASM() {
	}

	/**
	 * 
	 * @param classdata
	 * @param fieldEumData
	 *            允许传入null
	 * @return 返回null表示不需要增强，返回byte[0]表示该类已经增强，返回其他数据为增强后的class
	 * @throws Exception
	 */
	public byte[] doEnhance(byte[] classdata, byte[] fieldEumData) throws Exception {
		Assert.notNull(classdata);
		List<String> enumFields = parseEnumFields(fieldEumData);
		try {
			ClassReader reader = new ClassReader(classdata);
			if ((reader.getAccess() & Opcodes.ACC_PUBLIC) == 0) {
				return null;// 非公有跳过
			}
			boolean hasEntityAnnotation = AnnotationFetcher.findAny(reader, Entity.class.getName(), EasyEntity.class.getName()) != null;

			int isEntityInterface = isEntityClass(reader.getInterfaces(), reader.getSuperName(), POJO_ENTITY);
			if (isEntityInterface == NOT_ENTITY) {
				return null;
			}
			byte[] data;
			if (isEntityInterface == POJO_ENTITY && hasEntityAnnotation) {
				data = enhancePojoClass(reader, enumFields);
			} else if(isEntityInterface==ENTITY){
				if(!hasEntityAnnotation) {
					LogUtil.warn("The entity class {} has no @Entity annotation, this is a deprecated way, please add @Entity.",ASMUtils.getJavaClassName(reader));
				}
				data = enhanceClass(reader, enumFields);
			}else{
				LogUtil.warn("The class {} has no @Entity annotation, and will not be treated as an Entity",ASMUtils.getJavaClassName(reader));
				return null;
			}
			{
				// DEBUG
				// File file = new File("c:/asm/" +
				// StringUtils.substringAfterLast(className, ".") + ".class");
				// IOUtils.saveAsFile(file, data);
				// System.out.println(file +
				// " saved -- Enhanced class"+className);
			}
			return data;
		} catch (EnhancedException e) {
			return ArrayUtils.EMPTY_BYTE_ARRAY;
		}

	}

	public byte[] enhanceClass(ClassReader reader, List<String> enumFields) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		reader.accept(new EnhanceVisitor(cw, enumFields, new EnhanceHandler() {
			@Override
			public MethodVisitor newGetterVisitor(MethodVisitor mv, String name, String typeName) {
				return new GetterVisitor(mv, name, typeName);
			}

			@Override
			public MethodVisitor newSetterVisitor(MethodVisitor mv, String name, String typeName, Type paramType, int index) {
				return new SetterVisitor(mv, name, typeName, paramType, index);
			}

			@Override
			public MethodVisitor newSetterOfClearLazyload(MethodVisitor mv, String name, String typeName) {
				return new SetterOfClearLazyload(mv, name, typeName);
			}
		}), 0);
		return cw.toByteArray();
	}

	private byte[] enhancePojoClass(ClassReader reader, List<String> enumFields) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		reader.accept(new EnhanceVisitor(cw, enumFields, new EnhanceHandler() {
			@Override
			public MethodVisitor newGetterVisitor(MethodVisitor mv, String name, String type) {
				return new POJOGetterVisitor(mv, name);
			}

			@Override
			public MethodVisitor newSetterVisitor(MethodVisitor mv, String name, String typeName, Type type, int index) {
				return new POJOSetterVisitor(mv, typeName, index);
			}

			@Override
			public MethodVisitor newSetterOfClearLazyload(MethodVisitor mv, String name, String typeName) {
				return new POJOSetterOfClearLazyload(mv, name);
			}

			@Override
			public void beforeEnd(ClassVisitor classVisitor) {
				classVisitor.visitField(Modifier.TRANSIENT | Modifier.PUBLIC, "___touchRecord", ASMUtils.getDesc(BitSet.class), null, null);
				classVisitor.visitField(Modifier.TRANSIENT | Modifier.PUBLIC, "___lazy", ASMUtils.getDesc(jef.database.ILazyLoadContext.class), null, null);
				classVisitor.visitField(Modifier.TRANSIENT | Modifier.PUBLIC, "___notTouch", ASMUtils.getDesc(boolean.class), null, false);
			}
		}), 0);
		return cw.toByteArray();
	}

	public List<String> parseEnumFields(byte[] fieldEumData) {
		final List<String> enumFields = new ArrayList<String>();
		if (fieldEumData != null) {
			ClassReader reader = new ClassReader(fieldEumData);
			reader.accept(new ClassVisitor(Opcodes.ASM7) {
				@Override
				public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
					if ((access & Opcodes.ACC_ENUM) > 0) {
						enumFields.add(name);
					}
					return null;
				}
			}, ClassReader.SKIP_CODE);
		}
		return enumFields;
	}

	private static final int NOT_ENTITY = 0;
	private static final int ENTITY = 1;
	private static final int POJO_ENTITY = 2;

	/**
	 * 
	 * @param interfaces
	 * @param superName
	 * @param defaultValue
	 * @return
	 */
	private int isEntityClass(String[] interfaces, String superName, int defaultValue) {
		if ("jef/database/DataObject".equals(superName))
			return ENTITY;// 绝大多数实体都是继承这个类的
		if (ArrayUtils.contains(interfaces, "Ljef/database/IQueryableEntity;")) {
			return ENTITY;
		}
		if ("java/lang/Object".equals(superName)) {
			return POJO_ENTITY;
		}

		// 递归检查父类
		ClassReader cl = null;
		try {
			URL url = ClassLoader.getSystemResource(superName + ".class");
			if (url == null && root != null) {
				if (root != null) {
					url = root.getResource(superName + ".class");
				}
			}
			if (url == null) { // 父类找不到，无法准确判断
				return defaultValue;
			}
			byte[] parent = IOUtils.toByteArray(url);
			cl = new ClassReader(parent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (cl != null) {
			return isEntityClass(cl.getInterfaces(), cl.getSuperName(), defaultValue);
		}
		return POJO_ENTITY;
	}

	// public byte[] getBinaryData_x();
	// Code:
	// 0: aload_0
	// 1: ldc #117; //String binaryData
	// 3: invokevirtual #118; //Method beforeGet:(Ljava/lang/String;)V
	// 6: aload_0
	// 7: getfield #121; //Field binaryData:[B
	// 10: areturn
	static class GetterVisitor extends MethodVisitor implements Opcodes {
		private String name;
		private String typeName;

		public GetterVisitor(MethodVisitor mv, String name, String typeName) {
			super(Opcodes.ASM7, mv);
			this.name = name;
			this.typeName = typeName;
		}

		public void visitCode() {
			mv.visitIntInsn(ALOAD, 0);
			mv.visitLdcInsn(name);
			mv.visitMethodInsn(INVOKEVIRTUAL, typeName, "beforeGet", "(Ljava/lang/String;)V", false);
			super.visitCode();
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			mv.visitMaxs(Math.max(maxStack, 2), maxLocals);
		}

		// 去除本地变量表。
		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		}
	}

	static class POJOGetterVisitor extends MethodVisitor implements Opcodes {
		private String name;

		public POJOGetterVisitor(MethodVisitor mv, String name) {
			super(Opcodes.ASM7, mv);

			this.name = name;
		}

		public void visitCode() {
			mv.visitIntInsn(ALOAD, 0);
			mv.visitLdcInsn(name);
			mv.visitMethodInsn(INVOKESTATIC, ASMUtils.getDesc(Entities.class), "beforeGet", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
			super.visitCode();
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			mv.visitMaxs(Math.max(maxStack, 2), maxLocals);
		}

		// 去除本地变量表。
		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		}
	}

	static class SetterOfClearLazyload extends MethodVisitor implements Opcodes {
		private String name;
		private String typeName;

		public SetterOfClearLazyload(MethodVisitor mv, String name, String typeName) {
			super(Opcodes.ASM7, mv);
			this.name = name;
			this.typeName = typeName;
		}

		// 去除本地变量表。否则生成的类用jd-gui反编译时，添加的代码段无法正常反编译
		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		}

		public void visitCode() {
			mv.visitIntInsn(ALOAD, 0);
			mv.visitLdcInsn(name);
			mv.visitMethodInsn(INVOKEVIRTUAL, typeName, "beforeSet", "(Ljava/lang/String;)V", false);
			super.visitCode();
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			mv.visitMaxs(Math.max(maxStack, 4), maxLocals);
		}
	}

	static class POJOSetterOfClearLazyload extends MethodVisitor implements Opcodes {
		private String name;

		public POJOSetterOfClearLazyload(MethodVisitor mv, String name) {
			super(Opcodes.ASM7, mv);
			this.name = name;
		}

		// 0: aload_0
		// 1: ldc #98 // String children
		// 3: invokestatic #104 // Method
		// com/github/geequery/entity/Entities.beforeRefSet:(Ljava/lang/Object;Ljava/lang/String;)V
		@Override
		public void visitCode() {
			mv.visitIntInsn(ALOAD, 0);
			mv.visitLdcInsn(name);
			mv.visitMethodInsn(INVOKESTATIC, ASMUtils.getDesc(Entities.class), "beforeRefSet", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
			super.visitCode();
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			mv.visitMaxs(Math.max(maxStack, 2), maxLocals);
		}

		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		}
	}

	//
	// Code:
	// 0: aload_0
	// 1: getfield #125; //Field _recordUpdate:Z
	// 4: ifeq 16
	// 7: aload_0
	// 8: getstatic #128; //Field
	// jef/orm/onetable/model/TestEntity$Field.binaryData:Ljef/orm/onetable/model/TestEntity$Field;
	// 11: aload_1
	// 13: invokevirtual #133; //Method
	// prepareUpdate:(Ljef/database/Field;Ljava/lang/Object;)V
	// 16: aload_0
	// 17: aload_1
	// 18: putfield #121; //Field binaryData:[B
	// 21: return

	static class SetterVisitor extends MethodVisitor implements Opcodes {
		private String name;
		private String typeName;
		private Type paramType;
		private int index;

		public SetterVisitor(MethodVisitor mv, String name, String typeName, Type paramType, int index) {
			super(Opcodes.ASM7, mv);
			this.name = name;
			this.typeName = typeName;
			this.paramType = paramType;
			this.index = index;
		}

		// 去除本地变量表。否则生成的类用jd-gui反编译时，添加的代码段无法正常反编译
		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		}

		public void visitCode() {
			mv.visitIntInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, typeName, "_recordUpdate", "Z");
			Label norecord = new Label();
			mv.visitJumpInsn(IFEQ, norecord);
			mv.visitIntInsn(ALOAD, 0);
			ASMUtils.iconst(mv, index);
			mv.visitMethodInsn(INVOKEVIRTUAL, typeName, "_touch", "(I)V", false);
			mv.visitLabel(norecord);
			super.visitCode();

		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			mv.visitMaxs(Math.max(maxStack, 2), maxLocals);
		}
	}

	static class POJOSetterVisitor extends MethodVisitor implements Opcodes {
		private String typeName;
		private int index;

		// 去除本地变量表。否则生成的类用jd-gui反编译时，添加的代码段无法正常反编译
		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		}

		public POJOSetterVisitor(MethodVisitor mv, String typeName, int index) {
			super(Opcodes.ASM7, mv);
			this.index = index;
			this.typeName = typeName;
		}

		// 0: aload_0
		// 1: getfield #92 // Field ___notTouch:Z
		// 4: ifne 13
		// 7: aload_0
		// 8: bipush 6
		// 10: invokestatic #49 // Method
		// com/github/geequery/entity/Entities.beforeSet:(Ljava/lang/Object;I)V
		@Override
		public void visitCode() {
			mv.visitIntInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, typeName, "___notTouch", "Z");
			Label norecord = new Label();
			mv.visitJumpInsn(IFNE, norecord);

			mv.visitIntInsn(ALOAD, 0);
			ASMUtils.iconst(mv, index);
			mv.visitMethodInsn(INVOKESTATIC, ASMUtils.getDesc(Entities.class), "beforeSet", "(Ljava/lang/Object;I)V", false);
			mv.visitLabel(norecord);
			super.visitCode();
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			mv.visitMaxs(Math.max(maxStack, 2), maxLocals);
		}
	}

}
