package jef.database.wrapper.clause;

import java.util.ArrayList;
import java.util.List;

import jef.http.client.support.CommentEntry;
import jef.tools.StringUtils;

public class GroupClause implements SqlResult{
	private final List<String> groups = new ArrayList<String>();
	private final List<String> having = new ArrayList<String>();
	private SelectPart select;
	
	
	public static final GroupClause EMPTY=new GroupClause();
	public GroupClause(){
	}

	public void addGroup(String selectItem) {
		groups.add(selectItem);
	}

	public void addHaving(String havingClause) {
		having.add(havingClause);
	}

	public boolean isNotEmpty() {
		return !(groups.isEmpty()&& having.isEmpty());
	}

	@Override
	public String toString() {
		if(groups.isEmpty()&& having.isEmpty()){
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if (!groups.isEmpty()) {
			sb.append(" group by ");
			sb.append(StringUtils.join(groups, ','));
		}
		if (!having.isEmpty()) {
			sb.append(" having ");
			sb.append(StringUtils.join(having, " and "));
		}
		return sb.toString();
	}


	
	public void setSelect(SelectPart rs) {
		this.select=rs; //仅赋值不解析
	}

	/*
	 * 当确定需要内存分组时，需要解析Select部分的函数，从而得到合适的内存分组计算规则
	 * 每个查询字段信息包括
	 * 
	 * 分组列—— 别名
	 * 
	 * 计算列—— 函数，别名
	 */
	public List<GroupByEle> parseSelectFunction() {
		List<GroupByEle> result=new ArrayList<GroupByEle>();
		for(int i=0;i<select.getEntries().size();i++){
			CommentEntry e=select.getEntries().get(i);
			String sql=e.getKey();
			String alias=e.getValue();
			GroupFunctionType type;
			if(groups.contains(sql)){
				type=GroupFunctionType.GROUP;
				continue;
			}else{
				String exp=sql.toUpperCase();
				if(exp.startsWith("AVG(")){
					type=GroupFunctionType.AVG;
				}else if(exp.startsWith("COUNT(")){
					type=GroupFunctionType.COUNT;
				}else if(exp.startsWith("SUM(")){
					type=GroupFunctionType.SUM;
				}else if(exp.startsWith("MIN(")){
					type=GroupFunctionType.MIN;
				}else if(exp.startsWith("MAX(")){
					type=GroupFunctionType.MAX;
				}else if(exp.startsWith("ARRAY_TO_LIST(")){	
					type=GroupFunctionType.ARRAY_TO_LIST;
				}else{
					type=GroupFunctionType.NORMAL;
				}	
			}
			result.add(new GroupByEle(i,type,alias));
		}
		return result;
	}

	

}
