package jef.database.dialect.handler;

import jef.database.dialect.type.AColumnMapping;

/**
 * 
 * @author Administrator
 *
 */
public abstract class CaseHandler {
	abstract String getObjectNameToUse(String name);
	abstract String getObjectNameToUse(AColumnMapping column);
	
	public static CaseHandler UPPER=new CaseHandler(){
		@Override
		String getObjectNameToUse(String name) {
			return name.toUpperCase();
		}

		@Override
		String getObjectNameToUse(AColumnMapping column) {
			return column.upperColumnName();
		}
	};
	public static CaseHandler LOWER=new CaseHandler(){
		@Override
		String getObjectNameToUse(String name) {
			return name.toLowerCase();
		}

		@Override
		String getObjectNameToUse(AColumnMapping column) {
			return column.lowerColumnName();
		}
	};
}
