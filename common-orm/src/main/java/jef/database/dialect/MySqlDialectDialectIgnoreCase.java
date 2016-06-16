package jef.database.dialect;

/**
 * 默认情况下MySQL在linux上表名和列名均是区分大小写的。
 * 而在Windows上，表名和列名不区分大小写。
 * 
 * @author jiyi
 *
 */
public class MySqlDialectDialectIgnoreCase extends MySqlDialect {

	/**
	 * MYSQL中，表名是全转小写的，列名才是保持大小写的，先做小写处理，如果有处理列名的场合，改为调用
	 * {@link #getColumnNameToUse(String)}
	 */
	@Override
	public String getObjectNameToUse(String name) {
		if (name == null || name.length() == 0)
			return null;
		if (name.charAt(0) == '`')
			return name;
		return name.toLowerCase();
	}
}
