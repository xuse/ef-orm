package jef.tools;

import jef.common.wrapper.IntRange;

public final class PageLimit {
	private long start;
	private int limit;

	
	/**
	 * @param offset 从offset后面的记录读起
	 * @param limit 每页条数
	 */
	public PageLimit(long offset, int limit) {
		this.start = offset;
		this.limit = limit;

	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getStartAsInt() {
		if (start > Integer.MAX_VALUE) {
			throw new IllegalStateException("record offset too big: " + start);
		}
		return (int) start;
	}

	public int getEndAsInt() {
		long end = start + limit;
		if (end > Integer.MAX_VALUE) {
			throw new IllegalStateException("record number too big: " + end);
		}
		return (int) end;
	}

	public long getEnd() {
		return start + limit;
	}

	public int[] toArray() {
		if (start > Integer.MAX_VALUE) {
			throw new IllegalStateException("record offset too big: " + start);
		}
		return new int[] { (int) start, limit };
	}

	public static PageLimit parse(IntRange range) {
		if (range == null)
			return null;
		long start = range.getStart() - 1;
		int limit = range.size();
		return new PageLimit(start, limit);
	}
}
