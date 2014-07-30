package org.common;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import jef.accelerator.asm.AnnotationVisitor;
import jef.accelerator.asm.Attribute;
import jef.accelerator.asm.ClassReader;
import jef.accelerator.asm.ClassVisitor;
import jef.accelerator.asm.ClassWriter;
import jef.accelerator.asm.FieldVisitor;
import jef.accelerator.asm.MethodVisitor;
import jef.accelerator.asm.Opcodes;
import jef.tools.IOUtils;

import org.junit.Test;


public class ASMTest extends ClassLoader implements Opcodes {
	@Test
	public void createTest() throws Exception {

		// creates a ClassWriter for the Example public class,
		// which inherits from Object

		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_5, ACC_PUBLIC, "Example", null, "java/lang/Object", null);
		MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mw.visitVarInsn(ALOAD, 0);
		mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		mw.visitInsn(RETURN);
		mw.visitMaxs(1, 1);
		mw.visitEnd();
		mw = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		mw.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mw.visitLdcInsn("Hello world!");
		mw.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		mw.visitInsn(RETURN);
		mw.visitMaxs(2, 2);
		mw.visitEnd();
		byte[] code = cw.toByteArray();
		IOUtils.saveAsFile(new File("c:/temp/ssss1.class"), code);

		ASMTest loader = new ASMTest();
		Class exampleClass = loader.defineClass("Example", code, 0, code.length);

		exampleClass.getMethods()[0].invoke(null, new Object[] { null });
	}
	
	@Test
	public void readClassTest() throws Exception{
		
		ClassReader reader=new ClassReader("org.common.ASMTest");
		reader.accept(new ClassVisitor(){

			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				System.out.println(name);
				super.visit(version, access, name, signature, superName, interfaces);
			}

			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				// TODO Auto-generated method stub
				return super.visitAnnotation(desc, visible);
			}

			@Override
			public void visitAttribute(Attribute attr) {
				// TODO Auto-generated method stub
				super.visitAttribute(attr);
			}

			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				// TODO Auto-generated method stub
				return super.visitField(access, name, desc, signature, value);
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				// TODO Auto-generated method stub
				return super.visitMethod(access, name, desc, signature, exceptions);
			}

			@Override
			public void visitEnd() {
				// TODO Auto-generated method stub
				super.visitEnd();
			}
			
		}, ClassReader.SKIP_DEBUG);
		
		
	}
	
	@Test
	public void test123(){
		List<String> args=Arrays.asList("assdasdsa","ss","4");
		StringBuffer buf=new StringBuffer();
		boolean threeArgs=true;
		Object pattern = args.get(0);
		Object string = args.get(1);
		Object start = threeArgs ? args.get(2) : null;
		
		if (threeArgs) buf.append('(');
		buf.append("position(").append( pattern ).append(" in ");
		if (threeArgs) buf.append( "substring(");
		buf.append( string );
		if (threeArgs) buf.append( ", " ).append( start ).append(')');
		buf.append(')');
		if (threeArgs) buf.append('+').append( start ).append("-1)");
		System.out.println(buf.toString());
	}
}
