/**
 * 
 */
package jef.database.dialect;

import java.sql.SQLException;
import java.util.List;

import org.junit.Test;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.DbMetaData;
import jef.database.meta.object.Constraint;

/**
 * 系统约束获取测试类
 * @author qihongfei
 *
 */
public class ConstraintTest {

	@Test
	public void testPG94Constraint(){
		DbClient db=new DbClientBuilder().setDataSource("jdbc:postgresql://localhost:5432/postgres", "postgres", "admin").build();
		DbMetaData meta=db.getMetaData(null);
		DatabaseDialect dialect=AbstractDialect.getDialect("postgresql");
		try {
			List<Constraint> cons = dialect.getConstraintInfo(meta, "", "");
			cons.forEach(c -> System.out.println(c.toString()));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testOracleConstraint(){
		
		DbClient db=new DbClientBuilder().setDataSource("jdbc:oracle:thin:@localhost:1521:XE", "system", "admin").build();
		DbMetaData meta=db.getMetaData(null);
		DatabaseDialect dialect=AbstractDialect.getDialect("oracle");
		try {
			
			// 查询单个约束
			List<Constraint> cons = dialect.getConstraintInfo(meta, "SYSTEM", "REPCAT$_AUDIT_COLUMN_F2");
			cons.forEach(c -> System.out.println(c.toString()));
			
			// 查询schema下所有约束
			cons = dialect.getConstraintInfo(meta, "SYSTEM", null);
			cons.forEach(c -> System.out.println(c.toString()));
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
