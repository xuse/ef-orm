package jef.orm.postgresql;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import jef.database.DbCfg;
import jef.database.DbClient;
import jef.database.DebugUtil;
import jef.database.QB;
import jef.database.dialect.PostgreSqlDialect;
import jef.database.innerpool.IConnection;
import jef.database.query.Query;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.postgresql.model.TestColumnTypeEntity;
import jef.orm.postgresql.model.TestColumnTypeEntity82;
import jef.orm.postgresql.model.TestColumntypesCommon;
import jef.orm.postgresql.model.TestColumntypesDb2entity;
import jef.orm.postgresql.model.TestColumntypesDb2entity82;
import jef.orm.postgresql.model.TestColumntypesSpecial;
import jef.tools.DateUtils;
import jef.tools.JefConfiguration;
import jef.tools.ResourceUtils;
import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * PostgreSQL各个data type(Arrays, Composite Types以及自定义类型除外)数据读写测试类
 * 
 * @Company Asiainfo-Linkage Technologies (China), Inc.
 * @author luolp@asiainfo-linkage.com
 * @Date 2012-7-20
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({ @DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}") })
public class PostgreSQLTest {
	private DbClient db;
	private String queryTable = JefConfiguration.get(DbCfg.DB_QUERY_TABLE_NAME);
	private SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	protected static boolean isEQ90version = false;
	protected static boolean isEQ84version = false;
	protected static boolean isEQ83version = false;
	protected static boolean isLE82version = false;

	@DatabaseInit
	public void setUp() throws SQLException {
		dropTable();
		String dbVersion = db.getMetaData(null).getDatabaseVersion();
		String dvVersionMain = StringUtils.substringBeforeLast(dbVersion, ".");
		isEQ90version = StringUtils.equals(dvVersionMain, "9.0");
		isEQ84version = StringUtils.equals(dvVersionMain, "8.4");
		isEQ83version = StringUtils.equals(dvVersionMain, "8.3");
		isLE82version = Double.valueOf(dvVersionMain).doubleValue() <= 8.2;
	}

	private void dropTable() throws SQLException {
		db.dropTable(TestColumnTypeEntity.class);
	}

	/**
	 * 无论建表SQL中的表名是大写还是小写，最终DB中的表名都是小写， 所以当传入的参数值为大写时，需将其转换为小写。 <br>
	 * 此处实际上是验证了 {@link PostgreSqlDialect#getObjectNameIfUppercase(String name)}
	 */
	@Test
	public void testExistTableWithNameIsUppercase() {
		if (db == null)
			return;
		try {
			Assert.assertTrue(db.existTable(queryTable.toUpperCase()));
		} catch (SQLException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 测试DB中创建的表的data type是否与entity中定义的一致
	 */
	@Test
	public void testDbColumnTypes() {
		if (db == null)
			return;
		try {
			db.createTable(isLE82version ? TestColumnTypeEntity82.class : TestColumnTypeEntity.class);
		} catch (SQLException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 测试以对象方式进行CRUD（表中不含PostgreSQL特有data type）
	 * 
	 * @throws SQLException
	 * @throws ParseException
	 * @throws IOException
	 */
	@Test
	public void testCRUDByObjOnCommonColumns() throws SQLException, ParseException, IOException {
		if (db == null)
			return;
		prepareDbByNativeSqls("postgresql_createtable2_test.sql");

		TestColumntypesCommon entity = new TestColumntypesCommon();

		int smallInt = -32768;
		entity.setSmallintfield(smallInt);

		Integer int2 = 32767;
		entity.setInt2field(int2);

		Long intfield = 2147483647L;
		entity.setIntfield2(intfield);

		entity.setBigintfield(Long.MAX_VALUE);

		Double decimal = 99.99;
		entity.setDecimalfield(decimal);

		Double numeric = 99.999;
		entity.setNumericfield(numeric);

		Float numeric2 = 999.99F;
		entity.setNumericfield2(numeric2); // 小数点后位数不能大于2位，因该列的类型为numeric(5,2)，大于2位是将产生异常

		Float real = 0.11111111F;
		entity.setRealfield(real);
		entity.setFloatfield(real);

		Double doubleField = 999999.999999;
		entity.setDoublefield(doubleField);
		entity.setDoublefield2(doubleField);

		long serial = 1;
		entity.setSerialfield(serial);
		entity.setSerialfield2(999999999);

		String varchar = "abc";
		entity.setVarcharfield1(varchar);
		entity.setVarcharfield2(varchar);

		String charField = "a";
		entity.setCharfield1(charField);
		entity.setCharfield2(charField);

		entity.setBooleanfield1(true);
		entity.setBooleanfield2(false);

		Date dateField = DateUtils.truncate(new Date());
		entity.setDatefield(dateField);

		Date timestampField = DateUtils.now();
		entity.setTimestampfield1(timestampField);

		Date timestampField2 = new GregorianCalendar(TimeZone.getTimeZone("GMT-8:00")).getTime();
		entity.setTimestampfield2(timestampField2);

		Date time = DateUtils.parse("13:00:00", DateFormat.getTimeInstance());
		entity.setTimefield1(time);

		Date time2 = new GregorianCalendar(TimeZone.getTimeZone("GMT+10")).getTime();
		entity.setTimefield2(time2);

		String binary = "你好";
		entity.setBinaryfield(binary.getBytes());

		String text = "text";
		entity.setTextfield(text);

		// insert
		db.insert(entity);

		// select
		TestColumntypesCommon selectResult = db.select(entity).get(0);
		Assert.assertEquals(smallInt, selectResult.getSmallintfield());
		Assert.assertEquals(int2, selectResult.getInt2field());
		Assert.assertEquals(intfield, selectResult.getIntfield2());
		Assert.assertEquals(Long.MAX_VALUE, selectResult.getBigintfield().longValue());
		Assert.assertEquals(decimal, selectResult.getDecimalfield());
		Assert.assertEquals(numeric, selectResult.getNumericfield());
		Assert.assertEquals(numeric2, selectResult.getNumericfield2());
		Assert.assertEquals(real, selectResult.getRealfield());
		Assert.assertEquals(real, selectResult.getFloatfield());
		Assert.assertEquals(doubleField, selectResult.getDoublefield());
		Assert.assertEquals(doubleField, selectResult.getDoublefield2());
		Assert.assertEquals(serial, selectResult.getSerialfield());
		// serialfield2为PK，最终DB中的数值为自动生成，而非所设置的值
		Assert.assertEquals(serial, selectResult.getSerialfield2());
		Assert.assertEquals(varchar, selectResult.getVarcharfield1());
		Assert.assertEquals(varchar, selectResult.getVarcharfield2());
		// Charfield1定义的长度为2，不足长度的值将会被自动补足长度
		Assert.assertEquals(charField.concat(" "), selectResult.getCharfield1());
		Assert.assertEquals(charField, selectResult.getCharfield2());
		Assert.assertTrue(selectResult.isBooleanfield1());
		Assert.assertFalse(selectResult.isBooleanfield2());
		Assert.assertEquals(dateField, selectResult.getDatefield());
		Assert.assertEquals(timestampField, selectResult.getTimestampfield1());
		Assert.assertEquals(timestampField2, selectResult.getTimestampfield2());
		Assert.assertEquals(time, selectResult.getTimefield1());
		Assert.assertEquals(DateUtils.format(time2, "HH:mm:ss"), DateUtils.format(selectResult.getTimefield2(), "HH:mm:ss"));
		Assert.assertEquals(binary, new String(selectResult.getBinaryfield()));
		Assert.assertEquals(text, selectResult.getTextfield());

		// update
		Date nowDate = DateUtils.now();
		entity.setTimestampfield1(nowDate);
		db.update(entity);
		entity.getQuery().setCascade(false);
		Assert.assertEquals(sf.format(nowDate), sf.format(db.load(entity).getTimestampfield1()));

		// delete
		db.delete(entity);
		Assert.assertNull(db.load(entity));

	}

	/**
	 * 测试以JDBC方式进行select（表中含PostgreSQL特有data type）
	 * 
	 * <p>
	 * 测试结论：<br>
	 * 1)当数据库表中含PostgreSQL特有data type时，只能通过JDBC方式来获取record值。<br>
	 * 2)不同版本money类型值的差异：<br>
	 * PostgreSQL 9.1版本，在insert时，money类型值可以直接以numeric形式描述；<br>
	 * PostgreSQL 9.0及以下版本，在insert时，money类型值需进行显式转换，否则会产生异常；<br>
	 * PostgreSQL 8.2及以下版本，money类型值的转换方式也有所不同。<br>
	 * 3)不同版本bytea类型值的格式的差异：<br>
	 * PostgreSQL 8.4及以下版本仅支持escape format, 如E'\\000',<br>
	 * PostgreSQL 9.0及以上版本还支持hex format, 如E'\\xDEADBEEF'.<br>
	 * 4)不同版本查询tsvector类型值的结果有所不同：<br>
	 * PostgreSQL 8.3及以下版本，以长度为优先；<br>
	 * PostgreSQL 8.4及以上版本，以字典序为优先。<br>
	 * 5)不同版本interval类型值的格式的差异：<br>
	 * PostgreSQL 8.3及以下版本，不支持含"-"的形式，如"1-2"；<br>
	 * PostgreSQL 8.4及以上版本，支持含"-"的形式，如"1-2"，表示1 year 2 months.<br>
	 * 6)PostgreSQL 8.2及以下版本，无uuid, xml, txid这几个data type.
	 * </p>
	 * 
	 * @throws IOException
	 * @throws SQLException
	 */
	@Test
	public void testSelectByJdbc() throws SQLException, IOException {
		if (db == null)
			return;
		prepareDbByNativeSqls(isLE82version ? "postgresql_createtable_82_test.sql" : "postgresql_createtable_test.sql");

		String initSql = "postgresql_data_test.sql";
		if (isLE82version) {
			initSql = "postgresql_data_82_test.sql";
		} else if (isEQ83version) {
			initSql = "postgresql_data_83_test.sql";
		} else if (isEQ84version) {
			initSql = "postgresql_data_84_test.sql";
		} else if (isEQ90version) {
			initSql = "postgresql_data_90_test.sql";
		}
		prepareDbByNativeSqls(initSql);

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			IConnection conn=DebugUtil.getConnection(db.getSqlTemplate(null));
			pstmt = conn.prepareStatement("SELECT * FROM test_columntypes_db2entity");
			rs = pstmt.executeQuery();
			TestColumntypesDb2entity entity = new TestColumntypesDb2entity();
			while (rs.next()) {
				entity.setSmallintfield(rs.getInt("smallintfield"));
				entity.setInt2field(rs.getInt("int2field"));
				entity.setIntfield2(rs.getLong("intfield2"));
				entity.setBigintfield(rs.getLong("bigintfield"));
				entity.setDecimalfield(rs.getDouble("decimalfield"));
				entity.setNumericfield(rs.getDouble("numericfield"));
				entity.setNumericfield2(rs.getFloat("numericfield2"));
				entity.setRealfield(rs.getFloat("realfield"));
				entity.setFloatfield(rs.getFloat("floatfield"));
				entity.setDoublefield(rs.getDouble("doublefield"));
				entity.setDoublefield2(rs.getDouble("doublefield2"));
				entity.setSerialfield(rs.getInt("serialfield"));
				entity.setSerialfield2(rs.getLong("serialfield2"));
				entity.setMoneyfield(rs.getString("moneyfield"));
				entity.setVarcharfield1(rs.getString("varcharfield1"));
				entity.setVarcharfield2(rs.getString("varcharfield2"));
				entity.setCharfield1(rs.getString("charfield1"));
				entity.setCharfield2(rs.getString("charfield2"));
				entity.setVarbitfield1(rs.getString("varbitfield1"));
				entity.setVarbitfield2(rs.getString("varbitfield2"));
				entity.setBitfield1(rs.getString("bitfield1"));
				entity.setBitfield2(rs.getString("bitfield2"));
				entity.setCidrfield(rs.getString("cidrfield"));
				entity.setInetfield(rs.getString("inetfield"));
				entity.setMacaddrfield(rs.getString("macaddrfield"));
				entity.setBooleanfield1(rs.getBoolean("booleanfield1"));
				entity.setBooleanfield2(rs.getBoolean("booleanfield2"));
				entity.setDatefield(DateUtils.fromSqlDate(rs.getDate("datefield")));
				entity.setTimestampfield1(new Date(rs.getTimestamp("timestampfield1").getTime()));
				entity.setTimestampfield2(new Date(rs.getTimestamp("timestampfield2").getTime()));
				entity.setTimefield1(DateUtils.parse(rs.getString("timefield1"), "HH:mm:ss", null));
				entity.setTimefield2(getTimeWithZone(rs.getString("timefield2")));
				entity.setIntervalfield(rs.getString("intervalfield"));
				entity.setBinaryfield(rs.getBytes("binaryfield"));
				entity.setTextfield(rs.getString("textfield"));
				entity.setTsvectorfield(rs.getString("tsvectorfield"));
				entity.setTsqueryfield(rs.getString("tsqueryfield"));
				if (!isLE82version) {
					entity.setUuidfield(rs.getString("uuidfield"));
					entity.setXmlfield(rs.getString("xmlfield"));
					entity.setTxidfield(rs.getString("txidfield"));
				}
				entity.setBoxfield(rs.getString("boxfield"));
				entity.setCirclefield(rs.getString("circlefield"));
				entity.setLinefield(rs.getString("linefield"));
				entity.setLsegfield(rs.getString("lsegfield"));
				entity.setPathfield(rs.getString("pathfield"));
				entity.setPointfield(rs.getString("pointfield"));
				entity.setPolygonfield(rs.getString("polygonfield"));
			}

			assertEntityValue(entity, true);

		} catch (SQLException e) {
			Assert.fail(e.getMessage());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			try {
				rs.close();
				pstmt.close();
				// AfterClass will close conn, so need not call conn.close here.
			} catch (SQLException e) {
				Assert.fail(e.getMessage());
			} catch (Exception e) {
				Assert.fail(e.getMessage());
			}
		}
	}

	private Date getTimeWithZone(String timeWithZone) throws ParseException {
		Calendar gmt = new GregorianCalendar(TimeZone.getTimeZone("GMT" + timeWithZone.substring(8)));
		TimeZone.setDefault(TimeZone.getTimeZone("GMT" + timeWithZone.substring(8)));
		gmt.setTime(DateUtils.parse(timeWithZone, "HH:mm:ss", null));
		return gmt.getTime();
	}

	private void assertEntityValue(TestColumntypesDb2entity82 entity, boolean moneyFieldAssertRequired) throws ParseException {
		Assert.assertEquals(-32768, entity.getSmallintfield());
		Assert.assertEquals(32767, entity.getInt2field().intValue());
		Assert.assertEquals(2147483647, entity.getIntfield2().intValue());
		Assert.assertEquals(9223372036854775807L, entity.getBigintfield().longValue());
		Assert.assertEquals(99.99, entity.getDecimalfield());
		Assert.assertEquals(999.999, entity.getNumericfield());
		Assert.assertEquals(999.99F, entity.getNumericfield2());
		Assert.assertEquals(0.11111111F, entity.getRealfield());
		Assert.assertEquals(0.99999999F, entity.getFloatfield());
		Assert.assertEquals(999999.999999, entity.getDoublefield());
		Assert.assertEquals(999999.999999, entity.getDoublefield2());
		Assert.assertEquals(1, entity.getSerialfield());
		Assert.assertEquals(999999999, entity.getSerialfield2());
		System.out.printf("moneyfield:%s\r\n", entity.getMoneyfield());
		if (moneyFieldAssertRequired) {
			Assert.assertTrue(entity.getMoneyfield().endsWith("12.12"));
		}

		Assert.assertEquals("abc", entity.getVarcharfield1());
		Assert.assertEquals("abcd", entity.getVarcharfield2());
		Assert.assertEquals("a ", entity.getCharfield1());
		Assert.assertEquals("b", entity.getCharfield2());

		Assert.assertEquals("101010", entity.getVarbitfield1());
		Assert.assertEquals("1000000", entity.getVarbitfield2());

		// 通过JDBC和对象方式所取得的bitfield1值有所不同：
		// 对象方式得到的值是布尔值
		if (!StringUtils.equals("10101010", entity.getBitfield1())) {
			if("false".equals(entity.getBitfield1())){//当启用Cache Rowset后，这里的值变为boolean，为此不得不容错.
				entity.setBitfield1("0");
			}
			Assert.assertEquals("0", entity.getBitfield1());
		}

		if("true".equals(entity.getBitfield2())){//当启用Cache Rowset后，这里的值变为boolean，为此不得不容错.
			entity.setBitfield2("1");
		}
		Assert.assertEquals("1", entity.getBitfield2());

		Assert.assertEquals("192.168.100.128/25", entity.getCidrfield());
		Assert.assertEquals("192.168.1.1", entity.getInetfield());
		Assert.assertEquals("08:00:2b:01:02:03", entity.getMacaddrfield());

		Assert.assertTrue(entity.isBooleanfield1());
		Assert.assertFalse(entity.isBooleanfield2());

		Assert.assertEquals(DateUtils.parseDate("2012-07-27"), entity.getDatefield());
		Assert.assertEquals("2012-07-27 13:00:00", sf.format(entity.getTimestampfield1()));
		System.out.printf("timestampfield2:%s\r\n", entity.getTimestampfield2());

		Calendar gmtlocal = new GregorianCalendar(TimeZone.getTimeZone("GMT-4"));
		gmtlocal.set(Calendar.YEAR, 2012);
		gmtlocal.set(Calendar.MONTH, 6);
		gmtlocal.set(Calendar.DAY_OF_MONTH, 27);
		gmtlocal.set(Calendar.HOUR_OF_DAY, 13);
		gmtlocal.set(Calendar.MINUTE, 00);
		gmtlocal.set(Calendar.SECOND, 00);
		Assert.assertEquals(sf.format(gmtlocal.getTime()), sf.format(entity.getTimestampfield2()));
		Assert.assertEquals("13:00:00", new SimpleDateFormat("HH:mm:ss").format(entity.getTimefield1()));

		System.out.printf("timefield2:%s\r\n", entity.getTimefield2());
		Assert.assertEquals("13:00:00", new SimpleDateFormat("HH:mm:ss").format(entity.getTimefield2()));

		// 通过JDBC和对象方式所取得的intervalfield值有所不同
		System.out.printf("intervalfield:%s\r\n", entity.getIntervalfield());
		if (!StringUtils.endsWithIgnoreCase("1 year 2 mons", entity.getIntervalfield())) {
			Assert.assertTrue(entity.getIntervalfield().startsWith("1 year"));
			Assert.assertTrue(entity.getIntervalfield().contains("2 mons"));
		}

		System.out.printf("binaryfield:%s length:%s\r\n", new String(entity.getBinaryfield()), entity.getBinaryfield().length);
		Assert.assertEquals(4, entity.getBinaryfield().length);
		Assert.assertEquals("text", entity.getTextfield());
		System.out.printf("tsvectorfield:%s\r\n", entity.getTsvectorfield());
		if (isEQ83version || isLE82version) {
			Assert.assertEquals("'a' 'on' 'and' 'ate' 'cat' 'fat' 'mat' 'rat' 'sat'", entity.getTsvectorfield());
		} else {
			Assert.assertEquals("'a' 'and' 'ate' 'cat' 'fat' 'mat' 'on' 'rat' 'sat'", entity.getTsvectorfield());
		}

		Assert.assertEquals("'fat' & 'rat'", entity.getTsqueryfield());

		// 通过JDBC和对象方式所取得的boxfield值有所不同
		System.out.printf("boxfield:%s\r\n", entity.getBoxfield());
		if (!StringUtils.equals("(1,1),(0,0)", entity.getBoxfield())) {
			Assert.assertEquals("(1.0,1.0),(0.0,0.0)", entity.getBoxfield());
		}

		// 通过JDBC和对象方式所取得的circlefield值有所不同
		System.out.printf("circlefield:%s\r\n", entity.getCirclefield());
		if (!StringUtils.equals("<(1,1),2>", entity.getCirclefield())) {
			Assert.assertEquals("<(1.0,1.0),2.0>", entity.getCirclefield());
		}

		Assert.assertNull(entity.getLinefield());

		// 通过JDBC和对象方式所取得的lsegfield值有所不同
		System.out.printf("lsegfield:%s\r\n", entity.getLsegfield());
		if (!StringUtils.equals("[(1,1),(2,2)]", entity.getLsegfield())) {
			Assert.assertEquals("[(1.0,1.0),(2.0,2.0)]", entity.getLsegfield());
		}

		// 通过JDBC和对象方式所取得的pathfield值有所不同
		System.out.printf("pathfield:%s\r\n", entity.getPathfield());
		if (!StringUtils.equals("((0,0),(1,1),(2,2))", entity.getPathfield())) {
			Assert.assertEquals("((0.0,0.0),(1.0,1.0),(2.0,2.0))", entity.getPathfield());
		}

		// 通过JDBC和对象方式所取得的pointfield值有所不同
		System.out.printf("pointfield:%s\r\n", entity.getPointfield());
		if (!StringUtils.equals("(5,5)", entity.getPointfield())) {
			Assert.assertEquals("(5.0,5.0)", entity.getPointfield());
		}

		// 通过JDBC和对象方式所取得的polygonfield值有所不同
		System.out.printf("polygonfield:%s\r\n", entity.getPolygonfield());
		if (!StringUtils.equals("((0,0),(1,1),(2,2))", entity.getPolygonfield())) {
			Assert.assertEquals("((0.0,0.0),(1.0,1.0),(2.0,2.0))", entity.getPolygonfield());
		}

		if (!isLE82version) {
			// Assert.assertEquals("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
			// ((TestColumntypesDb2entity) entity).getUuidfield());
			// Assert.assertEquals("<foo>bar</foo>", ((TestColumntypesDb2entity)
			// entity).getXmlfield());
			Assert.assertNull(((TestColumntypesDb2entity) entity).getTxidfield());
		}
	}

	/**
	 * 测试以对象方式进行insert（表中含PostgreSQL特有data type）
	 * 
	 * <p>
	 * 测试结论：<br>
	 * 当数据库表中有以下类型的列时，使用对象方式的数据插入将会失败：<br>
	 * money, bit varying, bit, interval, <br>
	 * cidr, inet, macaddr, uuid, tsvector, tsquery, xml, txid_snapshot, <br>
	 * box, circle, line, lseg, path, point, polygon.
	 * </p>
	 */
	@Test
	public void testInsertByObjOnSpecialColumns() {
		if(db==null)return;
		try {
			prepareDbByNativeSqls("postgresql_createtable3_test.sql");

			TestColumntypesSpecial entity = new TestColumntypesSpecial();
			// 每次只设置1项属性值，从PSQLException的描述中得到失败原因为：该列类型与值的形式不符。
			entity.setMoneyfield("12.34");
			// entity.setVarbitfield1("101010");
			// entity.setVarbitfield2("1000000");
			// entity.setBitfield1("10101010");
			// entity.setBitfield2("1");
			// entity.setIntervalfield("1-2");
			// entity.setCidrfield("192.168.100.128/25");
			// entity.setInetfield("192.168.1.1");
			// entity.setMacaddrfield("08:00:2b:01:02:03");
			// entity.setUuidfield("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
			// entity.setTsvectorfield("a fat cat sat on a mat and ate a fat rat");
			// entity.setTsqueryfield("fat & rat");
			// entity.setXmlfield("<foo>bar</foo>");
			// entity.setTxidfield("");
			// entity.setBoxfield("((0,0),(1,1))");
			// entity.setCirclefield("((1,1),2)");
			// entity.setLinefield("((0,0),(1,1))");
			// entity.setLsegfield("((1,1),(2,2))");
			// entity.setPathfield("((0,0),(1,1),(2,2))");
			// entity.setPointfield("(5,5)");
			// entity.setPolygonfield("((0,0),(1,1),(2,2))");

			db.insert(entity);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 测试以对象方式进行select（表中含PostgreSQL特有data type）
	 * 
	 * <p>
	 * 测试结论：<br>
	 * 当数据库表中有money类型的列时，使用对象方式的查询数据将会失败：<br>
	 * 获取 money类型值时会产生异常， 因AbstractJdbc2ResultSet 137行将money类型映射到Types.DOUBLE，
	 * 而该类型值带有货币符号，故无法转成double。 这个问题疑似是jdbc的bug。
	 * </p>
	 */
	@Test
	public void testSelectByObjOnMoneyColumn() {
		if(db==null)return;
		try {
			selectByObjOnSpecialColumns(isLE82version ? "postgresql_data_82_test.sql" : "postgresql_data_test.sql");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void selectByObjOnSpecialColumns(String initSql) throws Exception {
		prepareDbByNativeSqls(isLE82version ? "postgresql_createtable_82_test.sql" : "postgresql_createtable_test.sql");
		prepareDbByNativeSqls(initSql);

		if (isLE82version) {
			Query<TestColumntypesDb2entity82> t1 = QB.create(TestColumntypesDb2entity82.class);
			List<TestColumntypesDb2entity82> list = db.select(t1);
			Assert.assertEquals(1, list.size());
			TestColumntypesDb2entity82 entity = list.get(0);
			assertEntityValue(entity, false);
		} else {
			Query<TestColumntypesDb2entity> t1 = QB.create(TestColumntypesDb2entity.class);
			List<TestColumntypesDb2entity> list = db.select(t1);
			Assert.assertEquals(1, list.size());
			TestColumntypesDb2entity entity = list.get(0);
			assertEntityValue(entity, false);
		}
	}

	/**
	 * 测试以对象方式进行select（表中含PostgreSQL特有data type，但money类型无值）
	 * 
	 * <p>
	 * 测试结论：<br>
	 * 带长度的bit类型值也将被获取为布尔类型值。这个问题疑似是jdbc的bug。<br>
	 * 其他特有类型均可以String形式得到其值。
	 * </p>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSelectByObjOnSpecialColumns() throws Exception {
		if(db==null)return;
		selectByObjOnSpecialColumns(isLE82version ? "postgresql_data2_82_test.sql" : "postgresql_data2_test.sql");

	}

	protected void prepareDbByNativeSqls(String sqlFilename) throws SQLException, IOException {
		try {
			db.getMetaData(null).executeScriptFile(ResourceUtils.getResource(sqlFilename));
		} catch (SQLException e) {
			throw new SQLException("Error at executing sql script:" + sqlFilename, e);
		}
	}
}
