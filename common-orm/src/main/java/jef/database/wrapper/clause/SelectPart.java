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
package jef.database.wrapper.clause;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jef.database.ORMConfig;
import jef.http.client.support.CommentEntry;

import org.apache.commons.lang.StringUtils;

public class SelectPart {
	private boolean distinct;
	private final List<CommentEntry> entries = new ArrayList<CommentEntry>();

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public List<CommentEntry> getEntries() {
		return entries;
	}

	public void appendNoGroupFunc(StringBuilder sb) {
		sb.append("select ");
		if (distinct)
			sb.append("distinct ");
		Set<String> alreadyField=new HashSet<String>();
		List<String> columns=new ArrayList<String>();
		
		for (CommentEntry entry: entries) {
			String column;
			if (entry.getKey().indexOf('(') > 0) {
				column=StringUtils.substringBetween(entry.getKey(), "(", ")");
			} else {
				column=entry.getKey();
			}
			int point=column.indexOf('.');
			String key=point==-1?column:column.substring(point+1);
			
			if("*".equals(key)){
				columns.clear();
				columns.add(column);
				break;
			}
			if(!alreadyField.contains(key)){
				alreadyField.add(key);
				columns.add(column);
			}
		}
		Iterator<String> iter=columns.iterator();
		sb.append(iter.next());
		for(;iter.hasNext();){
			sb.append(',').append(iter.next());
		}
		if (ORMConfig.getInstance().isFormatSQL() && columns.size() > 1) {
			sb.append("\n");
		}
	}

	/*
	 * 
	 * @param sb
	 */
	public void append(StringBuilder sb) {
		sb.append("select ");
		if (distinct)
			sb.append("distinct ");
		Iterator<CommentEntry> iter = entries.iterator();
		int i = 0;
		while (iter.hasNext()) {
			CommentEntry entry = iter.next();
			if (i > 0)
				sb.append(',').append(ORMConfig.getInstance().wrapt); // 从第2列开始，每列之后都需要添加,分隔符

			sb.append(entry.getKey());
			if (entry.getValue() != null) { // value 是别名
				// PostgreSQL中，当name作为列别名需要加AS，否则会报错
				sb.append(" AS ").append(entry.getValue());
			}
			i++;
		}
		if (ORMConfig.getInstance().isFormatSQL() && entries.size() > 1) {
			sb.append("\n");
		}
	}

	public void addAll(CommentEntry[] selectColumns) {
		for (int i = 0; i < selectColumns.length; i++) {
			entries.add(selectColumns[i]);
		}
	}

	public void add(CommentEntry item) {
		entries.add(item);
	}
}
