package com.github.geequery.codegen;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;

import jef.accelerator.asm.ASMUtils;
import jef.accelerator.asm.AnnotationVisitor;
import jef.accelerator.asm.ClassReader;
import jef.accelerator.asm.ClassVisitor;
import jef.accelerator.asm.Opcodes;
import jef.database.DataObject;
import jef.database.DbClient;
import jef.database.annotation.EasyEntity;
import jef.tools.ClassScanner;
import jef.tools.InitDataExporter;
import jef.tools.resource.IResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.geequery.orm.annotation.InitializeData;

public class DbExporter {
    private Logger log=LoggerFactory.getLogger(this.getClass());

	/**
	 * 将数据库中的数据全部作为初始化数据导出到src/main/resources下。
	 * 
	 * @throws Exception
	 */
	public void doxport(DbClient client,String packageName, File output) throws Exception {
		ClassScanner cs = new ClassScanner();
		IResource[] entities = cs.scan(packageName);
		InitDataExporter exporter = new InitDataExporter(client, output);
		for (IResource clz : entities) {
		    ClassReader cl=new ClassReader(clz.getInputStream(),true);
		    ClassAnnotationExtracter ae=new ClassAnnotationExtracter();
		    cl.accept(ae, ClassReader.SKIP_CODE);
		    if(ae.hasAnnotation(Entity.class) || ae.hasAnnotation(EasyEntity.class)){
	            Class<?> e = Class.forName(cl.getJavaClassName());
	            if (DataObject.class.isAssignableFrom(e)) {
	                InitializeData data = e.getAnnotation(InitializeData.class);
	                if (data != null)
	                    log.info("Starting export data:{}",e.getName());
	                    exporter.export(e);
	            }    
		    }
		    
		}
	}
	
	static class ClassAnnotationExtracter extends ClassVisitor{
	    private Set<String> annotations=new HashSet<String>();
	    
        public ClassAnnotationExtracter() {
            super(Opcodes.ASM5);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            annotations.add(desc);
            return null;
        }
        
        public boolean hasAnnotation(Class<? extends Annotation> clzName){
            return annotations.contains(ASMUtils.getDesc(clzName));
        }
	}
}
