package jef.database.meta;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jef.database.IQueryableEntity;
import jef.database.annotation.DynamicTable;
import jef.database.dialect.type.ColumnMapping;
import jef.database.query.ConditionQuery;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.FieldAccessor;
import jef.tools.reflect.FieldEx;

public class ExtensionTemplate implements ExtensionConfigFactory {
	private final Map<String, ExtensionInstance> cache = new ConcurrentHashMap<String, ExtensionInstance>();
	@SuppressWarnings("unused")
	private DynamicTable dt;
	private FieldAccessor keyAccessor;
	private MetadataAdapter parent;

	public ExtensionTemplate(DynamicTable dt, Class<?> clz, MetadataAdapter meta) {
		this.dt = dt;
		this.parent = meta;

		String keyField = dt.resourceTypeField();
		FieldEx field = BeanUtils.getField(clz, keyField);
		if (field == null) {
			throw new IllegalArgumentException("Field " + keyField + " not exist");
		}
		this.keyAccessor = field.getAccessor();
	}

	public MetadataAdapter getTemplate() {
		return parent;
	}
	
	public FieldAccessor getKeyAccessor(){
		return keyAccessor;
	}

	@Override
	public ExtensionConfig getExtension(IQueryableEntity q) {
		if (q == null) {
			throw new IllegalArgumentException();
		}
		String key = (String) keyAccessor.getObject(q);
		if (key == null || key.length() == 0) {
			if (q.hasQuery()) {
				key = (String) q.getQuery().getAttribute(ConditionQuery.CUSTOM_TABLE_TYPE);
			}
			if (key == null || key.length() == 0)
				throw new IllegalArgumentException("the entity has no key for dynamic extendsion");
		}
		return getExtension(key);
	}

	@Override
	public ExtensionConfig getExtension(String extensionName) {
		ExtensionInstance ec = cache.get(extensionName);
		if (ec != null)
			return ec;
		ec = new ExtensionInstance(extensionName, parent);
		cache.put(extensionName, ec);
		return ec;
	}

	@Override
	public ExtensionConfig getDefault() {
		// throw new UnsupportedOperationException();
		return null;
	}

	final class ExtensionInstance extends AbstractExtensionConfig {
		public ExtensionInstance(String key, MetadataAdapter meta) {
			super(key, meta);
		}

		@Override
		public boolean isDynamicTable() {
			return true;
		}

		@Override
		protected MetadataAdapter merge() {
			DynamicMetadata tuple = new DynamicMetadata(parent, this);
			for (ColumnMapping<?> f : getExtensionMeta().getColumns()) {
				tuple.updateColumn(f.fieldName(), f.rawColumnName(), f.get(), f.isPk());
			}
			return tuple;
		}
	}
}
