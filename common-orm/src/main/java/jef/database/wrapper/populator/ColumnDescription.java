package jef.database.wrapper.populator;

import java.sql.SQLException;

import jef.common.log.LogUtil;
import jef.database.dialect.type.ResultSetAccessor;
import jef.database.wrapper.result.IResultSet;

public class ColumnDescription{
	private int n;
	private int type;
	private String name;
	private String simpleName;
	private ResultSetAccessor accessor;
	
	public ColumnDescription(int n,int type,String name){
		this.n=n;
		this.type=type;
		this.name=name;
	}

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public int getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 总是返回小写的SimpleName
	 * @return
	 */
	public String getSimpleName() {
		return simpleName;
	}

	public void setSimpleName(String simpleName) {
		this.simpleName = simpleName.toLowerCase();
	}

	@Override
	public String toString() {
		return "("+n+")"+name+" [Type:"+type+"]";
	}

	public ResultSetAccessor getAccessor() {
		return accessor;
	}

	public void setAccessor(ResultSetAccessor accessor) {
		if(this.accessor!=null && this.accessor!=accessor){
			LogUtil.warn("Column "+this.name+"("+type+") received two different ResultSetAccessor!"+this.accessor+" : "+accessor);
		}
		this.accessor = accessor;
	}

	public Object getValue(IResultSet rs) throws SQLException {
		return accessor.getProperObject(rs, n);
	}
	
}
