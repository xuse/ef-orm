/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
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
package jef.database.meta;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Table;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.database.DebugUtil;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.OperateTarget;
import jef.database.PojoWrapper;
import jef.database.annotation.BindDataSource;
import jef.database.annotation.PartitionFunction;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;
import jef.database.dialect.ColumnType;
import jef.database.dialect.type.ColumnMapping;
import jef.database.routing.function.AbstractDateFunction;
import jef.database.routing.function.MapFunction;
import jef.database.routing.function.ModulusFunction;
import jef.database.routing.function.RawFunc;
import jef.tools.ArrayUtils;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

@SuppressWarnings("rawtypes")
public final class TableMetadata extends AbstractMetadata {

	/**
	 * 记录当前Schema所对应的DO类。
	 */
	private Class<?> thisType;
	private BeanAccessor pojoAccessor;
	
	private Class<? extends IQueryableEntity> containerType;
	BeanAccessor containerAccessor;

	// ///////////////分表配置信息//////////////////////
	private PartitionTable partition;// 分表策略
	
	//记录在每个字段上的函数，用来进行分表估算的时的采样
	private Multimap<String, PartitionFunction> partitionFuncs;
	
	
	private Entry<PartitionKey, PartitionFunction>[] effectPartitionKeys;

	private List<ColumnMapping> pkFields;// 记录主键列


	private final Map<Field, String> fieldToColumn = new IdentityHashMap<Field, String>();// 提供Field到列名的转换
	private final Map<String, String> lowerColumnToFieldName = new HashMap<String, String>();// 提供Column名称到Field的转换，不光包括元模型字段，也包括了非元模型字段但标注了Column的字段(key全部存小写)

	TableMetadata(Class<? extends IQueryableEntity> clz, AnnotationProvider annos) {
		this.containerType = clz;
		this.containerAccessor=FastBeanWrapperImpl.getAccessorFor(clz);
		this.thisType = clz;
		this.pkFields = Collections.emptyList();
		initByAnno(clz, annos.getAnnotation(Table.class),annos.getAnnotation(BindDataSource.class));
	}

	TableMetadata(Class<PojoWrapper> varClz, Class<?> clz, AnnotationProvider annos) {
		this.containerType = varClz;
		this.containerAccessor=FastBeanWrapperImpl.getAccessorFor(varClz);
		this.thisType = clz;
		this.pojoAccessor = FastBeanWrapperImpl.getAccessorFor(clz);
		this.pkFields = Collections.emptyList();
		initByAnno(clz,annos.getAnnotation(Table.class),annos.getAnnotation(BindDataSource.class));
	}

	/**
	 * 得到分表配置
	 */
	public PartitionTable getPartition() {
		return partition;
	}

	/**
	 * 设置和解析数据分片（分库分表）配置
	 * 
	 * @param t
	 */
	synchronized void setPartition(PartitionTable t) {
		if (t == null)
			return;
		effectPartitionKeys = withFunction(t.key());
		if (effectPartitionKeys.length == 0) {
			effectPartitionKeys = null;
			return;
		}
		this.partition = t;
		// 开始计算在同一个字段上的最小的分表参数单元
		Multimap<String, PartitionFunction> fieldKeyFn = ArrayListMultimap.create();
		for (Entry<PartitionKey, PartitionFunction> entry : getEffectPartitionKeys()) {
			PartitionKey key = entry.getKey();
			String field = key.field();
			if(entry.getValue() instanceof AbstractDateFunction){
				Collection<PartitionFunction> olds=fieldKeyFn.get(field);
				if(olds!=null){
					for(PartitionFunction<?> old:olds){
						if (old instanceof AbstractDateFunction){
							int oldLevel = ((AbstractDateFunction) old).getTimeLevel();
							int level = ((AbstractDateFunction) entry.getValue()).getTimeLevel();// 取最小的时间单位
							if (level < oldLevel) {
								fieldKeyFn.remove(field, old);
								break;//可以合并
							}else{
								continue;//无需合并
							}
						}
					}
				}
			}
			fieldKeyFn.put(field, entry.getValue());
		}
		partitionFuncs = fieldKeyFn;
	}

	public Class<?> getThisType() {
		return thisType;
	}

	public Class<? extends IQueryableEntity> getContainerType() {
		return containerType;
	}

	public List<jef.database.annotation.Index> getIndexDefinition() {
		return indexMap;
	}

	public List<Field> getPKField() {
		return new AbstractList<Field>(){
			@Override
			public Field get(int index) {
				return pkFields.get(index).field();
			}

			@Override
			public int size() {
				return pkFields.size();
			}
		};
	}

