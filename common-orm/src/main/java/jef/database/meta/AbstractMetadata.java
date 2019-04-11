package jef.database.meta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.common.Pair;
import jef.common.SimpleException;
import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.cache.KeyDimension;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.ColumnMapping;
import jef.database.dialect.type.VersionSupportColumn;
import jef.database.meta.def.IndexDef;
import jef.database.meta.def.UniqueConstraintDef;
import jef.database.query.DbTable;
import jef.database.query.JpqlExpression;
import jef.database.query.PKQuery;
import jef.database.wrapper.clause.BindSql;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.reflect.ConvertUtils;
import jef.tools.reflect.Property;

/**
 * 抽象类用于简化Tablemeta的实现
 * 
 * @author jiyi
 * 
 */
public abstract class AbstractMetadata implements ITableMetadata {
	/**
	 * schema of the table. (it is always the username in Oracle)
	 */
	protected String schema;
	/**
	 * name of the table.
	 */
	protected String tableName;
	/**
	 * Always operate the table in the named datasource.
	 */
	protected String bindDsName;
	/**
	 * Metadata of the columns autoincrement.
	 */
	private AutoIncrementMapping[] increMappings;
	/**
	 * Metadata of the data/time columns auto-update.
	 */
	private VersionSupportColumn[] autoUpdateColumns;

	/**
	 * the columnOfVersionColumn
	 */
	private VersionSupportColumn versionColumn;
	/**
	 * The fields mapping to columns.
	 */
	protected List<ColumnMapping> metaFields;
	/**
	 * The field of LOB
	 */
	protected Field[] lobNames;

	/**
	 * 记录对应表的所有索引，当建表时使用可自动创建索引
	 * Revised 2016-8 JPA 2.1规范中增加的@Table的indexes属性和Index注解，因此删除EF原先自己设计的Index注解，改用标准的JPA注解
	 */
	final List<IndexDef> indexes = new ArrayList<IndexDef>(5);
	
	/**
	 * 记录对应表所有Unqie约束.当建表时可自动创建约束
	 */
	final List<UniqueConstraintDef> uniques=new ArrayList<UniqueConstraintDef>(5);
	
	protected final Map<Field, ColumnMapping> schemaMap = new IdentityHashMap<Field, ColumnMapping>();
	protected Map<String, Field> fields = new HashMap<String, Field>(10, 0.6f);
	protected Map<String, Field> lowerFields = new HashMap<String, Field>(10, 0.6f);

	// /////////引用索引/////////////////
	protected final Map<String, AbstractRefField> refFieldsByName = new HashMap<String, AbstractRefField>();// 记录所有关联和引用字段referenceFields
	protected final Map<Reference, List<AbstractRefField>> refFieldsByRef = new HashMap<Reference, List<AbstractRefField>>();// 记录所有的引用字段，按引用关系
	protected boolean cacheable;
	protected boolean useOuterJoin = true;

	/**
	 * 列排序器，用这个排序器确保列的输出顺序如下 1、LOB字段总是排在最后。(由于一些数据库驱动问题，这样做能提高操作的成功率，对性能也有少量帮助)
	 * 2、按类定义中的枚举顺序排序，如果是TupleField，则直接按照字段名的ASCII顺序排列。
	 */
	private static final Comparator<ColumnMapping> COLUMN_COMPARATOR = new Comparator<ColumnMapping>() {
		public int compare(ColumnMapping field1, ColumnMapping field2) {
			Class<? extends ColumnType> type1 = field1.get().getClass();
			Class<? extends ColumnType> type2 = field2.get().getClass();
			Boolean b1 = (type1 == ColumnType.Blob.class || type1 == ColumnType.Clob.class);
			Boolean b2 = (type2 == ColumnType.Blob.class || type2 == ColumnType.Clob.class);
			int result = b1.compareTo(b2);
			if (result == 0) {
				Field f1 = field1.field();
				Field f2 = field2.field();
				// 在分库分表的情况下，IdentityHashMap.value的内容每一次都不一致，导致建的表的字段顺序也不一致。在select
				// * 并使用union时会出错
				// Advice by Nihf
				if (f1 instanceof Enum<?> && f2 instanceof Enum<?>) {
					return Integer.compare(((Enum<?>) f1).ordinal(), ((Enum<?>) f2).ordinal());
				} else {
					return f1.name().compareTo(f2.name());
				}
			}
			return result;
		}
	};

	public Field[] getLobFieldNames() {
		return lobNames;
	}

	public String getBindDsName() {
		return bindDsName;
	}

	public ColumnMapping getColumnDef(Field field) {
		// 2014-10-31
		// 在重构用columnMapping代替的设计过程中，会出现两类对象的混用，引起此处作一个判断拦截field(暂时还不需要)

		// if(field instanceof ColumnMapping)
		// return (ColumnMapping)field;
		return schemaMap.get(field);
	}

