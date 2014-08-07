package jef.database.wrapper.populator;

import java.sql.SQLException;

import jef.database.wrapper.result.IResultSet;
import jef.tools.reflect.BeanWrapper;

/**
 * 封装一段ResultSet到javabean的逻辑。
 * @author jiyi
 *
 */
public interface IPopulator {
	public void process(BeanWrapper wrapper, IResultSet rs) throws SQLException;
}