	public List<ColumnMapping> getPKFields() {
		return pkFields;
	}
	/**
	 * 将一个Java Field加入到列定义中
	 * 
	 * @param field
	 * @param column
	 */
	public void putJavaField(Field field, ColumnMapping type, String columnName,boolean isPk) {
		fields.put(field.name(), field);
		lowerFields.put(field.name().toLowerCase(), field);
	
		fieldToColumn.put(field, columnName);
		String lastFieldName=lowerColumnToFieldName.put(columnName.toLowerCase(), field.name());
		if(lastFieldName!=null && !field.name().equals(lastFieldName)){
			throw new IllegalArgumentException(String.format("The field [%s] and [%s] in [%s] have a duplicate column name [%s].",lastFieldName,field.name(),containerType.getName(),columnName));
		}
	
		if (isPk) {
			type.setPk(true);
			List<ColumnMapping> newPks = new ArrayList<ColumnMapping>(pkFields);
			newPks.add(type);
			Collections.sort(newPks, new Comparator<ColumnMapping>() {
				public int compare(ColumnMapping o1, ColumnMapping o2) {
					Integer i1=-1;
					Integer i2=-1;
					if(o1 instanceof Enum){
						i1=((Enum<?>) o1.field()).ordinal();
					}
					if(o2 instanceof Enum){
						i2=((Enum<?>) o2.field()).ordinal();
					}
					return i1.compareTo(i2);
				}
			});
			this.pkFields = Arrays.<ColumnMapping>asList(newPks.toArray(new ColumnMapping[newPks.size()]));
		}
		schemaMap.put(field, type);
		super.updateAutoIncrementAndUpdate(type);
		if (type.isLob()) {
			lobNames = ArrayUtils.addElement(lobNames, field, jef.database.Field.class);
		}
	}

	/**
	 * 添加一个索引定义。仅用于自动建表，不会对其作特殊的判断和处理。
	 * 
	 * @param fields
	 * @param comment
	 * @deprecated 向下兼容保留
	 */
	public void putIndex(Field[] fields, String comment) {
		Map<String, Object> data = new HashMap<String, Object>(4);
		String[] fieldnames = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			fieldnames[i] = fields[i].name();
		}
		data.put("fields", fieldnames);
		data.put("definition", StringUtils.toString(comment));
		data.put("name", "");
		jef.database.annotation.Index index = BeanUtils.asAnnotation(jef.database.annotation.Index.class, data);
		indexMap.add(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Entity: [").append(containerType.getName()).append("]\n");
		for (ColumnMapping m:schemaMap.values()) {
			String fname=m.fieldName();
			sb.append("  ").append(fname);
			StringUtils.repeat(sb, ' ', 10-fname.length());
			sb.append('\t').append(m.get());
			sb.append("\n");
		}
		sb.setLength(sb.length()-1);
		return sb.toString();
	}
	
	/**
	 * 获取当前生效的分区策略
	 * 注意生效的策略默认等同于Annotation上的策略，但是实际上如果配置了/partition-conf.properties后
	 * ，生效字段受改配置影响 {@link #partitPolicy}
	 * 
	 * @return
	 */
	public Entry<PartitionKey, PartitionFunction>[] getEffectPartitionKeys() {
		return effectPartitionKeys;
	}

	private Entry<PartitionKey, PartitionFunction>[] withFunction(PartitionKey[] key) {
		@SuppressWarnings("unchecked")
		Entry<PartitionKey, PartitionFunction>[] result = new Entry[key.length];
		for (int i = 0; i < key.length; i++) {
			result[i] = new Entry<PartitionKey, PartitionFunction>(key[i], createFunc(key[i]));
		}
		return result;
	}

