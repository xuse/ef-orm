package jef.database.meta.object;

import jef.database.dialect.type.AColumnMapping;

public enum Case {
	UPPER {
		public String getObjectNameToUse(String name) {
			return name.toUpperCase();
		}
		public String getObjectNameToUse(AColumnMapping column) {
			return column.upperColumnName();
		}
		public boolean isCaseSensitive() {
			return false;
		}
	}, LOWER {
		public String getObjectNameToUse(String name) {
			return name.toLowerCase();
		}
		public String getObjectNameToUse(AColumnMapping column) {
			return column.lowerColumnName();
		}
		public boolean isCaseSensitive() {
			return false;
		}
	}, MIXED_SENSITIVE {
		public String getObjectNameToUse(String name) {
			return name;
		}
		public String getObjectNameToUse(AColumnMapping column) {
			return column.rawColumnName;
		}
		public boolean isCaseSensitive() {
			return true;
		}
	};
	public abstract String getObjectNameToUse(String name);
	public abstract String getObjectNameToUse(AColumnMapping column);
	public abstract boolean isCaseSensitive();
}
