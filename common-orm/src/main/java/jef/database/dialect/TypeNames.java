package jef.database.dialect;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import jef.tools.StringUtils;

/**
 * This class maps a type to names. Associations may be marked with a capacity.
 * Calling the get() method with a type and actual size n will return the
 * associated name with smallest capacity >= n, if available and an unmarked
 * default type otherwise. Eg, setting
 * 
 * <pre>
 * names.put(type, &quot;TEXT&quot;);
 * names.put(type, 255, &quot;VARCHAR($l)&quot;);
 * names.put(type, 65534, &quot;LONGVARCHAR($l)&quot;);
 * </pre>
 * 
 * will give you back the following:
 * 
 * <pre>
 *  names.get(type)         // --> "TEXT" (default)
 *  names.get(type,    100) // --> "VARCHAR(100)" (100 is in [0:255])
 *  names.get(type,   1000) // --> "LONGVARCHAR(1000)" (1000 is in [256:65534])
 *  names.get(type, 100000) // --> "TEXT" (default)
 * </pre>
 * 
 * On the other hand, simply putting
 * 
 * <pre>
 * names.put(type, &quot;VARCHAR($l)&quot;);
 * </pre>
 * 
 * would result in
 * 
 * <pre>
 *  names.get(type)        // --> "VARCHAR($l)" (will cause trouble)
 *  names.get(type, 100)   // --> "VARCHAR(100)"
 *  names.get(type, 10000) // --> "VARCHAR(10000)"
 * </pre>
 */
public class TypeNames {

	private Map<Integer, Map<Integer, String>> weighted = new HashMap<Integer, Map<Integer, String>>();
	private Map<Integer, String> defaults = new HashMap<Integer, String>();

	/**
	 * get default type name for specified type
	 * 
	 * @param typecode
	 *            the type key
	 * @return the default type name associated with specified key
	 */
	public String get(int typecode) {
		String result = defaults.get(typecode);
		if (result == null)
			throw new IllegalArgumentException("No Dialect mapping for JDBC type: " + typecode);
		return result;
	}

	/**
	 * get type name for specified type and size
	 * 
	 * @param typecode
	 *            the type key
	 * @param size
	 *            the SQL length
	 * @param scale
	 *            the SQL scale
	 * @param precision
	 *            the SQL precision
	 * @return the associated name with smallest capacity >= size, if available
	 *         and the default type name otherwise
	 */
	public String get(int typecode, int size, int precision, int scale){
		Map<Integer, String> map = weighted.get(typecode);
		if (map != null && map.size() > 0) {
			// iterate entries ordered by capacity to find first fit
			for (Map.Entry<Integer, String> entry : map.entrySet()) {
				if (size <= entry.getKey()) {
					return replace(entry.getValue(), size, precision, scale);
				}
			}
		}
		return replace(get(typecode), size, precision, scale);
	}

	private static String replace(String type, int size, int precision, int scale) {
		type = StringUtils.replaceOnce(type, "$s", Integer.toString(scale));
		type = StringUtils.replaceOnce(type, "$l", Integer.toString(size));
		return StringUtils.replaceOnce(type, "$p", Integer.toString(precision));
	}

	/**
	 * set a type name for specified type key and capacity
	 * 
	 * @param typecode
	 *            the type key
	 */
	public void put(int typecode, int capacity, String value) {
		Map<Integer, String> map = weighted.get(typecode);
		if (map == null) {// add new ordered map
			map = new TreeMap<Integer, String>();
			weighted.put(typecode, map);
		}
		map.put(capacity, value);
	}

	/**
	 * set a default type name for specified type key
	 * 
	 * @param typecode
	 *            the type key
	 */
	public void put(int typecode, String value) {
		defaults.put(typecode, value);
	}
}
