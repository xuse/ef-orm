package jef.database;

import jef.database.meta.ITableMetadata;

/**
 * 所有自带TableMetadata信息的字段
 * @author jiyi
 *
 */
public interface MetadataContainer {
	/**
	 * 获得所在表的Metadata信息。
	 * @return
	 */
	public ITableMetadata getMeta();
}
