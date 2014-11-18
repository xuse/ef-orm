package jef.database.routing.jdbc;

/**
 */
public enum SqlType {
	SELECT,
	INSERT, 
	UPDATE, 
	DELETE,
	SELECT_FOR_UPDATE,
	REPLACE,
	TRUNCATE,
	CREATE,
	DROP,
	LOAD,
	MERGE,
	SHOW,
	ALTER,
	RENAME,
	DUMP,
	DEBUG,
	EXPLAIN,
	DEFAULT_SQL_TYPE;
}
