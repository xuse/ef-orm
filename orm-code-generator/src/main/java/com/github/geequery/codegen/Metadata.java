package com.github.geequery.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.geequery.codegen.ast.JavaAnnotation;

import jef.database.meta.object.Column;
import jef.database.meta.object.ForeignKeyItem;
import jef.database.meta.object.Index;
import jef.database.meta.object.PrimaryKey;
import jef.tools.ArrayUtils;

public class Metadata {
	private Optional<PrimaryKey> primaryKey;
	private List<Column> columns;
	private List<ForeignKeyItem> foreignKey;
	private Collection<Index> indexes;

	public Collection<Index> getIndexes() {
		return indexes;
	}

	private Map<String, String> custParams;

	public Map<String, String> getCustParams() {
		return custParams;
	}

	public void setCustParams(Map<String, String> custParams) {
		this.custParams = custParams;
	}

	public List<ForeignKeyItem> getForeignKey() {
		return foreignKey;
	}

	public void setForeignKey(List<ForeignKeyItem> foreignKey) {
		this.foreignKey = foreignKey;
	}

	public Optional<PrimaryKey> getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(Optional<PrimaryKey> primaryKey) {
		this.primaryKey = primaryKey;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

	public Column findColumn(String columnName) {
		for (Column c : columns) {
			if (columnName.equalsIgnoreCase(c.getColumnName())) {
				return c;
			}
		}
		return null;
	}

	public static class ColumnEx extends Column {
		private String fieldName;
		private Boolean generated; // 强制描述是否为自动生成键值
		private Class<?> javaType;
		private String initValue;
		private Collection<JavaAnnotation> annotation;

		public Collection<JavaAnnotation> getAnnotation() {
			return annotation;
		}

		public void setAnnotation(Collection<JavaAnnotation> annotation) {
			this.annotation = annotation;
		}

		public String getInitValue() {
			return initValue;
		}

		public void setInitValue(String initValue) {
			this.initValue = initValue;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public Class<?> getJavaType() {
			return javaType;
		}

		public void setJavaType(Class<?> javaType) {
			this.javaType = javaType;
		}

		public Boolean getGenerated() {
			return generated;
		}

		public void setGenerated(Boolean generated) {
			this.generated = generated;
		}
	}

	public static class ForeignKeyEx extends ForeignKeyItem {
		private String fieldName;

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}
	}

	public void setIndexes(Collection<Index> indexes) {
		this.indexes = indexes;
	}

	public Collection<Index> getIndexesWihoutPKIndex() {
		if (indexes == null) {
			return Collections.emptyList();
		}
		List<Index> list = new ArrayList<>(indexes);
		if (primaryKey.isPresent()) {
			for (Index i : list) {
				if (i.getColumns().size() == this.primaryKey.get().getColumns().length) {
					if (Arrays.equals(i.getColumnNames(), primaryKey.get().getColumns())) {
						list.remove(i);
						break;
					}
				}
			}
		}
		return list;
	}

	public boolean isPk(String columnName) {
		return primaryKey.isPresent() && ArrayUtils.contains(primaryKey.get().getColumns(), columnName);
	}
}
