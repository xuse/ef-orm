package com.github.geequery.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;

import com.github.geequery.codegen.ast.IClassUtil;
import com.github.geequery.codegen.ast.JavaAnnotation;
import com.github.geequery.codegen.ast.JavaField;
import com.github.geequery.codegen.ast.JavaMethod;
import com.github.geequery.codegen.ast.JavaUnit;

import jef.codegen.support.OverWrittenMode;
import jef.common.log.LogUtil;
import jef.database.jsqlparser.expression.operators.arithmetic.Mod;
import jef.tools.Exceptions;
import jef.tools.io.Charsets;

public class SpringApplicationGenerator {

	private String packageName;

	private File srcFolder = new File("src/main/java");

	private File testFoler = new File("src/test/java");

	public void generateSpringClass(String applicationClz) {
		/**
		 * 生成Application Java类
		 */

		this.saveJavaSource(generateSpringApplicationClass(applicationClz), srcFolder);

		/**
		 * 生成测试Application Java类。
		 */
		this.saveJavaSource(generateSpringTestClass(applicationClz), testFoler);
	}

	private JavaUnit generateSpringTestClass(String applicationClz) {
		JavaUnit java = new JavaUnit(packageName, applicationClz + "Test");
		IClassUtil.addCommonsLog(java);
		JavaAnnotation runWith = new JavaAnnotation("org.junit.runner.RunWith");
		runWith.put("value", IClassUtil.parse("org.springframework.test.context.junit4.SpringRunner"));
		java.addAnnotation(runWith.toCode(java));
		java.addAnnotation(new JavaAnnotation("org.springframework.boot.test.context.SpringBootTest"), java);

		{
			JavaField field = new JavaField("org.springframework.boot.test.rule.OutputCapture", "out");
			field.addAnnotation(new JavaAnnotation("org.junit.ClassRule"), java);
			field.setModifiers(Modifier.STATIC | Modifier.PUBLIC);
			field.setInitValue("new OutputCapture()");
			java.addField(field);
		}
		{
			JavaMethod method = new JavaMethod("test1");
			method.addAnnotation(new JavaAnnotation("org.junit.Test"), java);
			method.addContent("String output = out.toString();");
			method.addContent("Assert.assertThat(output,containsString(\"\"));");
			java.addMethod(method);
			java.addImportStatic("org.hamcrest.CoreMatchers.*");
			java.addImport("org.junit.Assert");
		}
		return java;
	}

	private JavaUnit generateSpringApplicationClass(String applicationClz) {
		JavaUnit java = new JavaUnit(packageName, applicationClz);
		IClassUtil.addCommonsLog(java);
		java.setImplementsInterface("org.springframework.boot.CommandLineRunner");
		java.addAnnotation("@SpringBootApplication");
		java.addImport("org.springframework.boot.autoconfigure.SpringBootApplication");
		JavaAnnotation autowired = new JavaAnnotation("org.springframework.beans.factory.annotation.Autowired");

		{
			JavaField field = new JavaField("org.springframework.context.ApplicationContext", "applicationContext");
			field.addAnnotation(autowired.toCode(java));
			java.addField(field);

			field = new JavaField("javax.persistence.EntityManagerFactory", "entityManagerFactory");
			field.addAnnotation(autowired.toCode(java));
			java.addField(field);
		}
		{
			JavaMethod method = new JavaMethod("run");
			method.addparam(String.class, "args");
			method.setVarArg(true);
			method.addAnnotation(new JavaAnnotation(Override.class).toCode(java));
			method.addContent("log.info(\"Spring-boot application executed.\");");
			java.addMethod(method);
		}
		{
			JavaMethod method = new JavaMethod("main");
			method.setStatic(true);
			method.addparam(String.class, "args");
			method.setVarArg(true);
			java.addImport("org.springframework.boot.SpringApplication");
			method.addContent("SpringApplication.run(" + applicationClz + ".class, args);");
			java.addMethod(method);
		}
		return java;
	}

	public File saveJavaSource(JavaUnit java, File root) {
		try {
			OverWrittenMode mode = OverWrittenMode.AUTO;
			File f = java.saveToSrcFolder(root, Charsets.UTF8, mode);
			if (f != null) {
				LogUtil.show(f.getAbsolutePath() + " generated.");
			} else {
				LogUtil.show(java.getClassName() + " Class file was modified, will not overwrite it.");
			}
			return f;
		} catch (IOException e) {
			throw Exceptions.asIllegalArgument(e);
		}
	}

	public SpringApplicationGenerator setPackageName(String packageName) {
		this.packageName = packageName;
		return this;
	}

	
	public SpringApplicationGenerator setTestFoler(File testFoler) {
		this.testFoler = testFoler;
		return this;
	}

	public SpringApplicationGenerator setSrcFolder(File srcFolder) {
		this.srcFolder = srcFolder;
		return this;
	}
}
