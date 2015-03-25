package com.github.geequery.codegen.ast;


public class JavaContainer extends DefaultJavaElement implements JavaElement{
	private boolean wrap= true;
	private String begin;
	private String end;
	
	public boolean isWrap() {
		return wrap;
	}

	public void setWrap(boolean wrap) {
		this.wrap = wrap;
	}
	
	public JavaContainer(){
	}

	public JavaContainer(String begin,String end){
		this.begin=begin;
		this.end=end;
	}
	
	public String getBegin() {
		return begin;
	}

	public void setBegin(String begin) {
		this.begin = begin;
	}

	public String getEnd() {
		return end;
	}

	public void setEnd(String end) {
		this.end = end;
	}

	public String toCode(JavaUnit main) {
		StringBuilder sb=new StringBuilder();
		sb.append(generateComments());
		if (this.getAnnotation()!=null) {
			for (String a : annotation) {
				if (a != null && a.length() > 0){
					sb.append(a).append("\r\n\t");
				}
			}
		}
		sb.append(begin);
		if(wrap)sb.append("\r\n");
		super.appendContent(sb, main,wrap);
		sb.append(end);
		if(wrap)sb.append("\r\n");
		return sb.toString();
	}
}
