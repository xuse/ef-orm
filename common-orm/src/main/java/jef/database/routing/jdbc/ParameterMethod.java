package jef.database.routing.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * PreparedStatement设置参数的处理
 * 
 * @author linxuan
 * 
 */
abstract class ParameterMethod {
	abstract void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException;

	static ParameterMethod setArray = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setArray(index, (Array) args[0]);
		}
	};
	static ParameterMethod setAsciiStream = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setAsciiStream(index, (InputStream) args[0], (Integer) args[1]);
		}
	};
	static ParameterMethod setBigDecimal = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setBigDecimal(index, (BigDecimal) args[0]);
		}
	};
	static ParameterMethod setBinaryStream = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setBinaryStream(index, (InputStream) args[0], (Integer) args[1]);
		}
	};

	static ParameterMethod setBlob = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setBlob(index, (Blob) args[0]);
		}
	};
	static ParameterMethod setBoolean = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setBoolean(index, (Boolean) args[0]);
		}
	};
	static ParameterMethod setByte = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setByte(index, (Byte) args[0]);
		}
	};
	static ParameterMethod setBytes = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setBytes(index, (byte[]) args[0]);
		}
	};
	static ParameterMethod setCharacterStream = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setCharacterStream(index, (Reader) args[0]);
		}
	};
	static ParameterMethod setClob = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setClob(index, (Clob) args[0]);
		}
	};
	static ParameterMethod setDate1 = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setDate(index, (Date) args[0]);
		}
	};
	static ParameterMethod setDate2 = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setDate(index, (Date) args[0], (Calendar) args[1]);
		}
	};
	static ParameterMethod setDouble = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setDouble(index, (Double) args[0]);
		}
	};
	static ParameterMethod setFloat = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setFloat(index, (Float) args[0]);
		}
	};

	static ParameterMethod setInt = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setInt(index, (Integer) args[0]);
		}
	};
	static ParameterMethod setLong = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setLong(index, (Long) args[0]);
		}
	};
	static ParameterMethod setNull1 = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setNull(index, (Integer) args[0]);
		}
	};

	static ParameterMethod setNull2 = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setNull(index, (Integer) args[0], (String) args[1]);
		}
	};
	static ParameterMethod setObject1 = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setObject(index, (Integer) args[0]);
		}
	};
	static ParameterMethod setObject2 = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setObject(index, (Integer) args[0], (Integer) args[1]);
		}
	};
	static ParameterMethod setObject3 = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setObject(index, (Integer) args[0], (Integer) args[1], (Integer) args[2]);
		}
	};

	static ParameterMethod setRef = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setRef(index, (Ref) args[0]);
		}
	};
	static ParameterMethod setShort = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setShort(index, (Short) args[0]);
		}
	};
	static ParameterMethod setString = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setString(index, (String) args[0]);
		}
	};

	static ParameterMethod setTime1 = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setTime(index, (Time) args[0]);
		}
	};
	static ParameterMethod setTime2 = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setTime(index, (Time) args[0], (Calendar) args[1]);
		}
	};
	static ParameterMethod setTimestamp1 = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setTimestamp(index, (Timestamp) args[0]);
		}
	};
	static ParameterMethod setTimestamp2 = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setTimestamp(index, (Timestamp) args[0], (Calendar) args[1]);
		}
	};

	static ParameterMethod setURL = new ParameterMethod() {
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setURL(index, (URL) args[0]);
		}
	};
	static ParameterMethod setUnicodeStream = new ParameterMethod() {
		@SuppressWarnings("deprecation")
		void setParameter(PreparedStatement stmt, int index, Object[] args) throws SQLException {
			stmt.setUnicodeStream(index, (InputStream) args[0], (Integer) args[1]);
		}
	};
}
