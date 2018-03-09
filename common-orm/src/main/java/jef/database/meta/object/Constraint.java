package jef.database.meta.object;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.google.common.base.Objects;

import jef.tools.ArrayUtils;

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

	/**
	 * 约束的catalog
	 */
	private String catalog;

	/**
	 * 约束所在schema
	 */
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
	private boolean initiallyDeferred;
	
	/**
	 * 约束字段列表
	 */
	private List<String> columns = new ArrayList<String>();
	
	/**
	 * 外键参照表所在schema
	 */
	private String refTableSchema;
	
	/**
	 * 外键参照表
	 */
	private String refTableName;
	
	/**
	 * 外键参照字段列表
	 */
	private List<String> refColumns = new ArrayList<String>();

	/**
	 * 外键更新规则
	 */
	private ForeignKeyAction updateRule;
	
	/**
	 * 外键删除规则
	 */
	private ForeignKeyAction deleteRule;
	
	/**
	 * 外键匹配类型
	 */
	private ForeignKeyMatchType matchType;
	
	/**
	 * 检查约束定义
	 */
	private String checkClause;
	
	/**
	 * 约束是否启用
	 */
	private boolean enabled = true;

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

	public boolean isInitiallyDeferred() {
		return initiallyDeferred;
	}

	public void setInitiallyDeferred(boolean initiallyDeferred) {
		this.initiallyDeferred = initiallyDeferred;
	}

	public List<String> getColumns() {
		return columns;
	}

	public void setColumns(List<String> columns) {
		this.columns = columns;
	}

	public String getRefTableName() {
		return refTableName;
	}

	public void setRefTableName(String refTableName) {
		this.refTableName = refTableName;
	}

	public List<String> getRefColumns() {
		return refColumns;
	}

	public void setRefColumns(List<String> refColumns) {
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

	public String getRefTableSchema() {
		return refTableSchema;
	}

	public void setRefTableSchema(String refTableSchema) {
		this.refTableSchema = refTableSchema;
	}

	public String getCheckClause() {
		return checkClause;
	}

	public void setCheckClause(String checkClause) {
		this.checkClause = checkClause;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == this) return true;
		if(!(obj instanceof Constraint)) return false;
		Constraint con = (Constraint)obj;
//		if(!Objects.equal(this.catalog, con.catalog))return false;
		if(!Objects.equal(this.schema, con.schema))return false;
		if(!Objects.equal(this.name, con.name))return false;
//		if(!Objects.equal(this.tableCatalog, con.tableCatalog))return false;
		if(!Objects.equal(this.tableSchema, con.tableSchema))return false;
		if(!Objects.equal(this.tableName, con.tableName))return false;
		if(!Objects.equal(this.type, con.type))return false;
		if(!Objects.equal(this.deferrable, con.deferrable))return false;
		if(!Objects.equal(this.initiallyDeferred, con.initiallyDeferred))return false;
		if(!ArrayUtils.equals(this.columns.toArray(), con.columns.toArray()))return false;
		if(!Objects.equal(this.refTableName, con.refTableName))return false;
		if(!ArrayUtils.equals(this.refColumns.toArray(), con.refColumns.toArray()))return false;
		if(!Objects.equal(this.updateRule, con.updateRule))return false;
		if(!Objects.equal(this.deleteRule, con.deleteRule))return false;
		if(!Objects.equal(this.matchType, con.matchType))return false;
		if(!Objects.equal(this.enabled, con.enabled))return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		int result = 17;  
		result = result * 31 + name.hashCode();  
		result = result * 31 + tableName.hashCode();  
		return result; 
	}
}
