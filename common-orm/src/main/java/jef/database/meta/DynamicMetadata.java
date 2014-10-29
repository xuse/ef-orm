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

import javax.persistence.FetchType;

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
import jef.database.query.ReferenceType;
import jef.database.support.accessor.EfPropertiesExtensionProvider;
import jef.database.support.accessor.ExtensionAccessor;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanAccessorMapImpl;
import jef.tools.reflect.BeanUtils;

import com.google.common.collect.Multimap;

public class DynamicMetadata extends MetadataAdapter {

	private Class<? extends IQueryableEntity> type = VarObject.class;
	private BeanAccessor accessor = BeanAccessorMapImpl.INSTANCE;
	private Map<String, Field> lowerColumnToFieldName = new HashMap<String, Field>(10, 0.6f);
	private List<ColumnMapping<?>> pk = new ArrayList<ColumnMapping<?>>();
	private final Set<TupleModificationListener> listeners = new HashSet<TupleModificationListener>();

	/**
	 * 创建当前元数据的对象实例。
	 * 由于2.0版开始，TupleMetadata的数据容器类型不再仅有VarObject一种，因此newInstance返回的不是VarObject类型。
	 * 2.0之前的代码需要改为使用{@link #newVar()}。
	 * @since 2.0
	 */
	public IQueryableEntity newInstance() {
		if(type==VarObject.class){
			return new VarObject(this);
		}
		return (IQueryableEntity) accessor.newInstance();
	}

