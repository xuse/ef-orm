package jef.database.wrapper.populator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.database.IQueryableEntity;
import jef.database.Session.PopulateStrategy;
import jef.database.VarObject;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.SqlContext;
import jef.database.wrapper.populator.Mappers.BeanMapper;
import jef.script.javascript.Var;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.StringUtils;

/**
 * 查询结果转换器。
 * 用于描述对查询结果的转换（拼装）策略
 * 
 * 用户可以使用此类——
 * <ul>
 * <li>{@linkplain #setResultType(Class)} | {@linkplain #setResultType(ITableMetadata)}<br>指定查询的操作结果</li>
 * <li>{@linkplain #setLoadVsMany(boolean)}<br>指定是否加载对多关联的字段</li>
 * <li>{@link #setLoadVsOne(boolean)}<br>指定是否加载对一关联的字段</li>
 * <li>{@link #addMapper(Mapper)}<br>指定自定义的映射器，例子参见 {@link Mappers}</li>
 * <li>{@link #ignoreAll()} | {@link #ignoreColumn(String)} | {@link #ignoreSchema(String)}<br>让框架在拼装结果时忽略指定的列</li>
 * </ul>
 * 
 * @author jiyi
 * 
 */
public class Transformer {

	/**
	 * @see PopulateStrategy
	 */
	private PopulateStrategy[] strategy;

	/**
	 * 返回的对象类
	 */
	Class<?> resultClazz;

	/**
	 * 如果resultClazz是VarObject，那么指定TableMetadata
	 */
	ITableMetadata meta;

	/**
	 * 默认情况下，ORM总是根据结果类的字段名（列名）去对应查询结果中的各个字段，从而映射出返回的对象。<br>
	 * 一旦ignoreAll，ORM将不再使用任何内建映射策略，而是完全以用户自定义的Mapper来映射
	 */
	private boolean ignoreAll;
	/**
	 * 让内建映射规则忽略这些列。
	 */
	private Set<String> ignoreColumns; // 所有不需要系统默认拼装的列的名称 大写
	/**
	 * 让内建映射规则忽略这些别名的前缀。例如 select id as T1__ID ...这里两个下划线前面的T1就是前缀。
	 */
	private Set<String> ignoreSchemas; // 所有不需要系统默认拼装的列的名称 大写

	/**
	 * 自定义的映射器。可以自己编写代码实现，也可以使用{@link Mappers}里的工具方法自动生成。
	 * 
	 * @see Mappers
	 */
	private List<Mapper<?>> mapper;// replace populator
	/**
	 * 记录自定义的映射器所写入的字段名，可能为null，数量总是和mapper的数量一致
	 */
	private List<String> mapperProperties;
	/**
	 * 是否加载対一关联
	 */
	private boolean loadVsOne;
	/**
	 * 是否加载对多关联
	 */
	private boolean loadVsMany;

	public static final Transformer VAR = new Transformer(Var.class);

	/**
	 * 空构造
	 */
	public Transformer() {
	}

	/**
	 * 构造一个Transformer
	 * 
	 * @param clz 返回结果类型
	 * @param strategies 返回结果转换策略
	 */
	public Transformer(Class<?> clz, PopulateStrategy... strategies) {
		this.strategy = strategies;
		setResultType(clz);
	}

	/**
	 * 构造一个ResultTransformer
	 * 
	 * @param meta 返回结果类型模型
	 * @param strategies 返回结果转换策略
	 */
	public Transformer(ITableMetadata meta, PopulateStrategy... strategies) {
		setResultType(meta);
		this.strategy = strategies;
	}

	/**
	 * 返回结果转换策略
	 * 
	 * @return  结果转换策略
	 */
	public PopulateStrategy[] getStrategy() {
		return strategy;
	}

	/**
	 * 设置结果转换策略
	 * 
	 * @param strategy 结果转换策略.
	 * @see PopulateStrategy
	 */
	public void setStrategy(PopulateStrategy... strategy) {
		this.strategy = strategy;
	}

