package jef.database;

import jef.database.support.RDBMS;
import jef.tools.StringUtils;

/**
 * 一个命名查询的SQL，通过链表的方式记录了不同RDBMS下的不同的命名查询
 * @author jiyi
 *
 */
final class NQEntry{
	public NQEntry(NamedQueryConfig namedQueryConfig, RDBMS type,String source) {
		this.config=namedQueryConfig;
		this.dialect=type;
		this.source=source;
	}
	//方言信息
	NQEntry next;
	RDBMS dialect;
	NamedQueryConfig config;
	private String source;
	
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public NamedQueryConfig get(RDBMS type) {
		if(type==null)return config;
		NQEntry current=this;
		while(current!=null){
			if(current.dialect==type)return current.config;
			current=current.next;
		}
		if(this.dialect==null)
			return config;//注意将通用的dialect要放在链表的第一位
		throw new IllegalArgumentException("Can not found NamedQuery ["+config.getName()+"] for "+type+".");
	}
	
	public String getTag() {
		String tag=null;
		NQEntry current=this;
		while(current!=null){
			tag=current.config.getTag();
			if(StringUtils.isNotEmpty(tag)){
				return tag;
			}
			current=current.next;
		}
		return null;
	}
}