package com.github.geequery.codegen.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jef.tools.ArrayUtils;
import jef.tools.StringUtils;

import com.github.javaparser.ast.comments.Comment;


public class DefaultJavaElement implements JavaElement{
	protected String[] annotation;
	protected List<String> comments = new ArrayList<String>();
	protected List<String> content=new ArrayList<String>();
	protected List<JavaElement> elements=new ArrayList<JavaElement>();
	public String[] getAnnotation() {
		return annotation;
	}
	public void setAnnotation(String... annotation) {
		this.annotation = annotation;
	}
	public void addContent(String... content){
		for(String c:content){
			this.content.add(c);
		}
	}
	public void addAnnotation(String... a){
		annotation=ArrayUtils.addAllElement(annotation, a);
	}
	
	public void removeAnnotation(String... annos){
		List<String> annoList = new ArrayList<String>(Arrays.asList(annotation));
		for(String anno : annos){
			if(annoList.contains(anno)){
				annoList.remove(anno);
			}
		}
		String[] newAnnos = new String[annoList.size()];
		annotation = annoList.toArray(newAnnos);
	}
	
	public void addComments(String... comments){
		for(String comment: comments){
			if(comment==null)continue;
			this.comments.add(comment);
		}
	}
	
	public String toCode(JavaUnit unit) {
		StringBuilder sb = new StringBuilder();
		sb.append(generateComments());
		if (this.getAnnotation()!=null) {
			for (String a : annotation) {
				if (a != null && a.length() > 0){
					sb.append(a).append("\r\n\t");
				}
			}
		}
		appendContent(sb,unit,true);
		if(sb.length()==0)return "";
		String s=StringUtils.lrtrim(sb.toString(), "\r\n".toCharArray(),  "\r\n".toCharArray());
		return s.concat("\r\n");
	}
	
	protected void appendContent(StringBuilder sb,JavaUnit main,boolean wrap) {
		for(String s: content){
			sb.append(s);
			if(wrap)sb.append("\r\n");
		}
		for(JavaElement element:elements){
			sb.append(element.toCode(main));
		}
	}
	
	public String toString(Comment c){
		return "\t"+StringUtils.rtrim(c.toString(),'\r','\n')+"\r\n";
	}
	public void buildImport(JavaUnit javaUnit) {
	}
	public List<String> getContent() {
		return content;
	}
	public void setContent(List<String> content) {
		this.content = content;
	}
	public String generateComments() {
		StringBuilder sb=new StringBuilder();
		if(this.comments!=null && comments.size()>0){
			sb.append("/**\r\n");
			for (String s : comments) {
				sb.append("\t * ");
				sb.append(s).append("\r\n");
			}
			sb.append("\t */\r\n\t");
		}
		return sb.toString();
	}
	
	public String toString(){
		return StringUtils.join(content, StringUtils.CRLF_STR);
	}
	

	public int contentSize(){
		return this.content.size();
	}
	
	public int elementSize(){
		return this.elements.size();
	}
}
