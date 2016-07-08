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
package jef.database.jpa;

import java.sql.Savepoint;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import jef.database.Session;
import jef.database.support.SavepointNotSupportedException;

/**
 * JPA接口 EntityManager 的JEF实现类
 * 
 * @author Administrator
 * 
 */
@SuppressWarnings("rawtypes")
public class JefEntityManagerWithoutTx extends JefEntityManager {
	/**
	 * 构造
	 * 
	 * @param parent
	 * @param properties
	 */
	public JefEntityManagerWithoutTx(EntityManagerFactory parent, Map properties) {
		super(parent,properties);
	}
	

	public JefEntityTransaction getTransaction() {
		throw new UnsupportedOperationException();
	}
	public Savepoint setSavepoint(String savepointName) throws SavepointNotSupportedException {
		throw new UnsupportedOperationException();
	}
	public void rollbackToSavepoint(Savepoint savepoint) throws SavepointNotSupportedException {
		throw new UnsupportedOperationException();
	}
	public void releaseSavepoint(Savepoint savepoint) {
		throw new UnsupportedOperationException();
	}

	/**
	 * get the current databa sesession.
	 * 
	 * @return
	 */
	public Session getSession() {
		if (parent == null)
			throw new RuntimeException("the " + this.toString() + " has been closed!");
		return parent.getDefault();
	}
}
