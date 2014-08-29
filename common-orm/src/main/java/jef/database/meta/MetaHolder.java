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
import java.lang.reflect.Modifier;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

import jef.accelerator.asm.Attribute;
import jef.accelerator.asm.ClassReader;
import jef.accelerator.asm.ClassVisitor;
import jef.common.log.LogUtil;
import jef.common.wrapper.Holder;
import jef.database.Condition.Operator;
import jef.database.DbCfg;
import jef.database.DbMetaData;
import jef.database.DbMetaData.TableInfo;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.JefClassLoader;
import jef.database.ORMConfig;
import jef.database.PojoWrapper;
import jef.database.Session;
import jef.database.VarMeta;
import jef.database.VarObject;
import jef.database.annotation.Cascade;
import jef.database.annotation.EasyEntity;
import jef.database.annotation.FieldOfTargetEntity;
import jef.database.annotation.HiloGeneration;
import jef.database.annotation.Indexed;
import jef.database.annotation.Indexes;
import jef.database.annotation.JoinDescription;
import jef.database.annotation.JoinType;
import jef.database.annotation.NoForceEnhance;
import jef.database.dialect.ColumnType;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.create.ColumnDefinition;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.query.ReadOnlyQuery;
import jef.database.query.SqlExpression;
import jef.database.support.EntityNotEnhancedException;
import jef.database.support.QuerableEntityScanner;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.IOUtils;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;
import jef.tools.reflect.BeanUtils;

import com.google.common.collect.ArrayListMultimap;

/**
 * 静态存放所有数据表元模型的存放类。
 * 
 * 各个数据表模型都可以从这个类的方法中得到。
 * 
 * @author jiyi
 * 
 */
@SuppressWarnings("deprecation")
public final class MetaHolder {
	private MetaHolder() {
	}

	// 分表分库策略加载器
	static private PartitionStrategyLoader partitionLoader;
	// 元数据加载器
	static MetadataConfiguration config;
	// Schema映射
	static Map<String, String> SCHEMA_MAPPING;
	// 站点映射
	static Map<String, String> SITE_MAPPING;

	// 元数据池
	static final Map<Class<?>, MetadataAdapter> pool = new java.util.IdentityHashMap<Class<?>, MetadataAdapter>(32);
	// 动态表元数据池
	static final Map<String, TupleMetadata> dynPool = new java.util.HashMap<String, TupleMetadata>(32);
	// 反向查找表
	private static final Map<String, ITableMetadata> inverseMapping = new HashMap<String, ITableMetadata>();

