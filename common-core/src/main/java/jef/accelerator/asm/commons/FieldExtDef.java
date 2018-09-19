package jef.accelerator.asm.commons;

import java.util.Collection;
import java.util.Map;

import com.github.geequery.asm.AnnotationVisitor;
import com.github.geequery.asm.Attribute;
import com.github.geequery.asm.FieldVisitor;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import jef.accelerator.asm.ASMUtils;
import jef.tools.Assert;


public class FieldExtDef extends FieldVisitor {
	private boolean end = false;
	private Multimap<String, AnnotationDef> annotations = ArrayListMultimap.create();
	private Multimap<String, Attribute> attrs = ArrayListMultimap.create();
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
		for (Map.Entry<String, AnnotationDef> e : annotations.entries()) {
			String desc = e.getKey();
			boolean visible = e.getValue().visible;
			AnnotationVisitor too = to.visitAnnotation(desc, visible);
			e.getValue().inject(too);
		}
		to.visitEnd();
	}

	public Collection<AnnotationDef> getAnnotation(String desc) {
		return annotations.get(desc);
	}

	public Collection<AnnotationDef> getAnnotation(Class<?> clz) {
		return annotations.get(ASMUtils.getDesc(clz));
	}
}
