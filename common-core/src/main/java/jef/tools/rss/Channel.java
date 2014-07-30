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

import java.util.Iterator;

import jef.common.DateSpan;

/**
 * 描述一个RSS频道
 * @author Administrator
 *
 */
public interface Channel {
	/**
	 * 获取频道标题
	 * @return
	 */
	String getTitle();
	/**
	 * 获取频道的消息
	 * @return
	 */
	Iterator<RssItem> getItems();
	/**
	 * 获取频道消息的发布时段
	 * @return
	 */
	DateSpan getItemDateSpan();
	/**
	 * 返回消息条数
	 * @return
	 */
	int size();
}
