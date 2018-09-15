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
package com.github.geequery.codegen.ast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.github.geequery.codegen.ast.IClass.GenericClass;
import com.github.geequery.codegen.ast.IClass.RealClass;
import com.github.geequery.codegen.support.ParseMode;
import com.github.geequery.codegen.util.GenUtil;

import jef.codegen.support.NotModified;
import jef.codegen.support.OverWrittenMode;
import jef.common.log.LogUtil;
import jef.tools.ArrayUtils;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanUtils;

/**
 * 用于生成和解析Java代码的辅助类
 * 
 * @author Administrator
 * 
 */
public class JavaUnit extends DefaultJavaElement{
	private static final String COMMA = ";";
	private boolean isInterface = false;
	private int modifiers = Modifier.PUBLIC;
	private String packageName;
	private String unitName;

	private Map<String,String> imports = new LinkedHashMap<String,String>();
	private Map<String,String> importsStatic = new LinkedHashMap<String,String>();

	private JavaElement rawLinesBeforeTypeDef;
	private List<JavaElement> rawBlocks = new ArrayList<JavaElement>();
	private Map<String, JavaElement> fields = new LinkedHashMap<String, JavaElement>();
	private Map<String, JavaElement> methods = new LinkedHashMap<String, JavaElement>(10, 0.8f, false);
	private String extendsClass;
	private String[] implementsInterface;
	private String[] typeParameters;
	boolean addNotModifiedTag = true;
	boolean protectMode = false;// 当为protected模式时，新增的方法和字段不能覆盖已有的方法或字段
	boolean trimUnderlineInMethod=true;//方法开头如果有下划线，那么要删除掉
	boolean trimUnderlineInField=false;//字段名开头如果有下划线，那么要删除掉
	
	public boolean isTrimUnderlineInMethod() {
		return trimUnderlineInMethod;
	}
	
