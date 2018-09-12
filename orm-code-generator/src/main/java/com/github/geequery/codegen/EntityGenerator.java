/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.geequery.codegen;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.github.geequery.codegen.Metadata.ColumnEx;
import com.github.geequery.codegen.ast.IClass;
import com.github.geequery.codegen.ast.IClassUtil;
import com.github.geequery.codegen.ast.JavaAnnotation;
import com.github.geequery.codegen.ast.JavaConstructor;
import com.github.geequery.codegen.ast.JavaContainer;
import com.github.geequery.codegen.ast.JavaField;
import com.github.geequery.codegen.ast.JavaUnit;
import com.github.geequery.orm.annotation.Comment;
import com.github.geequery.orm.annotation.InitializeData;

import jef.codegen.support.OverWrittenMode;
import jef.codegen.support.RegexpNameFilter;
import jef.common.PairSS;
import jef.common.log.LogUtil;
import jef.database.DataObject;
import jef.database.DbUtils;
import jef.database.annotation.Indexed;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;
import jef.database.dialect.AbstractDialect;
import jef.database.dialect.AnnotationDesc;
import jef.database.dialect.ColumnType;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.ColumnType.GUID;
import jef.database.dialect.ColumnType.Varchar;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.object.Column;
import jef.database.meta.object.Index;
import jef.database.meta.object.Index.IndexItem;
import jef.database.meta.object.PrimaryKey;
import jef.database.meta.object.TableInfo;
import jef.database.routing.function.KeyFunction;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.Exceptions;
import jef.tools.StringUtils;
import jef.tools.algorithm.LocalMappedSorter;
import jef.tools.io.Charsets;
import jef.tools.reflect.BeanUtils;

/**
 * 从数据库表生成JEF的Entity类 指定数据库表，自动生成表对应的DataObject.
 * 
 * @author Administrator
 */
public class EntityGenerator {
	/**
	 * 生成实体的包名
	 */
	private String basePackage = "jef.generated.dataobject";
	/**
	 * 方言
	 */
	private DatabaseDialect profile;
	/**
	 * 实体基类
	 */
	private String entityBaseClass = DataObject.class.getName();
	/**
	 * 元数据
	 */
	private MetaProvider provider;
	/**
	 * 源代码文件夹
	 */
	private File srcFolder = new File("src1");
	/**
	 * 包含表
	 */
	private String includePattern;
	/**
	 * 不包含表
	 */
	private String[] excludePatter;
	/**
	 * 最大生成
	 */
	private int maxTables = 1000;
	/**
	 * 进度回调
	 */
	private EntityProcessorCallback callback;

	/**
	 * 仓库包名
	 */
	private String reposPackageName;
	/**
	 * 仓库类后缀
	 */
	private String reposSuffix = "Repository";

	private Collection<String> tablesInitializeData = Collections.emptyList();

	private boolean initializeDataAll;

	/**
	 * 生成单个表对应的实体。
	 * 
	 * @param tablename
	 * @param options
	 * @return
	 * 
	 * @throws SQLException
	 */
	public void generateOne(String tablename, Option... options) throws SQLException {
		generateOne(tablename, null, null, options);
	}

	public void generateOne(String tablename, String entityName, String tableComment, Option... options) throws SQLException {
		Set<Option> optionSet = new HashSet<>(Arrays.asList(options));
		if (entityName == null)
			entityName = DbUtils.underlineToUpper(tablename, true);

		if (optionSet.contains(Option.generateEntity)) {
			if (tableComment == null) {
				TableInfo info = provider.getTableInfo(tablename);
				tableComment = info != null ? info.getRemarks() : null;
			}
			JavaUnit unit = generateEntity(tablename, entityName, tableComment, optionSet);
			File file = this.saveJavaSource(unit);
			if (file != null) {
				LogUtil.show(file.getAbsolutePath() + " generated.");
			} else {
				LogUtil.show(unit.getClassName()+" Class file was modified, will not overwrite it.");
			}
		}
		if (ArrayUtils.fastContains(options, Option.generateRepos)) {
			if (StringUtils.isEmpty(reposPackageName)) {
				reposPackageName = StringUtils.substringBeforeLast(basePackage, ".") + ".repos";
			}
			Metadata meta = provider.getTableMetadata(tablename);
			if (meta.getPrimaryKey().isPresent()) {
				File file = this.saveJavaSource(generateRepository(entityName, meta, optionSet));
				LogUtil.show(file.getAbsolutePath() + " generated.");
			}

		}
	}

