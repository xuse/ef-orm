package jef.database.meta.object;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * 描述一个数据库中的Constraint
 * 
 * @author jiyi
 * 
 *         MS SQLServer系统表 据说和MYSQL差不多：SELECT * FROM
 *         information_schema.TABLE_CONSTRAINTS
 * 
 * 
 *         MySQL系统表: SELECT * FROM information_schema.TABLE_CONSTRAINTS
 *
 *        
 *         Oracle系统表： SELECT * FROM all_CONSTRAINTS Oracle约束的种类 C Check on a
 *         table Column O Read Only on a view P Primary Key R Referential AKA
 *         Foreign Key U Unique Key V Check Option on a view
 */
public class Constraint {

	private String catalog;

	private String schema;

	/**
	 * 约束名
	 */
	private String name;

	/**
	 * 约束所在表的catalog
	 */
	private String tableCatalog;
	/**
	 * 约束所在表所在schema
	 */
	private String tableSchema;
	/**
	 * 约束所在表名
	 */
	private String tableName;

	/**
	 * 约束类型
	 */
	private ConstraintType type;
	/**
	 * 检测延迟
	 */
	private boolean deferrable;
	/**
	 * 检测延迟
	 */
	private boolean initiallyDeferrable;
	
	/**
	 * 约束字段列表
	 */
	private List<Column> columns; // 
	
	/**
	 * 外键参照表
	 */
	private String refTableName; // 
	
	/**
	 * 外键参照字段列表
	 */
	private List<Column> refColumns; // 
	
	/**
	 * 外键更新规则
	 */
	private ForeignKeyAction updateRule; // 外键更新规则
	
	/**
	 * 外键删除规则
	 */
	private ForeignKeyAction deleteRule; // 外键删除规则
	
	/**
	 * 外键匹配类型
	 */
	private ForeignKeyMatchType matchType;
	
	/**
	 * 约束是否启用
	 */
	private boolean enabled; // 是否启用

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTableCatalog() {
		return tableCatalog;
	}

	public void setTableCatalog(String tableCatalog) {
		this.tableCatalog = tableCatalog;
	}

	public String getTableSchema() {
		return tableSchema;
	}

	public void setTableSchema(String tableSchema) {
		this.tableSchema = tableSchema;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public ConstraintType getType() {
		return type;
	}

	public void setType(ConstraintType type) {
		this.type = type;
	}

	public boolean isDeferrable() {
		return deferrable;
	}

	public void setDeferrable(boolean deferrable) {
		this.deferrable = deferrable;
	}

	public boolean isInitiallyDeferrable() {
		return initiallyDeferrable;
	}

	public void setInitiallyDeferrable(boolean initiallyDeferrable) {
		this.initiallyDeferrable = initiallyDeferrable;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

	public String getRefTableName() {
		return refTableName;
	}

	public void setRefTableName(String refTableName) {
		this.refTableName = refTableName;
	}

	public List<Column> getRefColumns() {
		return refColumns;
	}

	public void setRefColumns(List<Column> refColumns) {
		this.refColumns = refColumns;
	}

	public ForeignKeyAction getUpdateRule() {
		return updateRule;
	}

	public void setUpdateRule(ForeignKeyAction updateRule) {
		this.updateRule = updateRule;
	}

	public ForeignKeyAction getDeleteRule() {
		return deleteRule;
	}

	public void setDeleteRule(ForeignKeyAction deleteRule) {
		this.deleteRule = deleteRule;
	}

	public ForeignKeyMatchType getMatchType() {
		return matchType;
	}

	public void setMatchType(ForeignKeyMatchType matchType) {
		this.matchType = matchType;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
}
