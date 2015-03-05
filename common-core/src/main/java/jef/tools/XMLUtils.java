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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.html.dom.HTMLDocumentImpl;
import org.easyframe.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 使用JAXP，封装了基于XML的各种基本操作
 * 
 * 
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
	private static final Logger log = LoggerFactory.getLogger("XMLUtils");

	/**
	 * 缓存的DocumentBuilderFactory<br>
	 * 每个DocumentBuilderFactory构造开销在0.3ms左右，缓存很有必要
	 */
	private static DocumentBuilderFactory domFactoryTT;
	private static DocumentBuilderFactory domFactoryTF;
	private static DocumentBuilderFactory domFactoryFT;
	private static DocumentBuilderFactory domFactoryFF;

	/**
	 * 初始化各类解析器，分析当前运行环境
	 */
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
		try {
			domFactoryTT = initFactory(true, true);
			domFactoryTF = initFactory(true, false);
			domFactoryFT = initFactory(false, true);
			domFactoryFF = initFactory(false, false);
		} catch (Exception e) {
			log.error("FATAL: Error in init DocumentBuilderFactory. XML Parser will not work!", e);
		}
	}

	/*
	 * 创建解析器工厂
	 * 
	 * @param ignorComments 忽略注释
	 * 
	 * @param namespaceAware 识别命名空间
	 * 
	 * @return DocumentBuilderFactoy
	 */
	private static DocumentBuilderFactory initFactory(boolean ignorComments, boolean namespaceAware) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setValidating(false); // 关闭DTD校验
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
			log.warn("Your xerces implemention is too old to support 'load-dtd-grammar' and 'load-external-dtd' feature. Please upgrade xercesImpl.jar to 2.6.2 or above.");
		} catch (AbstractMethodError e) {
			log.warn("Your xerces implemention is too old to support 'load-dtd-grammar' and 'load-external-dtd' feature. Please upgrade xercesImpl.jar to 2.6.2 or above.");
		}

		try {
			dbf.setAttribute("http://xml.org/sax/features/external-general-entities", false);
		} catch (IllegalArgumentException e) {
			log.warn("Your xerces implemention is too old to support 'external-general-entities' attribute.");
		}
		try {
			dbf.setAttribute("http://xml.org/sax/features/external-parameter-entities", false);
		} catch (IllegalArgumentException e) {
			log.warn("Your xerces implemention is too old to support 'external-parameter-entities' attribute.");
		}
		try {
			dbf.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (IllegalArgumentException e) {
			log.warn("Your xerces implemention is too old to support 'load-external-dtd' attribute.");
		}
		return dbf;
	}

	// 内部匿名类，ErrorHandler
	private static final ErrorHandler EH = new ErrorHandler() {
		public void error(SAXParseException x) throws SAXException {
			throw x;
		}

		public void fatalError(SAXParseException x) throws SAXException {
			throw x;
		}

		public void warning(SAXParseException x) throws SAXException {
			log.warn("SAXParserWarnning:", x);
		}
	};

	/**
	 * 内部匿名类，DTD解析器。优先寻找本地classpath下的DTD资源，然后才考虑通过网络连接获取DTD
	 */
	private static final EntityResolver ER = new EntityResolver() {
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			if (systemId != null && systemId.endsWith(".dtd")) {
				URL url = new URL(systemId);
				String file = StringUtils.substringAfterLastIfExist(url.getFile(), "/");
				URL u = this.getClass().getClassLoader().getResource(file);
				if (u == null) {
					u = url;
				}
				InputSource source = new InputSource(u.openStream());
				source.setPublicId(publicId);
				source.setSystemId(systemId);
				return source;
			}
			return null;
		}
	};

	/**
	 * 缓存的DocumentBuilderCache<br>
	 * 每个DocumentBuilder的构造开销在0.4ms左右，缓存很有必要
	 */
	private static final ThreadLocal<DocumentBuilderCache> REUSABLE_BUILDER = new ThreadLocal<DocumentBuilderCache>() {
		@Override
		protected DocumentBuilderCache initialValue() {
			return new DocumentBuilderCache();
		}
	};

	/**
	 * 缓存DocumentBuilder的容器
	 * 
	 * @author jiyi
	 * 
	 */
	private final static class DocumentBuilderCache {
		DocumentBuilder cacheTT;
		DocumentBuilder cacheTF;
		DocumentBuilder cacheFT;
		DocumentBuilder cacheFF;

		/**
		 * 根据传入的特性，提供满足条件的DocumentBuilder
		 * 
		 * @param ignorComments
		 * @param namespaceAware
		 * @return
		 */
		public DocumentBuilder getDocumentBuilder(boolean ignorComments, boolean namespaceAware) {
			if (ignorComments && namespaceAware) {
				if (cacheTT == null) {
					cacheTT = initBuilder(domFactoryTT);
				}
				return cacheTT;
			} else if (ignorComments) {
				if (cacheTF == null) {
					cacheTF = initBuilder(domFactoryTF);
				}
				return cacheTF;
			} else if (namespaceAware) {
				if (cacheFT == null) {
					cacheFT = initBuilder(domFactoryFT);
				}
				return cacheFT;
			} else {
				if (cacheFF == null) {
					cacheFF = initBuilder(domFactoryFF);
				}
				return cacheFF;
			}
		}

		private DocumentBuilder initBuilder(DocumentBuilderFactory domFactory) {
			DocumentBuilder builder;
			try {
				builder = domFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new UnsupportedOperationException(e);
			}
			builder.setErrorHandler(EH);
			builder.setEntityResolver(ER);
			return builder;
		}
	}

	/**
	 * Xpath解析器
	 */
	private static XPathFactory xp = XPathFactory.newInstance();

	/**
	 * HTML解析器
	 */
	private static jef.tools.IDOMFragmentParser parser;

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
	 *             解析错误
	 * @throws IOException
	 *             磁盘操作错误
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
		try {
			DocumentBuilder db = getDocumentBuilder(ignorComments, namespaceAware);
			InputSource is = new InputSource(reader);
			Document doc = db.parse(is);
			return doc;
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	/**
	 * 从当前ThreadLocal中获得缓存的DocumentBuilder
	 * @param ignorComments
	 * @param namespaceAware
	 * @return
	 */
	private static DocumentBuilder getDocumentBuilder(boolean ignorComments, boolean namespaceAware) {
		return REUSABLE_BUILDER.get().getDocumentBuilder(ignorComments, namespaceAware);
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
	public static Document parse(String xmlContent) throws SAXException, IOException {
		Reader reader = null;
		try {
			reader = new StringReader(xmlContent);
			return loadDocument(reader, true, false);
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
	 * @deprecated use #
	 */
	public static Document loadDocumentByString(String xmlContent) throws SAXException, IOException {
		return parse(xmlContent);
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
		DocumentBuilder db = getDocumentBuilder(ignorComment, namespaceaware);
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
	public static DocumentFragment parseHTML(Reader in) throws SAXException, IOException {
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
	public static DocumentFragment parseHTML(File file) throws IOException, SAXException {
		InputStream in = IOUtils.getInputStream(file);
		try {
			DocumentFragment document = parseHTML(in, null);
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
	public static DocumentFragment parseHTML(URL url) throws SAXException, IOException {
		return parseHTML(url.openStream(), null);
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
	public static DocumentFragment parseHTML(InputStream in, String charSet) throws SAXException, IOException {
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
		output(node, os, null, 4, null);
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
		output(node, os, encoding, 4, null);
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
	public static void output(Node node, OutputStream os, String encoding, int warpLine, Boolean xmlDeclare) throws IOException {
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
	public static void output(Node node, Writer os, String encoding, int indent) throws IOException {
		StreamResult sr = new StreamResult(os);
		output(node, sr, encoding, indent, null);
	}

	private static void output(Node node, StreamResult sr, String encoding, int indent, Boolean XmlDeclarion) throws IOException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = null;

		try {
			if (indent > 0) {
				try {
					tf.setAttribute("indent-number", indent);
					t = tf.newTransformer();
					// 某些垃圾的XML解析包会造成无法正确的设置属性。一些旧版本的XML解析器会有此问题
					t.setOutputProperty(OutputKeys.INDENT, "yes");
				} catch (Exception e) {
				}
			} else {
				t = tf.newTransformer();
			}

			t.setOutputProperty(OutputKeys.METHOD, "xml");
			if (encoding != null) {
				t.setOutputProperty(OutputKeys.ENCODING, encoding);
			}
			if (XmlDeclarion == null) {
				XmlDeclarion = (node instanceof Document);
			}
			if (node instanceof Document) {
				Document doc = (Document) node;
				t.setOutputProperty(javax.xml.transform.OutputKeys.DOCTYPE_PUBLIC, doc.getDoctype().getPublicId());
				t.setOutputProperty(javax.xml.transform.OutputKeys.DOCTYPE_SYSTEM, doc.getDoctype().getSystemId());
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
	 * 将Node打印到输出流
	 * 
	 * @param node
	 * @param out
	 * @throws IOException
	 */
	public static void printNode(Node node, OutputStream out) throws IOException {
		output(node, out, null, 4, null);
	}

	/**
	 * 将DOM节点转换为文本
	 * 
	 * @param node
	 *            DOM节点
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
			output(node, sr, charset, 4, xmlHeader);
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
	 * 设置XSD Schema
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

	private static Element[] findElementsByAttribute(Node node, String attribName, String keyword, boolean findFirst) {
		List<Element> result = new ArrayList<Element>();
		innerSearchByAttribute(node, attribName, keyword, result, findFirst);
		return result.toArray(new Element[0]);
	}

}