	private JavaUnit generateRepository(String entityName, Metadata meta, Set<Option> optionSet) {
		Assert.notNull(entityName);
		JavaUnit java = new JavaUnit(reposPackageName, entityName + reposSuffix);
		java.setInterface(true);
		IClass clz = IClassUtil.parse("com.github.geequery.springdata.repository.GqRepository");
		IClass entity = IClassUtil.parse(basePackage + "." + entityName);
		IClass PKType = getPkType(meta, entityName);
		IClass genericClz = IClassUtil.generic(clz, entity, PKType);
		java.setExtends(genericClz);
		java.addImport("org.springframework.stereotype.Repository");
		java.addAnnotation("@Repository");
		java.addComments("This class was generated by GeeQuery.");
		java.addComments("This is a Repository of spring-data that supports Querydsl Predicates, Query by example, paging and sorting.");
		java.addComments("Use @FindBy @Query, or a custom method with name 'findBy<i>Fieldname</i>... to fetch data.");
		java.addComments("@see com.github.geequery.springdata.annotation.FindBy");
		java.addComments("@see org.springframework.data.querydsl.QuerydslPredicateExecutor");
		return java;
	}

	private IClass getPkType(Metadata meta, String tablename) {
		Optional<PrimaryKey> pk = meta.getPrimaryKey();
		if (!pk.isPresent()) {
			return IClassUtil.of(Void.class);
		}
		if (pk.get().columnSize() == 1) {
			Column column = meta.findColumn(pk.get().getColumns()[0]);
			ColumnType columnType = getColumnType(column, tablename, true, meta.getPrimaryKey());
			return IClassUtil.of(BeanUtils.toWrapperClass(getTypeClz(column, columnType)));
		}
		return IClassUtil.of(judeg(meta, pk.get(), tablename));
	}

	private Type judeg(Metadata meta, PrimaryKey pk, String tablename) {
		Class<?>[] types = new Class[pk.columnSize()];
		int i = 0;
		Class<?> container = null;
		for (String name : pk.getColumns()) {
			Column column = meta.findColumn(name);
			ColumnType columnType = getColumnType(column, tablename, true, meta.getPrimaryKey());
			Class<?> clz = getTypeClz(column, columnType);
			types[i++] = BeanUtils.toWrapperClass(clz);
			if (container == null) {
				container = clz;
			} else {
				if (container != clz) {
					container = Object.class;
				}
			}
		}
		if (container == Object.class) {
			container = expand(types[0], types);
		}
		return Array.newInstance(container, 0).getClass();
	}

	private Class<?> expand(Class<?> container, Class<?>[] types) {
		for (int i = 1; i < types.length; i++) {
			if (!types[i].isAssignableFrom(container)) {
				if (Number.class.isAssignableFrom(container) && Number.class.isAssignableFrom(types[i])) {
					container = Number.class;
				}
				if (Date.class.isAssignableFrom(container) && Date.class.isAssignableFrom(types[i])) {
					container = Date.class;
				}
				if (Serializable.class.isAssignableFrom(container) && Serializable.class.isAssignableFrom(types[i])) {
					container = Serializable.class;
				}
				container = Object.class;
			}
		}
		return container;
	}

	public static void main(String[] args) {
		System.out.println(int.class.isAssignableFrom(Object.class));
	}

	public String getEntityBaseClass() {
		return entityBaseClass;
	}

	public void setEntityBaseClass(String entityBaseClass) {
		this.entityBaseClass = entityBaseClass;
	}

