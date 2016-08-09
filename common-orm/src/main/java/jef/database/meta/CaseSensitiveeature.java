package jef.database.meta;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class CaseSensitiveeature {
	
	/**
	 * 存入数据库时变大小写变化,如Oracle始终变大写
	 */
	private Case defaultCase;
	private boolean supportMixed;
	private Case quotedCase;
	private boolean supportMixedQuoted;
	private String quoteChar;
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

	CaseSensitiveeature(DatabaseMetaData metadata) throws SQLException{
		this.supportMixed=metadata.supportsMixedCaseIdentifiers();
		this.supportMixedQuoted=metadata.supportsMixedCaseQuotedIdentifiers();
		this.quoteChar=metadata.getIdentifierQuoteString();
		if(metadata.storesUpperCaseIdentifiers()){
			this.defaultCase=Case.UPPER;
		}else if(metadata.storesLowerCaseIdentifiers()){
			this.defaultCase=Case.LOWER;
		}else if(metadata.storesMixedCaseIdentifiers()){
			this.defaultCase=Case.MIXED;
		}else{
			throw new SQLException("The database driver "+metadata.getClass().getName()+" not support JDBC case feature.");
		}
		if(metadata.storesUpperCaseQuotedIdentifiers()){
			this.quotedCase=Case.UPPER;
		}else if(metadata.storesLowerCaseQuotedIdentifiers()){
			this.quotedCase=Case.LOWER;
		}else if(metadata.storesMixedCaseQuotedIdentifiers()){
			this.quotedCase=Case.MIXED;
		}else{
			throw new SQLException("The database driver "+metadata.getClass().getName()+" not support JDBC case feature.");
		}
	}
}
