package jef.accelerator.asm.commons;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang.ArrayUtils;

import com.github.geequery.asm.AnnotationVisitor;
import com.github.geequery.asm.ClassReader;
import com.github.geequery.asm.ClassVisitor;
import com.github.geequery.asm.Opcodes;

/*
 * 用于从类上提取注解
 */
public class AnnotationFetcher extends ClassVisitor {
	private final List<AnnotationData> typeAnnotation = new ArrayList<>();

	public AnnotationFetcher() {
		super(Opcodes.ASM7);
	}

	static final class AnnotationVisitorImpl extends AnnotationVisitor {
		private final AnnotationData data;
		private Consumer<AnnotationData> callback;

		AnnotationVisitorImpl(String desc, boolean visible, Consumer<AnnotationData> callback) {
			super(Opcodes.ASM7);
			this.data = new AnnotationData(desc, visible);
			this.callback = callback;
		}

		@Override
		public void visit(String name, Object value) {
			data.put(name, value);
		}

		@Override
		public void visitEnum(String name, String desc, String value) {
			data.put(name, new EnumDef(desc, value));
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String desc) {
			return new AnnotationVisitorImpl(desc, data.isVisible(), e -> data.put(name, e));
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			return new ArrayAnnotationVisitor(data.isVisible(), e -> data.put(name, e));
		}

		@Override
		public void visitEnd() {
			if (callback != null) {
				callback.accept(data);
			}
		}
	}

	static final class ArrayAnnotationVisitor extends AnnotationVisitor {
		private boolean visible;

		public ArrayAnnotationVisitor(boolean visiable, Consumer<List<Object>> callback) {
			super(Opcodes.ASM7);
			this.callback = callback;
			this.visible = visiable;
		}

		@Override
		public void visit(String name, Object value) {
			data.add(value);
		}

		@Override
		public void visitEnum(String name, String desc, String value) {
			data.add(new EnumDef(desc, value));
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String desc) {
			return new AnnotationVisitorImpl(desc, visible, e -> data.add(e));
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			return new ArrayAnnotationVisitor(visible, e -> data.add(e));
		}

		@Override
		public void visitEnd() {
			if (callback != null) {
				callback.accept(data);
			}
		}

		private final List<Object> data = new ArrayList<>();
		private Consumer<List<Object>> callback;

	}

	public List<AnnotationData> getTypeAnnotation() {
		return typeAnnotation;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return new AnnotationVisitorImpl(desc, visible, e -> typeAnnotation.add(e));
	}

	public static List<AnnotationData> onClass(ClassReader reader) {
		AnnotationFetcher af = new AnnotationFetcher();
		reader.accept(af, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
		return af.getTypeAnnotation();
	}

	public static AnnotationData findAny(ClassReader reader, String... ccs) {
		List<AnnotationData> list = onClass(reader);
		for (AnnotationData data : list) {
			if (ArrayUtils.contains(ccs, data.getClassname())) {
				return data;
			}
		}
		return null;
	}

	public static AnnotationData find(ClassReader reader, Class<? extends Annotation> cs) {
		List<AnnotationData> list = onClass(reader);
		for (AnnotationData data : list) {
			if (data.getClassname().equals(cs.getName())) {
				return data;
			}
		}
		return null;
	}

}
