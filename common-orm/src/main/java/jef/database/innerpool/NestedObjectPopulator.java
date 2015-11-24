package jef.database.innerpool;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.Assert;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.common.Entry;
import jef.database.DbUtils;
import jef.database.IQueryableEntity;
import jef.database.jdbc.result.IResultSet;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.wrapper.populator.IPopulator;
import jef.tools.FunctionProperty;
import jef.tools.reflect.BeanWrapper;
import jef.tools.reflect.GenericUtils;
import jef.tools.reflect.Property;

import com.google.common.base.Function;

public class NestedObjectPopulator implements IPopulator {
	private String propertyName;
	private InstancePopulator populator;

	/**
	 * 为了提高性能而将部分拼装策略对象抽取出来进行缓存
	 */
	@SuppressWarnings("rawtypes")
	private transient Class baseType;
	private transient List<Entry<Property, BeanAccessor>> accessPath;
	private transient Property lastAccessor;

	public NestedObjectPopulator(String property, InstancePopulator populator) {
		this.propertyName = property;
		this.populator = populator;
	}

	private void parepare(Class<?> base) {
		String name = propertyName;
		int n = name.indexOf('.');
		accessPath = new ArrayList<Entry<Property, BeanAccessor>>();

		BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(base);

		while (n > -1) {
			String thisName = name.substring(0, n);
			name = name.substring(n + 1);
			n = name.indexOf('.');

			Property accessor = ba.getProperty(thisName);
			Class<?> accessorType = ba.getPropertyType(thisName);
			ba = FastBeanWrapperImpl.getAccessorFor(accessorType);
			accessPath.add(new Entry<Property, BeanAccessor>(accessor, ba));
		}

		lastAccessor = ba.getProperty(name);
		checkAccessType();
		baseType = base;
	}

	private void checkAccessType() {
		Class<?> accessorType = lastAccessor.getType();
		if (!accessorType.isAssignableFrom(populator.getObjectType())) {
			if(accessorType==List.class || accessorType==ArrayList.class){
				Type type=GenericUtils.getCollectionType(lastAccessor.getGenericType());
				if(type==populator.getObjectType()){
					this.lastAccessor=new FunctionProperty(lastAccessor).setSetFunction(new ToListFunction());
					return;
				}
			}else if(accessorType==Set.class || accessorType==HashSet.class){
				Type type=GenericUtils.getCollectionType(lastAccessor.getGenericType());
				if(type==populator.getObjectType()){
					this.lastAccessor=new FunctionProperty(lastAccessor).setSetFunction(new ToSetFunction());
					return;
				}
			}
			throw new IllegalArgumentException("Invalid config:" + accessorType.getName()+ " can not fill with " + populator.getObjectType().getName() + ".");
		}
	}

	public void process(BeanWrapper wrapper, IResultSet rs) throws SQLException{
		Object raw=wrapper.getWrapped();
		if(raw.getClass()!=baseType){
			parepare(raw.getClass());//初始化策略，在
		}
		Object subDo = populator.instance();
		boolean flag =  populator.processOrNull(BeanWrapper.wrap(subDo), rs);
		if (flag) {
			if (subDo instanceof IQueryableEntity) {
				((IQueryableEntity) subDo).startUpdate();
				//TODO 
				//Jiyi 2015/11/24临时性增加，因为在项目中发现会存在空对象被外连接方式加载情况，为进一步跟踪此问题，添加相应断言代码
				//问题调试完成后需要删除
				ensureNotNull((IQueryableEntity) subDo);
			}
			for(Entry<Property,BeanAccessor> entry: accessPath){
				Object v=entry.getKey().get(raw);
				if(v==null){
					v=entry.getValue().newInstance();
					entry.getKey().set(raw, v);					
				}
				raw=v;//构造中间对象
			}
			lastAccessor.set(raw, subDo);
		}
	}
	private void ensureNotNull(IQueryableEntity subDo) {
		Assert.notNull(DbUtils.getPrimaryKeyValue(subDo));
	}
	@SuppressWarnings("rawtypes")
	static class ToSetFunction implements Function<Object,Object>{
		@SuppressWarnings("unchecked")
		public Object apply(Object input) {
			Set set=new HashSet();
			set.add(input);
			return set;
		}
		
	}
	@SuppressWarnings("rawtypes")
	static class ToListFunction implements Function<Object,Object>{
		@SuppressWarnings("unchecked")
		public Object apply(Object input) {
			List list=new ArrayList();
			list.add(input);
			return list;
		}
		
	}
}
