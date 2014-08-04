/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.management.ReflectionException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import jef.common.log.LogUtil;
import jef.tools.reflect.BeanWrapper;
import jef.tools.reflect.BeanWrapperImpl;
import jef.tools.reflect.Property;
import jef.tools.reflect.UnsafeUtils;
import jef.tools.string.CharsetName;
import jef.tools.string.StringSpliterEx;
import jef.tools.string.Substring;
import jef.tools.string.SubstringIterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.html.dom.HTMLDocumentImpl;
import org.easyframe.fastjson.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * <b>重要，关于xercesImpl</b>
 * 
 * <pre>
 * 本类的高级功能需要在有xerces解析器的情况下才能工作。
 * xerces是 apache的一个第三方解析包。
 * 作者目前测试了xercesImpl从 2.6.x到2.11.x各个版本的兼容性，推荐使用 2.7.1~2.9.1之间的版本。
 *    2.7.1之前的版本不能支持cyberneko的HTML解析。因此不建议使用2.6.2或以前的版本。
 *    2.10.0开始由于其用到了org.w3c.dom.ElementTraversal这个类，在JDK 6下要求再引入包xml-api。
 *    这容易在weblogic等环境下产生兼容性问题，也不推荐使用。
 *    本工程在这里默认引用2.9.1版本
 * </pre>
 * 
 * @author jiyi
 * 
 */
public class XMLUtils {
	/**
	 * Xpath解析器
	 */
	private static XPathFactory xp = XPathFactory.newInstance();

	/**
	 * HTML解析器
	 */
	private static jef.tools.IDOMFragmentParser parser;

	static {
		try {
			Class.forName("org.apache.xerces.xni.XMLDocumentHandler");
			try {
				Class<?> cParser = Class.forName("org.cyberneko.html.parsers.DOMFragmentParser");
				if (cParser != null) {
					parser = (jef.tools.IDOMFragmentParser) cParser.newInstance();
				}
			} catch (Exception e) {
				// 没有将common-net包依赖进来，无法使用HTML解析功能
				LogUtil.warn("The JEF-HTML parser engine not found, HTMLParser feature will be disabled. Import easyframe 'common-misc' library to the classpath to activate this feature.");
			}
		} catch (Exception e) {
			// xerces版本过旧，不支持进行HTML解析
			LogUtil.warn("The Apache xerces implemention not avaliable, HTMLParser feature will be disabled. you must import library 'xercesImpl'(version >= 2.7.1) into classpath.");
		}
	}

	/**
	 * 设置Schema
	 * 
	 * @param node
	 * @param schemaURL
	 */
	public static void setXsdSchema(Node node, String schemaURL) {
		Document doc;
		if (node.getNodeType() != Node.DOCUMENT_NODE) {
			doc = node.getOwnerDocument();
		} else {
			doc = (Document) node;
		}
		Element root = doc.getDocumentElement();
		if (schemaURL == null) {
			root.removeAttribute("xmlns:xsi");
			root.removeAttribute("xsi:noNamespaceSchemaLocation");
		} else {
			root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			root.setAttribute("xsi:noNamespaceSchemaLocation", schemaURL);
		}
	}

	/**
	 * 丛Json格式转换为XML Document(兼容Json-Lib)
	 * 
	 * @param json
	 *            要读取的json
	 * @return 由json转换而成的XML
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document loadDocument(JSONObject json) {
		return XMLFastJsonParser.DEFAULT.toDocument(json);
	}

	/**
	 * 从XML Document转换为JsonObject,loadDocument(JsonObject json)的逆运算
	 * 
	 * @param node
	 *            要转换的节点
	 * @return 转换后的json对象
	 */
	public static JSONObject toJsonObject(Node node) {
		return XMLFastJsonParser.DEFAULT.toJsonObject(node);
	}

	/**
	 * 载入XML文档
	 * 
	 * @param file
	 *            文件
	 * @return Document
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document loadDocument(File file) throws SAXException, IOException {
		return loadDocument(file, true);
	}

	/**
	 * 载入XML文件
	 * 
	 * @param file
	 *            文件
	 * @param ignorComments
	 *            是否忽略掉XML中的注释
	 * @return Document
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document loadDocument(File file, boolean ignorComments) throws SAXException, IOException {
		InputStream in = IOUtils.getInputStream(file);
		try {
			Document document = loadDocument(in, null, true, false);
			return document;
		} finally {
			in.close();
		}
	}

	/**
	 * 传入文件路径，解析XML文件
	 * 
	 * @param filename
	 *            文件路径
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document loadDocument(String filename) throws SAXException, IOException {
		return loadDocument(new File(filename));
	}

	/**
	 * 从URL装载XML
	 * 
	 * @param reader
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document loadDocument(URL url) throws SAXException, IOException {
		return loadDocument(url.openStream(), null, true, false);
	}

	/**
	 * 从Reader装载XML
	 * 
	 * @param reader
	 *            数据
	 * @param ignorComments
	 *            是否跳过注解
	 * @param namespaceAware
	 *            是否忽略命名空间
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document loadDocument(Reader reader, boolean ignorComments, boolean namespaceAware) throws SAXException, IOException {
		DocumentBuilderFactory dbf = getFactory(ignorComments, namespaceAware);
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(new EH());
			InputSource is = new InputSource(reader);
			Document doc = db.parse(is);
			return doc;
		} catch (ParserConfigurationException x) {
			throw new Error(x);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	/**
	 * 解析xml文本
	 * 
	 * @param xmlContent
	 *            XML文本
	 * @return Document
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document loadDocumentByString(String xmlContent) throws SAXException, IOException {
		Reader reader = null;
		try {
			reader = new StringReader(xmlContent);
			return loadDocument(reader, true, false);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	private static DocumentBuilderFactory domFactoryTT;
	private static DocumentBuilderFactory domFactoryTF;
	private static DocumentBuilderFactory domFactoryFT;
	private static DocumentBuilderFactory domFactoryFF;

	private static DocumentBuilderFactory getFactory(boolean ignorComments, boolean namespaceAware) {
		if (ignorComments && namespaceAware) {
			if (domFactoryTT == null) {
				domFactoryTT = createDocumentBuilderFactory(true, true);
			}
			return domFactoryTT;
		}else if(ignorComments){
			if(domFactoryTF==null){
				domFactoryTF=createDocumentBuilderFactory(true, false);
			}
			return domFactoryTF;
		}else if(namespaceAware){
			if(domFactoryFT==null){
				domFactoryFT=createDocumentBuilderFactory(false, true);
			}
			return domFactoryFT;
		}else{
			if(domFactoryFF==null){
				domFactoryFF=createDocumentBuilderFactory(false, false);
			}
			return domFactoryFF;
		}
	}

	private static DocumentBuilderFactory createDocumentBuilderFactory(boolean ignorComments, boolean namespaceAware) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setValidating(false);
		dbf.setIgnoringComments(ignorComments);
		dbf.setNamespaceAware(namespaceAware);
		// dbf.setCoalescing(true);//CDATA
		// 节点转换为Text节点，并将其附加到相邻（如果有）的文本节点，开启后解析更方便，但无法还原
		try {
			// dbf.setFeature("http://xml.org/sax/features/namespaces", false);
			// dbf.setFeature("http://xml.org/sax/features/validation", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (ParserConfigurationException e) {
			LogUtil.warn("Your xerces implemention is too old that does not support 'load-dtd-grammar' & 'load-external-dtd' feature.");
		}
		return dbf;
	}

	/**
	 * 读取XML文档
	 * 
	 * @param in
	 *            输入流
	 * @param charSet
	 *            字符编码
	 * @param ignorComment
	 *            忽略注释
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document loadDocument(InputStream in, String charSet, boolean ignorComment) throws SAXException, IOException {
		return loadDocument(in, charSet, ignorComment, false);
	}

	/**
	 * 载入XML文档
	 * 
	 * @param in
	 *            输入流
	 * @param charSet
	 *            编码
	 * @param ignorComment
	 *            跳过注释节点
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document loadDocument(InputStream in, String charSet, boolean ignorComment, boolean namespaceaware) throws SAXException, IOException {
		DocumentBuilderFactory dbf = getFactory(ignorComment, namespaceaware);
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(new EH());
			db.setEntityResolver(null);
			// 用自定义的EntityResolver来提供DTD文件的 重定向
			// db.setEntityResolver(new EntityResolver() {
			// public InputSource resolveEntity(String publicId, String
			// systemId) throws SAXException, IOException {
			// return null;
			// }
			// });
			InputSource is = null;
			// 解析流来获取charset
			if (charSet == null) {// 读取头200个字节来分析编码
				byte[] buf = new byte[200];
				PushbackInputStream pin = new PushbackInputStream(in, 200);
				in = pin;
				int len = pin.read(buf);
				if (len > 0) {
					pin.unread(buf, 0, len);
					charSet = getCharsetInXml(buf, len);
				}
			}
			if (charSet != null) {
				is = new InputSource(new XmlFixedReader(new InputStreamReader(in, charSet)));
				is.setEncoding(charSet);
			} else { // 自动检测编码
				Reader reader = new InputStreamReader(in, "UTF-8");// 为了过滤XML当中的非法字符，所以要转换为Reader，又为了转换为Reader，所以要获得XML的编码
				is = new InputSource(new XmlFixedReader(reader));
			}
			Document doc = db.parse(is);
			doc.setXmlStandalone(true);// 设置为True保存时才不会出现讨厌的standalone="no"
			return doc;
		} catch (ParserConfigurationException x) {
			throw new Error(x);
		}
	}

	/**
	 * 通过读取XML头部文字来判断xml文件的编码
	 * 
	 * @param buf
	 * @param len
	 * @return
	 */
	public static String getCharsetInXml(byte[] buf, int len) {
		buf = ArrayUtils.subarray(buf, 0, len);
		String s = new String(buf).toLowerCase();
		int n = s.indexOf("encoding=");
		if (n > -1) {
			s = s.substring(n + 9);
			if (s.charAt(0) == '\"' || s.charAt(0) == '\'') {
				s = s.substring(1);
			}
			n = StringUtils.indexOfAny(s, "\"' ><");
			if (n > -1) {
				s = s.substring(0, n);
			}
			if (StringUtils.isEmpty(s)) {
				return null;
			}
			s = CharsetName.getStdName(s);
			return s;
		} else {
			return null;
		}
	}

