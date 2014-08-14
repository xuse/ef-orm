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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jef.database.rowset.CachedRowSetImpl;
import jef.database.rowset.Row;

public class InMemoryOrderBy implements InMemoryProcessor {
	private int[] orderFields;
	private boolean[] orderAsc;

	public InMemoryOrderBy(int[] orders, boolean[] orderAsc2) {
		this.orderFields = orders;
		this.orderAsc = orderAsc2;
	}

	public int[] getOrderFields() {
		return orderFields;
	}

	public void setOrderFields(int[] orderFields) {
		this.orderFields = orderFields;
	}

	public boolean[] getOrderAsc() {
		return orderAsc;
	}

	public void setOrderAsc(boolean[] orderAsc) {
		this.orderAsc = orderAsc;
	}

	public void process(CachedRowSetImpl rowset) {
		List<Row> rows=rowset.getRvh();
		Collections.sort(rows, new Comparator<Row>() {
			public int compare(Row o1, Row o2) {
				for (int i = 0; i < orderFields.length; i++) {
					int r = compare0(o1.getColumnObject(orderFields[i]), o2.getColumnObject(orderFields[i]));
					if(r==0)
						continue; //判断下一个字段
					
					if(!orderAsc[i]){
						r=-r;
					}
					return r;
				}
				return 0;
			}
			@SuppressWarnings({ "unchecked", "rawtypes" })
			private int compare0(Object object, Object object2) {
				if (object == object2)
					return 0;
				if (object == null)
					return 1;
				if (object2 == null)
					return -1;
				return ((Comparable) object).compareTo(object2);
			}
		});
	}

	public String getName() {
		return "order by";
	}
}
