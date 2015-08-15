package jef.database.dialect;

public class Type {
	private String name;
	private int sqlType;
	private String[] alias;

	public Type(int i, String value) {
		this.name = value;
		this.sqlType = i;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSqlType() {
		return sqlType;
	}

	public void setSqlType(int sqlType) {
		this.sqlType = sqlType;
	}

	public String[] getAlias() {
		return alias;
	}

	public void setAlias(String... alias) {
		this.alias = alias;
	}
}
