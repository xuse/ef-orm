package jef.database.meta;

/**
 * 描述一个数据库列上的一种变化
 * 
 * @author Administrator
 * 
 */
public class ColumnChange {
	private Change type;
	private String from;
	private String to;
	
	/**
	 * 列的变化种类
	 * <p>
	 * <ul>
	 * <li>{@link #ADD_COLUMN} <br>列添加</li>
	 * <li>{@link #DROP_COLUMN} <br>列删除</li>
	 * <li>{@link #CHG_DATATYPE}<br>数据类型定义变化</li>
	 * <li>{@link #CHG_TO_NULL} <br>变更为允许NULL</li>
	 * <li>{@link #CHG_TO_NOT_NULL} <br>变更为NOT NULL</li>
	 * <li>{@link #CHG_DEFAULT} <br>变更缺省值的表达式</li>
	 * <li>{@link #CHG_DROP_DEFAULT}  <br>取消缺省值设置</li>
	 * </ul>
	 * @author jiyi
	 */
	public enum Change {
		/**
		 * 列变更种类：列添加
		 */
		ADD_COLUMN,
		/**
		 * 列变更种类： 列删除
		 */
		DROP_COLUMN,
		/**
		 * 列变更种类：数据类型变化
		 */
		CHG_DATATYPE, 
		/**
		 * 列变更种类： 变更为可NULL
		 */
		CHG_TO_NULL,
		/**
		 * 列变更种类：变更为NOT NULL
		 */
		CHG_TO_NOT_NULL, 
		/**
		 * 列变更种类：添加或修改缺省值的表达式
		 */
		CHG_DEFAULT, 
		/**
		 * 列变更种类： 取消DEFAULT设置
		 */
		CHG_DROP_DEFAULT 
	}
	/**
	 * 构造
	 * @param type
	 */
	public ColumnChange(Change type) {
		this.type = type;
	}
	/**
	 * 变更种类
	 * @return change枚举，描述变更种类
	 * @see Change
	 */
	public Change getType() {
		return type;
	}
	/**
	 * 获得变更前的描述
	 * @return 变更前描述
	 */
	public String getFrom() {
		return from;
	}
	/**
	 * 设置变更前描述
	 * @param from
	 */
	public void setFrom(String from) {
		this.from = from;
	}
	/**
	 * 获得变更后描述
	 * @return to
	 */
	public String getTo() {
		return to;
	}
	/**
	 *  设置变更后描述
	 * @param to
	 */
	public void setTo(String to) {
		this.to = to;
	}
}
