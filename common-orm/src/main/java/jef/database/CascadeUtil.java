package jef.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.FetchType;

import jef.database.annotation.Cascade;
import jef.database.meta.AbstractRefField;
import jef.database.meta.ISelectProvider;
import jef.database.meta.ITableMetadata;
import jef.database.meta.JoinKey;
import jef.database.meta.JoinPath;
import jef.database.meta.MetaHolder;
import jef.database.meta.Reference;
import jef.database.meta.TupleMetadata;
import jef.database.query.ReferenceType;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapper;

import com.google.common.base.Objects;

final class CascadeUtil {
	static int deleteWithRefInTransaction(IQueryableEntity source, Session trans, int minPriority) throws SQLException {
		return deleteCascadeByQuery(source, trans, true, true, minPriority);
	}

	// 删除单个对象或请求和其所有级联引用
	private static int deleteCascadeByQuery(IQueryableEntity source, Session trans, boolean doSelect, boolean delSubFirst, int minPriority) throws SQLException {
		ITableMetadata meta = MetaHolder.getMeta(source);
		List<Reference> delrefs = new ArrayList<Reference>();
		for (AbstractRefField f : meta.getRefFieldsByName().values()) {
			if (!f.canDelete() || f.getPriority() < minPriority) {
				continue;
			}
			Reference ref = f.getReference();
			if (ref.getType() == ReferenceType.ONE_TO_MANY || ref.getType() == ReferenceType.ONE_TO_ONE) {
				delrefs.add(ref);
			}else if(ref.getHint()!=null && ref.getHint().getRelationTable()!=null) {
				delrefs.add(ref);
			}
		}
		if (delrefs.isEmpty()) {
			return trans.delete0(source.getQuery()); // 没有需级联删除的引用，当做普通删除即可
		}
		// 要不要做
		List<IQueryableEntity> objs;
		if (doSelect) {
			source.getQuery().setCascade(false);
			objs = trans.select(source);
		} else {
			objs = Arrays.asList(source);
		}
		if (delSubFirst) {
			@SuppressWarnings("unused")
			int n = deleteChildren(objs, trans, delrefs);
			return trans.delete0(source.getQuery());
		} else {
			int n = trans.delete0(source.getQuery()); // 先删除自身，在删除引用，防止在解析引用时出现循环引用又来删除自身
			deleteChildren(objs, trans, delrefs);
			return n;
		}
	}

	private static int deleteChildren(List<IQueryableEntity> objs, Session trans, List<Reference> refs) throws SQLException {
		int count = 0;
		for (IQueryableEntity obj : objs) {
			for (Reference ref : refs) {
				//前面已经检查过，无需再次检查
//				if (ref.getType() == ReferenceType.ONE_TO_MANY || ref.getType() == ReferenceType.ONE_TO_ONE) {
					BeanWrapper bean = BeanWrapper.wrap(obj);
					count += doDeleteRef(trans, bean, ref);
//				}
			}
		}
		return count;
	}
	
	/**
	 * 目的是清理掉对象内的延迟加载上下文，并将延迟加载未处理的字段记录下来
	 * 
	 * 修改原因：算法优化——对于没有调用过延迟加载的关系，认为是无需参加级联操作的关系。
	 * 修改前：没有调用过的延迟加载，在级联触发之前会自动调用，从而先加载，然后再次加载，并且进行比对，比对后发现没有修改过，然后什么也不做。
	 * 改用这种优化算法后，对于没有触发的延迟加载最少可以省去两次查询操作。
	 * @param obj
	 * @return
	 */
	private static Set<String> clearLazy(DataObject obj) {
//		return Collections.emptySet();
		Set<String> unloaded;
		if(obj.lazyload!=null) {
			unloaded=new HashSet<String>();
			Map<String,Integer> fields=obj.lazyload.getProcessor().getOnFields();
			for(String s:fields.keySet()) {
				if(obj.lazyload.needLoad(s)>-1) {//将尚未执行过延迟加载的字段记录下来，后续忽略更新
					unloaded.add(s);
				}
			}
		}else {
			unloaded=Collections.emptySet();
		}
		obj.clearQuery();
		return unloaded;
	}
	

