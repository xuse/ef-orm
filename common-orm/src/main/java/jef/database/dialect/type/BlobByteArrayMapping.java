package jef.database.dialect.type;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.IResultSet;
import jef.tools.IOUtils;

public class BlobByteArrayMapping extends ATypeMapping<byte[]>{
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, session.getBlobDataType());
		}else{
			byte[] bb=(byte[])value;
			st.setBytes(index, bb);
//			st.setBinaryStream(index, new ByteArrayInputStream(bb),(long)bb.length);
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.BLOB;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		throw new UnsupportedOperationException();
	}

	public boolean isLob() {
		return true;
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		if(obj.getClass().isArray()){
			return obj;
		}
		Blob blob=(Blob) obj;
		InputStream in = blob.getBinaryStream();
		try {
			return IOUtils.toByteArray(in);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
