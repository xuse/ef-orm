package jef.database.dialect.type;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.clause.UpdateClause;

public interface VersionSupportColumn extends ColumnMapping{
	/**
	 * 处理自动更新值，主要用于绑定变量情况下的更新操作
	 * @param dialect  方言
	 * @param update
	 */
	void processAutoUpdate(DatabaseDialect dialect, UpdateClause update);
	
	/**
	 * 获得自动更新的数值，主要用于非绑定变量下的更新操作（其实基本不用）。
	 * 但另外一个功能也用到了,用来获取修改默认值。（这个以后应该要改掉）
	 * @param dialect 方言
	 * @param bean
	 * @return
	 */
	Object getAutoUpdateValue(DatabaseDialect dialect, Object bean);
	
	/**
	 * 描述这是否为一个每次update都更新的字段。
	 * @return true if the field is update every time
	 */
	boolean isUpdateAlways();
	
	/**
	 * 描述这是否为一个版本字段。如果一个字段为版本字段，那一定是一个每次update都更新的字段。
	 * @return true is the field is versioned.
	 */
	boolean isVersion();
}
