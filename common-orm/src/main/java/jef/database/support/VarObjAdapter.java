/*
 * EF-ORM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import jef.database.VarObject;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.tools.ArrayUtils;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.ConvertUtils;

/**
 * 使SimpleMap对象支持JAXB序列化和反序列化
 * 
 * @author Administrator
 * 
 * @param <K>
 * @param <V>
 */
public class VarObjAdapter extends XmlAdapter<VarAttribute[], VarObject> {

	@Override
	public VarObject unmarshal(VarAttribute[] vs) throws Exception {
		List<VarAttribute> list = ArrayUtils.asList(vs);
		String tableName = null;
		String className = null;
		for (Iterator<VarAttribute> iter = list.iterator(); iter.hasNext();) {
			VarAttribute v = iter.next();
			if (VarAttribute.ATTR_TABLE_NAME.equals(v.getName())) {
				tableName = v.getValue();
				iter.remove();
			} else if (VarAttribute.ATTR_CLASS_NAME.equals(v.getName())) {
				className = v.getValue();
				iter.remove();
			}
		}
		VarObject var = create(tableName, className);
		for (VarAttribute entry : list) {
			String name = entry.getName();
			Object value = getValue(entry.getValue(), entry.getDataType());
			var.put(name, value);
		}
		return var;
	}

	@Override
	public VarAttribute[] marshal(VarObject v) throws Exception {
		List<VarAttribute> list = new ArrayList<VarAttribute>(v.size() + 2);
		ITableMetadata meta = v.getMeta();
		VarAttribute a = new VarAttribute(VarAttribute.ATTR_CLASS_NAME, meta.getName());
		list.add(a);
		a = new VarAttribute(VarAttribute.ATTR_TABLE_NAME, meta.getTableName(true));
		list.add(a);
		for(Entry<String,Object> entry:v.entrySet()){
			new VarAttribute(entry.getKey(), entry.getValue());
			list.add(a);
		}
		return list.toArray(new VarAttribute[list.size()]);
	}

	private Object getValue(String value, String dataType) {
		if(dataType==null){
			return value;
		}
		try {
			Class clz = Class.forName(dataType);
			return ConvertUtils.toProperType(value, new ClassEx(clz), null);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private VarObject create(String tableName, String className) {
		ITableMetadata meta=MetaHolder.getDynamicMeta(className);
		if(meta==null){
			if(tableName.indexOf('.')>-1){
				int n=tableName.indexOf('.');
				meta=MetaHolder.lookup(tableName.substring(0,n), tableName.substring(n+1));
			}else{
				meta=MetaHolder.lookup(null, tableName);
			}
		}
		return new VarObject(meta,false);
	}

}
