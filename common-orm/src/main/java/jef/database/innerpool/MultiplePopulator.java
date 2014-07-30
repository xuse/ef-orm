package jef.database.innerpool;

import java.sql.SQLException;

import jef.database.wrapper.IResultSet;
import jef.tools.reflect.BeanWrapper;

public final class MultiplePopulator implements IPopulator{
	private IPopulator[] populators;
	public MultiplePopulator(IPopulator... populators){
		this.populators=populators;
	}
	
	public void process(BeanWrapper wrapper, IResultSet rs) throws SQLException {
		for(IPopulator p: populators){
			p.process(wrapper, rs);
		}
	}
}
