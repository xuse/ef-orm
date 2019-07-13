package com.github.geequery.codegen.ast;

import jef.tools.ArrayUtils;
import jef.tools.StringUtils;


/**
 * 描述一个Java方法的参数
 * @author Administrator
 *
 */
public class JavaParameter {
	JavaParameter(JavaMethod parent,String name, IClass type2, int modifiers) {
		this.parent=parent;
		this.name = name;
		this.type = type2;
		this.modifier = modifiers;
	}

	private IClass type;
	private int modifier;
	private String[] annotation;
	private String[] comment;
	private String name;
	private JavaMethod parent;

	public JavaMethod getDeclaringMethod() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public IClass getType() {
		return type;
	}

	public int getModifier() {
		return modifier;
	}

	public void setModifier(int modifier) {
		this.modifier = modifier;
	}

	public String[] getAnnotation() {
		if(annotation==null)return org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
		return annotation;
	}

	public void setAnnotation(String... annotation) {
		this.annotation = annotation;
	}
	
	public JavaParameter addAnnotation(String... annotation) {
		this.annotation = ArrayUtils.addAllElement(this.annotation, annotation);
		return this;
	}

	public String[] getComment() {
		if(comment==null)return org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
		return comment;
	}

	public void setComment(String... comment) {
		this.comment = comment;
	}
	
	public JavaParameter addComment(String... comment) {
		this.comment = ArrayUtils.addAllElement(this.comment, annotation);
		return this;
	}

	public void genetateCode(JavaMethod javaMethod, JavaUnit main, StringBuilder sb, boolean isVarArg) {
		if (annotation != null) {
			sb.append(StringUtils.join(annotation, "\n\t\t"));
			sb.append(' ');
		}
		JavaUnit.appendModifier(sb, modifier,false);
		sb.append(main.getJavaClassName(type));
		if (isVarArg) {
			sb.append("...");
		}
		sb.append(' ');
		sb.append(name);
		if (comment != null) {// 创建注释
			sb.append("/*");
			sb.append(StringUtils.join(comment, '\n'));
			sb.append("*/\n");
		}
	}
}
