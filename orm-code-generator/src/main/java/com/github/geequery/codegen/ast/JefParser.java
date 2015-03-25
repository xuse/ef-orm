package com.github.geequery.codegen.ast;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.TypeParameter;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.visitor.DumpVisitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;

public class JefParser implements JavaUnitParser{
	private static final char[] LEFT_TRIM=" \t\n\r".toCharArray();
	private static final char[] RIGHT_TRIM=" \t\n\r;".toCharArray();
	
	private BufferedReader reader;
	private CompilationUnit unit;
	private int lineNum=1;
	
	public JavaUnit parse(File file, String charset){
		
		try{
			this.reader=IOUtils.getReader(file, charset);
			unit= JavaParser.parse(file,charset);
			return doParse();
		}catch(IOException e){
			throw new RuntimeException(e);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}finally{
			IOUtils.closeQuietly(reader);
		}
	}
	
	private JavaUnit doParse()throws IOException{
		String pkgName=(unit.getPackage()==null)?"":unit.getPackage().getName().toString();
		ClassOrInterfaceDeclaration type;
		if(unit.getTypes().get(0) instanceof ClassOrInterfaceDeclaration){
			type=(ClassOrInterfaceDeclaration)unit.getTypes().get(0);
		}else{
			throw new RuntimeException("The Javaunit can only support One MainClass in a java file.");
		}
		JavaUnit java=new JavaUnit(pkgName,type.getName());
		java.setAddNotModifiedTag(false);
		   
		if(type.getTypeParameters()!=null){
			for(TypeParameter tp:type.getTypeParameters()){
				java.addTypeParameter(tp.toString());
			}	
		}
		if(type.getImplements()!=null){
			for(ClassOrInterfaceType t:type.getImplements()){
				java.addImplementsInterface(t.getName());
			}	
		}
		java.setModifiers(type.getModifiers());
		java.setInterface(type.isInterface());
		if(type.getExtends()!=null && type.getExtends().size()>0){
			java.setExtends(type.getExtends().get(0).getName());	
		}
		
		//jianghy3 2012.08.29 Bug #55696 解决思路：先保留原有的，再根据本次配置信息删除/修改。
		List<AnnotationExpr> annos = type.getAnnotations();
		if(!CollectionUtil.isEmpty(annos)){
			java.addAnnotation(parseAnnoExprToString(CollectionUtil.toArray(annos, AnnotationExpr.class)));
		}
		
		
		/*开始逐行解析和处理*/
		
		//包定义前的部分
		processBeforePackage(java,0,unit.getPackage().getBeginLine());
		
		//包定义到类定义前的部分
		int start=unit.getPackage().getEndLine()+1;
		int end=type.getBeginLine();//不含
		processRawBeforeTypeDef(java,start,end);//将所有的前行添加
		List<BodyDeclaration> members = type.getMembers();
		if(members!=null && members.size()>0){
			this.skipLines(members.get(0).getBeginLine());
		}
        for (BodyDeclaration member : members) {
        	//类成员1
        	if (member instanceof MethodDeclaration) {
        		MethodDeclaration m=(MethodDeclaration)member;
        		processMethod(java,m,m.getEndLine()+1);	
        	}else if(member instanceof FieldDeclaration){
        		FieldDeclaration f=( FieldDeclaration)member;
        		processField(java,f,f.getEndLine()+1);
        	}else if(member instanceof ConstructorDeclaration){
        		ConstructorDeclaration c=(ConstructorDeclaration)member;
        		processConstructor(java,c,c.getEndLine()+1);
        	}else{
        		processOtherBodyDec(java,member,member.getEndLine()+1);
        	}
        }
        return java;
	}
	