	/*
	 * smartMode: 智能模式，当开启后自动忽略掉那些没有set过的property
	 */
	static void insertWithRefInTransaction(List<IQueryableEntity> list, Session trans, boolean smartMode, int minPriority) throws SQLException {
		if (list.isEmpty())
			return;
		boolean single = list.size() == 1;
		ITableMetadata meta = MetaHolder.getMeta(list.get(0));
		for (IQueryableEntity obj : list) {
			// 在维护端操作之前
			Set<String> lazyloadSkip=clearLazy((DataObject)obj);
			BeanWrapper bean = BeanWrapper.wrap(obj);
			for (AbstractRefField f : meta.getRefFieldsByName().values()) {
				if(lazyloadSkip.contains(f.getName()))
					continue;
				// 无需执行级联操作
				if (!f.canInsert() || f.getPriority() < minPriority) {
					continue;
				}
				Reference ref = f.getReference();
				Object value = f.getField().get(obj);
				if (value != null && ref.getType() == ReferenceType.MANY_TO_ONE) { // 多对一的话，提前维护子表
					doInsertRef1(trans, value, bean, ref, false);
				}
			}
		}
		if (single) {
			trans.insert0(list.get(0), null, smartMode);
		} else {
			trans.batchInsert(list, smartMode);
		}
		for (IQueryableEntity obj : list) {
			BeanWrapper bean = BeanWrapper.wrap(obj);
			// 在维护端操作之后
			for (AbstractRefField f : meta.getRefFieldsByName().values()) {
				if (!f.canInsert() || f.getPriority() < minPriority)
					continue;

				Reference ref = f.getReference();
				Object value = f.getField().get(obj);
				if(value==null)continue;
				// 其他几种情况，维护子表
				switch (ref.getType()) {
				case ONE_TO_ONE:
					doInsertRef1(trans, value, bean, ref, true);
					break;
				case ONE_TO_MANY:
					doInsertRefN(trans, value, bean, f);
					break;
				case MANY_TO_MANY:
					doInsertRefNN(trans, value, bean, f);
				default:
				}
			}
		}

	}

	static int updateWithRefInTransaction(IQueryableEntity obj, Session trans, int minPriority) throws SQLException {
		Collection<AbstractRefField> refs = MetaHolder.getMeta(obj).getRefFieldsByName().values();
		Set<String> lazyloadSkip=clearLazy((DataObject)obj);
		int result = 0;
		// 在维护端操作之前
		for (AbstractRefField f : refs) {
			if(lazyloadSkip.contains(f.getName()))
				continue;
			if (!f.canUpdate() || f.getPriority() < minPriority) {
				continue;
			}
			Reference ref = f.getReference();
			Object value = f.getField().get(obj);
			if (ref.getType() == ReferenceType.MANY_TO_ONE) { // 多对一的话，提前维护子表
				doUpdateRef1(trans, value, BeanWrapper.wrap(obj), ref, false);
			}
		}
		// 维护端操作
		if (obj.needUpdate())
			result = trans.update0(obj, null);
		// 维护端操作之后
		for (AbstractRefField f : refs) {
			// 无需执行级联操作
			if(lazyloadSkip.contains(f.getName()))
				continue;
			if (!f.canUpdate() || f.getPriority() < minPriority) {
				continue;
			}
			Reference ref = f.getReference();
			Object value = f.getField().get(obj);
			BeanWrapper bean = BeanWrapper.wrap(obj);
			switch (ref.getType()) {
			case ONE_TO_MANY:
				doUpdateRefN(trans, value, bean, f, true);
				break;
			case MANY_TO_MANY:
				doUpdateRefNN(trans, value, bean, f);
				break;
			case ONE_TO_ONE:
				doUpdateRef1(trans, value, bean, ref, true);
			default:
				break;
			}
		}
		return result;
	}

