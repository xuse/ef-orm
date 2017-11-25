package jef.database.meta.object;

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

	@javax.persistence.Column(name = "CONSTRAINT_CATALOG")
	private String catalog;

	@javax.persistence.Column(name = "CONSTRAINT_SCHEMA")
	private String schema;

	@javax.persistence.Column(name = "CONSTRAINT_NAME")
	private String name;

	@javax.persistence.Column(name = "TABLE_CATALOP")
	private String tableCatalog;
	@javax.persistence.Column(name = "TABLE_SCHEMA")
	private String tableSchema;
	@javax.persistence.Column(name = "TABLE_NAME")
	private String tableName;

	private ConstraintType type;

	private boolean deferrable;

	private boolean initiallyDeferrable;

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
}
