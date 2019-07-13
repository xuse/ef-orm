package jef.database.cache;

import java.util.List;

import  org.apache.commons.lang3.builder.EqualsBuilder;
import  org.apache.commons.lang3.builder.HashCodeBuilder;

@SuppressWarnings("serial")
public class SqlCacheKey implements CacheKey{
	private KeyDimension dimension;
	private List<?> params;
	
	public SqlCacheKey(){
	}

	/**
	 * 构造
	 * @param dim
	 * @param params
	 */
	public SqlCacheKey(KeyDimension dim,List<?> params){
		this.dimension=dim;
		this.params=params;
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
		sb.append('[').append(dimension.getTableDefinition());
		sb.append("] ").append(dimension);
		sb.append(' ');
		sb.append(params);
		return sb.toString();
	}


	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(dimension).append(params).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SqlCacheKey) {
			SqlCacheKey rhs=(SqlCacheKey)obj;
			return new EqualsBuilder().append(this.dimension, rhs.dimension).append(this.params, rhs.params).isEquals();
		}
		return false;
	}

	@Override
	public String getStoreSpace() {
		return dimension.getTableDefinition();
	}

	@Override
	public List<String> getAffectedKey() {
		return dimension.getTables();
	}
	
	
}
