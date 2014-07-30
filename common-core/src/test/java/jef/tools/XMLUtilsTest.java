package jef.tools;

import java.io.IOException;

import jef.common.log.LogUtil;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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
		LogUtil.show(XMLUtils.getAttributesByXPath(doc, "entity/fields/field|complex-field|reference-field[?]@name"));
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
}
