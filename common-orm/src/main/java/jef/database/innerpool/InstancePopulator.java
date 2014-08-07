package jef.database.innerpool;

import java.sql.SQLException;

import jef.database.wrapper.populator.IPopulator;
import jef.database.wrapper.result.IResultSet;
import jef.tools.reflect.BeanWrapper;

public interface InstancePopulator extends IPopulator{
	Object instance();
	public Class<?> getObjectType();
	public boolean processOrNull(BeanWrapper wrapper, IResultSet rs) throws SQLException;
}
