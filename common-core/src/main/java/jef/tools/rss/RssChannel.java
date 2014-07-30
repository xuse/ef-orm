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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import jef.common.DateSpan;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RssChannel implements Channel{
	private Element channelE;
	private List<RssItem> items=new ArrayList<RssItem>();
	
	private DateSpan itemDateSpan;
	private String version;
	private Date pubDate;
	private String description;
	private String link;
	private String title;
	private String language;
	
	/**
	 * 根据XML文档构造
	 * @param xmlDoc
	 */
	public RssChannel(Document xmlDoc) {
		Element e =xmlDoc.getDocumentElement();
		Assert.notNull(e);
		version = e.getAttribute("version");
		channelE = XMLUtils.first(e, "channel");
		if(channelE==null){
			throw new NullPointerException("there's no channel node under this document!");
		}
		description = XMLUtils.nodeText(channelE, "description");
		title = XMLUtils.nodeText(channelE, "title");
		link = XMLUtils.nodeText(channelE, "link");
		language = XMLUtils.nodeText(channelE, "language");
		String pDate= XMLUtils.nodeText(channelE, "pubDate");
		if(pDate==null)pDate= XMLUtils.nodeText(e, "lastBuildDate");
		pubDate=RssParser.parseDate(pDate);
		
		Items is=new Items(channelE);
		while(is.hasNext()){
			RssItem item=is.next();
			if(itemDateSpan==null){
				itemDateSpan=new DateSpan(item.getPubDate(),item.getPubDate());
			}else{
				itemDateSpan.extendTo(item.getPubDate());
			}
			items.add(item);
		}
	}
	
	public Iterator<RssItem> getItems(){
		//return new Items(channelE);
		return items.iterator();
	}
	
	class Items implements Iterator<RssItem>{
		private List<Element> is;
		private int n=0;
		
		public Items(Element e){
			is=XMLUtils.childElements(e, "item");
		}

		public boolean hasNext() {
			return n<is.size();
		}

		public RssItem next() {
			Element e=is.get(n);
			RssItem item=new RssItem();
			
			String title=XMLUtils.nodeText(e,"title");
			item.setTitle(StringUtils.unescapeHtml(title));
			
			String author=XMLUtils.nodeText(e, "author");
			item.setAuthor(author);
			
			String link=XMLUtils.nodeText(e, "link");
			item.setLink(link);
			
			String description=XMLUtils.nodeText(e, "description");
			item.setDescription(description);
			String guid=XMLUtils.nodeText(e, "guid");
			item.setGuid(guid);
			
			Element catE=XMLUtils.first(e, "category");
			String category=XMLUtils.nodeText(catE);
			String categoryDomain=catE.getAttribute("domain");
			item.setCategory(category);
			item.setCategoryDomain(categoryDomain);
			
			String pDate= XMLUtils.nodeText(e, "pubDate");
			item.setPubDate(RssParser.parseDate(pDate));
			
			Element enclosure=XMLUtils.first(e, "enclosure");
			if(enclosure!=null){
				item.setEnclosureUrl(StringUtils.trimToNull(enclosure.getAttribute("url")));
				item.setEnclosureType(StringUtils.trimToNull(enclosure.getAttribute("type")));
			}
			item.setComments(XMLUtils.nodeText(e, "comments"));
				 
			n++;
			return item;
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public String getDescription() {
		return description;
	}
	public String getLanguage() {
		return language;
	}
	public String getLink() {
		return link;
	}
	public Date getPubDate() {
		return pubDate;
	}
	public String getTitle() {
		return title;
	}
	public String getVersion() {
		return version;
	}
	public DateSpan getItemDateSpan() {
		return itemDateSpan;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public int size() {
		return items.size();
	}
}
