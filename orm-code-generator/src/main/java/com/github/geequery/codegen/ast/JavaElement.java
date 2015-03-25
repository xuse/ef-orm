package com.github.geequery.codegen.ast;


public interface JavaElement {
	public String toCode(JavaUnit main);
	public void buildImport(JavaUnit javaUnit);
}
