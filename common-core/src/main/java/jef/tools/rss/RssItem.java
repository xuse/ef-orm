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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jef.common.log.LogUtil;
import jef.tools.DateUtils;

public class RssItem {
	private String title;
	private String link;
	private String author;
	private Date pubDate;
	private String description;
	private String guid;
	private String category;
	private String categoryDomain;
	private String comments;
	private String enclosureUrl;
	private String enclosureType;
	
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public String getEnclosureType() {
		return enclosureType;
	}
	public void setEnclosureType(String enclosureType) {
		this.enclosureType = enclosureType;
	}
	public String getEnclosureUrl() {
		return enclosureUrl;
	}
	public void setEnclosureUrl(String enclosureUrl) {
		this.enclosureUrl = enclosureUrl;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getCategoryDomain() {
		return categoryDomain;
	}
	public void setCategoryDomain(String categoryDomain) {
		this.categoryDomain = categoryDomain;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getGuid() {
		return guid;
	}
	public void setGuid(String guid) {
		this.guid = guid;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public Date getPubDate() {
		return pubDate;
	}
	public void setPubDate(Date pubDate) {
		this.pubDate = pubDate;
	}
	
	private DateFormat df=new SimpleDateFormat("yyyy/MM/dd");
	private DateFormat myDf=new SimpleDateFormat("yyyy/MM/dd HH:mm");
	
	
	public void setMyDf(DateFormat myDf) {
		if(myDf!=null)
			this.myDf = myDf;
	}
	public void setPubDateString(String pubDate) {
		if(pubDate.startsWith("昨天 ")){
			pubDate=pubDate.replace("昨天", df.format(new Date(System.currentTimeMillis()-DateUtils.MILLISECONDS_IN_DAY)));
		}else if(pubDate.startsWith("今天 ")){
			pubDate=pubDate.replace("今天", df.format(new Date()));
		}
		try {
			this.pubDate = DateUtils.parse(pubDate,myDf);
		} catch (ParseException e) {
			LogUtil.exception(pubDate+" format="+DateUtils.format(new Date(), myDf),e);
		}
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
}
