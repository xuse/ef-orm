package jef.database.meta.def;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Index;

import org.apache.commons.lang.StringUtils;

/**
 * Index描述
 * 
 * @author publicxtgxrj10
 * 
 */
public class IndexDef {
	/**
	 * 索引名，为空时自动创建名称（但是可能会超出数据库长度，因此也可以手工指定）
	 * 
	 * @return
	 */
	private String name;
	/**
	 * 索引的各个字段名称（是java字段名，不是列名）
	 * 
	 * @return
	 */
	private String[] columns;
	/**
	 * 其他索引类型的定义关键字，如bitmap
	 * 
	 * @return
	 */
	private String definition;
	/**
	 * unique索引
	 * 
	 * @return true if the index is a unique index.
	 */
	private boolean unique;
	/**
	 * 是否聚簇索引
	 * 
	 * @return true if the index is a clustered index.
	 */
	private boolean clustered;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getColumns() {
		return columns;
	}

	public void setColumns(String[] columns) {
		this.columns = columns;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		String[] defs = StringUtils.split(definition);
		List<String> result = new ArrayList<String>(defs.length);
		for (String s : defs) {
			if ("clustered".equalsIgnoreCase(s)) {
				this.clustered = true;
				continue;
			} else if ("unique".equalsIgnoreCase(s)) {
				this.unique = true;
				continue;
			}
			result.add(s);
		}
		this.definition = StringUtils.join(result, ' ');
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public IndexDef(String name, String[] columns) {
		this.name = name;
		this.columns = columns;
	}

	public static IndexDef create(Index index) {
		IndexDef def = new IndexDef(index.name(), StringUtils.split(index.columnList(), ","));
		def.setUnique(index.unique());
		return def;
	}

	public boolean isClustered() {
		return clustered;
	}
}
