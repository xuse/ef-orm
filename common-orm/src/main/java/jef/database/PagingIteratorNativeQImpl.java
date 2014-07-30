package jef.database;

import java.sql.SQLException;
import java.util.List;

import jef.common.wrapper.IntRange;
import jef.tools.Assert;
import jef.tools.PageInfo;

final class PagingIteratorNativeQImpl<T> extends PagingIterator<T>{
	private NativeQuery<T> nativeQuery;//3使用NativeQuery的情况

	PagingIteratorNativeQImpl(NativeQuery<T> sql, int pageSize) {
		this.nativeQuery=sql;
		Assert.notNull(sql);
		this.transformer = sql.getResultTransformer();
		page = new PageInfo();
		page.setRowsPerPage(pageSize);
	}

	@Override
	protected long doCount() throws SQLException {
		return nativeQuery.getResultCount();
	}

	protected List<T> doQuery(boolean pageFlag) throws SQLException {
		calcPage();
		IntRange range=page.getCurrentRecordRange();
		if(range.getStart()==1 && range.getEnd().intValue()==page.getTotal()){
			pageFlag=false;
		}
		if(pageFlag)
			nativeQuery.setRange(range);
		List<T> result=nativeQuery.getResultList();
		if (result.isEmpty()) {
			recordEmpty();
		}
		return result;
	}
	
	public NativeQuery<T> getQuery(){
		return nativeQuery;
	}
}
