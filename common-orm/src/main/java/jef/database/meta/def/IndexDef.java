package jef.database.meta.def;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Index;

import jef.tools.StringUtils;

/**
 * Index描述。 注意：内部的colums都是Java中的字段名
 * 
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
	 * 索引的各个字段名称（是java字段名，不是列名）。 此外还可能有DESC等倒序关键字
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

	/**
	 * @return The name of the index.
	 */
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

	/**
	 * 设置索引定义，可以设置多个用空格分隔的关键字。 识别其中的关键字——
	 * <ul>
	 * <li><code>clustered</code></li>
	 * <li><code>unique</code></li>
	 * </ul>
	 * (目前仅支持上述关键字，其余描述会被丢弃)
	 * 
	 * @param definition
	 *            Other definitions of the index.
	 */
	public void setDefinition(String definition) {
		if (definition == null)
			return;
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

	public IndexDef(String... columns) {
		this.columns = columns;
	}

	/**
	 * @param name
	 *            索引名称
	 * @param columns
	 *            索引列
	 */
	public IndexDef(String name, String[] columns) {
		this.name = name;
		this.columns = columns;
	}

	/**
	 * 基于Annotation @{link javax.persistence.Index}转换为IndexDef对象
	 * 
	 * @param index
	 * @return
	 */
	public static IndexDef valueOf(Index index) {
		IndexDef def = new IndexDef(index.name(), StringUtils.split(index.columnList(), ","));
		def.setUnique(index.unique());
		return def;
	}

	public boolean isClustered() {
		return clustered;
	}
}