	// 维护插入操作的子表(对一操作，此处不分多对一还是单对一。因此有些问题。)
	/**
	 * 
	 * @param trans
	 * @param value
	 * @param bean
	 * @param ref
	 * @param reverse
	 *            反向。 （当子表先操作，父表后操作（此处一般指insert或update操作），称为正向。）
	 *            当先操作父表，后操作字表，称为反向。正向情况下，要将子表操作完成后 关联键值赋值到父表中。
	 *            反向情况下，在操作完成前，就将父表关键字值赋值给子表
	 * 
	 * @throws SQLException
	 */
	private static void doInsertRef1(Session trans, Object value, BeanWrapper bean, Reference ref, boolean reverse) throws SQLException {
		IQueryableEntity d = cast(value, ref.getTargetType());
		BeanWrapper bwSub = BeanWrapper.wrap(d);
		JoinPath jp = ref.toJoinPath();
		if (reverse) {// 反向维护关联键值
			for (JoinKey jk : jp.getJoinKeys()) {
				Object parentKey = bean.getPropertyValue(jk.getLeft().name());
				Object subKey = bwSub.getPropertyValue(jk.getRightAsField().name());
				if (!Objects.equal(parentKey, subKey)) {
					bwSub.setPropertyValue(jk.getRightAsField().name(), parentKey);
				}
			}
		}
		checkAndInsert(trans, Arrays.asList(d), true);
		if (!reverse) {// 正向维护关联键值
			for (JoinKey jk : jp.getJoinKeys()) {
				Object subKey = bwSub.getPropertyValue(jk.getRightAsField().name());
				Object parentKey = bean.getPropertyValue(jk.getLeft().name());
				if (!Objects.equal(parentKey, subKey)) {
					bean.setPropertyValue(jk.getLeft().name(), subKey);
				}
			}
		}
	}

	/**
	 * 对多关系，无论是一对多还是多对多，目前都是先插入父表，再插入子表的……因此其实都是反向模式
	 * 
	 * @param trans 事务
	 * @param value 级联对象
	 * @param bean 父对象反射包装
	 * @param f 引用关系
	 * @throws SQLException
	 */
	private static void doInsertRefN(Session trans, Object value, BeanWrapper bean, AbstractRefField f) throws SQLException {
		Reference ref = f.getReference();
		
		Map<String, Object> map = new HashMap<String, Object>();
		for (JoinKey jk : ref.toJoinPath().getJoinKeys()) {
			if (bean.isReadableProperty(jk.getLeft().name())) {
				Object refValue = bean.getPropertyValue(jk.getLeft().name());
				map.put(jk.getRightAsField().name(), refValue);
			}
		}
		Collection<? extends IQueryableEntity> list = castToList(value, f);
		for (IQueryableEntity d : list) {
			BeanWrapper bwSub = BeanWrapper.wrap(d);
			for (Entry<String, Object> e : map.entrySet()) {
				Object newValue = e.getValue();
				Object oldValue = bwSub.getPropertyValue(e.getKey());
				if (!Objects.equal(oldValue, newValue)) {
					bwSub.setPropertyValue(e.getKey(), newValue);
				}
			}

		}
		checkAndInsert(trans, list, true);
	}

	/**
	 * 对多关系，无论是一对多还是多对多，目前都是先插入父表，再插入子表的……因此其实都是反向模式
	 * 
	 * @param trans 事务
	 * @param value 级联对象
	 * @param bean 父对象反射包装
	 * @param f 引用关系
	 * @throws SQLException
	 */
	private static void doInsertRefNN(Session trans, Object value, BeanWrapper bean, AbstractRefField f) throws SQLException {
		Reference ref = f.getReference();
		if(ref.getHint()!=null && ref.getHint().getRelationTable()==null) {
			doInsertRefN(trans,value,bean,f);
			return;
		}
		//对于用过中间表的多对多关系，其级联对象关联位于一张隐含的表中，和当前对象的单表字段没有关系。
		//故修改数值一致性操作省略
		Collection<? extends IQueryableEntity> list = castToList(value, f);
		//目标表处理
		checkAndInsert(trans,list,true);
		//关系表处理
		List<VarObject> relations=generateRelationData(ref,list,bean);
				
		checkAndInsert(trans,relations,false);
	}
	
