package jef.database.innerpool;

import java.lang.reflect.Array;
import java.sql.SQLException;

import jef.database.IQueryableEntity;
import jef.database.wrapper.IResultSet;
import jef.tools.reflect.BeanWrapper;

public class ArrayElementPopulator implements IPopulator{
	private int index;
	private InstancePopulator populator;
	
	public ArrayElementPopulator(int index,InstancePopulator populator){
		this.index=index;
		this.populator=populator;
	}
	
	public void process(BeanWrapper wrapper, IResultSet rs) throws SQLException{
		BeanWrapper bw = wrapper;
		Object array=bw.getWrapped();
		
		Object subDo = populator.instance();
		boolean flag =  populator.processOrNull(BeanWrapper.wrap(subDo), rs);
		if (flag) {
			if (subDo instanceof IQueryableEntity) {
				((IQueryableEntity) subDo).startUpdate();
			}
			Array.set(array, index, subDo);
		}
	}
}
