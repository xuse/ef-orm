/*
 * Copyright 2002-2008 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jef.database.datasource;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import jef.tools.Assert;

import org.slf4j.LoggerFactory;

/**
 * copy of org.springframework.jdbc.datasource.AbstractDataSource
 * 
 * @see org.springframework.jdbc.datasource.AbstractDataSource
 * 
 * @author luolp@asiainfo-linkage.com
 * @Date 2012-9-6
 */
public abstract class AbstractDataSource implements DataSource {

	/** Logger available to subclasses */
	protected final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Returns 0, indicating the default system timeout is to be used.
	 */
	public int getLoginTimeout() throws SQLException {
		return 0;
	}

	/**
	 * Setting a login timeout is not supported.
	 */
	public void setLoginTimeout(int timeout) throws SQLException {
		throw new UnsupportedOperationException("setLoginTimeout");
	}

	/**
	 * LogWriter methods are not supported.
	 */
	public PrintWriter getLogWriter() throws SQLException {
		throw new UnsupportedOperationException("getLogWriter");
	}

	/**
	 * LogWriter methods are not supported.
	 */
	public void setLogWriter(PrintWriter pw) throws SQLException {
		throw new UnsupportedOperationException("setLogWriter");
	}

	// ---------------------------------------------------------------------
	// Implementation of JDBC 4.0's Wrapper interface
	// ---------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		Assert.notNull(iface, "Interface argument must not be null");
		if (!DataSource.class.equals(iface)) {
			throw new SQLException(
					"DataSource of type [" + getClass().getName() +
							"] can only be unwrapped as [javax.sql.DataSource], not as ["
							+ iface.getName());
		}
		return (T) this;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		try{
			Class<? extends DataSource> clz=getWrappedClass();
			if(clz==null)return false;
			return clz.isAssignableFrom(iface);
		}catch(Exception e){
			return false;
		}
	}
	/**
	 * 返回子类能包装的DataSource的class。如果子类不是一个特定的datasource的包装器，返回null
	 * @return
	 */
	protected abstract Class<? extends DataSource> getWrappedClass();
	
	public Logger getParentLogger(){
		return Logger.getAnonymousLogger();
	}

}
