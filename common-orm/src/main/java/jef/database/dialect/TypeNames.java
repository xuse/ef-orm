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
 */
public class TypeNames {

	private Map<Integer, Map<Integer, Type>> weighted = new HashMap<Integer, Map<Integer, Type>>();
	private Map<Integer, Type> defaults = new HashMap<Integer, Type>();

	/**
	 * 返回该SQL类型在此数据库中的描述类型
	 * @param typecode 要求的sql类型
	 * @return  PairIS，first表示在当前数据库中的实现SQL类型，second表示字段类型描述文字
	 */
	public Type get(int typecode) {
		Type result = defaults.get(typecode);
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
	public Type get(int typecode, int size, int precision, int scale) {
		Map<Integer, Type> map = weighted.get(typecode);
		if (map != null && map.size() > 0) {
			if (size > 0) { // 举例: 如果是未指定长度的BLOB按BLOB的处理，指定长度后按VARBINARY处理。
				// iterate entries ordered by capacity to find first fit
				for (Map.Entry<Integer, Type> entry : map.entrySet()) {
					if (size <= entry.getKey()) {
						return replace(entry.getValue(), size, precision, scale);
					}
				}
			}
		}
		return replace(get(typecode), size, precision, scale);
	}

	private static Type replace(Type pair, int size, int precision, int scale) {
		String type = pair.getName();
		type = StringUtils.replaceEach(type, new String[] { "$l", "$s", "$p" }, new String[] { Integer.toString(size), Integer.toString(scale), Integer.toString(precision) });
		return new Type(pair.getSqlType(),type);
	}

	/**
	 * 注册一个类型
	 * @param typecode 类型
	 * @param capacity 容量（length或者precision）
	 * @param value    SQL描述符，支持 $l $s $p三个宏
	 * @param newSqlType 如果数据库实际的数据类型发生改变，那么记录改变后的SQL类型。（比如用CHAR模拟BOOLEAN，用VARBINARY代替BLOB等）。如果未变化，传入0即可。
	 */
	public void put(int typecode, int capacity, String value, int newSqlType) {
		Map<Integer, Type> map = weighted.get(typecode);
		if (map == null) {// add new ordered map
			map = new TreeMap<Integer, Type>();
			weighted.put(typecode, map);
		}
		map.put(capacity, new Type(newSqlType==0?typecode:newSqlType, value));
	}

	/**
	 * 某类sql数据类型在本数据库上的实现
	 * @param typecode
	 * @param value
	 * @param newSqlType
	 */
	public void put(int typecode, String value, int newSqlType,String... alias) {
		Type type=new Type(newSqlType==0?typecode:newSqlType, value);
		type.setAlias(alias);
		defaults.put(typecode, type);
	}
	
	/**
	 * 还原数据类型，根据一个数据类型描述，还原为java.sql.Types常量
	 * @return
	 */
	public Map<String,Integer> getTypeNameCodes(){
		Map<String,Integer> result=new HashMap<String,Integer>();
		for(Type p: defaults.values()) {
			result.put(StringUtils.substringBefore(p.getName().toUpperCase(),"("), p.getSqlType());
			if(p.getAlias()!=null && p.getAlias().length>0) {
				for(String alias: p.getAlias()) {
					result.put(alias.toUpperCase(), p.getSqlType());		
				}
			}
		}
		for(Map<Integer, Type> m: weighted.values()) {
			for(Type p: m.values()) {
				result.put(StringUtils.substringBefore(p.getName().toUpperCase(),"("), p.getSqlType());
				if(p.getAlias()!=null && p.getAlias().length>0) {
					for(String alias: p.getAlias()) {
						result.put(alias.toUpperCase(), p.getSqlType());		
					}
				}
			}	
		}
		return result;
	}
}