	private JavaUnit generateEntity(String tablename, String entityName, String tableComment, Set<Option> options) throws SQLException {
		Assert.notNull(tablename);
		Assert.notNull(entityName);
		if (profile == null) {
			LogUtil.warn("Db dialect not set,default dialect set to Oracle.");
			profile = AbstractDialect.getDialect("oracle");
		}
		tablename = profile.getObjectNameToUse(tablename);
		final JavaUnit java = new JavaUnit(basePackage, entityName);

		if (!options.contains(Option.ignoreClassComment)) {
			java.addComments("This class was generated by GeeQuery according to the table in database.");
			java.addComments("You need to modify the type of primary key field, to the strategy your own.");

		}
		java.setAnnotations("@Entity");
		JavaAnnotation tableAnnotation = new JavaAnnotation(Table.class);
		tableAnnotation.put("name", tablename);
		String schema = provider.getSchema();
		if (StringUtils.isNotEmpty(schema)) {
			tableAnnotation.put("schema", schema);
		}

		if (!options.contains(Option.ignoreCommentAnnotation)) {
			JavaAnnotation comment = new JavaAnnotation(Comment.class);
			if (StringUtils.isNotEmpty(tableComment)) {
				comment.put("value", "Table:" + tableComment);
			} else {
				comment.put("value", "");
			}
			java.addAnnotation(comment.toCode(java));
		}
		Metadata meta = provider.getTableMetadata(tablename);
		if (meta.getCustParams() != null && meta.getCustParams().get("custAnnot") != null) {
			String _tmpCustAnnot = meta.getCustParams().get("custAnnot");
			java.addAnnotation(_tmpCustAnnot);

			if (_tmpCustAnnot.indexOf("PartitionTable") >= 0) {
				java.addImport(PartitionTable.class);
				java.addImport(PartitionKey.class);
			}
			if (_tmpCustAnnot.indexOf("KeyFunction") >= 0) {
				java.addImport(KeyFunction.class);
			}
		}
		if (initializeDataAll || tablesInitializeData.contains(tablename)) {
			java.addAnnotation(new JavaAnnotation(InitializeData.class).toCode(java));
		}

		java.setExtends(entityBaseClass);

		if (callback != null) {
			callback.init(meta, tablename, provider.getSchema(), tableComment, java);
		}
		// 生成元模型
		final JavaContainer enumField = new JavaContainer("   public enum Field implements jef.database.Field{", "}");
		enumField.setWrap(false);
		java.addRawBlock(enumField);

		// 生成用于元模型的字段配置
		Collection<Index> indexes = meta.getIndexesWihoutPKIndex();
		final List<JavaField> pkFields = new ArrayList<JavaField>();
		final List<PairSS> allFields = new ArrayList<PairSS>();
		for (Column c : meta.getColumns()) {
			boolean isPk = meta.isPk(c.getColumnName());
			ColumnType columnType = getColumnType(c, tablename, isPk, meta.getPrimaryKey());
			generateColumn(isPk, c, columnType, java, allFields, pkFields, indexes, options);
		}
		// 处理枚举元模型
		if (meta.getPrimaryKey().isPresent() && meta.getPrimaryKey().get().columnSize() > 1) {
			String[] pkColumns = meta.getPrimaryKey().get().getColumns();
			LocalMappedSorter<PairSS, Integer> sorter = new LocalMappedSorter<PairSS, Integer>(allFields, e -> ArrayUtils.indexOf(pkColumns, e.first));
			sorter.filterAfter(e -> e >= 0);
			sorter.sort((a, b) -> a.compareTo(b));
		}

		Iterator<PairSS> iter = allFields.iterator();
		if (iter.hasNext()) {
			enumField.addContent(iter.next().second);
		}
		for (; iter.hasNext();) {
			enumField.addContent(", " + iter.next().second);
		}
		// 处理Index
		List<JavaAnnotation> indexAnnos = new ArrayList<>();
		for (Index i : indexes) {
			JavaAnnotation anno = new JavaAnnotation(javax.persistence.Index.class);
			anno.put("columnList", StringUtils.join(i.getColumnNamesWithOrder(), ','));
			anno.put("unique", i.isUnique());
			indexAnnos.add(anno);
		}
		if (!indexAnnos.isEmpty()) {
			tableAnnotation.put("indexes", indexAnnos);
		}
		// 将@Table加上
		java.addAnnotation(tableAnnotation.toCode(java));

		java.addImport(javax.persistence.Column.class);
		java.addImport(javax.persistence.Entity.class);

		// 生成默认的构造器和主键构造器
		JavaConstructor c1 = new JavaConstructor();
		java.addMethod(c1.getKey(), c1);
		if (!pkFields.isEmpty()) {
			JavaConstructor c2 = new JavaConstructor();
			for (JavaField field : pkFields) {
				c2.addparam(field.getType(), field.getName(), 0);
				c2.addContent(StringUtils.concat("this.", field.getName(), " = ", field.getName(), ";"));
			}
			java.addMethod(c2.getKey(), c2);
		}
		if (callback != null) {
			callback.finish(java);
		}
		return java;
	}