	/**
	 * 增加一个结果转换策略
	 * 
	 * @param strategy
	 */
	public void addStrategy(PopulateStrategy strategy) {
		if (this.strategy == null || this.strategy.length == 0) {
			this.strategy = new PopulateStrategy[] { strategy };
		} else {
			this.strategy = ArrayUtils.addElement(this.strategy, strategy);
		}
	}

	/**
	 * 获得结果转换类
	 * 
	 * @return 结果类
	 */
	public Class<?> getResultClazz() {
		return resultClazz;
	}

	/**
	 * 如果是复杂对象，得到metadata
	 * 
	 * @return 结果是数据库实体得到metadata，如果是简单类型返回null
	 */
	public ITableMetadata getResultMeta() {
		return meta;
	}

	/**
	 * 获得排除列
	 * 
	 * @return 框架不进行转换的结果列
	 */
	public Set<String> getSkipColumns() {
		return ignoreColumns;
	}

	/**
	 * 设置查询结果排除列
	 * 
	 * @param skipColumns 排除这些列。框架在拼装结果时不会处理查询结果中的这些列。用户可以用自定义的映射器（mapper）处理这些列。
	 * @see Mapper
	 */
	public void setSkipColumns(Set<String> skipColumns) {
		this.ignoreColumns = skipColumns;
	}

	/**
	 * 获得自定义映射器
	 * 
	 * @return 所有自定义映射器
	 * @see Mapper
	 */
	@SuppressWarnings("unchecked")
	public List<Mapper<?>> getMapper() {
		return mapper == null ? Collections.EMPTY_LIST : mapper;
	}

	/**
	 * 设置自定义映射器
	 * @param mapper 自定义的映射器
	 */
	public void setMapper(List<Mapper<?>> mapper) {
		this.mapper = mapper;
	}

	/**
	 * 排除所有列。让框架在拼装结果是不处理查询中的所有列。此时只有用户自定义的映射器（Mapper）会生效。
	 */
	public void ignoreAll() {
		ignoreAll = true;
	}

	/**
	 * 让ORM默认转换器不处理该命名空间下所有列
	 * 
	 * @param schema
	 */
	public void ignoreSchema(String schema) {
		if (schema == null)
			return;
		if (ignoreSchemas == null) {
			ignoreSchemas = new HashSet<String>(4);
		}
		ignoreSchemas.add(schema.toUpperCase());
	}

	/**
	 * 让ORM默认转换器不处理该列
	 * 
	 * @param column
	 */
	public void ignoreColumn(String column) {
		if (StringUtils.isEmpty(column))
			return;
		if (ignoreColumns == null) {
			ignoreColumns = new HashSet<String>(8);
		}
		ignoreColumns.add(column.toUpperCase());
	}

	/**
	 * 让ORM默认转换器不处理这些列
	 * 
	 * @param schema 命名空间
	 * @param column 列名
	 */
	public void ignoreColumn(String schema, String column) {
		if (schema == null && column == null) {
			return;
		}
		if (schema == null) {
			ignoreColumn(column);
		} else if (column == null) {
			ignoreSchema(schema);
		} else {
			ignoreColumn(schema + SqlContext.DIVEDER + column);
		}
	}

	/**
	 * 清除所有自定义映射器.
	 */
	public void clearMapper() {
		if (mapper != null) {
			mapper.clear();
		}
		if (mapperProperties != null) {
			mapperProperties.clear();
		}
	}

	/**
	 * 添加一个自定义的映射器。Mapper可以自定实现，也可以从Mappers工具类构造。
	 * 
	 * @param accessor
	 * @see Mappers
	 */
	public void addMapper(Mapper<?> accessor) {
		if (accessor != null) {
			if (mapper == null) {
				mapper = new ArrayList<Mapper<?>>();
			}
			mapper.add(accessor);
			if (accessor instanceof Mappers.BeanMapper) {
				BeanMapper<?> s = (BeanMapper<?>) accessor;
				addMapperProperties(s.toField);
			}
		}
	}
	
