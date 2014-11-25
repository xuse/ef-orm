package jef.database.support;

import jef.common.log.LogUtil;
import jef.database.DbCfg;
import jef.database.jdbc.JDBCTarget;
import jef.tools.JefConfiguration;

/**
 * SQL日志工具
 * 
 * @author jiyi
 * 
 */
public abstract class SqlLog {
	public abstract void append(int count, Object fieldName, Object value);

	public abstract void append(JDBCTarget db);

	/**
	 * 输出日志并清除缓冲区
	 */
	public abstract void output();

	public abstract void clear();

	public abstract SqlLog append(CharSequence csq);

	public abstract SqlLog append(CharSequence csq, CharSequence csq2);

	public abstract SqlLog append(CharSequence csq, int csq2);

	public abstract SqlLog append(CharSequence csq, long csq2);

	public abstract SqlLog append(int csq);

	public abstract SqlLog append(long csq);

	public abstract SqlLog append(char csq);

	public abstract SqlLog append(double csq);

	public abstract boolean isDebug();

	public abstract void directLog(String string);

	public abstract void ensureCapacity(int size);

	public final static class LogImpl extends SqlLog {
		private static LogFormat formatter = getLogFormat();

		private static LogFormat getLogFormat() {
			if ("no_wrap".equalsIgnoreCase(JefConfiguration.get(DbCfg.DB_LOG_FORMAT))) {
				return new jef.database.support.LogFormat.NowrapLineLogFormat();
			}
			return new jef.database.support.LogFormat.Default();
		}

		private StringBuilder sb;

		public LogImpl() {
			this(32);
		}

		public LogImpl(int i) {
			sb = new StringBuilder(i);
		}

		@Override
		public LogImpl append(CharSequence csq) {
			sb.append(csq);
			return this;
		}

		@Override
		public LogImpl append(char c) {
			sb.append(c);
			return this;
		}

		@Override
		public void output() {
			if (sb.length() > 0) {
				LogUtil.info(sb.toString());
				sb.setLength(0);
			}
		}

		@Override
		public void clear() {
			sb.setLength(0);
		}

		@Override
		public SqlLog append(int csq) {
			sb.append(csq);
			return this;
		}

		@Override
		public SqlLog append(long csq) {
			sb.append(csq);
			return this;
		}

		@Override
		public SqlLog append(double csq) {
			sb.append(csq);
			return this;
		}

		@Override
		public SqlLog append(CharSequence csq, CharSequence csq2) {
			sb.append(csq).append(csq2);
			return this;
		}

		@Override
		public SqlLog append(CharSequence csq, int csq2) {
			sb.append(csq).append(csq2);
			return this;
		}

		@Override
		public boolean isDebug() {
			return true;
		}

		public void directLog(String string) {
			LogUtil.info(string);
		}

		@Override
		public void append(int count, Object fieldName, Object value) {
			formatter.log(sb, count, String.valueOf(fieldName), value);
		}

		@Override
		public void append(JDBCTarget db) {
			sb.append(" | ").append(db.getTransactionId());
		}

		@Override
		public void ensureCapacity(int size) {
			sb.ensureCapacity(size);
		}

		@Override
		public SqlLog append(CharSequence csq, long l) {
			sb.append(csq).append(l);
			return this;
		}
	}

	/**
	 * 日志空实现
	 */
	public static final SqlLog DUMMY = new SqlLog() {
		public void output() {
		}

		public void clear() {
		}

		public SqlLog append(CharSequence csq) {
			return this;
		}

		public SqlLog append(CharSequence csq, CharSequence csq2) {
			return this;
		}

		public SqlLog append(CharSequence csq, int value) {
			return this;
		}

		public SqlLog append(int i) {
			return this;
		}

		public SqlLog append(long l) {
			return this;
		}

		public SqlLog append(char c) {
			return this;
		}

		public SqlLog append(double d) {
			return this;
		}

		@Override
		public SqlLog append(CharSequence csq, long csq2) {
			return this;
		}

		@Override
		public boolean isDebug() {
			return false;
		}

		@Override
		public void directLog(String string) {
		}

		@Override
		public void append(int count, Object fieldName, Object value) {
		}

		@Override
		public void append(JDBCTarget db) {
		}

		@Override
		public void ensureCapacity(int size) {
		}
	};

}
