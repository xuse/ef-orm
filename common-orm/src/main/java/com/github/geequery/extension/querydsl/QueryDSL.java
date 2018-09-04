package com.github.geequery.extension.querydsl;

import com.querydsl.sql.*;
import com.querydsl.sql.dml.AbstractSQLClause;
import com.querydsl.sql.dml.QueryDSLDMLVistor;

public class QueryDSL {
    /**
     * 获取查询总数的接口
     * @param query
     * @return
     */
    public static String getCountSql(ProjectableSQLQuery query) {
        SQLSerializer serializer = serialize(query, true);
        String countSql = serializer.toString();
        return countSql;
    }

    /**
     * ProjectableSQLQuery中的protected SQLSerializer serialize(boolean forCountRow)方法
     * 执行sql的serialize方法
     * @param query
     * @param forCountRow
     * @return
     */
    public static SQLSerializer serialize(ProjectableSQLQuery query, boolean forCountRow) {
        return QueryDSLVistor.serialize(query, forCountRow);
    }

    public static Configuration getConfigurationQuery(AbstractSQLQuery query) {
        Configuration c = QueryDSLVistor.getConfiguration(query);
        return c;
    }

    public static Configuration getConfigurationDML(AbstractSQLClause query) {
        Configuration c = QueryDSLDMLVistor.getConfiguration(query);
        return c;
    }

}
