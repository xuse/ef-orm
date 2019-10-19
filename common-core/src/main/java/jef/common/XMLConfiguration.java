package jef.common;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jef.tools.SimpleXPath;
import jef.tools.XMLUtils;

/**
 * 用XML文档实现的配置访问器
 * @author Administrator
 *
 */
public final class XMLConfiguration extends Cfg{
	/**
	 * xml文档 
	 */
	private Document doc;
	/**
	 * 文本节点模式
	 */
	private boolean nodeTextMode;
	
	/**
	 * 构造
	 * @param doc
	 */
	public XMLConfiguration(Document doc){
		this(doc,true);
	}
	
	/**
	 * 返回Document
	 * @return
	 */
	public Document getDocument(){
		return this.doc;
	}
	
	/**
	 * 获取元素
	 * @param xpath
	 * @return
	 */
	public Element getElement(String xpath){
		try{
			Element ele=(Element)SimpleXPath.getNodeByXPath(doc, xpath);
			return ele;
		}catch(Exception e){
			log.warn("",e);
		}
		return null;
	}
	
	/**
	 * 构造
	 * @param doc2 文档
	 * @param isNodeTextMode
	 * 如果设置为true，那么所有的请求key都对应一个xpath的node，配置值是这个node的Text节点。
	 * 如果为false,那么直接认为请求的key是一个xpath
	 */
	public XMLConfiguration(Document doc2, boolean isNodeTextMode) {
		this.doc=doc2;
		this.nodeTextMode=isNodeTextMode;
	}

	@Override
	protected String get(String key, String string) {
		if(nodeTextMode){
			Node node=null;
			try{
				node=SimpleXPath.getNodeByXPath(doc, key);
			}catch(Exception e){
				log.warn(e.getMessage());
			}
			if(node!=null)return XMLUtils.nodeText(node);
		}else{
			try{
				return SimpleXPath.getAttributeByXPath(doc, key);
			}catch(Exception e){
				log.warn(e.getMessage());
			}
		}
		return string;
	}
}
