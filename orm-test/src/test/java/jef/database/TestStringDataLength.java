package jef.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;

import jef.database.dialect.ColumnType;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.TupleMetadata;
import jef.database.query.Func;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.onetable.model.CaAsset;
import jef.tools.string.RandomData;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	@DataSource(name = "oracle", url = "${oracle.url}", user = "${oracle.user}", password = "${oracle.password}"),
	@DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	@DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"),
	@DataSource(name = "hsqldb", url = "${hsqldb.url}", user = "sa", password = ""),
	@DataSource(name = "derby", url = "${derby.url}"),
	@DataSource(name = "sqlite", url = "${sqlite.url}"),
	@DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class TestStringDataLength extends org.junit.Assert {

	private DbClient db;

	@DatabaseInit
	public void setup() throws SQLException {
		TupleMetadata meta = new TupleMetadata("XX");
		meta.addColumn("id", new ColumnType.Int(3));
		meta.addColumn("num", new ColumnType.Int(3));
		meta.addColumn("data", new ColumnType.Varchar(50));
		db.createTable(meta);

	}

	/**
	 * 
	 * @throws Exception
	 */
	@IgnoreOn({ "mysql", "postgresql", "hsqldb", "derby" })
	@Test
	public void testDbInfo() throws Exception {
		byte[] v = RandomData.randomByteArray(50);

		System.out.println("========== US-ASCII ===========");
		try {
			String s = new String(v, "US-ASCII");
			db.executeSql("insert into XX(ID,NUM,DATA) values(1,3,?)", s);
		} catch (SQLException e) {
			String message = e.getMessage();
			System.out.println(message);
			// ORA-12899: 列 "POMELO"."XX"."DATA" 的值太大 (实际值: 75, 最大值: 50)
		}
		System.out.println("========== iso-8859-1 ===========");
		try {
			String s = new String(v, "iso-8859-1");// 70
			db.executeSql("insert into XX(ID,NUM,DATA) values(1,3,?)", s);
		} catch (SQLException e) {
			String message = e.getMessage();
			System.out.println(message);
			// ORA-12899: 列 "POMELO"."XX"."DATA" 的值太大 (实际值: 74, 最大值: 50)
		}
		System.out.println("========== gb18030 ===========");
		try {
			String s = new String(v, "gb18030");// 成功
			db.executeSql("insert into XX(ID,NUM,DATA) values(1,3,?)", s);
		} catch (SQLException e) {
			String message = e.getMessage();
			System.out.println(message);
			// ORA-12899: 列 "POMELO"."XX"."DATA" 的值太大 (实际值: 51, 最大值: 50)
		}

	}

	@Test
	public void testParse() throws ParseException {
		// String s="nvl(columnA,columnB)";
		// String s="columnA || 'vv'";
		String s = "concat(columnA , 'vv')";
		Expression ex = DbUtils.parseExpression(s);
		// Expression ex1=DbUtils.parseBinaryExpression(s);
		System.out.println(ex);
	}

	@IgnoreOn({ "mysql", "oracle", "hsqldb", "derby" ,"sqlite"})
	@Test
	public void testPostgresqlSavePoints() throws Exception {
		db.dropTable(CaAsset.class);
		db.createTable(CaAsset.class);

		CaAsset t1 = RandomData.newInstance(CaAsset.class);
		db.insert(t1);

		Transaction session = db.startTransaction();
		{//故意出錯
			CaAsset t2 = RandomData.newInstance(CaAsset.class);
			t2.setAssetId(t1.getAssetId());
			try(Connection conn=session.selectTarget(null).get()){
				PreparedStatement stmt = conn.prepareStatement("insert into ca_asset(normal,acct_id,asset_type,valid_date,asset_id) values(?,?,?,?,?)");
				try {
					stmt.setString(1, "廖丘");
					stmt.setInt(2, 20474239);
					stmt.setInt(3, 109);
					stmt.setDate(4, new java.sql.Date(12333));
					stmt.setLong(5, t1.getAssetId());
					stmt.executeUpdate();
				} catch (Exception e) {
					System.out.println(e.getMessage());
				} finally {
					stmt.close();
				}				
			}
		}
		{
			try {//故意出錯
				session.selectBySql("select current_timestamp from dual", Date.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("===" + session.getExpressionValue(Func.current_timestamp, Object.class));
		session.commit(true);
	}
	
	@Test
	public void getCurrentTimestamp() throws SQLException{
		Date d=db.getExpressionValue(Func.current_timestamp, Date.class);
		System.out.println(d);
	}
}
