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
package jef.tools.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jef.common.LongList;

public class ArchiveSummary {
	private long unpSize = 0;
	private long packedSize = 0;
	private int itemCount = 0;
	private List<String> names = new ArrayList<String>();
	private LongList itemSize = new LongList();
	private LongList itemUnpSize = new LongList();

	public long getUnpSize() {
		return unpSize;
	}

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append("Items: ").append(itemCount).append(" total:").append(packedSize).append(" original:");
		sb.append(unpSize).append(" radio").append(getPackRadio());
		return sb.toString();
	}


	public void setUnpSize(long unpSize) {
		this.unpSize = unpSize;
	}

	public long getPackedSize() {
		return packedSize;
	}

	public void setPackedSize(long packedSize) {
		this.packedSize = packedSize;
	}

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	public void addItem(String name, long psize, long unpSize) {
		itemCount++;
		packedSize += psize;
		unpSize += unpSize;
		names.add(name);
		itemSize.add(psize);
		itemUnpSize.add(unpSize);
	}

	public float getPackRadio() {
		BigDecimal p = new BigDecimal(packedSize);
		return p.divide(new BigDecimal(unpSize),4).floatValue();
	}
}
