package com.github.geequery.extension.querydsl;

import jef.database.DataObject;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueryDSLTables {
    private static Map<Class, SQLRelationalPath> sqlRelationalPathBaseMap = new ConcurrentHashMap<>();

    public static void initRelationalPathBase(Map<Class<?>, ITableMetadata> classMap) {
        for (Map.Entry<Class<?>, ITableMetadata> entry : classMap.entrySet()) {
            SQLRelationalPath pb = new SQLRelationalPath(entry.getKey());
            sqlRelationalPathBaseMap.put(entry.getKey(), pb);
        }
    }

    public static <T extends DataObject> SQLRelationalPath<T> relationalPathBase(Class<T> clz) {
        AbstractMetadata tm = MetaHolder.getMeta(clz);
        SQLRelationalPath pb = relationalPathBase(tm);
        return pb;
    }
    
    
    public static <T> SQLRelationalPath<T> relationalPathBase2(Class<T> clz) {
        AbstractMetadata tm = MetaHolder.getMeta(clz);
        SQLRelationalPath pb = relationalPathBase(tm);
        return pb;
    }

    public static <T extends DataObject> SQLRelationalPath<T> relationalPathBase(AbstractMetadata tm) {
        SQLRelationalPath pb = sqlRelationalPathBaseMap.get(tm.getThisType());
        if (pb == null) {
            pb = new SQLRelationalPath(tm.getThisType());
            sqlRelationalPathBaseMap.put(tm.getThisType(), pb);
        }
        return pb;
    }
}
