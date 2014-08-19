package jef.database.query;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import jef.common.log.LogUtil;
import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IConditionField;
import jef.database.IConditionField.And;
import jef.database.IConditionField.Exists;
import jef.database.IConditionField.Not;
import jef.database.IConditionField.NotExists;
import jef.database.IConditionField.Or;
import jef.database.IQueryableEntity;
import jef.database.ORMConfig;
import jef.database.annotation.PartitionFunction;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionResult;
import jef.database.annotation.PartitionTable;
import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.PartitionSupport;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.MetadataAdapter;
import jef.database.routing.function.KeyFunction;
import jef.database.routing.function.ModulusFunction;
import jef.tools.ArrayUtils;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.BeanWrapper;
import jef.tools.reflect.NopBeanWrapper;

import org.apache.commons.lang.ObjectUtils;

import com.google.common.collect.Multimap;

/**
 * 分表计算器的EF-ORM默认实现，能最大限度的利用JEF的Annotation功能， 对基于Criteria
 * API的Query进行充分的解析，进而进行分表维度计算。
 * 
 * @author wangyuan 20110730
 */
public final class DefaultPartitionCalculator implements PartitionCalculator {

	public PartitionResult[] toTableNames(MetadataAdapter meta, IQueryableEntity instance, Query<?> q, PartitionSupport processor) {
		DatabaseDialect profile = processor.getProfile(null);
		List<DbTable> result;
		boolean doFileter = ORMConfig.getInstance().isFilterAbsentTables();
		if (meta.getPartition() != null && instance != null) {// 分区表，并且具备分区条件
			Set<DbTable> r = getPartitionTables(meta, getPartitionFieldValues(meta,BeanWrapper.wrap(instance), q), profile);
			if (r.isEmpty()) {
				return processor.getSubTableNames(meta); //返回一切可能
			}else{
				result = analyzeRegexpResults(r, processor, meta, profile);
				return toPartitionResult(result, processor, doFileter, meta);
			}
		}
		return meta.getBaseTable(profile).toPartitionResults();
	}
	
	public PartitionResult[] toTableNames(MetadataAdapter meta, Map<String,Dimension> val, PartitionSupport processor) {
		DatabaseDialect profile = processor.getProfile(null);
		List<DbTable> result;
		boolean doFileter = ORMConfig.getInstance().isFilterAbsentTables();
		if (meta.getPartition() != null && val != null) {// 分区表，并且具备分区条件
			Set<DbTable> r = getPartitionTables(meta, val, profile);
			if (r.isEmpty()) {
				return processor.getSubTableNames(meta); //返回一切可能
			}else{
				result = analyzeRegexpResults(r, processor, meta, profile);
				return toPartitionResult(result, processor, doFileter, meta);
			}
		}
		return meta.getBaseTable(profile).toPartitionResults();
	}

	public PartitionResult[] toTableNames(MetadataAdapter meta, PartitionSupport processor, int opType) {
		DatabaseDialect profile = processor.getProfile(null);
		List<DbTable> result;
		if (opType > 0 && meta.getPartition() != null) {// 分区表，并且具备分区条件
			if (opType > 2) { // 取数据库存在的表
				PartitionResult[] results = processor.getSubTableNames(meta);
				if (opType == 3) {
					return removeBaseTable(results,meta.getBaseTable(profile));
				} else {
					return results;
				}
			} else {
				Set<DbTable> tempResult = getPartitionTables(meta, getPartitionFieldValues(meta, NopBeanWrapper.getInstance(), null), profile);
				if (!tempResult.isEmpty()) {
					if (opType==2) {
						DbTable table=meta.getBaseTable(profile);
						tempResult.add(table);
					}
					result = analyzeRegexpResults(tempResult, processor, meta, profile);
					return toPartitionResult(result, processor, false, meta);
				}
			}
		}
		return meta.getBaseTable(profile).toPartitionResults();
	}

