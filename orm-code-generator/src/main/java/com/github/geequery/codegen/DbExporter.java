package com.github.geequery.codegen;

import java.io.File;

import jef.accelerator.asm.ClassReader;
import jef.database.DataObject;
import jef.database.DbClient;
import jef.tools.ClassScanner;
import jef.tools.InitDataExporter;
import jef.tools.resource.IResource;

import com.github.geequery.orm.annotation.InitializeData;

public class DbExporter {

	/**
	 * 将数据库中的数据全部作为初始化数据导出到src/main/resources下。
	 * 
	 * @throws Exception
	 */
	public void doxport(DbClient client,String packageName, File output) throws Exception {
		ClassScanner cs = new ClassScanner();
		IResource[] entities = cs.scan(packageName);
		InitDataExporter exporter = new InitDataExporter(client, output);
		for (IResource clz : entities) {
			ClassReader cl=new ClassReader(clz.getInputStream(),true);
			Class<?> e = Class.forName(cl.getClassName());
			if (DataObject.class.isAssignableFrom(e)) {
				InitializeData data = e.getAnnotation(InitializeData.class);
				if (data != null)
					exporter.export(e);
			}
		}
	}
}
