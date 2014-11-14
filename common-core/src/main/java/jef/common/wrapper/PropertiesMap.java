package jef.common.wrapper;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jef.common.AbstractMap;

/**
 * 将Properties封装成普通的Map
 * @author jiyi
 *
 */
public class PropertiesMap extends AbstractMap<String, String> implements Map<String, String>{
	private Properties prop;
	
	public PropertiesMap(Properties p){
		this.prop=p;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		return (Set)prop.entrySet();
	}

	@Override
	public int size() {
		return prop.size();
	}

	@Override
	public void clear() {
		prop.clear();		
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<? extends java.util.Map.Entry<String, String>> entryIterator() {
		return (Iterator<? extends java.util.Map.Entry<String, String>>) prop.entrySet().iterator();
	}
}
