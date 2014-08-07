package jef.database.wrapper.clause;

public enum GroupFunctionType {
	MIN,MAX,COUNT,AVG,SUM,ARRAY_TO_LIST,
	/**
	 * 非以上的任何一种情况 
	 */
	NORMAL,
	GROUP,
}
