package jef.orm.onetable;

import java.sql.SQLException;
import java.util.Arrays;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.IQueryableEntity;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.onetable.model.TestEntity;
import jef.tools.ThreadUtils;
import jef.tools.string.RandomData;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 自动创建的序列的起始值取值问题(Trac #56111)单元测试类
 * <p>
 * @see SequenceKeyHolder
 * 
 * @Date 2012-8-28
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
 @DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"), 
})
public class SequenceCreationTest extends org.junit.Assert{
	
	private DbClient db;

	private static String seqName;

	@DatabaseInit
	public void truncatTable() {
		try {
			db.dropTable(TestEntity.class);
			db.createTable(TestEntity.class);
			ThreadUtils.doSleep(500);
		} catch (SQLException e) {
			LogUtil.exception(e);
		}
	}

	@Test
	public void testCase() throws SQLException{
		testSequenceStartValueOnEmptyTable();
		testSequenceStartValueOnNotEmptyTable();
	}
	
	
	/**
	 * 测试表中无数据的情况下的起始值，应为1.
	 * 
	 * @throws SQLException
	 */
	
	private void testSequenceStartValueOnEmptyTable() throws SQLException {
		// 单个插入
		TestEntity t1 = RandomData.newInstance(TestEntity.class);
		db.insert(t1);
		assertEquals(1, t1.getLongField());
		db.truncate(TestEntity.class);
	}

	/**
	 * 测试表中已有数据的情况下的起始值，应为max+1.
	 * <p>
	 * 注：需单独运行。通过{@code SequenceCreationTest} 运行，由于{@code SequenceKeyHolder}
	 * 被缓存，故序列不会被自动创建。
	 * </p>
	 * 
	 * @throws SQLException
	 */
	private void testSequenceStartValueOnNotEmptyTable() throws SQLException {
		insertRecords(3);
		dropSequence();
		
		// 批量插入
		TestEntity t1 = RandomData.newInstance(TestEntity.class);
		TestEntity t2 = RandomData.newInstance(TestEntity.class);
		TestEntity t3 = RandomData.newInstance(TestEntity.class);
		TestEntity t4 = RandomData.newInstance(TestEntity.class);
		db.batchInsert(Arrays.asList(t1,t2,t3,t4));
		assertEquals(5, t1.getLongField());
		assertEquals(8, t4.getLongField());
	}

	/**
	 * 由于调用 DbClient.insert 或 DbClient.startBatchInsert 方法会引起 SequenceKeyHolder
	 * 缓存， 在insert后drop sequence，sequence不会重建，所以需通过直接JDBC方式插入数据。
	 * 
	 * @throws SQLException
	 */
	private void insertRecords(int num) throws SQLException {
		for(int i=0;i<num;i++){
			TestEntity t= RandomData.newInstance(TestEntity.class);
			db.insert(t);
		}
	}

	private void dropSequence() throws SQLException {
		seqName = getSeqNameByTable(TestEntity.class);
		db.dropSequence(seqName);
		db.getSequenceManager().clearHolders();
	}

	// 根据表名称计算SequenceName
	public String getSeqNameByTable(Class<? extends IQueryableEntity> table) {
		ITableMetadata meta = MetaHolder.getMeta(table);
		return meta.getFirstAutoincrementDef().getSequenceName(db.getProfile());
	}
}
