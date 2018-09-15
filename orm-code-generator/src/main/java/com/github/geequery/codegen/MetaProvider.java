package com.github.geequery.codegen;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.geequery.codegen.pdm.IMetaLoader;
import com.github.geequery.codegen.pdm.PDMetaLoader;
import com.github.geequery.codegen.pdm.model.MetaModel;
import com.github.geequery.codegen.pdm.model.MetaTable;

import jef.common.JefException;
import jef.database.DbClient;
import jef.database.DbMetaData;
import jef.database.meta.object.TableInfo;

public interface MetaProvider {
	/**
	 * 得到表的元数据
	 * 
	 * @param tablename
	 * @return
	 * @throws SQLException
	 */
	Metadata getTableMetadata(String tablename) throws SQLException;

	/**
	 * 得到所有表 key=表名 value=备注
	 * 
	 * @return
	 * @throws SQLException
	 */
	List<TableInfo> getTables() throws SQLException;

	String getSchema();

	/**
	 * 得到数据库表
	 * 
	 * @param name
	 * @return
	 * @throws SQLException
	 */
	default TableInfo getTableInfo(String name) throws SQLException {
		for (TableInfo info : getTables()) {
			if (info.getName().equalsIgnoreCase(name)) {
				return info;
			}
		}
		return null;
	}

	public static class DbClientProvider implements MetaProvider {
		private DbClient db;

		public DbClientProvider(DbClient db) {
			this.db = db;
		}

		public Metadata getTableMetadata(String tablename) throws SQLException {
			Metadata data = new Metadata();
			DbMetaData meta = db.getMetaData(null);
			data.setColumns(meta.getColumns(tablename, true));
			data.setIndexes(meta.getIndexes(tablename));
			data.setPrimaryKey(meta.getPrimaryKey(tablename));
			data.setForeignKey(meta.getForeignKey(tablename));
			return data;
		}

		public List<TableInfo> getTables() throws SQLException {
			return db.getMetaData(null).getTables(true);
		}

		public String getSchema() {
			return null;
		}
	}

	public static class PDMProvider implements MetaProvider {
		String schema;
		MetaModel model;

		public void setSchema(String schema) {
			this.schema = schema;
		}

		public PDMProvider(String schema, MetaModel model) {
			this.schema = schema;
			this.model = model;
		}

		public PDMProvider(File pdmFile) {
			IMetaLoader metaLoader = new PDMetaLoader();
			MetaModel model;
			try {
				model = metaLoader.getMetaModel(pdmFile);
			} catch (JefException e) {
				throw new RuntimeException(e.getMessage());
			}
			this.model = model;
		}

		public Metadata getTableMetadata(String tablename) throws SQLException {
			Metadata data = new Metadata();
			MetaTable table = model.getTable(tablename);
			data.setColumns(table.getJefColumns());
			data.setPrimaryKey(Optional.ofNullable(table.getJefPK()));
			data.setForeignKey(table.getJefFK());
			return data;
		}

		public List<TableInfo> getTables() throws SQLException {
			List<TableInfo> result = new ArrayList<TableInfo>();
			for (MetaTable t : model.getTables()) {
				TableInfo info = new TableInfo();
				info.setName(t.getCode());
				info.setRemarks(t.getComment());
				result.add(info);
			}
			return result;
		}

		public String getSchema() {
			return schema;
		}

	}
}
