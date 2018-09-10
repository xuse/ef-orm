package jef.database.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jef.common.SimpleSet;
import jef.database.DbUtils;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.object.Constraint;
import jef.database.meta.object.ConstraintType;
import jef.database.meta.object.Index;
import jef.database.support.RDBMS;
import jef.tools.StringUtils;

public class DdlGeneratorImpl implements DdlGenerator {
    private DatabaseDialect profile;
    private final String BRUKETS_LEFT;
    private final String BRUKETS_RIGHT;
    private boolean commandForEach;

    public DdlGeneratorImpl(DatabaseDialect profile) {
        this.profile = profile;
        commandForEach = profile.has(Feature.ALTER_FOR_EACH_COLUMN);
        if (profile.has(Feature.BRUKETS_FOR_ALTER_TABLE)) {
            BRUKETS_LEFT = " (";
            BRUKETS_RIGHT = ")";
        } else {
            BRUKETS_LEFT = " ";
            BRUKETS_RIGHT = "";
        }
    }

    /*
     * 生成建表语句 (non-Javadoc)
     * 
     * @see
     * jef.database.SqlProcessor#toTableCreateClause(jef.database.DataObject,
     * java.lang.String)
     */
    public TableCreateSQLs toTableCreateClause(ITableMetadata meta, String tablename) {
        TableCreateSQLs result = new TableCreateSQLs();
        result.addTableMeta(tablename, meta, profile);
        return result;
    }

    //TODO
    public List<String> toViewCreateClause() {
        return null;
    }

    // ALTER [ COLUMN ] column TYPE type [ USING expression ]
    // ALTER [ COLUMN ] column SET DEFAULT expression
    // ALTER [ COLUMN ] column DROP DEFAULT
    // ALTER [ COLUMN ] column { SET | DROP } NOT NULL
    public List<String> toTableModifyClause(ITableMetadata meta, String tableName, Map<String, ColumnType> insert, List<ColumnModification> changed,
            List<String> delete) {
        List<String> sqls = new ArrayList<String>();
        if (!insert.isEmpty()) {
            if (profile.has(Feature.ONE_COLUMN_IN_SINGLE_DDL)) {// 某些数据库一次ALTER
                                                                // TABLE语句只能修改一列
                for (Entry<String, ColumnType> entry : insert.entrySet()) {
                    Set<Entry<String, ColumnType>> ss = new SimpleSet<Entry<String, ColumnType>>();
                    ss.add(entry);
                    sqls.add(toAddColumnSql(tableName, ss));
                }
            } else {
                sqls.add(toAddColumnSql(tableName, insert.entrySet()));
            }

        }

        if (!changed.isEmpty()) {
            boolean complexSyntax = profile.has(Feature.COLUMN_ALTERATION_SYNTAX);
            if (profile.has(Feature.ONE_COLUMN_IN_SINGLE_DDL) || complexSyntax) {// 某些数据库一次ALTER
                                                                                 // TABLE语句只能修改一列
                for (ColumnModification entry : changed) {
                    if (complexSyntax) {// complex operate here
                        for (ColumnChange change : entry.getChanges()) {// 要针对每种Change单独实现SQL语句,目前已知Derby和postgresql是这样的，而且两者的语法有少量差别，这里尽量用兼容写法
                            sqls.add(toChangeColumnSql(tableName, DbUtils.escapeColumn(profile, entry.getFrom().getColumnName()), change, profile));
                        }
                    } else {
                        // 简单语法时
                        sqls.add(toChangeColumnSql(tableName, Arrays.asList(entry)));
                    }
                }
            } else {
                sqls.add(toChangeColumnSql(tableName, changed));
            }
        }

        if (!delete.isEmpty()) {
            if (profile.has(Feature.ONE_COLUMN_IN_SINGLE_DDL)) {// 某些数据库一次ALTER
                                                                // TABLE语句只能修改一列
                for (String entry : delete) {
                    sqls.add(toDropColumnSql(tableName, Arrays.asList(entry)));
                }
            } else {
                sqls.add(toDropColumnSql(tableName, delete));
            }

        }
        return sqls;
    }

