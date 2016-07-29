package jef.database.meta;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.common.Entry;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.PojoWrapper;
import jef.database.VarObject;
import jef.database.annotation.Index;
import jef.database.annotation.JoinType;
import jef.database.annotation.PartitionFunction;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;
import jef.database.dialect.ColumnType;
import jef.database.dialect.type.ColumnMapping;
import jef.database.dialect.type.ColumnMappings;
import jef.database.meta.extension.EfPropertiesExtensionProvider;
import jef.database.meta.extension.ExtensionAccessor;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanAccessorMapImpl;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.Property;

import com.google.common.collect.Multimap;

public class DynamicMetadata extends AbstractMetadata {

	private Class<? extends IQueryableEntity> type = VarObject.class;

	protected Map<String, Field> lowerColumnToFieldName = new HashMap<String, Field>(10, 0.6f);

	private List<ColumnMapping> pkFields = new ArrayList<ColumnMapping>();

	private final Set<TupleModificationListener> listeners = new HashSet<TupleModificationListener>();

	private BeanAccessor containerAccessor = BeanAccessorMapImpl.INSTANCE;
	
	protected final List<ColumnMapping> orderdColumns = new ArrayList<ColumnMapping>();

	/**
	 * 创建当前元数据的对象实例。 由于2.0版开始，TupleMetadata的数据容器类型不再仅有VarObject一种，
	 * 因此newInstance返回的不是VarObject类型。 2.0之前的代码需要改为使用{@link #newVar()}。
	 * 
	 * @since 2.0
	 */
	public IQueryableEntity newInstance() {
		if (type == VarObject.class) {
			return new VarObject(this);
		}
		return (IQueryableEntity) containerAccessor.newInstance();
	}

	@Override
	public String toString() {
		return tableName;
	}

	public String getName() {
		return getTableName(false);
	}

	public String getSimpleName() {
		return getTableName(false);
	}

	public Set<String> getAllColumnNames() {
		return lowerColumnToFieldName.keySet();
	}

	/*
	 * 特殊处理，由于半动态表在实例化过程中，通过新建的TupleField代替了原来在元模型中定义的field，Field不再是单例对象，因此只能通过名称去匹配
	 * 。
	 */
	public ColumnMapping getColumnDef(Field field) {
		return schemaMap.get(getField(field.name()));
	}

	/**
	 * 构造，半动态模型
	 * 
	 * @param parent
	 * @param extension
	 */
	public DynamicMetadata(AbstractMetadata parent, ExtensionConfig extension) {
		// System.err.println("初始化动态实体模板:"+parent.getName()+" - "+extension.getName());
		this.type = parent.getThisType().asSubclass(IQueryableEntity.class);
		BeanAccessor raw = FastBeanWrapperImpl.getAccessorFor(type);
		this.containerAccessor = new ExtensionAccessor(raw, extension.getName(), EfPropertiesExtensionProvider.getInstance());
		this.tableName = extension.getName();
		this.schema = parent.getSchema();
		setBindDsName(parent.getBindDsName());
		for (ColumnMapping m : parent.getColumnSchema()) {
			this.updateColumn(m.fieldName(), m.rawColumnName(), m.get(), m.isPk(), false);
		}
		this.refFieldsByName.putAll(parent.getRefFieldsByName());
		this.refFieldsByRef.putAll(parent.getRefFieldsByRef());
		this.indexMap.addAll(parent.getIndexDefinition());

	}

	public Class<?> getThisType() {
		return type;
	}

	public Class<? extends IQueryableEntity> getContainerType() {
		return type;
	}

	/**
	 * 快速获得动态模型定义的field对象
	 * 
	 * @param fieldname
	 * @return
	 */
	public Field f(String fieldname) {
		Field field = fields.get(fieldname);
		if (field == null)
			throw new IllegalArgumentException("There is no field '" + fieldname + "' in table " + this.tableName);
		return field;
	}

	public List<Field> getPKField() {
		if (pkFields == null)
			return Collections.emptyList();
		return new AbstractList<Field>() {
			@Override
			public Field get(int index) {
				return pkFields.get(index).field();
			}

			@Override
			public int size() {
				return pkFields.size();
			}
		};
	}

