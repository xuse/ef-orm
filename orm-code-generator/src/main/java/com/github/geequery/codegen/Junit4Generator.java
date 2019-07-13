/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.geequery.codegen;

import java.io.File;

import com.github.geequery.codegen.ast.JavaMethod;
import com.github.geequery.codegen.ast.JavaUnit;

import  org.apache.commons.lang3.StringUtils;

/**
 * 用于批量生成Junit4测试框架的代码
 * @Company: Asiainfo-Linkage Technologies （China）,Inc. 
 * @Date 2011-4-5
 */
public class Junit4Generator {
	private File sourceFolder = new File("./");
	private boolean generateEmptyClass=false;
	private String includePattern;
	private String[] excludePatter;
	
	public Junit4Generator(){
	}

	public void generateForPackage(String... pkgNames) {
//		ClassPool cp = ClassPool.getDefault();// 装载器
//		StackTraceElement[] eles=Thread.currentThread().getStackTrace();
//		StackTraceElement last=eles[eles.length-1];
//		File root = IOUtils.urlToFile(last.getClass().getResource("/"));
//		System.out.println("Start search class file in: "+ root.getPath() );
//		String[] clss=ClassScanner.listClassNameInPackage(last.getClass(),pkgNames, true, false,false);
//		int n=0;
//		for(String cls: clss){
//			RegexpNameFilter filter=new RegexpNameFilter(includePattern,excludePatter);
//			if(!filter.accept(cls)){
//				continue;
//			}
//			if(cls.endsWith("Test"))continue;
//			try {
//				CtClass clz = cp.getCtClass(cls);
//				if(!Modifier.isPublic(clz.getModifiers()))continue;
//				JavaUnit unit=initTestUnit(cls);
//				int testedMethod=0;
//				for(CtMethod m:clz.getDeclaredMethods()){
//					if(Modifier.isPublic(m.getModifiers())){
//						addTestMethod(unit,m.getName());
//						testedMethod++;
//					}
//				}
//				if(testedMethod==0 && !generateEmptyClass)continue;
//				File file=unit.saveToSrcFolder(sourceFolder,"UTF-8",OverWrittenMode.AUTO);
//				if(file==null){
//					System.out.println("skiping "+ cls);
//				}else{
//					System.out.println("generating "+cls);
//					n++;
//				}
//			} catch (NotFoundException e) {
//				System.out.println(cls + " not found!");
//				continue;
//			} catch (IOException e) {
//				LogUtil.exception(e);
//			}
//		}
//		System.out.println(n +" Test classes generated.");
	}

	private void addTestMethod(JavaUnit unit, String name) {
		JavaMethod m=new JavaMethod("test"+ StringUtils.capitalize(name));
		m.setAnnotations("@Test");
		m.addContent("fail(\"Not yet implemented\");");
		unit.addMethod(m);
	}

	private JavaUnit initTestUnit(String cls) {
		JavaUnit unit=new JavaUnit(cls+"Test");//生成测试类
		unit.addImportStatic("org.junit.Assert.*");
		unit.addImport("org.junit.Before");
		unit.addImport("org.junit.Test");
		JavaMethod m=new JavaMethod("setUp");
		m.setAnnotations("@Before");
		unit.addMethod(m);
		return unit;
	}

	public File getSourceFolder() {
		return sourceFolder;
	}

	public void setSourceFolder(File sourceFolder) {
		this.sourceFolder = sourceFolder;
	}

	public boolean isGenerateEmptyClass() {
		return generateEmptyClass;
	}

	public void setGenerateEmptyClass(boolean generateEmptyClass) {
		this.generateEmptyClass = generateEmptyClass;
	}

	public String getIncludePattern() {
		return includePattern;
	}

	public void setIncludePattern(String includePattern) {
		this.includePattern = includePattern;
	}

	public String[] getExcludePatter() {
		return excludePatter;
	}

	public void setExcludePatter(String[] excludePatter) {
		this.excludePatter = excludePatter;
	}
}