	/**
	 * 产生关系表数据
	 */
	private static List<VarObject> generateRelationData(Reference ref,Collection<? extends IQueryableEntity> list,BeanWrapper bean ) {
		List<VarObject> relations=new ArrayList<VarObject>();
		JoinPath path=ref.getHint();
		TupleMetadata meta=path.getRelationTable();
		JoinPath path2=path.getRelationToTarget();
		for(IQueryableEntity d:list) {
			VarObject obj=meta.newInstance();
			//主对象赋值
			for(JoinKey jk:path.getJoinKeys()) {
				obj.put(jk.getRightAsField().name(), bean.getPropertyValue(jk.getLeft().name()));
			}
			//级联对象赋值
			BeanWrapper sub=BeanWrapper.wrap(d);
			for(JoinKey jk:path2.getJoinKeys()) {
				obj.put(jk.getLeft().name(), sub.getPropertyValue(jk.getRightAsField().name()));
			}
			relations.add(obj);
		}
		return relations;
	}

	private static int doDeleteRef(Session trans, BeanWrapper bean, Reference ref) throws SQLException {
		IQueryableEntity rObj;
		if(ref.getHint()!=null && ref.getHint().getRelationTable()!=null) {
			rObj=ref.getHint().getRelationTable().newInstance();
		}else {
			rObj = ref.getTargetType().newInstance();
		}
		for (JoinKey r : ref.toJoinPath().getJoinKeys()) {
			rObj.getQuery().addCondition(r.getRightAsField(), bean.getPropertyValue(r.getLeft().name()));
		}
		return deleteCascadeByQuery(rObj, trans, true, false, 0);
	}

	private static Map<List<?>, IQueryableEntity> doSelectRef(Session trans, BeanWrapper bean, Reference ref) throws SQLException {
		Map<List<?>, IQueryableEntity> result = new HashMap<List<?>, IQueryableEntity>();
		IQueryableEntity rObj = ref.getTargetType().newInstance();
		for (JoinKey r : ref.toJoinPath().getJoinKeys()) {
			rObj.getQuery().addCondition(r.getRightAsField(), bean.getPropertyValue(r.getLeft().name()));
		}
		List<? extends IQueryableEntity> list = trans.select(rObj);// 查出旧的引用对象
		for (IQueryableEntity e : list) {
			List<Object> key = DbUtils.getPrimaryKeyValue(e);
			Assert.notNull(key);
			result.put(key, e);
		}
		return result;// 按主键为key记录每个引用的对象
	}

	// 维护更新操作的子表
	private static void doUpdateRef1(Session trans, Object value, BeanWrapper bean, Reference ref, boolean doDelete) throws SQLException {
		if (value == null) {
			if (doDelete) {
				doDeleteRef(trans, bean, ref);
			}
			return;
		}
		IQueryableEntity d = cast(value, ref.getTargetType());
		checkAndInsert(trans, Arrays.asList(d), true);
		BeanWrapper bwSub = BeanWrapper.wrap(d);
		JoinPath jp = ref.toJoinPath();
		for (JoinKey jk : jp.getJoinKeys()) {
			Object newValue = bwSub.getPropertyValue(jk.getRightAsField().name());
			Object oldValue = bean.getPropertyValue(jk.getLeft().name());
			if (!Objects.equal(oldValue, newValue)) {
				bean.setPropertyValue(jk.getLeft().name(), newValue);
			}
		}
	}

