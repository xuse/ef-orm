package jef.codegen;

import com.github.geequery.asm.ClassVisitor;
import com.github.geequery.asm.MethodVisitor;
import com.github.geequery.asm.Type;

public interface EnhanceHandler {
	MethodVisitor newGetterVisitor(MethodVisitor mv, String name, String typeName);
	
	MethodVisitor newSetterVisitor(MethodVisitor mv, String name, String typeName, Type type, int index);

	MethodVisitor newSetterOfClearLazyload(MethodVisitor mv, String name, String typeName);

	default void beforeEnd(ClassVisitor classVisitor) {
	};
}
