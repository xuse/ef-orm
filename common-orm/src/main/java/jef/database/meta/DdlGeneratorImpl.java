package jef.database.meta;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jef.common.SimpleSet;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.def.IndexDef;
import jef.database.meta.object.Index;
import jef.database.meta.object.Index.IndexItem;
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
    public TableCreateStatement toTableCreateClause(ITableMetadata meta, String tablename) {
        TableCreateStatement result = new TableCreateStatement();
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
                ;
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
            sb.append(profile.getCreationComment(entry.getNewColumn(), true));
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


    public String addIndex(IndexDef index, ITableMetadata meta,String tablename) {
        int n = tablename.indexOf('.');
        String tableschema = null;
        if (n > -1) {
            tableschema = tablename.substring(0, n);
            tablename = tablename.substring(n + 1);
        }
        
        List<Field> fields = new ArrayList<Field>();
        List<IndexItem> columns = new ArrayList<IndexItem>();
        for (String fieldname : index.getColumns()) {
            boolean asc = true;
            if (fieldname.toLowerCase().endsWith(" desc")) {
                asc = false;
                fieldname = fieldname.substring(0, fieldname.length() - 5).trim();
            }
            Field field = meta.getField(fieldname);
            if (field == null) {
                field = new FBIField(fieldname);
                columns.add(new IndexItem(fieldname, asc, 0));
            } else {
                String columnName = meta.getColumnName(field, profile, true);
                columns.add(new IndexItem(columnName, asc, 0));
            }
            fields.add(field);
        }

        String indexName = index.getName();
        if (StringUtils.isEmpty(indexName)) {
            StringBuilder iNameBuilder = new StringBuilder();
            iNameBuilder.append("IDX_").append(StringUtils.truncate(StringUtils.removeChars(tablename, '_'), 14));
            int maxField = ((28 - iNameBuilder.length()) / index.getColumns().length) - 1;
            if (maxField < 1)
                maxField = 1;
            for (Field field : fields) {
                iNameBuilder.append('_');
                if (field instanceof FBIField) {
                    iNameBuilder.append(StringUtils.truncate(StringUtils.randomString(), maxField));
                } else {
                    iNameBuilder.append(StringUtils.truncate(meta.getColumnDef(field).getColumnName(profile, false), maxField));
                }
            }
            indexName = iNameBuilder.toString();
        }
        if (indexName.length() > 30)
            indexName = indexName.substring(0, 30);

        Index indexobj = new Index(indexName);
        indexobj.setTableSchema(tableschema);
        indexobj.setTableName(tablename);
        indexobj.setUnique(index.isUnique());
        indexobj.setUserDefinition(index.getDefinition());
        if (index.isClustered()) {
            indexobj.setType(DatabaseMetaData.tableIndexClustered);
        }
        for (IndexItem c : columns) {
            indexobj.addColumn(c.column, c.asc);
        }
        return indexobj.toCreateSql(profile);
    }

    public String deleteIndex(Index index) {
        //TODO
        return null;
    }


}
