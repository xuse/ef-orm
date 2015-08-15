package com.github.geequery.codegen.pdm;

import java.io.File;
import java.sql.SQLException;

import jef.database.dialect.AbstractDialect;

import org.junit.Test;

import com.github.geequery.codegen.EntityGenerator;
import com.github.geequery.codegen.MetaProvider.PDMProvider;

public class PDMGenerateTest {
	@Test
	public void testGenerate() throws SQLException {
		String dbType="postgresql";
		File file=new File("E:/SVN/hikpaas/trunk/HAE/doc/02设计/hae-V0.2.pdm");
		
		EntityGenerator g = new EntityGenerator();
		g.setProfile(AbstractDialect.getProfile(dbType));
		g.setProvider(new PDMProvider(file));
		g.setMaxTables(100);
		g.setSrcFolder(new File("E:/Git/ef-orm/orm-code-generator/src/test/java"));
		g.setBasePackage("com.hikvision.test");
		g.generateSchema();

	}
	
//	@Test
//	public void testCreate() throws SQLException {
//		DbClient client=new DbClientBuilder("jdbc:postgresql://10.33.25.48:5432/hae2", "root", "admin").build();
//		client.createTable(Tenant.class);
//	}
}
