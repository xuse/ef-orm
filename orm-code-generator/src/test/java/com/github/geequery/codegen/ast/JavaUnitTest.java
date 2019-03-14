package com.github.geequery.codegen.ast;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.github.geequery.codegen.ast.DefaultJavaElement;
import com.github.geequery.codegen.ast.IClass;
import com.github.geequery.codegen.ast.JavaField;
import com.github.geequery.codegen.ast.JavaMethod;
import com.github.geequery.codegen.ast.JavaUnit;
public class JavaUnitTest {

	@Test
	public void testReorderFields() {
		String unitName="com.github.geequery.codegen.ast.JavaUnit";
		JavaUnit  ju=new JavaUnit(unitName);
		ju.protectMode=false;    //当为protected模式时，新增的方法和字段不能覆盖已有的方法或字段
				
		JavaField element1=new JavaField(String.class,"file1");
		JavaField element2=new JavaField(Integer.class,"file2");
		JavaField element3=new JavaField(Integer.class,"file3");
//		ju.addImport(org.apache.commons.logging.LogFactory.class);
//		JavaField logField = new JavaField(Log.class, "log");
//		logField.setModifiers(Modifier.PRIVATE);
//		logField.setInitValue("LogFactory.getLog(" + ju.getSimpleName() + ".class)");
//		ju.addField(logField);
		
		ju.addField("element1", element1);
		ju.addField("element2", element2);
		ju.addField("element3", element3);
		ju.addField("element3",element3);
		
		
		List<String> list=new ArrayList<String>();

		list.add("element2");
		list.add("element3");		
		ju.reorderFields(list);  // MAP 中 重新排序
		
		for(String fieldName :  ju.getFieldNames()){
			System.out.println("### reorder field:  "+fieldName);
		}		 
		
		Assert.assertEquals(element1, ju.getField("element1"));
		Assert.assertEquals(element2, ju.getFieldAsJavaField("element2"));
		Assert.assertEquals(element3 ,ju.getField("element3"));
//		Assert.assertEquals(3, ju.getFieldNames().length);
		System.out.println(ju.toString());	
	}
	
	@Test
	public  void     testFindMethod      (){
		String unitName="com.github.geequery.codegen.ast.JavaUnit";
		JavaUnit  ju=new JavaUnit(unitName);

		IClass intClass=new IClass.RealClass(Integer.class); 
		IClass strClass=new IClass.RealClass(String.class);		
		//生成方法名
		JavaMethod findMethod=new JavaMethod("findObject");		
        // 生成方法的参数
		findMethod.clearInputArgs();
//		JavaParameter  intParameter=new JavaParameter(findMethod,"intParameter",intClass,1); 
//		JavaParameter  strParameter=new JavaParameter(findMethod,"strParameter",strClass,1);				
		findMethod.addparam(intClass,"intParameter",0);  
		findMethod.addparam(strClass,"strParameter",0);  		
			
		Assert.assertEquals("intParameter", findMethod.getParameter(0).getName());
		Assert.assertEquals("strParameter", findMethod.getParameter(1).getName());
		System.out.println("parammeter(0) name:"+findMethod.getParameter(0).getName());
		System.out.println("parammeter(1) name:"+findMethod.getParameter(1).getName());	
		// 生成方法内的代码
//		findMethod.putAttribute("attribute1", "intParameter.intValue();");  //  
//		findMethod.putAttribute("attribute2", "Integer.parseInt(strParameter);");  //
		findMethod.appendCode("int num1= $attribute1$");
		findMethod.appendCode("int num2= $attribute2$");  
		findMethod.addContent("int num=num1+num2;");
		findMethod.addContent("return   String.valueOf(num);");  
		//生成方法的返回类型
		findMethod.setReturnType("String");			 
       	//	 
		ju.addMethod("findObject(Integer,String)", findMethod);		
		JavaMethod methodReturn =(JavaMethod) ju.getMethod("findObject", "Integer","String");
		Assert.assertNotNull(methodReturn);			
		System.out.println("method name:"+methodReturn.getName());		
		System.out.println(ju.toString());   // 打印整个方法

	}
	@Test
	public  void     testUpdateMethod  (){
		String unitName="com.github.geequery.codegen.ast.JavaUnit";
		JavaUnit  ju=new JavaUnit(unitName);

		JavaMethod updateMethod=new JavaMethod("updateObject");
		updateMethod.addparam(java.sql.Statement.class, "arg1");
		updateMethod.addparam(jef.database.jsqlparser.visitor.Statement.class, "arg2");
//		updateMethod.putAttribute("attribute1", "intParameter.intValue();");  //  
//		updateMethod.putAttribute("attribute2", "Integer.parseInt(strParameter);");  //
//		updateMethod.putAttribute("attribute3", "0.998");
		
		updateMethod.appendCode("var int = $attribute1$");
		updateMethod.appendCode("var String = $attribute2$");
		updateMethod.appendCode("var double= $attribute3$");		
		
		ju.addMethod("updateObject", updateMethod);
		System.out.println(ju.toString());	
		Assert.assertNotNull(updateMethod.getName());
		
	}
	
