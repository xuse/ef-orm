package jef.database.routing.jdbc;

import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.regex.Pattern;

import jef.tools.StringUtils;

public class SqlTypeParser {
	/**
	 * 用于判断是否是一个select ... for update的sql
	 */
	private static final Pattern SELECT_FOR_UPDATE_PATTERN = Pattern.compile(
			"^select\\s+.*\\s+for\\s+update.*$", Pattern.CASE_INSENSITIVE);

	public static boolean isQuerySql(String sql) throws SQLException {
		SqlType sqlType = getSqlType(sql);
		if (sqlType == SqlType.SELECT || sqlType == SqlType.SELECT_FOR_UPDATE
				|| sqlType == SqlType.SHOW || sqlType == SqlType.DUMP
				|| sqlType == SqlType.DEBUG || sqlType == SqlType.EXPLAIN) {
			return true;
		} else if (sqlType == SqlType.INSERT || sqlType == SqlType.UPDATE
				|| sqlType == SqlType.DELETE || sqlType == SqlType.REPLACE
				|| sqlType == SqlType.TRUNCATE || sqlType == SqlType.CREATE
				|| sqlType == SqlType.DROP || sqlType == SqlType.LOAD
				|| sqlType == SqlType.MERGE || sqlType == SqlType.ALTER
				|| sqlType == SqlType.RENAME) {
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
		// #bug 2011-11-24,modify by junyu,先不走缓存，否则sql变化巨大，缓存换入换出太多，gc太明显
		// SqlType sqlType = globalCache.getSqlType(sql);
		// if (sqlType == null) {
		SqlType sqlType = null;
		// #bug 2011-12-8,modify by junyu ,this code use huge cpu resource,and
		// most
		// sql have no comment,so first simple look for there whether have the
		// comment
		String noCommentsSql = sql;
		if (sql.contains("/*")) {
			noCommentsSql = stripComments(sql, "'\"", "'\"", true,
					false, true, true).trim();
		}

		if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "select")) {
			// #bug 2011-12-9,this select-for-update regex has low
			// performance,so
			// first judge this sql whether have ' for ' string.
			if (noCommentsSql.toLowerCase().contains(" for ")
					&& SELECT_FOR_UPDATE_PATTERN.matcher(noCommentsSql)
							.matches()) {
				sqlType = SqlType.SELECT_FOR_UPDATE;
			} else {
				sqlType = SqlType.SELECT;
			}
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"insert")) {
			sqlType = SqlType.INSERT;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"update")) {
			sqlType = SqlType.UPDATE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"delete")) {
			sqlType = SqlType.DELETE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "show")) {
			sqlType = SqlType.SHOW;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"replace")) {
			sqlType = SqlType.REPLACE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"truncate")) {
			sqlType = SqlType.TRUNCATE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"create")) {
			sqlType = SqlType.CREATE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "drop")) {
			sqlType = SqlType.DROP;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "load")) {
			sqlType = SqlType.LOAD;
		} else if (StringUtils
				.startsWithIgnoreCaseAndWs(noCommentsSql, "merge")) {
			sqlType = SqlType.MERGE;
		} else if (StringUtils
				.startsWithIgnoreCaseAndWs(noCommentsSql, "alter")) {
			sqlType = SqlType.ALTER;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"rename")) {
			sqlType = SqlType.RENAME;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"dump")) {
			sqlType = SqlType.DUMP;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"debug")) {
			sqlType = SqlType.DEBUG;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"explain")) {
			sqlType = SqlType.EXPLAIN;
		} else {
			throwNotSupportSqlTypeException();
		}
		return sqlType;
	}
	
	public static boolean throwNotSupportSqlTypeException() throws SQLException{
		throw new SQLException(
				"only select, insert, update, delete, replace, show, truncate, create, drop, load, merge, dump sql is supported");
	}
	
	/**
	 * Returns the given string, with comments removed
	 * 
	 * @param src
	 *            the source string
	 * @param stringOpens
	 *            characters which delimit the "open" of a string
	 * @param stringCloses
	 *            characters which delimit the "close" of a string, in
	 *            counterpart order to <code>stringOpens</code>
	 * @param slashStarComments
	 *            strip slash-star type "C" style comments
	 * @param slashSlashComments
	 *            strip slash-slash C++ style comments to end-of-line
	 * @param hashComments
	 *            strip #-style comments to end-of-line
	 * @param dashDashComments
	 *            strip "--" style comments to end-of-line
	 * @return the input string with all comment-delimited data removed
	 */
	private static String stripComments(String src, String stringOpens,
			String stringCloses, boolean slashStarComments,
			boolean slashSlashComments, boolean hashComments,
			boolean dashDashComments) {
		if (src == null) {
			return null;
		}

		StringBuffer buf = new StringBuffer(src.length());

		// It's just more natural to deal with this as a stream
		// when parsing..This code is currently only called when
		// parsing the kind of metadata that developers are strongly
		// recommended to cache anyways, so we're not worried
		// about the _1_ extra object allocation if it cleans
		// up the code

		StringReader sourceReader = new StringReader(src);

		int contextMarker = Character.MIN_VALUE;
		boolean escaped = false;
		int markerTypeFound = -1;

		int ind = 0;

		int currentChar = 0;

		try {
			while ((currentChar = sourceReader.read()) != -1) {

//				if (false && currentChar == '\\') {
//					escaped = !escaped;
//				} else
					if (markerTypeFound != -1 && currentChar == stringCloses.charAt(markerTypeFound)
						&& !escaped) {
					contextMarker = Character.MIN_VALUE;
					markerTypeFound = -1;
				} else if ((ind = stringOpens.indexOf(currentChar)) != -1
						&& !escaped && contextMarker == Character.MIN_VALUE) {
					markerTypeFound = ind;
					contextMarker = currentChar;
				}

				if (contextMarker == Character.MIN_VALUE && currentChar == '/'
						&& (slashSlashComments || slashStarComments)) {
					currentChar = sourceReader.read();
					if (currentChar == '*' && slashStarComments) {
						int prevChar = 0;
						while ((currentChar = sourceReader.read()) != '/'
								|| prevChar != '*') {
							if (currentChar == '\r') {

								currentChar = sourceReader.read();
								if (currentChar == '\n') {
									currentChar = sourceReader.read();
								}
							} else {
								if (currentChar == '\n') {

									currentChar = sourceReader.read();
								}
							}
							if (currentChar < 0)
								break;
							prevChar = currentChar;
						}
						continue;
					} else if (currentChar == '/' && slashSlashComments) {
						while ((currentChar = sourceReader.read()) != '\n'
								&& currentChar != '\r' && currentChar >= 0)
							;
					}
				} else if (contextMarker == Character.MIN_VALUE
						&& currentChar == '#' && hashComments) {
					// Slurp up everything until the newline
					while ((currentChar = sourceReader.read()) != '\n'
							&& currentChar != '\r' && currentChar >= 0)
						;
				} else if (contextMarker == Character.MIN_VALUE
						&& currentChar == '-' && dashDashComments) {
					currentChar = sourceReader.read();

					if (currentChar == -1 || currentChar != '-') {
						buf.append('-');

						if (currentChar != -1) {
							buf.append(currentChar);
						}

						continue;
					}

					// Slurp up everything until the newline

					while ((currentChar = sourceReader.read()) != '\n'
							&& currentChar != '\r' && currentChar >= 0)
						;
				}

				if (currentChar != -1) {
					buf.append((char) currentChar);
				}
			}
		} catch (IOException ioEx) {
			// we'll never see this from a StringReader
		}

		return buf.toString();
	}
}
