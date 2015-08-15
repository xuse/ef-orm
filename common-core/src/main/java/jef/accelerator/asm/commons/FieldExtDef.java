package jef.accelerator.asm.commons;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jef.accelerator.asm.ASMUtils;
import jef.accelerator.asm.AnnotationVisitor;
import jef.accelerator.asm.Attribute;
import jef.accelerator.asm.FieldVisitor;
import jef.tools.Assert;


public class FieldExtDef extends FieldVisitor {
	private boolean end = false;
	private Map<String, AnnotationDef> annotations = new HashMap<String, AnnotationDef>();
	private Map<String, Attribute> attrs = new LinkedHashMap<String, Attribute>();
	private FieldExtCallback call;

	public FieldExtDef(int api,FieldExtCallback call) {
		super(api,null);
		this.call = call;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		AnnotationDef ann = new AnnotationDef(api,desc);
		ann.visible = visible;
		annotations.put(desc, ann);
		return ann;
	}

	@Override
	public void visitAttribute(Attribute attr) {
		attrs.put(attr.type, attr);
	}

	@Override
	public void visitEnd() {
		end = true;
		if (call != null) {
			call.onFieldRead(this);
			if(call.visitor!=null){
				this.accept(call.visitor);
			}
		}
	}

	public boolean isEnd() {
		return end;
	}

	public void accept(FieldVisitor to) {
		Assert.isTrue(end);
		for (Attribute attr : attrs.values()) {
			to.visitAttribute(attr);
		}
		for (Map.Entry<String, AnnotationDef> e : annotations.entrySet()) {
			String desc = e.getKey();
			boolean visible = e.getValue().visible;
			AnnotationVisitor too = to.visitAnnotation(desc, visible);
			e.getValue().inject(too);
		}
		to.visitEnd();
	}

	public AnnotationDef getAnnotation(String desc) {
		return annotations.get(desc);
	}

	public AnnotationDef getAnnotation(Class<?> class1) {
		return annotations.get(ASMUtils.getDesc(class1));
	}

	public Attribute getAttribute(String type) {
		return attrs.get(type);
	}

}
