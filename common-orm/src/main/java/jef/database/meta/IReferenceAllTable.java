package jef.database.meta;

import jef.database.Field;
import jef.database.dialect.DatabaseDialect;

public interface IReferenceAllTable extends ISelectProvider {
	// /////////////////////////////// 全引用
	/**
	 * 当拼装返回结果时使用； 当此Field描述对应一张表中的多列选择时（比如 t.*），此方法返回一个类， 表示这张表中的字段映射的类。
	 * 如果当前描述对应单独的一个列，此方法将返回null
	 * 
	 * @return
	 */
	ITableMetadata getFullModeTargetType();

	/**
	 * 不使用别名，使用t.*方式
	 */
	String simpleModeSql(String tableAlias);

	/**
	 * 全对象引用模式下可以提供各个字段的别名 如果返回null，将不予拼装
	 * 
	 * @param f
	 * @param profile
	 * @param schema
	 * @return
	 */
	String getSelectedAliasOf(Field f, DatabaseDialect profile, String schema);

	boolean isLazyLob();

}
