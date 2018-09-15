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
package jef.database.meta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Table;

import com.alibaba.fastjson.util.IOUtils;
import com.github.geequery.orm.annotation.Comment;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.database.DbCfg;
import jef.database.DbUtils;
import jef.database.DebugUtil;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.OperateTarget;
import jef.database.PojoWrapper;
import jef.database.annotation.BindDataSource;
import jef.database.annotation.EasyEntity;
import jef.database.annotation.PartitionFunction;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.AnnotationProvider.FieldAnnotationProvider;
import jef.database.meta.def.IndexDef;
import jef.database.meta.def.UniqueConstraintDef;
import jef.database.routing.function.AbstractDateFunction;
import jef.database.routing.function.HashMod1024MappingFunction;
import jef.database.routing.function.MapFunction;
import jef.database.routing.function.ModulusFunction;
import jef.database.routing.function.RawFunc;
import jef.tools.ArrayUtils;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanAccessorMapImpl;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.FieldEx;

@SuppressWarnings("rawtypes")
public final class TableMetadata extends AbstractMetadata {

	/**
	 * 记录当前Schema所对应的DO类。
	 */
	private Class<?> thisType;
	private BeanAccessor pojoAccessor;

	private Class<? extends IQueryableEntity> containerType;
	BeanAccessor containerAccessor;

	// ///////////////分表配置信息//////////////////////
	private PartitionTable partition;// 分表策略

	// 记录在每个字段上的函数，用来进行分表估算的时的采样
	private Multimap<String, PartitionFunction> partitionFuncs;

	private Entry<PartitionKey, PartitionFunction>[] effectPartitionKeys;

	private List<ColumnMapping> pkFields = new ArrayList<>(2);// 记录主键列

	private final Map<Field, String> fieldToColumn = new IdentityHashMap<Field, String>();// 提供Field到列名的转换
	private final Map<String, String> lowerColumnToFieldName = new HashMap<String, String>();// 提供Column名称到Field的转换，不光包括元模型字段，也包括了非元模型字段但标注了Column的字段(key全部存小写)

	/**
	 * 标准实体的构造
	 * 
	 * @param clz
	 * @param annos
	 */
	TableMetadata(Class<? extends IQueryableEntity> clz, AnnotationProvider annos) {
		this.containerType = clz;
		this.containerAccessor = FastBeanWrapperImpl.getAccessorFor(clz);
		this.thisType = clz;
		initByAnno(clz, annos);
	}

	/**
	 * POJO类实体的构造
	 * 
	 * @param varClz
	 * @param clz
	 * @param annos
	 */
	TableMetadata(Class<PojoWrapper> varClz, Class<?> clz, AnnotationProvider annos) {
		this.containerType = varClz;
		this.containerAccessor = new BeanAccessorMapImpl(clz);

		this.thisType = clz;
		this.pojoAccessor = FastBeanWrapperImpl.getAccessorFor(clz);
		initByAnno(clz, annos);
	}

	protected void initByAnno(Class<?> thisType, AnnotationProvider annos) {
		// schema初始化
		Table table = annos.getAnnotation(javax.persistence.Table.class);
		if (table != null) {
			if (table.schema().length() > 0) {
				schema = MetaHolder.getMappingSchema(table.schema());// 重定向
			}
			if (table.name().length() > 0) {
				tableName = table.name();
			}
			for (javax.persistence.Index index : table.indexes()) {
				this.indexes.add(IndexDef.valueOf(index));
			}
			for (javax.persistence.UniqueConstraint unique : table.uniqueConstraints()) {
				this.uniques.add(new UniqueConstraintDef(unique));
			}
		}
		if (tableName == null) {
			// 表名未指定，缺省生成
			boolean needTranslate = JefConfiguration.getBoolean(DbCfg.TABLE_NAME_TRANSLATE, false);
			if (needTranslate) {
				tableName = DbUtils.upperToUnderline(thisType.getSimpleName());
			} else {
				tableName = thisType.getSimpleName();
			}
		}
		BindDataSource bindDs = annos.getAnnotation(BindDataSource.class);
		if (bindDs != null) {
			this.bindDsName = MetaHolder.getMappingSite(StringUtils.trimToNull(bindDs.value()));
		}

		Cacheable cache = annos.getAnnotation(Cacheable.class);
		this.cacheable = cache != null && cache.value();

		EasyEntity entity = annos.getAnnotation(EasyEntity.class);
		if (entity != null) {
			this.useOuterJoin = entity.useOuterJoin();
		}
	}

	/**
	 * 得到分表配置
	 */
	public PartitionTable getPartition() {
		return partition;
	}

