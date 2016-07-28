package com.github.geequery.springdata.repository.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.metamodel.Metamodel;

import jef.database.jpa.JefEntityManagerFactory;
import jef.database.meta.ITableMetadata;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.github.geequery.springdata.mapping.JpaMetamodelMappingContext;

/**
 * {@link FactoryBean} to setup {@link JpaMetamodelMappingContext} instances from Spring configuration.
 * 
 * @author Oliver Gierke
 * @since 1.6
 */
class JpaMetamodelMappingContextFactoryBean extends AbstractFactoryBean<JpaMetamodelMappingContext> implements
		ApplicationContextAware {

	private ListableBeanFactory beanFactory;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = applicationContext;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return JpaMetamodelMappingContext.class;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#createInstance()
	 */
	@Override
	protected JpaMetamodelMappingContext createInstance() throws Exception {

		Set<JefEntityManagerFactory> models = getMetamodels();
		Set<Class<?>> entitySources = new HashSet<Class<?>>();

		for (JefEntityManagerFactory metamodel : models) {
			for (ITableMetadata type : metamodel.getEntityTypes()) {
				Class<?> javaType = type.getThisType();
				if (javaType != null) {
					entitySources.add(javaType);
				}
			}
		}

		JpaMetamodelMappingContext context = new JpaMetamodelMappingContext(models);
		context.setInitialEntitySet(entitySources);
		context.initialize();
		return context;
	}

	/**
	 * Obtains all {@link Metamodel} instances of the current {@link ApplicationContext}.
	 * 
	 * @return
	 */
	private Set<JefEntityManagerFactory> getMetamodels() {

		Collection<JefEntityManagerFactory> factories = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory,
				JefEntityManagerFactory.class).values();
		Set<JefEntityManagerFactory> metamodels = new HashSet<JefEntityManagerFactory>(factories.size());

		for (JefEntityManagerFactory emf : factories) {
			metamodels.add(emf);
		}

		return metamodels;
	}
}