	private static void doUpdateRefN(Session trans, Object value, BeanWrapper bean, AbstractRefField f, boolean doDeletion) throws SQLException {
		// 2011-12-22:refactor logic, avoid to use delete-insert algorithm.
		// 2014-5-1: add rule, if refByMany then no deletion

		// 取得新旧的引用关系List.查出旧的引用数据集合，并按主键存放

		Reference ref = f.getReference();
		Map<List<?>, IQueryableEntity> olds = doSelectRef(trans, bean, ref);
		// 新的引用关系
		Collection<? extends IQueryableEntity> list = castToList(value, f);

		// 计算要更新要子表的字段和数值
		Map<String, Object> map = new HashMap<String, Object>();
		for (JoinKey jk : ref.toJoinPath().getJoinKeys()) {
			if (bean.isReadableProperty(jk.getLeft().name())) { // JoinKey的左边，就是主表中的值
				Object refValue = bean.getPropertyValue(jk.getLeft().name()); // 得到主表中的引用键值
				map.put(jk.getRightAsField().name(), refValue); // 记录要更新到子表记录中的字段和值,引用键值和自表中的字段名对应
			}
		}

		List<IQueryableEntity> toAdd = new ArrayList<IQueryableEntity>();
		// 更新新的子表数据到
		for (IQueryableEntity d : list) {
			BeanWrapper bwSub = BeanWrapper.wrap(d);
			// 更新内存数据(将新的引用关系中的引用键更新为何主表记录一致。)修复数据一致性。
			for (Entry<String, Object> e : map.entrySet()) {
				Object newValue = e.getValue();
				Object oldValue = bwSub.getPropertyValue(e.getKey());
				if (!Objects.equal(oldValue, newValue)) {
					bwSub.setPropertyValue(e.getKey(), newValue);
				}
			}
			// 对照旧值进行插入或更新
			List<Object> pks = DbUtils.getPrimaryKeyValue(d);
			IQueryableEntity old = null;

			if (pks != null) {
				old = olds.remove(pks);
			}
			if (old != null) {// 存在旧值，更新处理
				DbUtils.compareToUpdateMap(d, old);
				if (old.needUpdate()) {
					updateWithRefInTransaction(old, trans, 0);
				}
			} else {
				toAdd.add(d);// 无旧值，插入处理
			}
		}
		insertWithRefInTransaction(toAdd, trans, true, 0);
		// 旧值中有而新值中没有，删除处理
		if (doDeletion) {
			// 将剩余的子表数据删掉
			for (IQueryableEntity d : olds.values()) {
				deleteCascadeByQuery(d, trans, false, true, 0);
			}
		}
	}
	
	/**
	 * 多对多的更新操作
	 * @param trans 事务 
	 * @param value 级联对象
	 * @param bean 父对象
	 * @param f 引用字段
	 * @param b 是否执行删除
	 * @throws SQLException 
	 */
	private static void doUpdateRefNN(Session trans, Object value, BeanWrapper bean, AbstractRefField f) throws SQLException {
		Reference ref = f.getReference();
		if(ref.getHint()!=null && ref.getHint().getRelationTable()==null) {
			doUpdateRefN(trans,value,bean,f,false);
			return;
		}
		//检查目标表
		//对于用过中间表的多对多关系，其级联对象关联位于一张隐含的表中，和当前对象的单表字段没有关系。
		//故修改数值一致性操作省略
		Collection<? extends IQueryableEntity> list = castToList(value, f);
		//目标表处理
		checkAndInsert(trans,list,true);
		
		//维护关系表
		List<VarObject> relations=generateRelationData(ref,list,bean);
		doDeleteRef(trans,bean,ref);
		trans.batchInsert(relations);
	}


	/**
	 * 按主键去检查每条字表记录，有的就更新，没有的就插入
	 * @param trans
	 * @param subs 级联对象
	 * @param doUpdate 执行更新
	 * @throws SQLException
	 */
	private static void checkAndInsert(Session trans, Collection<? extends IQueryableEntity> subs, boolean doUpdate) throws SQLException {
		if (subs == null)
			return;
		List<IQueryableEntity> toAdd = new ArrayList<IQueryableEntity>();
		for (IQueryableEntity d : subs) {
			if (DbUtils.getPrimaryKeyValue(d) != null) {
				d.getQuery().clearQuery();
				d.getQuery().setCascadeViaOuterJoin(false);
				List<IQueryableEntity> oldValue = trans.select(d);
				if (oldValue.size() > 0) {// 更新
					if (doUpdate) {
						IQueryableEntity old = oldValue.get(0);
						DbUtils.compareToUpdateMap(d, old);
						if (old.needUpdate()) {
							updateWithRefInTransaction(old, trans, 0);
						}
					}
					continue;
				}
			}
			toAdd.add(d);
		}
		// 插入
		insertWithRefInTransaction(toAdd, trans, ORMConfig.getInstance().isDynamicInsert(), 0);
	}

