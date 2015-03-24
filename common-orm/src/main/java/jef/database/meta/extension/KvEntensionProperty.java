package jef.database.meta.extension;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jef.common.log.LogUtil;
import jef.database.EntityExtensionSupport;
import jef.database.VarObject;
import jef.database.annotation.DynamicKeyValueExtension;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.ITableMetadata;
import jef.database.meta.TupleMetadata;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.FieldAccessor;
import jef.tools.reflect.GenericUtils;
import jef.tools.reflect.Property;

public class KvEntensionProperty implements Property {
	private String kColumn;
	private String vColumn;
	private TupleMetadata extKvMeta;
	private ITableMetadata baseResourceMeta;
	private TupleMetadata extensionMeta;

	private static FieldAccessor accessor;
	static {
		try {
			Field field = EntityExtensionSupport.class.getDeclaredField("attributes");
			accessor = BeanUtils.getFieldAccessor(field, false);
		} catch (Exception e) {
			LogUtil.error("init field error.", e);
		}
	}

	public KvEntensionProperty(String string, TupleMetadata containerTuple, AbstractMetadata parent, TupleMetadata extensionMeta, DynamicKeyValueExtension config) {
		this.kColumn = config.keyColumn();
		this.vColumn = config.valueColumn();
		this.baseResourceMeta = parent;
		this.extensionMeta = extensionMeta;
		this.extKvMeta = containerTuple;
	}

	@Override
	public String getName() {
		return "attributes";
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
		@SuppressWarnings("unchecked")
		Map<String, Object> attributes = (Map<String, Object>) accessor.getObject(obj);
		if (attributes == null) {
			return Collections.EMPTY_LIST;
		}
		List<VarObject> attrs = new ArrayList<VarObject>();
		for (Entry<String, Object> entry : attributes.entrySet()) {
			VarObject v = new VarObject(extKvMeta);
			v.put(kColumn, entry.getKey());
			v.put(vColumn, String.valueOf(entry.getValue()));
//			for (ColumnMapping mapping : baseResourceMeta.getPKFields()) {
//				Object idValue = mapping.getFieldAccessor().get(obj);
//				if (idValue != null) {
//					v.put(mapping.fieldName(), idValue);
//				}
//			}
			attrs.add(v);
		}
		return attrs;
	}

	@Override
	public void set(Object obj, Object value) {
		@SuppressWarnings("rawtypes")
		Collection<?> values = (Collection) value;
		EntityExtensionSupport support = (EntityExtensionSupport) obj;
		for (Object entry : values) {
			if (entry instanceof VarObject) {
				VarObject var = (VarObject) entry;
				String key = var.getString(kColumn);
				String text = var.getString(vColumn);
				support.setAtribute(key, fixValue(key,text));
			} else {
				throw new IllegalArgumentException();
			}
		}
	}

	private Object fixValue(String key,String text) {
		ColumnMapping mapping=extensionMeta.getColumnDef(extensionMeta.getField(key));
		if(mapping==null)return text;
		return BeanUtils.toProperType(text, new ClassEx(mapping.getFieldType()), null);
	}

	@Override
	public Class<?> getType() {
		return List.class;
	}

	@Override
	public Type getGenericType() {
		return GenericUtils.newListType(VarObject.class);
	}
}
