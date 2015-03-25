package jef.database.meta.extension;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import jef.database.EntityExtensionSupport;
import jef.database.Field;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.DynamicMetadata;
import jef.database.meta.ExtensionConfig;
import jef.database.meta.ExtensionConfigFactory;
import jef.database.meta.KvExtensionImpl;
import jef.database.meta.ExtensionTemplate;
import jef.database.meta.MetaHolder;
import jef.database.meta.TupleMetadata;
import jef.database.meta.TupleModificationListener;
import jef.tools.reflect.Property;

public class EfPropertiesExtensionProvider implements BeanExtensionProvider {

	private static final EfPropertiesExtensionProvider extensionContext = new EfPropertiesExtensionProvider();

	public static EfPropertiesExtensionProvider getInstance() {
		return extensionContext;
	}

	private final Map<Class<?>, ExtensionConfigFactory> extensions = new HashMap<Class<?>, ExtensionConfigFactory>();

	@Override
	public boolean isDynamicExtensionClass(Class<?> javaBean) {
		ExtensionConfigFactory cf = extensions.get(javaBean);
		return cf != null && (cf instanceof ExtensionTemplate);
	}
	
	public ExtensionConfigFactory getExtensionFactory(Class<?> javaBean){
		return extensions.get(javaBean);
	}

	public static class ExtensionProperty implements Property {
		private String name;
		private Class<?> type;
		private Type genericType;

		public ExtensionProperty(String fieldName, Type genericType, Class<?> type) {
			this.name = fieldName;
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
			return ((EntityExtensionSupport) obj).getAtribute(name);
		}

		@Override
		public void set(Object obj, Object value) {
			((EntityExtensionSupport) obj).setAtribute(name, value);
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
		if (cf instanceof KvExtensionImpl) {
			((KvExtensionImpl) cf).getName();
		}
		return null;
	}

	@Override
	public Map<String, Property> getExtensionProperties(Class<?> type, String extensionName, ExtensionModificationListener listener) {
		ExtensionConfigFactory cf = extensions.get(type);
		if (cf != null) {
			Map<String, Property> props = new HashMap<String, Property>();
			ExtensionConfig config = cf.getExtension(extensionName);
			TupleMetadata extension = config.getExtensionMeta();
			for (ColumnMapping f : extension.getColumns()) {
				props.put(f.fieldName(), new ExtensionProperty(f.fieldName(), f.getFieldType(), f.getFieldType()));
			}
			extension.addListener(new Adapter(config, listener));
			return props;
		}
		throw new IllegalArgumentException();
	}

	class Adapter implements TupleModificationListener {
		private ExtensionModificationListener ext;
		private ExtensionConfig config;

		public Adapter(ExtensionConfig config, ExtensionModificationListener ext) {
			this.ext = ext;
			this.config=config;
		}

		@Override
		public void onDelete(DynamicMetadata meta, Field field) {
			event(meta);
		}

		@Override
		public void onUpdate(DynamicMetadata meta, Field field) {
			event(meta);
		}

		private void event(DynamicMetadata meta) {
			Map<String, Property> props = new HashMap<String, Property>();
			for (ColumnMapping f : meta.getColumns()) {
				props.put(f.fieldName(), new ExtensionProperty(f.fieldName(), f.getFieldType(), f.getFieldType()));
			}
			config.flush(meta);
			ext.setExtProperties(props);
		}
	}

	public void register(Class<?> clz, ExtensionConfigFactory ef) {
		ExtensionConfigFactory old=this.extensions.put(clz, ef);
//		if(old!=null){
//			LogUtil.warn("重复注册？"+clz+"  "+ef);
//		}
	}
	
	public ExtensionConfigFactory getEF(Class<? extends EntityExtensionSupport> clz) {
		ExtensionConfigFactory ef=extensions.get(clz);
		if(ef==null){
			MetaHolder.initMetadata(clz,null,null);
			ef=extensions.get(clz);
		}
		return ef;
	}
}