    /*
     * DERBY has much complexity than Oracle in modifying table columns.
     * 
     * key words must column-Name SET DATA TYPE VARCHAR(integer) | column-Name
     * SET DATA TYPE VARCHAR FOR BIT DATA(integer) | column-name SET INCREMENT
     * BY integer-constant | column-name RESTART WITH integer-constant |
     * column-name [ NOT ] NULL | column-name [ WITH | SET ] DEFAULT
     * default-value | column-name DROP DEFAULT
     */
    private String toChangeColumnSql(String tableName, String columnName, ColumnChange change, DatabaseDialect profile) {
        StringBuilder sb = new StringBuilder();
        sb.ensureCapacity(128);
        sb.append("ALTER TABLE ");
        sb.append(tableName).append(' ').append(profile.getProperty(DbProperty.MODIFY_COLUMN)).append(' ');
        sb.append(columnName).append(' ');
        String setDataType;
        String setNull;
        String setNotNull;
        if (RDBMS.postgresql == profile.getName()) {// PG
            setDataType = "TYPE";
            setNull = "DROP NOT NULL";
            setNotNull = "SET NOT NULL";
        } else if (RDBMS.derby == profile.getName()) {// DERBY
            setDataType = "SET DATA TYPE";
            setNull = "NULL";
            setNotNull = "NOT NULL";
        } else {// HSQLDB
            setDataType = "";
            setNull = "SET NULL";
            setNotNull = "SET NOT NULL";
        }
        switch (change.getType()) {
        case CHG_DATATYPE:
            sb.append(setDataType).append(' ').append(change.getTo());
            return sb.toString();
        case CHG_DEFAULT:
            sb.append("SET DEFAULT ").append(change.getTo());
            return sb.toString();
        case CHG_DROP_DEFAULT:
            sb.append("DROP DEFAULT");
            return sb.toString();
        case CHG_TO_NOT_NULL:
            sb.append(setNotNull);
            return sb.toString();
        case CHG_TO_NULL:
            sb.append(setNull);
            return sb.toString();
        default:
            throw new IllegalStateException("Unknown change type" + change.getType());
        }
    }

    private String toChangeColumnSql(String tableName, List<ColumnModification> entrySet) {
        StringBuilder sb = new StringBuilder();
        sb.ensureCapacity(128);
        sb.append("ALTER TABLE ");
        sb.append(tableName).append(' ').append(profile.getProperty(DbProperty.MODIFY_COLUMN)).append(BRUKETS_LEFT);
        int n = 0;
        for (ColumnModification entry : entrySet) {
            if (n > 0) {
                sb.append(",\n");
                if (commandForEach) {
                    sb.append(profile.getProperty(DbProperty.MODIFY_COLUMN)).append(' ');
                }
            }
            sb.append(DbUtils.escapeColumn(profile, entry.getFrom().getColumnName())).append(' ');
            sb.append(profile.getCreationComment(entry.getNewColumn(), false));
            n++;
        }
        sb.append(BRUKETS_RIGHT);
        return sb.toString();
    }

    private String toDropColumnSql(String tableName, List<String> entrySet) {
        StringBuilder sb = new StringBuilder();
        sb.ensureCapacity(128);
        sb.append("ALTER TABLE ");
        sb.append(tableName).append(' ').append(profile.getProperty(DbProperty.DROP_COLUMN)).append(BRUKETS_LEFT);
        int n = 0;
        for (String entry : entrySet) {
            if (n > 0) {
                sb.append(",\n");
                if (commandForEach) {
                    sb.append(profile.getProperty(DbProperty.DROP_COLUMN)).append(' ');
                }
            }
            sb.append(entry);
            n++;
        }
        sb.append(BRUKETS_RIGHT);
        return sb.toString();
    }

    private String toAddColumnSql(String tableName, Set<Entry<String, ColumnType>> entrySet) {
        StringBuilder sb = new StringBuilder();
        sb.ensureCapacity(128);
        sb.append("ALTER TABLE ");
        sb.append(tableName).append(' ').append(profile.getProperty(DbProperty.ADD_COLUMN)).append(BRUKETS_LEFT);
        int n = 0;
        for (Entry<String, ColumnType> entry : entrySet) {
            if (n > 0) {
                sb.append(", ");
                if (commandForEach) {
                    sb.append(profile.getProperty(DbProperty.ADD_COLUMN)).append(' ');
                }
            }
            sb.append(entry.getKey()).append(' ');
            sb.append(profile.getCreationComment(entry.getValue(), true));
            n++;
        }
        sb.append(BRUKETS_RIGHT);
        return sb.toString();
    }

    private static final String DROP_CONSTRAINT_SQL = "alter table %1$s drop constraint %2$s";

    @Override
    public String getDropConstraintSql(String tablename, String constraintName) {
        String template = this.profile.getProperty(DbProperty.DROP_FK_PATTERN);
        if (StringUtils.isEmpty(template)) {
            template = DROP_CONSTRAINT_SQL;
        }
        return String.format(template, tablename, constraintName);
    }


    private static final String DROP_INDEX_SQL = "drop index %2$s";
    