	private PartitionResult[] removeBaseTable(PartitionResult[] results,DbTable base) {
		return results;
//		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.PartitionCalculator#toTableName(java.lang.Class,
	 * java.lang.String, jef.database.IQueryableEntity,
	 * jef.database.query.Query, jef.database.meta.DbmsProfile)
	 */
	public PartitionResult toTableName(MetadataAdapter meta, IQueryableEntity instance, Query<?> q, PartitionSupport processor) {
		DatabaseDialect profile = processor.getProfile(null);
		DbTable result;
		if (meta.getPartition() != null && instance != null) {// 认为是分区表的场合
			Set<DbTable> tbs = getPartitionTables(meta, getPartitionFieldValues(meta, BeanWrapper.wrap(instance), q), profile);
			if (tbs.isEmpty()) { // 没有返回的情况，返回基表
				return meta.getBaseTable(profile).toPartitionResult();
			} else if (tbs.size() != 1) {
				throw new IllegalArgumentException("Can not determine which database or table to operate.(one table only)."+tbs);
			} else {
				result = tbs.iterator().next();
				if (result.isTbRegexp && result.isDbRegexp) {// 正则表达式，意味着由于某个分表维度没有设置，变成宽松匹配。这里无法获得实际存在表推算，因此直接退化为基表
					result = meta.getBaseTable(profile);
				} else if (result.isTbRegexp) {
					result = new DbTable(result.dbName, profile.getObjectNameIfUppercase(meta.getTableName(true)));
				} else if (result.isDbRegexp) { // 数据库名声正则的
					result = new DbTable(meta.getBindDsName(), result.table);
				}
			}
			return toSingleTableResult(processor, result, meta);
		} else { // 非分区表
			result = meta.getBaseTable(profile);
			return toSingleTableResult(processor, result, null);
		}

	}
	
	public PartitionResult toTableName(MetadataAdapter meta, Map<String,Dimension> val,PartitionSupport processor) {
		DatabaseDialect profile = processor.getProfile(null);
		DbTable result;
		if (meta.getPartition() != null && val != null) {// 认为是分区表的场合
			Set<DbTable> tbs = getPartitionTables(meta, val, profile);
			if (tbs.isEmpty()) { // 没有返回的情况，返回基表
				return meta.getBaseTable(profile).toPartitionResult();
			} else if (tbs.size() != 1) {
				throw new IllegalArgumentException("Can not determine which database or table to operate.(one table only)."+tbs);
			} else {
				result = tbs.iterator().next();
				if (result.isTbRegexp && result.isDbRegexp) {// 正则表达式，意味着由于某个分表维度没有设置，变成宽松匹配。这里无法获得实际存在表推算，因此直接退化为基表
					result = meta.getBaseTable(profile);
				} else if (result.isTbRegexp) {
					result = new DbTable(result.dbName, profile.getObjectNameIfUppercase(meta.getTableName(true)));
				} else if (result.isDbRegexp) { // 数据库名声正则的
					result = new DbTable(meta.getBindDsName(), result.table);
				}
			}
			return toSingleTableResult(processor, result, meta);
		} else { // 非分区表
			result = meta.getBaseTable(profile);
			return toSingleTableResult(processor, result, null);
		}

	}

	private PartitionResult toSingleTableResult(PartitionSupport processor, DbTable result, ITableMetadata meta) {
		ORMConfig config = ORMConfig.getInstance();
		PartitionResult pr = new PartitionResult(result.table);
		if (!config.isSingleSite()) {
			pr.setDatabase(result.dbName);
		}
		if (meta != null && config.isPartitionCreateTableInneed()) {
			try {
				processor.ensureTableExists(pr.getDatabase(), result.table, meta);
			} catch (SQLException e) {
				throw DbUtils.toRuntimeException(e);
			}
		}
		return pr;
	}