	/**
	 * 设置和解析数据分片（分库分表）配置
	 * 
	 * @param t
	 */
	synchronized void setPartition(PartitionTable t) {
		//观察发现初始化所有字段后会调用setPartition方法，因此利用这里机制将pkField锁定。
		if(this.pkFields instanceof ArrayList) {
			this.pkFields=Collections.unmodifiableList(this.pkFields);
		}
		if (t == null)
			return;
		effectPartitionKeys = withFunction(t.key());
		if (effectPartitionKeys.length == 0) {
			effectPartitionKeys = null;
			return;
		}
		this.partition = t;
		// 开始计算在同一个字段上的最小的分表参数单元
		Multimap<String, PartitionFunction> fieldKeyFn = ArrayListMultimap.create();
		for (Entry<PartitionKey, PartitionFunction> entry : getEffectPartitionKeys()) {
			PartitionKey key = entry.getKey();
			String field = key.field();
			if (entry.getValue() instanceof AbstractDateFunction) {
				Collection<PartitionFunction> olds = fieldKeyFn.get(field);
				if (olds != null) {
					for (PartitionFunction<?> old : olds) {
						if (old instanceof AbstractDateFunction) {
							int oldLevel = ((AbstractDateFunction) old).getTimeLevel();
							int level = ((AbstractDateFunction) entry.getValue()).getTimeLevel();// 取最小的时间单位
							if (level < oldLevel) {
								fieldKeyFn.remove(field, old);
								break;// 可以合并
							} else {
								continue;// 无需合并
							}
						}
					}
				}
			}
			fieldKeyFn.put(field, entry.getValue());
		}
		partitionFuncs = fieldKeyFn;
	}

	public Class<?> getThisType() {
		return thisType;
	}

	public Class<? extends IQueryableEntity> getContainerType() {
		return containerType;
	}

	public List<IndexDef> getIndexDefinition() {
		return indexes;
	}

	public List<ColumnMapping> getPKFields() {
		return pkFields;
	}

	/**
	 * 将一个Java Field加入到列定义中
	 * 
	 * @param field
	 * @param column
	 */
	public void putJavaField(Field field, ColumnMapping type, String columnName, boolean isPk) {
		fields.put(field.name(), field);
		lowerFields.put(field.name().toLowerCase(), field);

		fieldToColumn.put(field, columnName);
		String lastFieldName = lowerColumnToFieldName.put(columnName.toLowerCase(), field.name());
		if (lastFieldName != null && !field.name().equals(lastFieldName)) {
			throw new IllegalArgumentException(String.format("The field [%s] and [%s] in [%s] have a duplicate column name [%s].", lastFieldName, field.name(), containerType.getName(), columnName));
		}
		if (isPk) {
			type.setPk(true);
			this.pkFields.add(type);
			Collections.sort(pkFields, PK_COMPARE);
		}
		schemaMap.put(field, type);
		// 记录自增字段和自动更新字段
		super.updateAutoIncrementAndUpdate(type);
		if (type.isLob()) {
			lobNames = ArrayUtils.addElement(lobNames, field, jef.database.Field.class);
		}
	}

