package jef.database.routing.sql;

import java.sql.SQLException;
import java.util.regex.Pattern;

import jef.database.routing.jdbc.SqlType;
import jef.tools.StringUtils;

public class SqlTypeParser {
	/**
	 * 用于判断是否是一个select ... for update的sql
	 */
	private static final Pattern SELECT_FOR_UPDATE_PATTERN = Pattern.compile("^select\\s+.*\\s+for\\s+update.*$", Pattern.CASE_INSENSITIVE);

	public static boolean isQuerySql(String sql) throws SQLException {
		SqlType sqlType = getSqlType(sql);
		if (sqlType == SqlType.SELECT || sqlType == SqlType.SELECT_FOR_UPDATE || sqlType == SqlType.SHOW || sqlType == SqlType.DUMP || sqlType == SqlType.DEBUG || sqlType == SqlType.EXPLAIN) {
			return true;
		} else if (sqlType == SqlType.INSERT || sqlType == SqlType.UPDATE || sqlType == SqlType.DELETE || sqlType == SqlType.REPLACE || sqlType == SqlType.TRUNCATE || sqlType == SqlType.CREATE || sqlType == SqlType.DROP || sqlType == SqlType.LOAD || sqlType == SqlType.MERGE
				|| sqlType == SqlType.ALTER || sqlType == SqlType.RENAME) {
			return false;
		} else {
			return throwNotSupportSqlTypeException();
		}
	}

	/**
	 * 获得SQL语句种类
	 * 
	 * @param sql
	 *            SQL语句
	 * @throws SQLException
	 *             当SQL语句不是SELECT、INSERT、UPDATE、DELETE语句时，抛出异常。
	 */
	public static SqlType getSqlType(String sql) throws SQLException {
		SqlType sqlType = null;
		String noCommentsSql = sql;
		if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "select")) {
			if (noCommentsSql.toLowerCase().contains(" for ") && SELECT_FOR_UPDATE_PATTERN.matcher(noCommentsSql).matches()) {
				sqlType = SqlType.SELECT_FOR_UPDATE;
			} else {
				sqlType = SqlType.SELECT;
			}
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "insert")) {
			sqlType = SqlType.INSERT;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "update")) {
			sqlType = SqlType.UPDATE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "delete")) {
			sqlType = SqlType.DELETE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "show")) {
			sqlType = SqlType.SHOW;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "replace")) {
			sqlType = SqlType.REPLACE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "truncate")) {
			sqlType = SqlType.TRUNCATE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "create")) {
			sqlType = SqlType.CREATE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "drop")) {
			sqlType = SqlType.DROP;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "load")) {
			sqlType = SqlType.LOAD;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "merge")) {
			sqlType = SqlType.MERGE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "alter")) {
			sqlType = SqlType.ALTER;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "rename")) {
			sqlType = SqlType.RENAME;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "dump")) {
			sqlType = SqlType.DUMP;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "debug")) {
			sqlType = SqlType.DEBUG;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "explain")) {
			sqlType = SqlType.EXPLAIN;
		} else {
			throwNotSupportSqlTypeException();
		}
		return sqlType;
	}

	public static boolean throwNotSupportSqlTypeException() throws SQLException {
		throw new SQLException("only select, insert, update, delete, replace, show, truncate, create, drop, load, merge, dump sql is supported");
	}
}
