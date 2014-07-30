package jef.database.wrapper;

import java.io.Closeable;
import java.sql.SQLException;
import java.util.Iterator;

import jef.common.log.LogUtil;

/**
 * 用于辅助用户遍历数据库查询结果集的遍历器
 * 
 * 具备特性是：当遍历完成时，会自动关闭游标结果集。当最后一次调用hasNext()方法返回false时，就会去关闭。 当然，遍历未完成时，你也可以通过调用
 * {@link #close()}方法，手工关闭结果集。
 * 
 * @see #close()
 * @author jiyi
 * 
 * @param <T>
 */
public interface ResultIterator<T> extends Iterator<T>, Closeable {
	public void close();
	
	final class Impl<T> implements ResultIterator<T> {
		private IResultSet rs;
		private Iterator<T> iterateResultSet;

		public Impl(Iterator<T> iterateResultSet, IResultSet rs) {
			this.iterateResultSet = iterateResultSet;
			this.rs = rs;
		}

		public boolean hasNext() {
			boolean hasnext = iterateResultSet.hasNext();
			if (!hasnext)
				close();
			return hasnext;
		}

		public T next() {
			return iterateResultSet.next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public void close() {
			if (rs == null)
				return;
			try {
				rs.close();
			} catch (SQLException e) {
				LogUtil.exception(e);
			}
			rs = null;
		}
	}

}