	@SuppressWarnings("unchecked")
	private static <T extends IQueryableEntity> T cast(Object obj, ITableMetadata c) {
		if (obj == null)
			return null;
		if (c.getThisType().isAssignableFrom(obj.getClass())) {
			return (T) obj;
		} else {
			throw new ClassCastException(obj.getClass().getName() + " can not cast to " + c.getName());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T extends IQueryableEntity> Collection<T> castToList(Object obj, AbstractRefField ref) {
		if (obj == null)
			return Collections.EMPTY_LIST;
		ITableMetadata c = ref.getReference().getTargetType();
		if (obj instanceof Collection<?>) {// 其他的Clection
			Collection<?> collection = (Collection<?>) obj;
			List<T> list = new ArrayList<T>();
			for (Object o : collection) {
				if(o==null)continue;
				if (c.getThisType().isAssignableFrom(o.getClass())) {
					list.add((T) o);
				} else {
					throw new IllegalArgumentException("There's a value can't cast to class:" + c);
				}
			}
			return list;
		} else if (obj.getClass().isArray()) {
			List<T> list = new ArrayList<T>();
			for (Object element : (Object[]) obj) {
				if (c.getThisType().isAssignableFrom(element.getClass())) {
					list.add((T) element);
				} else {
					throw new IllegalArgumentException("There's a value can't cast to class:" + c);
				}
			}
			return list;
		} else if (obj instanceof Map) {
			Cascade cascade = ref.getAsMap();

			if (cascade == null || StringUtils.isEmpty(cascade.valueOfMap())) {
				Collection<T> collection = ((Map) obj).values();
				return collection;
			} else {
				List<T> result = new ArrayList<T>();
				for (Map.Entry<String, ?> entry : ((Map<String, ?>) obj).entrySet()) {
					Object target = c.newInstance();
					BeanWrapper bw = BeanWrapper.wrap(target);
					bw.setPropertyValue(cascade.keyOfMap(), entry.getKey());
					bw.setPropertyValue(cascade.valueOfMap(), String.valueOf(entry.getValue()));
					result.add((T) target);
				}
				return result;
			}
		}
		throw new IllegalArgumentException("Unknow set class:" + obj.getClass());
	}

	public static ReverseReferenceProcessor getReverseProcessor(Reference ref) {
		List<Reference> reverses = ref.getExistReverseReference();
		if (reverses == null || reverses.isEmpty())
			return null;

		for (Iterator<Reference> iter = reverses.iterator(); iter.hasNext();) {
			// 凡是走到这里的都是 对多关系的反向关系，因此如果是 nv1才处理，nvn暂时不处理
			// 应该说只剩下一种关系，那就是 Nv1关系
			Reference reverse = iter.next();
			if (!reverse.getType().isToOne()) {
				// == ReferenceType.MANY_TO_MANY ||== ReferenceType.ONE_TO_MANY
				iter.remove();
			}
		}
		return new ReverseReferenceProcessor(reverses);
	}

	// 填充1vsN的字段
	// 每次处理一个关系： JEF中一个关系允许有多个字段被填充
	// <T extends DataObject> is true
	// .get(entry.getKey())
	protected static <T> void fillOneVsManyReference(List<T> list, Map.Entry<Reference, List<AbstractRefField>> entry, Map<Reference, List<Condition>> filters, Session session) throws SQLException {
		if (list.isEmpty())
			return;
		CascadeLoaderTask task = new CascadeLoaderTask(entry, filters);
		if (list.size() > 1000 || lazy(entry.getValue())) {// 不对超过1000个元素进行一对多填充//必须使用延迟加载
			markTask(task, list, session);
		} else {
			for (T obj : list) {
				task.process(session, obj);
			}
		}
	}

	static void markTask(LazyLoadTask task, List<?> objs, Session session) {
		if (objs.isEmpty())
			return;
		DataObject obj = (DataObject) objs.get(0);
		if (obj.lazyload != null) {
			obj.lazyload.getProcessor().register(task);
			return;
		}

		LazyLoadProcessor processor = new LazyLoadProcessor(task, session);
		for (Object o : objs) {
			DataObject dobj = (DataObject) o;
			dobj.lazyload = new LazyLoadContext(processor);
		}
	}

	static private boolean lazy(List<AbstractRefField> value) {
		if (ORMConfig.getInstance().isEnableLazyLoad()) {
			for (ISelectProvider prov : value) {
				AbstractRefField refd = (AbstractRefField) prov;
				if (refd.getFetch() == FetchType.EAGER) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
}
