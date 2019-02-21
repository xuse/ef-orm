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
package jef.database.wrapper.populator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.PersistenceException;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.common.Entry;
import jef.database.DbUtils;
import jef.database.DebugUtil;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.LazyLoadProcessor;
import jef.database.LazyLoadTask;
import jef.database.LobLazyLoadTask;
import jef.database.Session;
import jef.database.Session.PopulateStrategy;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.dialect.type.ColumnMappings;
import jef.database.dialect.type.ResultSetAccessor;
import jef.database.innerpool.FieldPopulator;
import jef.database.innerpool.MultiplePopulator;
import jef.database.innerpool.NestedObjectPopulator;
import jef.database.jdbc.result.IResultSet;
import jef.database.meta.AbstractRefField;
import jef.database.meta.AliasProvider;
import jef.database.meta.AnnotationProvider;
import jef.database.meta.EntityType;
import jef.database.meta.Feature;
import jef.database.meta.IReferenceAllTable;
import jef.database.meta.IReferenceColumn;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.Reference;
import jef.database.query.EntityMappingProvider;
import jef.database.query.ISelectItemProvider;
import jef.database.query.Query;
import jef.script.javascript.Var;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.reflect.ArrayWrapper;
import jef.tools.reflect.BeanWrapper;
import jef.tools.reflect.UnsafeUtils;

public class ResultPopulatorImpl implements ResultSetPopulator {

	public static final ResultSetPopulator instance = new ResultPopulatorImpl();

	public <T> Iterator<T> iteratorSimple(IResultSet rs, Class<T> clz) {
		return new SimpleRsIterator<T>(rs, clz);
	}

	public <T> Iterator<T> iteratorNormal(Session session, IResultSet rs, EntityMappingProvider context, Transformer transformers) {
		return new NormalRsIterator<T>(rs, context, transformers, session);
	}

	public Iterator<Object[]> iteratorMultipie(IResultSet rs, EntityMappingProvider context, Transformer transformer) {
		return new MultipleRsIterator(rs, context, transformer);
	}

	public <T> Iterator<T> iteratorPlain(IResultSet rs, Transformer transformers) {
		return new PlainRsIterator<T>(rs, transformers);
	}

	public Iterator<Map<String, Object>> iteratorMap(IResultSet rs, Transformer transformers) {
		return new VarRsIterator(rs, transformers);
	}

	/**
	 * Map模式拼装
	 */
	public List<Map<String, Object>> toVar(IResultSet rs, Transformer transformers) {
		VarRsIterator rsVarIterator = new VarRsIterator(rs, transformers);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(40);
		while (rsVarIterator.hasNext()) {
			Map<String, Object> v = rsVarIterator.next();
			// v.remove("");
			list.add((Var) v);
		}
		return list;
	}

	/**
	 * 简单模式拼装。限于对SIMPLE_CLASSES(如字符串、数字、日期等基本类型)
	 * 
	 * @param rs
	 * @param clz
	 * @param strategies
	 * @return
	 */
	public <T> List<T> toSimpleObjects(IResultSet rs, Class<T> clz) {
		List<T> list = new ArrayList<T>();
		for (Iterator<T> iter = new SimpleRsIterator<T>(rs, clz); iter.hasNext();) {
			list.add(iter.next());
		}
		return list;
	}

	/**
	 * 平面模式拼装
	 */
	public <T> List<T> toPlainJavaObject(IResultSet rs, Transformer transformers) {
		List<T> list = new ArrayList<T>();
		for (Iterator<T> iter = new PlainRsIterator<T>(rs, transformers); iter.hasNext();) {
			list.add(iter.next());
		}
		return list;
	}

	/**
	 * 多重模式, 拼装成多个对象
	 */
	public List<Object[]> toDataObjectMap(IResultSet rs, EntityMappingProvider context, Transformer transformer) {
		List<Object[]> list = new ArrayList<Object[]>();
		for (Iterator<Object[]> iter = new MultipleRsIterator(rs, context, transformer); iter.hasNext();) {
			list.add(iter.next());
		}
		return list;
	}

