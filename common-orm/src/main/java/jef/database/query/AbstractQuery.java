package jef.database.query;

import java.io.Serializable;

import javax.persistence.Transient;

import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.populator.Transformer;

public abstract class AbstractQuery<T extends IQueryableEntity> implements Query<T>, Serializable {
	
	static final Query<?>[] EMPTY_Q = new Query[0];
	
	/**
	 * 实例
	 */
	@Transient
	transient T instance;
	/**
	 * 类型
	 */
	@Transient
	transient ITableMetadata type;

	/**
	 * 结果转换器
	 */
	protected Transformer t;

	private int maxResult;
	private int fetchSize;
	private int queryTimeout;
	
	public void setMaxResult(int size) {
		this.maxResult = size;
	}

	public void setFetchSize(int fetchszie) {
		this.fetchSize = fetchszie;
	}

	public void setQueryTimeout(int timeout) {
		this.queryTimeout = timeout;
	}

	public int getMaxResult() {
		return maxResult;
	}

	public int getFetchSize() {
		return fetchSize;
	}

	public int getQueryTimeout() {
		return queryTimeout;
	}
	
	public Transformer getResultTransformer(){
		if(t==null){
			t=new Transformer(type);
			t.setLoadVsOne(true);
			t.setLoadVsMany(true);
		}
		return t;
	}

	public void setAutoOuterJoin(boolean cascadeOuterJoin) {
		setCascadeViaOuterJoin(cascadeOuterJoin);
	}
	public Query<T> orderByAsc(Field... ascFields) {
		addOrderBy(true, ascFields);
		return this;
	}

	public Query<T> orderByDesc(Field... descFields) {
		addOrderBy(false, descFields);
		return this;
	}
	
	public ITableMetadata getMeta() {
		return type;
	}

	public T getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	public Class<T> getType() {
		return (Class<T>) type.getThisType();
	}
}
