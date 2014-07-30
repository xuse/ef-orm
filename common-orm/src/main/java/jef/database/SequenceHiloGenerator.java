package jef.database;

import java.sql.SQLException;

import jef.database.meta.AbstractSequence;

/**
 * 根据Hibernate的hilo算法的实现
 * @author Administrator
 *
 */
public final class SequenceHiloGenerator extends AbstractSequence {
	private Sequence inner;
	private int maxLo;

	public SequenceHiloGenerator(Sequence inner, int maxLo) {
		super(null,null);
		if (maxLo < 1) {
			maxLo = 1;
		}
		this.inner = inner;
		this.maxLo = maxLo;
	}

	@Override
	protected long getFirstAndPushOthers(int size,DbClient conn,String dbKey) throws SQLException {
		long value = inner.next();
		long min = value * (maxLo + 1);
		long max = min + maxLo;
		pushRange(min + 1, max);
		return min;
	}

	public boolean isTable() {
		return inner.isTable();
	}

	public String getName() {
		return inner.getName();
	}

	@Override
	protected boolean doInit(DbClient session, String dbKey) throws SQLException {
		return true;
	}

	public boolean isRawNative() {
		return false;
	}
}
