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
import jef.database.dialect.type.AbstractTimeMapping;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.ColumnMapping;
import jef.database.dialect.type.ColumnMappings;
import jef.database.query.Query;
import jef.database.query.ReferenceType;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanUtils;

import com.google.common.collect.Multimap;

/**
 * 支持动态表。
 * 
 * @author jiyi
 * 
 */
public class TupleMetadata extends MetadataAdapter {

	private Map<String, Field> fields = new HashMap<String, Field>(10, 0.6f);
	private Map<String, Field> fieldsLower = new HashMap<String, Field>(10, 0.6f);
	private Map<String, Field> columnToField = new HashMap<String, Field>(10, 0.6f);
	private List<ColumnMapping<?>> pk = new ArrayList<ColumnMapping<?>>();
	private AutoIncrementMapping<?>[] increMappings;
	private AbstractTimeMapping<?>[] updateTimeMapping;
	// /////////引用索引/////////////////
	private final Map<String, AbstractRefField> refFieldsByName = new HashMap<String, AbstractRefField>();// 记录所有关联和引用字段referenceFields
	private final Map<Reference, List<AbstractRefField>> refFieldsByRef = new HashMap<Reference, List<AbstractRefField>>();// 记录所有的引用字段，按引用关系
	private final Set<TupleModificationListener> listeners = new HashSet<TupleModificationListener>();

	public VarObject newInstance() {
		return new VarObject(this);
	}

	public VarObject instance() {
		return new VarObject(this, false);
	}

	public String getName() {
		return getTableName(false);
	}

	public String getSimpleName() {
		return getTableName(false);
	}

	public Set<String> getAllFieldNames() {
		return fields.keySet();
	}

	public Set<String> getAllColumnNames() {
		return columnToField.keySet();
	}