	/**
	 * 定义一个列。
	 * <p>
	 * 这个方法适用于java字段名和数据库列名相同的场景。如果java字段名和列名不一致，请使用
	 * {@link #addColumn(String, String, ColumnType, boolean)}
	 * 
	 * @param columnName
	 *            列名(字段名)
	 * @param type
	 *            数据类型
	 * 
	 * 
	 */
	public void addColumn(String columnName, ColumnType type) {
		boolean pk = (type instanceof ColumnType.AutoIncrement) || (type instanceof ColumnType.GUID);
		updateColumn(columnName, columnName, type, pk, false);
	}

	/**
	 * 定义一个列
	 * 
	 * @param fieldName
	 *            字段名
	 * @param columnName
	 *            列名
	 * @param type
	 *            数据类型
	 * @param isPk
	 *            是否主键
	 */
	public void addColumn(String fieldName, String columnName, ColumnType type, boolean isPk) {
		updateColumn(fieldName, columnName, type, isPk, false);
	}

	/**
	 * 更新或添加一个列
	 * 
	 * @param fieldName
	 *            字段名
	 * @param columnName
	 *            列名
	 * @param type
	 *            数据类型
	 * @param isPk
	 *            是否主键
	 * @return 返回 true 更新列 ， false 新增列
	 */
	public boolean updateColumn(String fieldName, String columnName, ColumnType type, boolean isPk) {
		return updateColumn(fieldName, columnName, type, isPk, true);
	}

	protected boolean internalUpdateColumn(Field field, String columnName, ColumnType type, boolean isPk, boolean replace) {
		if (isPk) {
			type.setNullable(false);
		}
		Field oldField = fields.get(field.name());
		if (oldField != null) {
			if (!replace) {
				throw new IllegalArgumentException("The field " + field + " already exist.");
			}
			// 替换的场合，先清除旧的缓存记录
			internalRemoveField(oldField);
		} else {
			replace = false;// 新建的场合
			oldField = field;
		}
		ColumnMapping mType = ColumnMappings.getMapping(oldField, this, columnName, type, isPk);
		updateAutoIncrementAndUpdate(mType);

		String fieldName = field.name();
		schemaMap.put(oldField, mType);
		orderdColumns.add(mType);
		
		fields.put(fieldName, oldField);
		lowerFields.put(fieldName.toLowerCase(), oldField);
		lowerColumnToFieldName.put(columnName.toLowerCase(), oldField);
		if (isPk)
			pkFields.add(mType);
		if (mType.isLob()) {
			lobNames = jef.tools.ArrayUtils.addElement(lobNames, oldField, jef.database.Field.class);
		}
		super.metaFields = null;// 清缓存
		super.pkDim = null;
		for (TupleModificationListener listener : listeners) {
			listener.onUpdate(this, field);
		}
		return replace;
	}

	private void internalRemoveField(Field field) {
		// fields
		ColumnMapping mType = schemaMap.remove(field);
		orderdColumns.remove(mType);
		if (mType != null) {
			// columnToField
			lowerColumnToFieldName.remove(mType.lowerColumnName());
			pkFields.remove(mType);
			lowerFields.remove(field.name().toLowerCase());
		}
		// increMappings
		removeAutoIncAndAutoUpdatingField(field);
		if (lobNames != null) {
			lobNames = (Field[]) ArrayUtils.removeElement(lobNames, field);
			if (lobNames.length == 0)
				lobNames = null;
		}
		super.metaFields = null;// 清缓存
		super.pkDim = null;

	}

	protected boolean updateColumn(String fieldName, String columnName, ColumnType type, boolean isPk, boolean replace) {
		fieldName = StringUtils.trimToNull(fieldName);
		Assert.notNull(fieldName);
		Field field = fields.get(fieldName);
		if (field == null) {
			field = new TupleField(this, fieldName);
		}
		return internalUpdateColumn(field, columnName, type, isPk, replace);
	}

