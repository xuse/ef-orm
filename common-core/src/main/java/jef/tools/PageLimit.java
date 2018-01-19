package jef.tools;

import jef.common.wrapper.IntRange;

/**
 * 描述offset和limit两个参数，用于分页
 * 
 * @author Joey
 *
 */
public final class PageLimit {
	private int limit;
	
	private long offset;
	
	/**
	 * @param offset 从offset后面的记录读起
	 * @param limit 每页条数
	 */
	public PageLimit(long offset, int limit) {
		this.offset = offset;
		this.limit = limit;

	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getStartAsInt() {
		if (offset > Integer.MAX_VALUE) {
			throw new IllegalStateException("record offset too big: " + offset);
		}
		return (int) offset;
	}

	/**
	 * @deprecated 
	 * @return
	 */
	public int getEndAsInt() {
		long end = offset + limit;
		if (end > Integer.MAX_VALUE) {
			throw new IllegalStateException("record number too big: " + end);
		}
		return (int) end;
	}

	public long getEnd() {
		return offset + limit;
	}

	/**
	 * @deprecated the offset may be a long value.
	 * @return
	 */
	public int[] toArray() {
		if (offset > Integer.MAX_VALUE) {
			throw new IllegalStateException("record offset too big: " + offset);
		}
		return new int[] { (int) offset, limit };
	}

	public static PageLimit parse(IntRange range) {
		if (range == null)
			return null;
		long offset = range.getStart() - 1;
		int limit = range.size();
		return new PageLimit(offset, limit);
	}
}
