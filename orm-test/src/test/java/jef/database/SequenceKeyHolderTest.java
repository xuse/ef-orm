package jef.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Queue;

import jef.database.DbMetaData.ObjectType;
import jef.database.innerpool.IConnection;
import jef.database.support.RDBMS;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.tools.JefConfiguration;
import jef.tools.reflect.BeanUtils;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@code SequenceKeyHolder}单元测试类
 * <p>
 * 运行前，需要：
 * <ul>
 * <li>去掉 @Ignore 注解</li>
 * <li>根据实际数据库的信息，修改SCHEMA的值、以及方法 init() 中 db =
 * DbClientFactory.getDbClient(...)这一句中的参数值</li>
 * </ul>
 * </p>
 * 
 * @see SequenceKeyHolder
 * 
 * @Company Asiainfo-Linkage Technologies (China), Inc.
 * @author luolp@asiainfo-linkage.com
 * @Date 2012-10-25
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({ @DataSource(name = "oracle", url = "${oracle.url}", user = "${oracle.user}", password = "${oracle.password}") })
public class SequenceKeyHolderTest {
	private DbClient db;
	private String SCHEMA;
	private static final int CACHE_SIZE = JefConfiguration.getInt(DbCfg.SEQUENCE_BATCH_SIZE, 50);

	private static int SEQ_START;
	private static int SEQ_STEP;
	private static String SEQ_NAME;
	private static String SEQ_CREATE_SQL;
	private static String SEQ_NEXVAL_SQL;

	@DatabaseInit
	public void init() throws SQLException {
		this.SCHEMA = db.getMetaData(null).getCurrentSchema();
	}

	@Test
	public void testNext() {
		if (db == null)
			return;
		if (db.getProfile(null).getName() == RDBMS.oracle) {
			try {
				// start < step
				System.out.println("============== testing sequence start < step ==============");
				SEQ_NAME = "seq_for_step_test1";
				SEQ_START = 2;
				SEQ_STEP = 5;
				recreateSequence();
				getNext();

				// start = step
				System.out.println("============== testing sequence start = step ==============");
				SEQ_NAME = "seq_for_step_test2";
				SEQ_START = 2;
				SEQ_STEP = 2;
				recreateSequence();
				getNext();

				// start > step
				System.out.println("============== testing sequence start > step ==============");
				SEQ_NAME = "seq_for_step_test3";
				SEQ_START = 10;
				SEQ_STEP = 5;
				recreateSequence();
				getNext();

			} catch (SQLException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@Test
	public void testCreatingSequenceInLimitedTries() {
		if (db == null)
			return;
		if (db.getProfile(null).getName() == RDBMS.oracle) {
			// 该sequence名称过长，将触发"ORA-00972: 标识符过长"错误，以此来验证失败时的尝试次数。
			String seq = "sys.seq_for_creation_in_limited_times";

			int i = 0;
			do {
				try {
					Sequence holder = db.selectTarget(null).getSequence(seq, 12);
					Assert.assertNotNull(holder);
					// 获取下一个sequence值时，也需要受到失败次数的约束。
					try {
						holder.next();
					} catch (Exception e) {
						System.out.println(e.getClass().getName() + " " + e.getMessage());
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} while (i++ < 10);
		}
	}

	private void recreateSequence() throws SQLException {
		if (db.selectTarget(null).getMetaData().existsInSchema(ObjectType.SEQUENCE, SCHEMA, SEQ_NAME)) {
			db.getMetaData(null).dropSequence(SEQ_NAME);
		}

		SEQ_CREATE_SQL = "create sequence " + SEQ_NAME + " minvalue 1 maxvalue 999999 start with " + SEQ_START + " increment by " + SEQ_STEP + " cache 200";
		db.executeSql(SEQ_CREATE_SQL);
		db.getSequenceManager().clearHolders();
	}

	private void getNext() throws SQLException {
		Sequence holder = db.selectTarget(null).getSequence(SEQ_NAME, 12);

		// 首次获取下一个序列值时将通过查询DB获得
		long next = getNextSequenceValue(holder);
		Assert.assertEquals(SEQ_START, next);

		// 接着，获取下一个序列值CACHE_SIZE次，此时均将直接从cache中获得
		for (int i = 1; i < CACHE_SIZE; i++) {
			next = getNextSequenceValue(holder);
			Assert.assertEquals(SEQ_START + i, next);
		}

		// 再次通过查询DB获取下一个序列值，该值与cache后的一致(即不会出现主键冲突问题)
		long nextFromDb = getNextSequenceValueFromDb();
		System.out.println("next value of ".concat(SEQ_NAME).concat("(from db)=") + nextFromDb);
		Assert.assertEquals(next + 1, nextFromDb);
	}

	@SuppressWarnings("rawtypes")
	private long getNextSequenceValue(Sequence holder) throws SQLException {
		long next = holder.next();
		System.out.println("next value of ".concat(SEQ_NAME).concat("=") + next);
		Queue cacheQueue = (Queue) BeanUtils.getFieldValue(holder, "cache");
		System.out.println("cache: " + ArrayUtils.toString(cacheQueue.toArray()));
		return next;
	}

	private long getNextSequenceValueFromDb() throws SQLException {
		IConnection conn = db.getConnection();
		System.out.println(SCHEMA);
		if(SCHEMA==null){
			SEQ_NEXVAL_SQL = "select " +SEQ_NAME + ".nextval from dual";			
		}else{
			SEQ_NEXVAL_SQL = "select " + SCHEMA + "." + SEQ_NAME + ".nextval from dual";
		}
		
		System.out.println(SEQ_NEXVAL_SQL);
		PreparedStatement ps = conn.prepareStatement(SEQ_NEXVAL_SQL);
		ResultSet rs = ps.executeQuery();

		try {
			rs.next();
			return rs.getLong(1);
		} finally {
			rs.close();
			ps.close();
			db.releaseConnection(conn);
		}
	}
}
