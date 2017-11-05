package com.github.geequery.test.common;

import jef.database.QB;
import jef.database.dialect.ColumnType;
import jef.database.meta.TupleMetadata;

public class TestUtils {
	
	public static final TupleMetadata URM_SERVICE;
	public static final TupleMetadata URM_GROUP;
	
	static{
		TupleMetadata meta= new TupleMetadata("URM_SERVICE_1");
		meta.addColumn("id", new ColumnType.AutoIncrement(8));
		meta.addColumn("name", new ColumnType.Varchar(100));
		meta.addColumn("pname", new ColumnType.Varchar(100));
		meta.addColumn("flag", new ColumnType.Boolean());
		meta.addColumn("photo", new ColumnType.Blob());
		meta.addColumn("groupid", new ColumnType.Int(10));
//		meta.addIndex("pname", "unique");
//		meta.addIndex(new String[]{"groupid","pname","name","flag"}, "unique");
		TupleMetadata GroupTable = new TupleMetadata("URM_GROUP");
		GroupTable.addColumn("id", new ColumnType.AutoIncrement(8));
		GroupTable.addColumn("serviceId", new ColumnType.Int(8));
		GroupTable.addColumn("name", new ColumnType.Varchar(100));
		GroupTable.addCascadeOneToMany("services", meta, QB.on(GroupTable.f("id"),meta.f("groupid")));
		
		URM_SERVICE=meta;
		URM_GROUP=GroupTable;
	}
}
