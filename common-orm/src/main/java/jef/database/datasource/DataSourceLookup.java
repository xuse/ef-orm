/*
 * Copyright 2002-2012 the original author or authors.
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

package jef.database.datasource;

import java.util.Collection;
import java.util.NoSuchElementException;

import javax.sql.DataSource;

/**
 * Strategy interface for looking up DataSources by name.
 *
 * <p>Used, for example, to resolve data source names in JPA
 * {@code persistence.xml} files.
 * <p/>
 * common-orm提供了若干基本的DataSourceLookup
 *<p/>
 * 在common-orm-routing包中，还提供了对SpringBeanFactory支持的：
 *  SpringBeansDataSourceLookup等类
 *  目前已知的几个实现类(六个)可以参见Seel Also:
 * 
 *
 * @see MapDataSourceLookup
 * @see DbDataSourceLookup
 * @see JndiDatasourceLookup
 * @see SpringBeansDataSourceLookup
 *  
 */
public interface DataSourceLookup {

	/**
	 * Retrieve the DataSource identified by the given name.
	 * @param dataSourceName the name of the DataSource
	 * @return the DataSource (never {@code null})
	 * @throws NoSuchElementException if the lookup failed
	 */
	DataSource getDataSource(String dataSourceName);
	
	/**
	 * 当不指定ds名称时，返回一个Key。
	 * 例如当Spring或者数据库中只配置了一个DataSource时，默认就返回该Ds，并不是所有的Lookup都能实现这一功能，如果不能实现，那么就返回null
	 * @return
	 */
	String getDefaultKey();
	
	/**
	 * 得到目前可用的key
	 * @return
	 */
	Collection<String> getAvailableKeys();
}
