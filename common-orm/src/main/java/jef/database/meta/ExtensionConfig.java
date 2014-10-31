package jef.database.meta;



public interface ExtensionConfig {
	/**
	 * 扩展名
	 * @return
	 */
	String getName();

	/**
	 * 获得关于扩充字段部分的元数据描述。
	 * @return
	 */
	TupleMetadata getExtensionMeta();

	/**
	 * 获得合并后所有字段的元数据描述
	 * @return
	 */
	AbstractMetadata getMeta();
	
	/**
	 * 是否为动态表实现
	 * @return
	 */
	boolean isDynamicTable();

	void flush(DynamicMetadata meta);
}