	/**
	 * 载入HTML文档
	 * 
	 * @param in
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public static DocumentFragment loadHtmlDocument(Reader in) throws SAXException, IOException {
		if (parser == null)
			throw new UnsupportedOperationException("HTML parser module not loaded, to activate this feature, you must add JEF common-ioc.jar to classpath");
		InputSource source;
		source = new InputSource(in);
		synchronized (parser) {
			HTMLDocument document = new HTMLDocumentImpl();
			DocumentFragment fragment = document.createDocumentFragment();
			parser.parse(source, fragment);
			return fragment;
		}
	}

	/**
	 * 从指定文件载入HTML
	 * 
	 * @param in
	 * @param charSet
	 * @return DocumentFragment对象
	 * @throws SAXException
	 * @throws IOException
	 */
	public static DocumentFragment loadHtmlDocument(File file) throws IOException, SAXException {
		InputStream in = IOUtils.getInputStream(file);
		try {
			DocumentFragment document = loadHtmlDocument(in, null);
			return document;
		} finally {
			in.close();
		}
	}

	/**
	 * 从指定的地址加载HTMLDocument
	 * 
	 * @param url
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public static DocumentFragment loadHtmlDocument(URL url) throws SAXException, IOException {
		return loadHtmlDocument(url.openStream(), null);
	}

	/**
	 * 从指定流解析HTML
	 * 
	 * @param in
	 * @param charSet
	 * @return DocumentFragment对象
	 * @throws SAXException
	 * @throws IOException
	 */
	public static DocumentFragment loadHtmlDocument(InputStream in, String charSet) throws SAXException, IOException {
		if (parser == null)
			throw new UnsupportedOperationException("HTML parser module not loaded, to activate this feature, you must add JEF common-ioc.jar to classpath");
		InputSource source;
		if (charSet != null) {
			source = new InputSource(new XmlFixedReader(new InputStreamReader(in, charSet)));
			source.setEncoding(charSet);
		} else {
			source = new InputSource(in);
		}
		synchronized (parser) {
			HTMLDocument document = new HTMLDocumentImpl();
			DocumentFragment fragment = document.createDocumentFragment();
			parser.parse(source, fragment);
			return fragment;
		}
	}

	/**
	 * 保存XML文档
	 * 
	 * @param doc
	 * @param file
	 * @throws IOException
	 */
	public static void saveDocument(Node doc, File file) throws IOException {
		saveDocument(doc, file, "UTF-8");
	}

	/**
	 * 保存XML文档
	 * 
	 * @param doc
	 *            DOM对象
	 * @param file
	 *            文件
	 * @param encoding
	 *            编码
	 * @throws IOException
	 */
	public static void saveDocument(Node doc, File file, String encoding) throws IOException {
		OutputStream os = IOUtils.getOutputStream(file);
		try {
			output(doc, os, encoding);
		} finally {
			os.close();
		}
	}

	/**
	 * 将XML文档输出到流
	 * 
	 * @param node
	 * @param os
	 * @param encoding
	 * @throws IOException
	 */
	public static void output(Node node, OutputStream os) throws IOException {
		output(node, os, null, true, null);
	}

	/**
	 * 将XML文档输出到流
	 * 
	 * @param node
	 * @param os
	 * @param encoding
	 * @throws IOException
	 */
	public static void output(Node node, OutputStream os, String encoding) throws IOException {
		output(node, os, encoding, true, null);
	}

	/**
	 * 节点转换为String
	 * 
	 * @param node
	 * @return
	 */
	public static String toString(Node node) {
		return toString(node, null);
	}

	/**
	 * 将XML文档输出到流
	 * 
	 * @param node
	 *            DOM对象
	 * @param os
	 *            输出流
	 * @param encoding
	 *            编码
	 * @param warpLine
	 *            折行输出
	 * @param xmlDeclare
	 *            null如果是document對象則頂部有xml定義，true不管如何都有 false都沒有
	 * @throws IOException
	 */
	public static void output(Node node, OutputStream os, String encoding, boolean warpLine, Boolean xmlDeclare) throws IOException {
		StreamResult sr = new StreamResult(encoding == null ? new OutputStreamWriter(os) : new OutputStreamWriter(os, encoding));
		output(node, sr, encoding, warpLine, xmlDeclare);
	}

	/**
	 * 保存文档
	 * 
	 * @param node
	 *            要保存的节点或Document
	 * @param os
	 *            输出流
	 * @param encoding
	 *            编码
	 * @param warpLine
	 *            是否要排版
	 * @throws IOException
	 */
	public static void output(Node node, Writer os, String encoding, boolean warpLine) throws IOException {
		StreamResult sr = new StreamResult(os);
		output(node, sr, encoding, warpLine, null);
	}