	private static final Comparator<ColumnMapping> PK_COMPARE = new Comparator<ColumnMapping>() {
		public int compare(ColumnMapping o1, ColumnMapping o2) {
			int i1 = -1;
			int i2 = -1;
			if (o1.field() instanceof Enum) {
				i1 = ((Enum<?>) o1.field()).ordinal();
			}
			if (o1.field() instanceof Enum) {
				i2 = ((Enum<?>) o2.field()).ordinal();
			}
			return Integer.compare(i1, i2);
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Entity: [").append(containerType.getName()).append("]\n");
		for (ColumnMapping m : schemaMap.values()) {
			String fname = m.fieldName();
			sb.append("  ").append(fname);
			StringUtils.repeat(sb, ' ', 10 - fname.length());
			sb.append('\t').append(m.get());
			sb.append("\n");
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * 获取当前生效的分区策略
	 * 注意生效的策略默认等同于Annotation上的策略，但是实际上如果配置了/partition-conf.properties后
	 * ，生效字段受改配置影响 {@link #partitPolicy}
	 * 
	 * @return
	 */
	public Entry<PartitionKey, PartitionFunction>[] getEffectPartitionKeys() {
		return effectPartitionKeys;
	}

	private Entry<PartitionKey, PartitionFunction>[] withFunction(PartitionKey[] key) {
		@SuppressWarnings("unchecked")
		Entry<PartitionKey, PartitionFunction>[] result = new Entry[key.length];
		for (int i = 0; i < key.length; i++) {
			result[i] = new Entry<PartitionKey, PartitionFunction>(key[i], createFunc(key[i]));
		}
		return result;
	}

	private static PartitionFunction<?> createFunc(PartitionKey value) {
		if (value.functionClass() != PartitionFunction.class) {
			try {
				String[] params = value.functionConstructorParams();
				if (params.length == 0) {
					return value.functionClass().newInstance();
				} else {
					Class[] clz = new Class[params.length];
					for (int i = 0; i < params.length; i++) {
						clz[i] = String.class;
					}
					Constructor cc = value.functionClass().getConstructor(clz);
					cc.setAccessible(true);
					return (PartitionFunction<?>) cc.newInstance((Object[]) params);
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
		switch (value.function()) {
		case DAY:
			return AbstractDateFunction.DAY;
		case HH24:
			return AbstractDateFunction.HH24;
		case MODULUS:
			if (value.functionConstructorParams().length == 0 || StringUtils.isEmpty(value.functionConstructorParams()[0])) {
				return ModulusFunction.getDefault();
			} else {
				return new ModulusFunction(value.functionConstructorParams()[0]);
			}
		case HASH_MOD1024_RANGE:
			if (value.functionConstructorParams().length == 0 || StringUtils.isEmpty(value.functionConstructorParams()[0])) {
				return new HashMod1024MappingFunction();
			} else {
				int num = 0;
				if (value.functionConstructorParams().length > 1) {
					num = StringUtils.toInt(value.functionConstructorParams()[1], 0);
				}
				return new HashMod1024MappingFunction(value.functionConstructorParams()[0], num);
			}
		case MONTH:
			return AbstractDateFunction.MONTH;
		case YEAR:
			return AbstractDateFunction.YEAR;
		case YEAR_LAST2:
			return AbstractDateFunction.YEAR_LAST2;
		case YEAR_MONTH:
			return AbstractDateFunction.YEAR_MONTH;
		case YEAR_MONTH_DAY:
			return AbstractDateFunction.YEAR_MONTH_DAY;
		case WEEKDAY:
			return AbstractDateFunction.WEEKDAY;
		case RAW:
			return new RawFunc(value.defaultWhenFieldIsNull(), value.length());
		case MAPPING:
			if (value.functionConstructorParams().length == 0) {
				throw new IllegalArgumentException("You must config the 'functionConstructorParams' while using funcuon Map");
			}
			int num = 0;
			if (value.functionConstructorParams().length > 1) {
				num = StringUtils.toInt(value.functionConstructorParams()[1], 0);
			}
			return new MapFunction(value.functionConstructorParams()[0], num);
		default:
			throw new IllegalArgumentException("Unknown KeyFunction:" + value.function());
		}
	}

	public Multimap<String, PartitionFunction> getMinUnitFuncForEachPartitionKey() {
		return partitionFuncs;
	}

	void setTableName(String tableName) {
		this.tableName = tableName;
	}

	void setSchema(String schema) {
		this.schema = schema;
	}

	/**
	 * 根据一个指定的实际数据库，核对metaData中的字段，如果发现某些可选的字段数据库里没有，就从元模型中删除来适应数据库
	 * 
	 * @param db
	 * @throws SQLException
	 */
	public synchronized void removeNotExistColumns(OperateTarget db) throws SQLException {
		Set<String> set = DebugUtil.getColumnsInLowercase(db, getTableName(true));
		List<Field> removeColumn = new ArrayList<Field>();
		for (Field field : fieldToColumn.keySet()) {
			String column = fieldToColumn.get(field).toLowerCase();
			if (!set.contains(column)) {
				removeColumn.add(field);
			}
		}
		for (Field field : removeColumn) {
			schemaMap.remove(field);
			// FIXME, others are not removed
			LogUtil.show("The field [" + field.name() + "] was remove since column not exist in db.");
		}
		if (removeColumn.size() > 0)
			metaFields = null;
	}

	public IQueryableEntity newInstance() {
		if (pojoAccessor != null) {
			return new PojoWrapper(pojoAccessor.newInstance(), pojoAccessor, this, false);
		} else {
			return (IQueryableEntity) containerAccessor.newInstance();
		}
	}

	public String getName() {
		return thisType.getName();
	}

	public String getSimpleName() {
		return thisType.getSimpleName();
	}

	public Field getFieldByLowerColumn(String columnLowercase) {
		return fields.get(lowerColumnToFieldName.get(columnLowercase));
	}

	public PojoWrapper transfer(Object p, boolean isQuery) {
		if (p == null)
			return null;
		if (p instanceof IQueryableEntity) {
			throw new IllegalArgumentException();
		}
		if (p.getClass() == this.thisType) {
			return new PojoWrapper(p, pojoAccessor, this, isQuery);
		} else {
			throw new IllegalArgumentException(p.getClass() + " != " + this.thisType);
		}
	}

	public EntityType getType() {
		return this.containerType == PojoWrapper.class ? EntityType.POJO : EntityType.NATIVE;
	}

	private List<Class> parents;

	public void addParent(Class<?> processingClz) {
		if (parents == null) {
			parents = new ArrayList<Class>(3);
		}
		parents.add(processingClz);

	}

	public boolean containsMeta(ITableMetadata type) {
		if (type == this)
			return true;
		if (parents == null)
			return false;
		for (Class clz : parents) {
			if (type.getThisType() == clz) {
				return true;
			}
		}
		return false;
	}

	@Override
	public BeanAccessor getContainerAccessor() {
		return containerAccessor;
	}

	TupleMetadata extendMeta;
	TupleMetadata extendContainer;

	@Override
	public TupleMetadata getExtendsTable() {
		return extendContainer;
	}

	@Override
	public Collection<ColumnMapping> getExtendedColumns() {
		return extendMeta == null ? Collections.<ColumnMapping> emptyList() : extendMeta.getColumnSchema();
	}

	@Override
	public ColumnMapping getExtendedColumnDef(String field) {
		return extendMeta.getColumnDef(extendMeta.getField(field));
	}

	@Override
	public Map<String, String> getColumnComments() {
		// 先根据源码解析来获取注解
		Map<String, String> result = getFromSource();
		// 再分析注解中的备注信息
		{
			Comment comment = thisType.getAnnotation(Comment.class);
			if (comment != null) {
				result.put("#TABLE", comment.value());
			}
		}
		for (ColumnMapping column : this.getColumns()) {
			FieldEx field = BeanUtils.getField(thisType, column.fieldName());
			if (field == null) {
				continue;
			}
			Comment comment = field.getAnnotation(Comment.class);
			if (comment != null) {
				result.put(column.fieldName(), comment.value());
			}
		}
		return result;
	}

	/**
	 * 尝试从源码的注释中获得列的注解信息
	 * 
	 * @return
	 */
	private Map<String, String> getFromSource() {
		Map<String, String> result = new HashMap<String, String>();
		Class<?> type = thisType;
		URL url = this.getClass().getResource("/" + type.getName().replace('.', '/') + ".java");
		if (url == null) {
			url = getFixedPathSource(type);
		}

		if (url == null)
			return result;
		try {
			InputStream in = url.openStream();
			try {
				CompilationUnit unit = JavaParser.parse(in, "UTF-8");
				if (unit.getTypes().isEmpty())
					return result;
				TypeDeclaration typed = unit.getTypes().get(0);
				if (typed instanceof ClassOrInterfaceDeclaration) {
					ClassOrInterfaceDeclaration clz = (ClassOrInterfaceDeclaration) typed;
					String table = getContent(clz.getComment());
					if (table != null)
						result.put("#TABLE", table);
					for (BodyDeclaration body : typed.getMembers()) {
						if (body instanceof FieldDeclaration) {
							FieldDeclaration field = (FieldDeclaration) body;
							if (ModifierSet.isStatic(field.getModifiers())) {
								continue;
							}
							if (field.getVariables().size() > 1) {
								continue;
							}
							String name = field.getVariables().get(0).getId().getName();
							String javaDoc = getContent(field.getComment());
							if (javaDoc != null)
								result.put(name, javaDoc);
						}
					}
				}
			} finally {
				IOUtils.close(in);
			}
		} catch (ParseException e) {
			LogUtil.exception(e);
		} catch (IOException e) {
			LogUtil.exception(e);
		}
		return result;
	}

	private String getContent(com.github.javaparser.ast.comments.Comment comment) {
		if (comment == null)
			return null;
		String s = comment.getContent();
		return s.replaceAll("\\s*\\*", "").trim();
	}

	/**
	 * 在开发环境的标准Maven目录场景情况下，寻找到源代码，实现注解信息读取
	 * 
	 * @param type
	 * @return
	 */
	private URL getFixedPathSource(Class type) {
		String clzPath = "/" + type.getName().replace('.', '/') + ".class";
		URL url = this.getClass().getResource(clzPath);
		if (url == null)
			return null;
		String path = url.getPath();
		path = path.substring(0, path.length() - clzPath.length());
		File source = null;
		if (path.endsWith("/target/test-classes")) {
			source = new File(path.substring(0, path.length() - 20), "src/test/java");
		} else if (path.endsWith("/target/classes")) {
			source = new File(path.substring(0, path.length() - 15), "src/main/java");
		}
		if (source == null)
			return null;
		File java = new File(source, type.getName().replace('.', '/') + ".java");
		if (java.exists())
			try {
				return java.toURI().toURL();
			} catch (MalformedURLException e) {
				LogUtil.exception(e);
				return null;
			}
		return null;
	}

	// 处理非元模型的Column描述字段
	public void addColumnHelper(FieldAnnotationProvider field) {
		Column column = field.getAnnotation(Column.class);
		if (column != null) {
			String name = column.name();
			if (StringUtils.isEmpty(name)) {
				name = field.getName();
			}
			lowerColumnToFieldName.put(name.toLowerCase(), field.getName());
		}
	}
}
