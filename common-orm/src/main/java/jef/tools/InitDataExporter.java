package jef.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

import jef.database.DbClient;
import jef.database.QB;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.Query;

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

	public void export(@SuppressWarnings("rawtypes") Class clz) throws SQLException {
		export(clz, false);
	}

	public void export(@SuppressWarnings("rawtypes") Class clz, boolean cascade) throws SQLException {
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
		BufferedWriter writer = IOUtils.getWriter(file, "UTF-8");
		JSON.writeJSONStringTo(o, writer, SerializerFeature.PrettyFormat);
		IOUtils.closeQuietly(writer);
		System.out.println(file.getAbsolutePath() + " was updated.");
	}

}
