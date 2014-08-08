package jef.database.meta;


public interface IReferenceAllTable extends ISelectProvider,AliasProvider {
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
	 * 是否延迟加载Lob字段
	 * @return
	 */
	boolean isLazyLob();

}
