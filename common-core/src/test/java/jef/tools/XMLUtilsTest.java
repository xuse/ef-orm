package jef.tools;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.tools.string.RandomData;

import org.junit.Test;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.alibaba.fastjson.JSON;

public class XMLUtilsTest {
	
	private static String[] keys={
		"|",
			"[",
			"]",
			"/"	
		};
	
	
	@Test
	public void testMyXpath() throws SAXException, IOException{
		Document doc=XMLUtils.loadDocument(ResourceUtils.getResource("DrFeeDetail.bo.xml"));
//		Element e=XMLUtils.first(doc.getDocumentElement(), "fields");
//		//XMLUtils.printNode(e, 99, true);
//		XMLUtils.saveDocument(e, System.out, "UTF-8");
		LogUtil.show(SimpleXPath.getAttributesByXPath(doc, "entity/fields/field|complex-field|reference-field[?]@name"));
		XMLUtils.setXsdSchema(doc,null);
		
		Element e=XMLUtils.first(doc.getDocumentElement(), "fields");
		System.out.println("==============");
		System.out.println(XMLUtils.first(e, "aop:auto-proxy").getAttribute("target"));
//		XMLUtils.output(doc, System.out);
	}
	
	
	@Test
	public void testXMLXpath() throws SAXException, IOException{
		String s="<o><name>jiyi</name><gender>123</gender></o>";
		Document doc=XMLUtils.loadDocumentByString(s);
		XMLUtils.printNode(doc, System.out);
	}
	
	@Test
	public void testCommentParse() throws Exception{
		Document doc=XMLUtils.loadDocument(this.getClass().getResource("/db-beans.xml").openStream(),null,false,false);
		Element ele=doc.getDocumentElement();
		readComments(ele);
		XMLUtils.printNode(doc, System.out);
	}


	private void readComments(Node parent) {
		NodeList nl=parent.getChildNodes();
		for(int i=0;i<nl.getLength();i++){
			Node node=nl.item(i);
			int type=node.getNodeType();
			if(type==Node.ATTRIBUTE_NODE ||type==Node.CDATA_SECTION_NODE ||type==Node.TEXT_NODE){
				continue;
			}
			if(type!=Node.COMMENT_NODE){
				readComments(node);
			}else{
				processComment((Comment)node);
			}
		}
	}


	private void processComment(Comment node) {
		String text=node.getNodeValue();
		int index=text.indexOf("password");
		if(index>-1){
			node.setNodeValue(text.substring(0,index+10)+"密码隐藏>");
		}
	}
	
	
	
	@Test
	public void testNodeMove() throws SAXException, IOException{
		Document doc=XMLUtils.loadDocument(this.getClass().getResource("/db-beans.xml").openStream(),null,false,false);
		Element ele=XMLUtils.first(doc.getDocumentElement(), "bean");
		XMLUtils.printNode(ele, System.out);
		
		
		Document doc2=XMLUtils.loadDocument(this.getClass().getResource("/db-empty.xml").openStream(),null,false,false);
		
		doc2.getDocumentElement().appendChild(doc2.importNode(ele, true));
		XMLUtils.printNode(doc2, System.out);
		
	}
	
	@Test
	public void testMapBeans() throws IOException{
		Fo1 bean=RandomData.newInstance(Fo1.class);
		bean.getMap2().put("tomodsds", RandomData.newInstance(Foo.class));
		Document doc=XMLUtils.newDocument("ROOT");
		//Element ele=JXB.objectToXML(bean, doc.getDocumentElement());
		
		Element ele=XMLUtils.putBean(doc.getDocumentElement(), bean,false);
		XMLUtils.printNode(doc, System.out);
		Fo1 fo1=XMLUtils.loadBean(ele, Fo1.class);
		System.out.println(JSON.toJSONString(fo1));
	}
	
	
	public static class Fo1{
		private String name;
		private Map<String,String> map1;
		private Map<String,Object> map2;
		private List<Date> list1;


		public String getName() {
			return name;
		}


		public void setName(String name) {
			this.name = name;
		}


		public Map<String, String> getMap1() {
			return map1;
		}


		public void setMap1(Map<String, String> map1) {
			this.map1 = map1;
		}


		public Map<String, Object> getMap2() {
			return map2;
		}


		public void setMap2(Map<String, Object> map2) {
			this.map2 = map2;
		}


		public List<Date> getList1() {
			return list1;
		}


		public void setList1(List<Date> list1) {
			this.list1 = list1;
		}
	}
}