	/**
	 * 在类中创建Equals方法
	 * @param idfields 要用于鉴别的fields
	 * @param overwirte 是否类中已有的方法
	 * @param doSuperMethod 是否沿用父类的判断逻辑
	 * @return
	 */
	public boolean createEqualsMethod(List<JavaField> idfields,boolean overwirte,String doSuperMethod){
		JavaMethod equals = new JavaMethod("equals");
		equals.setReturnType(boolean.class);
		equals.addparam(IClassUtil.of(Object.class), "rhs0", Modifier.FINAL);
		if(methods.containsKey(equals.getKey())){//方法已经存在
			if(!overwirte){
				return false;
			}
		}
		equals.addContent("if (rhs0 == null)return false;");
		String simpleName=getSimpleName();
		equals.addContent(simpleName+" rhs=("+simpleName+")rhs0;");
		//开始添加方法字段
		for (int i = 0; i < idfields.size(); i++) {
			JavaField field = idfields.get(i);
			String name = field.getName();
			if (Modifier.isAbstract(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			equals.addContent("if(!ObjectUtils.equals(" + name + ", rhs." + name + ")) return false;");
		}
		if(StringUtils.isEmpty(doSuperMethod)){
			equals.addContent("return true;");	
		}else{
			equals.addContent("return super."+doSuperMethod+"(rhs);");
		}
		addMethod(equals);
		addImport(ObjectUtils.class);
		return true;
	}
	
	
	/**
	 * 在类中创建Equals方法
	 * @param idfields 要用于鉴别的fields
	 * @param overwirte 是否类中已有的方法
	 * @param doSuperMethod 是否沿用父类的判断逻辑
	 * @return
	 */
	public boolean createHashCodeMethod(List<JavaField> idfields,boolean overwirte,String doSuperMethod){
		JavaMethod hashCode = new JavaMethod("hashCode");
		hashCode.setCheckReturn(false);
		hashCode.setReturnType(int.class);
		if(methods.containsKey(hashCode.getKey())){//方法已经存在
			if(!overwirte){
				return false;
			}
		}
		hashCode.addContent("return new HashCodeBuilder()");
		//开始添加方法字段
		for (int i = 0; i < idfields.size(); i++) {
			JavaField field = idfields.get(i);
			String name = field.getName();
			if (Modifier.isAbstract(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			hashCode.addContent(".append("+name+")");
		}
		if(StringUtils.isNotEmpty(doSuperMethod)){
			hashCode.addContent(".append(super."+doSuperMethod+"())");
		}
		hashCode.addContent(".toHashCode();");
		addMethod(hashCode);
		addImport(HashCodeBuilder.class);
		return true;
	}
	
	/**
	 * 重新排序Field
	 * @param names
	 */
	public void reorderFields(List<String> names){
		Map<String, JavaElement> result = new LinkedHashMap<String, JavaElement>();
		for(String s:names){
			JavaElement element=this.fields.remove(s);
			if(element==null)continue;
			result.put(s, element); //将顺序里指定的fields重新添加到新的map中。
		}
		result.putAll(fields);
		this.fields=result;
	}
	
	/**
	 * 当为true时，如果field的第一个字符是下划线，生成的getter和setter会截去这个下划线。
	 * 默认为true
	 * @param trimUnderlineInMethod
	 */
	public void setTrimUnderlineInMethod(boolean trimUnderlineInMethod) {
		this.trimUnderlineInMethod = trimUnderlineInMethod;
	}

	public boolean isTrimUnderlineInField() {
		return trimUnderlineInField;
	}

	/**
	 * 当为true时，如果field的第一个字符是下划线，field名称会截去这个下划线。
	 * 默认为true
	 * @param trimUnderlineInMethod
	 */
	public void setTrimUnderlineInField(boolean trimUnderlineInField) {
		this.trimUnderlineInField = trimUnderlineInField;
	}

	public JavaElement getRawLinesBeforeTypeDef() {
		return rawLinesBeforeTypeDef;
	}

	public void setRawLinesBeforeTypeDef(JavaElement rawLinesBeforeTypeDef) {
		this.rawLinesBeforeTypeDef = rawLinesBeforeTypeDef;
	}

	public List<JavaElement> getRawBlocks() {
		return rawBlocks;
	}

	public void setRawBlocks(List<JavaElement> rawBlocks) {
		this.rawBlocks = rawBlocks;
	}

	public boolean isInterface() {
		return isInterface;
	}

	public String[] getTypeParameters() {
		return typeParameters;
	}

	public void setTypeParameters(String[] typeParameters) {
		this.typeParameters = typeParameters;
	}

	public void addTypeParameter(String typeParameter) {
		this.typeParameters = ArrayUtils.addElement(typeParameters, typeParameter);
	}

	public JavaElement getField(String name) {
		return fields.get(name);
	}
	
	public JavaField getFieldAsJavaField(String name) {
		JavaElement element= fields.get(name);
		if(element==null)return null;
		if(element instanceof JavaField){
			return (JavaField)element;
		}
		return null;
	}
	
	public void addAnnotation(JavaAnnotation javaAnnotation) {
		super.addAnnotation(javaAnnotation.toCode(this));
	}

	/**
	 * 根据字段名获得一个方法
	 * 
	 * @param name
	 * @param typeArgs
	 * @return
	 */
	public JavaElement getMethod(String name, String... typeArgs) {
		IClass[] inputArgs = new IClass[typeArgs.length];
		for (int i = 0; i < typeArgs.length; i++) {
			inputArgs[i] = IClassUtil.parse(typeArgs[i]);
		}
		String key = JavaMethod.toMethodKey(name, inputArgs);
		return methods.get(key);
	}

	/**
	 * 根据名称和类型获得一个方法
	 * 
	 * @param name
	 * @param typeArgs
	 * @return
	 */
	public JavaElement getMethod(String name, Class<?>... typeArgs) {
		IClass[] inputArgs = new IClass[typeArgs.length];
		for (int i = 0; i < typeArgs.length; i++) {
			inputArgs[i] = new RealClass(typeArgs[i]);
		}
		String key = JavaMethod.toMethodKey(name, inputArgs);
		return methods.get(key);
	}
	
	public JavaConstructor getOrCreateConstructor(String... typeArgs){
		IClass[] inputArgs = new IClass[typeArgs.length];
		for (int i = 0; i < typeArgs.length; i++) {
			inputArgs[i] = IClassUtil.parse(typeArgs[i]);
		}
		String key = JavaConstructor.toConstratorKey(Arrays.asList(inputArgs));
		JavaConstructor result=(JavaConstructor)methods.get(key);
		if(result==null){
			result=new JavaConstructor();
			for(int i=0;i<typeArgs.length;i++){
				result.addparam(typeArgs[i], "arg"+i);	
			}
			addMethod(result.getKey(),result);
		}
		return result;
	}
	
	public JavaConstructor getConstructor(Class<?>... typeArgs){
		IClass[] inputArgs = new IClass[typeArgs.length];
		for (int i = 0; i < typeArgs.length; i++) {
			inputArgs[i] = new RealClass(typeArgs[i]);
		}
		String key = JavaConstructor.toConstratorKey(Arrays.asList(inputArgs));
		return (JavaConstructor)methods.get(key);
	}
	
	public JavaConstructor getConstructor(String... typeArgs){
		IClass[] inputArgs = new IClass[typeArgs.length];
		for (int i = 0; i < typeArgs.length; i++) {
			inputArgs[i] = IClassUtil.parse(typeArgs[i]);
		}
		String key = JavaConstructor.toConstratorKey(Arrays.asList(inputArgs));
		return (JavaConstructor)methods.get(key);
	}

	public void setInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	public JavaUnit(String fullName) {
		this.packageName = StringUtils.substringBeforeLast(fullName, ".");
		this.unitName = StringUtils.substringAfterLast(fullName, ".");
	}

	public JavaUnit(String packageName, String unitName) {
		this.packageName = packageName;
		this.unitName = unitName;
	}

	public boolean isAbstract() {
		return Modifier.isAbstract(modifiers);
	}

	public void setAbstract(boolean isAbstract) {
		if (isAbstract != isAbstract()) {
			this.modifiers ^= Modifier.ABSTRACT;
		}
	}

	public boolean addImportStatic(String importClass) {
		String simpleName=StringUtils.substringAfterLastIfExist(importClass, ".");
		if (!importsStatic.containsKey(simpleName)) {
			importsStatic.put(simpleName, importClass);
			return true;
		}else{
			String old=importsStatic.get(simpleName);
			return StringUtils.equals(importClass, old); 
		}
	}

	/**
	 * 添加Import项
	 * 
	 * @param importClass
	 */
	public boolean addImport(String importClass) {
		if(StringUtils.isBlank(importClass)){
			this.imports.put(StringUtils.randomString(), importClass);//添加一个空行
			return true;
		}else if(importClass.endsWith(".*")){
			if (!imports.containsValue(importClass)) {
				imports.put(StringUtils.randomString(),importClass);
			}
			return true;
		}else{
			String simpleName=StringUtils.substringAfterLastIfExist(importClass, ".");
			if (!imports.containsKey(simpleName)) {
				imports.put(simpleName,importClass);
				return true;
			}else{
				String old=imports.get(simpleName);
				return StringUtils.equals(importClass, old);
			}
		}
	}
	
	public void addImport(IClass clz) {
		if(clz instanceof GenericClass){
			addImport(((GenericClass) clz).getRawClass());
			for(IClass param: ((GenericClass) clz).getParamTypes()){
				addImport(param);
			}
		}else{
			if(clz.getName().indexOf('.')>-1){
				addImport(clz.getName());	
			}
		}
	}

	public void addImport(Class<?> importClass) {
		IClass clz = new RealClass(importClass);
		addImport(clz.getName());
	}

	/**
	 * 添加Field
	 * 
	 * @param modifier
	 * @param type
	 * @param field
	 */
	public JavaField addField(int modifier, Class<?> type, String name, String... annotation) {
		JavaField field = new JavaField(type, name);
		field.setModifiers(modifier);
		field.addAllAnnotation(Arrays.asList(annotation));
		return addField(field);
	}

	public JavaField addField(int modifier, String type, String name, String... annotation) {
		JavaField field = new JavaField(type, name);
		field.setModifiers(modifier);
		field.addAllAnnotation(Arrays.asList(annotation));
		return addField(field);
	}

	/**
	 * 如果能够成功添加，返回field
	 * @param field
	 * @return
	 */
	public JavaField addField(JavaField field) {
		if(this.trimUnderlineInField && field.getName().charAt(0)=='_'){
				field.setName(field.getName().substring(1));
		}
		return (JavaField)addField(field.getName(), field);
	}

	/**
	 * 如果能够成功添加，则返回field
	 * @param fieldName
	 * @param element
	 * @return
	 */
	public JavaElement addField(String fieldName, JavaElement element) {
		if (element == null)
			return null;
		if (fields.containsKey(fieldName)) {
			if (this.protectMode)
				return null;
			else {
				System.err.println("Exist field was overwritten:" + fieldName);
			}
		}
		this.fields.put(fieldName, element);
		return element;
	}

	public void addSetter(String fieldName){
		JavaField field=this.getFieldAsJavaField(fieldName);
		if (field==null)
			throw new IllegalArgumentException("field not exit while generate getter:"+fieldName);
		String methodName="set"+StringUtils.capitalize(fieldName);
		JavaMethod method=new JavaMethod(methodName);
		method.setModifier(Modifier.PUBLIC);
		method.addparam(field.getType().getSimpleName(), "arg");
		method.addContent("this."+field.getName()+"=arg;");
		this.addMethod(method);
	}
	
	public void addGetter(String fieldName){
		JavaField field=this.getFieldAsJavaField(fieldName);
		if (field==null)
			throw new IllegalArgumentException("field not exit while generate getter:"+fieldName);
		String methodName="get"+StringUtils.capitalize(fieldName);
		
		JavaMethod method=new JavaMethod(methodName);
		method.setModifier(Modifier.PUBLIC);
		method.setReturnType(field.getType());
		method.addContent("return this."+fieldName+";");
		this.addMethod(method);
	}
	/**
	 * 根据field名称得到Method
	 * 只返回JavaMethod，如果该方法内容为javaElement，则返回null
	 * @param fieldname
	 * @return
	 */
	public JavaMethod getGetter(String fieldname){
		JavaElement field=this.getField(fieldname);
		if(!(field instanceof JavaField)){
			return null;
		}
		JavaField f=(JavaField)field;
		String name=f.getName();
		if(trimUnderlineInMethod && name.charAt(0)=='_'){
			name=name.substring(1);
		}
		boolean isBoolean = f.getType().getSimpleName().equalsIgnoreCase("boolean") && !name.startsWith("is");
		String getName=(isBoolean ? "is" : "get") + StringUtils.capitalize(name);
		
		JavaElement method=this.methods.get(JavaMethod.toMethodKey(getName));
		if(method==null && isBoolean)method=this.methods.get(JavaMethod.toMethodKey("get"+StringUtils.capitalize(name)));
		if(method instanceof JavaMethod){
			return (JavaMethod)method;
		}
		return null;
	}
	
	/**
	 * 根据field名称得到Method
	 * 只返回JavaMethod，如果该方法内容为javaElement，则返回null
	 * @param fieldname
	 * @return
	 */
	public JavaMethod getSetter(String fieldname){
		JavaElement field=getField(fieldname);
		if(!(field instanceof JavaField)){
			return null;
		}
		JavaField f=(JavaField)field;
		String name=f.getName();
		if(trimUnderlineInMethod && name.charAt(0)=='_'){
			name=name.substring(1);
		}
		String setName="set" + StringUtils.capitalize(name);
		JavaElement method=this.methods.get(JavaMethod.toMethodKey(setName, f.getType()));
		if(method instanceof JavaMethod){
			return (JavaMethod)method;
		}
		return null;
	}
	
	public JavaField addFieldWithGetterAndSetter(JavaField field) {
		JavaField result=addField(field);
		if(result==null)return null;
		String name=field.getName();
		if(trimUnderlineInMethod && name.charAt(0)=='_'){
			name=name.substring(1);
		}
		JavaMethod setMethod = new JavaMethod("set" +BeanUtils.capitalizeFieldName(name));
		setMethod.setModifier(Modifier.PUBLIC);
		setMethod.addparam(field.getType(), "obj", 0);
		setMethod.addContent("this." +field.getName() + " = obj;");
		addMethod(setMethod);

		boolean isBoolean = field.getType().getSimpleName().equalsIgnoreCase("boolean") && !name.startsWith("is");
		JavaMethod getMethod = new JavaMethod((isBoolean ? "is" : "get") + BeanUtils.capitalizeFieldName(name));
		getMethod.setModifier(Modifier.PUBLIC);
		getMethod.setReturnType(field.getType());
		getMethod.addContent("return " + field.getName() + ";");
		addMethod(getMethod);
		return result;
	}

	public JavaField addFieldWithGetterAndSetter(int modifier, String javaType, String name, String... annotation) {
		JavaField field = new JavaField(javaType, name);
		field.addAllAnnotation(Arrays.asList(annotation));
		field.setModifiers(modifier);
		return addFieldWithGetterAndSetter(field);
	}

	public void addFieldWithGetterAndSetter(int modifier, Class<?> javaType, String name, String... annotation) {
		JavaField field = new JavaField(javaType, name);
		field.addAllAnnotation(Arrays.asList(annotation));
		field.setModifiers(modifier);
		addFieldWithGetterAndSetter(field);
	}

	private String checkAddImport(String raw) {
		String name;
		String[] generaicType = new String[0];
		if (raw.indexOf("<") > -1) {
			name = StringUtils.substringBefore(raw, "<");
			generaicType = StringUtils.split(StringUtils.substringBetween(raw, "<", ">"), ",");
		} else {
			name = raw;
		}
		if(doCheckUnit(name)){
			name=StringUtils.substringAfterLastIfExist(name, ".");
		}
		for (int i=0;i<generaicType.length;i++) {
			generaicType[i]=checkAddImport(generaicType[i]);
		}
		if(generaicType.length==0){
			return name;
		}else{
			StringBuilder sb=new StringBuilder(name);
			sb.append('<').append(generaicType[0]);
			for(int i=1;i<generaicType.length;i++){
				sb.append(',');
				sb.append(generaicType[i]);
			}
			sb.append('>');
			return sb.toString();
		}
	}

	private boolean doCheckUnit(String name) {
		if (name.indexOf(".") < 0)
			return true;
		String pkg = StringUtils.substringBeforeLast(name, ".");
		if (!name.startsWith("java.lang.") && !this.packageName.equals(pkg)) {
			if (!imports.containsValue(name) && !imports.containsValue(toPackageImport(name))) {
				return addImport(name);
			}
		}
		return true;
	}

	private String toPackageImport(String name) {
		if (name.indexOf('.') == -1)
			return null;
		String pkg = StringUtils.substringBeforeLast(name, ".");
		return pkg.concat(".*");
	}

	/**
	 * 添加一个Java方法
	 * @param method
	 */
	public void addMethod(JavaMethod method) {
		if (method.isAbstract())
			this.setAbstract(true);
		addMethod(method.getKey(), method);
	}

	/**
	 * 添加一个Java方法
	 * @param keyword  关键字，由方法名和参数名构成
	 * @param method
	 */
	public void addMethod(String keyword, JavaElement method) {
		if (method == null)
			return;
		if (methods.containsKey(keyword)) {
			if (this.protectMode) {
				return;
			} else {
				System.err.println("Exist method was overwritten:" + keyword);
			}

		}
		methods.put(keyword, method);
	}

	public String getJavaClassName(IClass cls) {
		if (cls instanceof GenericClass) {
			String pType = getJavaClassName(((GenericClass) cls).getRawClass());
			List<String> paramTypes = new ArrayList<String>();
			for (IClass clz : ((GenericClass) cls).getParamTypes()) {
				paramTypes.add(getJavaClassName(clz));
			}
			if (paramTypes.isEmpty())
				return pType;
			return pType + "<" + StringUtils.join(paramTypes, ",") + ">";
		} else if (cls.isArray()) {
			return getJavaClassName(cls.getComponentType()) + "[]";
		} else {
			return getJavaClassName(cls.getName());
		}
	}

	// 检查给定的类名是否已经导入，如果已经导入则直接用简称
	public String getJavaClassName(String raw) {
		String name;
		String[] generaicType = new String[0];
		if (raw.indexOf("<") > -1) {
			name = StringUtils.substringBefore(raw, "<");
			generaicType = StringUtils.split(StringUtils.substringBetween(raw, "<", ">"), ",");
		} else {
			name = raw;
		}
		name = toJavaClassName(name);
		for (int i = 0; i < generaicType.length; i++) {
			generaicType[i] = toJavaClassName(generaicType[i]);
		}
		if (generaicType.length == 0)
			return name;
		return name + "<" + StringUtils.join(generaicType, ',') + ">";
	}

	// 处理类型名，不支持泛型
	private String toJavaClassName(String name) {
		if (name.indexOf('.') < 0)
			return name;
		return checkAddImport(name);
	}

	static void appendModifier(StringBuilder sb, int modifier,boolean isInterface) {
		if (java.lang.reflect.Modifier.isPrivate(modifier)) {
			sb.append("private ");
		}
		if (java.lang.reflect.Modifier.isProtected(modifier)) {
			sb.append("protected ");
		}
		if (java.lang.reflect.Modifier.isPublic(modifier)) {
			sb.append("public ");
		}
		if (java.lang.reflect.Modifier.isFinal(modifier)) {
			sb.append("final ");
		}
		if (!isInterface && java.lang.reflect.Modifier.isAbstract(modifier)) {
			sb.append("abstract ");
		}
		if (java.lang.reflect.Modifier.isNative(modifier)) {
			sb.append("native ");
		}
		if (java.lang.reflect.Modifier.isStatic(modifier)) {
			sb.append("static ");
		}
		if (java.lang.reflect.Modifier.isSynchronized(modifier)) {
			sb.append("synchronized ");
		}
		if (java.lang.reflect.Modifier.isTransient(modifier)) {
			sb.append("transient ");
		}
		if (java.lang.reflect.Modifier.isVolatile(modifier)) {
			sb.append("volatile ");
		}
	}

	public void writeTo(BufferedWriter bw) throws IOException {
		prepareImports();
		if (StringUtils.isNotEmpty(packageName)) {
			bw.write("package ");
			bw.write(packageName);
			bw.write(COMMA);
			bw.newLine();
			bw.newLine();
		}
		if (addNotModifiedTag) {
			this.addImport(NotModified.class);
		}
		for (String cls : this.imports.values()) {
			if(StringUtils.isNotBlank(cls)){
				bw.write("import ");
				bw.write(cls);
				bw.write(COMMA);
			}
			bw.newLine();
		}
		if (!this.importsStatic.isEmpty()) {
			for (String cls : this.importsStatic.values()) {
				bw.write("import static ");
				bw.write(cls);
				bw.write(COMMA);
				bw.newLine();
			}
			bw.newLine();
		}
		
		if(rawLinesBeforeTypeDef!=null){
			bw.write(rawLinesBeforeTypeDef.toCode(this));
		}
		
		if (comments != null && comments.size() > 0) {
			bw.write("/**\r\n");
			for (String s : comments) {
				bw.write(" * ");
				bw.write(s);
				bw.newLine();
			}
			bw.write(" */\r\n");
		}

		if (addNotModifiedTag) {
			bw.write("@NotModified\r\n");
		}
		if (annotations != null) {
			for (String s : annotations) {
				bw.write(s);
				bw.write("\r\n");
			}
		}
		if (this.isInterface) {
			bw.write("public interface " + this.unitName);
		} else {
			StringBuilder sb=new StringBuilder(64);
			appendModifier(sb, this.modifiers,false);
			sb.append("class " + this.unitName);
			bw.write(sb.toString());
		}
		if (typeParameters != null && typeParameters.length > 0) {
			bw.write("<");
			for (int n = 0; n < typeParameters.length; n++) {
				if (n > 0)
					bw.write(',');
				bw.write(typeParameters[n]);
			}
			bw.write(">");
		}
		if (this.extendsClass != null) {
			bw.write(" extends ");
			bw.write(this.getJavaClassName(extendsClass));
		}
		if (this.implementsInterface != null && implementsInterface.length > 0) {
			for (int i = 0; i < implementsInterface.length; i++) {
				implementsInterface[i] = this.getJavaClassName(implementsInterface[i]);
			}
			bw.write(" implements ");
			bw.write(StringUtils.join(implementsInterface, ","));
		}
		bw.write("{");
		bw.newLine();
		bw.newLine();
		// 写入Fields
		if (fields.size() > 0) {
			for (JavaElement str : fields.values()) {
				if((str instanceof JavaField)&&((JavaField) str).isStatic()){
					String code = str.toCode(this);
					bw.write("\t");
					bw.write(code);
				}
			}
			bw.newLine();
			for (JavaElement str : fields.values()) {
				if((str instanceof JavaField)&&((JavaField) str).isStatic()){
				}else{
					String code = str.toCode(this);
					bw.write("\t");
					bw.write(code);
					bw.newLine();
				}
			}
		}
		// 写入方法
		if (!methods.isEmpty()) {
			for (JavaElement method : methods.values()) {
				bw.write("\t");
				bw.write(method.toCode(this));
				bw.newLine();
			}
		}

		if(!content.isEmpty()){
			bw.newLine();
			// 写入正文
			for (String str : content) {
				// bw.write("\t");
				bw.write(str);
				bw.newLine();
			}
		}
		if(rawBlocks!=null){
			bw.newLine();
			for(JavaElement block:rawBlocks){
				bw.write(block.toCode(this));
				bw.newLine();
			}
		}
		bw.write("}");
		bw.flush();
	}
	
	public String toString(){
		StringWriter sw=new StringWriter();
		BufferedWriter bw=new BufferedWriter(sw);
		try {
			this.writeTo(bw);
		} catch (IOException e) {
			LogUtil.exception(e);
		}
		return sw.toString();
	}
	
	
	//计算导入
	private void prepareImports() {
		calcImpInterfaces();
		if (methods != null) {
			for (JavaElement me : methods.values()) {
				me.buildImport(this);
			}
		}
		if (fields != null) {
			for (JavaElement me : fields.values()) {
				me.buildImport(this);
			}
		}
		if(rawBlocks!=null){
			for (JavaElement me : rawBlocks) {
				me.buildImport(this);
			}
		}
	}

	private void calcImpInterfaces() {
		if (implementsInterface == null || implementsInterface.length == 0)
			return;
		String[] realImport = new String[implementsInterface.length];
		for (int i = 0; i < implementsInterface.length; i++) {
			realImport[i] = getJavaClassName(implementsInterface[i]);
		}
		implementsInterface = realImport;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getUnitName() {
		return unitName;
	}

	public void setUnitName(String unitName) {
		this.unitName = unitName;
	}

	public String getFilePath() {
		return packageName.replace('.', '/') + "/" + unitName + ".java";
	}

	public String getSimpleName() {
		return unitName;
	}

	public String getClassName() {
		return packageName + "." + unitName;
	}

	public void setExtends(Class<?> cls) {
		if(cls.isInterface()){
			throw new IllegalArgumentException("the class "+ cls.getName()+" is a interface");
		}
		this.extendsClass = getJavaClassName(cls.getName());
	}

	public void setExtends(String name) {
		this.extendsClass = getJavaClassName(name);
	}
	
	public void setExtends(IClass name) {
		this.extendsClass = getJavaClassName(name);
	}
	
	public String getExtends() {
		return this.extendsClass;
	}

	public void setImplementsInterface(String... implementsInterface) {
		this.implementsInterface = implementsInterface;
	}
	
	public String[] getImplementsInterface(){
		return this.implementsInterface;
	}
	
	public void addImplementsInterface(String i) {
		if (this.implementsInterface == null) {
			this.implementsInterface = new String[] { i };
		} else {
			this.implementsInterface = (String[]) ArrayUtils.add(implementsInterface, i);
		}
	}
	
	public void addImplementsInterface(Class<?> s) {
		if(!s.isInterface()){
			throw new IllegalArgumentException("class "+ s.getName()+" is not a interface.");
		}
		if (implementsInterface == null) {
			implementsInterface = new String[] { s.getName() };
		} else {
			implementsInterface = (String[]) ArrayUtils.add(implementsInterface, s.getName());
		}
	}

	/**
	 * 另存為文件
	 * 
	 * @param javaFile
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public File saveAs(File javaFile, Charset charset) throws IOException {
		BufferedWriter bw = IOUtils.getWriter(javaFile, charset, false);
		writeTo(bw);
		bw.flush();
		bw.close();
		return javaFile;
	}

	/**
	 * 目标文件在指定的源文件夹中存在？
	 * @param srcFolder
	 * @return
	 */
	public boolean isFileExistInSourceFolder(File srcFolder){
		File javaFile = new File(srcFolder.getAbsolutePath(),getFilePath());
		return javaFile.exists();
	}
	
	/**
	 * 保存到指定的SourceFolder
	 * @param srcFolder
	 * @return
	 * @throws IOException
	 */
	public File saveToSrcFolder(File srcFolder, Charset charset, OverWrittenMode mode) throws IOException {
		File javaFile = new File(srcFolder.getAbsolutePath(),getFilePath());
		if (javaFile.exists()) {
			if (mode == OverWrittenMode.NO)
				return null;
			if (mode == OverWrittenMode.AUTO) {
				if (!protectMode && GenUtil.isModified(javaFile)) {
					// 非保护模式下，文件已经修改过，不能覆盖
					return null;
				}
			}
		}
		BufferedWriter bw = IOUtils.getWriter(javaFile, charset == null ? Charset.defaultCharset() : charset, false);
		writeTo(bw);
		bw.flush();
		bw.close();
		return javaFile;
	}

	public File asSourceFile() {
		File f = new File(StringUtils.randomString());
		if (!f.exists())
			f.mkdirs();
		try {
			return saveToSrcFolder(f, null, OverWrittenMode.AUTO);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public File asSourceFileInFolder(File folder) {
		try {
			return this.saveToSrcFolder(folder, null, OverWrittenMode.AUTO);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}

	public boolean asTempFile() {
		return true;
	}

	/**
	 * 从指定的位置读入
	 * 
	 * @param srcFolder
	 * @return 如果文件没有读入成功返回null
	 */
	public static JavaUnit loadFromSrcFolder(File srcFolder, String className, String charset, ParseMode mode) {
		File javaFile = new File(srcFolder.getAbsolutePath() + "/" + className.replace('.', '/') + ".java");
		if (!javaFile.exists()) {
			return null;
		}
		return load(javaFile, charset, mode);
	}

	public static JavaUnit load(File file,String charset,ParseMode mode){
		if(mode==ParseMode.JAVACC){
			JavaUnit java=new JapaParser().parse(file, charset);
			java.protectMode=true;
			return java;
		}else if(mode==ParseMode.KEEP_COMMENT){
			JavaUnit java=new JefParser().parse(file,charset);
			java.protectMode=true;
			return java;
		}else{
			throw new RuntimeException();
		}
	}

	public String[] getFieldNames() {
		Set<String> set = fields.keySet();
		return set.toArray(new String[set.size()]);
	}

	public JavaElement[] getMethods() {
		return methods.values().toArray(new JavaElement[methods.size()]);
	}

	public boolean isAddNotModifiedTag() {
		return addNotModifiedTag;
	}

	public void setAddNotModifiedTag(boolean addNotModifiedTag) {
		this.addNotModifiedTag = addNotModifiedTag;
	}

	public void addRawBlock(JavaElement element) {
		if(this.rawBlocks==null)rawBlocks=new ArrayList<JavaElement>();
		rawBlocks.add(element);
	}
}
