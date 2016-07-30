package jef.database.annotation;

/**
 * 对应时间自动自动生成功能 有四种时间自动生成规则
 *
 */
public enum DateGenerateType {
	/**
	 * 创建时使用数据库时间戳
	 */
	created(false, false),
	/**
	 * 修改时使用数据库时间戳
	 */
	modified(true, false),
	/**
	 * 创建时使用Java时间戳
	 */
	created_sys(false, true),
	/**
	 * 修改时使用Java时间戳
	 */
	modified_sys(true, true);
	
	public boolean isJavaTime;
	public boolean isModify;

	DateGenerateType(boolean isModify, boolean isJavaTime) {
		this.isModify = isModify;
		this.isJavaTime = isJavaTime;
	}
}
