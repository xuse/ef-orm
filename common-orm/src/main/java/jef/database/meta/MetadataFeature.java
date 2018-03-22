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
import jef.database.meta.object.Case;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.util.Assert;

public class MetadataFeature {
	/**
	 * 存入数据库时变大小写变化,如Oracle始终变大写
	 */
	private Case defaultCase;

	/**
	 * 大部分数据库都支持用引号来标注数据库对象。主要用途 1、让数据库认识到这是一个对象名而不是数据库关键字 2、保留原先的大小写
	 * 因此一般来说加了引号以后，数据库对象都变为混合大小写
	 */
	private Case quotedCase;

	/**
	 * 引号字符串
	 */
	private String quoteChar;
	/**
	 * 支持的表类型
	 */
	private String[] tableTypes;

	/**
	 * 数据库是否支持恢复点
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

	public Case getQuotedCase() {
		return quotedCase;
	}

	public void setQuotedCase(Case quotedCase) {
		this.quotedCase = quotedCase;
	}

	public boolean isSupportsSavepoints() {
		return supportsSavepoints;
	}

	public String getQuoteChar() {
		return quoteChar;
	}

	public void setQuoteChar(String quoteChar) {
		this.quoteChar = quoteChar;
	}

	public boolean supportsSavepoints() {
		return this.supportsSavepoints;
	}

	public MetadataFeature(DatabaseMetaData metadata) throws SQLException {
		Assert.notNull(metadata,"Not init");
		this.quoteChar = metadata.getIdentifierQuoteString();
		if (metadata.supportsMixedCaseIdentifiers() || metadata.storesMixedCaseIdentifiers()) {
			this.defaultCase = Case.MIXED_SENSITIVE;
		} else if (metadata.storesUpperCaseIdentifiers()) {
			this.defaultCase = Case.UPPER;
		} else if (metadata.storesLowerCaseIdentifiers()) {
			this.defaultCase = Case.LOWER;
		} else {
			throw new SQLException("The database driver " + metadata.getClass().getName() + " not support JDBC case feature.");
		}
		// 增加引号后。有三种情况 1、变为大小写敏感（大部分数据） 2、不支持引号（SQLITE）3、自动转小写（MYSQL开启对应开关后）
		if (metadata.supportsMixedCaseQuotedIdentifiers()) {// 大小写敏感
			this.quotedCase = Case.MIXED_SENSITIVE;
		} else {
			this.quotedCase = this.defaultCase;
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
