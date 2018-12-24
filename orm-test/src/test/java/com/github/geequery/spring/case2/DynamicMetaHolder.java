package com.github.geequery.spring.case2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jef.database.dialect.ColumnType;
import jef.database.meta.TupleMetadata;
import jef.tools.StringUtils;

import org.springframework.util.Assert;

public class DynamicMetaHolder {
	private DynamicMetaHolder(){};
	private static final Map<String, TupleMetadata> map=new ConcurrentHashMap<String, TupleMetadata>();
	
	/**
	 * 获取metadata
	 * @param d
	 * @return
	 */
	public final static TupleMetadata getMeta(String name) {
		if(name==null)return null;
		name=name.trim();
		if(name.length()==0)return null;
		return map.get(name.toUpperCase());
	}
	
	/**
	 * 获取metadata
	 * @param d
	 * @return
	 */
	public final static void putMeta(TupleMetadata metadata) {
		String name=metadata.getName();
		name=name.trim().toUpperCase();
		map.put(name, metadata);
	}
	
	static{
		TupleMetadata meta = new TupleMetadata("URM_SERVICE_1");
		meta.addColumn("id", new ColumnType.AutoIncrement(8));
		meta.addColumn("name", new ColumnType.Varchar(100));
		meta.addColumn("pname", new ColumnType.Varchar(100));
		meta.addColumn("flag", new ColumnType.Boolean());
		meta.addColumn("photo", new ColumnType.Blob());
		meta.addColumn("groupid", new ColumnType.Int(10));
		putMeta(meta);
	}
}
