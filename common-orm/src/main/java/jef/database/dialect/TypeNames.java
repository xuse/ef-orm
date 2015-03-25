package jef.database.dialect;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import jef.common.PairIS;
import jef.tools.StringUtils;

/**
 * This class maps a type to names. Associations may be marked with a capacity.
 * Calling the get() method with a type and actual size n will return the
 * associated name with smallest capacity >= n, if available and an unmarked
 * default type otherwise. Eg, setting
 */
public class TypeNames {

	private Map<Integer, Map<Integer, PairIS>> weighted = new HashMap<Integer, Map<Integer, PairIS>>();
	private Map<Integer, PairIS> defaults = new HashMap<Integer, PairIS>();

	/**
	 * 返回该SQL类型在此数据库中的描述类型
	 * @param typecode 要求的sql类型
	 * @return  PairIS，first表示在当前数据库中的实现SQL类型，second表示字段类型描述文字
	 */
	public PairIS get(int typecode) {
		PairIS result = defaults.get(typecode);
		if (result == null)
			throw new IllegalArgumentException("No Dialect mapping for JDBC type: " + typecode);
		return result;
	}

	/**
	 * 返回该SQL类型在此数据库中的描述类型
	 * @param typecode 要求的sql类型
	 * @param size  大小
	 * @param precision 数值长度 
	 * @param scale     精度
	 * @return PairIS，first表示在当前数据库中的实现SQL类型，second表示字段类型描述文字
	 */
	public PairIS get(int typecode, int size, int precision, int scale) {
		Map<Integer, PairIS> map = weighted.get(typecode);
		if (map != null && map.size() > 0) {
			if (size > 0) { // 举例: 如果是未指定长度的BLOB按BLOB的处理，指定长度后按VARBINARY处理。
				// iterate entries ordered by capacity to find first fit
				for (Map.Entry<Integer, PairIS> entry : map.entrySet()) {
					if (size <= entry.getKey()) {
						return replace(entry.getValue(), size, precision, scale);
					}
				}
			}
		}
		return replace(get(typecode), size, precision, scale);
	}

	private static PairIS replace(PairIS pair, int size, int precision, int scale) {
		String type = pair.second;
		type = StringUtils.replaceEach(type, new String[] { "$l", "$s", "$p" }, new String[] { Integer.toString(size), Integer.toString(scale), Integer.toString(precision) });
		return new PairIS(pair.first,type);
	}

	/**
	 * 注册一个类型
	 * @param typecode 类型
	 * @param capacity 容量（length或者precision）
	 * @param value    SQL描述符，支持 $l $s $p三个宏
	 * @param newSqlType 如果数据库实际的数据类型发生改变，那么记录改变后的SQL类型。（比如用CHAR模拟BOOLEAN，用VARBINARY代替BLOB等）。如果未变化，传入0即可。
	 */
	public void put(int typecode, int capacity, String value, int newSqlType) {
		Map<Integer, PairIS> map = weighted.get(typecode);
		if (map == null) {// add new ordered map
			map = new TreeMap<Integer, PairIS>();
			weighted.put(typecode, map);
		}
		map.put(capacity, new PairIS(newSqlType==0?typecode:newSqlType, value));
	}

	public void put(int typecode, String value, int newSqlType) {
		defaults.put(typecode, new PairIS(newSqlType==0?typecode:newSqlType, value));
	}
}