	private void addMapperProperties(String value){
		if(value==null)return;
		if(mapperProperties==null){
			mapperProperties = new ArrayList<String>();
		}
		mapperProperties.add(value);
	}

	/*
	 * 内部使用
	 */
	void prepareTransform(Map<String, ColumnDescription> nameIndex) {
		if (mapper != null) {
			for (Mapper<?> m : mapper) {
				m.prepare(nameIndex);
			}
		}
	}

	/*
	 * 内部使用
	 */
	boolean hasIgnoreSchema(String schema) {
		if (ignoreAll)
			return true;
		return ignoreSchemas == null ? false : ignoreSchemas.contains(schema);
	}

	/*
	 * 内部使用
	 */
	boolean hasIgnoreColumn(String column) {
		if (ignoreAll)
			return true;
		return ignoreColumns == null ? false : ignoreColumns.contains(column);
	}

	/*
	 * 内部使用:当使用自定义查询需要返回多个结果类的时候，用这个方法来计算数组的长度。
	 */
	int getMaxMapperIndex() {
		int max = 0;
		if (mapperProperties != null) {
			for (String s : mapperProperties) {
				if (StringUtils.isNumeric(s)) {
					try {
						int index = Integer.parseInt(s);
						if (index > max)
							max = index;
					} catch (NumberFormatException e) {// Skip
					}
				}
			}
		}
		return max;
	}

	/*
	 * 内部使用
	 */
	public boolean hasStrategy(PopulateStrategy ps) {
		if (strategy == null)
			return false;
		for (int i = 0; i < strategy.length; i++) {
			if (strategy[i] == ps) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否加载対多关联
	 * 
	 * @return 否加载対多关联
	 */
	public boolean isLoadVsMany() {
		return loadVsMany;
	}

	/**
	 * 设置是否加载对多关联
	 * 
	 * @param loadVsMany 是否加载对多关联
	 */
	public void setLoadVsMany(boolean loadVsMany) {
		this.loadVsMany = loadVsMany;
	}

	/**
	 * 是否加载対一关联
	 * 
	 * @return 是否加载対一关联
	 */
	public boolean isLoadVsOne() {
		return loadVsOne;
	}

	/**
	 * 设置是否加载対一关联
	 * @param loadVsOne 是否加载対一级联
	 */
	public void setLoadVsOne(boolean loadVsOne) {
		this.loadVsOne = loadVsOne;
	}

	/**
	 * 询问是否为数据库实体。
	 * 
	 * @return 是否为数据库实体。
	 */
	public boolean isQueryableEntity() {
		return meta != null;
	}

	/**
	 * 询问是否为动态对象实体
	 * 
	 * @return 是否为动态对象实体
	 */
	public boolean isVarObject() {
		if (resultClazz == VarObject.class) {
			Assert.notNull(meta);
			return true;
		}
		return false;
	}

	/**
	 * 设置返回结果的类型
	 * 
	 * @param clz 查询要返回的结果类型
	 */
	public void setResultType(Class<?> clz) {
		if (clz == null)
			return;
		this.resultClazz = clz;
		if (IQueryableEntity.class.isAssignableFrom(clz)) {
			meta = MetaHolder.getMeta(resultClazz.asSubclass(IQueryableEntity.class));
		} else {
			meta = null;
		}
	}

	/**
	 * 设置返回结果的类型
	 * 
	 * @param meta 询要返回的结果类型
	 */
	public void setResultType(ITableMetadata meta) {
		if (meta == null)
			return;
		this.resultClazz = meta.getThisType();
		this.meta = meta;
	}
	
	/**
	 * 将结果的返回类型设置为Object数组，同时指定数组的最大长度
	 * @param size
	 */
	public void setResultTypeAsObjectArray(int size){
		this.resultClazz=Object[].class;
		addMapperProperties(String.valueOf(size-1));
	}
}
