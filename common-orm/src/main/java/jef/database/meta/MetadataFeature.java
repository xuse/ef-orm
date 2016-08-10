package jef.database.meta;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.DbUtils;

import org.apache.commons.lang.builder.ToStringBuilder;

public class MetadataFeature {

	/**
	 * 存入数据库时变大小写变化,如Oracle始终变大写
	 */
	private Case defaultCase;
	private boolean supportMixed;
	private Case quotedCase;
	private boolean supportMixedQuoted;
	private String quoteChar;
	// 运行时缓存
	private String[] tableTypes;
	/**
	 * 记录数据库是否支持恢复点
	 */
	private boolean supportsSavepoints;

	/**
	 * JDBC版本
	 */
	private int jdbcVersion;

	public Case getDefaultCase() {
		return defaultCase;
	}

	public void setDefaultCase(Case defaultCase) {
		this.defaultCase = defaultCase;
	}

	public boolean isSupportMixed() {
		return supportMixed;
	}

	public void setSupportMixed(boolean supportMixed) {
		this.supportMixed = supportMixed;
	}

	public Case getQuotedCase() {
		return quotedCase;
	}

	public void setQuotedCase(Case quotedCase) {
		this.quotedCase = quotedCase;
	}

	public boolean isSupportMixedQuoted() {
		return supportMixedQuoted;
	}

	public String getQuoteChar() {
		return quoteChar;
	}

	public void setQuoteChar(String quoteChar) {
		this.quoteChar = quoteChar;
	}

	public void setSupportMixedQuoted(boolean supportMixedQuoted) {
		this.supportMixedQuoted = supportMixedQuoted;
	}

	public boolean supportsSavepoints() {
		return this.supportsSavepoints;
	}

	public MetadataFeature(DatabaseMetaData metadata) throws SQLException {
		this.supportMixed = metadata.supportsMixedCaseIdentifiers();
		this.supportMixedQuoted = metadata.supportsMixedCaseQuotedIdentifiers();
		this.quoteChar = metadata.getIdentifierQuoteString();
		if (metadata.storesUpperCaseIdentifiers()) {
			this.defaultCase = Case.UPPER;
		} else if (metadata.storesLowerCaseIdentifiers()) {
			this.defaultCase = Case.LOWER;
		} else if (metadata.storesMixedCaseIdentifiers()) {
			this.defaultCase = Case.MIXED;
		} else {
			throw new SQLException("The database driver " + metadata.getClass().getName() + " not support JDBC case feature.");
		}
		if (metadata.storesUpperCaseQuotedIdentifiers()) {
			this.quotedCase = Case.UPPER;
		} else if (metadata.storesLowerCaseQuotedIdentifiers()) {
			this.quotedCase = Case.LOWER;
		} else if (metadata.storesMixedCaseQuotedIdentifiers()) {
			this.quotedCase = Case.MIXED;
		} else {
			this.quotedCase = Case.MIXED;
		}
		supportsSavepoints = metadata.supportsSavepoints();
		try {
			jdbcVersion = caclJdbcVersion(metadata.getConnection());
		} catch (SQLException e) {
			jdbcVersion = -1;
		}
		List<String> type = new ArrayList<String>();
		ResultSet rs = null;
		try {
			rs = metadata.getTableTypes();
			while (rs.next()) {
				type.add(rs.getString(1));
			}
		} finally {
			DbUtils.close(rs);
		}
		this.tableTypes = type.toArray(new String[type.size()]);
	}

	private int caclJdbcVersion(Connection conn) throws SQLException {
		try {
			if (testJdbc4(conn))
				return 4;
			if (testJdbc3(conn))
				return 3;
			if (testJdbc2(conn))
				return 2;
			return 1;
		} catch (Exception e) {
			LogUtil.exception(e);
		}
		return -1;
	}

	private boolean testJdbc2(Connection conn) {
		try {
			Statement st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			DbUtils.close(st);
		} catch (SQLException e) {
			LogUtil.exception(e);
		} catch (AbstractMethodError e) {
			return false;
		}
		return true;
	}

	private boolean testJdbc3(Connection conn) {
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.getGeneratedKeys();
		} catch (SQLFeatureNotSupportedException e) {
			return false;
		} catch (AbstractMethodError e) {
			return false;
		} catch (SQLException e) {
		} finally {
			DbUtils.close(rs);
			DbUtils.close(st);
		}
		return true;
	}

	private boolean testJdbc4(Connection conn) {
		try {
			conn.isValid(1);
			return true;
		} catch (AbstractMethodError e) {
			return false;
		} catch (SQLException e) {
		}
		try {
			Statement st = conn.createStatement();
			DbUtils.close(st);
			st.isClosed();
			return true;
		} catch (AbstractMethodError e) {
			return false;
		} catch (SQLException e) {
		}
		try {
			conn.createBlob();
			return true;
		} catch (AbstractMethodError e) {
			return false;
		} catch (SQLException e) {
			return false;
		}
	}

	public String[] getTableTypes() {
		return tableTypes;
	}

	public int getJdbcVersion() {
		return jdbcVersion;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	

}
