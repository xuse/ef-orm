package jef.database.annotation;

/**
 * 对应时间自动自动生成功能 有四种时间自动生成规则
 * 
 */
public enum DateGenerateType {
	/**
	 * 创建时使用数据库时间戳
	 */
	created(false, false){
		public long generateLong() {
			throw new UnsupportedOperationException();
		}
	},
	/**
	 * 修改时使用数据库时间戳
	 */
	modified(true, false){
		public long generateLong() {
			throw new UnsupportedOperationException();
		}
	},
	/**
	 * 创建时使用Java时间戳
	 */
	created_sys(false, true){
		public long generateLong() {
			return System.currentTimeMillis();
		}
	},
	/**
	 * 修改时使用Java时间戳
	 */
	modified_sys(true, true) {
		public long generateLong() {
			return System.currentTimeMillis();
		}
	},
	/**
	 * 修改使用JavaNano时间戳
	 */
	modified_nano(true, true){
		public long generateLong() {
			return System.nanoTime();
		}
	};

	/**
	 * 是由Java生成时间还是数据库生成时间
	 */
	public boolean isJavaTime;
	/**
	 * 是否每次修改时都要刷新时间
	 */
	public boolean isModify;

	/**
	 * 如果是由java生成时间，并且精度为转换为long的毫秒数来管理（因为Oracle和MySQL的时间精度都只到秒）
	 * 则生成当前时间戳
	 * @return
	 */
	public abstract long generateLong();

	private DateGenerateType(boolean isModify, boolean isJavaTime) {
		this.isModify = isModify;
		this.isJavaTime = isJavaTime;
	}
}
