package jef.database.meta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.common.log.LogUtil;
import jef.database.DbCfg;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.annotation.BindDataSource;
import jef.database.cache.KeyDimension;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AbstractTimeMapping;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.ColumnMapping;
import jef.database.query.DbTable;
import jef.database.query.JpqlExpression;
import jef.database.query.PKQuery;
import jef.database.wrapper.clause.BindSql;
import jef.tools.ArrayUtils;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;

/**
 * 抽象类用于简化Tablemeta的实现
 * 
 * @author jiyi
 * 
 */
public abstract class MetadataAdapter implements ITableMetadata {
	protected String schema;
	protected String tableName;
	private String bindDsName;
	private AutoIncrementMapping<?>[] increMappings;
	private AbstractTimeMapping<?>[] updateTimeMapping;

	protected List<ColumnMapping<?>> metaFields;
	protected Field[] lobNames;

	final List<jef.database.annotation.Index> indexMap = new ArrayList<jef.database.annotation.Index>(5);// 记录对应表的所有索引，当建表时使用可自动创建索引
	protected final Map<Field, ColumnMapping<?>> schemaMap = new IdentityHashMap<Field, ColumnMapping<?>>();
	protected Map<String, Field> fields = new HashMap<String, Field>(10, 0.6f);
	protected Map<String, Field> lowerFields = new HashMap<String, Field>(10, 0.6f);

	// /////////引用索引/////////////////
	protected final Map<String, AbstractRefField> refFieldsByName = new HashMap<String, AbstractRefField>();// 记录所有关联和引用字段referenceFields
	protected final Map<Reference, List<AbstractRefField>> refFieldsByRef = new HashMap<Reference, List<AbstractRefField>>();// 记录所有的引用字段，按引用关系

	public Field[] getLobFieldNames() {
		return lobNames;
	}

	protected void initByAnno(Class<?> thisType, javax.persistence.Table table, BindDataSource bindDs) {
		// schema初始化
		if (table != null) {
			if (table.schema().length() > 0) {
				schema = MetaHolder.getMappingSchema(table.schema());// 重定向
			}
			if (table.name().length() > 0) {
				tableName = table.name();
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
		if (bindDs != null) {
			this.bindDsName = MetaHolder.getMappingSite(StringUtils.trimToNull(bindDs.value()));
		}
	}

	public String getBindDsName() {
		return bindDsName;
	}

	public ColumnMapping<?> getColumnDef(Field field) {
		return schemaMap.get(field);
	}

	public void setBindDsName(String bindDsName) {
		this.bindDsName = MetaHolder.getMappingSite(bindDsName);
		this.bindProfile = null;
	}

	public List<ColumnMapping<?>> getColumns() {
		if (metaFields == null) {
			Collection<ColumnMapping<?>> map = this.getColumnSchema();
			ColumnMapping<?>[] fields = map.toArray(new ColumnMapping[map.size()]);
			Arrays.sort(fields, new Comparator<ColumnMapping<?>>() {
				public int compare(ColumnMapping<?> field1, ColumnMapping<?> field2) {
					Class<? extends ColumnType> type1 = field1.get().getClass();
					Class<? extends ColumnType> type2 = field2.get().getClass();
					Boolean b1 = (type1 == ColumnType.Blob.class || type1 == ColumnType.Clob.class);
					Boolean b2 = (type2 == ColumnType.Blob.class || type2 == ColumnType.Clob.class);
					return b1.compareTo(b2);
				}
			});
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
		ColumnMapping<?> mType = this.schemaMap.get(fld);
		if (mType != null) {
			return mType.getColumnName(profile, escape);
		}
		// 意外情况
		if (fld instanceof JpqlExpression) {
			throw new UnsupportedOperationException();
		}
		String name = profile.getColumnNameToUse(fld.name());
		return escape ? DbUtils.escapeColumn(profile, name) : name;
	}

	public String getColumnName(Field fld, String alias, DatabaseDialect profile) {
		if (alias != null) {
			if (fld instanceof JpqlExpression) {
				throw new UnsupportedOperationException();
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append(alias).append('.').append(getColumnName(fld, profile, true));
				return sb.toString();
			}
		} else {
			return getColumnName(fld, profile, true);
		}
	}

	private DbTable cachedTable;
	private DatabaseDialect bindProfile;
	protected KeyDimension pkDim;

	public DbTable getBaseTable(DatabaseDialect profile) {
		if (bindProfile != profile) {
			synchronized (this) {
				initCache(profile);
			}
		}
		return cachedTable;
	}

	private void initCache(DatabaseDialect profile) {
		bindProfile = profile;
		cachedTable = new DbTable(bindDsName, profile.getObjectNameToUse(getTableName(true)), false, false);
	}

	public KeyDimension getPKDimension(List<Serializable> pks, DatabaseDialect profile) {
		if (pkDim == null) {
			PKQuery<?> query = new PKQuery<IQueryableEntity>(this, pks, instance());
			BindSql sql = query.toPrepareWhereSql(null, profile);
			KeyDimension dim = new KeyDimension(sql.getSql(), null);
			pkDim = dim;
		}
		return pkDim;
	}

	public Field findField(String left) {
		if (left == null)
			return null;
		return lowerFields.get(left.toLowerCase());
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

	protected void removeAutoIncAndTimeUpdatingField(Field oldField) {
		if (increMappings != null) {
			for (AutoIncrementMapping<?> m : increMappings) {
				if (m.field() == oldField) {
					increMappings = (AutoIncrementMapping[]) ArrayUtils.removeElement(increMappings, m);
					break;
				}
			}
		}
		if (updateTimeMapping != null) {
			for (AbstractTimeMapping<?> m : updateTimeMapping) {
				if (m.field() == oldField) {
					updateTimeMapping = (AbstractTimeMapping[]) ArrayUtils.removeElement(updateTimeMapping, m);
					break;
				}
			}
		}
	}

	protected void updateAutoIncrementAndUpdate(ColumnMapping<?> mType) {
		if (mType instanceof AbstractTimeMapping<?>) {
			AbstractTimeMapping<?> m = (AbstractTimeMapping<?>) mType;
			if (m.isForUpdate()) {
				updateTimeMapping = ArrayUtils.addElement(updateTimeMapping, m);
			}
		}

		if (mType instanceof AutoIncrementMapping<?>) {
			increMappings = ArrayUtils.addElement(increMappings, (AutoIncrementMapping<?>) mType);
		}
	}

	protected void addRefField(AbstractRefField f) {
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

	@Override
	public List<ITableMetadata> getReferenceTables() {
		// TODO Auto-generated method stub
		return null;
	}

	public ExtensionConfig getExtension(IQueryableEntity d) {
		throw new UnsupportedOperationException();
	}

	public ExtensionConfig getExtension(String key) {
		throw new UnsupportedOperationException();
	}
	protected abstract Collection<ColumnMapping<?>> getColumnSchema();
}
