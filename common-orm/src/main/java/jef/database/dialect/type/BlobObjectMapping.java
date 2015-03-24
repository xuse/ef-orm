package jef.database.dialect.type;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import jef.common.BigDataBuffer;
import jef.database.DbCfg;
import jef.database.ORMConfig;
import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.meta.Feature;
import jef.tools.IOUtils;
import jef.tools.JefConfiguration;


public class BlobObjectMapping extends AColumnMapping {
	static int blobType;

	static {
		String type = JefConfiguration.get(DbCfg.DB_BLOB_RETURN_TYPE, "stream");
		if ("stream".equalsIgnoreCase(type)) {
			blobType = 1;
		} else if ("string".equalsIgnoreCase(type)) {
			blobType = 2;
		} else if ("byte".equalsIgnoreCase(type)) {
			blobType = 3;
		} else if ("file".equalsIgnoreCase(type)) {
			blobType = 4;
		} else {
			throw new IllegalArgumentException("Invalid config of db.blob.return.type=" + type);
		}
	}

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect profile) throws SQLException {
		if (value == null) {
			st.setNull(index, profile.getImplementationSqlType(Types.BLOB));
		} else if (value instanceof byte[]) {
			byte[] data = (byte[]) value;
			st.setBytes(index, data);
		} else if (value instanceof File) {
			File file = (File) value;
			try {
				if(profile.has(Feature.NOT_SUPPORT_SET_BINARY)){
					st.setBytes(index, IOUtils.toByteArray(file));
				}else{
					st.setBinaryStream(index, IOUtils.getInputStream(file), file.length());	
				}
			} catch (IOException e) {
				throw new SQLException("Can not read file" + file, e);
			}
		} else {
			throw new SQLException("Can not set to Blob for" + value.getClass().getName());
		}
		return value;

	}

	public int getSqlType() {
		return java.sql.Types.BLOB;
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object value = rs.getObject(n);
		if (value == null)
			return null;
		try{
			if(value instanceof byte[]){
				return cast((byte[])value);
			}else{
				return cast((Blob)value);
			}	
		}catch(IOException e){
			throw new SQLException(e);
		}
	}

	private Object cast(Blob value) throws SQLException, IOException {
		switch (blobType) {
		case 1:
			BigDataBuffer bf = new BigDataBuffer();
			byte[] buffer = new byte[4096];
			int res;
			InputStream in=value.getBinaryStream();
			while ((res = in.read(buffer)) != -1) {
				bf.write(buffer, 0, res);
			}
			return bf.getAsStream();
		case 2:
			return IOUtils.asString(value.getBinaryStream(),ORMConfig.getInstance().getDbEncoding(),true);
		case 3:
			return IOUtils.toByteArray(value.getBinaryStream());
		case 4:
			return IOUtils.saveAsTempFile(value.getBinaryStream());
		}
		return IOUtils.toByteArray(value.getBinaryStream());
	}

	private Object cast(byte[] value) throws IOException {
		switch (blobType) {
		case 1:
			return new ByteArrayInputStream(value);
		case 2:
			return new String(value,ORMConfig.getInstance().getDbEncodingCharset());
		case 3:
			return value;
		case 4:
			return IOUtils.saveAsTempFile(new ByteArrayInputStream(value));
		}
		return value;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Object.class;
	}

}