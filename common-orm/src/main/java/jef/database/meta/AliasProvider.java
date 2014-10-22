package jef.database.meta;

import jef.database.Field;
import jef.database.dialect.DatabaseDialect;
import jef.database.query.SqlContext;
import jef.tools.StringUtils;

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
	String getSelectedAliasOf(Field f, DatabaseDialect dialect, String schema);
	
	/**
	 * 结果时获得列别名，要求返回大写
	 * @param f
	 * @param dialect
	 * @param schema
	 * @return
	 */
	String getResultAliasOf(Field f,DatabaseDialect dialect,String schema);
	
	public static final AliasProvider DEFAULT=new AliasProvider(){
		public String getSelectedAliasOf(Field f, DatabaseDialect profile, String alias) {
			String fieldName = f.name();
			return profile.getColumnNameToUse(StringUtils.isEmpty(alias) ? StringUtils.concat(alias, SqlContext.DIVEDER, fieldName) : fieldName);
		}

		@Override
		public String getResultAliasOf(Field f, DatabaseDialect dialect, String alias) {
			String fieldName = f.name();
			return StringUtils.isEmpty(alias) ? StringUtils.concat(alias, SqlContext.DIVEDER, fieldName).toUpperCase() : fieldName.toUpperCase();
		}
	};
}
