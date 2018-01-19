package jef.database.wrapper.populator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.database.jdbc.result.IResultSet;

public abstract class ListResultSetTransformer<T> extends AbstractResultSetTransformer<List<T>>{
	@Override
	public List<T> transformer(IResultSet rs) throws SQLException {
		List<T> list=new ArrayList<T>();
		while(rs.next()){
			T s=transform(rs);
			if(s!=null){
				list.add(s);
			}
		}
		return list;
	}

	protected abstract T transform(IResultSet rs) throws SQLException;
}