	private static void output(Node node, StreamResult sr, String encoding, boolean warpLine, Boolean XmlDeclarion) throws IOException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = null;
		if (warpLine) {
			try {
				tf.setAttribute("indent-number", 4);
			} catch (Exception e) {
			}
		}
		try {
			t = tf.newTransformer();
			if (warpLine) {
				try {// 某些垃圾的XML解析包会造成无法正确的设置属性
					t.setOutputProperty(OutputKeys.INDENT, "yes");
				} catch (Exception e) {
				}
			}
			t.setOutputProperty(OutputKeys.METHOD, "xml");
			if (encoding != null) {
				t.setOutputProperty(OutputKeys.ENCODING, encoding);
			}
			if (XmlDeclarion == null) {
				XmlDeclarion = (node instanceof Document);
			}
			if (XmlDeclarion) {
				t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			} else {
				t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			}
		} catch (Exception tce) {
			assert (false);
		}
		DOMSource doms = new DOMSource(node);
		try {
			t.transform(doms, sr);
		} catch (TransformerException te) {
			IOException ioe = new IOException();
			ioe.initCause(te);
			throw ioe;
		}
	}

	private static class EH implements ErrorHandler {
		public void error(SAXParseException x) throws SAXException {
			throw x;
		}

		public void fatalError(SAXParseException x) throws SAXException {
			throw x;
		}

		public void warning(SAXParseException x) throws SAXException {
			throw x;
		}
	}

	/**
	 * 在指定节点下添加一个CDATA节点
	 * 
	 * @param node
	 *            父节点
	 * @param data
	 *            CDATA文字内容
	 * @return
	 */
	public static CDATASection addCDataText(Node node, String data) {
		Document doc = null;
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			doc = (Document) node;
		} else {
			doc = node.getOwnerDocument();
		}
		CDATASection e = doc.createCDATASection(data);
		node.appendChild(e);
		return e;
	}

	/**
	 * 标准XPath计算
	 * 
	 * @param expr
	 * @param startPoint
	 * @return
	 * @throws XPathExpressionException
	 */
	@Deprecated
	public static String evalXpath(Object startPoint, String expr) throws XPathExpressionException {
		XPath xpath = xp.newXPath();
		return xpath.evaluate(expr, startPoint);
	}

	/**
	 * 标准XPATH计算
	 * 
	 * @param expr
	 * @param startPoint
	 * @param returnType
	 * @return
	 * @throws XPathExpressionException
	 */
	@Deprecated
	public static Node selectNode(Object startPoint, String expr) throws XPathExpressionException {
		XPath xpath = xp.newXPath();
		return (Node) xpath.evaluate(expr, startPoint, XPathConstants.NODE);
	}

	/**
	 * 标准XPATH计算
	 * 
	 * @param startPoint
	 * @param expr
	 * @return
	 * @throws XPathExpressionException
	 */
	@Deprecated
	public static NodeList selectNodes(Object startPoint, String expr) throws XPathExpressionException {
		XPath xpath = xp.newXPath();
		return (NodeList) xpath.evaluate(expr, startPoint, XPathConstants.NODESET);
	}

	/**
	 * 标准XPATH计算
	 * 
	 * @param start
	 * @param expr
	 * @return
	 * @throws XPathExpressionException
	 */
	public static List<Element> selectElements(Node start, String expr) throws XPathExpressionException {
		return toElementList(selectNodes(start, expr));
	}

	/**
	 * 在节点下插入文本
	 * 
	 * @param node
	 *            节点
	 * @param data
	 *            文本内容
	 * @return
	 */
	public static Text setText(Node node, String data) {
		Document doc = null;
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			doc = (Document) node;
		} else {
			doc = node.getOwnerDocument();
		}
		clearChildren(node, Node.TEXT_NODE);
		Text t = doc.createTextNode(data);
		node.appendChild(t);
		return t;
	}

	/**
	 * 在一个节点下插入注释
	 * 
	 * @param node
	 * @param comment
	 *            注释内容
	 * @return
	 */
	public static Comment addComment(Node node, String comment) {
		Document doc = null;
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			doc = (Document) node;
		} else {
			doc = node.getOwnerDocument();
		}
		Comment e = doc.createComment(comment);
		node.appendChild(e);
		return e;
	}

	/**
	 * 在指定节点之前插入节点
	 * 
	 * @param node
	 * @param tagName
	 * @param nodeText
	 * @return
	 */
	public static Element addElementBefore(Node node, String tagName, String... nodeText) {
		Node pNode = node.getParentNode();
		List<Node> movingNodes = new ArrayList<Node>();
		for (Node n : toArray(pNode.getChildNodes())) {
			if (n == node) {
				movingNodes.add(n);
			} else if (movingNodes.size() > 0) {
				movingNodes.add(n);
			}
		}
		Element e = addElement(pNode, tagName, nodeText);
		for (Node n : movingNodes) {
			pNode.appendChild(n);
		}
		return e;
	}

	/**
	 * 在之后插入节点
	 * 
	 * @param node
	 * @param tagName
	 * @param nodeText
	 * @return
	 */
	public static Element addElementAfter(Node node, String tagName, String... nodeText) {
		Node pNode = node.getParentNode();
		List<Node> movingNodes = new ArrayList<Node>();
		boolean flag = false;
		for (Node n : toArray(pNode.getChildNodes())) {
			if (flag) {
				movingNodes.add(n);
			} else if (n == node) {
				flag = true;
			}
		}
		Element e = addElement(pNode, tagName, nodeText);
		for (Node n : movingNodes) {
			pNode.appendChild(n);
		}
		return e;
	}

	/**
	 * 生成新节点替换原来的节点
	 * 
	 * @param node
	 *            旧节点
	 * @param tagName
	 *            新节点名称
	 * @param nodeText
	 *            节点文本
	 * @author Administrator
	 */
	public static Element replaceElement(Node node, String tagName, String... nodeText) {
		Node pNode = node.getParentNode();
		Assert.notNull(pNode);
		Document doc = null;
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			doc = (Document) node;
		} else {
			doc = node.getOwnerDocument();
		}
		Element e = doc.createElement(tagName);
		if (nodeText.length == 1) {
			setText(e, nodeText[0]);
		} else if (nodeText.length > 1) {
			setText(e, StringUtils.join(nodeText, '\n'));
		}
		pNode.replaceChild(e, node);
		return e;
	}

	/**
	 * 在指定节点下查找一个Element，如果没有就添加
	 * 
	 * @param parent
	 * @param tagName
	 * @param attribName
	 * @param attribValue
	 * @return
	 */
	public static Element getOrCreateChildElement(Node parent, String tagName, String attribName, String attribValue) {
		for (Element e : XMLUtils.childElements(parent, tagName)) {
			if (attribValue == null || attribValue.equals(XMLUtils.attrib(e, attribName))) {
				return e;
			}
		}
		Element e = XMLUtils.addElement(parent, tagName);
		e.setAttribute(attribName, attribValue);
		return e;
	}

	/**
	 * 删除节点下的指定了TagName的元素
	 * 
	 * @param node
	 * @param tagName
	 * @return 删除数量
	 */
	public static int removeChildElements(Node node, String... tagName) {
		List<Element> list = XMLUtils.childElements(node, tagName);
		for (Element e : list) {
			node.removeChild(e);
		}
		return list.size();
	}

	/**
	 * 清除节点的所有子元素
	 * 
	 * @param node
	 */
	public static void clearChildren(Node node) {
		clearChildren(node, 0);
	}

	/**
	 * 清除下属的指定类型的节点
	 * 
	 * @param node
	 * @param type
	 *            如果不限制NodeType，传入0
	 */
	public static void clearChildren(Node node, int type) {
		for (Node child : toArray(node.getChildNodes())) {
			if (type == 0 || child.getNodeType() == type) {
				node.removeChild(child);
			}
		}
	}

	/**
	 * 清除元素节点的所有属性
	 * 
	 * @param element
	 */
	public static void clearAttribute(Element element) {
		for (Node node : toArray(element.getAttributes())) {
			element.removeAttributeNode((Attr) node);
		}
	}

	/**
	 * 清除元素节点所有属性和子节点
	 * 
	 * @param element
	 */
	public static void clearChildrenAndAttr(Element element) {
		clearChildren(element);
		clearAttribute(element);
	}

	/**
	 * 在一个节点下插入元素和文本
	 * 
	 * @param node
	 * @param tagName
	 * @param nodeText
	 * @return
	 */
	public static Element addElement(Node node, String tagName, String... nodeText) {
		Document doc = null;
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			doc = (Document) node;
		} else {
			doc = node.getOwnerDocument();
		}
		Element e = doc.createElement(tagName);
		node.appendChild(e);
		if (nodeText.length == 1) {
			setText(e, nodeText[0]);
		} else if (nodeText.length > 1) {
			setText(e, StringUtils.join(nodeText, '\n'));
		}
		return e;
	}

	/**
	 * 反回一个新节点，代替旧节点，其名称可以设置
	 * 
	 * @param node
	 * @param newName
	 * @return
	 */
	public static Element changeNodeName(Element node, String newName) {
		Document doc = node.getOwnerDocument();
		Element newEle = doc.createElement(newName);
		Node parent = node.getParentNode();
		parent.removeChild(node);
		parent.appendChild(newEle);

		for (Node child : toArray(node.getChildNodes())) {
			node.removeChild(child);
			newEle.appendChild(child);
		}
		return newEle;
	}

	/**
	 * 得到节点下，具有指定标签的Element。(只搜索一层)
	 * 
	 * @param node
	 * @param tagName
	 *            ,标签，如果为null表示返回全部Element
	 * @return
	 */
	public static List<Element> childElements(Node node, String... tagName) {
		if (node == null)
			throw new NullPointerException("the input node can not be null!");
		List<Element> list = new ArrayList<Element>();
		NodeList nds = node.getChildNodes();
		if (tagName.length == 0 || tagName[0] == null) {// 预处理，兼容旧API
			tagName = null;
		}
		for (int i = 0; i < nds.getLength(); i++) {
			Node child = nds.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) child;
				if (tagName == null || ArrayUtils.contains(tagName, e.getNodeName())) {
					list.add(e);
				}
			} else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
			} else if (child.getNodeType() == Node.COMMENT_NODE) {
			} else if (child.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {

			} else if (child.getNodeType() == Node.DOCUMENT_NODE) {

			} else if (child.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
			} else if (child.getNodeType() == Node.ATTRIBUTE_NODE) {
			} else if (child.getNodeType() == Node.TEXT_NODE) {
			}
		}
		return list;
	}

	static class MyNodeList implements NodeList {
		Node[] list;

		public int getLength() {
			return list.length;
		}

		public Node item(int index) {
			return list[index];
		}

		public MyNodeList(Node[] list) {
			this.list = list;
		}

		public MyNodeList(List<? extends Node> list) {
			this.list = list.toArray(new Node[list.size()]);
		}
	}

	/**
	 * 获取指定元素的文本(Trimed)
	 * 
	 * @param element
	 * @return
	 */
	public static String nodeText(Node element) {
		Node first = first(element, Node.TEXT_NODE, Node.CDATA_SECTION_NODE);
		if (first != null && first.getNodeType() == Node.CDATA_SECTION_NODE) {
			return ((CDATASection) first).getTextContent();
		}
		StringBuilder sb = new StringBuilder();
		if (first == null || StringUtils.isBlank(first.getTextContent())) {
			for (Node n : toArray(element.getChildNodes())) {
				if (n.getNodeType() == Node.TEXT_NODE) {
					sb.append(n.getTextContent());
				} else if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
					sb.append(((CDATASection) n).getTextContent());
				}
			}
		} else {
			sb.append(first.getTextContent());
		}
		return StringUtils.trimToNull(StringEscapeUtils.unescapeHtml(sb.toString()));
	}

	/**
	 * 得到节点下全部的text文本内容
	 * 
	 * @param element
	 * @param withChildren
	 *            :如果为真，则将该节点下属所有节点的文本合并起来返回
	 * @return
	 */
	public static String nodeText(Node element, boolean withChildren) {
		StringBuilder sb = new StringBuilder();
		for (Node node : toArray(element.getChildNodes())) {
			if (node.getNodeType() == Node.TEXT_NODE) {
				sb.append(node.getNodeValue().trim());
			} else if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
				sb.append(((CDATASection) node).getTextContent());
			} else if (withChildren) {
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					sb.append(nodeText((Element) node, true));
				}
			}
		}
		return sb.toString();
	}

	/**
	 * 获得属性值
	 * 
	 * @param e
	 * @param attributeName
	 * @return
	 */
	public static String attrib(Element e, String attributeName) {
		if (!e.hasAttribute(attributeName))
			return null;
		String text = e.getAttribute(attributeName);
		return (text == null) ? null : StringEscapeUtils.unescapeHtml(text.trim());
	}

	/**
	 * 获得属性值(遍历子节点)
	 * 
	 * @param e
	 * @param attributeName
	 * @return
	 */
	public static List<String> attribs(Element e, String attributeName) {
		List<String> _list = new ArrayList<String>();
		if (e.hasAttribute(attributeName)) {
			String text = e.getAttribute(attributeName);
			_list.add((text == null) ? null : StringEscapeUtils.unescapeHtml(text.trim()));
		}
		if (e.hasChildNodes()) {
			NodeList nds = e.getChildNodes();
			for (int i = 0; i < nds.getLength(); i++) {
				Node child = nds.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					_list.addAll(attribs((Element) child, attributeName));
				}
			}
		}
		return _list;
	}

	/**
	 * 获取当前元素下，一个子元素的文本(Trim)
	 * 
	 * @param element
	 * @param subEleName
	 * @return
	 */
	public static String nodeText(Element element, String subEleName) {
		Element e = first(element, subEleName);
		if (e == null)
			return null;
		return nodeText(e);
	}

	/** 得到节点下第n个指定元素(不分层次) */
	public static Element nthElement(Element parent, String elementName, int index) {
		NodeList nds = parent.getElementsByTagName(elementName);
		if (nds.getLength() < index)
			throw new NoSuchElementException();
		Element node = (Element) nds.item(index - 1);
		return node;
	}

	/**
	 * 得到当前元素下，第一个符合Tag Name的子元素
	 * 
	 * @param parent
	 * @param elementName
	 * @return
	 */
	public static Element first(Node node, String tagName) {
		if (node == null)
			return null;
		NodeList nds = node.getChildNodes();
		for (int i = 0; i < nds.getLength(); i++) {
			Node child = nds.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) child;
				if (tagName == null || tagName.equals(e.getNodeName())) {
					return e;
				}
				// } else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
				// } else if (child.getNodeType() == Node.COMMENT_NODE) {
				// } else if (child.getNodeType() ==
				// Node.DOCUMENT_FRAGMENT_NODE) {
				// } else if (child.getNodeType() == Node.DOCUMENT_NODE) {
				// } else if (child.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
				// } else if (child.getNodeType() == Node.ATTRIBUTE_NODE) {
				// } else if (child.getNodeType() == Node.TEXT_NODE) {
			}
		}
		return null;
	}

	/**
	 * 获得符合类型的第一个节点(单层)
	 * 
	 * @param node
	 * @param nodeType
	 * @return
	 */
	public static Node first(Node node, int... nodeType) {
		if (node == null)
			return null;
		NodeList nds = node.getChildNodes();
		for (int i = 0; i < nds.getLength(); i++) {
			Node child = nds.item(i);
			if (ArrayUtils.contains(nodeType, child.getNodeType())) {
				return child;
			}
		}
		return null;
	}

	/**
	 * 创建一份带有根元素节点的XML文档
	 * 
	 * @param tagName
	 *            根节点元素名
	 * @return
	 */
	public static Document newDocument(String tagName) {
		Assert.notNull(tagName);
		Document doc = newDocument();
		addElement(doc, tagName);
		return doc;
	}

	/**
	 * 创建一份新的空白XML文档
	 * 
	 * @return
	 */
	public static Document newDocument() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.newDocument();
			document.setXmlStandalone(true);
			return document;
		} catch (ParserConfigurationException e) {
			LogUtil.exception(e);
			return null;
		}
	}

	/**
	 * 将NamedNodeMap对象转换为List对象
	 * 
	 * @param nds
	 * @return
	 */
	public static Node[] toArray(NamedNodeMap nds) {
		Node[] array = new Node[nds.getLength()];
		for (int i = 0; i < nds.getLength(); i++) {
			array[i] = nds.item(i);
		}
		return array;
	}

	/**
	 * 将Map所有值设置为属性
	 * 
	 * @param e
	 * @param attrMap
	 *            属性，这些值将作为属性设置到当前节点上
	 * @isSubNode 设置方式，false时优先设置为属性,true时设置为子节点
	 */
	public static void setAttributesByMap(Element e, Map<String, Object> attrMap, boolean isSubNode) {
		if (attrMap == null)
			return;
		setAttrMap(e, attrMap, isSubNode);
	}

	/**
	 * 将Map所有值设置为属性
	 * 
	 * @param e
	 * @param map
	 */
	public static void setAttributesByMap(Element e, Map<String, Object> map) {
		setAttributesByMap(e, map, false);
	}

	@SuppressWarnings("rawtypes")
	private static void setAttrMap(Element e, Map attrMap, boolean isSubNode) {
		if (isSubNode) {
			for (Object keyObj : attrMap.keySet()) {
				String key = StringUtils.toString(keyObj);
				Object value = attrMap.get(key);
				if (value.getClass().isArray()) {
					setAttrArray(e, key, (Object[]) value, isSubNode);
					continue;
				} else if (value instanceof List) {
					setAttrArray(e, key, ((List) value).toArray(), isSubNode);
					continue;
				}
				Element child = first(e, key);
				if (child == null) {
					child = addElement(e, key);
				}

				if (value instanceof Map) {
					setAttrMap(child, (Map) value, isSubNode);
				} else {
					setText(child, StringUtils.toString(value));
				}

			}
		} else {
			for (Object keyObj : attrMap.keySet()) {
				String key = StringUtils.toString(keyObj);
				Object value = attrMap.get(key);
				if (value instanceof Map) {
					Element child = first(e, key);
					if (child == null) {
						child = addElement(e, key);
					}
					setAttrMap(child, (Map) value, isSubNode);
				} else if (value.getClass().isArray()) {
					setAttrArray(e, key, (Object[]) value, isSubNode);
				} else if (value instanceof List) {
					setAttrArray(e, key, ((List) value).toArray(), isSubNode);
				} else {
					e.setAttribute(key, StringUtils.toString(value));
				}

			}
		}
	}

	@SuppressWarnings("rawtypes")
	private static void setAttrArray(Element e, String key, Object[] value, boolean isSubNode) {
		for (Object o : value) {
			if (o instanceof Map) {
				Element child = addElement(e, key);
				setAttrMap(child, (Map) o, isSubNode);
			} else {
				Element child = addElement(e, key);
				setText(child, StringUtils.toString(o));
			}
		}
	}

	/**
	 * 根据xpath设置若干属性的值
	 * 
	 * @param node
	 * @param xPath
	 * @param attribute
	 * @param isSubNode
	 */
	public static void setAttributeByXpath(Node node, String xPath, Map<String, Object> attribute, boolean isSubNode) {
		int i = xPath.lastIndexOf('@');
		if (i >= 0)
			throw new IllegalArgumentException("there is @ in your xpath.");
		Node n = getNodeByXPath(node, xPath);
		if (n instanceof Element) {
			setAttributesByMap((Element) n, attribute, isSubNode);
		} else {
			throw new IllegalArgumentException("node at " + xPath + " is not a element!");
		}
	}

	public static void setAttributeByXPath(Node node, String xPath, String value) {
		int i = xPath.lastIndexOf('@');
		if (i < 0)
			throw new IllegalArgumentException("there is no @ in your xpath.");
		String left = xPath.substring(0, i);
		String right = xPath.substring(i + 1);
		Node n = getNodeByXPath(node, left);
		if (n instanceof Element) {
			if ("#text".equals(right)) {
				setText(n, value);
			} else {
				((Element) n).setAttribute(right, value);
			}
		} else {
			throw new IllegalArgumentException("node at " + left + " is not a element!");
		}
	}

	/**
	 * 设置文本节点的值
	 * 
	 * @param e
	 * @param tagName
	 * @param value
	 */
	public static void setNodeText(Element e, String tagName, String value) {
		Element child = first(e, tagName);
		if (child != null) {
			setText(child, value);
		}
	}

	/**
	 * 获取所有属性，以Map形式返回
	 */
	public static Map<String, String> getAttributesMap(Element e) {
		return getAttributesMap(e, false);
	}

	/**
	 * 获取所有属性。
	 * 
	 * @param e
	 * @param subElementAsAttr
	 *            为true时，包括下属第一级的Element后的文本节点，也作为属性返回<br>
	 *            例如
	 * 
	 *            <pre>
	 * &lt;Foo size="103" name="Karen"&gt;
	 *   &lt;dob&gt;2012-4-12&lt;/dobh&gt;
	 *   &lt;dod&gt;2052-4-12&lt;/dodh&gt;
	 * &lt;/Foo&gt;
	 * </pre>
	 * 
	 *            当subElementAsAttr=false时，dob,dod不作为属性，而当为true时则作为属性处理
	 * @return
	 */
	public static Map<String, String> getAttributesMap(Element e, boolean subElementAsAttr) {
		Map<String, String> attribs = new HashMap<String, String>();
		if (e == null)
			return attribs;
		NamedNodeMap nmp = e.getAttributes();
		for (int i = 0; i < nmp.getLength(); i++) {
			Attr child = (Attr) nmp.item(i);
			attribs.put(StringEscapeUtils.unescapeHtml(child.getName()), StringEscapeUtils.unescapeHtml(child.getValue()));
		}
		if (subElementAsAttr) {
			NodeList nds = e.getChildNodes();
			for (int i = 0; i < nds.getLength(); i++) {
				Node node = nds.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE)
					continue;
				Element sub = (Element) node;
				String key = sub.getNodeName();
				String value = nodeText(sub);
				if (attribs.containsKey(key)) {
					attribs.put(key, attribs.get(key) + "," + value);
				} else {
					attribs.put(key, value);
				}
			}
		}
		return attribs;
	}

	/**
	 * 从子节点的文本当做属性来获取
	 * 
	 * @param e
	 * @return
	 */
	public static Map<String, String> getAttributesInChildElements(Element e, String... keys) {
		NodeList nds = e.getChildNodes();
		Map<String, String> attribs = new HashMap<String, String>();
		for (int i = 0; i < nds.getLength(); i++) {
			Node node = nds.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element sub = (Element) node;
			String key = sub.getNodeName();
			if (keys.length == 0 || ArrayUtils.contains(keys, key)) {
				String value = nodeText(sub);
				if (attribs.containsKey(key)) {
					attribs.put(key, attribs.get(key) + "," + value);
				} else {
					attribs.put(key, value);
				}
			}
		}
		return attribs;
	}

	public static void moveChildElementAsAttribute(Element e, String... keys) {
		NodeList nds = e.getChildNodes();
		for (Node node : toArray(nds)) {
			if (node.getNodeType() == Node.TEXT_NODE) {
				e.removeChild(node); // 删除空白文本节点
			}
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element sub = (Element) node;
			String key = sub.getNodeName();
			if (keys.length == 0 || ArrayUtils.contains(keys, key)) {
				String value = nodeText(sub);
				e.setAttribute(key, value);
				e.removeChild(sub);
			}

		}
	}

	/**
	 * 将节点的属性赋值到指定的bean中
	 * 
	 * @param e
	 * @param bean
	 * @throws ReflectionException
	 * @deprecated 使用loadBean(Element,Class)方法
	 */
	@Deprecated
	public static <W> W elementToBean(Element e, Class<W> clz) throws ReflectionException {
		return loadBean(e, clz);
	}

	/**
	 * 将一个Element作为一个Bean那样载入 注意这个方法和并不是putBean的逆运算、因为条件所限，这里只load
	 * bean的属性，但不会load bean内部其他bean的值。即不支持递归嵌套。 而putBean的功能是比较强的。
	 * 
	 * @param e
	 * @param bean
	 * @throws ReflectionException
	 */
	public static <W> W loadBean(Element e, Class<W> clz) {
		W bean = UnsafeUtils.newInstance(clz);
		BeanWrapperImpl bw = new BeanWrapperImpl(bean);
		Map<String, String> attrs = getAttributesMap(e, true);
		for (String key : bw.getPropertyNames()) {
			if (attrs.containsKey(key)) {
				bw.setPropertyValueByString(key, attrs.get(key));
			}
		}
		return bean;
	}

	/**
	 * 将指定的bean展开后添加到当前的XML节点下面
	 * 
	 * @param node
	 *            要放置的节点
	 * @param bean
	 *            要放置的对象
	 * @return
	 */
	public static Element putBean(Node node, Object bean) {
		if (bean == null)
			return null;
		return appendBean(node, bean, bean.getClass(), null, null);
	}

	/**
	 * 将指定的bean展开后添加到当前的XML节点下面
	 * 
	 * @param node
	 *            要放置的节点
	 * @param bean
	 *            要放置的对象
	 * @param tryAttribute
	 *            当为true时对象的属性尽量作为XML属性 当为false对象的属性都作为XML文本节点
	 *            当为null时自动判断，一些简单类型作为属性，复杂类型用文本节点
	 * @return
	 */
	public static Element putBean(Node node, Object bean, Boolean tryAttribute) {
		if (bean == null)
			return null;
		return appendBean(node, bean, bean.getClass(), tryAttribute, null);
	}

	private static Element appendBean(Node parent, Object bean, Class<?> type, Boolean asAttrib, String tagName) {
		if (type == null) {
			if (bean == null) {
				return null;
			}
			type = bean.getClass();
		}
		if (tagName == null || tagName.length() == 0) {
			tagName = type.getSimpleName();
		}
		if (type.isArray()) {
			if (bean == null)
				return null;
			Element collection = addElement(parent, tagName);
			for (int i = 0; i < Array.getLength(bean); i++) {
				appendBean(collection, Array.get(bean, i), null, asAttrib, null);
			}
			return collection;
		} else if (Collection.class.isAssignableFrom(type)) {
			if (bean == null)
				return null;
			Element collection = addElement(parent, tagName);
			for (Object obj : (Collection<?>) bean) {
				appendBean(collection, obj, null, asAttrib, null);
			}
			return collection;
		} else if (CharSequence.class.isAssignableFrom(type)) {
			if (Boolean.TRUE.equals(asAttrib)) {
				((Element) parent).setAttribute(tagName, StringUtils.toString(bean));
			} else {
				addElement(parent, tagName, StringUtils.toString(bean));
			}
		} else if (Date.class.isAssignableFrom(type)) {
			if (Boolean.FALSE.equals(asAttrib)) {
				addElement(parent, tagName, DateUtils.formatDateTime((Date) bean));
			} else {
				((Element) parent).setAttribute(tagName, DateUtils.formatDateTime((Date) bean));
			}
		} else if (Number.class.isAssignableFrom(type) || type.isPrimitive() || type == Boolean.class) {
			if (Boolean.FALSE.equals(asAttrib)) {
				addElement(parent, tagName, StringUtils.toString(bean));
			} else {
				((Element) parent).setAttribute(tagName, StringUtils.toString(bean));
			}
		} else {
			if (bean == null)
				return null;
			Element root = addElement(parent, type.getSimpleName());
			BeanWrapper bw = BeanWrapper.wrap(bean);
			for (Property p : bw.getProperties()) {
				appendBean(root, p.get(bean), p.getType(), asAttrib, p.getName());
			}
			return root;
		}
		return null;
	}

	/**
	 * NodeList转换为数组
	 * 
	 * @param nds
	 * @return
	 */
	public static Node[] toArray(NodeList nds) {
		if (nds instanceof MyNodeList)
			return ((MyNodeList) nds).list;
		Node[] array = new Node[nds.getLength()];
		for (int i = 0; i < nds.getLength(); i++) {
			array[i] = nds.item(i);
		}
		return array;
	}

	/**
	 * NodeList对象转换为List
	 * 
	 * @param nds
	 * @return
	 */
	public static List<? extends Node> toList(NodeList nds) {
		if (nds instanceof MyNodeList)
			return Arrays.asList(((MyNodeList) nds).list);
		List<Node> list = new ArrayList<Node>();

		for (int i = 0; i < nds.getLength(); i++) {
			Node child = nds.item(i);
			list.add(child);
		}
		return list;
	}

	static class NodeListIterable implements Iterable<Node> {
		private int n;
		private int len;
		private NodeList nds;

		NodeListIterable(NodeList nds) {
			this.nds = nds;
			this.len = nds.getLength();
		}

		public Iterator<Node> iterator() {
			return new Iterator<Node>() {
				public boolean hasNext() {
					return n < len;
				}

				public Node next() {
					return nds.item(n++);
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	/**
	 * 将Nodelist转换为Element List
	 * 
	 * @param nds
	 * @return
	 */
	public static List<Element> toElementList(NodeList nds) {
		List<Element> list = new ArrayList<Element>();
		for (int i = 0; i < nds.getLength(); i++) {
			Node child = nds.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				list.add((Element) child);
			}
		}
		return list;
	}

	/**
	 * 将List对象转换为NodeList对象
	 * 
	 * @param nds
	 * @return
	 */
	public static NodeList toNodeList(List<? extends Node> list) {
		return new MyNodeList(list);
	}

	/**
	 * 数组转换为NodeList
	 * 
	 * @param list
	 * @return
	 */
	public static NodeList toNodeList(Node[] list) {
		return new MyNodeList(list);
	}

	/**
	 * 在当前节点及下属节点中查找文本
	 * 
	 * @param node
	 * @param text
	 * @param searchAttribute
	 * @return Node对象，匹配文本的第一个节点
	 */
	public static Node findFirst(Node node, String text, boolean searchAttribute) {
		String value = getValue(node);
		if (value != null && value.indexOf(text) > -1)
			return node;
		if (searchAttribute && node.getAttributes() != null) {
			for (Node n : toArray(node.getAttributes())) {
				value = getValue(n);
				if (value != null && value.indexOf(text) > -1)
					return n;
			}
		}
		for (Node sub : toArray(node.getChildNodes())) {
			Node nd = findFirst(sub, text, searchAttribute);
			if (nd != null)
				return nd;
		}
		return null;
	}

	/**
	 * 在当前节点及下属节点中并删除节点
	 * 
	 * @param node
	 * @param text
	 * @param searchAttribute
	 * @return Node对象，匹配文本的第一个节点
	 */
	public static void removeFirstNode(Node node, String text, boolean searchAttribute) {
		String value = getValue(node);
		if (value != null && value.indexOf(text) > -1)
			node.getParentNode().removeChild(node);
		if (searchAttribute && node.getAttributes() != null) {
			for (Node n : toArray(node.getAttributes())) {
				value = getValue(n);
				if (value != null && value.indexOf(text) > -1)
					node.getParentNode().removeChild(node);
			}
		}
		for (Node sub : toArray(node.getChildNodes())) {
			removeFirstNode(sub, text, searchAttribute);
		}
	}

	/**
	 * 在当前节点及下属节点中查找文本
	 * 
	 * @param node
	 * @param text
	 * @param searchAttribute
	 * @return Node对象，匹配文本的第一个节点
	 */
	public static Node[] find(Node node, String text, boolean searchAttribute) {
		List<Node> result = new ArrayList<Node>();
		innerSearch(node, text, result, searchAttribute);
		return result.toArray(new Node[0]);
	}

	private static void innerSearch(Node node, String text, List<Node> result, boolean searchAttribute) {
		String value = getValue(node);
		// 检查节点本身
		if (value != null && value.indexOf(text) > -1)
			result.add(node);
		// 检查属性节点
		if (searchAttribute && node.getAttributes() != null) {
			for (Node n : toArray(node.getAttributes())) {
				value = getValue(n);
				if (value != null && value.indexOf(text) > -1) {
					result.add(n);
				}
			}
		}
		// 检查下属元素节点
		for (Node sub : toArray(node.getChildNodes())) {
			innerSearch(sub, text, result, searchAttribute);
		}
	}

	private static String getValue(Node node) {
		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE:
			return nodeText((Element) node);
		case Node.TEXT_NODE:
			return StringUtils.trimToNull(StringEscapeUtils.unescapeHtml(node.getTextContent()));
		case Node.CDATA_SECTION_NODE:
			return ((CDATASection) node).getTextContent();
		default:
			return StringEscapeUtils.unescapeHtml(node.getNodeValue());
		}
	}

	private static void innerSearchByAttribute(Node node, String attribName, String id, List<Element> result, boolean findFirst) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element e = (Element) node;
			String s = attrib(e, attribName);
			if (s != null && s.equals(id)) {
				result.add(e);
				if (findFirst)
					return;
			}
		}

		for (Node sub : toArray(node.getChildNodes())) {
			innerSearchByAttribute(sub, attribName, id, result, findFirst);
			if (findFirst && result.size() > 0)
				return;
		}
	}

	/**
	 * 查找指定名称的Element，并且其指定的属性值符合条件
	 * 
	 * @param root
	 *            根节点
	 * @param tagName
	 *            要匹配的element名称
	 * @param attribName
	 *            要匹配的属性名城
	 * @param keyword
	 *            要匹配的属性值
	 * @return
	 */
	public static Element findElementByNameAndAttribute(Node root, String tagName, String attribName, String keyword) {
		Element[] es = findElementsByNameAndAttribute(root, tagName, attribName, keyword, true);
		if (es.length > 0)
			return es[0];
		return null;
	}

	/**
	 * 查找指定名称的Element，并且其指定的属性值符合条件
	 * 
	 * @param root
	 *            根节点
	 * @param tagName
	 *            要匹配的element名称
	 * @param attribName
	 *            要匹配的属性名城
	 * @param keyword
	 *            要匹配的属性值
	 * @return
	 */
	public static Element[] findElementsByNameAndAttribute(Node root, String tagName, String attribName, String keyword) {
		return findElementsByNameAndAttribute(root, tagName, attribName, keyword, false);
	}

	private static Element[] findElementsByNameAndAttribute(Node root, String tagName, String attribName, String keyword, boolean findFirst) {
		List<Element> result = new ArrayList<Element>();
		List<Element> es;
		if (root instanceof Document) {
			es = toElementList(((Document) root).getElementsByTagName(tagName));
		} else if (root instanceof Element) {
			es = toElementList(((Element) root).getElementsByTagName(tagName));
		} else if (root instanceof DocumentFragment) {
			Element eRoot = (Element) first(root, Node.ELEMENT_NODE);
			es = toElementList(eRoot.getElementsByTagName(tagName));
			if (eRoot.getNodeName().equals(tagName))
				es.add(eRoot);
		} else {
			throw new UnsupportedOperationException(root + " is a unknow Node type to find");
		}
		for (Element e : es) {
			String s = attrib(e, attribName);
			if (s != null && s.equals(keyword)) {
				result.add(e);
				if (findFirst)
					break;
			}
		}
		return result.toArray(new Element[result.size()]);
	}

	/**
	 * 查找第一个属性为某个值的Element节点并返回
	 * 
	 * @param node
	 * @param attribName
	 * @param keyword
	 * @return 符合条件的第一个Element
	 */
	public static Element findElementByAttribute(Node node, String attribName, String keyword) {
		Element[] result = findElementsByAttribute(node, attribName, keyword, true);
		if (result.length == 0)
			return null;
		return result[0];
	}

	/**
	 * 查找Element,其拥有某个指定的属性值。
	 * 
	 * @param node
	 * @param attribName
	 * @param keyword
	 * @return
	 */
	public static Element[] findElementsByAttribute(Node node, String attribName, String keyword) {
		return findElementsByAttribute(node, attribName, keyword, false);
	}

	private static Element[] findElementsByAttribute(Node node, String attribName, String keyword, boolean findFirst) {
		List<Element> result = new ArrayList<Element>();
		innerSearchByAttribute(node, attribName, keyword, result, findFirst);
		return result.toArray(new Element[0]);
	}

	/**
	 * 根据attrib属性id定位节点，功能类似于JS中的document.getElementById();
	 * 
	 * @param node
	 * @param tagName
	 * @return
	 */
	public static Element findElementById(Node node, String id) {
		if (node == null)
			return null;
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element e = (Element) node;
			if (e.hasAttribute("id")) {
				String ss = StringUtils.trim(e.getAttribute("id"));
				if (ss.equals(id)) {
					return e;
				}
			}
		}
		for (Node sub : toArray(node.getChildNodes())) {
			Element nd = findElementById(sub, id);
			if (nd != null)
				return nd;
		}
		return null;
	}

	/**
	 * 逐级向上查找父节点，返回第一个符合指定的tagName的Element
	 * 
	 * @param node
	 * @param tagName
	 *            要匹配的元素名称，为空表示无限制
	 * @return
	 */
	public static Element firstParent(Node node, String tagName) {
		if (StringUtils.isEmpty(tagName))
			return (Element) node.getParentNode();
		Node p = node.getParentNode();
		while (p != null) {
			if (p.getNodeType() == Node.ELEMENT_NODE && p.getNodeName().equals(tagName)) {
				return (Element) p;
			}
			p = p.getParentNode();
		}
		return null;
	}

	/**
	 * 向后查找兄弟节点
	 * 
	 * @param node
	 * @param tagName
	 *            匹配的元素名称，为空表示无限制
	 * @return
	 */
	public static Element firstSibling(Node node, String tagName) {
		Node p = node.getNextSibling();
		while (p != null) {
			if (p.getNodeType() == Node.ELEMENT_NODE) {
				if (StringUtils.isEmpty(tagName) || p.getNodeName().equals(tagName))
					return (Element) p;
			}
			p = p.getNextSibling();
		}
		return null;
	}

	/**
	 * 向前查找符合条件的兄弟节点
	 * 
	 * @param node
	 * @param tagName
	 *            匹配的元素名称，为空表示无限制
	 * @return
	 */
	public static Element firstPrevSibling(Node node, String tagName) {
		Node p = node.getPreviousSibling();
		while (p != null) {
			if (p.getNodeType() == Node.ELEMENT_NODE) {
				if (StringUtils.isEmpty(tagName) || p.getNodeName().equals(tagName))
					return (Element) p;
			}
			p = p.getPreviousSibling();
		}
		return null;
	}

	/**
	 * 得到指定节点的Xpath
	 * 
	 * @param node
	 * @return
	 */
	public static String getXPath(Node node) {
		String path = "";
		if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			path = "@" + node.getNodeName();
			node = ((Attr) node).getOwnerElement();
		}
		while (node != null) {
			int index = getIndexOfNode(node);
			String tmp = "/" + ((index > 1) ? node.getNodeName() + "[" + index + "]" : node.getNodeName());
			path = tmp + path;
			node = node.getParentNode();
		}
		return path;
	}

	private static int getIndexOfNode(Node node) {
		if (node.getParentNode() == null)
			return 0;
		int count = 0;
		for (Node e : toArray(node.getParentNode().getChildNodes())) {
			if (e.getNodeName().equals(node.getNodeName())) {
				count++;
				if (e == node)
					return count;
			}
		}
		throw new RuntimeException("Cann't locate the node's index of its parent.");
	}

	/**
	 * 过滤xml的无效字符。
	 * <p/>
	 * XML中出现以下字符就是无效的，此时Parser会抛出异常，仅仅因为个别字符导致整个文档无法解析，是不是小题大作了点？
	 * 为此编写了这个类来过滤输入流中的非法字符。
	 * 不过这个类的实现不够好，性能比起原来的Reader实现和nio的StreamReader下降明显，尤其是read(char[] b, int
	 * off, int len)方法. 如果不需要由XmlFixedReader带来的容错性，还是不要用这个类的好。
	 * <ol>
	 * <li>0x00 - 0x08</li>
	 * <li>0x0b - 0x0c</li>
	 * <li>0x0e - 0x1f</li>
	 * </ol>
	 */
	public static class XmlFixedReader extends FilterReader {
		public XmlFixedReader(Reader reader) {
			super(new BufferedReader(reader));
		}

		public int read() throws IOException {
			int ch = super.read();
			while ((ch >= 0x00 && ch <= 0x08) || (ch >= 0x0b && ch <= 0x0c) || (ch >= 0x0e && ch <= 0x1f) || ch == 0xFEFF) {
				ch = super.read();
			}
			return ch;
		}

		// 最大的问题就是这个方法，一次读取一个字符速度受影响。

		public int read(char[] b, int off, int len) throws IOException {
			if (b == null) {
				throw new NullPointerException();
			} else if (off < 0 || len < 0 || len > b.length - off) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return 0;
			}
			int c = read();
			if (c == -1) {
				return -1;
			}
			b[off] = (char) c;
			int i = 1;
			try {
				for (; i < len; i++) {
					c = read();
					if (c == -1) {
						break;
					}
					b[off + i] = (char) c;
				}
			} catch (IOException ee) {
			}
			return i;
		}
	}

	private static final String[] XPATH_KEYS = { "//", "@", "/" };

	/**
	 * W3C Xpath的语法规范功能很强，但是性能不佳，许多时候我只需要用一个表达式定位节点即可，不需要复杂考虑的逻辑运算、命名空间等问题。
	 * JEF在xpath规范的基础上，从新提炼了一个简化版的Xpath规则，参见:{@link XPathFunction}
	 * 
	 * <p>
	 * 提供了若干方法用于快速计算XPath表达式。
	 * </p>
	 * 
	 * @param node
	 * @param xPath
	 *            jef-xpath表达式
	 * @return 计算后的属性值，多值文本列表
	 */
	@SuppressWarnings("unchecked")
	public static String[] getAttributesByXPath(Node node, String xPath) {
		Object re = getByXPath(node, xPath, false);
		if (re instanceof String)
			return new String[] { (String) re };
		if (re instanceof List)
			return ((List<String>) re).toArray(ArrayUtils.EMPTY_STRING_ARRAY);
		if (re instanceof NodeList) {
			if (((NodeList) re).getLength() == 0) {
				return ArrayUtils.EMPTY_STRING_ARRAY;
			}
		}
		throw new IllegalArgumentException("Can not return Attribute, Xpath expression[" + xPath + "] result is a " + re.getClass().getSimpleName());
	}

	/**
	 * W3C Xpath的语法规范功能很强，但是性能不佳，许多时候我只需要用一个表达式定位节点即可，不需要复杂考虑的逻辑运算、命名空间等问题。
	 * JEF在xpath规范的基础上，从新提炼了一个简化版的Xpath规则，参见:{@link XPathFunction}
	 * 
	 */
	public static String getAttributeByXPath(Node node, String xPath) {
		String[] re = getAttributesByXPath(node, xPath);
		if (re.length > 0)
			return re[0];
		throw new IllegalArgumentException("No proper attribute matchs. can not return Attribute.");
	}

	/**
	 * W3C Xpath的语法规范功能很强，但是性能不佳，许多时候我只需要用一个表达式定位节点即可，不需要复杂考虑的逻辑运算、命名空间等问题。
	 * JEF在xpath规范的基础上，从新提炼了一个简化版的Xpath规则，参见:{@link XPathFunction}
	 * 
	 * 注意，这个方法的目的是在文档中定位，当运算中间结果出现多个元素时，会自动取第一个元素而不抛出异常。 下面提供了三个方法
	 * getAttributeByXPath / getNodeByXPath / getNodeListByXPath 用于快速计算XPath表达式。
	 * 
	 * @param node
	 * @param xPath
	 *            简易版Xpath表达式
	 * @return 计算后的节点
	 */
	public static Node getNodeByXPath(Node node, String xPath) {
		try {
			Object re = getByXPath(node, xPath, false);
			if (re instanceof Node)
				return (Node) re;
			if (re instanceof NodeList) {
				NodeList l = ((NodeList) re);
				if (l.getLength() == 0)
					return null;
				return l.item(0);
			}
			throw new IllegalArgumentException("Can not return node, Xpath [" + xPath + "] result is a " + re.getClass().getSimpleName());
		} catch (NullPointerException e) {
			try {
				File file = new File("c:/dump" + StringUtils.getTimeStamp() + ".xml");
				FileOutputStream out = new FileOutputStream(file);
				printNode(node, out);
				out.write(("======\nXPATH:" + xPath).getBytes());
				LogUtil.show("Xpath error, dump file is:" + file.getAbsolutePath());
				IOUtils.closeQuietly(out);
			} catch (Exception e1) {
				LogUtil.exception(e1);
			}
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * 无视层级，获得所有指定Tagname的element节点
	 * 
	 * @param node
	 * @param tagName
	 * @return
	 */
	public static List<Element> getElementsByTagNames(Node node, String... tagName) {
		List<Element> nds = new ArrayList<Element>();
		if (tagName.length == 0)
			tagName = new String[] { "" };
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element doc = (Element) node;
			for (String elementName : tagName) {
				nds.addAll(toElementList(doc.getElementsByTagName(elementName)));
			}
		} else if (node instanceof Document) {
			Document doc = ((Document) node);
			for (String elementName : tagName) {
				nds.addAll(toElementList(doc.getElementsByTagName(elementName)));
			}
		} else if (node instanceof DocumentFragment) {
			Document doc = ((DocumentFragment) node).getOwnerDocument();
			for (String elementName : tagName) {
				nds.addAll(toElementList(doc.getElementsByTagName(elementName)));
			}
		} else {
			throw new IllegalArgumentException("a node who doesn't support getElementsByTagName operation.");
		}
		return nds;
	}

	/**
	 * JEF Enhanced Xpath <li>// 表示当前节点下多层</li> <li>/ 当前节点下一层</li> <li>../ 上一层</li>
	 * <li>@ 取属性</li> <li>@#text 取节点文本</li> <li>| 允许选择多个不同名称的节点，用|分隔</li> <li>
	 * [n] 选择器：返回第n个</li> <li>[-2] 选择器：倒数第二个</li> <li>[2--2] 选择器：从第2个到倒数第2个</li>
	 * <li>[?] 选择器：返回所有</li> <li>/count: 函数,用于计算节点的数量</li> <li>/plain:
	 * 函数,获得节点内下属结点转换而成文本(含节点本身)</li> <li>/childrenplain:
	 * 函数,节点本身和下属结点转换而成文本(不含节点本身)</li> <li>/text:
	 * 函数,节点下的所有文本节点输出，如果碰到HTML标签作一定的处理</li> <li>/find:<code>str</code>
	 * 函数,自动查找id=str的节点</li> <li>/findby:<code>name:value</code>
	 * 函数,自动查找属性名城和属性值匹配的节点</li> <li>/parent:<code>str</code>
	 * 函数,向上级查找节点指定名称的父节点，如不指定，则等效于../</li> <li>/parent:<code>str</code>
	 * 函数,向上级查找节点指定名称的父节点，如不指定，则等效于../</li> <li>/next:<code>str</code> 函数,
	 * 在平级向后查找指定名称的兄弟节点，如不指定，则取后第一个兄弟节点</li> <li>/prev:<code>str</code> 函数,
	 * 在平级向前查找指定名称的兄弟节点，如不指定，则取前第一个兄弟节点</li>
	 */
	public static enum XPathFunction {
		COUNT, // 用于计算节点的数量
		PLAIN, // 用于获得节点内下属结点转换而成文本(含节点本身)
		CHILDRENPLAIN, // 用于获得节点本身和下属结点转换而成文本(不含节点本身)
		TEXT, // 将节点下的所有文本节点输出，如果碰到HTML标签作一定的处理
		FIND, FINDBY, PARENT, NEXT, PREV
	}

	/**
	 * W3C Xpath的语法规范功能很强，但是性能不佳，许多时候我只需要用一个表达式定位节点即可，不需要复杂考虑的逻辑运算、命名空间等问题。
	 * JEF在xpath规范的基础上，从新提炼了一个简化版的Xpath规则，参见:{@link XPathFunction}
	 * 
	 * @param node
	 * @param xPath
	 *            简易版Xpath表达式
	 * @return 计算后的NodeList
	 */
	public static NodeList getNodeListByXPath(Node node, String xPath) {
		Object re = getByXPath(node, xPath, false);
		if (re instanceof NodeList)
			return (NodeList) re;
		throw new IllegalArgumentException("Can not return NodeList, the result type of xpath[" + xPath + "] is " + re.getClass().getSimpleName());
	}

	static final int INDEX_DEFAULT = -999;
	static final int INDEX_RANGE = -1000;

	// 1号解析函数，处理单个节点的运算
	// 解析并获取Xpath的对象，这个方法不够安全，限制类内部使用
	// 返回以下对象
	// String / Node /NodeList/ List<String>
	private static Object getByXPath(Node node, String xPath, boolean allowNull) {
		if (StringUtils.isEmpty(xPath))
			return node;
		Node curNode = node;
		XPathFunction function = null;
		for (Iterator<Substring> iter = new SubstringIterator(new Substring(xPath), XPATH_KEYS, true); iter.hasNext();) {
			Substring str = iter.next();
			if (str.isEmpty())
				continue;

			if (str.startsWith("/count:")) {
				Assert.isNull(function);
				function = XPathFunction.COUNT;
				continue;
			} else if (str.startsWith("/plain:")) {
				Assert.isNull(function);
				function = XPathFunction.PLAIN;
				continue;
			} else if (str.startsWith("/childrenplain:")) {
				Assert.isNull(function);
				function = XPathFunction.CHILDRENPLAIN;
				continue;
			} else if (str.startsWith("/text:")) {
				Assert.isNull(function);
				function = XPathFunction.TEXT;
				continue;
			} else if (str.startsWith("/find:")) {
				str = StringUtils.stringRight(str.toString(), "find:", false);
				Element newNode = findElementById(curNode, str.toString());
				if (newNode == null) {
					throw new IllegalArgumentException("There's no element with id=" + str + " under xpath " + getXPath(curNode));
				}
				curNode = newNode;
				continue;
			} else if (str.startsWith("/findby:")) {
				str = StringUtils.stringRight(str.toString(), "findby:", false);
				String[] args = StringUtils.split(str.toString(), ':');
				Assert.isTrue(args.length == 2, "findby function must have to args, divide with ':'");
				Element[] newNodes = findElementsByAttribute(curNode, args[0], args[1]);
				if (newNodes.length == 0) {
					throw new IllegalArgumentException("There's no element with attrib is " + str + " under xpath " + getXPath(curNode));
				} else if (newNodes.length == 1) {
					curNode = newNodes[0];
					continue;
				} else {
					return withFunction(getByXPath(toNodeList(newNodes), iter), function);// 按照NodeList继续进行计算
				}
			} else if (str.startsWith("/parent:")) {
				str = StringUtils.stringRight(str.toString(), "parent:", false);
				Element newNode = firstParent(curNode, str.toString());
				if (newNode == null) {
					throw new IllegalArgumentException("There's no element with name=" + str + " under xpath " + getXPath(curNode));
				}
				curNode = newNode;
				continue;
			} else if (str.startsWith("/next:")) {
				str = StringUtils.stringRight(str.toString(), "next:", false);
				Element newNode = firstSibling(curNode, str.toString());
				if (newNode == null) {
					throw new IllegalArgumentException("There's no element with name=" + str + " under xpath " + getXPath(curNode));
				}
				curNode = newNode;
				continue;
			} else if (str.startsWith("/prev:")) {
				str = StringUtils.stringRight(str.toString(), "prev:", false);
				Element newNode = firstPrevSibling(curNode, str.toString());
				if (newNode == null) {
					throw new IllegalArgumentException("There's no element with name=" + str + " under xpath " + getXPath(curNode));
				}
				curNode = newNode;
				continue;
			} else if (str.equals("/.")) {
				continue;
			} else if (str.equals("/..")) {
				curNode = (Element) curNode.getParentNode();
				continue;
			}
			String elementName = null;
			String index = null;
			boolean isWild = false;
			if (str.startsWith("//")) {
				StringSpliterEx sp = new StringSpliterEx(str.sub(2, str.length()));
				if (sp.setKeys("[", "]") == StringSpliterEx.RESULT_BOTH_KEY) {
					elementName = sp.getLeft().toString();
					index = sp.getMiddle().toString();
				} else {
					elementName = sp.getSource().toString();
				}
				isWild = true;
			} else if (str.startsWith("/")) {
				StringSpliterEx sp = new StringSpliterEx(str.sub(1, str.length()));
				if (sp.setKeys("[", "]") == StringSpliterEx.RESULT_BOTH_KEY) {
					elementName = sp.getLeft().toString();
					index = sp.getMiddle().toString();
				} else {
					elementName = sp.getSource().toString();
				}
			} else if (str.startsWith("@")) {
				String attribName = str.sub(1, str.length()).toString();
				String value = null;
				if (attribName.equals("#text")) {
					value = nodeText(curNode);
				} else if (attribName.equals("#alltext")) {
					value = nodeText(curNode, true);
				} else {
					Element el = null;
					if (curNode instanceof Document) {
						el = ((Document) curNode).getDocumentElement();
					} else {
						el = (Element) curNode;
					}
					if (str.siblingLeft().equals("//")) {
						return attribs(el, attribName);
					}
					value = attrib(el, attribName);
				}
				if (value == null)
					value = "";
				if (iter.hasNext())
					throw new IllegalArgumentException("Xpath invalid, there's no attributes after.");
				return withFunction(value, function); // 返回节点内容
			} else {
				StringSpliterEx sp = new StringSpliterEx(str);
				if (sp.setKeys("[", "]") == StringSpliterEx.RESULT_BOTH_KEY) {
					elementName = sp.getLeft().toString();
					index = sp.getMiddle().toString();
				} else {
					elementName = sp.getSource().toString();
				}
			}
			NodeList nds = null;
			int i;
			if ("?".equals(index)) {
				i = INDEX_DEFAULT;
			} else if (index != null && index.lastIndexOf("-") > 0) {// 指定的是一个Index范围
				i = INDEX_RANGE;
			} else {
				i = StringUtils.toInt(index, 1);
			}

			if (StringUtils.isNotEmpty(elementName)) {
				if (isWild) {
					nds = toNodeList(getElementsByTagNames(curNode, StringUtils.split(elementName, '|')));
				} else {
					nds = toNodeList(childElements(curNode, StringUtils.split(elementName, '|')));
				}
				if ((!iter.hasNext() && index == null))
					i = INDEX_DEFAULT;// 没有下一个并且没有显式指定序号

				if (i == INDEX_DEFAULT) {// && nds.getLength()!=1
					return withFunction(getByXPath(nds, iter), function);// 按照NodeList继续进行计算
				} else if (i == INDEX_RANGE) { // 指定序号范围
					Node[] nArray = toArray(nds);
					int x = index.indexOf("--");
					if (x < 0)
						x = index.lastIndexOf('-');
					int iS = StringUtils.toInt(index.substring(0, x), 1);
					if (iS < 0)
						iS += nds.getLength() + 1;
					int iE = StringUtils.toInt(index.substring(x + 1), nArray.length);
					if (iE < 0)
						iE += nds.getLength() + 1;
					nds = toNodeList(ArrayUtils.subArray(nArray, iS - 1, iE));
					return withFunction(getByXPath(nds, iter), function);// 按照NodeList继续进行计算
				} else if (i < 0) {// 倒数第i个节点
					if (nds.getLength() < Math.abs(i)) {
						if (allowNull)
							return null;
						throw new NoSuchElementException("Node not found:" + getXPath(curNode) + " " + str + "the parent nodelist has " + nds.getLength() + " elements, but index is " + i);
					} else {
						curNode = (Element) nds.item(nds.getLength() + i);
					}
				} else {// 正数第i个节点
					if (nds.getLength() < i) {
						if (allowNull)
							return null;
						throw new NoSuchElementException("Node not found:" + getXPath(curNode) + " /" + elementName + " element.[" + str + "] the nodelist has " + nds.getLength() + " elements, but index is " + i);
					} else {
						curNode = (Element) nds.item(i - 1);
					}
				}
			} else {// 无视节点名称

			}
		}
		return withFunction(curNode, function);
	}

	private static enum Tee {
		StringList, NodeList, Node, String
	}

	// 2号解析函数，处理节点集的下一步运算，合并结果集
	@SuppressWarnings("unchecked")
	private static Object getByXPath(NodeList nds, Iterator<Substring> iter) {
		for (; iter.hasNext();) {
			List<Node> nlist = new ArrayList<Node>();
			List<String> slist = new ArrayList<String>();
			Tee type = null;
			String xpath = iter.next().toString();
			if (xpath.indexOf(':') > -1) {// 如果是函数，就将剩下的字串全部交给1号解析函数处理
				for (; iter.hasNext();) {
					xpath += iter.next();
				}
			}
			if (StringUtils.isEmpty(xpath))
				continue;

			for (Node node : toArray(nds)) {
				if (node.getNodeType() != Node.ELEMENT_NODE) {
					throw new UnsupportedOperationException("Unsupport node type:" + node.getNodeType());
				}
				Object obj = getByXPath(node, xpath, true);
				if (obj == null) {
					// skip it;
				} else if (obj instanceof List) {
					if (type == null) {
						type = Tee.StringList;
					} else if (type != Tee.StringList) {
						throw new UnsupportedOperationException();
					}
					slist.addAll((List<String>) obj);
				} else if (obj instanceof Node) {
					if (type == null) {
						type = Tee.Node;
					} else if (type != Tee.Node) {
						throw new UnsupportedOperationException("old type is " + type.name());
					}
					nlist.add((Node) obj);
				} else if (obj instanceof NodeList) {
					if (type == null) {
						type = Tee.NodeList;
					} else if (type != Tee.NodeList) {
						throw new UnsupportedOperationException();
					}
					nlist.addAll(toList((NodeList) obj));
				} else if (obj instanceof String) {
					if (type == null) {
						type = Tee.String;
					} else if (type != Tee.String) {
						throw new UnsupportedOperationException();
					}
					slist.add((String) obj);
				} else {
					throw new UnsupportedOperationException();
				}
			}
			if (type == Tee.String || type == Tee.StringList) {
				return slist;
			} else if (type == Tee.Node || type == Tee.NodeList) {
				nds = toNodeList(nlist);
			}
		}
		return nds;
	}

	@SuppressWarnings({ "rawtypes" })
	private static Object withFunction(Object obj, XPathFunction function) {
		if (function == null)
			return obj;
		if (function == XPathFunction.COUNT) {
			if (obj instanceof NodeList) {
				return String.valueOf(((NodeList) obj).getLength());
			} else if (obj instanceof NamedNodeMap) {
				return String.valueOf(((NamedNodeMap) obj).getLength());
			} else if (obj instanceof List) {
				return String.valueOf(((List) obj).size());
			} else {
				throw new IllegalArgumentException();
			}
		} else if (function == XPathFunction.PLAIN) {
			return toPlainText(obj, true);
		} else if (function == XPathFunction.CHILDRENPLAIN) {
			return toPlainText(obj, false);
		} else if (function == XPathFunction.TEXT) {
			if (obj instanceof Node) {
				return htmlNodeToString((Node) obj, true);
			} else if (obj instanceof NodeList) {
				StringBuilder sb = new StringBuilder();
				Node[] list = toArray((NodeList) obj);
				for (int i = 0; i < list.length; i++) {
					Node node = list[i];
					if (i > 0)
						sb.append("\n");
					sb.append(htmlNodeToString(node, true));
				}
				return sb.toString();
			} else if (obj instanceof List) {
				StringBuilder sb = new StringBuilder();
				for (Object o : (List) obj) {
					if (sb.length() > 0)
						sb.append(",");
					sb.append(o.toString());
				}
				return sb.toString();
			} else {
				return obj.toString();
			}
		}
		return obj;
	}

	@SuppressWarnings("rawtypes")
	private static String toPlainText(Object obj, boolean includeMe) {
		if (obj instanceof Node) {
			if (includeMe) {
				return toString((Node) obj);
			} else {
				StringBuilder sb = new StringBuilder();
				for (Node node : toArray(((Node) obj).getChildNodes())) {
					sb.append(toString(node));
				}
				return sb.toString();
			}
		} else if (obj instanceof NodeList) {
			StringBuilder sb = new StringBuilder();
			for (Node node : toArray((NodeList) obj)) {
				sb.append(toPlainText(node, includeMe));
			}
			return sb.toString();
		} else if (obj instanceof List) {
			StringBuilder sb = new StringBuilder();
			for (Object o : (List) obj) {
				if (sb.length() > 0)
					sb.append(",");
				sb.append(o.toString());
			}
			return sb.toString();
		} else {
			return obj.toString();
		}
	}

	/**
	 * 将Node打印到输出流
	 * 
	 * @param node
	 * @param out
	 * @throws IOException
	 */
	public static void printNode(Node node, OutputStream out) throws IOException {
		output(node, out, null, true, null);
	}

	/**
	 * 控制台打印出节点和其下属的内容，在解析和调试DOM时很有用。
	 * 
	 * @param node
	 *            要打印的节点
	 * @param maxLevel
	 *            打印几层
	 */
	public static void printChilrenNodes(Node node, int maxLevel, boolean attFlag) {
		if (maxLevel < 0)
			maxLevel = Integer.MAX_VALUE;
		printChilrenNodes(node, 0, maxLevel, attFlag);
	}

	private static void printChilrenNodes(Node parentNode, int level, int maxLevel, boolean attFlag) {
		for (Node node : toArray(parentNode.getChildNodes())) {
			if (node.getNodeType() == Node.TEXT_NODE) {
				continue;
			}
			String span = StringUtils.repeat("   ", level);
			StringBuilder sb = new StringBuilder();
			sb.append(span);
			sb.append("<").append(node.getNodeName());

			if (attFlag && node.getAttributes() != null) {// 打印属性
				Node[] atts = toArray(node.getAttributes());
				for (Node att : atts) {
					sb.append(" ");
					sb.append(att.getNodeName() + "=\"" + att.getNodeValue() + "\"");
				}
			}
			if (node.hasChildNodes()) {
				sb.append(">");
				if (node.getChildNodes().getLength() == 1 && node.getFirstChild().getNodeType() == Node.TEXT_NODE) {
					sb.append(node.getFirstChild().getNodeValue().trim());
					sb.append("</" + node.getNodeName() + ">");
					LogUtil.show(sb.toString());
				} else {
					LogUtil.show(sb.toString());
					if (maxLevel > level) {
						printChilrenNodes(node, level + 1, maxLevel, attFlag);
					}
					LogUtil.show(span + "</" + node.getNodeName() + ">");
				}
			} else {
				sb.append("/>");
				LogUtil.show(sb.toString());
			}
		}
	}

	/**
	 * @param node
	 * @param charset
	 *            字符集，该属性只影响xml头部的声明，由于返回的string仍然是标准的unicode string，
	 *            你必须注意在输出时指定编码和此处的编码一致.
	 * @param xmlHeader
	 *            是否要携带XML头部标签<?xml ....>
	 * @return
	 */
	public static String toString(Node node, String charset, Boolean xmlHeader) {
		StringWriter sw = new StringWriter(4096);
		StreamResult sr = new StreamResult(sw);
		try {
			output(node, sr, charset, true, xmlHeader);
		} catch (IOException e) {
			LogUtil.exception(e);
		}
		return sw.toString();
	}

	/**
	 * 将DOM节点还原为XML片段文本
	 * 
	 * @param node
	 * @param charset
	 *            字符集，该属性只影响xml头部的声明，由于返回的string仍然是标准的unicode string，
	 *            你必须注意在输出时指定编码和此处的编码一致.
	 * @return
	 */
	public static String toString(Node node, String charset) {
		return toString(node, charset, null);
	}

	/**
	 * 将一个HTML节点内转换成格式文本
	 * 
	 * @param node
	 * @param keepenter
	 *            是否保留原来文字当中的换行符
	 * @return
	 */
	public static String htmlNodeToString(Node node, boolean... keepenter) {
		boolean keepEnter = (keepenter.length == 0 || keepenter[0] == true);
		if (node.getNodeType() == Node.TEXT_NODE) {
			if (keepEnter) {
				return node.getTextContent();
			} else {
				String str = node.getTextContent();
				str = StringUtils.remove(str, '\t');
				str = StringUtils.remove(str, '\n');
				return str;
			}
		} else {
			StringBuilder sb = new StringBuilder();
			if ("BR".equals(node.getNodeName()) || "TR".equals(node.getNodeName()) || "P".equals(node.getNodeName())) {
				// if (keepEnter) {
				sb.append("\n");
				// }
			} else if ("TD".equals(node.getNodeName())) {
				// if (keepEnter) {
				sb.append("\t");
				// }
			} else if ("IMG".equals(node.getNodeName())) {
				sb.append("[img]").append(attrib((Element) node, "src")).append("[img]");
			}
			for (Node child : toArray(node.getChildNodes())) {
				sb.append(htmlNodeToString(child, keepenter));
			}
			return sb.toString();
		}
	}
}