	/**
	 * 删除指定的列
	 * 
	 * @param fieldName
	 *            当列名\字段名 不同时，这个方法按照字段名删除
	 * @return false如果没找到此列
	 */
	public boolean removeColumnByFieldName(String fieldName) {
		Field field = fields.remove(fieldName);
		if (field == null)
			return false;
		internalRemoveField(field);
		for (TupleModificationListener listener : listeners) {
			listener.onDelete(this, field);
		}
		return true;
	}

	public Field getFieldByLowerColumn(String fieldname) {
		return lowerColumnToFieldName.get(fieldname);
	}

	/*
	 * 添加多对多引用字段
	 */
	public void addCascadeManyToMany(String fieldName, Field targetField, JoinKey... path) {
		CascadeConfig config = new CascadeConfig(null, (ManyToMany) null);
		if (path.length > 0) {
			config.path = new JoinPath(JoinType.INNER, path);
		}
		ColumnMapping targetFld = DbUtils.toColumnMapping(targetField);
		Property pp = containerAccessor.getProperty(fieldName);
		innerAdd(pp, targetFld, config);
	}

	/*
	 * 添加多对多引用字段
	 */
	public void addCascadeManyToMany(String fieldName, ITableMetadata targetClass, JoinKey... path) {
		CascadeConfig config = new CascadeConfig(null, (ManyToMany) null);
		if (path.length > 0) {
			config.path = new JoinPath(JoinType.INNER, path);
		}
		Property pp = containerAccessor.getProperty(fieldName);
		innerAdd(pp, targetClass, config);
	}

	/**
	 * 添加一个1对1引用字段，引用实体表的DO对象
	 * 
	 * @param fieldName
	 *            字段名称
	 * 
	 * @param target
	 *            实体表对应的类
	 * 
	 * @param path
	 *            用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addCascadeOneToOne(String fieldName, ITableMetadata target, JoinPath path) {
		CascadeConfig config = new CascadeConfig(null, (OneToOne) null);
		config.path = path;
		Property pp = containerAccessor.getProperty(fieldName);
		innerAdd(pp, target, config);
	}

	/**
	 * 添加一个1对1引用字段，引用实体表的某个字段
	 * 
	 * @param fieldName
	 *            字段名称
	 * 
	 * @param target
	 *            实体表被引用字段
	 * 
	 * @param path
	 *            用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addCascadeOneToOne(String fieldName, Field target, JoinPath path) {
		CascadeConfig config = new CascadeConfig(null, (OneToOne) null);
		config.path = path;
		ColumnMapping targetFld = DbUtils.toColumnMapping(target);
		Property pp = containerAccessor.getProperty(fieldName);
		innerAdd(pp, targetFld, config);
	}

	/**
	 * 添加一个1对多引用字段，引用实体表的DO对象
	 * 
	 * @param fieldName
	 *            字段名称
	 * 
	 * @param target
	 *            实体表的对应DO对象
	 * 
	 * @param path
	 *            用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addCascadeOneToMany(String fieldName, ITableMetadata target, JoinKey... path) {
		CascadeConfig config = new CascadeConfig(null, (OneToMany) null);
		if (path.length > 0) {
			config.path = new JoinPath(JoinType.INNER, path);
		}
		Property pp = containerAccessor.getProperty(fieldName);
		innerAdd(pp, target, config);
	}

	/**
	 * 添加一个1对多引用字段，引用实体表的某个字段
	 * 
	 * @param fieldName
	 *            字段名称
	 * 
	 * @param target
	 *            实体表被引用字段
	 * 
	 * @param path
	 *            用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addCascadeOneToMany(String fieldName, Field target, JoinKey... path) {
		CascadeConfig config = new CascadeConfig(null, (OneToMany) null);
		if (path.length > 0) {
			config.path = new JoinPath(JoinType.INNER, path);
		}
		ColumnMapping targetFld = DbUtils.toColumnMapping(target);
		Property pp = containerAccessor.getProperty(fieldName);
		innerAdd(pp, targetFld, config);
	}

	/**
	 * 添加一个多对一引用字段，引用实体表的DO对象
	 * 
	 * @param fieldName
	 *            字段名称
	 * 
	 * @param target
	 *            实体表被引用字段
	 * 
	 * @param path
	 *            用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addCascadeManyToOne(String fieldName, ITableMetadata target, JoinPath path) {
		CascadeConfig config = new CascadeConfig(null, (ManyToOne) null);
		config.path = path;
		Property pp = containerAccessor.getProperty(fieldName);
		innerAdd(pp, target, config);
	}

	/**
	 * 添加一个多对一引用字段，引用实体表的一个字段
	 * 
	 * @param fieldName
	 *            字段名称
	 * 
	 * @param target
	 *            实体表被引用字段
	 * 
	 * @param path
	 *            用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addCascadeManyToOne(String fieldName, Field target, JoinPath path) {
		CascadeConfig config = new CascadeConfig(null, (ManyToOne) null);
		config.path = path;
		ColumnMapping targetFld = DbUtils.toColumnMapping(target);
		Property pp = containerAccessor.getProperty(fieldName);
		innerAdd(pp, targetFld, config);
	}

	/**
	 * 设置索引
	 * 
	 * @param fields
	 *            这些字段将被加入到一个复合索引中
	 * @param comment
	 *            索引修饰,如"unique"，"bitmap"。无修饰传入null或""均可。
	 */
	public void addIndex(String[] fields, String comment) {
		Map<String, Object> data = new HashMap<String, Object>(4);
		data.put("fields", fields);
		data.put("definition", StringUtils.toString(comment));
		data.put("name", "");
		jef.database.annotation.Index index = BeanUtils.asAnnotation(jef.database.annotation.Index.class, data);
		indexMap.add(index);
	}

