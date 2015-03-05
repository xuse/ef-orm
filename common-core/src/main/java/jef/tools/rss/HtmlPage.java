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
package jef.tools.rss;

import java.util.List;

import jef.tools.SimpleXPath;
import jef.tools.XMLUtils;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HtmlPage{
	protected Element bodyNode;
	protected Element headNode;
	protected String title;
	protected static final String DUMMY="DUMMY";
	
	/**
	 * 返回BODY节点
	 * @return
	 */
	public Element getBodyNode() {
		return bodyNode;
	}

	/**
	 * 返回HEAD节点
	 * @return
	 */
	public Element getHeadNode() {
		return headNode;
	}
	/**
	 * 构造，对于没有BODY节点的文档会主动创建一个BODY节点
	 * @param doc
	 */
	public HtmlPage(DocumentFragment doc) {
		Element root=XMLUtils.first(doc, "HTML");
		if(root==null){
			root=(Element) XMLUtils.first(doc,Node.ELEMENT_NODE);
			this.bodyNode=XMLUtils.first(doc, "BODY");
			this.headNode=XMLUtils.first(doc, "HEAD");
			
			if(bodyNode==null){
				if(root.getNextSibling()==null){
					bodyNode=root;
				}else{//将所有节点放到BODY节点下，BODY节点和root节点相同
					bodyNode=doc.getOwnerDocument().createElement("BODY");
					for(Node nd:XMLUtils.toList(doc.getChildNodes())){
						nd=doc.removeChild(nd);//从原来的根节点下移出
						bodyNode.appendChild(nd);
					}
					doc.appendChild(bodyNode);
					root=bodyNode;
				}
			}
		}else{
			this.bodyNode=XMLUtils.first(root, "BODY");
			this.headNode=XMLUtils.first(root, "HEAD");	
		}
		this.title=XMLUtils.nodeText(headNode, "TITLE");
	}

	/**
	 * 返回HTML文档的标题
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * 得到指定节点（集）下面的格式化文本
	 * @param xpath
	 * @return
	 */
	public String getFormattedText(String xpath) {
		return SimpleXPath.getAttributeByXPath(bodyNode, "/text:"+xpath);
	}

	public List<? extends Node> getElementList(String xpath) {
		NodeList nds=SimpleXPath.getNodeListByXPath(bodyNode, xpath);
		return XMLUtils.toList(nds);
	}
}
