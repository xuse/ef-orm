package com.github.geequery.codegen.ast;

import java.lang.reflect.Modifier;
import java.util.List;

import com.github.geequery.codegen.ast.IClass.RealClass;

public class JavaField extends DefaultJavaElement implements JavaElement{
	private int modifiers;
	private String name;
	private IClass type;
	private String initValue;
	public boolean isPublic(){
		return Modifier.isPublic(modifiers);
	}
	public boolean isStatic(){
		return Modifier.isStatic(modifiers);
	}
	public boolean isFinal(){
		return Modifier.isFinal(modifiers);
	}
	public String getInitValue() {
		return initValue;
	}
	public void setInitValue(String initValue) {
		this.initValue = initValue;
	}
	public void setInitStringValue(String initValue) {
		this.initValue = "\""+initValue+"\"";
	}
	public void setInitValue(int initValue) {
		this.initValue=String.valueOf(initValue);
	}
	public void setInitValue(long initValue){
		this.initValue=String.valueOf(initValue).concat("L");
	}
	public void setInitValue(float initValue){
		this.initValue=String.valueOf(initValue).concat("F");
	}
	
	public JavaField(Class<?> type, String name) {
		this.name=name;
		this.type=new RealClass(type);
	}
	public JavaField(String type, String name) {
		this.name=name;
		this.type=IClassUtil.parse(type);
	}
	public JavaField(IClass type, String name) {
		this.name=name;
		this.type=type;
	}
	public int getModifiers() {
		return modifiers;
	}
	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public IClass getType() {
		return type;
	}
	public void setType(IClass type) {
		this.type = type;
	}
	public String toCode(JavaUnit main) {
		if(code!=null)return code;
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.generateComments());
		if (this.getAnnotations()!=null) {
			for (String a : annotations) {
				if (a != null && a.length() > 0){
					sb.append(a).append("\r\n\t");
				}
			}
		}
		JavaUnit.appendModifier(sb, modifiers,false);
		sb.append(main.getJavaClassName(type)).append(" ");
		sb.append(name);
		if(initValue!=null){
			sb.append(" = ").append(initValue);
		}
		sb.append(";\r\n");
		return sb.toString();
	}
	
	//	当使用toCode方法生成imports时已经太迟了，所以生成要提前
	private String code=null;
	public void buildImport(JavaUnit javaUnit) {
		super.buildImport(javaUnit);
		this.code=toCode(javaUnit);
	}
	
	public void clear(){
		this.code=null;
	}
	
	public void addAllAnnotation(List<String> annos) {
		this.annotations.addAll(annos);
		
	}
}
