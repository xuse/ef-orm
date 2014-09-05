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

/**
 * Strategy interface for looking up DataSources by name.
 *
 * <p>Used, for example, to resolve data source names in JPA
 * {@code persistence.xml} files.
  
 * 使用DataSourceInfoLookup，可以设置一个自定义的解密器，用于处理当数据库密码加密的场景
 * 目前已知的六个实现类可以参见Seel Also:
 * 
 * @see URLJsonDataSourceInfoLookup
 * @see DbDataSourceLookup
 * @see MapDataSourceInfoLookup
 * @see PropertyDataSourceInfoLookup
 * @see JndiDatasourceLookup
 * @see jef.database.datasource.SpringBeansDataSourceInfoLookup
 */
public interface DataSourceInfoLookup {

	/**
	 * Retrieve the DataSource identified by the given name.
	 * @param dataSourceName the name of the DataSource
	 * @return the DataSource (never {@code null})
	 * @throws NoSuchElementException if the lookup failed
	 */
	DataSourceInfo getDataSourceInfo(String dataSourceName);
	
	/**
	 * 设置数据库密码解密回调类。很多时候，我们配置的数据库密码都是加密后的，这种场合下我们可以实现PasswordDecryptor接口，
	 * 并将其设置到DataSourceLookup中，每当发现新的数据源，就可以对其中的用户口令解密.
	 * @param passwordDecryptor
	 */
	void setPasswordDecryptor(PasswordDecryptor passwordDecryptor);
	
	/**
	 * 返回缺省的数据源名称<br>
	 * 例如当Spring或者数据库中只配置了一个DataSource时，默认就返回该Ds。
	 * 或者如果用户配置了缺省数据源名称时，返回用户配置的名称。<br>
	 * 如果无法判断哪一个是缺省数据源，那么就返回null
	 * @return
	 */
	String getDefaultKey();
	
	/**
	 * 得到目前可用的key
	 * @return
	 */
	Collection<String> getAvailableKeys();
}
