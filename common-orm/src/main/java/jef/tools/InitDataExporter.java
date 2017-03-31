package jef.tools;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import jef.database.DbClient;
import jef.database.QB;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.Query;
import jef.tools.csvreader.Codecs;
import jef.tools.csvreader.CsvWriter;
import jef.tools.reflect.Property;

public class InitDataExporter {

	private DbClient session;
	private boolean deleteEmpty;
	private File rootPath;
	
	
	public InitDataExporter(DbClient session) {
		this.session = session;
		this.rootPath = new File(System.getProperty("user.dir"));
	}

	public InitDataExporter(DbClient session, File sourcePath) {
		this.session = session;
		this.rootPath = sourcePath;
	}

	public void export(@SuppressWarnings("rawtypes") Class clz){
		try {
			export0(clz);
		}catch(SQLException e) {
			throw new RuntimeException(e);
		}catch(IOException e){
			throw new RuntimeException(e);
		}
	}

	public void export0(@SuppressWarnings("rawtypes") Class clz) throws SQLException, IOException {
		ITableMetadata meta = MetaHolder.getMeta(clz);
		if (meta == null)
			return;

		File file = new File(rootPath, meta.getThisType().getName()+ ".csv");
		@SuppressWarnings("unchecked")
		Query<?> query = QB.create(clz);
		query.setCascade(false);
		List<?> o = session.select(query);
		if (o.isEmpty()) {
			if (deleteEmpty && file.exists()) {
				file.delete();
			}
			return;
		}
		
		CsvWriter cw=new CsvWriter(file,',',"UTF-8");
		Collection<ColumnMapping> columns=meta.getColumns();
		for(ColumnMapping column:columns){
			cw.write(column.fieldName());
		}
		cw.endRecord();
		
		for(Object obj: o) {
			for(ColumnMapping column:columns){
				Property accessor=column.getFieldAccessor();
				String data=Codecs.toString(accessor.get(obj), accessor.getGenericType());
				cw.write(data);
			}
			cw.endRecord();
		}
		cw.close();
		System.out.println(file.getAbsolutePath() + " was updated.");
	}

}
