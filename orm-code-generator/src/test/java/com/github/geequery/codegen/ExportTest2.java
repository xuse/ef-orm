package com.github.geequery.codegen;

import java.io.File;

import org.junit.Test;

import com.github.geequery.codegen.MetaProvider.DbClientProvider;

import jef.codegen.EntityEnhancer;
import jef.database.DataObject;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.ORMConfig;
import jef.database.datasource.SimpleDataSource;
import jef.database.support.InitDataExporter;
import jef.database.support.QuerableEntityScanner;

public class ExportTest2 {
	@Test
	public void testGenerateSource() throws Exception {

		String jdbcUrl = "jdbc:mysql://localhost:3307/test8?useUnicode=true&characterEncoding=UTF-8";
		String jdbcUser = "root";
		String jdbcPassword = "admin";
		final DbClient db = new DbClient(new SimpleDataSource(jdbcUrl, jdbcUser, jdbcPassword));

		EntityGenerator g = new EntityGenerator();
		g.setProfile(db.getProfile(null));
		g.addExcludePatter(".*_\\d+$"); // 防止出现分表
		g.addExcludePatter("AAA"); // 排除表
		g.setMaxTables(999);
		g.setSrcFolder(new File(System.getProperty("user.dir"), "target/generated-sources"));
		g.setBasePackage("com.github.geequery.codegen.entity");
		g.setProvider(new DbClientProvider(db));
		//g.generateSchema(Option.generateEntity, Option.generateRepos);
		g.generateOne("customer_order_history", Option.generateEntity);
		db.shutdown();
	}

	@Test
	public void testUseGenerateSource2() throws Exception {
		String jdbcUrl = "jdbc:mysql://localhost:3307/test8?useUnicode=true&characterEncoding=UTF-8";
		String jdbcUser = "root";
		String jdbcPassword = "admin";

		final DbClient db = new DbClientBuilder().setDataSource(new SimpleDataSource(jdbcUrl, jdbcUser, jdbcPassword)).setPackagesToScan(new String[] { "com.github.geequery.codegen.entity" }).setEnhanceScanPackages(true)
				// .setCreateTable(false)
				.setAlterTable(true).setInitData(false).setDebug(true).build();

		// db.dropTable(com.github.geequery.codegen.entity.AuthInfo.class);
		// db.createTable(com.github.geequery.codegen.entity.AuthInfo.class);
		db.shutdown();
	}

	@Test
	public void testUseGenerateSource() throws Exception {
		String jdbcUrl = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8";
		String jdbcUser = "root";
		String jdbcPassword = "admin";

		final DbClient db = new DbClientBuilder().setDataSource(new SimpleDataSource(jdbcUrl, jdbcUser, jdbcPassword)).setPackagesToScan(new String[] { "com.github.geequery.codegen.entity" }).setEnhanceScanPackages(true)
				// .setCreateTable(false)
				.setAlterTable(true).setInitData(false).setDebug(true).build();

		// db.dropTable(com.github.geequery.codegen.entity.AuthInfo.class);
		// db.createTable(com.github.geequery.codegen.entity.AuthInfo.class);
		db.shutdown();
	}

	@Test
	public void testExporterData() throws Exception {
		DbClient db = new DbClient(new SimpleDataSource("jdbc:mysql://localhost:3306/api", "root", "admin"));
		InitDataExporter ex = new InitDataExporter(db, new File(System.getProperty("user.dir"), "src/test/resources"));
		ex.exportPackage("com.github.geequery.codegen.entity");
		db.shutdown();
	}

	@Test
	public void testInitData() {
		EntityEnhancer en = new EntityEnhancer();
		en.enhance("com.github.geequery.codegen.entity");
		ORMConfig.getInstance().setDebugMode(true);
		DbClient db = new DbClient(new SimpleDataSource("jdbc:mysql://localhost:3306/test1?useUnicode=true&characterEncoding=UTF-8", "root", "admin"));
		QuerableEntityScanner qe = new QuerableEntityScanner();
		qe.setImplClasses(DataObject.class);
		qe.setAllowDropColumn(true);
		qe.setAlterTable(true);
		qe.setCreateTable(true);
		qe.setEntityManagerFactory(db, false, "UTF-8", "txt", "/");
		qe.setPackageNames("com.github.geequery.codegen.entity");
		qe.doScan();
		qe.finish();
		db.shutdown();
	}

	@Test
	public void testGenerated1Seq() throws Exception {
		ORMConfig.getInstance().setDebugMode(true);
		EntityEnhancer en = new EntityEnhancer();
		en.enhance("com.github.geequery.codegen.testid");
		DbClient db = new DbClient(new SimpleDataSource("jdbc:mysql://localhost:3306/test1", "root", "admin"));
	}

	@Test
	public void testGeneratedSeq() throws Exception {
		ORMConfig.getInstance();


		ORMConfig.getInstance().setManualSequence(true);
		// EntityEnhancer en = new EntityEnhancer();
		// en.enhance("com.github.geequery.codegen.testid");
		DbClient db = new DbClient(new SimpleDataSource("jdbc:mysql://localhost:3306/test1", "root", "admin"));
		QuerableEntityScanner qe = new QuerableEntityScanner();
		qe.setImplClasses(DataObject.class);
		qe.setAllowDropColumn(true);
		qe.setAlterTable(true);
		qe.setCreateTable(true);
		qe.setEntityManagerFactory(db, false, "UTF-8", "txt", "/");
		qe.setPackageNames("com.github.geequery.codegen.testid");
		qe.doScan();
		qe.finish();
		db.shutdown();
		// db.createTable(Foo.class);

	}

}
