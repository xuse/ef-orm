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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.w3c.dom.Document;

import jef.tools.Exceptions;
import jef.tools.StringUtils;

public class RssParser {
	/**
	 * 将XML document 转化为RSS对象加以读取
	 * @param xmlDoc
	 * @return
	 */
	public static RssChannel parse(Document xmlDoc){
		RssChannel channel=new RssChannel(xmlDoc);
		return channel;
	}
	
	private static final SimpleDateFormat sd=new SimpleDateFormat("dd MMM yyyy HH:mm:ss" ,Locale.US);
	static final Date parseDate(String pDate) {
		if(StringUtils.isNotEmpty(pDate)){
			pDate=StringUtils.substringBetween(pDate, ",", "+").trim();
			try {
				synchronized(sd){
					return sd.parse(pDate);
				}
			} catch (ParseException e1) {
				Exceptions.log(e1);
			}
		}
		return null;
	}
}
