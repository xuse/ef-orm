package jef.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jef.database.DbClient;
import jef.database.QB;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.Query;
import jef.tools.reflect.BeanUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class InitDataExporter {

	private DbClient session;
	private boolean deleteEmpty;
	private File rootPath;

	public InitDataExporter(DbClient session, File sourcePath) {
		this.session = session;
		this.rootPath = sourcePath;
	}

	public void export(@SuppressWarnings("rawtypes") Class clz){
		try {
			export(clz, false);
		}catch(SQLException e) {
			throw new RuntimeException(e);
		}catch(IOException e){
			throw new RuntimeException(e);
		}
	}

	public void export(@SuppressWarnings("rawtypes") Class clz, boolean cascade) throws SQLException, IOException {
		ITableMetadata meta = MetaHolder.getMeta(clz);
		if (meta == null)
			return;

		File file = new File(rootPath, meta.getThisType().getName().replace('.', '/') + ".init.json");
		@SuppressWarnings("unchecked")
		Query<?> query = QB.create(clz);
		query.setCascade(cascade);
		List<?> o = session.select(query);
		if (o.isEmpty()) {
			if (deleteEmpty && file.exists()) {
				file.delete();
			}
			return;
		}
		List<Map<String,Object>> list=new ArrayList<Map<String,Object>>(o.size());
		for(Object obj: o) {
			list.add(BeanUtils.describe(obj));
		}
		BufferedWriter writer = IOUtils.getWriter(file, "UTF-8");
		if(cascade){
			writer.write("#cascade:true\r\n");
		}
		
		JSON.writeJSONStringTo(list, writer, SerializerFeature.PrettyFormat);
		IOUtils.closeQuietly(writer);
		System.out.println(file.getAbsolutePath() + " was updated.");
	}

}