	/**
	 * 构造
	 * 
	 * @param tableName
	 *            表名
	 */
	public TupleMetadata(String tableName) {
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
	 * 构造
	 * 
	 * @param schema
	 *            schema名
	 * @param tableName
	 *            表名
	 */
	public TupleMetadata(String schema, String tableName) {
		if (StringUtils.isBlank(tableName)) {
			throw new IllegalArgumentException("Invalid table name " + tableName);
		}
		this.tableName = tableName.trim();
		this.schema = StringUtils.trimToNull(schema);
	}

	public Class<?> getThisType() {
		return VarObject.class;
	}

	public Class<? extends IQueryableEntity> getContainerType() {
		return VarObject.class;
	}

	public Field f(String fieldname) {
		Field field = fields.get(fieldname);
		if (field == null)
			throw new IllegalArgumentException("There is no field '" + fieldname + "' in table " + this.tableName);
		return field;
	}

	public Field getField(String fieldname) {
		return fields.get(fieldname);
	}

	protected Collection<ColumnMapping<?>> getColumnSchema() {
		return schemaMap.values();
	}

	public ColumnType getColumnType(String name) {
		Field field = fields.get(name);
		if (field != null) {
			return schemaMap.get(field).get();
		}
		return null;
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

	public Field findField(String left) {
		if (left == null)
			return null;
		return fieldsLower.get(left.toLowerCase());
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
		if (mType instanceof AutoIncrementMapping<?>) {
			increMappings = ArrayUtils.addElement(increMappings, (AutoIncrementMapping<?>) mType);
		}
		if (mType instanceof AbstractTimeMapping<?>) {
			AbstractTimeMapping<?> tm = (AbstractTimeMapping<?>) mType;
			if (tm.isForUpdate()) {
				updateTimeMapping = ArrayUtils.addElement(updateTimeMapping, tm);
			}
		}

		String fieldName = field.name();
		schemaMap.put(oldField, mType);
		fields.put(fieldName, oldField);
		fieldsLower.put(fieldName.toLowerCase(), oldField);
		columnToField.put(columnName.toLowerCase(), oldField);
		if (isPk)
			pk.add(mType);
		if (mType.isLob()) {
			lobNames = jef.tools.ArrayUtils.addElement(lobNames, oldField, jef.database.Field.class);
		}
		super.metaFields = null;// 清缓存
		super.pkDim=null;
		for(TupleModificationListener listener:listeners){
			listener.onUpdate(this,field);
		}
		return replace;
	}

	private void internalRemoveField(Field field) {
		// fields
		ColumnMapping<?> mType = schemaMap.remove(field);
		if (mType != null) {
			// columnToField
			columnToField.remove(mType.lowerColumnName());
			pk.remove(mType);
			fieldsLower.remove(field.name().toLowerCase());
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

	private void removeAutoIncAndTimeUpdatingField(Field oldField) {
		if (increMappings != null) {
			for (AutoIncrementMapping<?> m : increMappings) {
				if (m.field() == oldField) {
					increMappings=(AutoIncrementMapping[])ArrayUtils.removeElement(increMappings, m);
					break;
				}
			}
		}
		if(updateTimeMapping!=null){
			for (AbstractTimeMapping<?> m : updateTimeMapping) {
				if (m.field() == oldField) {
					updateTimeMapping=(AbstractTimeMapping[])ArrayUtils.removeElement(updateTimeMapping, m);
					break;
				}
			}
		}
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
		Field field = columnToField.get(columnName.toLowerCase());
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
		for(TupleModificationListener listener:listeners){
			listener.onDelete(this,field);
		}
		return true;
	}

	public Field getFieldByLowerColumn(String fieldname) {
		return columnToField.get(fieldname);
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

	private void addRefField(AbstractRefField f) {
		List<AbstractRefField> list = refFieldsByRef.get(f.getReference());
		if (list == null) {
			list = new ArrayList<AbstractRefField>();
			refFieldsByRef.put(f.getReference(), list);
		}
		list.add(f);
		refFieldsByName.put(f.getSourceField(), f);
	}

	public Map<Reference, List<AbstractRefField>> getRefFieldsByRef() {
		return refFieldsByRef;
	}

	public Map<String, AbstractRefField> getRefFieldsByName() {
		return refFieldsByName;
	}

	public Reference findPath(ITableMetadata class1) {
		for (Reference r : this.refFieldsByRef.keySet()) {
			if (r.getTargetType() == class1) {
				return r;
			}
		}
		return null;
	}

	public Reference findDistinctPath(ITableMetadata target) {
		Reference ref = null;
		for (Reference reference : this.refFieldsByRef.keySet()) {
			if (reference.getTargetType() == target) {
				if (ref != null) {
					throw new IllegalArgumentException("There's more than one reference to [" + target.getSimpleName() + "] in type [" + getSimpleName() + "],please assign the reference field name.");
				}
				ref = reference;
			}
		}
		if (ref == null) {
			throw new IllegalArgumentException("Target class " + target.getSimpleName() + "of fileter-condition is not referenced by " + getSimpleName());
		}
		return ref;
	}

	public boolean isAssignableFrom(ITableMetadata type) {
		return type == this;
	}

	public ColumnMapping<?> getColumnDef(Field field) {
		return schemaMap.get(field);
	}

	public List<Index> getIndexSchema() {
		return indexMap;
	}

	public AutoIncrementMapping<?> getFirstAutoincrementDef() {
		AutoIncrementMapping<?>[] array = increMappings;
		if (array != null && array.length > 0) {
			return array[0];
		} else {
			return null;
		}
	}

	public AutoIncrementMapping<?>[] getAutoincrementDef() {
		if (increMappings == null) {
			return new AutoIncrementMapping<?>[0];
		} else {
			return increMappings;
		}
	}

	public AbstractTimeMapping<?>[] getUpdateTimeDef() {
		return updateTimeMapping;
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

	@Override
	public ExtensionConfig getExtensionConfig(Query<?> q) {
		return null;
	}

	public void addListener(TupleModificationListener adapter) {
		this.listeners.add(adapter);
	}
}
