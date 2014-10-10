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
	 * @param f 字段
	 * @param dialect 数据库方言
	 * @param schema  表的别名
	 * @param forSelect 当生成查询语句时true，当拼装结果时false
	 * @return
	 */
	String getSelectedAliasOf(Field f, DatabaseDialect dialect, String schema,boolean forSelect);
	
	public static final AliasProvider DEFAULT=new AliasProvider(){
		public String getSelectedAliasOf(Field f, DatabaseDialect profile, String schema,boolean forSelect) {
			return DbUtils.getDefaultColumnAlias(f, profile, schema);
		}
	};
}
