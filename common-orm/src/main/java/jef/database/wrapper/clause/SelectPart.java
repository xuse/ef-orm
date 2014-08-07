package jef.database.wrapper.clause;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jef.database.ORMConfig;
import jef.http.client.support.CommentEntry;

import org.apache.commons.lang.StringUtils;

public class SelectPart {
	private boolean distinct;
	private final List<CommentEntry> entries=new ArrayList<CommentEntry>();

	public boolean isDistinct() {
		return distinct;
	}
	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}
	public List<CommentEntry> getEntries() {
		return entries;
	}
	public void append(StringBuilder sb){
		append(sb,false);
	}
	
	/*
	 * 
	 * @param sb
	 * @param noGroupFunc 在分表后的group操作中，每个子句要不做函数，而在所有union all语句的外部才做函数。因此当true时表示在unionall 内部的select获取
	 */
	public void append(StringBuilder sb,boolean noGroupFunc){
		sb.append("select ");
		if (distinct)
			sb.append("distinct ");
		Iterator<CommentEntry> iter=entries.iterator();
		int i = 0;
		while(iter.hasNext()){
			CommentEntry entry=iter.next();
			if (i > 0)
				sb.append(',').append(ORMConfig.getInstance().wrapt); // 从第2列开始，每列之后都需要添加,分隔符
			if(noGroupFunc){
				if(entry.getKey().indexOf('(')>0){
					 sb.append(StringUtils.substringBetween(entry.getKey(), "(",")"));
				 }else{
					 sb.append(entry.getKey()); 
				 }
			}else{
				sb.append(entry.getKey());
				if (entry.getValue() != null) { // value 是别名
					// PostgreSQL中，当name作为列别名需要加AS，否则会报错
					sb.append(" AS ").append(entry.getValue());
				}
			}
			i++;
		}
		if(ORMConfig.getInstance().isFormatSQL() && entries.size()>1){
			sb.append("\n");
		}
	}
	
	public void addAll(CommentEntry[] selectColumns) {
		for(int i=0;i<selectColumns.length;i++){
			entries.add(selectColumns[i]);
		}
	}
	
	public void add(CommentEntry item) {
		entries.add(item);
	}
}