	// 初始化分表规则加载器
	static {
		try {
			String clz = JefConfiguration.get(DbCfg.PARTITION_STRATEGY_LOADER);
			if (StringUtils.isNotEmpty(clz)) {
				partitionLoader = (PartitionStrategyLoader) BeanUtils.newInstance(clz);
			}
			if (partitionLoader == null) {
				partitionLoader = new DefaultPartitionStrategyLoader();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			String clz = JefConfiguration.get(DbCfg.CUSTOM_METADATA_LOADER);
			if (StringUtils.isNotEmpty(clz)) {
				config = (MetadataConfiguration) BeanUtils.newInstance(clz);
			}
			if (config == null) {
				config = new DefaultMetaLoader();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			SCHEMA_MAPPING = StringUtils.toMap(JefConfiguration.get(DbCfg.SCHEMA_MAPPING), ",", ":", 1);
			String configStr = JefConfiguration.get(DbCfg.DB_DATASOURCE_MAPPING);
			SITE_MAPPING = StringUtils.toMap(configStr, ",", ":", -1);
			if (!SITE_MAPPING.isEmpty()) {
				LogUtil.info("Database mapping: " + SITE_MAPPING);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获得重定向后的Schema
	 * 
	 * @param key
	 * @return 如果返回""，表示无schema 如果返回入参本身，表示不作改动 其他情况，返回重定向的schema
	 */
	public static String getMappingSchema(String key) {
		if (key == null)
			return null;
		String result = SCHEMA_MAPPING.get(key.toUpperCase());
		if (result == null)
			return key;
		return result.length() == 0 ? null : result;
	}

	/**
	 * 获得重定向后的Site
	 * 
	 * @param key
	 * @return 如果返回""，表示无Site 如果返回入参本身，表示不作改动 其他情况，返回重定向的Site
	 */
	public static String getMappingSite(String key) {
		if (key == null)
			return null;
		String result = SITE_MAPPING.get(key.toLowerCase());
		if (result == null)
			return key;
		return result.length() == 0 ? null : result;
	}

	/**
	 * 根据数据库的表情况，初始化动态表的模型 初始化完成后会缓存起来，下次获取可以直接用{@link #getMeta(String)}得到
	 * 
	 * @param session 数据库访问句柄 Session.
	 * @param tableName 表名
	 * @return
	 */
	public static TupleMetadata initMetadata(Session session, String tableName) throws SQLException {
		return initMetadata(session, tableName, true);
	}

	/**
	 * 根据数据库的表情况，初始化动态表的模型 初始化完成后会缓存起来，下次获取可以直接用{@link #getMeta(String)}得到
	 * 
	 * @param session  数据库访问句柄 Session.
	 * @param tableName 表名
	 * @param convertColumnNames
	 *            是否将数据库的列名转换为 java 习惯。 <br/>
	 *            eg. CREATE_TIME -> createTime
	 * @return
	 */
	public static TupleMetadata initMetadata(Session session, String tableName, boolean convertColumnNames) throws SQLException {
		DbMetaData meta = session.getNoTransactionSession().getMetaData(null);
		List<TableInfo> table = meta.getTable(tableName);
		if (table.isEmpty()) {
			throw new SQLException("The table " + tableName + " does not exit in database " + session.getNoTransactionSession().toString());
		}
		PrimaryKey pks = meta.getPrimaryKey(tableName);
		List<jef.database.meta.Column> columns = meta.getColumns(tableName);
		TupleMetadata m = new TupleMetadata(tableName);
		for (jef.database.meta.Column c : columns) {
			boolean isPk = (pks == null) ? false : pks.hasColumn(c.getColumnName());
			// m.addColumn(c.getColumnName(), c.getColumnName(),
			// c.toColumnType(meta.getProfile()), isPk);
			m.addColumn(DbUtils.underlineToUpper(c.getColumnName(), false), c.getColumnName(), c.toColumnType(meta.getProfile()), isPk);
		}
		putDynamicMeta(m);
		return m;
	}

	/**
	 * 放置动态表的模型
	 * 
	 * @param meta
	 */
	public static void putDynamicMeta(TupleMetadata meta) {
		String name = meta.getTableName(true).toUpperCase();
		TupleMetadata old = dynPool.put(name, meta);
		if (old != null) {
			LogUtil.warn("replace tuple metadata:{}", name);
		}
	}

	/**
	 * 返回动态表的模型
	 * 
	 * @param name
	 * @return
	 */
	public static TupleMetadata getDynamicMeta(String name) {
		if (name == null)
			return null;
		return dynPool.get(name.toUpperCase());
	}

	/**
	 * 初始化数据，可以指定schema和tablename
	 * 
	 * @param clz
	 *            实体类
	 * @param schema
	 *            传入null表示不修改默认的schema，传""表示修改为当前数据库schema，传入其他则为指定的schema
	 * @param tablename
	 *            传入null表示不修正
	 * @return
	 */
	public static ITableMetadata initMetadata(Class<? extends IQueryableEntity> clz, String schema, String tablename) {
		Assert.notNull(clz);
		TableMetadata m = (TableMetadata) pool.get(clz);
		if (m == null) {
			m = (TableMetadata) initData(clz);
			pool.put(clz, m);
		}
		if (schema != null)
			m.setSchema(getMappingSchema(schema));
		if (StringUtils.isNotEmpty(tablename))
			m.setTableName(tablename);
		return m;
	}

	/**
	 * 将一个对象名（数据表、索引等）转换为schemaMapping后的名称
	 * 
	 * @param objectName
	 */
	public static String toSchemaAdjustedName(String objectName) {
		if (objectName == null) {
			return null;
		}

		int n = objectName.indexOf('.');
		if (n < 0)
			return objectName;
		String schema = objectName.substring(0, n);
		String schema1 = MetaHolder.getMappingSchema(schema);
		if (schema == schema1) {
			return objectName;
		}
		return schema1 == null ? objectName.substring(n + 1) : schema1.concat(objectName.substring(n));
	}

	/**
	 * 获取所有已经缓存的动态表模型
	 * 
	 * @return
	 */
	public static Collection<TupleMetadata> getCachedDynamicModels() {
		return dynPool.values();
	}

	/**
	 * 获取所有已经缓存的静态表模型
	 */
	public static Collection<MetadataAdapter> getCachedModels() {
		return pool.values();
	}

	/**
	 * 根据类获取表模型
	 * 
	 * @param clz
	 * @return
	 */
	public static final MetadataAdapter getMeta(Class<?> clz) {
		Assert.notNull(clz);
		if (clz == VarObject.class) {
			throw new IllegalArgumentException("A VarObject class does not indicted to any table metadata.");
		}
		MetadataAdapter m = pool.get(clz);
		return m == null ? initData(clz) : m;
	}

	/**
	 * 获取metadata
	 * 
	 * @param d
	 * @return
	 */
	public final static MetadataAdapter getMeta(Object d) {
		if (d instanceof VarMeta) {
			return (MetadataAdapter)((VarMeta) d).meta();
		}
		return getMeta(d.getClass());
	}

	private static boolean isFirstInterfaceClzEntity(Class<?> sc) {
		Class<?>[] interfaces = sc.getInterfaces();
		if (interfaces == null || interfaces.length == 0)
			return false;
		return ArrayUtils.contains(interfaces, IQueryableEntity.class);
	}

	/**
	 * 在获取类时，需要有一个标记快速判断该类是否经过增强（无论是动态增强还是静态增强）一旦发现没增强的类，就抛出异常�?
	 * 
	 * @param clz
	 * @return
	 */
	private synchronized static MetadataAdapter initData(Class<?> clz) {
		{
			MetadataAdapter m1 = pool.get(clz);
			if (m1 != null)
				return m1; // 双重检查锁定
		}
		if (IQueryableEntity.class.isAssignableFrom(clz)) {
			return initEntity(clz.asSubclass(IQueryableEntity.class));
		} else {
			return initPojo(clz);
		}
	}

	private static MetadataAdapter initPojo(Class<?> clz) {
		AnnotationProvider annos = config.getAnnotations(clz);
		TableMetadata meta = new TableMetadata(PojoWrapper.class, clz, annos);
		List<java.lang.reflect.Field> unprocessedField = new ArrayList<java.lang.reflect.Field>();

		MeteModelFields metaFields = new MeteModelFields(clz, meta);

		Class<?> processingClz = clz;
		while (processingClz != Object.class) {
			processMetaForClz(processingClz, unprocessedField, meta, annos, metaFields);
			processingClz = processingClz.getSuperclass();
			if (isFirstInterfaceClzEntity(processingClz)) {
				break;
			}
		}
		metaFields.check();
		return meta;
	}

	private static MetadataAdapter initEntity(Class<? extends IQueryableEntity> clz) {
		AnnotationProvider annos = config.getAnnotations(clz);
		{
			// Entity e = annos.getAnnotation(Entity.class);
			// if (e == null)
			// throw new IllegalArgumentException(clz.getName() +
			// " is not a Entity Class");
			EasyEntity ee = annos.getAnnotation(EasyEntity.class);
			if (ORMConfig.getInstance().isCheckEnhancement()) {
				assertEnhanced(clz, ee, annos);
			}
		}
		TableMetadata meta = new TableMetadata(clz, annos);
		List<java.lang.reflect.Field> unprocessedField = new ArrayList<java.lang.reflect.Field>();

		MeteModelFields metaFields = new MeteModelFields(clz, meta);

		Class<?> processingClz = clz;
		while (processingClz != Object.class) {
			processMetaForClz(processingClz, unprocessedField, meta, annos, metaFields);
			if (processingClz != clz) {
				meta.addParent(processingClz);
			}
			processingClz = processingClz.getSuperclass();// 父类:下一个要解析的类
			if (isFirstInterfaceClzEntity(processingClz)) {
				break;
			}
		}
		metaFields.check();
		// 计算复合索引
		Indexes indexes = annos.getAnnotation(Indexes.class);
		if (indexes != null) {
			for (jef.database.annotation.Index index : indexes.value()) {
				meta.indexMap.add(index);
			}
		}
		// 加载分表策略
		Assert.notNull(partitionLoader, "the Partition loader is null!");
		meta.setPartition(partitionLoader.get(clz));
		// 此时就将基本字段计算完成的元数据加入缓存，以免在多表关系处理时遭遇死循环
		pool.put(clz, meta);

		// 针对未处理的字段，当做外部引用关系处理
		for (java.lang.reflect.Field f : unprocessedField) {
			// 将这个字段作为外部引用处理
			processReference(f, meta, annos);
			// 还有一种情况，即定义了Column注解，但不属于元模型的一个字段，用于辅助映射的。当结果拼装时有用
			processColumnHelper(f, meta, annos);
		}
		return meta;
	}

	// 处理非元模型的Column描述字段
	private static void processColumnHelper(java.lang.reflect.Field f, TableMetadata meta, AnnotationProvider annos) {
		Column column = annos.getFieldAnnotation(f, Column.class);
		if (column != null) {
			meta.addNonMetaModelFieldMapping(f.getName(), column);
		}
	}

	/**
	 * 检查是否执行了增强
	 * 
	 * @param type
	 */
	private static void assertEnhanced(Class<? extends IQueryableEntity> type, EasyEntity ee, AnnotationProvider annos) {
		if (annos.getAnnotation(NoForceEnhance.class) != null) {
			return;
		}
		if (ee != null && ee.checkEnhanced() == false) {
			return;
		}
		// 如果实体扫描时作了动态增强的话
		if (QuerableEntityScanner.dynamicEnhanced.contains(type.getName())) {
			return;
		}
		// 仅需对非JefClassLoader加载的类做check.
		if (type.getClassLoader().getClass().getName().equals(JefClassLoader.class.getName())) {
			return;
		}
		String resourceName = type.getName().replace('.', '/') + ".class";
		URL url = type.getClassLoader().getResource(resourceName);
		if (url == null) {
			LogUtil.warn("The source of class " + type + " not found, skip enhanced-check.");
			return;
		}
		byte[] data;
		try {
			data = IOUtils.toByteArray(url);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		ClassReader cr = new ClassReader(data);

		final Holder<Boolean> checkd = new Holder<Boolean>(false);
		cr.accept(new ClassVisitor() {
			public void visitAttribute(Attribute attr) {
				if ("jefd".equals(attr.type)) {
					checkd.set(true);
				}
				super.visitAttribute(attr);
			}
		}, ClassReader.SKIP_CODE);
		if (!checkd.get()) {
			throw new EntityNotEnhancedException(type.getName());
		}
		// System.out.println("You may not executing project with the default Jetty Console. this may disable the dynamic enhance feature. please make sure you have enhaced the entity staticly.");
	}

	static class MeteModelFields {
		private boolean isTuple;
		ArrayListMultimap<String, Field> enumFields;
		private ITableMetadata parent;

		MeteModelFields(Class<?> clz, ITableMetadata meta) {
			isTuple = !IQueryableEntity.class.isAssignableFrom(clz);
			parent = meta;

			if (isTuple)
				return;
			enumFields = com.google.common.collect.ArrayListMultimap.create();
			Class<?> looping = clz;
			while (looping != Object.class) {
				for (Class<?> c : looping.getDeclaredClasses()) {
					if (c.isEnum() && ArrayUtils.contains(c.getInterfaces(), jef.database.Field.class)) {
						@SuppressWarnings("rawtypes")
						Class<? extends Enum> sub = c.asSubclass(Enum.class);
						for (Enum<?> fieldDef : sub.getEnumConstants()) {
							Field field = (Field) fieldDef;
							enumFields.put(field.name(), field);// 父类的放在后面，子类的放在前面。
						}
						break;
					}
				}
				looping = looping.getSuperclass();
			}
		}

		public void check() {
			if (hasMappingFailure()) {
				throw new IllegalArgumentException("These meta model field is not exist in [" + parent.getName() + "]:" + enumFields.keySet());
			}
		}

		List<jef.database.Field> remove(String name) {
			if (isTuple) {
				return Collections.<Field> singletonList(new TupleField(parent, name));
			}
			return enumFields.removeAll(name);
		}

		boolean hasMappingFailure() {
			if (isTuple) {
				return false;
			} else {
				return !enumFields.isEmpty();
			}
		}
	}

	private static void processMetaForClz(Class<?> processingClz, List<java.lang.reflect.Field> unprocessedField, TableMetadata meta, AnnotationProvider annos, MeteModelFields metaModel) {
		for (java.lang.reflect.Field f : processingClz.getDeclaredFields()) {
			if (Modifier.isStatic(f.getModifiers()))
				continue;
			if (meta.getField(f.getName()) != null) { // 当子类父类中有同名field时，跳过父类的field
				continue;
			}

			Column c = annos.getFieldAnnotation(f, Column.class);
			List<Field> fieldss = metaModel.remove(f.getName());
			if (fieldss.isEmpty()) {
				unprocessedField.add(f);
				continue;
			}
			Field field = fieldss.get(0);
			if (field instanceof Enum) {
				/*
				 * 必须至少有一个meta field的定义类==
				 * processingClz。这样才能保证这个属性被增强过。否则不能保证该属性被增强过。
				 * 
				 * 因为目前增强算法都只按当前类的enum Field中的枚举来增强属性。不会去增强父类中的属性。
				 * 所以如果在父类中定义属性而在子类中定义元模型来使用。这个属性就会有未被增强的风险。
				 * 
				 * 增加这样的检查逻辑，有利于用户在复杂继承关系下，确保父类的元模型不缺失，从而安全的使用。
				 * 
				 * 关于为什么不作增强父类的功能： a 父类可能在JAR包中，不能直接修改。 b
				 * 如果在子类中通过覆盖方法来实现，也有问题，因为ASM中去解析父类并查找同名方法较为复杂
				 * 。在增强前，不能调用类实现反射，因此相当于要自行用ASM实现父子类解析的JAVA逻辑，太麻烦了…… c
				 * 此外，如果父类本身也定义了该元模型
				 * ，子类覆盖父类元模型，此时也很悲剧——子类生成一个增强过的方法覆盖父类方法，而父类本身又做了增强
				 * ，此时延迟加载和等植入代码将被执行两遍。
				 * 因此，我们还是要尽可能避免这种父类定义属性，子类定义元模型的方式。即元模型要定义在各自的类里，子类可以覆盖父类的。
				 */
				boolean isEnhancedProperty = false;
				for (Field ff : fieldss) {
					Class<?> cc = ff.getClass().getDeclaringClass();
					if (cc == processingClz) {
						isEnhancedProperty = true;
						break;
					}
				}
				if (!isEnhancedProperty) {
					throw new IllegalArgumentException("Field [" + field.name() + "] may be not enhanced. Please add the enum Field [" + field.name() + "] into " + processingClz.getName());
				}
			}

			// 在得到了元模型的情况下
			javax.persistence.Id id = annos.getFieldAnnotation(f, javax.persistence.Id.class);
			GeneratedValue gv = annos.getFieldAnnotation(f, javax.persistence.GeneratedValue.class);
			ColumnType ct;
			try {
				if (c == null || StringUtils.isEmpty(c.columnDefinition())) {// 在没有Annonation的情况下,根据Field类型默认生成元数�?
					ct = defaultColumnTypeByJava(f, c, gv, annos);
				} else {
					ct = createTypeByAnnotation(c.columnDefinition().toUpperCase(), c.length(), c.precision(), c.scale(), gv, f, annos);
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(processingClz + " has invalid field/column " + f.getName(), e);
			}
			if (id != null)
				ct.notNull();
			meta.putJavaField(field, ct, id, c);
			// 设置索引
			Indexed i = annos.getFieldAnnotation(f, Indexed.class);// 单列索引
			if (i != null) {
				Map<String, Object> data = new HashMap<String, Object>(4);
				data.put("fields", new String[] { field.name() });
				data.put("definition", i.definition());
				data.put("name", i.name());
				meta.indexMap.add(BeanUtils.asAnnotation(jef.database.annotation.Index.class, data));
			}
		}
	}
	
	private static JoinPath getHint(AnnotationProvider annos, java.lang.reflect.Field f, MetadataAdapter meta, ITableMetadata target) {
		if (annos.getFieldAnnotation(f, JoinColumn.class) != null) {
			JoinColumn j = annos.getFieldAnnotation(f, JoinColumn.class);
			return processJoin(meta, f.getName(), target, annos.getFieldAnnotation(f, JoinDescription.class), j);
		} else if (annos.getFieldAnnotation(f, JoinColumns.class) != null) {
			JoinColumns jj = annos.getFieldAnnotation(f, JoinColumns.class);
			return processJoin(meta, f.getName(), target, annos.getFieldAnnotation(f, JoinDescription.class), jj.value());
		}
		return null;
	}

	private static boolean processReference(java.lang.reflect.Field f, TableMetadata meta, AnnotationProvider annos) {
		FieldOfTargetEntity targetField = annos.getFieldAnnotation(f, FieldOfTargetEntity.class);
		CascadeConfig config=new CascadeConfig();
		config.asMap=annos.getFieldAnnotation(f, Cascade.class);
		
		
		
		if (annos.getFieldAnnotation(f, OneToOne.class) != null) {
			OneToOne r1Vs1 = annos.getFieldAnnotation(f, OneToOne.class);
			ITableMetadata target = getTargetType(r1Vs1.targetEntity(), targetField, f, false);
			config.path =getHint(annos,f,meta,target);
			if(config.path==null){
				String mappedBy=r1Vs1.mappedBy();
				if(StringUtils.isNotEmpty(mappedBy)){
					config.path=processJoin(meta,f.getName(),target,annos.getFieldAnnotation(f, JoinDescription.class),mappedBy); 
				}
			}
			if (targetField == null) {
				meta.addRefField_1vs1(f.getType(), f.getName(), target, r1Vs1,config);
			} else {
				jef.database.Field field = target.getField(targetField.value());
				Assert.notNull(field);
				meta.addRefField_1vs1(f.getType(), f.getName(), field, config);
			}
			return true;
		}
		if (annos.getFieldAnnotation(f, OneToMany.class) != null) {
			OneToMany r1VsN = annos.getFieldAnnotation(f, OneToMany.class);
			ITableMetadata target = getTargetType(r1VsN.targetEntity(), targetField, f, true);
			config.path =getHint(annos,f,meta,target);
			if(config.path==null){
				String mappedBy=r1VsN.mappedBy();
				if(StringUtils.isNotEmpty(mappedBy)){
					config.path=processJoin(meta,f.getName(),target,annos.getFieldAnnotation(f, JoinDescription.class),mappedBy); 
				}
			}
			if (targetField == null) {
				meta.addRefField_1vsN(f.getType(), f.getName(), target, r1VsN, config);
			} else {
				jef.database.Field field = target.getField(targetField.value());
				Assert.notNull(field);
				meta.addRefField_1vsN(f.getType(), f.getName(), field, config);
			}
			return true;
		}

		if (annos.getFieldAnnotation(f, ManyToOne.class) != null) {
			ManyToOne rNVs1 = annos.getFieldAnnotation(f, ManyToOne.class);
			ITableMetadata target = getTargetType(rNVs1.targetEntity(), targetField, f, false);
			config.path =getHint(annos,f,meta,target);
			if (targetField == null) {
				meta.addRefField_Nvs1(f.getType(), f.getName(), target, rNVs1, config);
			} else {
				jef.database.Field field = target.getField(targetField.value());
				if(field==null){
					throw new IllegalArgumentException("["+targetField.value()+"] is not exist in entity:"+ target.getName());
				}
				meta.addRefField_Nvs1(f.getType(), f.getName(), field, config);
			}
			return true;
		}
		if (annos.getFieldAnnotation(f, ManyToMany.class) != null) {
			ManyToMany rNVsN = annos.getFieldAnnotation(f, ManyToMany.class);
			ITableMetadata target = getTargetType(rNVsN.targetEntity(), targetField, f, true);
			config.path =getHint(annos,f,meta,target);
			if(config.path==null){
				String mappedBy=rNVsN.mappedBy();
				if(StringUtils.isNotEmpty(mappedBy)){
					config.path=processJoin(meta,f.getName(),target,annos.getFieldAnnotation(f, JoinDescription.class),mappedBy); 
				}
			}
			if (targetField == null) {
				meta.addRefField_NvsN(f.getType(), f.getName(), target, rNVsN, config);
			} else {
				jef.database.Field field = target.getField(targetField.value());
				Assert.notNull(field);
				meta.addRefField_NvsN(f.getType(), f.getName(), field, config);
			}
			return true;
		}
		return false;
	}



	/**
	 * 计算出目标连接的类型
	 * 
	 * @param targetEntity
	 * @param targetField
	 * @param type
	 * @param isMany
	 *            是toMany类型的连�?
	 * @return
	 */
	private static ITableMetadata getTargetType(Class<?> targetEntity, FieldOfTargetEntity targetField, java.lang.reflect.Field field, boolean isMany) {
		if (targetEntity != void.class) {
			if (IQueryableEntity.class.isAssignableFrom(targetEntity)) {
				return MetaHolder.getMeta(targetEntity.asSubclass(IQueryableEntity.class));
			} else {
				throw new IllegalArgumentException("The target entity type [" + targetEntity.getName() + "] for " + field.getDeclaringClass().getSimpleName() + ":" + field.getName() + " is not subclass of DataObject.");
			}
		}
		if (targetField != null) {
			throw new IllegalArgumentException(field.getDeclaringClass().getSimpleName() + ":" + field.getName() + " miss its targetEntity annotation.");
		}
		if (isMany) {
			Class<?> compType = CollectionUtil.getSimpleComponentType(field.getGenericType());
			if (compType != null && IQueryableEntity.class.isAssignableFrom(compType)) {
				return MetaHolder.getMeta(compType.asSubclass(IQueryableEntity.class));
			}
		} else {
			Class<?> compType = field.getType();
			if (IQueryableEntity.class.isAssignableFrom(compType)) {
				return MetaHolder.getMeta(compType.asSubclass(IQueryableEntity.class));
			}
		}
		throw new IllegalArgumentException(field.getDeclaringClass().getSimpleName() + ":" + field.getName() + " miss its targetEntity annotation.");
	}

	private static JoinPath processJoin(MetadataAdapter thisMeta, String fieldName, ITableMetadata target, JoinDescription joinDesc, JoinColumn... jj) {
		List<JoinKey> result = new ArrayList<JoinKey>();
		for (JoinColumn j : jj) {
			if (StringUtils.isBlank(j.name())) {
				throw new IllegalArgumentException("Invalid reference [" + thisMeta.getThisType().getName() + "." + fieldName + "]:The field 'name' in JoinColumn is empty");
			}
			Field left = thisMeta.getField(j.name());
			Assert.notNull(left, "Invalid reference [" + thisMeta.getThisType().getName() + "." + fieldName + "]: field [" + j.name() + "] not found from entity " + thisMeta.getThisType().getName());
			Field right = target.getField(j.referencedColumnName());
			if (right == null) {
				throw new NullPointerException("Invalid reference [" + thisMeta.getThisType().getName() + "." + fieldName + "]: '" + j.referencedColumnName() + "' is not available in " + target.getThisType().getName());
			}
			result.add(new JoinKey(left, right));
		}
		JoinType type = JoinType.LEFT;
		if (joinDesc != null) {
			type = joinDesc.type();
		}
		if (result.size() > 0) {
			JoinPath path = new JoinPath(type, result.toArray(new JoinKey[result.size()]));
			path.setDescription(joinDesc);
			if (joinDesc != null && joinDesc.filterCondition().length() > 0) {
				JoinKey joinExpress = getJoinExpress(target, joinDesc.filterCondition().trim());
				if (joinExpress != null)
					path.addJoinKey(joinExpress);
			}
			return path;
		}
		return null;
	}

	private static JoinPath processJoin(TableMetadata meta, String name, ITableMetadata target, JoinDescription joinDesc, String mappedBy) {
		List<JoinKey> result = new ArrayList<JoinKey>();

		if (meta.getPKField().size() != 1) {
			throw new IllegalArgumentException(meta.getSimpleName() + " cann't map to " + target.getSimpleName() + " since its primary key field count " + meta.getPKField().size());
		}
		Field left = meta.getPKField().get(0);
		Field right = target.getField(mappedBy);
		if (right == null) {
			throw new IllegalArgumentException(meta.getSimpleName() + " cann't map to " + target.getSimpleName() + " since there is no field [" + mappedBy + "] in target entity");
		}
		result.add(new JoinKey(left, right));
		JoinType type = JoinType.LEFT;
		if (joinDesc != null) {
			type = joinDesc.type();
		}
		if (result.size() > 0) {
			JoinPath path = new JoinPath(type, result.toArray(new JoinKey[result.size()]));
			path.setDescription(joinDesc);
			if (joinDesc != null && joinDesc.filterCondition().length() > 0) {
				JoinKey joinExpress = getJoinExpress(target, joinDesc.filterCondition().trim());
				if (joinExpress != null)
					path.addJoinKey(joinExpress);
			}
			return path;
		}
		return null;
	}

	private static JoinKey getJoinExpress(ITableMetadata targetMeta, String exp) {
		try {
			Expression ex = DbUtils.parseBinaryExpression(exp);
			if (ex instanceof BinaryExpression) {
				BinaryExpression bin = (BinaryExpression) ex;
				String left = bin.getLeftExpression().toString().trim();
				Field leftF = targetMeta.findField(left);// 假定左边的是字段
				JoinKey key;
				if (leftF == null) {
					key = new JoinKey(null, null, new FBIField(bin, ReadOnlyQuery.getEmptyQuery(targetMeta)));// 建立一个函数Field
				} else {
					String oper = bin.getStringExpression();
					Object value = new FBIField(bin.getRightExpression().toString(), ReadOnlyQuery.getEmptyQuery(targetMeta));
					key = new JoinKey(leftF, Operator.valueOfKey(oper), value);
				}
				return key;
			} else {
				throw new RuntimeException("the expression " + exp + " is not a Binary Expression but a " + ex.getClass().getName());
			}
		} catch (ParseException e) {
			throw new RuntimeException("Unknown expression config on class:" + targetMeta.getThisType().getName() + ": " + exp);
		}
	}

	private static ColumnType createTypeByAnnotation(String defStr, int length, int precision, int scale, GeneratedValue gv, java.lang.reflect.Field field, AnnotationProvider annos) throws ParseException {
		ColumnDefinition c = DbUtils.parseColumnDef(defStr);
		String def = c.getColDataType().getDataType();
		SqlExpression defaultExpression = null;
		boolean nullable = true;
		List<String> params = c.getColDataType().getArgumentsStringList();
		String[] typeArgs = params == null ? ArrayUtils.EMPTY_STRING_ARRAY : params.toArray(new String[params.size()]);
		if (c.getColumnSpecStrings() != null) {
			for (int i = 0; i < c.getColumnSpecStrings().size(); i++) {
				String s = c.getColumnSpecStrings().get(i);
				if ("not".equalsIgnoreCase(s)) {
					i++;
					String s1 = c.getColumnSpecStrings().get(i);
					if ("null".equalsIgnoreCase(s1)) {
						nullable = false;
					}
				} else if ("null".equalsIgnoreCase(s)) {
					nullable = true;
				} else {
					if ("default".equalsIgnoreCase(s)) {
						String ex = c.getColumnSpecStrings().get(++i);
						if (ex.length() > 0) {
							defaultExpression = new SqlExpression(ex);
						}
					}
				}
			}
		}
		// //////////////////////////////////////////////
		GenerationType geType = gv == null ? null : gv.strategy();
		if ("VARCHAR".equals(def) || "VARCHAR2".equals(def)) {
			if (geType != null) {
				if (geType == GenerationType.TABLE || geType == GenerationType.SEQUENCE) {
					return new ColumnType.AutoIncrement(length, geType, annos.getFieldAnnotation(field, TableGenerator.class), annos.getFieldAnnotation(field, SequenceGenerator.class), annos.getFieldAnnotation(field, HiloGeneration.class));
				} else {
					return new ColumnType.GUID();
				}
			}
			if (typeArgs.length > 0) {
				length = StringUtils.toInt(typeArgs[0], length);
			}
			Assert.isTrue(length > 0);
			return new ColumnType.Varchar(length).setNullable(nullable).defaultIs(defaultExpression);
		} else if ("CHAR".equalsIgnoreCase(def)) {
			if (geType != null) {
				if (geType == GenerationType.TABLE || geType == GenerationType.SEQUENCE) {
					return new ColumnType.AutoIncrement(length, geType, annos.getFieldAnnotation(field, TableGenerator.class), annos.getFieldAnnotation(field, SequenceGenerator.class), annos.getFieldAnnotation(field, HiloGeneration.class));
				} else {
					return new ColumnType.GUID();
				}
			}
			if (typeArgs.length > 0) {
				length = StringUtils.toInt(typeArgs[0], length);
			}
			Assert.isTrue(length > 0, "The char column length must greater than 0!");
			return new ColumnType.Char(length).setNullable(nullable).defaultIs(defaultExpression);
		} else if ("NUMBER".equals(def) || "INT".equals(def) || "INTEGER".equals(def)) {
			if (precision == 0 && (length > 0 && length < 100)) {
				precision = length;
			}
			if (geType != null) {
				return new ColumnType.AutoIncrement(precision, geType, annos.getFieldAnnotation(field, TableGenerator.class), annos.getFieldAnnotation(field, SequenceGenerator.class), annos.getFieldAnnotation(field, HiloGeneration.class));
			} else if (scale > 0) {
				return new ColumnType.Double(precision, scale).setNullable(nullable).defaultIs(defaultExpression);
			} else {
				return new ColumnType.Int(precision).setNullable(nullable).defaultIs(defaultExpression);
			}
		} else if ("CLOB".equalsIgnoreCase(def)) {
			return new ColumnType.Clob().setNullable(nullable).defaultIs(defaultExpression);
		} else if ("BLOB".equalsIgnoreCase(def)) {
			return new ColumnType.Blob().setNullable(nullable).defaultIs(defaultExpression);
		} else if ("Date".equalsIgnoreCase(def)) {
			ColumnType.Date d = new ColumnType.Date();
			d.setNullable(nullable).defaultIs(defaultExpression);
			d.setGenerateType(getDateGenerateType(gv));
			return d;
		} else if ("TIMESTAMP".equalsIgnoreCase(def)) {
			ColumnType.TimeStamp d = new ColumnType.TimeStamp();
			d.setNullable(nullable).defaultIs(defaultExpression);
			d.setGenerateType(getDateGenerateType(gv));
			return d;
		} else if ("BOOLEAN".equalsIgnoreCase(def)) {
			return new ColumnType.Boolean().setNullable(nullable).defaultIs(defaultExpression);
		} else if ("XML".equalsIgnoreCase(def)) {
			return new ColumnType.XML().setNullable(nullable);
		} else {
			throw new IllegalArgumentException("Unknow column Def:" + def);
		}
	}

	private static ColumnType defaultColumnTypeByJava(java.lang.reflect.Field field, Column c, GeneratedValue gv, AnnotationProvider annos) {
		int len = c == null ? 0 : c.length();
		int precision = c == null ? 0 : c.precision();
		int scale = c == null ? 0 : c.scale();
		GenerationType geType = gv == null ? null : gv.strategy();
		if (geType != null && field.getType() == String.class) {
			return new ColumnType.GUID();
		} else if (geType != null && Number.class.isAssignableFrom(BeanUtils.toWrapperClass(field.getType()))) {
			return new ColumnType.AutoIncrement(precision, geType, annos.getFieldAnnotation(field, TableGenerator.class), annos.getFieldAnnotation(field, SequenceGenerator.class), annos.getFieldAnnotation(field, HiloGeneration.class));
		}
		Lob lob = annos.getFieldAnnotation(field, Lob.class);
		Class<?> type = field.getType();
		if (type == String.class) {
			if (lob != null)
				return new ColumnType.Clob();
			return new ColumnType.Varchar(len > 0 ? len : 255);
		} else if (type == Integer.class) {
			return new ColumnType.Int(precision);
		} else if (type == Integer.TYPE) {
			return new ColumnType.Int(precision).notNull();
		} else if (type == Double.class) {
			return new ColumnType.Double(precision, scale);
		} else if (type == Double.TYPE) {
			return new ColumnType.Double(precision, scale).notNull();
		} else if (type == Float.class) {
			return new ColumnType.Double(precision, scale);
		} else if (type == Float.TYPE) {
			return new ColumnType.Double(precision, scale).notNull();
		} else if (type == Boolean.class) {
			return new ColumnType.Boolean();
		} else if (type == Boolean.TYPE) {
			return new ColumnType.Boolean().notNull();
		} else if (type == Long.class) {
			return new ColumnType.Int(precision > 0 ? precision : 16);
		} else if (type == Long.TYPE) {
			return new ColumnType.Int(precision > 0 ? precision : 16).notNull();
		} else if (type == Character.class) {
			return new ColumnType.Char(1);
		} else if (type == Character.TYPE) {
			return new ColumnType.Char(1).notNull();
		} else if (type == Date.class) {
			ColumnType.TimeStamp ct = new ColumnType.TimeStamp();
			ct.setGenerateType(getDateGenerateType(gv));
			return ct;
		} else if (Enum.class.isAssignableFrom(type)) {
			return new ColumnType.Varchar(32);
		} else if (type.isArray() && type.getComponentType() == Byte.TYPE) {
			return new ColumnType.Blob();
		} else if (type == File.class) {
			return new ColumnType.Blob();
		} else {
			throw new IllegalArgumentException("Java type " + type.getName() + " can't mapping to a Db column type by default");
		}
	}

	private static int getDateGenerateType(GeneratedValue gv) {
		String generated = gv == null ? null : gv.generator().toLowerCase();
		if (generated != null) {
			if ("created".equals(generated)) {
				return 1;
			} else if ("modified".equals(generated)) {
				return 2;
			} else if ("created-sys".equals(generated)) {
				return 3;
			} else if ("modified-sys".equals(generated)) {
				return 4;
			} else if (generated.length() == 0) {
				return 1;
			}
			throw new IllegalArgumentException("Unknown date generator [" + generated + "]");
		}
		return 0;
	}

	/**
	 * 逆向查找元模型
	 */
	public static ITableMetadata lookup(String schema, String table) {
		String key = (schema + "." + table).toUpperCase();
		ITableMetadata m = inverseMapping.get(key);
		if (m != null)
			return m;

		// Schema还原
		if (schema != null) {
			schema = schema.toUpperCase();
			for (Entry<String, String> e : SCHEMA_MAPPING.entrySet()) {
				if (e.getValue().equals(schema)) {
					schema = e.getKey();
					break;
				}
			}
		}

		// Lookup static models
		for (ITableMetadata meta : pool.values()) {
			String tablename = meta.getTableName(false);
			if (schema != null && (!StringUtils.equals(meta.getSchema(), schema))) {// schema不同则跳
				continue;
			}
			if (tablename.equalsIgnoreCase(table)) {
				m = meta;
				break;
			}
		}
		if (m == null) {
			// Lookup dynamic models
			for (ITableMetadata meta : dynPool.values()) {
				String tablename = meta.getTableName(false);
				if (schema != null && (!StringUtils.equals(meta.getSchema(), schema))) {// schema不同则跳
					continue;
				}
				if (tablename.equalsIgnoreCase(table)) {
					m = meta;
					break;
				}
			}
		}
		inverseMapping.put(key, m);
		return m;
	}

	public static void clear() {
		pool.clear();
		dynPool.clear();
		inverseMapping.clear();
	}
}
