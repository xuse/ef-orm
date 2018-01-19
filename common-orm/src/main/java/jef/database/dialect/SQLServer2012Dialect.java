package jef.database.dialect;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import jef.database.DbMetaData;
import jef.database.dialect.handler.DerbyLimitHandler;
import jef.database.dialect.handler.LimitHandler;
import jef.database.jdbc.result.IResultSet;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.meta.object.SequenceInfo;
import jef.database.query.Func;
import jef.database.query.function.StandardSQLFunction;
import jef.database.wrapper.populator.ListResultSetTransformer;
import jef.tools.StringUtils;

import com.querydsl.sql.SQLServer2012Templates;
import com.querydsl.sql.SQLTemplates;

/**
 * SQL Server 2012
 * 
 * @author jiyi
 *
 */
public class SQLServer2012Dialect extends SQLServer2008Dialect {

	public SQLServer2012Dialect() {
		super();
		// 2012开始支持SEQUENCE
		features.add(Feature.SUPPORT_SEQUENCE);
		super.setProperty(DbProperty.SEQUENCE_FETCH, "SELECT next value for %s");
		addFunctions();
	}

	/*
	 * 2012新增的14个函数 Reference http://msdn.microsoft.com/en-us/library/09f
	 * 0096e-ab95-4be0-8c01-f98753255747(v=sql.110)
	 */
	private void addFunctions() {
		registerNative(new StandardSQLFunction("parse"));
		registerNative(new StandardSQLFunction("try_convert"));
		registerNative(new StandardSQLFunction("try_parse"));
		registerNative(new StandardSQLFunction("datefromparts"));
		registerNative(new StandardSQLFunction("datetime2fromparts"));
		registerNative(new StandardSQLFunction("datetimefromparts"));
		registerNative(new StandardSQLFunction("datetimeoffsetfromparts"));
		registerNative(new StandardSQLFunction("eomonth"));
		registerNative(new StandardSQLFunction("smalldatetimefromparts"));
		registerNative(new StandardSQLFunction("timefromparts"));

		registerNative(new StandardSQLFunction("choose"));
		registerNative(new StandardSQLFunction("iif"));
		registerNative(new StandardSQLFunction("format"));
		super.features.remove(Feature.CONCAT_IS_ADD);
		registerNative(Func.concat);// 2012开始支持原生的concat函数。

		// 2012新增的分析函数
		// CUME_DIST (Transact-SQL)
		// LAST_VALUE (Transact-SQL)
		// PERCENTILE_DISC (Transact-SQL)
		//
		// FIRST_VALUE (Transact-SQL)
		// LEAD (Transact-SQL)
		// PERCENT_RANK (Transact-SQL)
		//
		// LAG (Transact-SQL)
		// PERCENTILE_CONT (Transact-SQL)
		//

	}

	@Override
	public List<SequenceInfo> getSequenceInfo(DbMetaData conn, String schema, String seqName) throws SQLException {
		schema = StringUtils.isBlank(schema) ? "%" : schema;
		seqName = StringUtils.isBlank(seqName) ? "%" : seqName;
		String sql = "SELECT CONVERT(varchar,seq.name),CONVERT(int,seq.cache_size),"
				+ "CONVERT(bigint,seq.current_value),"
				+ "CONVERT(bigint,seq.minimum_value),"
				+ "CONVERT(bigint,seq.start_value),"
				+ "CONVERT(int,seq.increment),"
				+ "CONVERT(varchar,m.name) as schema_name from sys.sequences seq, sys.schemas m WHERE seq.SCHEMA_ID=m.SCHEMA_ID "
				+ "AND seq.name LIKE ? AND m.name LIKE ?";
		return conn.selectBySql(sql, new ListResultSetTransformer<SequenceInfo>() {
			protected SequenceInfo transform(IResultSet rs) throws SQLException {
				SequenceInfo s=new SequenceInfo();
				s.setName(rs.getString(1));
				s.setCacheSize(rs.getInt(2));
				s.setCurrentValue(rs.getLong(3));
				s.setMinValue(rs.getLong(4));
				s.setStartValue(rs.getLong(5));
				s.setStep(rs.getInt(6));
				s.setSchema(rs.getString(7));
				return s;
			}
		}, Arrays.asList(seqName, schema));
	}

	@Override
	protected LimitHandler generateLimitHander() {
		return new DerbyLimitHandler();
	}

	// to be override
	protected SQLTemplates generateQueryDslTemplates() {
		return new SQLServer2012Templates();
	}

	// private final static String PAGE_SQL_2012 =
	// " offset %start% row fetch next %next% rows only";
	//
	//
	// @Override
	// public String toPageSQL(String sql, IntRange range) {
	// String start = String.valueOf(range.getLeastValue() - 1);
	// String next = String.valueOf(range.getGreatestValue() -
	// range.getLeastValue() + 1);
	// String limit = StringUtils.replaceEach(PAGE_SQL_2012, new String[] {
	// "%start%", "%next%" }, new String[] { start, next });
	// return sql.concat(limit);
	// }

}
