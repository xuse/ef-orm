package com.github.geequery.codegen;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jef.common.log.LogUtil;
import jef.tools.ArrayUtils;
import jef.tools.IOUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;

/**
 * 将一个非JEF的POJO快速转化为JEF的Entity
 * @author Administrator
 * @Date 2011-4-16
 */
public class EntityCastor {
//	public static void main(String[] args) {
//		EntityCastor ec=new EntityCastor();
//		File f=new File("src/test/java/jef/orm/test/onetable/model/TestEntity.java");
//		Assert.fileExist(f);
//		ec.process(f);
//	}
	
	private String charset = "UTF-8";

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public int process(File... files) {
		int n = 0;
		for (File f : files) {
			if(f.isFile()){
				if(f.getName().endsWith(".java")){
					if(processFile(f))n++;
				}
				continue;
			}
			n += processSourceFolder(f);
		}
		return n;
	}
	
	
	public boolean processFile(File java){
		try {
			CompilationUnit unit = JavaParser.parse(java, charset);
			if (processUnit(unit)) {
				System.out.println("Modified " + java.getPath());
				IOUtils.saveAsFile(new File(java.getPath()),Charset.forName(charset),unit.toString());
				return true;
			}
		} catch (ParseException e) {
			LogUtil.error("Analyzing " + java.getPath() + " error!");
			LogUtil.exception(e);
		} catch (IOException e) {
			LogUtil.error("Analyzing " + java.getPath() + " error!");
			LogUtil.exception(e);
		}
		return false;
	}

	private int processSourceFolder(File root) {
		int n = 0;
		for (File java : IOUtils.listFilesRecursive(root, "java")) {
			processFile(java);
		}
		return n;
	}
	
	private static final String[] DB_FIELDS={
		"Integer",
		"Long",
		"Short",
		"Double",
		"Number",
		"Float",
		"Date",
		"Time",
		"Timestamp",
		"File",
		"JFile",
		"String",
		"byte[]"
	};

	private boolean processUnit(CompilationUnit unit) {
		
		TypeDeclaration mainType=unit.getTypes().get(0);
		addImport(unit);
		addEntityAnnotation(mainType);
		((ClassOrInterfaceDeclaration)mainType).setExtends(Arrays.asList(new ClassOrInterfaceType("jef.database.DataObject")));
		EnumDeclaration metaModel= findModel(mainType);
		if(metaModel==null){
			metaModel = new EnumDeclaration();
			metaModel.setName("Field");
			metaModel.setModifiers(Modifier.PUBLIC);
			List<ClassOrInterfaceType> types=new ArrayList<ClassOrInterfaceType>();	
			types.add(new ClassOrInterfaceType("jef.database.Field"));
			metaModel.setImplements(types);			
			mainType.getMembers().add(metaModel);
		}

		List<EnumConstantDeclaration> entries=new ArrayList<EnumConstantDeclaration>();
		for(BodyDeclaration member:mainType.getMembers()){
			if(!(member instanceof FieldDeclaration))
				continue;
			FieldDeclaration field=(FieldDeclaration)member;
			if(field.getType() instanceof PrimitiveType){
			}else{
				String typeStr=field.getType().toString();
				if(!ArrayUtils.contains(DB_FIELDS, typeStr)){
					continue;
				}
			}
			if(Modifier.isStatic(field.getModifiers()))continue;
			entries.add(new EnumConstantDeclaration(field.getVariables().get(0).getId().getName()));
		}
		
		metaModel.setEntries(entries);
		return true;
	}

	private void addImport(CompilationUnit unit) {
		if(unit.getImports()==null){
			unit.setImports(new ArrayList<ImportDeclaration>());
		}		
		for(ImportDeclaration im: unit.getImports()){
			if(im.getName().getName().equals("javax.persistence.Entity")){
				return;
			}
		}
		ImportDeclaration entity=new ImportDeclaration();
		entity.setName(new NameExpr("javax.persistence.Entity"));
		unit.getImports().add(entity);
	}

	private EnumDeclaration findModel(TypeDeclaration mainType) {
		for(BodyDeclaration member:mainType.getMembers()){
			if(member instanceof EnumDeclaration){
				EnumDeclaration mem=(EnumDeclaration)member;
				if(mem.getName().equals("Field")){
					return (EnumDeclaration) member;
				}
			}
		}
		return null;
	}

	private void addEntityAnnotation(TypeDeclaration mainType) {
		AnnotationExpr anno=new NormalAnnotationExpr();
		anno.setName(new NameExpr("Entity"));
		if(mainType.getAnnotations()==null){
			mainType.setAnnotations(new ArrayList<AnnotationExpr>());
		}else{
			for(AnnotationExpr annot:mainType.getAnnotations()){
				if(annot.getName().getName().equals("Entity")){
					return;
				}
			}
		}
		mainType.getAnnotations().add(anno);
	}
}