	@Test
	public  void     testFieldMethod      (){
		String unitName="com.github.geequery.codegen.ast.JavaUnit";
		JavaUnit  ju=new JavaUnit(unitName); 
		//生成set，get 方法
		JavaField strField=new JavaField(String.class,"fieldName");
        ju.addFieldWithGetterAndSetter(strField);
        
		JavaField _strField2=new JavaField(String.class,"_fieldName2");
        ju.addFieldWithGetterAndSetter(_strField2);
        
        JavaMethod getMethod= ju.getGetter("fieldName");
        JavaMethod setMethod= ju.getSetter("fieldName");
        
        Assert.assertNotNull(getMethod);
        Assert.assertNotNull(setMethod);
        System.out.println("### "+getMethod.getName()  );
        System.out.println("### "+ setMethod.getName()  );
        
        JavaMethod  _getMethod= ju.getGetter("_fieldName2");
        JavaMethod _setMethod= ju.getSetter("_fieldName2");
        Assert.assertNotNull(_getMethod);
        Assert.assertNotNull(_setMethod);
        System.out.println("### "+_getMethod.getName()  ); 
        System.out.println("### "+_setMethod.getName()  ); 
//		System.out.println(ju.toString());
	}	
	// 测试注释生成情况
	@Test 
	public void testElement(){      
		JavaUnit  unit=new JavaUnit("com.github.geequery.codegen.ast");
		//comments
		DefaultJavaElement  jeComments=new DefaultJavaElement();
		jeComments.addComments("comments_1","comments_2"); 
		System.out.println(jeComments.toCode(unit)); 
		
		 //annotation
		DefaultJavaElement  jeAnnotation=new DefaultJavaElement();
		jeAnnotation.addAnnotation("annotation_1"	);
		jeAnnotation.setAnnotations("annotation_set1","annotation_set2");
		String annotationStr="annotation_set1"+"\r\n\t"+"annotation_set2"+"\r\n\t\r\n";
		Assert.assertEquals(annotationStr, jeAnnotation.toCode(unit));
		
		//content
		DefaultJavaElement  jeContent=new DefaultJavaElement();		
		jeContent.addContent("content","content");
		jeContent.appendContent(new StringBuilder(), unit, true);
		Assert.assertEquals("content\r\ncontent\r\n",jeContent.toCode(unit));
		
	}
	
	
	@Test
	public void testRemoveAnnotation(){
		DefaultJavaElement  java =new DefaultJavaElement();	
		java.addAnnotation(new String[]{"@Transactional(rollbackFor={Exception.class})", "@Controller", "@NotModified"});
		java.removeAnnotation("@Transactional(rollbackFor={Exception.class})");
		List<String> annos = java.getAnnotations();
		Assert.assertEquals(2, annos.size());
		System.out.println(annos);
	}
	
	
}