	private void processConstructor(JavaUnit java, ConstructorDeclaration c, int i) throws IOException {
		DefaultJavaElement element=new DefaultJavaElement();
		boolean isStarted=false;
		for(;lineNum<i;lineNum++){
			String line=reader.readLine();
			if(StringUtils.isBlank(line) && !isStarted){
				continue;
			}
			if(isStarted==false){
				element.addContent(line.trim());
			}else{
				element.addContent(line);
			}
			isStarted=true;
		}
		JavaConstructor jm=new JavaConstructor();
        if(c.getParameters()!=null){
        	 for(Parameter param:c.getParameters()){
             	jm.addparam(param.getType().toString(), param.getId().getName(),param.getModifiers());
             	if(param.isVarArgs()){
             		jm.setVarArg(true);
             	}
             }
        }
        java.addMethod(jm.getKey(), element);
	}

	private void processOtherBodyDec(JavaUnit java, BodyDeclaration member, int i) throws IOException {
		boolean isStarted=false;
		DefaultJavaElement element=new DefaultJavaElement();
		for(;lineNum<i;lineNum++){
			String line=reader.readLine();
			if(StringUtils.isBlank(line) && !isStarted){
				continue;
			}
			isStarted=true;
			element.addContent(line);
		}
		java.addRawBlock(element);
	}

	private void processField(JavaUnit java, FieldDeclaration field, int i) throws IOException {
		DefaultJavaElement element=new DefaultJavaElement();
		boolean isStarted=false;
		for(;lineNum<i;lineNum++){
			String line=reader.readLine();
			if(StringUtils.isBlank(line) && !isStarted){
				continue;
			}
			if(isStarted==false){
				element.addContent(line.trim());
			}else{
				element.addContent(line);
			}
			isStarted=true;
		}
    	VariableDeclarator v=field.getVariables().get(0);
    	String name=v.getId().toString();
		java.addField(name,element);
	}

	private void processMethod(JavaUnit java, MethodDeclaration method, int i) throws IOException {
		DefaultJavaElement element=new DefaultJavaElement();
		boolean isStarted=false;
		for(;lineNum<i;lineNum++){
			String line=reader.readLine();
			if(StringUtils.isBlank(line) && !isStarted){
				continue;
			}
			if(isStarted==false){
				element.addContent(line.trim());
			}else{
				element.addContent(line);
			}
			isStarted=true;
		}
		JavaMethod jm=new JavaMethod(method.getName());
        if(method.getParameters()!=null){
        	 for(Parameter param:method.getParameters()){
             	jm.addparam(param.getType().toString(), param.getId().getName(),param.getModifiers());
             	if(param.isVarArgs()){
             		jm.setVarArg(true);
             	}
             }
        }
        java.addMethod(jm.getKey(), element);
	}

	private void processBeforePackage(JavaUnit java, int start, int end) throws IOException {
		skipLines(end);
//		for(;lineNum<end;lineNum++){
//			reader.readLine();
//			lineNum++;
//		}
	}

	private void processRawBeforeTypeDef(JavaUnit java, int start, int end) throws IOException {
		DefaultJavaElement element=new DefaultJavaElement();
		skipLines(start);
		
		boolean isStarted=false;
		for(;lineNum<end;lineNum++){
			String line=reader.readLine();
			if(StringUtils.isBlank(line) && !isStarted){
				continue;
			}
			isStarted=true;
			if(line.startsWith("import ")){
				String name=StringUtils.substringAfter(line, "import ");
				name=StringUtils.lrtrim(name, LEFT_TRIM, RIGHT_TRIM);
				java.addImport(name);
			}else if(StringUtils.isBlank(line)){
				java.addImport("");
			}else{
				element.addContent(line);	
			}
		}
		java.setRawLinesBeforeTypeDef(element);
	}

	private void skipLines(int start) throws IOException {
		while(lineNum<start){
			@SuppressWarnings("unused")
			String line=reader.readLine();
			lineNum++;
		}
	}
	
	private String[] parseAnnoExprToString(AnnotationExpr[] annos){
		if(annos != null){
			String[] annoStrArr = new String[annos.length];
			for(int i=0; i<annos.length; i++){
				//copy from japa.parser.ast.Node.toString().
				 DumpVisitor visitor = new DumpVisitor();
				 annos[i].accept(visitor, null);
				 annoStrArr[i] = visitor.getSource();
			}
			return annoStrArr;
		}
		return new String[0];
	}

}