	private void generateColumn(boolean isPk, final Column c, final ColumnType columnType, final JavaUnit java, final List<PairSS> allFields, final List<JavaField> pkFields, final Collection<Index> index, final Set<Option> options) {
		String columnName = c.getColumnName();
		List<JavaAnnotation> annonations = getFieldAnnotation(java, columnName, columnType, isPk, c);
		String initValue = null;
		String fieldName;
		if (c instanceof ColumnEx) {
			fieldName = ((ColumnEx) c).getFieldName();
			initValue = ((ColumnEx) c).getInitValue();
		} else {
			fieldName = columnToField(columnName);
		}
		if (isSingleColumnIndex(columnName, index)) {
			annonations.add(new JavaAnnotation(Indexed.class));
		}

		Class<?> clz = getTypeClz(c, columnType);
		JavaField field = new JavaField(clz, fieldName);
		field.setModifiers(Modifier.PRIVATE);
		for (JavaAnnotation anno1 : annonations) {
			field.addAnnotation(anno1.toCode(java));
		}
		if (!options.contains(Option.ignoreCommentAnnotation) && StringUtils.isNotEmpty(c.getRemarks())) {
			if (!c.getRemarks().equals(c.getColumnName()) && !c.getRemarks().equals(fieldName)) {
				JavaAnnotation fieldComment = new JavaAnnotation(Comment.class);
				fieldComment.put("value", c.getRemarks());
				field.addAnnotation(fieldComment.toCode(java));
			}
		}
		field.addComments(StringUtils.trimToNull(c.getRemarks()));
		if (initValue != null)
			field.setInitValue(initValue);

		java.addFieldWithGetterAndSetter(field);
		allFields.add(new PairSS(columnName, fieldName));
		if (isPk)
			pkFields.add(field);

		if (callback != null) {
			callback.addField(java, field, c, columnType);
		}

	}

	private Class<?> getTypeClz(Column c, ColumnType columnType) {
		return (c instanceof ColumnEx) ? ((ColumnEx) c).getJavaType() : columnType.getDefaultJavaType();
	}

	private ColumnType getColumnType(Column c, final String tablename, boolean isPk, final Optional<PrimaryKey> pkColumns) {
		ColumnType columnType;
		try {
			columnType = c.toColumnType(profile);
		} catch (Exception e) {
			throw new RuntimeException("The column [" + tablename + ":" + c.getColumnName() + "] 's type is error:" + e.getMessage());
		}

		if (!isPk) {// 不是主键时，错误的配置要还原
			if (columnType.getClass() == ColumnType.AutoIncrement.class) {
				columnType = ((AutoIncrement) columnType).toNormalType();
			} else if (columnType.getClass() == ColumnType.GUID.class) {
				columnType = ((GUID) columnType).toNormalType();
			}
		}
		if (pkColumns.isPresent() && pkColumns.get().columnSize() == 1 && isPk) { // 如果是单一主键，则修改为特定的JEF字段类型
			if (columnType.getClass() == ColumnType.Int.class) {
				if (c.getColumnSize() == 0)
					c.setColumnSize(8);
				columnType = new ColumnType.AutoIncrement(c.getColumnSize());
			} else if (columnType.getClass() == ColumnType.Varchar.class) {
				if (((Varchar) columnType).getLength() >= 32) {
					columnType = new ColumnType.GUID();
				}
			}
			columnType.setNullable(false); // 主键列不允许为空
		}
		return columnType;
	}

	private boolean isSingleColumnIndex(String columnName, Collection<Index> indexes) {
		for (Index index : indexes) {
			if (index.columnSize() == 1) {
				IndexItem item = index.getColumns().get(0);
				if (item.column.equals(columnName)) {
					indexes.remove(index);
					return true;
				}
			}
		}
		return false;
	}

	private String columnToField(String columnName) {
		if (callback == null) {
			return DbUtils.underlineToUpper(columnName.toLowerCase(), false);
		} else {
			return callback.columnToField(columnName);
		}
	}

	public File saveJavaSource(JavaUnit java) {
		Assert.notNull(srcFolder);
		try {
			File f = java.saveToSrcFolder(srcFolder, Charsets.UTF8, OverWrittenMode.AUTO);
			return f;
		} catch (IOException e) {
			throw Exceptions.asIllegalArgument(e);
		}
	}

