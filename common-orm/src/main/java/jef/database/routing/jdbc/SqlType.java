package jef.database.routing.jdbc;

/**
 * @author linxuan
 */
public enum SqlType {
	SELECT(0), INSERT(1), UPDATE(2), DELETE(3), SELECT_FOR_UPDATE(4),REPLACE(5),TRUNCATE(6),CREATE(7),DROP(8),LOAD(9),MERGE(10),SHOW(11),ALTER(12),RENAME(13),DUMP(14),DEBUG(15), EXPLAIN(16), DEFAULT_SQL_TYPE(-100);
	private int i;

	private SqlType(int i) {
		this.i = i;
	}

	public int value() {
		return this.i;
	}

	public static SqlType valueOf(int i) {
		for (SqlType t : values()) {
			if (t.value() == i) {
				return t;
			}
		}
		throw new IllegalArgumentException("Invalid SqlType:" + i);
	}
}
