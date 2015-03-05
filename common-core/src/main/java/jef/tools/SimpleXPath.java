package jef.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import jef.common.log.LogUtil;
import jef.tools.string.StringSpliterEx;
import jef.tools.string.Substring;
import jef.tools.string.SubstringIterator;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 将原来自己编写Xpath解析等功能移动到这里。
 * 
 * @author jiyi
 * 
 */
public class SimpleXPath {
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
		for (Node node : XMLUtils.toArray(parentNode.getChildNodes())) {
			if (node.getNodeType() == Node.TEXT_NODE) {
				continue;
			}
			String span = StringUtils.repeat("   ", level);
			StringBuilder sb = new StringBuilder();
			sb.append(span);
			sb.append("<").append(node.getNodeName());

			if (attFlag && node.getAttributes() != null) {// 打印属性
				Node[] atts = XMLUtils.toArray(node.getAttributes());
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
				Element newNode = XMLUtils.findElementById(curNode, str.toString());
				if (newNode == null) {
					throw new IllegalArgumentException("There's no element with id=" + str + " under xpath " + getXPath(curNode));
				}
				curNode = newNode;
				continue;
			} else if (str.startsWith("/findby:")) {
				str = StringUtils.stringRight(str.toString(), "findby:", false);
				String[] args = StringUtils.split(str.toString(), ':');
				Assert.isTrue(args.length == 2, "findby function must have to args, divide with ':'");
				Element[] newNodes = XMLUtils.findElementsByAttribute(curNode, args[0], args[1]);
				if (newNodes.length == 0) {
					throw new IllegalArgumentException("There's no element with attrib is " + str + " under xpath " + getXPath(curNode));
				} else if (newNodes.length == 1) {
					curNode = newNodes[0];
					continue;
				} else {
					return withFunction(getByXPath(XMLUtils.toNodeList(newNodes), iter), function);// 按照NodeList继续进行计算
				}
			} else if (str.startsWith("/parent:")) {
				str = StringUtils.stringRight(str.toString(), "parent:", false);
				Element newNode = XMLUtils.firstParent(curNode, str.toString());
				if (newNode == null) {
					throw new IllegalArgumentException("There's no element with name=" + str + " under xpath " + getXPath(curNode));
				}
				curNode = newNode;
				continue;
			} else if (str.startsWith("/next:")) {
				str = StringUtils.stringRight(str.toString(), "next:", false);
				Element newNode = XMLUtils.firstSibling(curNode, str.toString());
				if (newNode == null) {
					throw new IllegalArgumentException("There's no element with name=" + str + " under xpath " + getXPath(curNode));
				}
				curNode = newNode;
				continue;
			} else if (str.startsWith("/prev:")) {
				str = StringUtils.stringRight(str.toString(), "prev:", false);
				Element newNode = XMLUtils.firstPrevSibling(curNode, str.toString());
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
					value = XMLUtils.nodeText(curNode);
				} else if (attribName.equals("#alltext")) {
					value = XMLUtils.nodeText(curNode, true);
				} else {
					Element el = null;
					if (curNode instanceof Document) {
						el = ((Document) curNode).getDocumentElement();
					} else {
						el = (Element) curNode;
					}
					if (str.siblingLeft().equals("//")) {
						return XMLUtils.attribs(el, attribName);
					}
					value = XMLUtils.attrib(el, attribName);
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
					nds = XMLUtils.toNodeList(XMLUtils.getElementsByTagNames(curNode, StringUtils.split(elementName, '|')));
				} else {
					nds = XMLUtils.toNodeList(XMLUtils.childElements(curNode, StringUtils.split(elementName, '|')));
				}
				if ((!iter.hasNext() && index == null))
					i = INDEX_DEFAULT;// 没有下一个并且没有显式指定序号

				if (i == INDEX_DEFAULT) {// && nds.getLength()!=1
					return withFunction(getByXPath(nds, iter), function);// 按照NodeList继续进行计算
				} else if (i == INDEX_RANGE) { // 指定序号范围
					Node[] nArray = XMLUtils.toArray(nds);
					int x = index.indexOf("--");
					if (x < 0)
						x = index.lastIndexOf('-');
					int iS = StringUtils.toInt(index.substring(0, x), 1);
					if (iS < 0)
						iS += nds.getLength() + 1;
					int iE = StringUtils.toInt(index.substring(x + 1), nArray.length);
					if (iE < 0)
						iE += nds.getLength() + 1;
					nds = XMLUtils.toNodeList(ArrayUtils.subArray(nArray, iS - 1, iE));
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
				Node[] list = XMLUtils.toArray((NodeList) obj);
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
				return XMLUtils.toString((Node) obj);
			} else {
				StringBuilder sb = new StringBuilder();
				for (Node node : XMLUtils.toArray(((Node) obj).getChildNodes())) {
					sb.append(XMLUtils.toString(node));
				}
				return sb.toString();
			}
		} else if (obj instanceof NodeList) {
			StringBuilder sb = new StringBuilder();
			for (Node node : XMLUtils.toArray((NodeList) obj)) {
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

			for (Node node : XMLUtils.toArray(nds)) {
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
					nlist.addAll(XMLUtils.toList((NodeList) obj));
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
				nds = XMLUtils.toNodeList(nlist);
			}
		}
		return nds;
	}

	private static enum Tee {
		StringList, NodeList, Node, String
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
				XMLUtils.printNode(node, out);
				out.write(("======\nXPATH:" + xPath).getBytes());
				LogUtil.show("Xpath error, dump file is:" + file.getAbsolutePath());
				IOUtils.closeQuietly(out);
			} catch (Exception e1) {
				LogUtil.exception(e1);
			}
			throw new IllegalArgumentException(e);
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
				sb.append("[img]").append(XMLUtils.attrib((Element) node, "src")).append("[img]");
			}
			for (Node child : XMLUtils.toArray(node.getChildNodes())) {
				sb.append(htmlNodeToString(child, keepenter));
			}
			return sb.toString();
		}
	}

	private static int getIndexOfNode(Node node) {
		if (node.getParentNode() == null)
			return 0;
		int count = 0;
		for (Node e : XMLUtils.toArray(node.getParentNode().getChildNodes())) {
			if (e.getNodeName().equals(node.getNodeName())) {
				count++;
				if (e == node)
					return count;
			}
		}
		throw new RuntimeException("Cann't locate the node's index of its parent.");
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

	public static void setAttributeByXPath(Node node, String xPath, String value) {
		int i = xPath.lastIndexOf('@');
		if (i < 0)
			throw new IllegalArgumentException("there is no @ in your xpath.");
		String left = xPath.substring(0, i);
		String right = xPath.substring(i + 1);
		Node n = getNodeByXPath(node, left);
		if (n instanceof Element) {
			if ("#text".equals(right)) {
				XMLUtils.setText(n, value);
			} else {
				((Element) n).setAttribute(right, value);
			}
		} else {
			throw new IllegalArgumentException("node at " + left + " is not a element!");
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
			XMLUtils.setAttributesByMap((Element) n, attribute, isSubNode);
		} else {
			throw new IllegalArgumentException("node at " + xPath + " is not a element!");
		}
	}
	
	final static class NodeListIterable implements Iterable<Node> {
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

}
