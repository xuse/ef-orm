package jef.database.dialect;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import jef.tools.Assert;

public class AnnotationDesc {
	private final Class<? extends Annotation> annotationClz;
	private final Map<String, Object> proprties = new HashMap<String, Object>();

	public AnnotationDesc(Class<? extends Annotation> annotationClz) {
		Assert.notNull(annotationClz);
		this.annotationClz = annotationClz;
	}

	public AnnotationDesc put(String key, Object value) {
		proprties.put(key, value);
		return this;
	}

	public Class<? extends Annotation> getAnnotationClz() {
		return annotationClz;
	}

	public Map<String, Object> getProprties() {
		return proprties;
	}

//	public void addImport(Class<?> clz){
//		this.importClasses.add(clz);
//	}
//
//	public List<Class<?>> getImportClasses() {
//		return importClasses;
//	}

	@Override
	public String toString() {
		return annotationClz.getName()+proprties;
	}
	
	
}
