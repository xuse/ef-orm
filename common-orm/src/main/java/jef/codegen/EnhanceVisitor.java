package jef.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.github.geequery.asm.Attribute;
import com.github.geequery.asm.ClassVisitor;
import com.github.geequery.asm.FieldVisitor;
import com.github.geequery.asm.MethodVisitor;
import com.github.geequery.asm.Opcodes;
import com.github.geequery.asm.Type;

import jef.accelerator.asm.commons.AnnotationDef;
import jef.accelerator.asm.commons.FieldExtCallback;
import jef.accelerator.asm.commons.FieldExtDef;
import jef.tools.StringUtils;

final class EnhanceVisitor extends ClassVisitor {
	private List<String> enumFields;
	private EnhanceHandler handler;

	public EnhanceVisitor(ClassVisitor cv, List<String> enumFields, EnhanceHandler handler) {
		super(Opcodes.ASM7, cv);
		this.enumFields = enumFields;
		this.handler = handler;
	}

	private List<String> nonStaticFields = new ArrayList<String>();
	private List<String> lobAndRefFields = new ArrayList<String>();
	private String typeName;

	@Override
	public void visit(int version, int access, String name, String sig, String superName, String[] interfaces) {
		this.typeName = name.replace('.', '/');
		super.visit(version, access, name, sig, superName, interfaces);
	}

	@Override
	public void visitAttribute(Attribute attr) {
		if ("jefd".equals(attr.type)) {
			throw new EnhancedException();
		}
		super.visitAttribute(attr);
	}

	@Override
	public void visitEnd() {
		handler.beforeEnd(super.cv);
		Attribute attr = new Attribute("jefd", new byte[] { 0x1f });
		super.visitAttribute(attr);
	}

	@Override
	public FieldVisitor visitField(final int access, final String name, final String desc, String sig, final Object value) {
		FieldVisitor visitor = super.visitField(access, name, desc, sig, value);
		if ((access & Opcodes.ACC_STATIC) > 0)
			return visitor;
		nonStaticFields.add(name);
		return new FieldExtDef(Opcodes.ASM7, new FieldExtCallback(visitor) {
			public void onFieldRead(FieldExtDef info) {
				boolean contains = enumFields.contains(name);
				if (contains) {
					if (!info.getAnnotation("Ljavax/persistence/Lob;").isEmpty()) {
						lobAndRefFields.add(name);
					}
				} else {
					Collection<AnnotationDef> o = info.getAnnotation(OneToMany.class);
					if (o.isEmpty())
						o = info.getAnnotation(ManyToOne.class);
					if (o.isEmpty())
						o = info.getAnnotation(ManyToMany.class);
					if (o.isEmpty())
						o = info.getAnnotation(OneToOne.class);
					// 判断完成
					if (!o.isEmpty()) {
						lobAndRefFields.add(name);
					}
				}
			}
		});
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
		String fieldName;
		if (name.startsWith("get")) {
			fieldName = StringUtils.uncapitalize(name.substring(3));
			return asGetter(fieldName, access, name, desc, exceptions, sig);
		} else if (name.startsWith("is")) {
			fieldName = StringUtils.uncapitalize(name.substring(2));
			return asGetter(fieldName, access, name, desc, exceptions, sig);
		} else if (name.startsWith("set")) {
			fieldName = StringUtils.uncapitalize(name.substring(3));
			return asSetter(fieldName, access, name, desc, exceptions, sig);
		}
		return super.visitMethod(access, name, desc, sig, exceptions);
	}

	private MethodVisitor asGetter(String fieldName, int access, String name, String desc, String[] exceptions, String sig) {
		MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);
		Type[] types = Type.getArgumentTypes(desc);
		if (fieldName.length() == 0 || types.length > 0)
			return mv;
		if (lobAndRefFields.contains(fieldName)) {
			return handler.newGetterVisitor(mv, fieldName, typeName);
		}
		return mv;
	}

	private MethodVisitor asSetter(String fieldName, int access, String name, String desc, String[] exceptions, String sig) {
		MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);
		Type[] types = Type.getArgumentTypes(desc);
		if (fieldName.length() == 0 || types.length != 1)
			return mv;
		int index = enumFields.indexOf(fieldName);
		if (index >= 0 && nonStaticFields.contains(fieldName)) {
			return handler.newSetterVisitor(mv, fieldName, typeName, types[0], index);
		} else if (lobAndRefFields.contains(fieldName)) {
			return handler.newSetterOfClearLazyload(mv, fieldName, typeName);
		} else {
			String altFieldName = "is" + StringUtils.capitalize(fieldName);
			// 特定情况，当boolean类型并且field名称是isXXX，setter是setXXX()
			index = enumFields.indexOf(altFieldName);
			if (index >= 0) {
				return handler.newSetterVisitor(mv, altFieldName, typeName, types[0], index);
			}
		}
		return mv;
	}

}
