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

import jef.common.wrapper.IntRange;
import jef.database.cache.CacheKeyProvider;
import jef.database.routing.PartitionResult;
import jef.database.routing.sql.InMemoryOperateProvider;

public interface QueryClause extends SqlClause, CacheKeyProvider,InMemoryOperateProvider {
	BindSql getSql(PartitionResult site);
	static final PartitionResult[] P = new PartitionResult[] { new PartitionResult("") };
	/**
	 * 不允许返回null。
	 * @return
	 */
	PartitionResult[] getTables();

	OrderClause getOrderbyPart();

	SelectPart getSelectPart();

	boolean isEmpty();

	void setOrderbyPart(OrderClause orderClause);

	void setPageRange(IntRange range);

	boolean isMultiDatabase();

	GroupClause getGrouphavingPart();

	boolean isDistinct();
}
