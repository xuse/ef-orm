package jef.database.meta;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jef.database.IQueryableEntity;
import jef.database.annotation.DynamicTable;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.AnnotationProvider.ClassAnnotationProvider;
import jef.database.query.ConditionQuery;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.FieldAccessor;
import jef.tools.reflect.FieldEx;

public class ExtensionTemplate implements ExtensionConfigFactory {
	private final Map<String, ExtensionInstance> cache = new ConcurrentHashMap<String, ExtensionInstance>();
	@SuppressWarnings("unused")
	private DynamicTable dt;
	private FieldAccessor keyAccessor;
	private AbstractMetadata parent;
	
	List<java.lang.reflect.Field> unprocessedField;
	ClassAnnotationProvider annos;
	
	
	public ExtensionTemplate(DynamicTable dt, Class<?> clz, AbstractMetadata meta) {
		this.dt = dt;
		this.parent = meta;

		String keyField = dt.resourceTypeField();
		FieldEx field = BeanUtils.getField(clz, keyField);
		if (field == null) {
			throw new IllegalArgumentException("Field " + keyField + " not exist");
		}
		this.keyAccessor = field.getAccessor();
	}

	public AbstractMetadata getTemplate() {
		return parent;
	}

	public FieldAccessor getKeyAccessor() {
		return keyAccessor;
	}

	@Override
	public ExtensionConfig getExtension(IQueryableEntity q) {
		if (q == null) {
			throw new IllegalArgumentException();
		}
		String key = (String) keyAccessor.get(q);
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
		public ExtensionInstance(String key, AbstractMetadata meta) {
			super(key, meta);
		}

		@Override
		public boolean isDynamicTable() {
			return true;
		}

		@Override
		protected AbstractMetadata merge() {
			DynamicMetadata tuple = new DynamicMetadata(parent, this);
			for (ColumnMapping f : getExtensionMeta().getColumns()) {
				tuple.updateColumn(f.fieldName(), f.rawColumnName(), f.get(), f.isPk());
			}

			// 针对未处理的字段，当做外部引用关系处理
			for (java.lang.reflect.Field f : unprocessedField) {
				// 将这个字段作为外部引用处理
				MetaHolder.processReference(tuple, annos.forField(f));
				// 还有一种情况，即定义了Column注解，但不属于元模型的一个字段，用于辅助映射的。当结果拼装时有用
//				processColumnHelper(f, tuple, annos);
			}
			return tuple;
		}
	}
}
