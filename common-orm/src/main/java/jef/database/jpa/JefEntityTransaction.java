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

import java.sql.SQLException;

import javax.persistence.EntityTransaction;

import jef.common.log.LogUtil;
import jef.database.Transaction;
import jef.tools.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA接口 EntityTransaction的JEF实现类。这个类描述一个JPA事务。
 * 为了实现JPA的多事务嵌套（其实是spring的多事务嵌套特性，这里实际上使用一个Stack的接口）也就是说是多个JEF食物链构成的链表
 * 
 * @author Administrator
 * 
 */
public class JefEntityTransaction implements EntityTransaction {
	private Transaction trans;
	private JefEntityManager parent = null;
	private static Logger log = LoggerFactory.getLogger(JefEntityTransaction.class);

	public JefEntityTransaction(JefEntityManager parent) {
		this.parent = parent;
	}

	public void begin() {
		if (trans != null) {
			throw new IllegalStateException("Can not open a transaction twice on a EntityTransaction..");
		}
		trans = parent.parent.getDefault().startTransaction();
		log.trace("[JPA DEBUG]:Transaction {}, started at {}", trans, parent);
	}
	

	public void begin(int timeout, int isolationLevel, boolean readOnly) {
		if (trans != null) {
			throw new IllegalStateException("Can not open a transaction twice on a EntityTransaction..");
		}
		trans = parent.parent.getDefault().startTransaction(timeout,isolationLevel,readOnly);
		
		
		
	}
	

	public void commit() {
		if (trans != null) {
			try {
				trans.commit();
			} catch (SQLException e) {// 若是发生其他异常可能会导致事务退栈不正确，因为有可能不会去执行close方法
				LogUtil.exception(e);
			}
		}
	}

	public void rollback() {
		if (trans != null) {
			try {
				trans.rollback();
			} catch (SQLException e) {
				LogUtil.exception(e);
			}
		}
	}

	public void setRollbackOnly() {
		Assert.notNull(trans, "this transaction is closed.");
		trans.setRollbackOnly(true);
	}

	public boolean getRollbackOnly() {
		return trans == null ? false : trans.isRollbackOnly();
	}

	public boolean isActive() {
		return trans != null && trans.isOpen();
	}

	@Override
	public String toString() {
		if (trans == null) {
			StringBuilder sb = new StringBuilder();
			sb.append("[empty entity-trans] from ").append(this.parent.toString());
			return sb.toString();
		} else {
			return trans.toString();
		}
	}

	public Transaction get() {
		return trans;
	}
}
