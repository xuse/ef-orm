package jef.database.meta;

import jef.database.DbUtils;
import jef.database.Field;
import jef.database.dialect.DatabaseDialect;

/**
 *  别名供应器
 * @author jiyi
 *
 */
public interface AliasProvider {
	/**
	 * 全对象引用模式下可以提供各个字段的别名 如果返回null，将不予拼装
	 * 
	 * @param f
	 * @param profile
	 * @param schema
	 * @return
	 */
	String getSelectedAliasOf(Field f, DatabaseDialect profile, String schema);
	
	//TODO 考虑吧将两种场景分开
//	String getAliasForResult(ITableMetadata meta,)
	
	public static final AliasProvider DEFAULT=new AliasProvider(){
		public String getSelectedAliasOf(Field f, DatabaseDialect profile, String schema) {
			return DbUtils.getDefaultColumnAlias(f, profile, schema);
		}
	};
}
