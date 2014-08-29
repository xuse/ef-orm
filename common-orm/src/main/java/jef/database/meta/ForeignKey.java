package jef.database.meta;

import java.sql.DatabaseMetaData;

import javax.persistence.Column;

/**
 * 描述外键信息
 * @author Administrator
 *
 */
public class ForeignKey {
	//删除被引用的记录引发的策略，默认为禁止删除，不同数据库的返回值常量不同
	@Column(name="FKTABLE_SCHEM")
	private String fromSchema;
	@Column(name="FKTABLE_NAME")
	private String fromTable;
	@Column(name="FKCOLUMN_NAME")
	private String fromColumn;
	@Column(name="PKTABLE_SCHEM")
	private String referenceSchema;
	@Column(name="PKTABLE_NAME")
	private String referenceTable;
	@Column(name="PKCOLUMN_NAME")
	private String referenceColumn;
	@Column(name="KEY_SEQ")
	private int keySeq;
	
	/**
	 * 删除规则：当被引用的数据删除时：
	 * 默认： 禁止删除/on　delete　cascade: 删除那些引用此记录的记录/ on delete set null:  清空那些引用此记录的字段
	 * 
	 * importedKeyNoAction - do not allow delete of primary key if it has been imported 
	 * importedKeyCascade - delete rows that import a deleted key 
	 * importedKeySetNull - change imported key to NULL if its primary key has been deleted 
	 * importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility) 
	 * importedKeySetDefault - change imported key to default if its primary key has been deleted
	 */
	@Column(name="DELETE_RULE")
	private int deleteRule;//		
	
	/**
	 * 更新规则：当被引用的记录键值更新时
	 * 默认：禁止更新 /on　update　cascade: 删除那些引用此记录的记录
	 * 注意：Oracle不支持此操作，因此Oracle驱动返回的值总是0
	 * 
	 * importedNoAction - do not allow update of primary key if it has been imported 
	 * importedKeyCascade - change imported key to agree with primary key update 
	 * importedKeySetNull - change imported key to NULL if its primary key has been updated 
	 * importedKeySetDefault - change imported key to default values if its primary key has been updated 
	 * importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility) 
	 */
	@Column(name="UPDATE_RULE")
	private int updateRule;
	
	/**
	 * 外键名称
	 */
	@Column(name="FK_NAME")
	private String name;
	
	/**
	 * 主键名称
	 */
	@Column(name="PK_NAME")
	private String pkName;//??
	
	/**
	 * 	Initially immediate(default) - constraint validated at statement level
	 *  Initially deferred - constraint validated at commit level
	 */
	@Column(name="DEFERRABILITY")
	private int deferrAbility; 
	
	public String getFromTable() {
		return fromTable;
	}
	public void setFromTable(String fromTable) {
		this.fromTable = fromTable;
	}
	public String getFromColumn() {
		return fromColumn;
	}
	public void setFromColumn(String fromColumn) {
		this.fromColumn = fromColumn;
	}
	public String getReferenceTable() {
		return referenceTable;
	}
	public void setReferenceTable(String referenceTable) {
		this.referenceTable = referenceTable;
	}
	public String getReferenceColumn() {
		return referenceColumn;
	}
	public void setReferenceColumn(String referenceColumn) {
		this.referenceColumn = referenceColumn;
	}
	public int getKeySeq() {
		return keySeq;
	}
	public void setKeySeq(int keySeq) {
		this.keySeq = keySeq;
	}
	public int getDeleteRule() {
		return deleteRule;
	}
	public void setDeleteRule(int deleteRule) {
		this.deleteRule = deleteRule;
	}
	public int getUpdateRule() {
		return updateRule;
	}
	public void setUpdateRule(int updateRule) {
		this.updateRule = updateRule;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPkName() {
		return pkName;
	}
	public void setPkName(String pkName) {
		this.pkName = pkName;
	}
	public int getDeferrAbility() {
		return deferrAbility;
	}
	public void setDeferrAbility(int deferrAbility) {
		this.deferrAbility = deferrAbility;
	}
	public String getFromSchema() {
		return fromSchema;
	}
	public void setFromSchema(String fromSchema) {
		this.fromSchema = fromSchema;
	}
	public String getReferenceSchema() {
		return referenceSchema;
	}
	public void setReferenceSchema(String referenceSchema) {
		this.referenceSchema = referenceSchema;
	}
	
	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append(name).append("(");
		sb.append(fromTable).append('.').append(fromColumn);
		sb.append("->").append(referenceTable).append('.').append(referenceColumn).append(')');
		sb.append("D:").append(this.deleteRule);
		sb.append("U:").append(this.updateRule);
		sb.append("F:").append(this.deferrAbility);
		return sb.toString();
	}

	/**
	 * 生成删除外键的SQL语句
	 * @return
	 */
	public String toDropSql(){
		StringBuilder sb=new StringBuilder();
		sb.append("alter table ").append(fromTable).append(" drop constraint ").append(name);
		return sb.toString();
	}
	
	/**
	 * 生成修改表增加外键的SQL语句
	 * @return
	 */
	public String toCreateSql(){
		StringBuilder sb=new StringBuilder();
		sb.append("alter table ").append(fromTable).append(" add constraint ").append(name);
		sb.append(" foreign key (").append(fromColumn).append(") references ");
		sb.append(referenceTable).append('(').append(referenceColumn).append(')');
		if(this.deleteRule==DatabaseMetaData.importedKeyCascade){
			sb.append(" on delete cascade"); //		on　delete　cascade: 删除那些引用此记录的记录
		}else if(deleteRule==DatabaseMetaData.importedKeySetNull){
			sb.append(" on delete set null");//    on delete set null:  清空那些引用此记录的字段
		}
		if(this.updateRule==DatabaseMetaData.importedKeyCascade){
			sb.append(" on update cascade");//    ON UPDATE CASCADE : 更新时也更新那些引用此字段的记录
		}else if(this.updateRule==DatabaseMetaData.importedKeySetNull){
			sb.append(" on update set null");//    ON UPDATE CASCADE : 更新时也更新那些引用此字段的记录
		}
		if(this.deferrAbility==DatabaseMetaData.importedKeyInitiallyDeferred){
			sb.append(" initially deferred");
		}else if(this.deferrAbility==DatabaseMetaData.importedKeyInitiallyImmediate){
			sb.append(" Initially immediate");
		}
		return sb.toString();
	}
}