	// 笛卡尔积
	private static List<Map<String, Object>> descartes(List<Map<String, Object>> dList, Collection<?> arr1, String newField) {
		// 当新增维度的枚举数量为1时，原有的数据结构可以直接使用。
		if (arr1.size() == 1) {
			for (Map<String, Object> map : dList) {
				map.put(newField, arr1.iterator().next());
			}
			return dList;
		}
		// 最复杂的情况，新增维度枚举数量大于1，此时需要计算笛卡尔乘积
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < dList.size(); i++) {
			for (Object v : arr1) {
				Map<String, Object> map = new HashMap<String, Object>();// 重新处理原先的
				map.putAll(dList.get(i));
				map.put(newField, v);
				list.add(map);
			}
		}
		return list;
	}

	/*
	 * 这个方法会返回长度为0的数组，这一般是由于_dimVectors.size()=0引起的。
	 * 如果fieldDims>0而_dimVectors=0，说明没有录入足够的维度。而且也不是取模运算维度（因为取模运算维度是能自动补全的）
	 * 种情况要区别对待，1是应用的操作可能就是针对基表的，2是应当返回全部表供查询。
	 * 这两种情况在这里是无法判断的，因此上一级的方法要根据这两种情形，加以区分
	 */
	private Set<DbTable> getPartitionTables(ITableMetadata meta, Map<String, Dimension> fieldDims, DatabaseDialect profile) {
		// 分表向量的分析与组合.分析即将Range向量拆解为多点向量，组合即取多个向量间的笛卡尔积.(俗称维度展开并交叉)
		List<Map<String, Object>> _dimVectors = toDimensionVectors(fieldDims, meta.getMinUnitFuncForEachPartitionKey());

		String tbBaseName = meta.getTableName(true);
		tbBaseName = profile.getObjectNameIfUppercase(tbBaseName);
		PartitionTable pt = meta.getPartition();// 得到分区配置
		Set<DbTable> _tbNames = new LinkedHashSet<DbTable>();

		for (int x = 0; x < _dimVectors.size(); x++) {
			boolean isTbRegexp = false;
			boolean isDbRegexp = false;
			StringBuilder db = new StringBuilder(meta.getPartition().dbPrefix());
			StringBuilder sb = new StringBuilder(pt.appender());

			for (@SuppressWarnings("rawtypes")
			Entry<PartitionKey, PartitionFunction> entry : meta.getEffectPartitionKeys()) {
				PartitionKey key = entry.getKey();
				String field = key.field();
				Object obj = _dimVectors.get(x).get(field);
				if (key.isDbName()) {
					if (appendSuffix(obj, key, entry.getValue(), db, profile, "[a-zA-Z0-9-:]")) {
						isDbRegexp = true;
					}
				} else {
					if (appendSuffix(obj, key, entry.getValue(), sb, profile, "[a-zA-Z0-9]")) {
						isTbRegexp = true;
					}
				}
			}
			String siteName = db.length() == 0 ? null : MetaHolder.getMappingSite(db.toString());
			if (sb.length() == pt.appender().length())
				sb.setLength(0);
			DbTable dt = new DbTable(siteName, tbBaseName + sb.toString(), isTbRegexp, isDbRegexp);
			_tbNames.add(dt);
		}
		return _tbNames;
	}

	/**
	 * 在指定的字符串后面加上通过分表函数计算得到的后缀。
	 * 
	 * @param obj
	 * @param key
	 * @param func
	 * @param sb
	 * @return true/false 即当前匹配是否为一个正则表达式
	 *         什么情况下会返回正则表达式？就是当维度值obj为null的情况下，无法根据维度值来计算表名后缀。
	 *         这样的话就将可能出现的字符串用正则表达式的方式返回上一层，上一层可以根据当前数据库的实际情况来计算出能匹配上这个正则表达式的表名
	 *         
	 *         
	 *         
	 *         
	 *         
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean appendSuffix(Object obj, PartitionKey key, PartitionFunction func, StringBuilder sb, DatabaseDialect profile, String regexp) {
		if (obj == null) {
			if (key.defaultWhenFieldIsNull().length() > 0) {
				sb.append(profile.getObjectNameIfUppercase(toFixedLength(key.defaultWhenFieldIsNull(), key.filler(), key.length())));
			} else { // 将其转换为正则表示匹配
				if (isNumberFun(key)) {
					sb.append("\\d");
				} else {
					sb.append(regexp);
				}
				if (key.length() > 0) {
					sb.append("{" + key.length() + "}");
				} else {
					sb.append("+");
				}
				return true;
			}
		} else if(obj instanceof RegexpDimension){
			String baseExp=((RegexpDimension) obj).getBaseExp();
			//<0 无穷 ==0刚好 >0不足
			int leftLen=-1; //剩余可变长度
			if(key.length()>0){
				leftLen=key.length()-baseExp.length();
				if(leftLen<0){
					baseExp = baseExp.substring(0,key.length());
					leftLen=0;//刚好
				}
			}
			sb.append(baseExp);
			if(leftLen!=0){
				if (isNumberFun(key)) {
					sb.append("\\d");
				} else {
					sb.append(regexp);
				}
				if (leftLen > 0) {
					sb.append("{" + leftLen + "}");
				} else {
					sb.append("+");
				}	
			}
			return true;
		} else {
			String value = func.eval(obj);
			sb.append(toFixedLength(value, key.filler(), key.length()));
		}
		return false;
	}

	/*
	 * 完成以下两步运算——
	 * 1 维度展开
	 * 2 维度交叉
	 * 计算一切可能的分表组合 即当查询中包含的维度之间取笛卡尔积,来确保每种情况下的分表组合都被计算到
	 * 当查询条件中存在多个维度时，实质上构成了一个n维矢量空间，每个维度上的矢量进行组合，得到所有可能的复合矢量。
	 */
	private List<Map<String, Object>> toDimensionVectors(Map<String, Dimension> metaDimension, @SuppressWarnings("rawtypes") Multimap<String, PartitionFunction> fieldKeyFn) {
		List<Map<String, Object>> _dimVectors = null;
		for (Entry<String, Dimension> entry : metaDimension.entrySet()) {
			String fieldName = entry.getKey();
			Dimension vObjs = entry.getValue();

			Collection<?> enums;
			try {
				enums = vObjs.toEnumationValue(fieldKeyFn.get(fieldName));
			} catch (UnsupportedOperationException e) {
				LogUtil.error("The field " + fieldName + " is not avaliable to enums.");
				throw e;
			}
			if (_dimVectors == null) {// 建立初始维度
				_dimVectors = new ArrayList<Map<String, Object>>();
				for (Object v : enums) {
					Map<String, Object> tMap = new HashMap<String, Object>(4);
					tMap.put(fieldName, v);
					_dimVectors.add(tMap);
				}
			} else {// 叠加维度（ //笛卡尔积
				_dimVectors = descartes(_dimVectors, enums, fieldName);
			}
		}
		return _dimVectors;
	}

	/**
	 * 获取维度的值
	 * 
	 * @param keys
	 * @param instance
	 * @param q
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map<String, Dimension> getPartitionFieldValues(ITableMetadata meta, BeanWrapper instance, Query<?> q) {
		Entry<PartitionKey, PartitionFunction>[] keys=meta.getEffectPartitionKeys();
		// 获取分表向量
		Map<String, Dimension> fieldVal = new HashMap<String, Dimension>();
		for (Entry<PartitionKey, PartitionFunction> entry : keys) {
			String field = entry.getKey().field();
			// 如果存在一个field多个key function,只读取一次field值
			if (fieldVal.containsKey(field)) {
				continue;
			}
			Dimension obj = null;
			// 获取分表维度值
			if (field.startsWith("attr:")) {
				if (q != null) {
					String name = field.substring(5).trim();
					obj = new RangeDimension((Comparable) q.getAttribute(name));
				} else {
					obj = RangeDimension.EMPTY_RANGE;
				}
			} else {
				if (q != null) {
					obj = findConditionValuesByName(((QueryImpl<?>) q).conditions, field, false);
				}
				if (obj == null){
					Object term=instance.getPropertyValue(field);
					// 当相等条件时
					if(term!=null){
						Class<?> clz=instance.getPropertyRawType(field);
						if(clz.isPrimitive()){//如果是缺省值，当做null处理。
							if(ObjectUtils.equals(term, BeanUtils.defaultValueOfPrimitive(clz))){//如果是和原生值一样
								IQueryableEntity qq=(IQueryableEntity)instance.getWrapped();
								Field fld=meta.getField(field);
								if(!(qq).isUsed(fld)){
									term=null;
								}
							}
						}	
					}
					obj = new RangeDimension((Comparable) term);
					
				}
			}
			fieldVal.put(field, obj);
		}
		return fieldVal;
	}
	


	/**
	 * 根据一个分表字段名称查找属于该字段的维度
	 * 
	 * @param q
	 * @param field
	 * @return
	 */
	public static Dimension findConditionValuesByName(QueryImpl<?> q, String field) {
		return findConditionValuesByName(q.conditions, field, false);
	}

	private static Dimension findConditionValuesByName(Iterable<Condition> conditions, String field, boolean isOr) {
		Dimension result = null;
		for (Condition c : conditions) {
			if (c == Condition.AllRecordsCondition) {
				continue;
			}
			Dimension d = findDimensionByName(field, c);
			if (d == null) {
				continue;
			}
			if (result == null) {
				result = d;
			} else if (isOr) {
				result = result.mergeOr(d);
			} else {
				result = result.mergeAnd(d);
			}
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Dimension findDimensionByName(String field, Condition c) {
		Field f = c.getField();
		if (f instanceof IConditionField) {
			if (f instanceof Not) {
				Dimension d = findConditionValuesByName(((IConditionField) f).getConditions(), field, false);
				if (d != null)
					return d.mergeNot();
			} else if (f instanceof NotExists || f instanceof Exists) {// 不支持，忽略
				return null;
			} else if (f instanceof And) {
				return findConditionValuesByName(((IConditionField) f).getConditions(), field, false);
			} else if (f instanceof Or) {
				return findConditionValuesByName(((IConditionField) f).getConditions(), field, true);
			} else {// 不支持
				return null;
			}
		}
		String name = c.getField().name();
		if (!field.equals(name))
			return null;

		Operator op = c.getOperator();
		if (op == null)
			op = Operator.EQUALS;
		Object v = c.getValue();

		switch (op) {
		case EQUALS:
			return new RangeDimension((Comparable) v);
		case IN:
			return getAsPointsDimension(v);
		case BETWEEN_L_L:
			Object min;
			Object max;
			if (v.getClass().isArray()) {
				min = Array.get(v, 0);
				max = Array.get(v, 1);
			} else if (v instanceof Collection) {
				Object[] vo = ((Collection) v).toArray();
				min = vo[0];
				max = vo[1];
			} else {
				throw new IllegalArgumentException();
			}
			return new RangeDimension((Comparable) min, (Comparable) max);
		case GREAT:
			return new RangeDimension((Comparable) v, null, false, false);
		case GREAT_EQUALS:
			return new RangeDimension((Comparable) v, null);
		case LESS:
			return new RangeDimension(null, (Comparable) v, false, false);
		case LESS_EQUALS:
			return new RangeDimension(null, (Comparable) v);
		case MATCH_START:
			return new RegexpDimension(String.valueOf(v));
		case NOT_EQUALS: {
			Dimension d = new RangeDimension((Comparable) v);
			;
			if (d != null)
				d = d.mergeNot();
			return d;
		}
		case NOT_IN: {
			Dimension d = getAsPointsDimension(v);
			if (d != null)
				d = d.mergeNot();
			return d;
		}
		default:// 包含其他几个IS NULL 和 match等条件，忽略
			return null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Dimension getAsPointsDimension(Object v) {
		Class<?> type = v.getClass();
		if (type.isArray()) {
			if (type.getComponentType().isPrimitive()) {
				return ComplexDimension.create((Comparable[]) ArrayUtils.toObject(v));
			} else {
				return ComplexDimension.create((Comparable[]) v);
			}
		} else if (v instanceof Collection) {
			Collection cv = (Collection) v;
			return ComplexDimension.create((Comparable[]) cv.toArray(new Comparable[cv.size()]));
		} else {
			throw new IllegalArgumentException("Invalid condition value, must be array or collection..");
		}
	}

	private static String toFixedLength(String value, char filler, int length) {
		if (length <= 0)
			return value;
		if (value.length() > length) {
			return value.substring(0, length);
		}
		return StringUtils.leftPad(value, length, filler);
	}

	private List<String> getMatchedExistDbNames(PartitionSupport processor, String regexp) {
		Pattern key = StringUtils.isEmpty(regexp) ? null : Pattern.compile(regexp);
		List<String> result = new ArrayList<String>();
		for (String s : processor.getDdcNames()) {
			if (key == null || key.matcher(s).matches()) {
				result.add(s);
			}
			;
		}
		return result;
	}

	private List<DbTable> analyzeRegexpResults(Set<DbTable> r, PartitionSupport processor, MetadataAdapter meta, DatabaseDialect profile) {
		List<DbTable> list = new ArrayList<DbTable>();
		for (DbTable e : r) {
			// 对于返回的每个分表分库结果，计算其分库名的正则表达式
			List<String> dbs = null;
			if (e.isDbRegexp) {
				dbs = getMatchedExistDbNames(processor, e.dbName);
				if (dbs.isEmpty()) {
					dbs.add(null);
				}
			} else {
				dbs = Arrays.asList(e.dbName);
			}
			// 计算完成，得到每个分表结果的匹配数据库集
			for (String dbname : dbs) {
				if (e.isTbRegexp) {
					for (String s : getMatchedExistTableNames(processor, Pattern.compile(e.table), dbname, meta)) {
						list.add(new DbTable(dbname, s));
					}
				} else {
					list.add(new DbTable(dbname, e.table));
				}
			}
		}
		if (list.size() > 0) {
			return list;
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * 重新分组并返回结果
	 * 
	 * @param result
	 * @return
	 */
	private PartitionResult[] toPartitionResult(List<DbTable> result, PartitionSupport support, boolean filter, ITableMetadata tmeta) {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		boolean singleSite= ORMConfig.getInstance().isSingleSite();
		for (DbTable dt : result) {
			String dbName = singleSite? "" : dt.dbName;
			if (filter && !support.isExist(dbName, dt.table, tmeta)) {
				System.out.println("过滤掉"+dt);
				continue;
			}
			List<String> list = map.get(dbName);
			if (list == null) {
				list = new ArrayList<String>();
				map.put(dbName, list);
			}
			list.add(dt.table);
		}
		PartitionResult[] pr = new PartitionResult[map.size()];
		int n = 0;
		for (Entry<String, List<String>> e : map.entrySet()) {
			PartitionResult p = new PartitionResult();
			p.setDatabase(e.getKey());
			p.setTables(e.getValue());
			pr[n++] = p;
		}
		return pr;
	}

	@SuppressWarnings("unchecked")
	private Collection<String> getMatchedExistTableNames(PartitionSupport processor, Pattern key, String dbName, ITableMetadata meta) {
		try {
			Collection<String> allTbs = processor.getSubTableNames(dbName, meta);
			List<String> result = new ArrayList<String>();
			for (String s : allTbs) {
				if (key.matcher(s).matches()) {
					result.add(s);
				}
			}
			return result;
		} catch (SQLException e) {
			return Collections.EMPTY_LIST;
		}
	}

	private static KeyFunction[] NUM_FUNS = new KeyFunction[] { KeyFunction.DAY, KeyFunction.HH24, KeyFunction.MODULUS, KeyFunction.MONTH, KeyFunction.YEAR, KeyFunction.YEAR_LAST2, KeyFunction.YEAR_MONTH };

	public static boolean isNumberFun(PartitionKey key) {
		if (ArrayUtils.contains(NUM_FUNS, key.function())) {
			return true;
		}
		if (ModulusFunction.class.isAssignableFrom(key.functionClass())) {
			return true;
		}
		return false;
	}
}
