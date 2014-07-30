package jef.database.dialect.type;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Feature;
import jef.database.wrapper.IResultSet;
import jef.tools.IOUtils;

public class BlobFileMapping extends ATypeMapping<File>{

	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect profile) throws SQLException {
		File file=(File)value;
		if(value==null || !file.exists()){
			st.setNull(index, profile.getBlobDataType());
		}else{
			try {
				if(profile.has(Feature.NOT_SUPPORT_SET_BINARY)){
					st.setBytes(index, IOUtils.toByteArray(file));
				}else{
					st.setBinaryStream(index, IOUtils.getInputStream(file),file.length());
				}
			} catch (IOException e) {
				throw new IllegalArgumentException();
			}
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
			try{
				File file=File.createTempFile("tmp", ".io");
				IOUtils.saveAsFile(file,(byte[])obj);
				return file;	
			}catch(IOException e){
				throw new SQLException("Error at saving Blob to file",e);	
			}
		}
		Blob blob=(Blob) obj;
		InputStream in = blob.getBinaryStream();
		try {
			return IOUtils.saveAsTempFile(in);
		} catch (IOException e) {
			throw new SQLException("Error at saving Blob to file",e);
		}
	}
}
