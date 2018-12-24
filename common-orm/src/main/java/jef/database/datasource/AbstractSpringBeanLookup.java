package jef.database.datasource;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import jef.database.DbUtils;
import jef.tools.Assert;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.GenericUtils;

/**
 * 抽象类，能从Spring中找寻指定类型的bean
 * @author jiyi
 *
 * @param <T>
 */
public abstract class AbstractSpringBeanLookup<T> implements ApplicationContextAware,InitializingBean{
	protected ApplicationContext context;
	protected Map<String, T> cache;
	private boolean ignorCase = true;//默认是忽略大小写的
	private Class<T> t;
	protected Logger log=LoggerFactory.getLogger(this.getClass());
	protected String defaultBeanName;
	
	@SuppressWarnings("unchecked")
	public AbstractSpringBeanLookup(){
		Class<?> c = getClass();
		c = ClassEx.getRealClass(c);
		Type[] t = DbUtils.getTypeParameters(c, AbstractSpringBeanLookup.class);
		Type type = t[0];
		if (type instanceof Class<?>) {
		} else if (type instanceof ParameterizedType) {
			type = GenericUtils.getRawClass(type);
		} else {
			throw new IllegalArgumentException("The class " + this.getClass().getName() + " must assign the generic type T.");
		}
		this.t = (Class<T>) type;		
		
	}
	
	protected final Map<String, T> getCache() {
		Assert.notNull(context);
		Map<String, T> ds = context.getBeansOfType(t);// 这是一个非常复杂的操作，因此将结果缓存起来
		log.debug("getting type:{} from spring context, found {} beans.",t.getClass(),ds.size());
		Map<String, T> result = new LinkedHashMap<String, T>();
		for (Map.Entry<String, T> entry : ds.entrySet()) {
			if(entry.getValue() instanceof DefaultRoutingDataSource){
				continue;
			}
			log.debug("puting [{}] into map, bean: {}",entry.getKey(),entry.getValue());
			result.put(ignorCase?StringUtils.lowerCase(entry.getKey()):entry.getKey(), entry.getValue());
		}
		return result;
	}

	public final void afterPropertiesSet() throws Exception {
		Assert.notNull(context);
	}

	public String getDefaultBeanName() {
		return defaultBeanName;
	}

	public void setDefaultBeanName(String defaultBeanName) {
		this.defaultBeanName = defaultBeanName;
	}

	/**
	 * 查找DataSoruce是否忽略大小写
	 * 
	 * @return true if ignore the case of datasoure name
	 */
	public boolean isIgnorCase() {
		return ignorCase;
	}

	/**
	 * 查找DataSoruce是否忽略大小写
	 * 
	 * @param ignorCase
	 */
	public void setIgnorCase(boolean ignorCase) {
		this.ignorCase = ignorCase;
	}
	public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context=applicationContext;
	}
}
