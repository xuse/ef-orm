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
package jef.database.wrapper.clause;

import java.util.List;

import jef.database.jdbc.result.ResultSetContainer;
import jef.database.jdbc.statement.ResultSetLaterProcess;
import jef.database.routing.sql.InMemoryOperateProvider;
import jef.database.wrapper.processor.BindVariableDescription;
import jef.tools.PageLimit;

/**
 * 描述一个绑定变量的SQL语句
 * @author jiyi
 *
 */
public final class BindSql implements InMemoryOperateProvider{
	private String sql;
	private List<BindVariableDescription> bind;
	private ResultSetLaterProcess isReverseResultSet;
	
	public BindSql(String sql){
		this.sql=sql;
	}
	
	public BindSql(String sql,List<BindVariableDescription> bind){
		this.sql=sql;
		this.bind=bind;
	}
	/**
	 * 得到SQL语句
	 * @return SQL语句
	 */
	public String getSql() {
		return sql;
	}
	public BindSql setSql(String sql) {
		this.sql = sql;
		return this;
	}
	/**
	 * 得到绑定变量描述
	 * @return 绑定变量描述
	 */
	public List<BindVariableDescription> getBind() {
		return bind;
	}
	public BindSql setBind(List<BindVariableDescription> bind) {
		this.bind = bind;
		return this;
	}
	public boolean isBind(){
		return bind!=null && bind.size()>0;
	}
	@Override
	public String toString() {
		return sql;
	}

	public ResultSetLaterProcess getRsLaterProcessor() {
		return isReverseResultSet;
	}

	public BindSql setReverseResult(ResultSetLaterProcess isReverseResultSet) {
		this.isReverseResultSet = isReverseResultSet;
		return this;
	}

	@Override
	public boolean hasInMemoryOperate() {
		return false;
	}

	@Override
	public void parepareInMemoryProcess(PageLimit range, ResultSetContainer rs) {
		throw new UnsupportedOperationException();
	}
}
