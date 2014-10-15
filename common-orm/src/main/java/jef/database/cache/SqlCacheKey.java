package jef.database.cache;

import java.util.List;

import org.apache.commons.lang.StringUtils;

@SuppressWarnings("serial")
public class SqlCacheKey implements CacheKey{
	private String table;
	private KeyDimension dimension;
	private List<?> params;
	
	public SqlCacheKey(){
	}
	

	private String formatTable(String table) {
		//TODO 本来想在这里统一转为小写处理的，但实际发现Cache table有两种，一种是类名，一种是由Join等关键字构成的多个表名，因此统一转小写后，类名无法准确匹配。
		//如果不转统一小写，那么用户自行编写的SQL语句变得大小写敏感，也影响SQL命中。
		//但是后者问题更为复杂，因为目前table当中还包括了SQL中表定义的别名，这进一步影响缓存命中效率。所以目前暂不处理大小写，待别名问题一起处理。
		return StringUtils.remove(table, '\n');
	}

	
	public SqlCacheKey(String tableName,KeyDimension dim,List<?> params){
		this.table=formatTable(tableName);
		this.dimension=dim;
		this.params=params;
	}

	public String getTable() {
		return table;
	}

	public List<?> getParams() {
		return params;
	}

	public KeyDimension getDimension() {
		return dimension;
	}

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append('[').append(table);
		sb.append("] ").append(dimension);
		sb.append(' ');
		sb.append(params);
		return sb.toString();
	}
}
