package jef.database.meta;


import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import jef.database.DbClient;
import jef.database.DbMetaData;
import jef.database.DbMetaData.ObjectType;
import jef.database.Session;
import jef.database.meta.object.Column;
import jef.database.meta.object.ForeignKey;
import jef.database.meta.object.Index;
import jef.database.meta.object.TableInfo;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.tools.StringUtils;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
	 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	 @DataSource(name= "h2", url="${h2.url}", user="SA", password=""),
	 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
	 @DataSource(name="derby",url="${derby.url}"),
	 @DataSource(name = "hsqldb", url = "${hsqldb.url}", user = "${h2.user}", password = ""),
	 @DataSource(name = "sqlite", url = "${sqlite.url}"),
	 @DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class MetadataTest extends org.junit.Assert{
	private DbClient db;
		
	private static final String[] SQLS={"CREATE TABLE t1 (c1 int PRIMARY KEY,c2 int not null)",
	"ALTER TABLE t1 add CONSTRAINT test_111 UNIQUE (c2)",
	"CREATE TABLE t2 (c1 int PRIMARY KEY,c2 int REFERENCES t1(c2)  on delete set null)",
	"CREATE TABLE t3 (c1 int, c2 int, CONSTRAINT t1_fk FOREIGN KEY (c1) REFERENCES t1)"
	};
	
	
	@Test
//	@IgnoreOn(allButExcept="h2")
	public void testFetchTableAndColumns() throws Exception{
		initSchema();
		DbMetaData meta=db.getMetaData(null);
		int i=0;
		List<TableInfo> tables=meta.getTables();
		System.out.println("There's "+tables.size()+" tables.");
		for(TableInfo e:tables){
			i++;
			System.out.println(e.getName()+":"+e.getRemarks());
			List<Column> columns=meta.getColumns(e.getName());
			for(Column c:columns){
				if(StringUtils.isNotEmpty(c.getColumnDef())){
					System.out.println("["+c.getColumnDef()+"]");
				}
			}
			if(i>5)
				break;
			
		}
	}

	@Test
	public void initSchema() throws SQLException{
		db.dropTable("t1");
		db.dropTable("t2");
		db.dropTable("t3");
		for(String s: SQLS){
			try{
				db.executeSql(s);
			}catch(SQLException e){
				System.out.println(e.getSQLState());
			}
		}
	}
	
	
	@Test
	@IgnoreOn("sqlite")
	public void testMeta() throws SQLException {
		List<ForeignKey> fk=db.getMetaData(null).getForeignKey("T3");
		for(ForeignKey k: fk){
			System.out.println("===============");
			System.out.println(k.toString());
			System.out.println(k.toCreateSql(db.getProfile()));
			System.out.println(k.getPkName());
		}
	}
	
	/**
	 * Schema测试
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn({"mysql","sqlite"})
	public void testSchema() throws SQLException {
		Session s=db;
		DbMetaData meta=s.getNoTransactionSession().getMetaData(null);
		String[] schemas=meta.getSchemas();
		
		assertTrue(schemas.length>0);
		if(meta.getCurrentSchema()!=null){
			assertTrue(ArrayUtils.contains(schemas, meta.getCurrentSchema()));	
		}
		
		List<TableInfo> tables=meta.getTables();
		if(!tables.isEmpty()){
			String name=tables.get(0).getName();
			for(Index indexInfo:meta.getIndexes(name)){
				System.out.println(indexInfo.toString());
				System.out.println("索引名称:"+indexInfo.getIndexName());
				System.out.println("索引字段:"+Arrays.toString(indexInfo.getColumnNames()));
				System.out.println("是否唯一:"+indexInfo.isUnique());
			}
		}
		
		meta.getFunctions(null);
	}

	/**
	 * 测试目的：当{@code schema}不为{@code null}时，判断SEQUENCE存在与否是否正确。<br/>
	 * 预期结果：{@code schema}应是不区分大小写的。
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn("hsqldb")
	public void testExistSequenceWithSchema() throws SQLException {
		DbMetaData meta = db.getMetaData(null);
		if(meta.supportsSequence()){
			String schema = meta.getCurrentSchema();
			String seqName = "seq_test_for_exist";
			createSequence(schema, seqName);

			boolean exist = meta.existsInSchema(ObjectType.SEQUENCE, StringUtils.upperCase(schema), seqName);
			Assert.assertTrue(exist);

			exist = meta.existsInSchema(ObjectType.SEQUENCE,StringUtils.lowerCase(schema), seqName);
			Assert.assertTrue(exist);

			dropSequence(schema + "." + seqName);
			Assert.assertFalse(meta.existsInSchema(ObjectType.SEQUENCE, schema, seqName));	
		}
	}

	private void createSequence(String schema, String seqName) throws SQLException {
		db.getMetaData(null).createSequence(schema, seqName, 1, 999999999L);
	}
	
	private void dropSequence(String seqName) throws SQLException {
		db.executeSql("drop sequence " + seqName);
	}

	/**
	 * 测试目的：当{@code schema}为{@code null}时，判断SEQUENCE存在与否是否正确。<br/>
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn("hsqldb")
	public void testExistSequenceWithoutSchema() throws SQLException {
		DbMetaData meta = db.getMetaData(null);
		if(meta.supportsSequence()){
			String seqName = "seq_test_for_exist";
			createSequence(null, seqName);
			Assert.assertTrue(meta.exists(ObjectType.SEQUENCE, seqName));

			dropSequence(seqName);
			Assert.assertFalse(meta.exists(ObjectType.SEQUENCE, seqName));	
		}
	}

	@Test
	public void testFunctionTest() throws SQLException{
		DbMetaData meta = db.getMetaData(null);
		meta.getFunctions("");
	}
}