    public String deleteIndex(Index index) {
    	
    	StringBuilder sb = new StringBuilder();
    	// 特殊语句
    	String dropIndexTablePattern = profile.getProperty(DbProperty.DROP_INDEX_TABLE_PATTERN);
    	if(dropIndexTablePattern != null){
    		sb.append("DROP INDEX " + String.format(dropIndexTablePattern, index.getIndexName(), index.getTableName()));
    	}else{
    		sb.append(String.format(DROP_INDEX_SQL, index.getTableName(), index.getIndexName()));
    	}
        return sb.toString();
    }

    public String addConstraint(Constraint con){
    	
    	StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE ");
		sb.append(con.getTableName());
		sb.append(" ADD CONSTRAINT ");
		sb.append(con.getName());
		
		if(ConstraintType.R == con.getType()){
			// 外键约束
			sb.append(" FOREIGN KEY");
			sb.append("(");
			sb.append(StringUtils.join(con.getColumns(), ","));
			sb.append(")");
			sb.append(" REFERENCES ");
			sb.append(con.getRefTableSchema());
			sb.append(".");
			sb.append(con.getRefTableName());
			sb.append("(");
			sb.append(StringUtils.join(con.getRefColumns(), ","));
			sb.append(")");
			if(con.getMatchType() != null){ // 外键匹配模式
				sb.append(" MATCH ");
				sb.append(con.getMatchType().name());
			}
			if(con.getUpdateRule() != null){ // 外键更新时策略
				sb.append(" ON UPDATE ");
				sb.append(con.getUpdateRule().getName());
			}
			if(con.getDeleteRule() != null){ // 外键删除时策略
				sb.append(" ON DELETE ");
				sb.append(con.getDeleteRule().getName());
			}
			
		}else if(ConstraintType.C == con.getType()){
			// 检查约束
			sb.append(" CHECK(");
			sb.append(con.getCheckClause()); // 检查约束具体内容
			sb.append(")");
		}else{
			
			// 主键约束
			if(ConstraintType.P == con.getType()){
				sb.append(" PRIMARY KEY");
				
				// 唯一约束
			}else if(ConstraintType.U == con.getType()){
				sb.append(" UNIQUE");
			}
			sb.append("(");
			sb.append(StringUtils.join(con.getColumns(), ","));
			sb.append(")");
		}
		if(!con.isEnabled()){ // 创建时不启用(oracle)
			sb.append(" DISABLE ");
		}
    	
    	return sb.toString();
    }

    /**
     * 生成删除约束的语句(与getDropConstraintSql有所不同)
     * @param con 约束对象
     * @return SQL语句
     */
    public String deleteConstraint(Constraint con){
    	
    	StringBuilder sb = new StringBuilder();
    	
    	// 删除外键约束
    	if(ConstraintType.R == con.getType()){
    		String fkDeleteTemplate = profile.getProperty(DbProperty.DROP_FK_PATTERN); 
    		if(fkDeleteTemplate == null){
    			fkDeleteTemplate = DROP_CONSTRAINT_SQL;
    		}
    		sb.append(String.format(fkDeleteTemplate, con.getTableName(), con.getName()));
    		
    	}else if(ConstraintType.U == con.getType()){
    		// 删除唯一键约束
	    	if(RDBMS.mysql == profile.getName() || RDBMS.mariadb == profile.getName()){
	    		String dropIndexTablePattern = profile.getProperty(DbProperty.DROP_INDEX_TABLE_PATTERN);
	    		sb.append("DROP INDEX " + String.format(dropIndexTablePattern, con.getName(), con.getTableName()));
    		}else{
    			sb.append(String.format(DROP_CONSTRAINT_SQL, con.getTableName(), con.getName()));
    		}
    	}else if(ConstraintType.P == con.getType()){
    		// 删除主键约束
    		if(RDBMS.mysql == profile.getName() || RDBMS.mariadb == profile.getName()){
    			// 如果是自增主键，先删除自增 TODO 
    			sb.append(String.format(DROP_CONSTRAINT_SQL.replace("constraint", ""), con.getTableName(), "PRIMARY KEY"));
    		}else{
    			sb.append(String.format(DROP_CONSTRAINT_SQL, con.getTableName(), con.getName()));
    		}
    	}else{
    		// 删除其他约束
    		sb.append(String.format(DROP_CONSTRAINT_SQL, con.getTableName(), con.getName()));
    	}
    	return sb.toString();
    }
    
    // not used
    public String modifyPrimaryKey(Constraint conBefore, Constraint conAfter){
    	StringBuilder sb = new StringBuilder();
    	sb.append("ALTER TABLE ");
    	sb.append(conBefore.getTableName());
    	sb.append(" DROP PRIMARY KEY, ADD PRIMARY KEY");
    	sb.append("(");
		sb.append(StringUtils.join(conAfter.getColumns(), ","));
		sb.append(")");
    	return sb.toString();
    }
}