	public IQueryableEntity instance() {
		if(type==VarObject.class){
			return new VarObject(this, false);
		}
		return (IQueryableEntity) accessor.newInstance();
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

	public ColumnMapping<?> getColumnDef(Field field) {
		return schemaMap.get(getField(field.name()));
	}

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

	protected DynamicMetadata(String schema, String tableName) {
		if (StringUtils.isBlank(tableName)) {
			throw new IllegalArgumentException("Invalid table name " + tableName);
		}
		this.tableName = tableName.trim();
		this.schema = StringUtils.trimToNull(schema);
	}

	public DynamicMetadata(MetadataAdapter parent, ExtensionConfig extension) {
		System.err.println("初始化动态实体模板:"+parent.getName()+" - "+extension.getName());
		this.type = parent.getThisType().asSubclass(IQueryableEntity.class);
		BeanAccessor raw = FastBeanWrapperImpl.getAccessorFor(type);
		this.accessor = new ExtensionAccessor(raw, extension.getName(), EfPropertiesExtensionProvider.getInstance());
		this.tableName = extension.getName();
		this.schema = parent.getSchema();
		setBindDsName(parent.getBindDsName());
		for (ColumnMapping<?> m : parent.getColumnSchema()) {
//			this.internalUpdateColumn(m.field(), m.rawColumnName(), m.get(), m.isPk(), false);
			this.updateColumn(m.fieldName(), m.rawColumnName(), m.get(), m.isPk(), false);
		}
		this.refFieldsByName.putAll(parent.getRefFieldsByName());
		this.refFieldsByRef.putAll(parent.getRefFieldsByRef());
		this.indexMap.addAll(parent.getIndexSchema());

	}

	public Class<?> getThisType() {
		return type;
	}

	public Class<? extends IQueryableEntity> getContainerType() {
		return type;
	}

	public Field f(String fieldname) {
		Field field = fields.get(fieldname);
		if (field == null)
			throw new IllegalArgumentException("There is no field '" + fieldname + "' in table " + this.tableName);
		return field;
	}

	protected Collection<ColumnMapping<?>> getColumnSchema() {
		return schemaMap.values();
	}

	public List<Field> getPKField() {
		if (pk == null)
			return Collections.emptyList();
		return new AbstractList<Field>() {
			@Override
			public Field get(int index) {
				return pk.get(index).field();
			}

			@Override
			public int size() {
				return pk.size();
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

	public void putJavaField(Field field, ColumnType type) {
		boolean pk = (type instanceof ColumnType.AutoIncrement) || (type instanceof ColumnType.GUID);
		this.internalUpdateColumn(field, field.name(), type, pk, false);

	}

	/**
	 * 更新或添加一个列
	 * 
	 * @param columnName
	 * @param type
	 * @return
	 */
	public boolean updateColumn(String columnName, ColumnType type) {
		boolean pk = (type instanceof ColumnType.AutoIncrement) || (type instanceof ColumnType.GUID);
		return updateColumn(columnName, columnName, type, pk, true);
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

	private boolean internalUpdateColumn(Field field, String columnName, ColumnType type, boolean isPk, boolean replace) {
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

		ColumnMapping<?> mType = ColumnMappings.getMapping(oldField, this, columnName, type, isPk);

		updateAutoIncrementAndUpdate(mType);

		String fieldName = field.name();
		schemaMap.put(oldField, mType);
		fields.put(fieldName, oldField);
		lowerFields.put(fieldName.toLowerCase(), oldField);
		lowerColumnToFieldName.put(columnName.toLowerCase(), oldField);
		if (isPk)
			pk.add(mType);
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
		ColumnMapping<?> mType = schemaMap.remove(field);
		if (mType != null) {
			// columnToField
			lowerColumnToFieldName.remove(mType.lowerColumnName());
			pk.remove(mType);
			lowerFields.remove(field.name().toLowerCase());
		}
		// increMappings
		removeAutoIncAndTimeUpdatingField(field);
		if (lobNames != null) {
			lobNames = (Field[]) ArrayUtils.removeElement(lobNames, field);
			if (lobNames.length == 0)
				lobNames = null;
		}
		super.metaFields = null;// 清缓存
		super.pkDim = null;

	}

	private boolean updateColumn(String fieldName, String columnName, ColumnType type, boolean isPk, boolean replace) {
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
	 * @param columnName
	 * @return false如果没找到此列
	 */
	public boolean removeColumn(String columnName) {
		if (columnName == null)
			return false;
		Field field = lowerColumnToFieldName.get(columnName.toLowerCase());
		if (field != null) {
			removeColumnByFieldName(field.name());
			return true;
		}
		return false;
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
	public void addFieldReference_NvsN(String fieldName, Field targetField, JoinKey... path) {
		if (path.length > 0) {
			innerAdd(fieldName, targetField, ReferenceType.MANY_TO_MANY, new JoinPath(JoinType.INNER, path));
		} else {
			innerAdd(fieldName, targetField, ReferenceType.MANY_TO_MANY, null);
		}
	}

	/*
	 * 添加多对多引用字段
	 */
	public void addReference_NvsN(String fieldName, ITableMetadata targetClass, JoinKey... path) {
		if (path.length > 0) {
			innerAdd(fieldName, targetClass, ReferenceType.MANY_TO_MANY, new JoinPath(JoinType.INNER, path), FetchType.LAZY);
		} else {
			innerAdd(fieldName, targetClass, ReferenceType.MANY_TO_MANY, null, FetchType.LAZY);
		}
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
	public void addReference_1vs1(String fieldName, ITableMetadata target, JoinPath path) {
		innerAdd(fieldName, target, ReferenceType.ONE_TO_ONE, path, FetchType.LAZY);
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
	public void addFieldReference_1vs1(String fieldName, Field target, JoinPath path) {
		innerAdd(fieldName, target, ReferenceType.ONE_TO_ONE, path);
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
	public void addReference_1vsN(String fieldName, ITableMetadata target, JoinKey... path) {
		if (path.length > 0) {
			innerAdd(fieldName, target, ReferenceType.ONE_TO_MANY, new JoinPath(JoinType.INNER, path), FetchType.LAZY);
		} else {
			innerAdd(fieldName, target, ReferenceType.ONE_TO_MANY, null, FetchType.LAZY);
		}

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
	public void addFieldReference_1vsN(String fieldName, Field target, JoinKey... path) {
		if (path.length > 0) {
			innerAdd(fieldName, target, ReferenceType.ONE_TO_MANY, new JoinPath(JoinType.INNER, path));
		} else {
			innerAdd(fieldName, target, ReferenceType.ONE_TO_MANY, null);
		}

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
	public void addReference_Nvs1(String fieldName, ITableMetadata target, JoinPath path) {
		innerAdd(fieldName, target, ReferenceType.MANY_TO_ONE, path, FetchType.LAZY);
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
	public void addFieldReference_Nvs1(String fieldName, Field target, JoinPath path) {
		innerAdd(fieldName, target, ReferenceType.MANY_TO_ONE, path);
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

	private void innerAdd(String fieldName, Field targetField, ReferenceType type, JoinPath path) {
		ITableMetadata target = DbUtils.getTableMeta(targetField);
		Reference r = new Reference(target, type, this);
		if (path != null) {
			r.setHint(path);
		}

		Class<?> containerType = Object.class;
		if (!type.isToOne()) {
			containerType = Collections.class;
		}
		ReferenceField f = new ReferenceField(containerType, fieldName, r, targetField, null);
		addRefField(f);
	}

	private void innerAdd(String fieldName, ITableMetadata target, ReferenceType type, JoinPath path, FetchType fetch) {
		Reference r = new Reference(target, type, this);
		if (path != null) {
			r.setHint(path);
		}

		Class<?> containerType = target.getThisType();
		if (!type.isToOne()) {
			containerType = Collection.class;
		}
		ReferenceObject field = new ReferenceObject(containerType, fieldName, r, null);
		field.setCascade(ALL, fetch);
		addRefField(field);
	}

	public boolean isAssignableFrom(ITableMetadata type) {
		return type == this;
	}

	public List<Index> getIndexSchema() {
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
	public List<ColumnMapping<?>> getPKFields() {
		if (pk == null)
			return Collections.emptyList();
		return pk;
	}

	public void addListener(TupleModificationListener adapter) {
		this.listeners.add(adapter);
	}

	@Override
	public BeanAccessor getBeanAccessor() {
		return accessor;
	}
}