	/**
	 * 标准模式
	 */
	public <T> List<T> toJavaObject(Session session, IResultSet rs, EntityMappingProvider context, Transformer transformers) {
		List<T> list = new ArrayList<T>();
		for (Iterator<T> iter = new NormalRsIterator<T>(rs, context, transformers, session); iter.hasNext();) {
			list.add(iter.next());
		}
		return list;
	}

	// /////////////////////////////////////////////////////////
	@SuppressWarnings("all")
	final static class VarRsIterator implements Iterator<Map<String, Object>> {
		private IResultSet rs;
		private boolean hasNext;
		private List<Mapper<?>> mappers;

		public VarRsIterator(IResultSet rs, Transformer transformers) {
			this.rs = rs;
			/*
			 * Eclipse或者findbugs或者checkStyle很有可能都对下面这句语句提出了警告。但事实上这是一个性能优化上的小技巧
			 * 
			 * 如果把一句话拆成两句来写 hasNext=rs.next(); if(hasNext){
			 * 这样可以消除警告。但是从编译后的代码来看。两句话的编译结果为—— 35: invokeinterface #57, 1;
			 * //InterfaceMethod jef/database/wrapper/IResultSet.next:()Z 40:
			 * putfield #62; //Field hasNext:Z 43: aload_0 44: getfield #62;
			 * //Field hasNext:Z 现在这种写法的编译结果为—— 35: invokeinterface #57, 1;
			 * //InterfaceMethod jef/database/wrapper/IResultSet.next:()Z 40:
			 * dup_x1 41: putfield #62; //Field hasNext:Z 在栈内做一次
			 * dup的开销要小于getfield的开销。（前者是栈访问后者是堆访问）
			 * 
			 * 各种代码检查工具是针对写不好代码的人起到查漏补遗的作用的，但是不能因此在有能力控制代码逻辑正确性的前提下，不去追求性能更好的写法。
			 * 因此，此处就是要这么写，不管各种代码检查工具如何警告，不管省下的性能多么微不足道，我坚持用性能最优的写法。
			 */
			if (hasNext = rs.next()) { // 请忽略代码检查工具的告警
				initColumnAccessor(transformers);
			}
		}

		private void initColumnAccessor(Transformer transformers) {
			this.mappers = transformers.getMapper();
			ColumnMeta cnames = rs.getColumns();
			cnames.initSchemas(transformers);
			for (String schema : cnames.getSchemas()) {
				for (ColumnDescription s : cnames.getColumns(schema)) {
					s.setAccessor(ColumnMappings.RAW);
				}
			}
		}

		public boolean hasNext() {
			return hasNext;
		}

