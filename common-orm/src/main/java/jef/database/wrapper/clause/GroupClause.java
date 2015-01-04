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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jef.http.client.support.CommentEntry;
import jef.tools.StringUtils;

public class GroupClause implements SqlClause {
	private final List<String> groups = new ArrayList<String>(4);
	private final List<HavingEle> having = new ArrayList<HavingEle>(4);

	public static final GroupClause DEFAULT = new GroupClause();

	public GroupClause() {
	}

	public void addGroup(String selectItem) {
		groups.add(selectItem);
	}

	public void addHaving(HavingEle havingClause) {
		having.add(havingClause);
	}

	public boolean isNotEmpty() {
		return !(groups.isEmpty() && having.isEmpty());
	}

	@Override
	public String toString() {
		return getSql(true);
	}

	public String getSql(boolean withHaving) {
		if (groups.isEmpty() && having.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if (!groups.isEmpty()) {
			sb.append(" group by ");
			sb.append(StringUtils.join(groups, ','));
		}
		if (withHaving && !having.isEmpty()) {
			sb.append(" having ");
			sb.append(StringUtils.join(having, " and "));
		}
		return sb.toString();
	}

	/*
	 * 当确定需要内存分组时，需要解析Select部分的函数，从而得到合适的内存分组计算规则 每个查询字段信息包括
	 * 
	 * 分组列—— 别名
	 * 
	 * 计算列—— 函数，别名
	 */
	public InMemoryGroupByHaving parseSelectFunction(SelectPart select) {
		List<GroupByItem> keys = new ArrayList<GroupByItem>(4);
		List<GroupByItem> values = new ArrayList<GroupByItem>(4);
		List<HavingEle> he = new ArrayList<HavingEle>(3);
		Map<String, HavingEle> havings = prepareHavingIndex();
		for (int i = 0; i < select.getEntries().size(); i++) {
			CommentEntry e = select.getEntries().get(i);
			String sql = e.getKey();
			String alias = e.getValue();
			if (groups.contains(sql)) {
				keys.add(new GroupByItem(i, GroupFunctionType.GROUP, alias));
			} else {
				GroupFunctionType type;
				String exp = sql.toUpperCase();
				if (exp.startsWith("AVG(")) {
					type = GroupFunctionType.AVG;
				} else if (exp.startsWith("COUNT(")) {
					type = GroupFunctionType.COUNT;
				} else if (exp.startsWith("SUM(")) {
					type = GroupFunctionType.SUM;
				} else if (exp.startsWith("MIN(")) {
					type = GroupFunctionType.MIN;
				} else if (exp.startsWith("MAX(")) {
					type = GroupFunctionType.MAX;
				} else if (exp.startsWith("ARRAY_TO_STRING(")) {
					type = GroupFunctionType.ARRAY_TO_STRING;
				} else {
					type = GroupFunctionType.NORMAL;
				}
				values.add(new GroupByItem(i, type, alias));
			}
			HavingEle h = havings.remove(sql);
			if (h != null) {
				h.setIndex(i);
				he.add(h);
			}
		}
		InMemoryGroupByHaving process = new InMemoryGroupByHaving(keys, values);
		if (!he.isEmpty()) {
			process.setHaving(he);
		}
		return process;
	}

	private Map<String, HavingEle> prepareHavingIndex() {
		Map<String, HavingEle> map = new HashMap<String, HavingEle>(4);
		for (HavingEle h : having) {
			map.put(h.column, h);
		}
		return map;
	}

}
