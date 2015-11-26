package jef.database.test.jdbc;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.datasource.SimpleDataSource;
import jef.orm.multitable.model.Person;
import jef.tools.string.RandomData;

import org.junit.Test;

public class SqliteTest {
	
	
	@Test
	public void test1() throws SQLException {
		Map<String,DataSource> rds=new HashMap<String,DataSource>();
		rds.put("ds1", new SimpleDataSource("jdbc:sqlite:c:/aaa.db", null, null));
		rds.put("mem", new SimpleDataSource("jdbc:sqlite::memory:", null, null));
		DbClient db=new DbClientBuilder().setDataSources(rds).build();
		
		System.out.println(db.getAllDatasourceNames());
		System.out.println(db.getMetaData("ds1").getTables());
		System.out.println(db.getMetaData("mem").getTables());
		
		db.getMetaData("ds1").createTable(Person.class);
		Person p=new Person();
		p.setName(RandomData.randomChineseName());
		p.setCell("1888888888");
		p.setGender("M");
		p.setPhone("88394999");
		db.insert(p);
		
		p.setName(RandomData.randomChineseName());
		p.setCell("1888888888");
		p.setGender("M");
		p.setPhone("88394999");
		db.insert(p);
	
		db.shutdown();
	}
	
	@Test
	public void test2() throws SQLException {
		Map<String,DataSource> rds=new HashMap<String,DataSource>();
		rds.put("ds1", new SimpleDataSource("jdbc:sqlite:c:/aaa.db", null, null));
		rds.put("mem", new SimpleDataSource("jdbc:sqlite::memory:", null, null));
		DbClient db=new DbClientBuilder().setDataSources(rds).build();
		
		System.out.println(db.getAllDatasourceNames());
		System.out.println(db.getMetaData("ds1").getTables());
		System.out.println(db.getMetaData("mem").getTables());
		
		db.getMetaData("ds1").executeSql("ATTACH database \":memory:\" as memdb");
		
		db.getSqlTemplate("ds1").executeSql("create table memdb.person_table as select * from person_table");
		List<Person> persons=db.getSqlTemplate("ds1").selectBySql("select * from memdb.person_table", Person.class);
		LogUtil.show(persons);
		System.out.println(db.getMetaData("mem").getTables());
		db.shutdown();
	}

}
