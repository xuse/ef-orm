package jef.database;

import java.util.HashMap;
import java.util.Map;

import jef.database.meta.ExtensionConfig;
import jef.database.meta.ExtensionConfigFactory;
import jef.database.meta.ITableMetadata;
import jef.database.support.accessor.EfPropertiesExtensionProvider;

/**
 * 扩展属性支持
 * 
 * @author jiyi
 * 
 */
public abstract class EntityExtensionSupport extends DataObject implements MetadataContainer{
	private transient Map<String, Object> attributes;
	// 扩展点信息
	private ExtensionConfigFactory extensionFactory;
	// 绑定后的表结构
	private ExtensionConfig config;

	public EntityExtensionSupport() {
		this.extensionFactory = EfPropertiesExtensionProvider.getInstance().getEF(this.getClass());
		if(extensionFactory==null){
			System.out.println(this.getClass());
		}
		this.config = extensionFactory.getDefault();
	}

	public ITableMetadata getMeta() {
		if (config == null) {
			if(extensionFactory==null){
				System.out.println(extensionFactory);
			}
			config = extensionFactory.getExtension(this);
		}
		return config.getMeta();
	}

	/**
	 * 动态扩展
	 * 
	 * @param typeName
	 */
	public EntityExtensionSupport(String typeName) {
		this.extensionFactory = EfPropertiesExtensionProvider.getInstance().getEF(this.getClass());
		if (extensionFactory == null) {
			throw new IllegalArgumentException();
		}
		this.config = extensionFactory.getExtension(typeName);
	}

	/**
	 * 设置扩展属性
	 * 
	 * @param prop
	 * @param value
	 */
	public void setAtribute(String key, Object value) {
		if (attributes == null) {
			attributes = new HashMap<String, Object>();
		}
		ITableMetadata meta=this.getMeta();
		Field field =  meta.getField(key);
		if (field == null) {
			throw new IllegalArgumentException("Unknown [" + key + "] .Avaliable: " + getMeta().getAllFieldNames());
		} else {
			// Check the data type
			Class<?> expected =  meta.getColumnDef(field).getFieldType();
			if (value != null && !expected.isAssignableFrom(value.getClass())) {
				throw new IllegalArgumentException("Field value invalid for the data type of column. field name '" + key + "' value is a '" + value.getClass().getSimpleName() + "'. expected is " + expected.getSimpleName());
			}
			if (_recordUpdate)
				super.markUpdateFlag(field, value);
		}
		attributes.put(key, value);
	}

	/**
	 * 获取扩展属性
	 * 
	 * @param prop
	 * @return
	 */
	public Object getAtribute(String key) {
		super.beforeGet(key);
		if (attributes == null) {
			return null;
		}
		return attributes.get(key);
	}
}
