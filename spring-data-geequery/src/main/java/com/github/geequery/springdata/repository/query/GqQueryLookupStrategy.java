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
package com.github.geequery.springdata.repository.query;

import java.lang.reflect.Method;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;

import com.github.geequery.springdata.annotation.FindBy;

import jef.database.NativeQuery;
import jef.database.SessionFactory;
import jef.tools.StringUtils;

/**
 * Query lookup strategy to execute finders.
 */
public final class GqQueryLookupStrategy implements QueryLookupStrategy {
    private final SessionFactory emf;

    public GqQueryLookupStrategy(SessionFactory em) {
        this.emf = em;
    }

    @Override
    public RepositoryQuery resolveQuery(Method m, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {
        GqQueryMethod method = new GqQueryMethod(m, metadata, factory, emf);
        String qName = method.getNamedQueryName();
        String qSql = method.getAnnotatedQuery();
        if (method.isProcedureQuery()) {
            return new GqProcedureQuery(method, emf);
        } else if (StringUtils.isNotEmpty(qSql)) {
            NativeQuery<?> q;
            if (method.isNativeQuery()) {
                q = (NativeQuery<?>) emf.asDbClient().createNativeQuery(qSql, method.getReturnedObjectType());
            } else {
                q = (NativeQuery<?>) emf.asDbClient().createQuery(qSql, method.getReturnedObjectType());
            }
            return new GqNativeQuery(method, emf, q);
        }
        FindBy findBy = method.getFindByAnnotation();
        if (findBy != null) {
            return new GqPartTreeQuery(method, emf, findBy);
        } else if (emf.asDbClient().hasNamedQuery(qName)) {
            NativeQuery<?> q = (NativeQuery<?>) emf.asDbClient().createNamedQuery(qName, method.getReturnedObjectType());
            return new GqNativeQuery(method, emf, q);
        } else {
            if (qName.endsWith(".".concat(method.getName()))) {
                try {
                    return new GqPartTreeQuery(method, emf);
                } catch (Exception e) {
                    throw new IllegalArgumentException(method + ": " + e.getMessage(), e);
                }
            } else {
                throw new IllegalArgumentException("Named query not found: '" + qName + "' in method" + method);
            }
        }

    }

}