	private static PartitionFunction<?> createFunc(PartitionKey value) {
		if (value.functionClass() != PartitionFunction.class) {
			try {
				String[] params = value.functionConstructorParams();
				if (params.length == 0) {
					return value.functionClass().newInstance();
				} else {
					Class[] clz = new Class[params.length];
					for (int i = 0; i < params.length; i++) {
						clz[i] = String.class;
					}
					Constructor cc = value.functionClass().getConstructor(clz);
					cc.setAccessible(true);
					return (PartitionFunction<?>) cc.newInstance((Object[]) params);
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
		switch (value.function()) {
		case DAY:
			return AbstractDateFunction.DAY;
		case HH24:
			return AbstractDateFunction.HH24;
		case MODULUS:
			if(value.functionConstructorParams().length==0 || StringUtils.isEmpty(value.functionConstructorParams()[0])){
				return ModulusFunction.getDefault();
			}else{
				return new ModulusFunction(value.functionConstructorParams()[0]);
			}
		case MONTH:
			return AbstractDateFunction.MONTH;
		case YEAR:
			return AbstractDateFunction.YEAR;
		case YEAR_LAST2:
			return AbstractDateFunction.YEAR_LAST2;
		case YEAR_MONTH:
			return AbstractDateFunction.YEAR_MONTH;
		case YEAR_MONTH_DAY:
			return AbstractDateFunction.YEAR_MONTH_DAY;
		case WEEKDAY:
			return AbstractDateFunction.WEEKDAY;
		case RAW:
			return new RawFunc(value.defaultWhenFieldIsNull(),value.length());
		case MAPPING:
			if(value.functionConstructorParams().length==0){
				throw new IllegalArgumentException("You must config the 'functionConstructorParams' while using funcuon Map");
			}
			int num=0;
			if(value.functionConstructorParams().length>1){
				num=StringUtils.toInt(value.functionConstructorParams()[1],0);
			}
			return new MapFunction(value.functionConstructorParams()[0],num);
		default:
			throw new IllegalArgumentException("Unknown KeyFunction:" + value.function());
		}
	}
	

	public Multimap<String, PartitionFunction> getMinUnitFuncForEachPartitionKey() {
		return partitionFuncs;
	}

	/**
	 * 添加一个非元模型的 Column映射字段（一般用于分表辅助）
	 * 
	 * @param name
	 * @param column
	 */
	public void addNonMetaModelFieldMapping(String field, Column column) {
		String name = column.name();
		if (StringUtils.isEmpty(name)) {
			name = field;
		}
		lowerColumnToFieldName.put(name.toLowerCase(), field);
	}

	void setTableName(String tableName) {
		this.tableName = tableName;
	}

	void setSchema(String schema) {
		this.schema = schema;
	}

	// 会将LOB移动到最后
	public List<ColumnMapping> getColumns() {
		if (metaFields == null) {
			ColumnMapping[] fields = schemaMap.values().toArray(new ColumnMapping[schemaMap.size()]);
			Arrays.sort(fields, new Comparator<ColumnMapping>() {
				public int compare(ColumnMapping field1, ColumnMapping field2) {
					Class<? extends ColumnType> type1 = field1.get().getClass();
					Class<? extends ColumnType> type2 = field1.get().getClass();
					Boolean b1 = (type1 == ColumnType.Blob.class || type1 == ColumnType.Clob.class);
					Boolean b2 = (type2 == ColumnType.Blob.class || type2 == ColumnType.Clob.class);
					return b1.compareTo(b2);
				}
			});
			metaFields = Arrays.asList(fields);
		}
		return metaFields;
	}

	/**
	 * 根据一个指定的实际数据库，核对metaData中的字段，如果发现某些可选的字段数据库里没有，就从元模型中删除来适应数据库
	 * 
	 * @param db
	 * @throws SQLException
	 */
	public synchronized void removeNotExistColumns(OperateTarget db) throws SQLException {
		Set<String> set = DebugUtil.getColumnsInLowercase(db, getTableName(true));
		List<Field> removeColumn = new ArrayList<Field>();
		for (Field field : fieldToColumn.keySet()) {
			String column = fieldToColumn.get(field).toLowerCase();
			if (!set.contains(column)) {
				removeColumn.add(field);
			}
		}
		for (Field field : removeColumn) {
			schemaMap.remove(field);
			// FIXME, others are not removed
			LogUtil.show("The field [" + field.name() + "] was remove since column not exist in db.");
		}
		if (removeColumn.size() > 0)
			metaFields = null;
	}

	public IQueryableEntity newInstance() {
		if (pojoAccessor != null) {
			return new PojoWrapper(pojoAccessor.newInstance(), pojoAccessor, this, false);
		} else {
			return (IQueryableEntity) containerAccessor.newInstance();
		}
	}

	public String getName() {
		return thisType.getName();
	}

	public String getSimpleName() {
		return thisType.getSimpleName();
	}

	public Field getFieldByLowerColumn(String columnLowercase) {
		return fields.get(lowerColumnToFieldName.get(columnLowercase));
	}

	public PojoWrapper transfer(Object p, boolean isQuery) {
		if (p == null)
			return null;
		if (p instanceof IQueryableEntity) {
			throw new IllegalArgumentException();
		}
		if (p.getClass() == this.thisType) {
			return new PojoWrapper(p, pojoAccessor, this, isQuery);
		} else {
			throw new IllegalArgumentException(p.getClass()+" != " + this.thisType);
		}
	}

	public EntityType getType() {
		return this.containerType==PojoWrapper.class?EntityType.POJO:EntityType.NATIVE;
	}
	
	private List<Class> parents;
	public void addParent(Class<?> processingClz) {
		if(parents==null){
			parents=new ArrayList<Class>(3);
		}
		parents.add(processingClz);
		
	}
//	
//
//	public boolean isAssignableFrom(ITableMetadata type) {
//		return this == type || this.containerType.isAssignableFrom(type.getThisType());
//	}
	
	public boolean containsMeta(ITableMetadata type) {
		if(type==this)return true;
		if(parents==null)return false;
		for(Class clz:parents){
			if(type.getThisType()==clz){
				return true;
			}
		}
		return false;
	}

	@Override
	public BeanAccessor getContainerAccessor() {
		return containerAccessor;
	}
	
	TupleMetadata extendMeta;
	TupleMetadata extendContainer;

	@Override
	public TupleMetadata getExtendsTable() {
		return extendContainer;
	}

	@Override
	public Collection<ColumnMapping> getExtendedColumns() {
		return extendMeta.getColumnSchema();
	}

	@Override
	public ColumnMapping getExtendedColumnDef(String field) {
		return extendMeta.getColumnDef(extendMeta.getField(field));
	}
}
