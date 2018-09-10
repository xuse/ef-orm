package com.querydsl.sql;

/**
 * 打破访问限制.
 */
public class QueryDSLVistor {
    public static Configuration getConfiguration(ProjectableSQLQuery sqlQuery) {
        return sqlQuery.configuration;
    }

    public static SQLSerializer serialize(ProjectableSQLQuery sqlQuery, boolean forCountRow) {
        return sqlQuery.serialize(forCountRow);
    }
}
