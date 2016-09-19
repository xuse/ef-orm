package jef.database;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import jef.database.annotation.EasyEntity;
import jef.database.meta.ITableMetadata;
import jef.database.support.VarObjAdapter;
import jef.tools.Assert;

@XmlJavaTypeAdapter(VarObjAdapter.class)
@EasyEntity(checkEnhanced = false, refresh = false)
public final class VarObject extends DataObject implements Map<String, Object>,MetadataContainer {
	private static final long serialVersionUID = 3915258646897359358L;
	private final HashMap<String, Object> map = new HashMap<String, Object>();

	private ITableMetadata meta;

	public ITableMetadata getMeta() {
		return meta;
	}

	protected VarObject(){
	}
	
	public VarObject(ITableMetadata meta) {
		Assert.notNull(meta);
		this.meta = meta;
	}

	public VarObject(ITableMetadata meta, boolean recordField) {
		this.meta = meta;
		this._recordUpdate = false;
	}

	public int size() {
		return map.size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	/**
	 * 得到一个字段值
	 * 
	 * @param key
	 * @return
	 */
	public Object get(String key) {
		super.beforeGet(key);
		return map.get(key);
	}

	/**
	 * 得到字段值，并转换为List<VarObject>
	 * 
	 * @param key
	 * @throws ClassCastException
	 *             当目标类型不能转换为List时
	 * @return 将类型强制转换为List<VarObject>并返回。 由于泛型擦除机制，并不能保证每个元素都是VarObject
	 */
	@SuppressWarnings("unchecked")
	public List<VarObject> getList(String key) {
		Object obj = get(key);
		if (obj == null)
			return Collections.EMPTY_LIST;
		return (List<VarObject>) obj;
	}

	/**
	 * 得到字段值，并转换为Collection<VarObject>
	 * 
	 * @param key
	 * @throws ClassCastException
	 *             当目标类型不能转换为Collection时
	 * @return 将类型强制转换为Collection<VarObject>并返回。 由于泛型擦除机制，并不能保证每个元素都是VarObject
	 */
	@SuppressWarnings("unchecked")
	public Collection<VarObject> getCollection(String key) {
		Object obj = get(key);
		return (Collection<VarObject>) obj;
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String key, Class<T> clz) {
		Object obj = get(key);
		return (List<T>) obj;
	}

	/**
	 * 得到字段值，并转换为Collection<T>
	 * 
	 * @param key
	 * @throws ClassCastException
	 *             当目标类型不能转换为Collection时
	 * @return 将类型强制转换为Collection<T>并返回。 由于泛型擦除机制，并不能保证每个元素都是T
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getCollection(String key, Class<T> clz) {
		Object obj = get(key);
		return (Collection<T>) obj;
	}

	public Object get(Object key) {
		return map.get(key);
	}

	/**
	 * 设置字段的值，增加了字段名和数据类型的校验，可以安全地设置数值
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public VarObject set(String key, Object value) {
		Field field = meta.getField(key);
		if (field == null) {
			// Check field available
			if (!meta.getRefFieldsByName().containsKey(key)) {
				throw new IllegalArgumentException("Unknown field name '" + key + "' in table " + meta.getTableName(true) + ". Avaliable:" + meta.getAllFieldNames());
			}
		} else {
			// Check the data type
			Class<?> expected = meta.getColumnDef(field).getFieldType();
			if (value != null && !expected.isAssignableFrom(value.getClass())) {
				throw new IllegalArgumentException("Field value not match the column type in database. field name '" + key + "' value is a '" + value.getClass().getSimpleName() + "'. expected is " + expected.getSimpleName());
			}
			if (_recordUpdate) {
				super.prepareUpdate(field, value);
			}
		}
		map.put(key, value);
		return this;
	}

	/**
	 * 设置字段的值，不作数据类型和字段值的校验
	 */
	public Object put(String key, Object value) {
		Field field = meta.getField(key);
		if (field != null) {
			if (_recordUpdate) {
				super.prepareUpdate(field, value, true);
			}
		}
		return map.put(key, value);
	}

	public Object remove(Object key) {
		Field field = meta.getField(String.valueOf(key));
		if (field == null) {
			if (!meta.getRefFieldsByName().containsKey(key)) {
				throw new IllegalArgumentException("Unknown field name" + key);
			}
		} else {
			if (_recordUpdate) {
				super.prepareUpdate(field, null);
			}
		}
		return map.remove(key);
	}

	public void putAll(Map<? extends String, ? extends Object> m) {
		map.putAll(m);
	}

	public void clear() {
		map.clear();
	}

	public Set<String> keySet() {
		return meta.getAllFieldNames();
	}

	public Collection<Object> values() {
		return map.values();
	}

	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return map.entrySet();
	}

	/**
	 * 得到字段值，类型转换为String
	 * 
	 * @param key
	 * @return
	 */
	public String getString(String key) {
		Object obj = get(key);
		if (obj == null)
			return null;
		return String.valueOf(obj);
	}

	/**
	 * 得到字段值，类型转换为Integer
	 * 
	 * @param key
	 * @return
	 */
	public Integer getInteger(String key) {
		Object obj = get(key);
		return (Integer) obj;
	}

	/**
	 * 得到字段值，类型转换为Long
	 * 
	 * @param key
	 * @return
	 */
	public Long getLong(String key) {
		Object obj = get(key);
		return (Long) obj;
	}

	/**
	 * 得到字段值，类型转换为Double
	 * 
	 * @param key
	 * @return
	 */
	public Double getDouble(String key) {
		Object obj = get(key);
		return (Double) obj;
	}

	/**
	 * 得到字段值，类型转换为Boolean
	 * 
	 * @param key
	 * @return
	 */
	public Boolean getBoolean(String key) {
		Object obj = get(key);
		return (Boolean) obj;
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return map.equals(obj);
	}

	@Override
	public String toString() {
		return map.toString();
	}
}
