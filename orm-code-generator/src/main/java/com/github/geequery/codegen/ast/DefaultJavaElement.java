package com.github.geequery.codegen.ast;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.comments.Comment;

import jef.tools.StringUtils;

public class DefaultJavaElement implements JavaElement {
	protected final List<String> annotations = new ArrayList<String>();
	protected final List<String> comments = new ArrayList<String>();
	protected final List<String> content = new ArrayList<String>();
	protected final List<JavaElement> elements = new ArrayList<JavaElement>();

	public List<String> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(String... annotation) {
		this.annotations.clear();
		for (String s : annotation) {
			this.annotations.add(s);
		}
	}

	public void addContent(String... content) {
		for (String c : content) {
			this.content.add(c);
		}
	}

	public void addAnnotation(String... s) {
		for(String a:s) {
			this.annotations.add(a);
		}
	}

	public void removeAnnotation(String anno) {
		this.annotations.remove(anno);
	}

	public void addComments(String... comments) {
		for (String comment : comments) {
			if (comment == null)
				continue;
			this.comments.add(comment);
		}
	}

	public String toCode(JavaUnit unit) {
		StringBuilder sb = new StringBuilder();
		sb.append(generateComments());
		for (String a : annotations) {
			if (a != null && a.length() > 0) {
				sb.append(a).append("\r\n\t");
			}
		}
		appendContent(sb, unit, true);
		if (sb.length() == 0)
			return "";
		String s = StringUtils.lrtrim(sb.toString(), "\r\n".toCharArray(), "\r\n".toCharArray());
		return s.concat("\r\n");
	}

	protected void appendContent(StringBuilder sb, JavaUnit main, boolean wrap) {
		for (String s : content) {
			sb.append(s);
			if (wrap)
				sb.append("\r\n");
		}
		for (JavaElement element : elements) {
			sb.append(element.toCode(main));
		}
	}

	public String toString(Comment c) {
		return "\t" + StringUtils.rtrim(c.toString(), '\r', '\n') + "\r\n";
	}

	public void buildImport(JavaUnit javaUnit) {
	}

	public List<String> getContent() {
		return content;
	}

	public String generateComments() {
		StringBuilder sb = new StringBuilder();
		if (this.comments != null && comments.size() > 0) {
			sb.append("/**\r\n");
			for (String s : comments) {
				sb.append("\t * ");
				sb.append(s).append("\r\n");
			}
			sb.append("\t */\r\n\t");
		}
		return sb.toString();
	}

	public String toString() {
		return StringUtils.join(content, StringUtils.CRLF_STR);
	}

	public int contentSize() {
		return this.content.size();
	}

	public int elementSize() {
		return this.elements.size();
	}
}
