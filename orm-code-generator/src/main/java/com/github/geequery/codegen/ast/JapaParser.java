package com.github.geequery.codegen.ast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jef.tools.Assert;
import jef.tools.StringUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public class JapaParser implements JavaUnitParser{
	public JavaUnit parse(File file,String charset) {
		CompilationUnit unit;
		List<Comment> comments;
		
		try {
			unit = JavaParser.parse(file,charset);
			if(unit.getTypes().size()>1){
				throw new RuntimeException("The Javaunit can only support One MainClass in a java file.");	
			}
		} catch (ParseException e) {
			throw new RuntimeException(e.getMessage());
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
		comments=new ArrayList<Comment>();
		if(unit.getComments()!=null)comments.addAll(unit.getComments());
		
		String pkgName=(unit.getPackage()==null)?"":unit.getPackage().getName().toString();
		ClassOrInterfaceDeclaration type;
		if(unit.getTypes().get(0) instanceof ClassOrInterfaceDeclaration){
			type=(ClassOrInterfaceDeclaration)unit.getTypes().get(0);
		}else{
			throw new RuntimeException("The Javaunit can only support One MainClass in a java file.");
		}
		
		JavaUnit java=new JavaUnit(pkgName,type.getName());
		if(unit.getImports()!=null){
			for(ImportDeclaration im:unit.getImports()){
				java.addImport(im.getName().toString());
			}	
		}
		java.addComments(getCommentFor(comments,type.getBeginLine()));
		
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
		if(type.getAnnotations()!=null){
			for(AnnotationExpr anno:type.getAnnotations()){
				if("NotModified".equals(anno.getName().getName())){
					continue;
				}
				String ann=anno.toString();
				java.addAnnotation(ann);
	        }	
		}
		
		List<BodyDeclaration> members = type.getMembers();
        for (BodyDeclaration member : members) {
            if (member instanceof MethodDeclaration) {
                MethodDeclaration method = (MethodDeclaration) member;
                JavaMethod jm=new JavaMethod(method.getName());
                jm.addComments(getCommentFor(comments,method.getBeginLine()));
                if(method.getAnnotations()!=null){
                	for(AnnotationExpr anno:method.getAnnotations()){
                    	jm.addAnnotation(anno.toString());
                    }
                }
                if(method.getTypeParameters()!=null){
                	for(TypeParameter tp:method.getTypeParameters()){
                    	jm.addTypeParameter(tp.toString());
                    }	
                }
                if(method.getParameters()!=null){
                	 for(Parameter param:method.getParameters()){
                     	jm.addparam(param.getType().toString(), param.getId().getName(),param.getModifiers());
                     	if(param.isVarArgs()){
                     		jm.setVarArg(true);
                     	}
                     }
                }
                jm.setModifier(method.getModifiers());
                jm.setReturnType(method.getType().toString());
                if(method.getThrows()!=null){
                	for(NameExpr t:method.getThrows()){
                    	jm.addThrows(t.getName());
                    }	
                }
                if(method.getBody()!=null && method.getBody().getStmts()!=null){
                	for(Statement st:method.getBody().getStmts()){
                    	jm.addContent(processMethodContent(st.toString()));
                    }	
                }
                jm.setCheckReturn(false);
                java.addMethod(jm);
            }else if(member instanceof FieldDeclaration){
            	FieldDeclaration field=(FieldDeclaration)member;
            	JavaField jf=new JavaField(field.getType().toString(),null);
            	Assert.isTrue(field.getVariables().size()==1);
            	VariableDeclarator v=field.getVariables().get(0);
            	jf.setName(v.getId().toString());
            	if(v.getInit()!=null)jf.setInitValue(v.getInit().toString());
            	jf.addComments(getCommentFor(comments,field.getBeginLine()));
            	jf.setModifiers(field.getModifiers());
            	List<String> annos=new ArrayList<String>();
            	if(field.getAnnotations()!=null){
            		for(AnnotationExpr anno:field.getAnnotations()){
                		annos.add(anno.toString());
                    }	
            	}
            	jf.addAnnotation(annos.toArray(new String[annos.size()]));
            	java.addField(jf);
            }else{
            	System.out.println("unknown data:" + member.getClass().getName());
            	DefaultJavaElement je=new DefaultJavaElement();
            	je.addComments(getCommentFor(comments,member.getBeginLine()));
            	String s=member.toString();
            	String data=StringUtils.rtrim(StringUtils.ltrim(s,' ','{','\t'),' ','}','\t');
            	if(data.length()==0)continue;
            	je.addContent(s);
            	java.addContent(je.toCode(java));
            }
        }
		return java;
	}
	private static String[] getCommentFor(List<Comment> comments, int beginLine) {
		List<String> result=new ArrayList<String>();
		for(Iterator<Comment> iter=comments.iterator();iter.hasNext();){
			Comment c=iter.next();
			int endLine=c.getEndLine();
			if(endLine<beginLine){
				String comment=StringUtils.rtrim(StringUtils.ltrim(c.getContent(),'*',' ','\r','\n','\t'));
				if(StringUtils.isNotEmpty(comment)){
					result.add(comment);
				}
				iter.remove();
			}
		}
		return result.toArray(new String[result.size()]);
	}

	//将japa的格式化修改为eclipse习惯
	private static String[] processMethodContent(String str) {
		String[] args=StringUtils.split(str,'\n');
		for(int i=0;i<args.length;i++){
			String a=args[i];
			a = a.replace("  ", "\t");
			args[i]=a;
		}
		return args;
	}
}
