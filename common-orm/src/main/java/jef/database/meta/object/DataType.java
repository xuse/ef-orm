package jef.database.meta.object;

import javax.persistence.Column;

/**
 * 数据库支持的一个数据类型
 * 
 * @author Jiyi
 * 
 */
public final class DataType {
	/**
	 * name
	 */
	@Column(name = "TYPE_NAME")
	private String typeName;
	/**
	 * SQL data type from java.sql.Types
	 */
	@Column(name = "DATA_TYPE")
	private int dataType;
	/**
	 * maximum precision
	 */
	@Column(name = "PRECISION")
	private int precision;

	/**
	 * can you use NULL for this type. typeNoNulls - does not allow NULL values
	 * typeNullable - allows NULL values typeNullableUnknown - nullability
	 * unknown
	 */
	@Column(name = "NULLABLE")
	private short nullable;

	/**
	 * boolean=> is it case sensitive.
	 */
	@Column(name = "CASE_SENSITIVE")
	private boolean caseSensitive;

	/**
	 * short => can you use "WHERE" based on this type: typePredNone - No
	 * support typePredChar - Only supported with WHERE .. LIKE typePredBasic -
	 * Supported except for WHERE .. LIKE typeSearchable - Supported for all
	 * WHERE ..
	 */
	@Column(name = "SEARCHABLE")
	private short searchable;

	/**
	 * boolean => is it unsigned.
	 * */
	@Column(name = "UNSIGNED_ATTRIBUTE")
	private boolean unsigned;

	/**
	 * can it be a money value.
	 */
	@Column(name = "FIXED_PREC_SCALE")
	private boolean fixedPrecScale;

	/**
	 * can it be used for an auto-increment value.
	 */
	@Column(name = "AUTO_INCREMENT")
	private boolean autoIncrement;

	/**
	 * minimum scale supported
	 */
	@Column(name = "MINIMUM_SCALE")
	private short minimumScale;

	/**
	 * maximum scale supported
	 * 
	 */
	@Column(name = "MAXIMUM_SCALE")
	private short maximumScale;

	@Column(name = "SQL_DATA_TYPE")
	private int sqlDataType;
	/**
	 * int => usually 2 or 10
	 */
	@Column(name = "NUM_PREC_RADIX")
	private int numPrecRadix;

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		this.dataType = dataType;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public short isNullable() {
		return nullable;
	}

	public void setNullable(short nullable) {
		this.nullable = nullable;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public short getSearchable() {
		return searchable;
	}

	public void setSearchable(short searchable) {
		this.searchable = searchable;
	}

	public boolean isUnsigned() {
		return unsigned;
	}

	public void setUnsigned(boolean unsigned) {
		this.unsigned = unsigned;
	}

	public boolean isFixedPrecScale() {
		return fixedPrecScale;
	}

	public void setFixedPrecScale(boolean fixedPrecScale) {
		this.fixedPrecScale = fixedPrecScale;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public void setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
	}

	public short getMinimumScale() {
		return minimumScale;
	}

	public void setMinimumScale(short minimumScale) {
		this.minimumScale = minimumScale;
	}

	public short getMaximumScale() {
		return maximumScale;
	}

	public void setMaximumScale(short maximumScale) {
		this.maximumScale = maximumScale;
	}

	public int getSqlDataType() {
		return sqlDataType;
	}

	public void setSqlDataType(int sqlDataType) {
		this.sqlDataType = sqlDataType;
	}

	public int getNumPrecRadix() {
		return numPrecRadix;
	}

	public void setNumPrecRadix(int numPrecRadix) {
		this.numPrecRadix = numPrecRadix;
	}

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder(typeName);
		sb.append('(').append(dataType).append(')');
		if(this.autoIncrement){
			sb.append(" auto-increament");
		}
		if(this.unsigned){
			sb.append(" unsigned");
		}
		if(this.dataType==java.sql.Types.VARCHAR || this.dataType==java.sql.Types.CHAR){
			if(!this.caseSensitive){
				sb.append(" case-insensitive");
			}	
		}
		if(this.fixedPrecScale){
			sb.append(" fixed-scale");
		}
		return sb.toString();
	}
}
