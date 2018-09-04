package com.querydsl.sql.dml;

import com.querydsl.sql.Configuration;

/**
 * 打破访问限制.
 */
public class QueryDSLDMLVistor {

    public static Configuration getConfiguration(AbstractSQLClause sqlClause) {
        return sqlClause.configuration;
    }
}
