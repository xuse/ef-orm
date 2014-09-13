package jef.database.routing.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jef.database.query.ParameterProvider;

public final class ParamsContextProvider implements ParameterProvider{
	final List<ParameterContext> params=new ArrayList<ParameterContext>();
	public List<Object> asValues() {
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public Object getNamedParam(String name) {
//		Integer index = Integer.valueOf(name);
//		return this.nameParams.get(index);
//	}
//
//	@Override
//	public boolean containsParam(Object key) {
//		if (key instanceof Integer) {
//			return nameParams.size() > (Integer) key;
//		}
//		return false;
//	}

	public void clear() {
		// TODO Auto-generated method stub
		
	}

	public void remove(int index) {
		// TODO Auto-generated method stub
		
	}

	public void set(Collection<ParameterContext> params2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object getIndexedParam(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getNamedParam(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean containsParam(Object key) {
		// TODO Auto-generated method stub
		return false;
	}

	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void apply(PreparedStatement st) throws SQLException {
		for(ParameterContext v:params){
			v.getParameterMethod().setParameter(st, v.getArgs());
		}
		
	}
	
}