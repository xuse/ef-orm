/*
 * Copyright 2008-2016 the original author or authors.
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
package com.github.geequery.springdata.provider;

import static com.github.geequery.springdata.provider.PersistenceProvider.Constants.GQ_ENTITY_MANAGER_INTERFACE;
import static com.github.geequery.springdata.provider.PersistenceProvider.Constants.GQ_JPA_METAMODEL_TYPE;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.metamodel.Metamodel;

import jef.database.DbUtils;
import jef.database.IQueryableEntity;
import jef.database.jpa.JefEntityManagerFactory;

import org.springframework.data.util.CloseableIterator;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Enumeration representing persistence providers to be used.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public enum PersistenceProvider implements ProxyIdAccessor {

	GEEQUERY(//
			Arrays.asList(GQ_ENTITY_MANAGER_INTERFACE), //
			Arrays.asList(GQ_JPA_METAMODEL_TYPE)) {
		public String extractQueryString(Query query) {
			return query.toString();
		}

		@Override
		public String getCountQueryPlaceholder() {
			return "*";
		}

		@Override
		public <T> Collection<T> potentiallyConvertEmptyCollection(Collection<T> collection) {
			return collection == null || collection.isEmpty() ? null : collection;
		}

		@Override
		public boolean shouldUseAccessorFor(Object entity) {
			return false;
		}

		@Override
		public Object getIdentifierFrom(Object entity) {
			List<Object> result = DbUtils.getPrimaryKeyValue((IQueryableEntity) entity);
			if (result.size() == 1) {
				return result.get(0);
			}
			throw new UnsupportedOperationException();
		}
	};

	/**
	 * Holds the PersistenceProvider specific interface names.
	 * 
	 * @author Thomas Darimont
	 */
	static interface Constants {
		String GQ_ENTITY_MANAGER_INTERFACE = "jef.database.jpa.JefEntityManager";
		String GQ_JPA_METAMODEL_TYPE = "jef.database.jpa.JefEntityManagerFactory";
	}

	private static ConcurrentReferenceHashMap<Class<?>, PersistenceProvider> CACHE = new ConcurrentReferenceHashMap<Class<?>, PersistenceProvider>();

	private final Iterable<String> entityManagerClassNames;
	private final Iterable<String> metamodelClassNames;

	/**
	 * Creates a new {@link PersistenceProvider}.
	 * 
	 * @param entityManagerClassNames
	 *            the names of the provider specific {@link EntityManager}
	 *            implementations. Must not be {@literal null} or empty.
	 */
	private PersistenceProvider(Iterable<String> entityManagerClassNames, Iterable<String> metamodelClassNames) {

		this.entityManagerClassNames = entityManagerClassNames;
		this.metamodelClassNames = metamodelClassNames;
	}

	/**
	 * Determines the {@link PersistenceProvider} from the given
	 * {@link EntityManager}. If no special one can be determined
	 * {@link #GENERIC_JPA} will be returned.
	 * 
	 * 传入的是JPA EM的Proxy，因此无法转换为JefEM
	 * 
	 * 
	 * @param em
	 *            must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static PersistenceProvider fromEntityManager(EntityManagerFactory em) {
		return PersistenceProvider.GEEQUERY;
//		Assert.notNull(em, "EntityManager must not be null!");
//
//		Class<?> entityManagerType = em.getDelegate().getClass();
//		PersistenceProvider cachedProvider = CACHE.get(entityManagerType);
//
//		if (cachedProvider != null) {
//			return cachedProvider;
//		}
//		for (PersistenceProvider provider : values()) {
//			for (String entityManagerClassName : provider.entityManagerClassNames) {
//				
//			}
//		}
//		throw new UnsupportedOperationException();
	}

	/**
	 * Determines the {@link PersistenceProvider} from the given
	 * {@link Metamodel}. If no special one can be determined
	 * {@link #GENERIC_JPA} will be returned.
	 * 
	 * @param metamodel
	 *            must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static PersistenceProvider fromMetamodel(JefEntityManagerFactory metamodel) {
		Assert.notNull(metamodel, "Metamodel must not be null!");
		return PersistenceProvider.GEEQUERY;
//		Class<? extends Metamodel> metamodelType = metamodel.getClass();
//		PersistenceProvider cachedProvider = CACHE.get(metamodelType);
//
//		if (cachedProvider != null) {
//			return cachedProvider;
//		}
//
//		for (PersistenceProvider provider : values()) {
//			for (String metamodelClassName : provider.metamodelClassNames) {
//				if (isMetamodelOfType(metamodel, metamodelClassName)) {
//					return cacheAndReturn(metamodelType, provider);
//				}
//			}
//		}
//		throw new UnsupportedOperationException();
	}

	/**
	 * Caches the given {@link PersistenceProvider} for the given source type.
	 * 
	 * @param type
	 *            must not be {@literal null}.
	 * @param provider
	 *            must not be {@literal null}.
	 * @return
	 */
	private static PersistenceProvider cacheAndReturn(Class<?> type, PersistenceProvider provider) {
		CACHE.put(type, provider);
		return provider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.geequery.springdata.repository.query.QueryExtractor#
	 * canExtractQuery ()
	 */
	public boolean canExtractQuery() {
		return true;
	}

	/**
	 * Returns the placeholder to be used for simple count queries. Default
	 * implementation returns {@code *}.
	 * 
	 * @return
	 */
	public String getCountQueryPlaceholder() {
		return "x";
	}

	/**
	 * Potentially converts an empty collection to the appropriate
	 * representation of this {@link PersistenceProvider}, since some JPA
	 * providers cannot correctly handle empty collections.
	 * 
	 * @see DATAJPA-606
	 * @param collection
	 * @return
	 */
	public <T> Collection<T> potentiallyConvertEmptyCollection(Collection<T> collection) {
		return collection;
	}

	public CloseableIterator<Object> executeQueryWithResultStream(Query jpaQuery) {
		throw new UnsupportedOperationException("Streaming results is not implement for this PersistenceProvider: " + name());
	}

	public static PersistenceProvider fromEntityManager(EntityManager em) {
		return PersistenceProvider.GEEQUERY;
	}
}
