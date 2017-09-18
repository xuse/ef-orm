package jef.database.meta;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
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
	String getSelectedAliasOf(ColumnMapping f, DatabaseDialect dialect, String schema);
	
	/**
	 * 结果时获得列别名，要求返回大写
	 * @param f
	 * @param dialect
	 * @param schema
	 * @return
	 */
	String getResultAliasOf(ColumnMapping f,String schema);
	
	public static final AliasProvider DEFAULT=new AliasProvider(){
		public String getSelectedAliasOf(ColumnMapping f, DatabaseDialect profile, String alias) {
			String fieldName = f.fieldName();
			return profile.getObjectNameToUse(StringUtils.isEmpty(alias) ?fieldName: StringUtils.concat(alias, SqlContext.DIVEDER, fieldName));
		}

		@Override
		public String getResultAliasOf(ColumnMapping f, String alias) {
			String fieldName = f.fieldName();
			return StringUtils.isEmpty(alias) ? fieldName.toUpperCase():StringUtils.concat(alias, SqlContext.DIVEDER, fieldName).toUpperCase() ;
		}
	};
}
