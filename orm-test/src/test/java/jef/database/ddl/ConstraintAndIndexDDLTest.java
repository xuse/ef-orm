/**
 * 
 */
package jef.database.ddl;

import java.sql.SQLException;
import java.util.Date;

import org.junit.Test;

import jef.database.DbClient;
import jef.database.DbClientBuilder;

/**
 * @author qihongfei
 *
 */
public class ConstraintAndIndexDDLTest {
	
	
	private DbClient db=new DbClientBuilder().setDataSource("jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8", "root", "admin").build();

	@Test
	public void testCreateTable() throws SQLException {
		
//		DbMetaData meta=db.getMetaData(null);
//		DatabaseDialect dialect=AbstractDialect.getDialect("mysql");
		
		try {
			db.dropTable(TableForTest.class);
	        db.createTable(TableForTest.class);

	        TableForTest tb = new TableForTest();
	        tb.setAmount(10L);
	        tb.setCode("123");
	        tb.setData("sads".getBytes());
	        db.insert(tb);
	        System.out.println(tb.getModified());// 目前不会回写

	        System.out.println("====================Step.2====================");
	        tb.setName("修改");
	        db.update(tb);

	        tb = new TableForTest();
	        tb.setAmount(12L);
	        tb.setCode("124");
	        tb.setData("sadssdd".getBytes());
	        tb.setModified(new Date());
	        db.insert(tb);
	        System.out.println(tb.getModified()); // 事先获得
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{
				db.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void beforeRefreshTable() throws SQLException {
		
		db.dropTable(TableForTest.class);
        db.createTable(TableForTest.class);
        
        db.executeSql("ALTER TABLE TABLE_FOR_TEST DROP INDEX UQ1_FOR_TEST");
        db.executeSql("ALTER TABLE TABLE_FOR_TEST ADD CONSTRAINT UQ3_FOR_TEST UNIQUE(name)");
        
        db.executeSql("ALTER TABLE TABLE_FOR_TEST DROP INDEX IDX_DEFAULT_TEST");
        db.executeSql("ALTER TABLE TABLE_FOR_TEST ADD UNIQUE INDEX IDX_FOR_TEST (code, name desc)");
	}
	
	@Test
	public void refreshTable() throws SQLException {
		db.refreshTable(TableForTest.class);
	}
	
	
	@Test
	public void createTable() throws SQLException {
		db.dropTable(TableForTest.class);
		db.createTable(TableForTest.class);
	}
}