		public Var next() {
			if (!hasNext)
				throw new NoSuchElementException();
			Var v = new Var();
			ColumnMeta cnames = rs.getColumns();
			for (String schema : cnames.getSchemas()) {
				for (ColumnDescription s : cnames.getColumns(schema)) {
					try {
						v.putLowerKey(s.getSimpleName(), s.getValue(rs));
					} catch (SQLException e) {
						throw new PersistenceException(e);
					}
				}
			}
			BeanWrapper wapper = BeanWrapper.wrap(v);
			for (Mapper<?> mapp : mappers) {
				try {
					mapp.process(wapper, rs);
				} catch (SQLException e) {
					throw new PersistenceException(e);
				}
			}
			hasNext = rs.next();
			return v;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	final static class SimpleRsIterator<T> implements Iterator<T> {
		private IResultSet rs;
		private ResultSetAccessor accessor;

		public SimpleRsIterator(IResultSet rs, Class<T> clz) {
			this.rs = rs;
			hasNext = rs.next();
			if (hasNext) {
				this.accessor = initColumnAccessor(clz, rs.getColumns().getN(0));
			}
		}

		private ResultSetAccessor initColumnAccessor(Class<?> clz, ColumnDescription c) {
			ResultSetAccessor accessor = ColumnMappings.getAccessor(clz, c, null);
			if (accessor == null) {
				throw new IllegalArgumentException(clz + "is not a known simple datetype.");
			}
			return accessor;
		}

		public boolean hasNext() {
			return hasNext;
		}

		@SuppressWarnings("unchecked")
		public T next() {
			if (!hasNext)
				throw new NoSuchElementException();
			try {
				return (T) accessor.jdbcGet(rs, 1);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			} finally {
				hasNext = rs.next();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		private boolean hasNext;
	}

	final static class PlainRsIterator<T> implements Iterator<T> {
		private IResultSet rs;
		private Class<T> clz;
		private boolean skipAnnataion;
		private ObjectPopulator populateMeta;
		private List<Mapper<?>> extendPopulator;

		@SuppressWarnings("all")
		public PlainRsIterator(IResultSet rs, Transformer transformers) {
			this.rs = rs;
			/*
			 * Eclipse或者findbugs或者checkStyle很有可能都对下面这句语句提出了警告。但事实上这是一个性能优化上的小技巧
			 * 
			 * 如果把一句话拆成两句来写 hasNext=rs.next(); if(hasNext){
			 * 这样可以消除警告。但是从编译后的代码来看。两句话的编译结果为—— 35: invokeinterface #57, 1;
			 * //InterfaceMethod jef/database/wrapper/IResultSet.next:()Z 40:
			 * putfield #62; //Field hasNext:Z 43: aload_0 44: getfield #62;
			 * //Field hasNext:Z 现在这种写法的编译结果为—— 35: invokeinterface #57, 1;
			 * //InterfaceMethod jef/database/wrapper/IResultSet.next:()Z 40:
			 * dup_x1 41: putfield #62; //Field hasNext:Z 在栈内做一次
			 * dup的开销要小于getfield的开销。（前者是栈访问后者是堆访问）
			 * 
			 * 各种代码检查工具是针对写不好代码的人起到查漏补遗的作用的，但是不能因此在有能力控制代码逻辑正确性的前提下，不去追求性能更好的写法。
			 * 因此，此处就是要这么写，不管各种代码检查工具如何警告，不管省下的性能多么微不足道，我坚持用性能最优的写法。
			 */
			if (hasNext = rs.next()) {// eclipse warning is not correct. here
										// i'm attempt to set variable 'hasNext'
										// and judge it as condition.
				this.clz = (Class<T>) transformers.getResultClazz();
				skipAnnataion = ArrayUtils.fastContains(transformers.getStrategy(), PopulateStrategy.SKIP_COLUMN_ANNOTATION);
				ColumnMeta columnMeta = rs.getColumns();
				columnMeta.initSchemas(transformers);
				populateMeta = initColumnAccessor(columnMeta, transformers);
			}
		}

		private ObjectPopulator initColumnAccessor(ColumnMeta columnMeta, Transformer transformers) {
			extendPopulator = transformers.getMapper();
			BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(clz);
			return fillPlain(ba, skipAnnataion, columnMeta, extendPopulator==null);
		}

		public boolean hasNext() {
			return hasNext;
		}

		public T next() {
			if (!hasNext)
				throw new NoSuchElementException();
			IResultSet rs = this.rs;
			try {
				T retObj = (T) UnsafeUtils.newInstance(clz);
				BeanWrapper wrapper = BeanWrapper.wrap(retObj);
				populateMeta.process(wrapper, rs);
				for (Mapper<?> m : extendPopulator) {
					m.process(wrapper, rs);
				}
				endPopulate(retObj);
				return retObj;
			} catch (SQLException e) {
				throw new RuntimeException(e);
			} finally {
				hasNext = rs.next();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		private boolean hasNext;
	}

	final static private class RawArrayMapper extends Mapper<Object[]> {
		private ResultSetAccessor[] accessor;

		public RawArrayMapper(ResultSetAccessor[] accessor) {
			this.accessor = accessor;
		}

		@Override
		protected void transform(Object[] obj, IResultSet rs) throws SQLException {
			int size = accessor.length;
			for (int i = 0; i < size; i++) {
				obj[i] = accessor[i].jdbcGet(rs, i + 1);
			}
		}
	}

	final static class MultipleRsIterator implements Iterator<Object[]> {
		private IResultSet rs;
		private int size;
		private List<Mapper<?>> populators;
		private Class<?> componentType;

		@SuppressWarnings("all")
		public MultipleRsIterator(IResultSet rs, EntityMappingProvider context, Transformer transformer) {
			this.componentType = transformer.getResultClazz().getComponentType();
			Assert.isFalse(componentType.isPrimitive(), "Please use the complex type of " + componentType);
			this.rs = rs;
			/*
			 * Eclipse或者findbugs或者checkStyle很有可能都对下面这句语句提出了警告。但事实上这是一个性能优化上的小技巧
			 * 
			 * 如果把一句话拆成两句来写 hasNext=rs.next(); if(hasNext){
			 * 这样可以消除警告。但是从编译后的代码来看。两句话的编译结果为—— 35: invokeinterface #57, 1;
			 * //InterfaceMethod jef/database/wrapper/IResultSet.next:()Z 40:
			 * putfield #62; //Field hasNext:Z 43: aload_0 44: getfield #62;
			 * //Field hasNext:Z 现在这种写法的编译结果为—— 35: invokeinterface #57, 1;
			 * //InterfaceMethod jef/database/wrapper/IResultSet.next:()Z 40:
			 * dup_x1 41: putfield #62; //Field hasNext:Z 在栈内做一次
			 * dup的开销要小于getfield的开销。（前者是栈访问后者是堆访问）
			 * 
			 * 各种代码检查工具是针对写不好代码的人起到查漏补遗的作用的，但是不能因此在有能力控制代码逻辑正确性的前提下，不去追求性能更好的写法。
			 * 因此，此处就是要这么写，不管各种代码检查工具如何警告，不管省下的性能多么微不足道，我坚持用性能最优的写法。
			 */
			if (hasNext = rs.next()) { // Please Ignore the warning of eclipse.
				boolean noContext = context == null || transformer.hasStrategy(PopulateStrategy.COLUMN_TO_ARRAY);
				if (!noContext) { // 为了尽可能自动，当context中参与的表只有一个时，默认认为还是Column_To_Array模式的
					noContext = context.getReference().size() < 2;
				}
				if (noContext && transformer.getMapper().isEmpty()) {
					initSimpleArrayPopulator();
				} else {
					initColumnAccessor(rs.getColumns(), context == null ? null : context.getReference(), transformer);
				}
			}
		}

		private void initSimpleArrayPopulator() {
			populators = new ArrayList<Mapper<?>>(1);
			ColumnMeta meta = rs.getColumns();
			this.size = meta.length();

			ResultSetAccessor[] accessors = new ResultSetAccessor[size];
			for (int i = 0; i < size; i++) {
				accessors[i] = ColumnMappings.getAccessor(componentType, meta.getN(i), null);
			}
			populators.add(new RawArrayMapper(accessors));
		}

		private void initColumnAccessor(ColumnMeta columnNames, List<ISelectItemProvider> queries, Transformer transformer) {
			if (transformer.getMapper().isEmpty()) {
				populators = new ArrayList<Mapper<?>>();
				int i = 0;
				for (ISelectItemProvider prov : queries) {
					Query<?> q = prov.getTableDef();
					String schema = prov.getSchema();
					Mapper<? extends IQueryableEntity> mapper = Mappers.toArrayElement(i++, q.getMeta(), schema);
					mapper.prepare(columnNames.nameIndex);
					populators.add(mapper);
				}
				this.size = populators.size();
			} else {
				this.populators = transformer.getMapper();
				this.size = transformer.getMaxMapperIndex() + 1;
			}

			columnNames.initSchemas(transformer);
		}

		public boolean hasNext() {
			return hasNext;
		}

		public Object[] next() {
			if (!hasNext)
				throw new NoSuchElementException();
			try {
				Object[] map = (Object[]) Array.newInstance(componentType, size);
				ArrayWrapper aw = new ArrayWrapper(map);
				for (Mapper<?> prov : populators) {
					prov.process(aw, rs);
				}
				return map;
			} catch (SQLException e) {
				throw new RuntimeException(e);
			} finally {
				hasNext = rs.next();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		private boolean hasNext;
	}

	final static class NormalRsIterator<T> implements Iterator<T> {
		private IResultSet rs;
		private ITableMetadata meta;
		private Class<T> retClz;

		private String[] schemas;
		private ISelectItemProvider[] subObjects;

		private List<ObjectPopulator> directPopulator;
		private List<IPopulator> extendPopulator;

		private AliasProvider defaultField;
		private ColumnMeta columnNames;
		private Session session;

		@SuppressWarnings("all")
		public NormalRsIterator(IResultSet rs, EntityMappingProvider context, Transformer transformers, Session session) {
			Class<T> resultObj = (Class<T>) transformers.getResultClazz();
			this.rs = rs;
			this.retClz = resultObj;
			this.meta = transformers.getResultMeta();
			this.session = session;
			/*
			 * 如果把一句话拆成两句来写 hasNext=rs.next(); if(hasNext){
			 * 这样可以消除警告。但是从编译后的代码来看。编译结果为—— 35: invokeinterface #57, 1;
			 * //InterfaceMethod jef/database/wrapper/IResultSet.next:()Z 40:
			 * putfield #62; //Field hasNext:Z 43: aload_0 44: getfield #62;
			 * //Field hasNext:Z 现在这种写法的编译结果为—— 35: invokeinterface #57, 1;
			 * //InterfaceMethod jef/database/wrapper/IResultSet.next:()Z 40:
			 * dup_x1 41: putfield #62; //Field hasNext:Z 在栈内做一次
			 * dup的开销要小于getfield的开销。（前者是栈访问后者是堆访问）
			 */
			if (hasNext = rs.next()) {
				columnNames = rs.getColumns();
				subObjects = null;
				defaultField = null;

				if (context != null) {
					Entry<String[], ISelectItemProvider[]> desc = context.getPopulationDesc();
					schemas = desc.getKey();
					subObjects = desc.getValue();
					defaultField = context.isMultiTable() ? AliasProvider.DEFAULT : null;
				} else {
					schemas = new String[] { "" };// 当引用查询时的基础表Schema
				}

				if (meta == null && IQueryableEntity.class.isAssignableFrom(resultObj)) {
					this.meta = MetaHolder.getMeta(resultObj.asSubclass(IQueryableEntity.class));
				}
				initColumnAccessor(transformers);
			}
		}

		private void initColumnAccessor(Transformer transformers) {
			boolean skipAnnataion = transformers.hasStrategy(PopulateStrategy.SKIP_COLUMN_ANNOTATION);
			BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(retClz);
			directPopulator = new ArrayList<ObjectPopulator>(schemas.length);
			Set<ColumnMapping> populated = new HashSet<ColumnMapping>(meta.getColumns().size());
			for (String schema : schemas) {
				if (!transformers.hasIgnoreSchema(schema)) {
					directPopulator.add(fill(schema, defaultField, rs, columnNames, meta, skipAnnataion, populated));
				}
			}
			if (subObjects != null) {// 主对象拼装完毕后，拼装JOIN对象中指出的其他引用字段
				extendPopulator = new ArrayList<IPopulator>(subObjects.length);
				for (ISelectItemProvider rp : subObjects) {
					extendPopulator.add(fillReference(columnNames, ba, rs, rp, rs.getProfile()));
				}
			}
			if (!transformers.getMapper().isEmpty()) {
				if (extendPopulator == null) {
					extendPopulator = new ArrayList<IPopulator>(transformers.getMapper());
				} else {
					extendPopulator.addAll(transformers.getMapper());
				}
			}
		}

		public boolean hasNext() {
			return hasNext;
		}

		@SuppressWarnings("unchecked")
		public T next() {
			if (!hasNext)
				throw new NoSuchElementException();
			try {

				T retObj;
				BeanWrapper wrapper;
				if (meta == null) {
					retObj = UnsafeUtils.newInstance(retClz);
					wrapper = BeanWrapper.wrap(retObj, BeanWrapper.FAST);
				} else {
					IQueryableEntity e = meta.newInstance();
					e.stopUpdate();
					retObj = (T) e;
					wrapper = new FastBeanWrapperImpl(retObj, meta.getContainerAccessor());
				}
				for (ObjectPopulator op : directPopulator) {
					op.process(wrapper, rs);

				}
				if (extendPopulator != null) {// 主对象拼装完毕后，拼装JOIN对象中指出的其他引用字段
					for (IPopulator rp : extendPopulator) {
						rp.process(wrapper, rs);
					}
				}
				endPopulate(retObj);
				return retObj;
			} catch (SQLException e) {
				throw DbUtils.toRuntimeException(e);
			} finally {
				hasNext = rs.next();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		private boolean hasNext;

		private IPopulator fillReference(ColumnMeta columns, BeanAccessor ba, IResultSet rs, ISelectItemProvider rp, DatabaseDialect profile) {
			List<IPopulator> result = new ArrayList<IPopulator>();
			if (rp.getReferenceObj() != null) {
				IReferenceAllTable allcolumns = rp.getReferenceObj();
				String name = allcolumns.getName();
				ITableMetadata targetType = allcolumns.getFullModeTargetType();
				BeanAccessor targetAccessor = (name == null ? ba : FastBeanWrapperImpl.getAccessorFor(targetType.getContainerType()));

				ObjectPopulator op1;
				List<LazyLoadTask> tasks = new ArrayList<LazyLoadTask>();
				if (targetType.getType() != EntityType.NATIVE || targetAccessor.getType() == targetType.getThisType()) {
					op1 = fill(rp.getSchema(), allcolumns, rs, columns, targetType, false, null);
					if (allcolumns.isLazyLob()) {
						for (Field field : targetType.getLobFieldNames()) {
							ColumnMapping mType = targetType.getColumnDef(field);
							LobLazyLoadTask task = new LobLazyLoadTask(mType, rs.getProfile(), targetType.getTableName(true));
							tasks.add(task);
						}
					}
				} else {
					op1 = fillPlain(targetAccessor, false, columns, extendPopulator==null);
				}
				if (rp.getStaticRef() != null) {// 级联对象的级联任务作为延迟加载
					Map<Reference, List<AbstractRefField>> map = targetType.getRefFieldsByRef();

					for (Map.Entry<Reference, List<AbstractRefField>> entry : map.entrySet()) {
						LazyLoadTask task = DebugUtil.getLazyTaskMarker(entry, rs.getFilters(), session);
						tasks.add(task);
					}
				}
				if (!tasks.isEmpty()) {
					LazyLoadProcessor lz = new LazyLoadProcessor(tasks, session);
					op1.setProcessor(lz);
				}

				if (name == null) {
					result.add(op1);
				} else {
					result.add(new NestedObjectPopulator(name, op1));
				}
			}

			for (IReferenceColumn field : rp.getReferenceCol()) {
				// TODO 修复关于引用单字段的LOB的延迟加载问题。
				result.add(new FieldPopulator(field, profile, rp.getSchema(), columns, ba));
			}
			if (result.size() == 1)
				return result.get(0);
			return new MultiplePopulator(result.toArray(new IPopulator[result.size()]));
		}
	}

	private static void endPopulate(Object retObj) {
		if (retObj instanceof IQueryableEntity) {
			((IQueryableEntity) retObj).startUpdate();
		}
	}

	/*
	 * @ba拼装java模型 schema schema fullRef 引用 columns 数据库列 meta
	 */
	private static ObjectPopulator fill(String schema, AliasProvider fullRef, IResultSet rs, ColumnMeta columns, ITableMetadata meta, boolean skipColumn,
			Set<ColumnMapping> populated) {
		Map<String, ColumnDescription> data = new HashMap<String, ColumnDescription>();
		DatabaseDialect profile = rs.getProfile();
		// 这里要按照列名来拼装，不是默认全拼装
		for (ColumnMapping ft : meta.getColumns()) {
			String columnName;
			Field f = ft.field();
			if (fullRef == null) {
				columnName = skipColumn ? f.name().toUpperCase() : ft.upperColumnName();
			} else {
				columnName = fullRef.getResultAliasOf(ft, schema);
			}
			if (columnName != null) {
				ColumnDescription columnDesc = columns.getByUpperName(columnName);
				if (columnDesc == null) {
					// 没找到合适的列，可以认为是SQL语句中并未查询该列，保持该列不设置即可。
					// if (schema == "")
					// System.err.println("Warnning: populating object " +
					// meta.getThisType() + " error," + schema + ":" +
					// columnName + " not found in the selected columns");
				} else {
					if (populated == null || populated.add(ft)) {
						ResultSetAccessor accessor = ColumnMappings.getAccessor(ft.getFieldAccessor().getType(), ft, columnDesc, false);
						columnDesc.setAccessor(accessor);
						data.put(f.name(), columnDesc);
					} else {
						throw new PersistenceException(
								"Ambiguous column will set to field ["
										+ ft
										+ "]. Hint: you can use Object[] as a result container to receive multiple tables in this join, or remove unsed select columns in the query.");
					}
					;
				}
			}
		}
		ObjectPopulator op = new ObjectPopulator(meta, data);

		if (schema == "" || schema.length() < 2) {
			if (profile.has(Feature.SELECT_ROW_NUM)) {
				ColumnDescription columnDesc = columns.getByUpperName("ROWID_");
				if (columnDesc != null) {
					op.bindRowidForColumn = columnDesc.getN();
				}
			}
		}
		return op;
	}

	@SuppressWarnings({ "unchecked" })
	private static <A extends Annotation> A getAnnotation(BeanAccessor ba, String fieldName, Class<A> class1) {
		Map<Class<?>, Annotation> map = ba.getAnnotationOnField(fieldName);
		return map == null ? null : (A) map.get(class1);
	}

	static class Annotations implements AnnotationProvider {
		private BeanAccessor ba;
		private String fieldName;

		Annotations(BeanAccessor ba, String fieldName) {
			this.ba = ba;
			this.fieldName = fieldName;
		}

		@Override
		public <T extends Annotation> T getAnnotation(Class<T> type) {
			return ResultPopulatorImpl.getAnnotation(ba, fieldName, type);
		}

		@Override
		public String getName() {
			return fieldName;
		}

	}

	private static ObjectPopulator fillPlain(BeanAccessor ba, boolean skipAnnataion, ColumnMeta columnMeta, boolean noExtendPopulator) {
		Map<String, ColumnDescription> matched = new HashMap<String, ColumnDescription>();
		for (String fieldName : ba.getPropertyNames()) {
			javax.persistence.Column columnAnnotation = skipAnnataion ? null : getAnnotation(ba, fieldName, javax.persistence.Column.class);
			String columnName;
			if (columnAnnotation != null && columnAnnotation.name().length() != 0) {
				columnName = columnAnnotation.name();
			} else {
				columnName = fieldName;
			}
			ColumnDescription c = columnMeta.getByUpperName(columnName.toUpperCase());// findBySimpleName(columnName)
			if (c != null) {
				c.setAccessor(ColumnMappings.getAccessor(ba.getPropertyType(fieldName), c, skipAnnataion ? null : new Annotations(ba, fieldName)));
				matched.put(fieldName, c);
			}
		}
		if (matched.isEmpty() && noExtendPopulator) {
			throw new PersistenceException("No column match any fields in result class [" + ba.getType().getName() + "]. Columns:" + columnMeta.toString());
		}
		return new ObjectPopulator(null, matched);
	}
}