	// 生成默认的annonation
	protected List<JavaAnnotation> getFieldAnnotation(JavaUnit java, String columnName, ColumnType column, boolean isPk, Column columnDef) {
		List<JavaAnnotation> result = new ArrayList<JavaAnnotation>();
		Boolean b = null;
		if (columnDef instanceof ColumnEx) {
			ColumnEx ex = (ColumnEx) columnDef;
			b = ex.getGenerated();
			if (ex.getAnnotation() != null) {
				result.addAll(ex.getAnnotation());
			}
		}
		boolean isGenerated = false;
		for (AnnotationDesc ad : column.toJpaAnnonation(columnName)) {
			if (ad.getAnnotationClz() == GeneratedValue.class) {
				if (Boolean.FALSE.equals(b)) {// 强行不做generate
					continue;
				}
				isGenerated = true;
			}
			JavaAnnotation ja = new JavaAnnotation(ad.getAnnotationClz());
			ja.getProperties().putAll(ad.getProprties());
			result.add(ja);
		}
		// 如果强行要求generated
		if (Boolean.TRUE.equals(b) && isGenerated == false) {
			JavaAnnotation generateValue = new JavaAnnotation(GeneratedValue.class);
			Class<?> type = column.getDefaultJavaType();
			if (type == String.class) {
				generateValue.put("strategy", GenerationType.IDENTITY);
			} else if (type == Long.class || type == Integer.class || type == Integer.TYPE || type == Long.TYPE) {
				generateValue.put("strategy", GenerationType.AUTO);
			}
		}
		if (isPk) {
			if (!containsAnnotation(result, Id.class)) {
				result.add(0, new JavaAnnotation(Id.class));
			}
		}
		return result;
	}

	private boolean containsAnnotation(List<JavaAnnotation> result, Class<? extends Annotation> key) {
		for (JavaAnnotation ja : result) {
			if (ja.getName().equals(key.getName())) {
				return true;
			}
		}
		return false;
	}

	public void generateSchema(Option... generateOptions) throws SQLException {
		Assert.notNull(provider);
		RegexpNameFilter filter = new RegexpNameFilter(includePattern, excludePatter);
		int n = 0;
		List<TableInfo> tables = provider.getTables();

		if (callback != null) {
			callback.setTotal(tables.size());
		}
		for (TableInfo table : tables) {
			String tableName = table.getName();
			if (filter.accept(tableName)) {
				try {
					generateOne(tableName, null, table.getRemarks(), generateOptions);
				} catch (SQLException e) {
					LogUtil.exception(e);
				}
				n++;
			}
			if (n >= maxTables)
				break;
		}
		LogUtil.info(n + " Class Mapping to Table are generated.");

	}

	public void setProfile(DatabaseDialect profile) {
		this.profile = profile;
	}

	public void setSrcFolder(File srcFolder) {
		this.srcFolder = srcFolder;
	}

	public void setProvider(MetaProvider provider) {
		this.provider = provider;
	}

	public void setIncludePattern(String includePattern) {
		this.includePattern = includePattern;
	}

	public void addExcludePatter(String pattern) {
		if (pattern == null) {
			this.excludePatter = new String[] { pattern };
		}
		this.excludePatter = (String[]) ArrayUtils.add(this.excludePatter, pattern);
	}

	public int getMaxTables() {
		return maxTables;
	}

	public void setMaxTables(int maxTables) {
		this.maxTables = maxTables;
	}

	public String getBasePackage() {
		return basePackage;
	}

	public void setBasePackage(String basePackage) {
		this.basePackage = basePackage;
	}

	public void setCallback(EntityProcessorCallback callback) {
		this.callback = callback;
	}

	public String getReposPackageName() {
		return reposPackageName;
	}

	public void setReposPackageName(String reposPackageName) {
		this.reposPackageName = reposPackageName;
	}

	public String getReposSuffix() {
		return reposSuffix;
	}

	public void setReposSuffix(String reposSuffix) {
		this.reposSuffix = reposSuffix;
	}

	public void setAddInitializeData(Collection<String> addInitializeData) {
		this.tablesInitializeData = addInitializeData;
	}

	public void setInitializeDataAll(boolean initializeDataAll) {
		this.initializeDataAll = initializeDataAll;
	}

}
