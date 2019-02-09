package jef.accelerator.asm.commons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jef.accelerator.asm.AnnotationVisitor;
import jef.tools.Assert;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class AnnotationDef extends AnnotationVisitor {
    private boolean end = false;

    public AnnotationDef(int api, String desc) {
        super(api);
        this.desc = desc;
    }

    protected boolean visible;
    protected String desc;
    protected List<String[]> enums = new ArrayList<String[]>();
    protected Multimap<String, Object> attrs = ArrayListMultimap.create();
    protected Multimap<String, AnnotationDef> annotations = ArrayListMultimap.create();
    protected Multimap<String, AnnotationDef> arrays = ArrayListMultimap.create();

    @Override
    public void visit(String name, Object value) {
        attrs.put(name, value);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        this.enums.add(new String[] { name, desc, value });
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
        AnnotationDef ann = new AnnotationDef(api, desc);
        annotations.put(name, ann);
        return ann;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        AnnotationDef ann = new AnnotationDef(api, name);
        arrays.put(name, ann);
        return ann;
    }

    @Override
    public void visitEnd() {
        end = true;
    }

    public void inject(AnnotationVisitor to) {
        Assert.isTrue(end);
        for (Entry<String, Object> e : attrs.entries()) {
            to.visit(e.getKey(), e.getValue());
        }
        for (String[] enu : enums) {
            to.visitEnum(enu[0], enu[1], enu[2]);
        }
        for (Entry<String, AnnotationDef> e : annotations.entries()) {
            AnnotationVisitor too = to.visitAnnotation(e.getKey(), e.getValue().desc);
            e.getValue().inject(too);
        }
        for (Map.Entry<String, AnnotationDef> e : arrays.entries()) {
            AnnotationVisitor too = to.visitArray(e.getKey());
            e.getValue().inject(too);
        }
        to.visitEnd();
    }

    public boolean isEnd() {
        return end;
    }
}
