package jef.database.meta;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.BeanExtensionProvider;
import jef.accelerator.bean.ExtensionModificationListener;
import jef.database.Field;
import jef.database.dialect.type.ColumnMapping;
import jef.tools.Assert;
import jef.tools.reflect.Property;

public class EfPropertiesExtensionProvider implements BeanExtensionProvider {
	@SuppressWarnings({ "unused", "rawtypes" })
	private Map<Class, BeanAccessor> cacheView;

	private final Map<Class<?>, ExtensionConfigFactory> extensions = new HashMap<Class<?>, ExtensionConfigFactory>();

	@SuppressWarnings({ "rawtypes" })
	@Override
	public void setBeanAccessorCache(Map<Class, BeanAccessor> cache) {
		this.cacheView = cache;
	}

	@Override
	public boolean isDynamicExtensionClass(Class<?> javaBean) {
		ExtensionConfigFactory cf = extensions.get(javaBean);
		return cf != null && (cf instanceof ExtensionTemplate);
	}

	public static class ExtensionProperty implements Property {
		private ExtensionConfig ef;
		private String name;
		private Class<?> type;
		private Type genericType;

		public ExtensionProperty(String fieldName, ExtensionConfig cf, Type genericType, Class<?> type) {
			this.name = fieldName;
			this.ef = cf;
			this.genericType = genericType;
			this.type = type;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isReadable() {
			return true;
		}

		@Override
		public boolean isWriteable() {
			return true;
		}

		@Override
		public Object get(Object obj) {
			return ef.doPropertyGet(obj, name);
		}

		@Override
		public void set(Object obj, Object value) {
			ef.doPropertySet(obj, name, value);
		}

		@Override
		public Class<?> getType() {
			return type;
		}

		@Override
		public Type getGenericType() {
			return genericType;
		}
	}

	@Override
	public String getStaticExtensionName(Class<?> clz) {
		ExtensionConfigFactory cf = extensions.get(clz);
		if (cf instanceof ExtensionKeyValueTable) {
			((ExtensionKeyValueTable) cf).getName();
		}
		return null;
	}

	@Override
	public Map<String, Property> getExtensionProperties(Class<?> type, String extensionName, ExtensionModificationListener listener) {
		ExtensionConfigFactory cf = extensions.get(type);
		if (cf instanceof ExtensionKeyValueTable) {
			ExtensionKeyValueTable ext = (ExtensionKeyValueTable) cf;
			Assert.equals(extensionName, ext.getName());
			Map<String, Property> props = new HashMap<String, Property>();
			for (ColumnMapping<?> f : ext.getMeta().getMetaFields()) {
				props.put(f.fieldName(), new ExtensionProperty(f.fieldName(), ext, f.getFieldType(), f.getFieldType()));
			}
			ext.getMeta().addListener(new Adapter(listener,ext));
			return props;
		} else if (cf instanceof ExtensionTemplate) {
			ExtensionTemplate temp = (ExtensionTemplate) cf;
			ExtensionConfig ext = temp.valueOf(extensionName);
			Map<String, Property> props = new HashMap<String, Property>();
			for (ColumnMapping<?> f : ext.getMeta().getMetaFields()) {
				props.put(f.fieldName(), new ExtensionProperty(f.fieldName(), ext, f.getFieldType(), f.getFieldType()));
			}
			ext.getMeta().addListener(new Adapter(listener,ext));
			return props;

		}
		throw new IllegalArgumentException();
	}

	class Adapter implements TupleModificationListener {
		private ExtensionModificationListener ext;
		private ExtensionConfig ef;

		public Adapter(ExtensionModificationListener ext, ExtensionConfig ef) {
			this.ext = ext;
			this.ef = ef;
		}

		@Override
		public void onDelete(TupleMetadata meta, Field field) {
			event(meta);
		}
		@Override
		public void onUpdate(TupleMetadata meta, Field field) {
			event(meta);
		}

		private void event(TupleMetadata meta) {
			Map<String, Property> props = new HashMap<String, Property>();
			for (ColumnMapping<?> f : meta.getMetaFields()) {
				props.put(f.fieldName(), new ExtensionProperty(f.fieldName(), ef, f.getFieldType(), f.getFieldType()));
			}
			ext.setExtProperties(props);
		}

	}
}
