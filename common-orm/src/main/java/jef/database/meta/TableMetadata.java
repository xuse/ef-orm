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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.database.DbUtils;
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
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AbstractTimeMapping;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.ColumnMappings;
import jef.database.dialect.type.MappingType;
import jef.database.query.JpqlExpression;
import jef.database.query.ReferenceType;
import jef.database.query.RegexpDimension;
import jef.database.routing.function.AbstractDateFunction;
import jef.database.routing.function.MapFunction;
import jef.database.routing.function.ModulusFunction;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.UnsafeUtils;
import jef.tools.string.CharUtils;
import jef.tools.string.StringIterator;

import org.apache.commons.lang.ObjectUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

@SuppressWarnings("rawtypes")
public final class TableMetadata extends MetadataAdapter {

	/**
	 * 记录当前Schema所对应的DO类。
	 */
	private Class<? extends IQueryableEntity> containerType;
	private Class<?> thisType;
	private BeanAccessor metaAccessor;

	// ///////////////分表配置信息//////////////////////
	private PartitionTable partition;// 分表策略
	
	//记录在每个字段上的函数，用来进行分表估算的时的采样
	private Multimap<String, PartitionFunction> partitionFuncs;
	
	
	private Entry<PartitionKey, PartitionFunction>[] effectPartitionKeys;

	private List<Field> pkFields;// 记录主键列

	// //////////列名/字段索引//////////////////////
	private final Map<Field, MappingType<?>> schemaMap = new IdentityHashMap<Field, MappingType<?>>(); // 提供Field到字段类型的转换

	private final Map<Field, String> fieldToColumn = new IdentityHashMap<Field, String>();// 提供Field到列名的转换
	private final Map<String, String> lowerColumnToFieldName = new HashMap<String, String>();// 提供Column名称到Field的转换，不光包括元模型字段，也包括了非元模型字段但标注了Column的字段(key全部存小写)
	private final Map<String, Field> fields = new HashMap<String, Field>(); // 提供field名称文本到field对象的转换
	private final Map<String, Field> lowerFields = new HashMap<String, Field>(); // 提供field名称文本到field对象的转换

	// /////////引用索引/////////////////
	private final Map<String, AbstractRefField> refFieldsByName = new HashMap<String, AbstractRefField>();// 记录所有关联和引用字段referenceFields
	private final Map<Reference, List<AbstractRefField>> refFieldsByRef = new HashMap<Reference, List<AbstractRefField>>();// 记录所有的引用字段，按引用关系

	private List<MappingType<?>> metaFields;

	TableMetadata(Class<? extends IQueryableEntity> clz, AnnotationProvider annos) {
		this.containerType = clz;
		this.thisType = clz;
		this.pkFields = Collections.emptyList();
		initByAnno(clz, annos.getAnnotation(Table.class),annos.getAnnotation(BindDataSource.class));
	}