	/**
	 * 设置索引
	 * 
	 * @param fields
	 *            索引字段名
	 * @param comment
	 *            索引修饰,如"unique"，"bitmap"。无修饰传入null或""均可。
	 */
	public void addIndex(String fieldName, String comment) {
		addIndex(new String[] { fieldName }, comment);
	}

	public boolean isAssignableFrom(ITableMetadata type) {
		return type == this;
	}

	public List<Index> getIndexDefinition() {
		return indexMap;
	}

	public PartitionTable getPartition() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	public Entry<PartitionKey, PartitionFunction>[] getEffectPartitionKeys() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	public Multimap<String, PartitionFunction> getMinUnitFuncForEachPartitionKey() {
		return null;
	}

	public PojoWrapper transfer(Object p, boolean isQuery) {
		throw new UnsupportedOperationException();
	}

	public EntityType getType() {
		return EntityType.TUPLE;
	}

	public boolean containsMeta(ITableMetadata meta) {
		return meta == this;
	}

	@Override
	public List<ColumnMapping> getPKFields() {
		if (pkFields == null)
			return Collections.emptyList();
		return pkFields;
	}

	public void addListener(TupleModificationListener adapter) {
		this.listeners.add(adapter);
	}

	@Override
	public BeanAccessor getContainerAccessor() {
		return containerAccessor;
	}

	/**
	 * 给子类用的构造
	 * 
	 * @param tableName
	 */
	protected DynamicMetadata(String tableName) {
		tableName = tableName.trim();
		String schema = null;
		int index = tableName.indexOf('.');
		if (index > -1) {
			schema = tableName.substring(0, index);
			tableName = tableName.substring(index + 1);
		}
		this.schema = schema;
		this.tableName = tableName;
	}

	/**
	 * 给子类用的构造
	 * 
	 * @param schema
	 * @param tableName
	 */
	protected DynamicMetadata(String schema, String tableName) {
		if (StringUtils.isBlank(tableName)) {
			throw new IllegalArgumentException("Invalid table name " + tableName);
		}
		this.tableName = tableName.trim();
		this.schema = StringUtils.trimToNull(schema);
	}

	@Override
	public TupleMetadata getExtendsTable() {
		return null;
	}

	@Override
	public Collection<ColumnMapping> getExtendedColumns() {
		return getColumnSchema();
	}

	@Override
	public ColumnMapping getExtendedColumnDef(String field) {
		return schemaMap.get(getField(field));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String,String> getColumnComments() {
		return Collections.EMPTY_MAP;
	}
}