	public void setBindDsName(String bindDsName) {
		this.bindDsName = MetaHolder.getMappingSite(bindDsName);
		this.cachedTable=null;
	}

	public Collection<ColumnMapping> getColumns() {
		if (metaFields == null) {
			Collection<ColumnMapping> map = this.getColumnSchema();
			ColumnMapping[] fields = map.toArray(new ColumnMapping[map.size()]);
			Arrays.sort(fields, COLUMN_COMPARATOR);
			metaFields = Arrays.asList(fields);
		}
		return metaFields;
	}

	public String getSchema() {
		return schema;
	}

	/**
	 * 返回表名
	 * 
	 * @param withSchema
	 *            true要求带schema
	 * @return
	 */
	public String getTableName(boolean withSchema) {
		if (withSchema && schema != null)
			return new StringBuilder(schema.length() + tableName.length() + 1).append(schema).append('.').append(tableName).toString();
		return tableName;
	}

	public String getColumnName(Field fld, DatabaseDialect profile, boolean escape) {
		ColumnMapping mType = this.schemaMap.get(fld);
		if (mType != null) {
			return mType.getColumnName(profile, escape);
		}
		// 意外情况
		if (fld instanceof JpqlExpression) {
			throw new UnsupportedOperationException();
		}
		String name = profile.getObjectNameToUse(fld.name());
		return escape ? DbUtils.escapeColumn(profile, name) : name;
	}

	private volatile Pair<DatabaseDialect, DbTable> cachedTable;
	protected KeyDimension pkDim;

	public DbTable getBaseTable(DatabaseDialect profile) {
		Pair<DatabaseDialect, DbTable> cachedTable = this.cachedTable;
		if (cachedTable == null) {
			return initCache(profile);
		}
		if (cachedTable.first != profile) {
			return initCache(profile);
		}
		return cachedTable.second;
	}

	private synchronized DbTable initCache(DatabaseDialect profile) {
		DbTable baseTable = new DbTable(bindDsName, profile.getObjectNameToUse(getTableName(true)), false, false);
		Pair<DatabaseDialect, DbTable> cache = new Pair<DatabaseDialect, DbTable>(profile, baseTable);
		this.cachedTable = cache;
		return baseTable;
	}

	public KeyDimension getPKDimension(DatabaseDialect profile) {
		if (pkDim == null) {
			List<Serializable> pks = new ArrayList<Serializable>();
			for (ColumnMapping mapping : this.getPKFields()) {
				pks.add((Serializable) ConvertUtils.defaultValueForBasicType(mapping.getFieldType()));
			}
			PKQuery<?> query = new PKQuery<IQueryableEntity>(this, pks, newInstance());
			BindSql sql = query.toPrepareWhereSql(null, profile);
			KeyDimension dim = KeyDimension.forSingleTable(tableName, sql.getSql(), null, profile);
			pkDim = dim;
		}
		return pkDim;
	}

	public ColumnMapping findField(String left) {
		if (left == null)
			return null;
		Field field = lowerFields.get(left.toLowerCase());
		if (field != null) {
			return getColumnDef(field);
		}
		return null;
	}

	public Field getField(String name) {
		return fields.get(name);
	}

	public Set<String> getAllFieldNames() {
		return fields.keySet();
	}

	public ColumnType getColumnType(String fieldName) {
		Field field = fields.get(fieldName);
		if (field == null) {
			LogUtil.warn(jef.tools.StringUtils.concat("The field [", fieldName, "] does not find in ", this.getThisType().getName()));
			return null;
		}
		return schemaMap.get(field).get();
	}

	public AutoIncrementMapping getFirstAutoincrementDef() {
		AutoIncrementMapping[] array = increMappings;
		if (array != null && array.length > 0) {
			return array[0];
		} else {
			return null;
		}
	}

	public AutoIncrementMapping[] getAutoincrementDef() {
		if (increMappings == null) {
			return new AutoIncrementMapping[0];
		} else {
			return increMappings;
		}
	}

	public VersionSupportColumn[] getAutoUpdateColumnDef() {
		return autoUpdateColumns;
	}

	protected void removeAutoIncAndAutoUpdatingField(Field oldField) {
		if (increMappings != null) {
			for (AutoIncrementMapping m : increMappings) {
				if (m.field() == oldField) {
					increMappings = (AutoIncrementMapping[]) ArrayUtils.removeElement(increMappings, m);
					break;
				}
			}
		}
		if (autoUpdateColumns != null) {
			for (VersionSupportColumn m : autoUpdateColumns) {
				if (m.field() == oldField) {
					autoUpdateColumns = (VersionSupportColumn[]) ArrayUtils.removeElement(autoUpdateColumns, m);
					break;
				}
			}
		}
	}

