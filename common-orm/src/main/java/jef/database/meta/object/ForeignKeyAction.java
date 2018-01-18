/**
 * 
 */
package jef.database.meta.object;

/**
 * 外键的动作类型
 * 
 * @author qihongfei
 *
 */
public enum ForeignKeyAction {

	/**
	 * 如果违反外键约束会产生一个错误。如果约束被延迟，那么到事务结束检查约束时如果仍然因为存在一个引用行而违反外键约束，则仍会产生错误。这是默认值。其他的动作action都不能被延迟。
	 */
	NO_ACTION("NO ACTION"),
	/**
	 * 违反外键约束会产生一个错误。
	 */
	RESTRICT("RESTRICT"),
	/**
	 * 级联删除或更新。分别删除一个引用行或者更新一个引用列的值。
	 */
	CASCADE("CASCADE"),
	/**
	 * 设置引用列的值为null
	 */
	SET_NULL("SET NULL"), 
	/**
	 * 设置引用列为其缺省值。如果缺省值不是null,那么仍然需要被引用表中有一条记录的被引用字段的值与之匹配，否则操作会失败。
	 */
	SET_DEFAULT("SET DEFAULT"); 

	private String name;

	private ForeignKeyAction(String name) {
		this.name = name;
	}

	public static ForeignKeyAction parseName(String name) {
		for (ForeignKeyAction a : ForeignKeyAction.values()) {
			if (a.name.equals(name)) {
				return a;
			}
		}
		return null;
	}

	public String getName() {
		return this.name;
	}
}