	TableMetadata(Class<PojoWrapper> varClz, Class<?> clz, AnnotationProvider annos) {
		this.containerType = varClz;
		this.thisType = clz;
		this.metaAccessor = FastBeanWrapperImpl.getAccessorFor(clz);
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

	public MappingType<?> getColumnDef(jef.database.Field field) {
		return schemaMap.get(field);
	}

	public List<jef.database.annotation.Index> getIndexSchema() {
		return indexMap;
	}

	public List<Field> getPKField() {
		return pkFields;
	}

	/**
	 * 按照引用的关系获取所有关联字段
	 * 
	 * @return
	 */
	public Map<Reference, List<AbstractRefField>> getRefFieldsByRef() {
		return refFieldsByRef;
	}

	/**
	 * 按照名称获得所有关联字段
	 * 
	 * @return
	 */
	public Map<String, AbstractRefField> getRefFieldsByName() {
		return refFieldsByName;
	}

	private Field[] lobNames;

	/**
	 * 将一个Java Field加入到列定义中
	 * 
	 * @param field
	 * @param column
	 */
	public void putJavaField(Field field, ColumnType column, Id annoId, javax.persistence.Column a) {
		fields.put(field.name(), field);
		lowerFields.put(field.name().toLowerCase(), field);

		// 计算列名并备份
		// FieldEx f = BeanUtils.getField(metaType, field.name());
		// Assert.notNull(f, "There's no field named '" + field.name() +
		// "' in class " + metaType.getName());

		String cName;
		if (a != null && a.name().length() > 0) {
			cName = a.name().trim();
		} else {
			cName = field.name();

		}
		fieldToColumn.put(field, cName);
		String lastFieldName=lowerColumnToFieldName.put(cName.toLowerCase(), field.name());
		if(lastFieldName!=null && !field.name().equals(lastFieldName)){
			throw new IllegalArgumentException(String.format("The field [%s] and [%s] in [%s] have a duplicate column name [%s].",lastFieldName,field.name(),containerType.getName(),cName));
		}
		MappingType<?> type;
		try {
			type = ColumnMappings.getMapping(field, this, cName, column, false);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(this.getName() + ":" + field.name() + " can not mapping to sql type.", e);
		}
		if (annoId != null) {
			type.setPk(true);
			List<Field> newPks = new ArrayList<Field>(pkFields);
			newPks.add(field);
			Collections.sort(newPks, new Comparator<Field>() {
				public int compare(Field o1, Field o2) {
					Integer i1=-1;
					Integer i2=-1;
					if(o1 instanceof Enum){
						i1=((Enum<?>) o1).ordinal();
					}
					if(o2 instanceof Enum){
						i2=((Enum<?>) o2).ordinal();
					}
					return i1.compareTo(i2);
				}
			});
			this.pkFields = Arrays.asList(newPks.toArray(new Field[newPks.size()]));
		}
		schemaMap.put(field, type);
		
		if(type instanceof AbstractTimeMapping<?>){
			AbstractTimeMapping<?> m=(AbstractTimeMapping<?>)type;
			if(m.isForUpdate()){
				updateTimeMapping=ArrayUtils.addElement(updateTimeMapping,m);
			}
		}

		if (type instanceof AutoIncrementMapping<?>) {
			incMapping = ArrayUtils.addElement(incMapping, (AutoIncrementMapping<?>) type);
		}
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

	public Field getField(String name) {
		return fields.get(name);
	}

	public Field findField(String name) {
		if (name == null)
			return null;
		Field field = lowerFields.get(name.toLowerCase());
//		if (field == null) {
//			LogUtil.warn("looking field [" + name + "] in " + thisType.getName() + " but not found.");
//		}
		return field;
	}

	public ColumnType getColumnType(String fieldName) {
		Field field = fields.get(fieldName);
		if (field == null) {
			LogUtil.warn(jef.tools.StringUtils.concat("The field [", fieldName, "] does not find in ", this.getThisType().getName()));
			return null;
		}
		return schemaMap.get(field).get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Entity: [").append(containerType.getName()).append("]\n");
		for (MappingType<?> m:this.getMetaFields()) {
			String fname=m.fieldName();
			sb.append("  ").append(fname);
			StringUtils.repeat(sb, ' ', 10-fname.length());
			sb.append('\t').append(m.get());
			sb.append("\n");
		}
		sb.setLength(sb.length()-1);
		return sb.toString();
	}

	public Reference findPath(ITableMetadata class1) {
		for (Reference r : this.refFieldsByRef.keySet()) {
			if (r.getTargetType() == class1) {
				return r;
			}
		}
		return null;
	}

	public Reference findDistinctPath(ITableMetadata target){
		Reference ref = null;
		for(Reference reference:this.refFieldsByRef.keySet()){
			if(reference.getTargetType()==target){
				if(ref!=null){
					throw new IllegalArgumentException("There's more than one reference to ["+target.getSimpleName()+"] in type ["+getSimpleName()+"],please assign the reference field name.");
				}
				ref=reference;
			}
		}
		if(ref==null){
			throw new IllegalArgumentException("Target class "+ target.getSimpleName() + "of fileter-condition is not referenced by "+ getSimpleName());
		}
		return ref;
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
			return new MapFunction(value.functionConstructorParams()[0]);
		default:
			throw new IllegalArgumentException("Unknown KeyFunction:" + value.function());
		}
	}

	/**
	 * 描述针对分表的维度，不是一个可度量的维度，而是直接将这个字符串拼到表名中
	 */
	static final class RawFunc implements PartitionFunction<Object> {
		private String[] nullValue;
		private int maxLen;

		public RawFunc(String defaultWhenFieldIsNull,int maxLen) {
			if (defaultWhenFieldIsNull.length() > 0) {
				this.nullValue = StringUtils.split(defaultWhenFieldIsNull, ',');
				for (int i = 0; i < nullValue.length; i++) {
					nullValue[i] = nullValue[i].trim();
				}
			}
			if(maxLen>0)
				this.maxLen=maxLen;
		}

		public String eval(Object value) {
			return String.valueOf(value);
		}

		public List<Object> iterator(Object min, Object max, boolean left, boolean right) {
			if (min == null && max == null) {
				 if(nullValue != null){
					 return Arrays.asList((Object[]) nullValue);
				 }else{
					 return Collections.EMPTY_LIST;
				 }
			} else if (ObjectUtils.equals(min, max)) {
				return Arrays.asList(min);
			} else {
				//范围丢失
				
				if(min instanceof Integer && max instanceof Integer){
					return iteratorInt((Integer)min,(Integer)max);
				}else if(min instanceof Long && max instanceof Long){
					return iteratorLong((Long)min,(Long)max);
				}else if(min instanceof String){
					return iteratorString((String)min,(String)max);
				}else{
					return Arrays.asList(min, max);
				}
			}
		}

		private List<Object> iteratorLong(long min, long max) {
			List<Object> result=new ArrayList<Object>();
			if(max<min){
				return Collections.EMPTY_LIST; 
			}
			long step=1;
			if((max-min)>1000){
				step=(max-min)/100;
			}
			long i=min;
			for(;i<max;i+=step){
				result.add(i);
			}
			if(i<max){
				result.add(max);
			}
			return result;
		}


		private List<Object> iteratorInt(int min, int max) {
			List<Object> result=new ArrayList<Object>();
			if(max<min){
				return Collections.EMPTY_LIST; 
			}
			int step=1;
			if((max-min)>1000){
				step=(max-min)/100;
			}
			int i=min;
			for(;i<max;i+=step){
				result.add(i);
			}
			if(i<max){
				result.add(max);
			}
			return result;
		}
		
		private List<Object> iteratorString(String min, String max) {
			StringIterator st=new StringIterator(min, max,maxLen,"0123456789".toCharArray());
			List<Object> result=new ArrayList<Object>();
			while(st.hasNext()){
				result.add(st.next());
			}
			return result;
		}
		

		public boolean acceptRegexp() {
			return true;
		}

		public Collection<Object> iterator(RegexpDimension regexp) {
			if(maxLen>0 && regexp.getBaseExp().length()>=maxLen){
				return Arrays.<Object>asList(regexp.getBaseExp());
			}else{
				String baseExp=regexp.getBaseExp();
				Collection<Object> list=new ArrayList<Object>(100);
				for(char c: CharUtils.ALPHA_NUM_UNDERLINE){
					list.add(baseExp+c);
				}
				return list;
			}
			
		}
	};


	public Multimap<String, PartitionFunction> getMinUnitFuncForEachPartitionKey() {
		return partitionFuncs;
	}

	private void innerAdd(Class<?> fieldType, String fieldName, ITableMetadata target, CascadeConfig config) {
		Assert.notNull(containerType);
		Reference r = new Reference(target, config.refType, this);
		if (config.path != null) {
			r.setHint(config.path);
		}
		ReferenceObject field = new ReferenceObject(fieldType, fieldName, r,config);
		if (config.cascade != null) {
			if (config.cascade.length == 0) {// 由于EF-ORM中的级联操作都是显式操作，因此当不指定时可以默认用ALL计算
				field.setCascade(ALL, config.fetch);
			} else {
				field.setCascade(config.cascade, config.fetch);
			}
		}
		addRefField(field);
	}

	private void innerAdd(Class<?> fieldType, String fieldName, Field targetField, CascadeConfig config) {
		ITableMetadata target = DbUtils.getTableMeta(targetField);
		Assert.notNull(containerType);
		Reference r = new Reference(target, config.refType, this);
		if (config.path != null) {
			r.setHint(config.path);
		}

		ReferenceField f = new ReferenceField(fieldType, fieldName, r, targetField, config);
		addRefField(f);
	}

	private void addRefField(AbstractRefField f) {
		List<AbstractRefField> list = refFieldsByRef.get(f.getReference());
		if (list == null) {
			list = new ArrayList<AbstractRefField>();
			refFieldsByRef.put(f.getReference(), list);
		}
		list.add(f);
		refFieldsByName.put(f.getSourceField(), f);
	}

	public String getColumnName(Field fld, DatabaseDialect profile, boolean escape) {
		String name = this.fieldToColumn.get(fld);
		if (name != null) {
			name = profile.getColumnNameIncase(name);
		} else {
			if (fld instanceof JpqlExpression) {
				throw new UnsupportedOperationException();
			}
			if (name == null){
//				if(DbUtils.getTableMeta(fld)!=this){
//					throw new IllegalArgumentException(fld+" is not a property of "+this.getName());
//				}
				name = profile.getColumnNameIncase(fld.name());
				//FIXME 容错处理似无必要.
			}
		}
		// 进行关键字判断和处理
		if (escape) {
			return DbUtils.escapeColumn(profile, name);
		}
		return name;
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

	/*
	 * 添加多对多引用字段
	 */
	public void addRefField_NvsN(Class<?> fieldType, String fieldName, Field targetField, CascadeConfig config) {
		ITableMetadata targetClass = DbUtils.getTableMeta(targetField);
		Reference r = new Reference(targetClass, ReferenceType.MANY_TO_MANY, this);
		if (config.path != null) {
			r.setHint(config.path);
		}

		ReferenceField f = new ReferenceField(fieldType, fieldName, r, targetField,config);
		addRefField(f);
	}

	/*
	 * 添加多对多引用字段
	 */
	public void addRefField_NvsN(Class<?> fieldType, String fieldName, ITableMetadata targetClass, ManyToMany m, CascadeConfig config) {
		Assert.notNull(containerType);
		Reference r = new Reference(targetClass, ReferenceType.MANY_TO_MANY, this);
		if (config.path != null) {
			r.setHint(config.path);
		}

		ReferenceObject f = new ReferenceObject(fieldType, fieldName, r,config);
		if(m!=null){
			CascadeType[] cascade=m.cascade();
			if (cascade != null) {
				if (cascade.length == 0) {
					f.setCascade(ALL, m.fetch());
				} else {
					f.setCascade(cascade, m.fetch());
				}
			}	
		}
		addRefField(f);
	}

	/*
	 * 添加一个1对1引用字段，引用实体表的DO对象
	 * 
	 * @param fieldName 字段名称
	 * 
	 * @param target 实体表对应的类
	 * 
	 * @param path 用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addRefField_1vs1(Class<?> fieldType, String fieldName, ITableMetadata target, OneToOne m,CascadeConfig config) {
		if (m != null) {
			config.cascade = m.cascade();
			config.fetch = m.fetch();
		}
		config.refType=ReferenceType.ONE_TO_ONE;
		innerAdd(fieldType, fieldName, target, config);
	}

	/*
	 * 添加一个1对1引用字段，引用实体表的某个字段
	 * 
	 * @param fieldName 字段名称
	 * 
	 * @param target 实体表被引用字段
	 * 
	 * @param path 用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addRefField_1vs1(Class<?> fieldType, String fieldName, Field target, CascadeConfig config) {
		config.refType=ReferenceType.ONE_TO_ONE;
		innerAdd(fieldType, fieldName, target,config);
	}

	/*
	 * 添加一个1对多引用字段，引用实体表的DO对象
	 * 
	 * @param fieldName 字段名称
	 * 
	 * @param target 实体表的对应DO对象
	 * 
	 * @param path 用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addRefField_1vsN(Class<?> fieldType, String fieldName, ITableMetadata target, OneToMany m,CascadeConfig config) {
		if (m != null) {
			config.cascade = m.cascade();
			config.fetch = m.fetch();
		}
		config.refType= ReferenceType.ONE_TO_MANY;
		innerAdd(fieldType, fieldName, target ,config);
	}

	/*
	 * 添加一个1对多引用字段，引用实体表的某个字段
	 * 
	 * @param fieldName 字段名称
	 * 
	 * @param target 实体表被引用字段
	 * 
	 * @param path 用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addRefField_1vsN(Class<?> fieldType, String fieldName, Field target, CascadeConfig config) {
		config.refType=ReferenceType.ONE_TO_MANY;
		innerAdd(fieldType, fieldName, target,config);
	}

	/*
	 * 添加一个多对一引用字段，引用实体表的DO对象
	 * 
	 * @param fieldName 字段名称
	 * 
	 * @param target 实体表被引用字段
	 * 
	 * @param path 用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addRefField_Nvs1(Class<?> fieldType, String fieldName, ITableMetadata target, ManyToOne m, CascadeConfig config) {
		if (m != null) {
			config.cascade = m.cascade();
			config.fetch = m.fetch();
		}
		config.refType= ReferenceType.MANY_TO_ONE;
		innerAdd(fieldType, fieldName, target, config);
	}

	/*
	 * 添加一个多对一引用字段，引用实体表的一个字段
	 * 
	 * @param fieldName 字段名称
	 * 
	 * @param target 实体表被引用字段
	 * 
	 * @param path 用于连接到实体表的连接提示（如果在全局中注册了关系，则此处可以省略）
	 */
	public void addRefField_Nvs1(Class<?> fieldType, String fieldName, Field target, CascadeConfig config) {
		config.refType=ReferenceType.MANY_TO_ONE;
		innerAdd(fieldType, fieldName, target, config);
	}

	void setTableName(String tableName) {
		this.tableName = tableName;
	}

	void setSchema(String schema) {
		this.schema = schema;
	}

	// 会将LOB移动到最后
	public List<MappingType<?>> getMetaFields() {
		if (metaFields == null) {
			MappingType<?>[] fields = schemaMap.values().toArray(new MappingType<?>[schemaMap.size()]);
			Arrays.sort(fields, new Comparator<MappingType<?>>() {
				public int compare(MappingType<?> field1, MappingType<?> field2) {
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
		try {
			return containerType.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public IQueryableEntity instance() {
		if (metaAccessor != null) {
			return new PojoWrapper(metaAccessor.newInstance(), metaAccessor, this, false);
		} else {
			return UnsafeUtils.newInstance(containerType);
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

	public boolean isAssignableFrom(ITableMetadata type) {
		return this == type || this.containerType.isAssignableFrom(type.getThisType());
	}

	public Field[] getLobFieldNames() {
		return lobNames;
	}

	private AutoIncrementMapping<?>[] incMapping;
	private AbstractTimeMapping<?>[]  updateTimeMapping;

	public AutoIncrementMapping<?> getFirstAutoincrementDef() {
		AutoIncrementMapping<?>[] array = incMapping;
		if (array != null && array.length > 0) {
			return array[0];
		} else {
			return null;
		}
	}

	public AutoIncrementMapping<?>[] getAutoincrementDef() {
		if (incMapping == null) {
			return new AutoIncrementMapping<?>[0];
		} else {
			return incMapping;
		}
	}

	public AbstractTimeMapping<?>[] getUpdateTimeDef() {
		return updateTimeMapping;
	}

	@Override
	protected Collection<MappingType<?>> getColumnSchema() {
		return this.schemaMap.values();
	}

	public PojoWrapper transfer(Object p, boolean isQuery) {
		if (p == null)
			return null;
		if (p instanceof IQueryableEntity) {
			throw new IllegalArgumentException();
		}
		if (p.getClass() == this.thisType) {
			return new PojoWrapper(p, metaAccessor, this, isQuery);
		} else {
			throw new IllegalArgumentException(p.getClass()+" != " + this.thisType);
		}
	}

	public Set<String> getAllFieldNames() {
		return fields.keySet();
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
	public boolean containsMeta(ITableMetadata meta) {
		if(meta==this)return true;
		if(parents==null)return false;
		for(Class clz:parents){
			if(meta.getThisType()==clz){
				return true;
			}
		}
		return false;
	}
}
