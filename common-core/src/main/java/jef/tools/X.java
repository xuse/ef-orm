package jef.tools;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import jef.common.log.LogUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 继承XMLUtils,相当于提供一个XMLUtils的别名
 * 无实际内容，仅仅为了在解析XML时可以少打几个字
 * @author Administrator
 *
 */
public final class X extends XMLUtils{
	private X(){}
	

	/**
	 * 创建文档
	 * @return
	 */
	public static Document $new(){
		return newDocument();
	}
	
	
	
	/**
	 * 找寻一个元素节点
	 * @param xpath xpath
	 * @param node  要查找的Node
	 * @return 找不到返回null
	 */
	public static Element $(String xpath,Node node){
		try{
			Node result=SimpleXPath.getNodeByXPath(node, xpath);
			if(result.getNodeType()==Node.ELEMENT_NODE){
				return (Element) result;
			}
		}catch(NullPointerException e){
			//DO nothing
		}
		return null;
	}
	

	/**
	 * 仿选择器，在node下找寻指定的元素(Element)列表
	 * @param xpath  xpath
	 * @param node 要查找的Node
	 * @return 找不到返回null
	 */
	public static List<Element> $$(String xpath,Node node){
		try{
			NodeList result=SimpleXPath.getNodeListByXPath(node, xpath);
			return toElementList(result) ;
		}catch(Exception e){
			LogUtil.show(e.getMessage());
		}
		return Collections.emptyList();
	}
	
	/**
	 * 仿选择器，在node下找寻指定id的元素(Element)
	 * @param id
	 * @param node
	 * @return
	 */
	public static Element $id(String id,Node node){
		return findElementById(node, id);
	}
	
	/**
	 * 简化方法，相当于toElementList(NodeList)
	 * @param nds
	 * @return
	 */
	public static List<Element> le(NodeList nds) {
		return toElementList(nds);
	}
	
	public static void print(Node node){
		try {
			printNode(node, System.out);
		} catch (IOException e) {
			LogUtil.exception(e);
		}
	}
}
