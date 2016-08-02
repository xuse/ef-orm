package jef.database;

import java.sql.SQLException;

/**
 * 持有一个查询结果，以及对应的resultset中的记录
 * 
 * @author Administrator
 *
 * @param <T>
 */
public class RecordHolder<T extends IQueryableEntity> {
	private T object;
	private RecordsHolder<T> parent;
	int status = 0;// 1表示是删除的，-1表示是已经删除的
	int index;// 0表示rs的第一条记录。

	public static final int INSERT = -1;
	public static final int INSERTED = -2;
	public static final int DELETE = -3;
	public static final int DELETED = -4;
	public static final int CACNLED = -5;

	int getStatus() {
		return status;
	}

	boolean isInsert() {
		return status == INSERT;
	}

	boolean isInserted() {
		return status == INSERTED;
	}

	boolean isBeingDelete() {
		return status == DELETE;
	}

	boolean isDeleted() {
		return status == DELETED;
	}

	void setDeleted() {
		if (this.status == DELETE) {
			this.status = DELETED;
		} else {
			throw new IllegalArgumentException("the record is not about to delete.");
		}
	}

	void setInserted() {
		if (this.status == INSERT) {
			this.status = INSERTED;
		} else {
			throw new IllegalArgumentException("the record is not about to insert.");
		}
	}

	RecordHolder(RecordsHolder<T> parent, int index, T object) {
		this.parent = parent;
		this.index = index;
		if (index < 0)
			status = INSERT;
		this.object = object;
	}

	/**
	 * 获得记录对应的Entity Bean
	 * 
	 * @return
	 */
	public T get() {
		if (status == DELETED)
			return null;
		return object;
	}

	/**
	 * 更新此条记录。并提交到数据库。最后释放游标
	 * 
	 * @throws SQLException
	 * @retuen true返回是否更新了记录
	 */
	public boolean commit() throws SQLException {
		return parent.commit() > 0;
	}

	/**
	 * 删除此条记录，并提交到数据库
	 * 
	 * @throws SQLException
	 */
	public void delete() throws SQLException {
		if (status == INSERT) {
			this.status = CACNLED;
		} else if (status == INSERTED) {
			throw new UnsupportedOperationException("Can not delete a record inseted right now.");
		} else if (status == DELETED) {
			throw new UnsupportedOperationException("Can not delete a record deleted right now.");
		} else {
			this.status = DELETE;
		}
		parent.commit();
	}

	/**
	 * 关闭当前ResultSet，如果有未提交的修改直接丢弃
	 * 
	 * @throws SQLException
	 */
	public void close() throws SQLException {
		parent.close();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("index:").append(index).append('\t');
		sb.append(status);
		return sb.toString();
	}

}