	protected void updateAutoIncrementAndUpdate(ColumnMapping mType) {
		if (mType instanceof VersionSupportColumn) {
			VersionSupportColumn m = (VersionSupportColumn) mType;
			if (m.isUpdateAlways()) {
				autoUpdateColumns = ArrayUtils.addElement(autoUpdateColumns, m, VersionSupportColumn.class);
			}
			if (m.isVersion()) {
				if(this.versionColumn!=null){
					throw new IllegalArgumentException("There can be only one version column in a entity, but" + this.getName()+" has more.");
				}
				this.versionColumn = m;
			}
		}
		if (mType instanceof AutoIncrementMapping) {
			increMappings = ArrayUtils.addElement(increMappings, (AutoIncrementMapping) mType);
		}
	}

	private void addRefField(AbstractRefField f) {
		List<AbstractRefField> list = refFieldsByRef.get(f.getReference());
		if (list == null) {
			list = new ArrayList<AbstractRefField>();
			refFieldsByRef.put(f.getReference(), list);
		}
		list.add(f);
		refFieldsByName.put(f.getName(), f);
	}

	public Map<Reference, List<AbstractRefField>> getRefFieldsByRef() {
		return refFieldsByRef;
	}

	public Map<String, AbstractRefField> getRefFieldsByName() {
		return refFieldsByName;
	}

	public ExtensionConfig getExtension(IQueryableEntity d) {
		throw new UnsupportedOperationException();
	}

	public ExtensionConfig getExtension(String key) {
		throw new UnsupportedOperationException();
	}

	protected Collection<ColumnMapping> getColumnSchema() {
		return this.schemaMap.values();
	}

	protected ReferenceObject innerAdd(Property pp, ITableMetadata target, CascadeConfig config) {
		Reference r = new Reference(target, config.getRefType(), this);
		if (config.path != null) {
			try {
				r.setHint(config.path);
			} catch (Exception e) {
				throw new SimpleException(e);
			}
		}
		ReferenceObject ref = new ReferenceObject(pp, r, config);
		if (pp.getType() == Object.class) {
			Class<?> targetContainer = target.getThisType();
			if (!config.getRefType().isToOne()) {
				targetContainer = Collection.class;
			}
			ref.setSourceFieldType(targetContainer);
		}

		addRefField(ref);
		return ref;
	}

	protected ReferenceField innerAdd(Property pp, ColumnMapping targetFld, CascadeConfig config) {
		Assert.notNull(targetFld);
		Reference r = new Reference(targetFld.getMeta(), config.getRefType(), this);
		if (config.path != null) {
			r.setHint(config.path);
		}
		ReferenceField f = new ReferenceField(pp, r, targetFld, config);
		if (pp.getType() == Object.class) {
			Class<?> containerType = targetFld.getFieldType();
			if (!config.getRefType().isToOne()) {
				containerType = Collections.class;
			}
			f.setSourceFieldType(containerType);
		}
		addRefField(f);
		return f;
	}

	/*
	 * 添加一个引用字段，引用实体表的DO对象
	 * 
	 * @param fieldName 字段名称
	 * 
	 * @param target 实体表对应的类
	 * 
	 * @param path 用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	protected ReferenceObject addCascadeField(String fieldName, ITableMetadata target, CascadeConfig config) {
		Property pp = getContainerAccessor().getProperty(fieldName);
		if (pp == null) {
			throw new IllegalArgumentException(fieldName + " is not exist in " + this.getName());
		}
		return innerAdd(pp, target, config);
	}

	/*
	 * 添加一个引用字段，引用实体表的某个字段
	 * 
	 * @param fieldName 字段名称
	 * 
	 * @param target 实体表被引用字段
	 * 
	 * @param path 用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	protected ReferenceField addCascadeField(String fieldName, Field target, CascadeConfig config) {
		Assert.notNull(target);
		Property pp = getContainerAccessor().getProperty(fieldName);
		if (pp == null) {
			throw new IllegalArgumentException(fieldName + " is not exist in " + this.getName());
		}
		ColumnMapping targetFld = DbUtils.toColumnMapping(target);
		return innerAdd(pp, targetFld, config);
	}

	public boolean isCacheable() {
		return cacheable;
	}

	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}

	public boolean isUseOuterJoin() {
		return useOuterJoin;
	}

	public void setUseOuterJoin(boolean useOuterJoin) {
		this.useOuterJoin = useOuterJoin;
	}
	
	public List<UniqueConstraintDef> getUniques() {
		return uniques;
	}

	public VersionSupportColumn getVersionColumn() {
		return versionColumn;
	}
}
